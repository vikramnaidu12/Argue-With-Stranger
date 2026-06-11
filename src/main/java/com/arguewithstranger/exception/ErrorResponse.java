package com.arguewithstranger.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error response returned for every failure.
 *
 * Every error the API returns — validation, auth, business rule,
 * server error — is wrapped in this structure. The frontend
 * always knows where to find the message and the status code.
 *
 * {
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "errorCode": "DEBATE_FULL",
 *   "message": "This debate is already full.",
 *   "path": "/debates/5/join",
 *   "validationErrors": {          ← only present for validation failures
 *     "side": "Side must be either FAVOR or AGAINST"
 *   }
 * }
 *
 * @JsonInclude(NON_NULL) — validationErrors is omitted when null,
 * keeping the response clean for non-validation errors.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String errorCode;
    private String message;
    private String path;

    // Only populated for @Valid constraint violations
    private Map<String, String> validationErrors;
}