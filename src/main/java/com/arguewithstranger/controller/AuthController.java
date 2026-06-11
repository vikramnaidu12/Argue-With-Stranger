package com.arguewithstranger.controller;

import com.arguewithstranger.dto.request.LoginRequest;
import com.arguewithstranger.dto.request.RegisterRequest;
import com.arguewithstranger.dto.response.AuthResponse;
import com.arguewithstranger.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles user registration and login.
 *
 * Both endpoints are public — no JWT required.
 * Permitted in SecurityConfig via .requestMatchers("/auth/**").permitAll()
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/register
     *
     * Registers a new user and returns a JWT immediately.
     * The user is logged in as soon as they register —
     * no separate login step required.
     *
     * @param request username, email, password
     * @return 201 CREATED with AuthResponse (JWT + user info)
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Registration request for username: {}",
                request.getUsername());

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /auth/login
     *
     * Authenticates a user and returns a JWT.
     *
     * @param request username and password
     * @return 200 OK with AuthResponse (JWT + user info)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request for username: {}",
                request.getUsername());

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}