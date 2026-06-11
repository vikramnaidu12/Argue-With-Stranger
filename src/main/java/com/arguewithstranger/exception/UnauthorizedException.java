package com.arguewithstranger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an authenticated user lacks permission to
 * perform the requested action. This is HTTP 403 Forbidden,
 * not 401 Unauthorized — the user is known, but not allowed.
 *
 * Examples:
 *   - A regular USER trying to create a debate topic
 *     (if that is restricted to ADMINs)
 *   - A spectator trying to end a debate
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public static UnauthorizedException accessDenied() {
        return new UnauthorizedException(
                "You do not have permission to perform this action."
        );
    }
}