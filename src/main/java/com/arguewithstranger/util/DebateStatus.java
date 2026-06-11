package com.arguewithstranger.util;

/**
 * Represents the lifecycle state of a debate.
 *
 * OPEN     — Debate topic created. Waiting for both debaters to join.
 *            One or zero sides may be filled.
 *
 * ONGOING  — Both FAVOR and AGAINST sides are occupied.
 *            Real-time chat is active. Spectators may vote.
 *
 * CLOSED   — Debate ended (via End Debate button or admin action).
 *            Chat is frozen. History is permanently preserved.
 *            No new messages or votes accepted.
 *
 * Stored as a STRING in MySQL (EnumType.STRING) so that adding new
 * values later does not break existing rows — unlike EnumType.ORDINAL
 * which breaks if enum order changes.
 */
public enum DebateStatus {

    OPEN,
    ONGOING,
    CLOSED
}