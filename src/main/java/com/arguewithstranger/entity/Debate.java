package com.arguewithstranger.entity;

import com.arguewithstranger.util.DebateStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single debate topic and its full state.
 *
 * favorUser    — The user who joined the FAVOR side. Nullable until joined.
 * againstUser  — The user who joined the AGAINST side. Nullable until joined.
 * createdBy    — The admin/user who created this topic.
 * status       — Drives the entire debate lifecycle (OPEN → ONGOING → CLOSED).
 *
 * FetchType.LAZY on all @ManyToOne relations — we never want Hibernate
 * to automatically join users when loading a list of debates. We fetch
 * user details explicitly only when needed, preventing N+1 query problems.
 */
@Entity
@Table(name = "debates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Debate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── Participants ───────────────────────────────────────────

    /**
     * User on the FAVOR side. Null when no one has joined yet.
     * join(column) uses a clean FK column name rather than
     * Hibernate's default "favor_user_id".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favor_user_id")
    private User favorUser;

    /**
     * User on the AGAINST side. Null when no one has joined yet.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "against_user_id")
    private User againstUser;

    /**
     * The user who created this debate topic.
     * Required — a debate always has a creator.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // ── Status ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DebateStatus status;

    // ── Timestamps ─────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Null until the debate is closed
    private LocalDateTime endedAt;

    // ── Lifecycle ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // Every new debate starts as OPEN
        if (this.status == null) {
            this.status = DebateStatus.OPEN;
        }
    }

    // ── Convenience helpers ────────────────────────────────────

    /**
     * Returns true if both debater slots are filled.
     * Used by DebateService to transition OPEN → ONGOING.
     */
    public boolean isFull() {
        return favorUser != null && againstUser != null;
    }

    /**
     * Returns true if the given user is one of the two debaters.
     * Used by the WebSocket controller and voting service to
     * enforce the rule that debaters cannot vote.
     */
    public boolean isDebater(User user) {
        if (user == null) return false;
        return (favorUser  != null && favorUser.getId().equals(user.getId()))
                || (againstUser != null && againstUser.getId().equals(user.getId()));
    }

    /**
     * Returns true if this debate is accepting new messages.
     */
    public boolean isAcceptingMessages() {
        return this.status == com.arguewithstranger.util.DebateStatus.ONGOING;
    }
}