package com.wedding.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voting_session_id", nullable = false)
    private VotingSession votingSession;

    @Column(name = "line_user_id", nullable = false, length = 100)
    private String lineUserId;

    @Column(name = "line_display_name", length = 255)
    private String lineDisplayName;

    @Column(name = "option_key", nullable = false, columnDefinition = "char(1)")
    private String optionKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
