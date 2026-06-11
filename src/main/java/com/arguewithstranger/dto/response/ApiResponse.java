package com.arguewithstranger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper used for simple success/error messages.
 *
 * Used when an endpoint doesn't return domain data — for example:
 *   POST /debates/{id}/end  → { "success": true, "message": "Debate ended" }
 *   POST /debates/{id}/vote → { "success": true, "message": "Vote cast for FAVOR" }
 *
 * Keeps all API responses consistently structured — the frontend
 * always has a "message" field to display to the user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {

    private boolean success;
    private String message;

    public static ApiResponse ok(String message) {
        return new ApiResponse(true, message);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, message);
    }
}