package com.wedding.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "line_user_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineUserState {

    public enum State {
        IDLE, AWAITING_MESSAGE, AWAITING_VOTE
    }

    @Id
    @Column(name = "line_user_id", length = 100)
    private String lineUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 50, columnDefinition = "varchar(50)")
    @Builder.Default
    private State currentState = State.IDLE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_data", columnDefinition = "json")
    private Map<String, Object> stateData;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
