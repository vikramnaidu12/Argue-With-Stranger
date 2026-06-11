package com.arguewithstranger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /auth/register and POST /auth/login
 *
 * Returns the JWT token and enough user information for the
 * frontend to personalise the UI immediately after login
 * without needing a separate /users/me call.
 *
 * The token is a signed JWT — the frontend stores it in
 * localStorage and attaches it to every subsequent request
 * as: Authorization: Bearer <token>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private String role;

    // Convenience constructor used by AuthService
    public AuthResponse(String token, Long userId,
                        String username, String email, String role) {
        this.token     = token;
        this.tokenType = "Bearer";
        this.userId    = userId;
        this.username  = username;
        this.email     = email;
        this.role      = role;
    }
}