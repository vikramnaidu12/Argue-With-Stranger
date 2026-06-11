package com.arguewithstranger.dto.response;

import com.arguewithstranger.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response for GET /users/profile
 *
 * Safe representation of a User — password is never included.
 * debateCount and messageCount are populated by the service
 * using the repository count methods we defined in Step 5.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String role;
    private LocalDateTime createdAt;

    // Activity statistics
    private long debateCount;
    private long messageCount;

    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .debateCount(0L)
                .messageCount(0L)
                .build();
    }
}