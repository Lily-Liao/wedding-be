package com.wedding.backend.repository;

import com.wedding.backend.domain.MediaScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaSchemeRepository extends JpaRepository<MediaScheme, UUID> {

    List<MediaScheme> findAllByOrderBySortOrderAsc();

    Optional<MediaScheme> findByIsLiveTrue();

    @Modifying
    @Query("UPDATE MediaScheme s SET s.isLive = false WHERE s.isLive = true")
    void clearLiveScheme();
}
