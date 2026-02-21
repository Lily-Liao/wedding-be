package com.wedding.backend.dto;

import com.wedding.backend.domain.VotingSession;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VotingStatusRequest {

    @NotNull(message = "Status is required")
    private VotingSession.Status status;
}
