package com.arguewithstranger.service;

import com.arguewithstranger.dto.request.VoteRequest;
import com.arguewithstranger.dto.response.ApiResponse;
import com.arguewithstranger.dto.response.VoteResultResponse;
import com.arguewithstranger.entity.Debate;
import com.arguewithstranger.entity.User;
import com.arguewithstranger.entity.Vote;
import com.arguewithstranger.exception.ResourceNotFoundException;
import com.arguewithstranger.exception.VotingException;
import com.arguewithstranger.repository.DebateRepository;
import com.arguewithstranger.repository.UserRepository;
import com.arguewithstranger.repository.VoteRepository;
import com.arguewithstranger.util.DebateStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages spectator voting.
 *
 * Business rules enforced:
 *   1. Voting only allowed on ONGOING or CLOSED debates
 *      (not on OPEN debates — no one has debated yet)
 *   2. Debaters cannot vote in their own debate
 *   3. Each user can only vote once per debate
 *      (enforced at application layer AND DB unique constraint)
 *
 * The winner is computed dynamically from live vote counts —
 * never stored as a field, so it always reflects the true tally.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository   voteRepository;
    private final DebateRepository debateRepository;
    private final UserRepository   userRepository;

    // ── Cast Vote ──────────────────────────────────────────────

    /**
     * Records a spectator's vote for FAVOR or AGAINST.
     *
     * Enforcement order:
     *   1. Debate must be ONGOING or CLOSED (not OPEN)
     *   2. Voter must not be a debater in this debate
     *   3. Voter must not have already voted
     *
     * The DB unique constraint on (debate_id, user_id) acts as
     * a final safety net against race conditions — two concurrent
     * requests that both pass check #3 will result in one insert
     * succeeding and one throwing a constraint violation, which
     * Spring wraps as DataIntegrityViolationException.
     *
     * @param debateId the debate to vote in
     * @param request  which side: FAVOR or AGAINST
     * @param voter    the authenticated user casting the vote
     * @return ApiResponse confirming the vote
     */
    @Transactional
    public ApiResponse castVote(
            Long debateId,
            VoteRequest request,
            UserDetails voter) {

        Debate debate = findDebateById(debateId);
        User   user   = loadUser(voter.getUsername());

        // Rule 1: Debate must be ONGOING or CLOSED
        if (debate.getStatus() == DebateStatus.OPEN) {
            throw VotingException.debateNotVotable();
        }

        // Rule 2: Debaters cannot vote in their own debate
        if (debate.isDebater(user)) {
            throw VotingException.debatersCannotVote();
        }

        // Rule 3: Each user can only vote once
        if (voteRepository.existsByDebateIdAndUserId(
                debateId, user.getId())) {

            // Fetch existing vote to tell the user which side they voted for
            voteRepository.findByDebateIdAndUserId(debateId, user.getId())
                    .ifPresent(existingVote -> {
                        throw VotingException.alreadyVoted(
                                existingVote.getSelectedSide());
                    });
        }

        String side = request.getSelectedSide().toUpperCase();

        Vote vote = Vote.builder()
                .debate(debate)
                .user(user)
                .selectedSide(side)
                .build();

        voteRepository.save(vote);

        log.info("Vote cast: debateId={}, userId={}, side={}",
                debateId, user.getId(), side);

        return ApiResponse.ok(
                "Your vote for " + side + " has been recorded.");
    }

    // ── Get Results ────────────────────────────────────────────

    /**
     * Returns the current vote tally for a debate.
     *
     * The winner is computed from live counts — not cached.
     * This means calling this endpoint always reflects the
     * true current state, even as votes arrive in real time.
     *
     * Winner logic:
     *   favorCount > againstCount → "FAVOR"
     *   againstCount > favorCount → "AGAINST"
     *   equal                     → "TIE"
     *   no votes yet              → "TIE"
     *
     * @param debateId the debate whose results to fetch
     * @return VoteResultResponse with counts and winner
     */
    @Transactional(readOnly = true)
    public VoteResultResponse getResults(Long debateId) {

        // Verify debate exists
        findDebateById(debateId);

        long favorCount   = voteRepository
                .countByDebateIdAndSelectedSide(debateId, "FAVOR");
        long againstCount = voteRepository
                .countByDebateIdAndSelectedSide(debateId, "AGAINST");
        long totalVotes   = favorCount + againstCount;

        String winner = computeWinner(favorCount, againstCount);

        log.debug("Results for debateId={}: favor={}, against={}, winner={}",
                debateId, favorCount, againstCount, winner);

        return VoteResultResponse.builder()
                .debateId(debateId)
                .favorCount(favorCount)
                .againstCount(againstCount)
                .totalVotes(totalVotes)
                .winner(winner)
                .build();
    }

    // ── Private Helpers ────────────────────────────────────────

    /**
     * Computes the winner string from vote counts.
     *
     * @param favorCount   votes for FAVOR
     * @param againstCount votes for AGAINST
     * @return "FAVOR", "AGAINST", or "TIE"
     */
    private String computeWinner(long favorCount, long againstCount) {
        if (favorCount > againstCount)   return "FAVOR";
        if (againstCount > favorCount)   return "AGAINST";
        return "TIE";
    }

    /**
     * Loads a Debate by ID or throws ResourceNotFoundException.
     */
    private Debate findDebateById(Long debateId) {
        return debateRepository.findById(debateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Debate", "id", debateId));
    }

    /**
     * Loads a User by username or throws ResourceNotFoundException.
     */
    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "username", username));
    }
}