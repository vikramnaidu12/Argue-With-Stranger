package com.arguewithstranger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cross-Origin Resource Sharing (CORS) configuration.
 *
 * During development, the frontend HTML files are often served
 * by a live server (VS Code Live Server, IntelliJ built-in, or
 * a separate Node server) on a different port than the Spring Boot
 * backend. The browser treats different ports as different origins
 * and blocks requests unless the server explicitly allows them.
 *
 * Allowed origins in development: * (any origin)
 * In production: restrict to your actual domain only.
 *
 * This bean is picked up by SecurityConfig via cors().configure(http)
 * and also applies to regular Spring MVC requests via @CrossOrigin
 * or the global CORS filter.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ── Allowed origins ────────────────────────────────────
        // Development: allow all origins.
        // Production: replace with ["https://yourdomain.com"]
        config.setAllowedOriginPatterns(List.of("*"));

        // ── Allowed HTTP methods ───────────────────────────────
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ── Allowed headers ────────────────────────────────────
        // Authorization — for the JWT Bearer token
        // Content-Type  — for JSON request bodies
        // Accept        — for content negotiation
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // ── Exposed headers ────────────────────────────────────
        // Headers the browser JavaScript can read from responses
        config.setExposedHeaders(List.of("Authorization"));

        // ── Allow credentials ──────────────────────────────────
        // Required for SockJS WebSocket connections to include
        // cookies and Authorization headers in cross-origin requests.
        config.setAllowCredentials(true);

        // ── Preflight cache ────────────────────────────────────
        // Browser caches the preflight OPTIONS response for 1 hour,
        // reducing the number of preflight requests.
        config.setMaxAge(3600L);

        // Apply this configuration to all routes
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}