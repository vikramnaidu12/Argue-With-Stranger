package com.arguewithstranger.controller;

import com.arguewithstranger.dto.request.SendMessageRequest;
import com.arguewithstranger.dto.response.MessageResponse;
import com.arguewithstranger.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Handles real-time WebSocket messages over STOMP.
 *
 * Flow for every message:
 *   1. Client sends STOMP frame to /app/sendMessage
 *   2. This method receives it
 *   3. MessageService validates rules and persists to MySQL
 *   4. MessageResponse is broadcast to /topic/debate/{debateId}
 *   5. All subscribers (debaters + spectators) receive it instantly
 *
 * The Principal is set by JwtChannelInterceptor during STOMP CONNECT.
 * If the connection had no valid JWT, Principal is null and
 * the message is rejected before reaching MessageService.
 *
 * Note: @Controller not @RestController — WebSocket controllers
 * do not return HTTP responses, they use SimpMessagingTemplate
 * to push messages to broker destinations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService       messageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Receives a message from a debater and broadcasts it
     * to all subscribers of the debate's topic channel.
     *
     * Client sends to:   /app/sendMessage
     * Server publishes to: /topic/debate/{debateId}
     *
     * The Principal is injected from the STOMP session —
     * it was set by JwtChannelInterceptor on CONNECT.
     *
     * @param request   debateId and message content
     * @param principal the authenticated WebSocket principal
     */
    @MessageMapping("/sendMessage")
    public void sendMessage(
            @Payload SendMessageRequest request,
            Principal principal) {

        // Reject unauthenticated WebSocket connections
        if (principal == null) {
            log.warn("Rejected WebSocket message — no authenticated principal");
            return;
        }

        String username = principal.getName();

        log.debug("WebSocket message from '{}' to debate id={}",
                username, request.getDebateId());

        // Persist and get the broadcast-ready response
        MessageResponse response =
                messageService.sendMessage(request, username);

        // Broadcast to all subscribers of this debate's channel
        String destination = "/topic/debate/" + request.getDebateId();
        messagingTemplate.convertAndSend(destination, response);

        log.debug("Message broadcast to {}: messageId={}",
                destination, response.getId());
    }

    /**
     * Sends an error message back to a specific user's private queue.
     * Used to notify a debater that their message was rejected
     * (e.g. debate was just closed while they were typing).
     *
     * Destination: /user/{username}/queue/errors
     * Only the user whose session has this username receives it.
     *
     * @param username    the recipient's username
     * @param errorMessage the error to deliver
     */
    private void sendErrorToUser(String username, String errorMessage) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/errors",
                errorMessage
        );
    }
}