package com.wedding.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemVisibilityRequest {

    @NotNull(message = "Visible flag is required")
    private Boolean visible;
}
