package com.wedding.backend.repository;

import com.wedding.backend.domain.VotingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VotingSessionRepository extends JpaRepository<VotingSession, Long> {

    Optional<VotingSession> findTopByOrderByIdDesc();
}
