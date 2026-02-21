package com.wedding.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VotingOptionItem {

    @NotBlank
    private String key;    // e.g. "A", "B", "C", "D"

    @NotBlank
    private String label;  // 顯示文字 e.g. "海風清透藍 Sea Blue"

    @NotBlank
    private String color;  // 十六進位顏色碼 e.g. "#AAC6E6"
}
