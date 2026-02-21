package com.wedding.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    private MediaScheme scheme;

    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    @Column(name = "read_url", nullable = false, length = 1000)
    private String readUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean isVisible = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
