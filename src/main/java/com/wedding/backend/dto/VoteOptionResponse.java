package com.wedding.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VoteOptionResponse {

    private List<VoteOption> options;
    private long totalVotes;
    private String correctAnswer;

    @Data
    @Builder
    public static class VoteOption {
        private String key;
        private String label;
        private String color;
        private long count;
        private double percentage;
    }
}
