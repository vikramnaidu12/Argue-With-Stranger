package com.arguewithstranger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a voting business rule is violated.
 *
 * Examples:
 *   - A debater attempting to vote in their own debate
 *   - A user voting twice in the same debate
 *   - Attempting to vote on an OPEN debate (not yet started)
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class VotingException extends RuntimeException {

    private final String errorCode;

    public VotingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // ── Named factory methods ──────────────────────────────────

    public static VotingException debatersCannotVote() {
        return new VotingException(
                "DEBATER_CANNOT_VOTE",
                "Debate participants cannot vote in their own debate."
        );
    }

    public static VotingException alreadyVoted(String side) {
        return new VotingException(
                "ALREADY_VOTED",
                "You have already voted for the " + side + " side in this debate."
        );
    }

    public static VotingException debateNotVotable() {
        return new VotingException(
                "DEBATE_NOT_VOTABLE",
                "Voting is only allowed during ONGOING or CLOSED debates."
        );
    }
}