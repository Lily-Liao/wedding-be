package com.wedding.backend.dto;

import com.wedding.backend.domain.Message;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MessageResponse {

    private UUID id;
    private String name;
    private String pictureUrl;
    private String content;
    private long timestamp;

    public static MessageResponse from(Message message, String pictureUrl) {
        return MessageResponse.builder()
                .id(message.getId())
                .name(message.getName())
                .pictureUrl(pictureUrl)
                .content(message.getContent())
                .timestamp(message.getCreatedAt() != null
                        ? message.getCreatedAt().toInstant().toEpochMilli()
                        : Instant.now().toEpochMilli())
                .build();
    }
}
