package com.wedding.backend.repository;

import com.wedding.backend.domain.MediaItem;
import com.wedding.backend.domain.MediaScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaItemRepository extends JpaRepository<MediaItem, UUID> {

    List<MediaItem> findAllBySchemeOrderBySortOrderAsc(MediaScheme scheme);

    List<MediaItem> findAllBySchemeAndIsVisibleTrueOrderBySortOrderAsc(MediaScheme scheme);
}
