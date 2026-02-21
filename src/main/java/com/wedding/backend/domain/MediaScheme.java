package com.wedding.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "media_schemes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaScheme {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "char(36)")
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "is_live", nullable = false)
    @Builder.Default
    private Boolean isLive = false;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<MediaItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
