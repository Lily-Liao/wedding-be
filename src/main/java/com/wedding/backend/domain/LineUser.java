package com.wedding.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "line_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineUser {

    @Id
    @Column(name = "line_user_id", length = 100)
    private String lineUserId;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "picture_url", length = 500)
    private String pictureUrl;

    @Column(name = "language", length = 10)
    private String language;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
