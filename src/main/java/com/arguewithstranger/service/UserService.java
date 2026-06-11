package com.arguewithstranger.service;

import com.arguewithstranger.dto.response.UserProfileResponse;
import com.arguewithstranger.entity.User;
import com.arguewithstranger.exception.ResourceNotFoundException;
import com.arguewithstranger.repository.DebateRepository;
import com.arguewithstranger.repository.MessageRepository;
import com.arguewithstranger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles user profile and leaderboard operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository    userRepository;
    private final DebateRepository  debateRepository;
    private final MessageRepository messageRepository;

    // ── Profile ────────────────────────────────────────────────

    /**
     * Returns the profile of the currently authenticated user
     * including debate and message activity counts.
     *
     * @param currentUser the authenticated principal
     * @return UserProfileResponse with stats
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(UserDetails currentUser) {
        User user = loadUser(currentUser.getUsername());
        return buildProfile(user);
    }

    /**
     * Returns a public profile by username.
     *
     * @param username the user to look up
     * @return UserProfileResponse
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUsername(String username) {
        User user = loadUser(username);
        return buildProfile(user);
    }

    // ── Leaderboard ────────────────────────────────────────────

    /**
     * Returns the top 10 most active debaters ranked by
     * number of debates participated in.
     *
     * For a production leaderboard, this would be a dedicated
     * query with a materialized view or cached result.
     * For this scope, fetching all users and sorting in-memory
     * is acceptable given the expected user count.
     *
     * @return list of top 10 profiles ordered by debate count
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getLeaderboard() {
        return userRepository.findAll()
                .stream()
                .map(this::buildProfile)
                .sorted((a, b) ->
                        Long.compare(b.getDebateCount(), a.getDebateCount()))
                .limit(10)
                .toList();
    }

    // ── Private Helpers ────────────────────────────────────────

    /**
     * Builds a UserProfileResponse with live activity stats.
     */
    private UserProfileResponse buildProfile(User user) {
        long debateCount  = debateRepository
                .countDebatesByParticipant(user);
        long messageCount = messageRepository
                .countBySenderId(user.getId());

        UserProfileResponse profile =
                UserProfileResponse.fromEntity(user);
        profile.setDebateCount(debateCount);
        profile.setMessageCount(messageCount);

        return profile;
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