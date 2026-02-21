package com.wedding.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "winners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Winner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "char(36)")
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @Column(name = "line_user_id", nullable = false, length = 100)
    private String lineUserId;

    @Column(name = "line_display_name", length = 255)
    private String lineDisplayName;

    @Column(name = "option_key", nullable = false, columnDefinition = "char(1)")
    private String optionKey;

    @CreationTimestamp
    @Column(name = "drawn_at", updatable = false)
    private OffsetDateTime drawnAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
