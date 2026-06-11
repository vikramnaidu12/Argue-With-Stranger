package com.arguewithstranger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for GET /debates/{id}/result
 *
 * winner is "FAVOR", "AGAINST", or "TIE".
 * Computed by VoteService based on vote counts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteResultResponse {

    private Long debateId;
    private long favorCount;
    private long againstCount;
    private long totalVotes;
    private String winner;
}