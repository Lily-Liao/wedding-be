package com.wedding.backend.repository;

import com.wedding.backend.domain.Vote;
import com.wedding.backend.domain.VotingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    boolean existsByVotingSessionAndLineUserId(VotingSession session, String lineUserId);

    Optional<Vote> findByVotingSessionAndLineUserId(VotingSession session, String lineUserId);

    List<Vote> findAllByVotingSession(VotingSession session);

    @Query("SELECT v.optionKey, COUNT(v) FROM Vote v WHERE v.votingSession = :session GROUP BY v.optionKey")
    List<Object[]> countByOptionKeyForSession(@Param("session") VotingSession session);

    long countByVotingSession(VotingSession session);

    @Query("""
            SELECT v FROM Vote v
            WHERE v.votingSession = :session
            AND v.optionKey = :correctAnswer
            AND v.id NOT IN (SELECT w.vote.id FROM Winner w)
            """)
    List<Vote> findEligibleForDraw(@Param("session") VotingSession session,
                                   @Param("correctAnswer") String correctAnswer);
}
