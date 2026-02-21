package com.wedding.backend.service;

import com.wedding.backend.config.R2Properties;
import com.wedding.backend.domain.MediaItem;
import com.wedding.backend.domain.MediaScheme;
import com.wedding.backend.dto.*;
import com.wedding.backend.exception.BusinessException;
import com.wedding.backend.exception.ResourceNotFoundException;
import com.wedding.backend.repository.MediaItemRepository;
import com.wedding.backend.repository.MediaSchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaSchemeRepository mediaSchemeRepository;
    private final MediaItemRepository mediaItemRepository;
    private final S3Presigner r2S3Presigner;
    private final R2Properties r2Properties;

    @Transactional(readOnly = true)
    public List<MediaSchemeResponse> getAllSchemes() {
        return mediaSchemeRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(MediaSchemeResponse::from)
                .toList();
    }

    @Transactional
    public MediaSchemeResponse createScheme(MediaSchemeRequest request) {
        long count = mediaSchemeRepository.count();

        MediaScheme scheme = MediaScheme.builder()
                .name(request.getName())
                .sortOrder((int) count)
                .build();

        MediaScheme saved = mediaSchemeRepository.save(scheme);
        log.info("Created media scheme: {}", saved.getId());
        return MediaSchemeResponse.from(saved);
    }

    @Transactional
    public MediaSchemeResponse renameScheme(UUID schemeId, MediaSchemeRequest request) {
        MediaScheme scheme = findSchemeById(schemeId);
        scheme.setName(request.getName());
        return MediaSchemeResponse.from(mediaSchemeRepository.save(scheme));
    }

    @Transactional
    public void deleteScheme(UUID schemeId) {
        MediaScheme scheme = findSchemeById(schemeId);
        if (Boolean.TRUE.equals(scheme.getIsLive())) {
            throw new BusinessException("Cannot delete the currently live scheme.");
        }
        mediaSchemeRepository.delete(scheme);
        log.info("Deleted media scheme: {}", schemeId);
    }

    @Transactional
    public MediaSchemeResponse setLiveScheme(UUID schemeId) {
        findSchemeById(schemeId);
        mediaSchemeRepository.clearLiveScheme();

        MediaScheme scheme = findSchemeById(schemeId);
        scheme.setIsLive(true);
        return MediaSchemeResponse.from(mediaSchemeRepository.save(scheme));
    }

    @Transactional
    public MediaSchemeResponse togglePin(UUID schemeId) {
        MediaScheme scheme = findSchemeById(schemeId);
        scheme.setIsPinned(!Boolean.TRUE.equals(scheme.getIsPinned()));
        return MediaSchemeResponse.from(mediaSchemeRepository.save(scheme));
    }

    @Transactional
    public PresignResponse presignUpload(UUID schemeId, PresignRequest request) {
        MediaScheme scheme = findSchemeById(schemeId);

        String fileKey = String.format("schemes/%s/%s/%s",
                schemeId,
                UUID.randomUUID(),
                sanitizeFileName(request.getFileName()));

        // Create media item record first
        List<MediaItem> existingItems = mediaItemRepository.findAllBySchemeOrderBySortOrderAsc(scheme);
        int nextOrder = existingItems.size();

        String readUrl = r2Properties.getPublicUrl() + "/" + fileKey;

        MediaItem item = MediaItem.builder()
                .scheme(scheme)
                .fileKey(fileKey)
                .readUrl(readUrl)
                .fileName(request.getFileName())
                .contentType(request.getContentType())
                .fileSize(request.getFileSize())
                .sortOrder(nextOrder)
                .build();

        MediaItem savedItem = mediaItemRepository.save(item);

        // Generate presigned URL
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(r2Properties.getBucketName())
                .key(fileKey)
                .contentType(request.getContentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(r2Properties.getPresignExpiryMinutes()))
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presigned = r2S3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presigned.url().toString();

        log.info("Generated presign URL for scheme: {}, item: {}", schemeId, savedItem.getId());

        return PresignResponse.builder()
                .itemId(savedItem.getId())
                .uploadUrl(uploadUrl)
                .fileKey(fileKey)
                .readUrl(readUrl)
                .fileName(request.getFileName())
                .expiresInSeconds((long) r2Properties.getPresignExpiryMinutes() * 60)
                .build();
    }

    @Transactional
    public void updateItemOrder(UUID schemeId, ItemOrderRequest request) {
        MediaScheme scheme = findSchemeById(schemeId);
        List<UUID> itemIds = request.getItemIds();

        for (int i = 0; i < itemIds.size(); i++) {
            UUID itemId = itemIds.get(i);
            MediaItem item = mediaItemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("MediaItem", "id", itemId));

            if (!item.getScheme().getId().equals(scheme.getId())) {
                throw new BusinessException("Item does not belong to the specified scheme.");
            }

            item.setSortOrder(i);
            mediaItemRepository.save(item);
        }
    }

    @Transactional
    public MediaSchemeResponse.MediaItemResponse updateItemVisibility(
            UUID schemeId, UUID itemId, ItemVisibilityRequest request) {

        findSchemeById(schemeId);
        MediaItem item = mediaItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MediaItem", "id", itemId));

        if (!item.getScheme().getId().equals(schemeId)) {
            throw new BusinessException("Item does not belong to the specified scheme.");
        }

        item.setIsVisible(request.getVisible());
        MediaItem saved = mediaItemRepository.save(item);

        return MediaSchemeResponse.MediaItemResponse.builder()
                .id(saved.getId())
                .fileKey(saved.getFileKey())
                .readUrl(saved.getReadUrl())
                .fileName(saved.getFileName())
                .contentType(saved.getContentType())
                .fileSize(saved.getFileSize())
                .sortOrder(saved.getSortOrder())
                .isVisible(saved.getIsVisible())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteItem(UUID schemeId, UUID itemId) {
        findSchemeById(schemeId);
        MediaItem item = mediaItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MediaItem", "id", itemId));

        if (!item.getScheme().getId().equals(schemeId)) {
            throw new BusinessException("Item does not belong to the specified scheme.");
        }

        mediaItemRepository.delete(item);
        log.info("Deleted media item: {} from scheme: {}", itemId, schemeId);
    }

    private MediaScheme findSchemeById(UUID schemeId) {
        return mediaSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("MediaScheme", "id", schemeId));
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
