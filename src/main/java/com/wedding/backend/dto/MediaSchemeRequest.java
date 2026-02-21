package com.wedding.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MediaSchemeRequest {

    @NotBlank(message = "Name is required")
    private String name;
}
