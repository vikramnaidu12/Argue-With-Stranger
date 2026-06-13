package com.arguewithstranger.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for POST /auth/register
 *
 * @NotBlank   — rejects null, empty string, and whitespace-only strings
 * @Size       — enforces length bounds before hitting the database
 * @Email      — validates email format via Jakarta regex
 * @Pattern    — username: alphanumeric + underscore only, no spaces
 *
 * Validation errors are caught by GlobalExceptionHandler and returned
 * as a structured 400 response — the controller stays clean.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username may only contain letters, numbers, and underscores"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@gmail\\.com$",
            message = "Enter valid Gmail"
    )
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}