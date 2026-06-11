package com.arguewithstranger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Payload for POST /debates/{id}/join
 *
 * The client must explicitly declare which side they want to join.
 * We validate that the value is exactly "FAVOR" or "AGAINST" —
 * no other string is accepted. This is enforced at the DTO level
 * before it reaches the service, providing an early rejection
 * with a clear error message.
 */
@Data
public class JoinDebateRequest {

    @NotBlank(message = "Side is required")
    @Pattern(
            regexp = "^(FAVOR|AGAINST)$",
            message = "Side must be either FAVOR or AGAINST"
    )
    private String side;
}