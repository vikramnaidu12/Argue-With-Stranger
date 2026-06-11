package com.arguewithstranger.repository;

import com.arguewithstranger.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for the messages table.
 *
 * The critical design decision here is the JOIN FETCH in
 * findByDebateIdOrderByTimestampAsc — without it, loading
 * 100 messages would trigger 101 queries (1 for messages,
 * then 1 per message to load the sender). JOIN FETCH collapses
 * this into a single efficient query.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Loads the complete chat history for a debate in
     * chronological order with sender details pre-loaded.
     *
     * JOIN FETCH d.sender — prevents N+1 queries.
     * The sender username is needed for every message in the UI,
     * so we fetch it eagerly here in one SQL JOIN.
     *
     * @param debateId the debate whose messages we want
     * @return ordered list of messages with senders hydrated
     */
    @Query("""
        SELECT m FROM Message m
        JOIN FETCH m.sender
        WHERE m.debate.id = :debateId
        ORDER BY m.timestamp ASC
        """)
    List<Message> findByDebateIdOrderByTimestampAsc(@Param("debateId") Long debateId);

    /**
     * Counts total messages in a debate.
     * Used for debate statistics and the leaderboard
     * (most active debaters by message count).
     *
     * @param debateId the debate to count messages for
     * @return total message count
     */
    long countByDebateId(Long debateId);

    /**
     * Counts how many messages a specific user has sent
     * across all debates. Used for the user profile and
     * leaderboard activity score.
     *
     * @param senderId the user whose messages we count
     * @return total messages sent by this user
     */
    long countBySenderId(Long senderId);
}