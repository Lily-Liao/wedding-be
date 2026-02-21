package com.wedding.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {

    private String type;
    private T payload;

    public static <T> WebSocketMessage<T> of(String type, T payload) {
        return WebSocketMessage.<T>builder()
                .type(type)
                .payload(payload)
                .build();
    }
}
