package com.wedding.backend.controller;

import com.wedding.backend.dto.ApiResponse;
import com.wedding.backend.dto.ParticipantResponse;
import com.wedding.backend.dto.WinnerResponse;
import com.wedding.backend.service.LuckyDrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "幸運抽獎 Lucky Draw", description = "從投票答對的參與者中隨機抽出中獎者，抽出後自動從抽獎池移除")
public class LuckyDrawController {

    private final LuckyDrawService luckyDrawService;

    @GetMapping("/participants/eligible")
    @Operation(
        summary = "取得抽獎池名單",
        description = "列出目前所有尚未中獎的合格參與者及總人數，供大螢幕顯示抽獎前的準備畫面。"
    )
    public ResponseEntity<ApiResponse<ParticipantResponse>> getEligibleParticipants() {
        ParticipantResponse response = luckyDrawService.getEligibleParticipants();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/winners")
    @Operation(
        summary = "執行抽獎",
        description = "從抽獎池中隨機抽出一位中獎者並回傳其資訊，同時將該用戶標記為已中獎（自動從池中移除）。"
    )
    public ResponseEntity<ApiResponse<WinnerResponse>> drawWinner() {
        WinnerResponse winner = luckyDrawService.drawWinner();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Winner drawn successfully", winner));
    }

    @GetMapping("/winners")
    @Operation(
        summary = "取得所有中獎者名單",
        description = "回傳歷次抽獎的完整中獎記錄，供大螢幕顯示歷史中獎清單使用。"
    )
    public ResponseEntity<ApiResponse<List<WinnerResponse>>> getAllWinners() {
        List<WinnerResponse> winners = luckyDrawService.getAllWinners();
        return ResponseEntity.ok(ApiResponse.success(winners));
    }

    @DeleteMapping("/winners/{id}")
    @Operation(
        summary = "取消中獎資格",
        description = "當中獎者棄權時，將該筆中獎記錄標記為無效，該用戶不會重新回到抽獎池。"
    )
    public ResponseEntity<ApiResponse<Void>> cancelWinner(
            @Parameter(description = "中獎記錄的 UUID", required = true)
            @PathVariable UUID id) {
        luckyDrawService.cancelWinner(id);
        return ResponseEntity.ok(ApiResponse.success("Winner cancelled successfully", null));
    }

    @DeleteMapping("/winners")
    @Operation(
        summary = "重置抽獎（Admin）",
        description = "清空所有中獎記錄，讓整個抽獎環節可以重來。被抽過的人將重新回到抽獎池。"
    )
    public ResponseEntity<ApiResponse<Void>> resetWinners() {
        luckyDrawService.resetWinners();
        return ResponseEntity.ok(ApiResponse.success("Lucky draw has been reset", null));
    }
}
