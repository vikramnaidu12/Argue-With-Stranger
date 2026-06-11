package com.arguewithstranger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown for authentication and registration failures.
 *
 * Examples:
 *   - Invalid username or password on login
 *   - Registering with a username that is already taken
 *   - Registering with an email that is already registered
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public AuthException(String errorCode,
                         String message,
                         HttpStatus httpStatus) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
    }

    public String     getErrorCode()  { return errorCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }

    // ── Named factory methods ──────────────────────────────────

    public static AuthException invalidCredentials() {
        return new AuthException(
                "INVALID_CREDENTIALS",
                "Invalid username or password.",
                HttpStatus.UNAUTHORIZED
        );
    }

    public static AuthException usernameTaken(String username) {
        return new AuthException(
                "USERNAME_TAKEN",
                "Username '" + username + "' is already taken.",
                HttpStatus.CONFLICT
        );
    }

    public static AuthException emailTaken(String email) {
        return new AuthException(
                "EMAIL_TAKEN",
                "An account with email '" + email + "' already exists.",
                HttpStatus.CONFLICT
        );
    }
}