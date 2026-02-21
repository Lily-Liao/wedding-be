package com.wedding.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MediaSchemeIdRequest {

    @NotNull(message = "Scheme ID is required")
    private UUID schemeId;
}
