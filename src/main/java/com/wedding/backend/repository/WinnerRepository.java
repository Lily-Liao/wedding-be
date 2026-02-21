package com.wedding.backend.repository;

import com.wedding.backend.domain.Winner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WinnerRepository extends JpaRepository<Winner, UUID> {

    List<Winner> findAllByOrderByDrawnAtDesc();

    List<Winner> findAllByIsActiveTrueOrderByDrawnAtDesc();
}
