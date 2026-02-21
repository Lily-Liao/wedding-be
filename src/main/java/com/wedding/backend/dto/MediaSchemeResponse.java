package com.wedding.backend.dto;

import com.wedding.backend.domain.MediaItem;
import com.wedding.backend.domain.MediaScheme;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MediaSchemeResponse {

    private UUID id;
    private String name;
    private Boolean isLive;
    private Boolean isPinned;
    private Integer sortOrder;
    private List<MediaItemResponse> items;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Data
    @Builder
    public static class MediaItemResponse {
        private UUID id;
        private String fileKey;
        private String readUrl;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private Integer sortOrder;
        private Boolean isVisible;
        private OffsetDateTime createdAt;
    }

    public static MediaSchemeResponse from(MediaScheme scheme) {
        List<MediaItemResponse> itemResponses = scheme.getItems() == null
                ? List.of()
                : scheme.getItems().stream()
                        .map(MediaSchemeResponse::mapItem)
                        .toList();

        return MediaSchemeResponse.builder()
                .id(scheme.getId())
                .name(scheme.getName())
                .isLive(scheme.getIsLive())
                .isPinned(scheme.getIsPinned())
                .sortOrder(scheme.getSortOrder())
                .items(itemResponses)
                .createdAt(scheme.getCreatedAt())
                .updatedAt(scheme.getUpdatedAt())
                .build();
    }

    private static MediaItemResponse mapItem(MediaItem item) {
        return MediaItemResponse.builder()
                .id(item.getId())
                .fileKey(item.getFileKey())
                .readUrl(item.getReadUrl())
                .fileName(item.getFileName())
                .contentType(item.getContentType())
                .fileSize(item.getFileSize())
                .sortOrder(item.getSortOrder())
                .isVisible(item.getIsVisible())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
