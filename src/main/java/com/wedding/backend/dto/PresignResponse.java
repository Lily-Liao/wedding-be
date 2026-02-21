package com.wedding.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PresignResponse {

    private UUID itemId;
    private String uploadUrl;
    private String fileKey;
    private String readUrl;
    private String fileName;
    private long expiresInSeconds;
}
