package com.wedding.backend.dto;

import com.wedding.backend.domain.Winner;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class WinnerResponse {

    private UUID id;
    private String lineUserId;
    private String lineDisplayName;
    private String optionKey;
    private OffsetDateTime drawnAt;
    private Boolean isActive;

    public static WinnerResponse from(Winner winner) {
        return WinnerResponse.builder()
                .id(winner.getId())
                .lineUserId(winner.getLineUserId())
                .lineDisplayName(winner.getLineDisplayName())
                .optionKey(winner.getOptionKey())
                .drawnAt(winner.getDrawnAt())
                .isActive(winner.getIsActive())
                .build();
    }
}
