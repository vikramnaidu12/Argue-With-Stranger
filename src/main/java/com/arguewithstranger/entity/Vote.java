package com.arguewithstranger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single vote cast by a spectator in a debate.
 *
 * selectedSide stores either "FAVOR" or "AGAINST" as a plain
 * string — we use a simple String rather than an enum here
 * because the value is always one of exactly two choices and
 * is validated at the service layer before persisting.
 *
 * The composite unique constraint on (debate_id, user_id) is
 * the database-level enforcement of the one-vote-per-user rule.
 * It acts as a safety net even if the application layer fails.
 *
 * Indexes on debate_id speed up the vote count query:
 * "COUNT votes for debate X grouped by selectedSide"
 */
@Entity
@Table(
        name = "votes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_votes_debate_user",
                        columnNames = {"debate_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_votes_debate_id", columnList = "debate_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The debate this vote belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", nullable = false)
    private Debate debate;

    /**
     * The spectator who cast this vote.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Which side this vote was cast for.
     * Valid values: "FAVOR" or "AGAINST"
     * Validated in VoteService before persisting.
     */
    @Column(name = "selected_side", nullable = false, length = 10)
    private String selectedSide;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // ── Lifecycle ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}