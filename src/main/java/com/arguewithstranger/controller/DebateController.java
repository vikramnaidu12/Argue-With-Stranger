package com.arguewithstranger.controller;

import com.arguewithstranger.dto.request.CreateDebateRequest;
import com.arguewithstranger.dto.request.JoinDebateRequest;
import com.arguewithstranger.dto.response.ApiResponse;
import com.arguewithstranger.dto.response.DebateResponse;
import com.arguewithstranger.service.DebateService;
import com.arguewithstranger.util.DebateStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles all debate lifecycle endpoints.
 *
 * GET  /debates              — paginated list of all debates
 * POST /debates              — create a new debate (ADMIN only)
 * GET  /debates/{id}         — single debate detail
 * POST /debates/{id}/join    — join FAVOR or AGAINST side
 * POST /debates/{id}/end     — end an ONGOING debate
 * GET  /debates/search       — search debates by keyword
 * GET  /debates/status/{s}   — filter debates by status
 * GET  /debates/my           — debates the current user participated in
 */
@Slf4j
@RestController
@RequestMapping("/debates")
@RequiredArgsConstructor
public class DebateController {

    private final DebateService debateService;

    /**
     * GET /debates?page=0&size=10
     *
     * Returns a paginated list of all debates ordered by
     * creation date descending. Used by the home page.
     *
     * @param page zero-based page number (default 0)
     * @param size debates per page (default 10)
     * @return Page of DebateResponse
     */
    @GetMapping
    public ResponseEntity<Page<DebateResponse>> getAllDebates(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                debateService.getAllDebates(page, size));
    }

    /**
     * POST /debates
     *
     * Creates a new debate topic. Restricted to ADMIN role
     * via SecurityConfig (.hasRole("ADMIN")).
     *
     * @param request topic and description
     * @param creator the authenticated admin user
     * @return 201 CREATED with the new DebateResponse
     */
    @PostMapping
    public ResponseEntity<DebateResponse> createDebate(
            @Valid @RequestBody CreateDebateRequest request,
            @AuthenticationPrincipal UserDetails creator) {

        log.info("Create debate request by: {}", creator.getUsername());

        DebateResponse response =
                debateService.createDebate(request, creator);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /debates/{id}
     *
     * Returns full debate details including participant info
     * and live vote counts.
     *
     * @param id the debate ID
     * @return DebateResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<DebateResponse> getDebateById(
            @PathVariable Long id) {

        return ResponseEntity.ok(debateService.getDebateById(id));
    }

    /**
     * POST /debates/{id}/join
     *
     * Joins the authenticated user to one side of the debate.
     * The side (FAVOR or AGAINST) is specified in the request body.
     *
     * Business rules enforced in DebateService:
     *   - Debate must be OPEN
     *   - Side must not already be taken
     *   - User cannot join twice
     *
     * @param id      the debate ID
     * @param request which side to join
     * @param joiner  the authenticated user
     * @return updated DebateResponse
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<DebateResponse> joinDebate(
            @PathVariable Long id,
            @Valid @RequestBody JoinDebateRequest request,
            @AuthenticationPrincipal UserDetails joiner) {

        log.info("User '{}' joining debate id={} side={}",
                joiner.getUsername(), id, request.getSide());

        return ResponseEntity.ok(
                debateService.joinDebate(id, request, joiner));
    }

    /**
     * POST /debates/{id}/end
     *
     * Ends an ONGOING debate, transitioning it to CLOSED.
     * Only the debate participants or an ADMIN can call this.
     *
     * @param id        the debate ID
     * @param requester the authenticated user
     * @return updated DebateResponse with CLOSED status
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<DebateResponse> endDebate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails requester) {

        log.info("End debate request: id={} by={}",
                id, requester.getUsername());

        return ResponseEntity.ok(
                debateService.endDebate(id, requester));
    }

    /**
     * GET /debates/search?keyword=ai&page=0&size=10
     *
     * Searches debates by keyword across topic and description.
     *
     * @param keyword the search term
     * @param page    zero-based page number
     * @param size    results per page
     * @return Page of matching DebateResponse objects
     */
    @GetMapping("/search")
    public ResponseEntity<Page<DebateResponse>> searchDebates(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                debateService.searchDebates(keyword, page, size));
    }

    /**
     * GET /debates/status/{status}?page=0&size=10
     *
     * Returns debates filtered by status.
     * Used by the frontend to show only OPEN or ONGOING debates.
     *
     * @param status OPEN, ONGOING, or CLOSED
     * @param page   zero-based page number
     * @param size   results per page
     * @return Page of DebateResponse objects
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<DebateResponse>> getDebatesByStatus(
            @PathVariable DebateStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                debateService.getDebatesByStatus(status, page, size));
    }

    /**
     * GET /debates/my
     *
     * Returns all debates the authenticated user has
     * participated in as a debater (FAVOR or AGAINST).
     * Used for the "My Debates" section of the user profile.
     *
     * @param currentUser the authenticated user
     * @return list of DebateResponse objects
     */
    @GetMapping("/my")
    public ResponseEntity<List<DebateResponse>> getMyDebates(
            @AuthenticationPrincipal UserDetails currentUser) {

        return ResponseEntity.ok(
                debateService.getDebatesByParticipant(
                        currentUser.getUsername()));
    }
}