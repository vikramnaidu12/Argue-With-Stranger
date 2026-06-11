package com.arguewithstranger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single chat message sent during a debate.
 *
 * Append-only — messages are never updated or deleted.
 * This guarantees a complete, immutable debate transcript.
 *
 * The index on debate_id makes "load all messages for debate X"
 * a fast index scan rather than a full table scan — essential
 * once the messages table grows large.
 */
@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_messages_debate_id", columnList = "debate_id"),
                @Index(name = "idx_messages_timestamp",  columnList = "timestamp")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The debate this message belongs to.
     * Non-null — every message must belong to a debate.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", nullable = false)
    private Debate debate;

    /**
     * The user who sent this message.
     * Non-null — every message must have a sender.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * The actual message text.
     * TEXT type allows long messages without truncation.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // ── Lifecycle ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}