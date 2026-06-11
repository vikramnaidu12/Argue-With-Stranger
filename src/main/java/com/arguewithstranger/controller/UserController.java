package com.arguewithstranger.controller;

import com.arguewithstranger.dto.response.UserProfileResponse;
import com.arguewithstranger.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Handles user profile and leaderboard endpoints.
 *
 * GET /users/profile          — current user's own profile
 * GET /users/{username}       — public profile by username
 * GET /users/leaderboard      — top 10 most active debaters
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /users/profile
     *
     * Returns the authenticated user's own profile including
     * debate count and message count statistics.
     *
     * @param currentUser the authenticated principal
     * @return UserProfileResponse with activity stats
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails currentUser) {

        return ResponseEntity.ok(
                userService.getMyProfile(currentUser));
    }

    /**
     * GET /users/leaderboard
     *
     * Returns the top 10 most active debaters ranked by
     * number of debates participated in.
     *
     * @return list of top 10 UserProfileResponse objects
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserProfileResponse>> getLeaderboard() {
        return ResponseEntity.ok(userService.getLeaderboard());
    }

    /**
     * GET /users/{username}
     *
     * Returns a public profile for any user by username.
     * Used to view other debaters' profiles and stats.
     *
     * @param username the username to look up
     * @return UserProfileResponse
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable String username) {

        return ResponseEntity.ok(
                userService.getProfileByUsername(username));
    }
}