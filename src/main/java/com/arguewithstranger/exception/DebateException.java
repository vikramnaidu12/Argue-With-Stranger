package com.arguewithstranger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a debate-related business rule is violated.
 *
 * Examples:
 *   - Joining a debate that is already full (both sides taken)
 *   - Sending a message to a CLOSED debate
 *   - Ending a debate that is not ONGOING
 *   - Joining a side that is already occupied
 *   - A debater trying to join the same debate twice
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DebateException extends RuntimeException {

    private final String errorCode;

    public DebateException(String message) {
        super(message);
        this.errorCode = "DEBATE_ERROR";
    }

    public DebateException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // ── Named factory methods ──────────────────────────────────
    // These read like documentation at the call site:
    // throw DebateException.alreadyFull()
    // is cleaner than
    // throw new DebateException("DEBATE_FULL", "Debate is already full")

    public static DebateException alreadyFull() {
        return new DebateException(
                "DEBATE_FULL",
                "This debate is already full. You may join as a spectator."
        );
    }

    public static DebateException sideAlreadyTaken(String side) {
        return new DebateException(
                "SIDE_TAKEN",
                "The " + side + " side is already taken by another user."
        );
    }

    public static DebateException alreadyParticipating() {
        return new DebateException(
                "ALREADY_PARTICIPATING",
                "You are already participating in this debate."
        );
    }

    public static DebateException notOngoing() {
        return new DebateException(
                "DEBATE_NOT_ONGOING",
                "This action requires the debate to be in ONGOING status."
        );
    }

    public static DebateException alreadyClosed() {
        return new DebateException(
                "DEBATE_CLOSED",
                "This debate has ended. No further actions are allowed."
        );
    }

    public static DebateException notAuthorizedToEnd() {
        return new DebateException(
                "NOT_AUTHORIZED_TO_END",
                "Only a debate participant or admin can end this debate."
        );
    }

    public static DebateException messagingNotAllowed() {
        return new DebateException(
                "MESSAGING_NOT_ALLOWED",
                "Messages can only be sent during an ONGOING debate."
        );
    }

    public static DebateException notADebater() {
        return new DebateException(
                "NOT_A_DEBATER",
                "Only the debate participants can send messages."
        );
    }
}