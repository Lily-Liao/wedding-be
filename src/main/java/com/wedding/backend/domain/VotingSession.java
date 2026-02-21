package com.wedding.backend.domain;

import com.wedding.backend.dto.VotingOptionItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "voting_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotingSession {

    public enum Status {
        WAITING, START, CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Status status;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "correct_answer", length = 1, columnDefinition = "char(1)")
    private String correctAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "json")
    private List<VotingOptionItem> options;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}