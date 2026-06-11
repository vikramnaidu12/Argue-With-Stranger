package com.arguewithstranger.dto.response;

import com.arguewithstranger.entity.Debate;
import com.arguewithstranger.util.DebateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response for GET /debates, GET /debates/{id}, POST /debates,
 * POST /debates/{id}/join, and POST /debates/{id}/end
 *
 * Flattens the Debate entity into a clean JSON structure.
 * User objects are represented as simple UserSummary nested
 * records rather than full User objects — no password hash,
 * no authorities list, no circular references.
 *
 * The static factory method fromEntity() keeps the mapping
 * logic here in the DTO rather than in the service, following
 * the principle that each class owns its own transformation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateResponse {

    private Long id;
    private String topic;
    private String description;
    private DebateStatus status;

    private UserSummary favorUser;
    private UserSummary againstUser;
    private UserSummary createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime endedAt;

    // Derived convenience fields for the frontend
    private boolean full;
    private long favorVoteCount;
    private long againstVoteCount;

    /**
     * Lightweight user representation embedded in debate responses.
     * Only exposes what the UI needs — id and username.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String username;
    }

    /**
     * Factory method: converts a Debate entity into a DebateResponse.
     * Vote counts default to 0 here — they are populated separately
     * by DebateService when building the full response.
     *
     * @param debate the entity to convert
     * @return a DebateResponse safe for JSON serialization
     */
    public static DebateResponse fromEntity(Debate debate) {
        return DebateResponse.builder()
                .id(debate.getId())
                .topic(debate.getTopic())
                .description(debate.getDescription())
                .status(debate.getStatus())
                .favorUser(toSummary(debate.getFavorUser()))
                .againstUser(toSummary(debate.getAgainstUser()))
                .createdBy(toSummary(debate.getCreatedBy()))
                .createdAt(debate.getCreatedAt())
                .endedAt(debate.getEndedAt())
                .full(debate.isFull())
                .favorVoteCount(0L)
                .againstVoteCount(0L)
                .build();
    }

    /**
     * Null-safe conversion of a User entity to a UserSummary.
     * Returns null if the user is null (unfilled debater slot).
     * Jackson will omit this field from JSON due to
     * spring.jackson.default-property-inclusion=non_null
     *
     * @param user the user entity, may be null
     * @return UserSummary or null
     */
    private static UserSummary toSummary(
            com.arguewithstranger.entity.User user) {
        if (user == null) return null;
        return UserSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
    }
}