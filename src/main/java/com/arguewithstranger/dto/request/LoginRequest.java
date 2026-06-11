package com.arguewithstranger.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Payload for POST /auth/login
 *
 * Intentionally minimal — we only need the credentials.
 * The service layer loads the full User from the database.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}