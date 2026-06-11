package com.arguewithstranger.service;

import com.arguewithstranger.dto.request.CreateDebateRequest;
import com.arguewithstranger.dto.request.JoinDebateRequest;
import com.arguewithstranger.dto.response.DebateResponse;
import com.arguewithstranger.entity.Debate;
import com.arguewithstranger.entity.User;
import com.arguewithstranger.exception.DebateException;
import com.arguewithstranger.exception.ResourceNotFoundException;
import com.arguewithstranger.repository.DebateRepository;
import com.arguewithstranger.repository.UserRepository;
import com.arguewithstranger.repository.VoteRepository;
import com.arguewithstranger.util.DebateStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages the full debate lifecycle:
 *   OPEN → ONGOING → CLOSED
 *
 * Business rules enforced here:
 *   - Only one user per side (FAVOR / AGAINST)
 *   - A user cannot join both sides of the same debate
 *   - Debate transitions to ONGOING when both sides are filled
 *   - Only a debater or admin can end a debate
 *   - Ended debates are preserved permanently (never deleted)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DebateService {

    private final DebateRepository debateRepository;
    private final UserRepository   userRepository;
    private final VoteRepository   voteRepository;

    // ── Create ─────────────────────────────────────────────────

    /**
     * Creates a new debate topic in OPEN status.
     * The creator is recorded but does not automatically
     * join either side — they may join separately or moderate.
     *
     * @param request topic and description
     * @param creator the authenticated user creating the topic
     * @return the created debate as a DebateResponse
     */
    @Transactional
    public DebateResponse createDebate(
            CreateDebateRequest request,
            UserDetails creator) {

        User creatorUser = loadUser(creator.getUsername());

        Debate debate = Debate.builder()
                .topic(request.getTopic())
                .description(request.getDescription())
                .createdBy(creatorUser)
                .status(DebateStatus.OPEN)
                .build();

        Debate saved = debateRepository.save(debate);

        log.info("Debate created: id={}, topic='{}', createdBy={}",
                saved.getId(), saved.getTopic(), creatorUser.getUsername());

        return buildResponse(saved);
    }

    // ── Read ───────────────────────────────────────────────────

    /**
     * Returns a paginated list of all debates ordered by
     * creation date descending (newest first).
     *
     * @param page zero-based page number
     * @param size number of debates per page
     * @return page of DebateResponse objects
     */
    @Transactional(readOnly = true)
    public Page<DebateResponse> getAllDebates(int page, int size) {
        Pageable pageable = PageRequest.of(
                page, size, Sort.by("createdAt").descending());

        return debateRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::buildResponse);
    }

    /**
     * Returns debates filtered by status.
     * Used by the frontend to show only OPEN debates
     * on the "Join a debate" page.
     *
     * @param status OPEN, ONGOING, or CLOSED
     * @param page   zero-based page number
     * @param size   debates per page
     * @return page of DebateResponse objects
     */
    @Transactional(readOnly = true)
    public Page<DebateResponse> getDebatesByStatus(
            DebateStatus status, int page, int size) {

        Pageable pageable = PageRequest.of(
                page, size, Sort.by("createdAt").descending());

        return debateRepository
                .findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::buildResponse);
    }

    /**
     * Returns a single debate by ID with full details
     * including vote counts.
     *
     * @param debateId the debate ID
     * @return DebateResponse with participant and vote info
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public DebateResponse getDebateById(Long debateId) {
        Debate debate = findDebateById(debateId);
        return buildResponse(debate);
    }

    /**
     * Searches debates by keyword across topic and description.
     *
     * @param keyword search term
     * @param page    zero-based page number
     * @param size    debates per page
     * @return matching debates
     */
    @Transactional(readOnly = true)
    public Page<DebateResponse> searchDebates(
            String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(
                page, size, Sort.by("createdAt").descending());

        return debateRepository
                .searchByKeyword(keyword, pageable)
                .map(this::buildResponse);
    }

    /**
     * Returns all debates a user has participated in as a debater.
     * Used for the user profile "My Debates" section.
     *
     * @param username the user whose debates to fetch
     * @return list of DebateResponse objects
     */
    @Transactional(readOnly = true)
    public List<DebateResponse> getDebatesByParticipant(String username) {
        User user = loadUser(username);
        return debateRepository
                .findDebatesByParticipant(user)
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    // ── Join ───────────────────────────────────────────────────

    /**
     * Joins the authenticated user to one side of a debate.
     *
     * Rules enforced:
     *   1. Debate must be OPEN (not ONGOING or CLOSED)
     *   2. User cannot already be participating in this debate
     *   3. The requested side must not already be taken
     *   4. If both sides are now filled → transition to ONGOING
     *
     * @param debateId the debate to join
     * @param request  which side: FAVOR or AGAINST
     * @param joiner   the authenticated user
     * @return updated DebateResponse
     */
    @Transactional
    public DebateResponse joinDebate(
            Long debateId,
            JoinDebateRequest request,
            UserDetails joiner) {

        Debate debate = findDebateById(debateId);
        User   user   = loadUser(joiner.getUsername());

        // Rule 1: Debate must be OPEN
        if (debate.getStatus() == DebateStatus.CLOSED) {
            throw DebateException.alreadyClosed();
        }
        if (debate.getStatus() == DebateStatus.ONGOING) {
            throw DebateException.notOngoing();
        }

        // Rule 2: User cannot join a debate they are already in
        if (debate.isDebater(user)) {
            throw DebateException.alreadyParticipating();
        }

        String side = request.getSide().toUpperCase();

        // Rule 3: Requested side must be available
        if ("FAVOR".equals(side)) {
            if (debate.getFavorUser() != null) {
                throw DebateException.sideAlreadyTaken("FAVOR");
            }
            debate.setFavorUser(user);
        } else {
            if (debate.getAgainstUser() != null) {
                throw DebateException.sideAlreadyTaken("AGAINST");
            }
            debate.setAgainstUser(user);
        }

        // Rule 4: Transition to ONGOING if both sides are now filled
        if (debate.isFull()) {
            debate.setStatus(DebateStatus.ONGOING);
            log.info("Debate id={} transitioned to ONGOING. " +
                            "Favor={}, Against={}",
                    debate.getId(),
                    debate.getFavorUser().getUsername(),
                    debate.getAgainstUser().getUsername());
        }

        Debate saved = debateRepository.save(debate);

        log.info("User '{}' joined debate id={} on the {} side",
                user.getUsername(), debateId, side);

        return buildResponse(saved);
    }

    // ── End ────────────────────────────────────────────────────

    /**
     * Ends a debate, transitioning it to CLOSED status.
     *
     * Rules enforced:
     *   1. Debate must be ONGOING (cannot end an OPEN or
     *      already CLOSED debate)
     *   2. Only a debater (FAVOR or AGAINST) or ADMIN can end it
     *
     * Chat history is preserved permanently — only the status
     * and endedAt timestamp are updated.
     *
     * @param debateId the debate to end
     * @param requester the authenticated user requesting the end
     * @return updated DebateResponse with CLOSED status
     */
    @Transactional
    public DebateResponse endDebate(Long debateId, UserDetails requester) {

        Debate debate = findDebateById(debateId);
        User   user   = loadUser(requester.getUsername());

        // Rule 1: Must be ONGOING to end
        if (debate.getStatus() != DebateStatus.ONGOING) {
            throw DebateException.notOngoing();
        }

        // Rule 2: Only debaters or admins can end
        boolean isDebater = debate.isDebater(user);
        boolean isAdmin   = user.getRole().name().equals("ADMIN");

        if (!isDebater && !isAdmin) {
            throw DebateException.notAuthorizedToEnd();
        }

        debate.setStatus(DebateStatus.CLOSED);
        debate.setEndedAt(LocalDateTime.now());

        Debate saved = debateRepository.save(debate);

        log.info("Debate id={} ended by user '{}'",
                debateId, user.getUsername());

        return buildResponse(saved);
    }

    // ── Private Helpers ────────────────────────────────────────

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

    /**
     * Converts a Debate entity to a DebateResponse DTO,
     * populating vote counts from the VoteRepository.
     *
     * Vote counts are always fetched fresh — no caching —
     * so the results page always reflects the live tally.
     */
    private DebateResponse buildResponse(Debate debate) {
        DebateResponse response = DebateResponse.fromEntity(debate);

        // Populate live vote counts
        long favorCount   = voteRepository
                .countByDebateIdAndSelectedSide(debate.getId(), "FAVOR");
        long againstCount = voteRepository
                .countByDebateIdAndSelectedSide(debate.getId(), "AGAINST");

        response.setFavorVoteCount(favorCount);
        response.setAgainstVoteCount(againstCount);

        return response;
    }
}