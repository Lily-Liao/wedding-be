package com.wedding.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ParticipantResponse {

    private List<Participant> data;
    private Metadata metadata;

    @Data
    @Builder
    public static class Participant {
        private UUID voteId;
        private String lineUserId;
        private String lineDisplayName;
        private String optionKey;
    }

    @Data
    @Builder
    public static class Metadata {
        private int totalCount;
        private OffsetDateTime updatedAt;
    }
}
