package com.arguewithstranger.repository;

import com.arguewithstranger.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for the votes table.
 *
 * Two queries do the heavy lifting:
 *  1. existsByDebateIdAndUserId — duplicate vote check
 *  2. countByDebateIdAndSelectedSide — vote tallying for results
 *
 * Both are covered by the composite index on (debate_id, user_id)
 * and the single-column index on debate_id declared in the entity.
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    /**
     * Checks whether a user has already voted in a debate.
     * Called before every vote insert to enforce the one-vote rule
     * at the application layer (the DB unique constraint is the
     * second line of defence).
     *
     * @param debateId the debate being voted in
     * @param userId   the user attempting to vote
     * @return true if a vote already exists
     */
    boolean existsByDebateIdAndUserId(Long debateId, Long userId);

    /**
     * Retrieves the existing vote for a user in a debate.
     * Used to return a clear error message telling the user
     * which side they already voted for.
     *
     * @param debateId the debate
     * @param userId   the user
     * @return Optional containing the vote, or empty if not voted
     */
    Optional<Vote> findByDebateIdAndUserId(Long debateId, Long userId);

    /**
     * Counts votes for a specific side in a debate.
     * Called twice per result request — once for "FAVOR",
     * once for "AGAINST" — to build the VoteResultResponse.
     *
     * selectedSide values: "FAVOR" or "AGAINST"
     *
     * @param debateId     the debate
     * @param selectedSide which side to count
     * @return vote count for that side
     */
    long countByDebateIdAndSelectedSide(Long debateId, String selectedSide);

    /**
     * Returns the total number of votes cast in a debate
     * regardless of side. Used for displaying participation
     * statistics on the results page.
     *
     * @param debateId the debate
     * @return total vote count
     */
    long countByDebateId(Long debateId);

    /**
     * Fetches vote distribution for a debate grouped by side.
     * Returns rows of [selectedSide, count] — used for building
     * charts or summary statistics without two separate queries.
     *
     * @param debateId the debate
     * @return list of [selectedSide, count] pairs
     */
    @Query("""
        SELECT v.selectedSide, COUNT(v)
        FROM Vote v
        WHERE v.debate.id = :debateId
        GROUP BY v.selectedSide
        """)
    java.util.List<Object[]> getVoteDistribution(@Param("debateId") Long debateId);
}