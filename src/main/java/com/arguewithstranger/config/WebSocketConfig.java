package com.arguewithstranger.config;

import com.arguewithstranger.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket and STOMP message broker configuration.
 *
 * Architecture overview:
 *
 *   Browser (SockJS + STOMP)
 *       │
 *       │  WS handshake
 *       ▼
 *   /chat  ──────────────────────── endpoint
 *       │
 *       │  STOMP CONNECT (JWT in header)
 *       ▼
 *   JwtChannelInterceptor  ──────── validates JWT
 *       │
 *       ├─ Client SENDS to /app/sendMessage
 *       │       │
 *       │       ▼
 *       │  @MessageMapping in ChatWebSocketController
 *       │       │
 *       │       ▼
 *       │  MessageService.persist() → MySQL
 *       │       │
 *       │       ▼
 *       │  SimpMessagingTemplate.convertAndSend()
 *       │       │
 *       └─ Broker broadcasts to /topic/debate/{id}
 *               │
 *               ▼
 *         All subscribed clients receive MessageResponse JSON
 *
 * SockJS fallback — when WebSocket is not available (some
 * corporate proxies block WS), SockJS automatically falls
 * back to long-polling. The client code is identical either way.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil            jwtUtil;
    private final UserDetailsService userDetailsService;

    // ── Broker Configuration ───────────────────────────────────

    /**
     * Configures the message broker.
     *
     * enableSimpleBroker("/topic")
     *   → In-memory broker. Messages sent to /topic/... are
     *     automatically routed to all subscribers of that destination.
     *     For production at scale, replace with a dedicated broker
     *     (RabbitMQ, ActiveMQ) using enableStompBrokerRelay().
     *
     * setApplicationDestinationPrefixes("/app")
     *   → Messages sent by clients to /app/... are routed to
     *     @MessageMapping methods in our controllers.
     *     /app/sendMessage → ChatWebSocketController.sendMessage()
     *
     * setUserDestinationPrefix("/user")
     *   → Enables sending messages to a specific user via
     *     /user/{username}/queue/... (for private notifications).
     *     Not used for debate chat but useful for error feedback
     *     to individual connections.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Registers the WebSocket endpoint the browser connects to.
     *
     * /chat
     *   → The URL path for the WebSocket handshake.
     *     Client: new SockJS('http://localhost:8080/chat')
     *
     * withSockJS()
     *   → Enables SockJS fallback transport (xhr-polling, etc.)
     *     when native WebSocket is unavailable.
     *
     * setAllowedOriginPatterns("*")
     *   → Allows connections from any origin during development.
     *     In production, restrict to your actual domain:
     *     setAllowedOrigins("https://yourdomain.com")
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    // ── JWT Channel Interceptor ────────────────────────────────

    /**
     * Registers the channel interceptor that validates JWTs
     * on incoming WebSocket STOMP frames.
     *
     * WebSocket connections bypass the HTTP filter chain after
     * the initial handshake — JwtAuthFilter does not run for
     * STOMP messages. This interceptor fills that gap.
     */
    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {
        registration.interceptors(new JwtChannelInterceptor());
    }

    // ── Inner Interceptor Class ────────────────────────────────

    /**
     * STOMP channel interceptor that authenticates WebSocket
     * connections using the JWT in the STOMP CONNECT frame.
     *
     * STOMP protocol sends a CONNECT frame when the client
     * first establishes the session. This is where we check
     * the JWT. Subsequent SEND and SUBSCRIBE frames inherit
     * the authentication set during CONNECT.
     *
     * Client sends:
     *   CONNECT
     *   Authorization: Bearer eyJhbGci...
     *
     * If valid: sets the Principal on the WebSocket session.
     * If invalid: allows the connection but without a Principal,
     *   so @MessageMapping methods that call getPrincipal()
     *   will see null and reject the message.
     */
    private class JwtChannelInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(
                Message<?> message,
                MessageChannel channel) {

            StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(
                            message, StompHeaderAccessor.class);

            if (accessor == null) {
                return message;
            }

            // Only process CONNECT frames — auth is set once
            // at connection time and inherited by all frames
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                String token = extractToken(accessor);

                if (StringUtils.hasText(token)) {
                    try {
                        String username = jwtUtil.extractUsername(token);

                        if (username != null) {
                            UserDetails userDetails =
                                    userDetailsService
                                            .loadUserByUsername(username);

                            if (jwtUtil.isTokenValid(token, userDetails)) {
                                // Build authentication and set as
                                // the WebSocket session Principal
                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails,
                                                null,
                                                userDetails.getAuthorities()
                                        );

                                SecurityContextHolder.getContext()
                                        .setAuthentication(auth);

                                accessor.setUser(auth);

                                log.debug(
                                        "WebSocket authenticated: user='{}'",
                                        username);
                            }
                        }
                    } catch (Exception e) {
                        log.warn(
                                "WebSocket JWT validation failed: {}",
                                e.getMessage());
                        // Connection continues without authentication.
                        // The controller will reject messages from
                        // unauthenticated principals.
                    }
                } else {
                    log.warn(
                            "WebSocket CONNECT received without JWT token.");
                }
            }

            return message;
        }

        /**
         * Extracts the JWT from the STOMP CONNECT frame headers.
         *
         * Checks two locations (clients may use either):
         *   1. "Authorization" native header → "Bearer eyJ..."
         *   2. First value of the "Authorization" header list
         *
         * @param accessor the STOMP frame header accessor
         * @return the raw JWT string, or null if not found
         */
        private String extractToken(StompHeaderAccessor accessor) {
            // Try native header first
            String authHeader = accessor.getFirstNativeHeader(
                    "Authorization");

            if (StringUtils.hasText(authHeader)
                    && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }

            return null;
        }
    }
}