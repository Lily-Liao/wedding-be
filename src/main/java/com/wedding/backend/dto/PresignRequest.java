package com.wedding.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PresignRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Content type is required")
    private String contentType;

    private Long fileSize;
}
