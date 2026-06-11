package com.arguewithstranger.config;

import com.arguewithstranger.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Master Spring Security configuration for Spring Boot 4.0.x
 * with Spring Security 7.x.
 *
 * Key changes in Security 7.x vs 6.x:
 *  - DaoAuthenticationProvider constructor requires both
 *    UserDetailsService AND PasswordEncoder — no setters available
 *  - PasswordEncoder declared as static @Bean to prevent
 *    circular dependency during context initialization
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter      jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    // ── Security Filter Chain ──────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configure(http))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // Public — no JWT required
                        .requestMatchers("/auth/**").permitAll()

                        // Static frontend files
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/register.html",
                                "/profile.html",
                                "/debate.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // WebSocket handshake endpoint
                        .requestMatchers("/chat/**").permitAll()

                        // Admin-only: creating debate topics
                        .requestMatchers(HttpMethod.POST, "/debates")
                        .hasRole("ADMIN")

                        // Everything else requires a valid JWT
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // ── Password Encoder ───────────────────────────────────────

    /**
     * Declared static so Spring instantiates it before any
     * instance beans — prevents circular dependency between
     * SecurityConfig → PasswordEncoder → SecurityConfig.
     */
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── Authentication Provider ────────────────────────────────

    /**
     * Spring Security 7.x API — DaoAuthenticationProvider
     * constructor requires BOTH UserDetailsService and
     * PasswordEncoder passed at construction time.
     * No setters exist in this version.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ── Authentication Manager ─────────────────────────────────

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── JSON Error Responses ───────────────────────────────────

    /**
     * Builds a plain JSON error string without any Jackson
     * dependency — keeps SecurityConfig self-contained and
     * free of bean initialization ordering issues.
     */
    private String buildErrorJson(
            int    status,
            String error,
            String errorCode,
            String message,
            String path) {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return """
            {
              "timestamp": "%s",
              "status": %d,
              "error": "%s",
              "errorCode": "%s",
              "message": "%s",
              "path": "%s"
            }
            """.formatted(timestamp, status, error, errorCode, message, path);
    }

    /**
     * Writes the JSON string to the response with correct
     * status code, content type, and encoding.
     */
    private void writeJsonResponse(
            HttpServletResponse response,
            int    status,
            String json) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    /**
     * JSON 401 response for unauthenticated requests.
     * AuthenticationEntryPoint handles missing/invalid auth.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest  request,
                HttpServletResponse response,
                AuthenticationException authException) ->
                writeJsonResponse(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        buildErrorJson(
                                401,
                                "Unauthorized",
                                "AUTHENTICATION_REQUIRED",
                                "Authentication is required to access this resource.",
                                request.getRequestURI()
                        )
                );
    }

    /**
     * JSON 403 response for authenticated but unauthorized requests.
     * AccessDeniedHandler handles insufficient permissions.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest  request,
                HttpServletResponse response,
                AccessDeniedException accessDeniedException) ->
                writeJsonResponse(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        buildErrorJson(
                                403,
                                "Forbidden",
                                "ACCESS_DENIED",
                                "You do not have permission to access this resource.",
                                request.getRequestURI()
                        )
                );
    }
}