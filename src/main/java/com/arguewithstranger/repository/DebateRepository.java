package com.arguewithstranger.repository;

import com.arguewithstranger.entity.Debate;
import com.arguewithstranger.entity.User;
import com.arguewithstranger.util.DebateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for the debates table.
 *
 * Uses a mix of derived query methods (Spring Data naming convention)
 * and explicit @Query JPQL for complex lookups that the naming
 * convention cannot express cleanly.
 */
@Repository
public interface DebateRepository extends JpaRepository<Debate, Long> {

    /**
     * Returns a paginated list of all debates ordered by creation
     * date descending (newest first).
     * Used by the home page to display all topics.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @return a Page of Debate objects
     */
    Page<Debate> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Returns all debates with a specific status.
     * Used for admin dashboards and filtering.
     *
     * @param status OPEN, ONGOING, or CLOSED
     * @return list of matching debates
     */
    List<Debate> findByStatus(DebateStatus status);

    /**
     * Returns a paginated list of debates filtered by status.
     * Used when the frontend wants only OPEN debates (joinable ones).
     *
     * @param status   the desired status filter
     * @param pageable pagination parameters
     * @return a Page of Debate objects
     */
    Page<Debate> findByStatusOrderByCreatedAtDesc(DebateStatus status, Pageable pageable);

    /**
     * Finds all debates where a given user is a participant
     * (either on the FAVOR or AGAINST side).
     *
     * JPQL: checks both FK columns in one query.
     * Used for the "My Debates" section of the user profile page.
     *
     * @param user the user to search for
     * @return list of debates the user has participated in
     */
    @Query("""
        SELECT d FROM Debate d
        WHERE d.favorUser = :user
           OR d.againstUser = :user
        ORDER BY d.createdAt DESC
        """)
    List<Debate> findDebatesByParticipant(@Param("user") User user);

    /**
     * Full-text style search on the topic field.
     * Uses SQL LIKE via JPQL LOWER() for case-insensitive matching.
     * For production scale, replace with a full-text search index
     * or Elasticsearch, but this is correct for this scope.
     *
     * @param keyword  the search term
     * @param pageable pagination parameters
     * @return matching debates ordered by creation date
     */
    @Query("""
        SELECT d FROM Debate d
        WHERE LOWER(d.topic) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY d.createdAt DESC
        """)
    Page<Debate> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Counts how many debates a user has participated in as a debater.
     * Used by the leaderboard to rank active users.
     *
     * @param user the user to count for
     * @return number of debates participated in
     */
    @Query("""
        SELECT COUNT(d) FROM Debate d
        WHERE d.favorUser = :user
           OR d.againstUser = :user
        """)
    long countDebatesByParticipant(@Param("user") User user);
}