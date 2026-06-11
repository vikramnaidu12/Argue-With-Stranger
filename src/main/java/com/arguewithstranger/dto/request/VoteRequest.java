package com.arguewithstranger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Payload for POST /debates/{id}/vote
 *
 * Same side validation pattern as JoinDebateRequest.
 * The service layer performs additional checks:
 *   - User must not be a debater in this debate
 *   - User must not have already voted
 *   - Debate must be ONGOING or CLOSED (no voting on OPEN debates)
 */
@Data
public class VoteRequest {

    @NotBlank(message = "Selected side is required")
    @Pattern(
            regexp = "^(FAVOR|AGAINST)$",
            message = "Selected side must be either FAVOR or AGAINST"
    )
    private String selectedSide;
}