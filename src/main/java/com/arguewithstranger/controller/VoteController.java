package com.arguewithstranger.controller;

import com.arguewithstranger.dto.request.VoteRequest;
import com.arguewithstranger.dto.response.ApiResponse;
import com.arguewithstranger.dto.response.VoteResultResponse;
import com.arguewithstranger.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Handles spectator voting.
 *
 * POST /debates/{id}/vote   — cast a vote (FAVOR or AGAINST)
 * GET  /debates/{id}/result — get current vote tally
 */
@Slf4j
@RestController
@RequestMapping("/debates")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    /**
     * POST /debates/{id}/vote
     *
     * Records a spectator's vote.
     *
     * Business rules enforced in VoteService:
     *   - Debate must be ONGOING or CLOSED (not OPEN)
     *   - Voter must not be a debater
     *   - Each user can vote only once per debate
     *
     * @param id      the debate ID
     * @param request which side: FAVOR or AGAINST
     * @param voter   the authenticated spectator
     * @return ApiResponse confirming the vote
     */
    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse> castVote(
            @PathVariable Long id,
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal UserDetails voter) {

        log.info("Vote cast: debateId={}, user={}, side={}",
                id, voter.getUsername(), request.getSelectedSide());

        return ResponseEntity.ok(
                voteService.castVote(id, request, voter));
    }

    /**
     * GET /debates/{id}/result
     *
     * Returns the current vote tally with favor count,
     * against count, total votes, and computed winner.
     *
     * Available for all debate statuses — results are
     * readable during and after the debate.
     *
     * @param id the debate ID
     * @return VoteResultResponse with live counts
     */
    @GetMapping("/{id}/result")
    public ResponseEntity<VoteResultResponse> getResult(
            @PathVariable Long id) {

        return ResponseEntity.ok(voteService.getResults(id));
    }
}