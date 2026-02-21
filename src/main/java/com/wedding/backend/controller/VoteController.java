package com.wedding.backend.controller;

import com.wedding.backend.dto.ApiResponse;
import com.wedding.backend.dto.VoteOptionResponse;
import com.wedding.backend.dto.VotingOptionItem;
import com.wedding.backend.dto.VotingStatusRequest;
import com.wedding.backend.domain.VotingSession;
import com.wedding.backend.service.VoteService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "互動投票 Voting", description = "婚禮現場限時投票功能，支援 Waiting / Start / Closed 三種狀態切換")
public class VoteController {

    private final VoteService voteService;

    @GetMapping("/api/votes/options")
    @Operation(
        summary = "取得投票選項與統計",
        description = "回傳 A~D 四個禮服色系選項，包含各選項目前的票數與百分比。"
    )
    public ResponseEntity<ApiResponse<VoteOptionResponse>> getVoteOptions() {
        VoteOptionResponse options = voteService.getVoteOptions();
        return ResponseEntity.ok(ApiResponse.success(options));
    }

    @PatchMapping("/admin/voting-session/status")
    @Operation(
        summary = "切換投票狀態（Admin）",
        description = "將投票功能切換為 START（開放投票）或 CLOSED（結束投票）。" +
                      "狀態流程：WAITING → START → CLOSED，不可逆轉。"
    )
    public ResponseEntity<ApiResponse<VotingSession>> updateVotingStatus(
            @Valid @RequestBody VotingStatusRequest request) {
        VotingSession session = voteService.updateSessionStatus(request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Voting status updated successfully", session));
    }

    @PutMapping("/admin/voting-session/options")
    @Operation(
        summary = "設定投票選項（Admin）",
        description = "自訂 A~D 四個選項的顯示文字與顏色。只有在 WAITING 狀態下才能修改。" +
                      "Request body 範例：[{\"key\":\"A\",\"label\":\"海風清透藍\",\"color\":\"#AAC6E6\"}, ...]"
    )
    public ResponseEntity<ApiResponse<VotingSession>> updateVotingOptions(
            @RequestBody List<VotingOptionItem> options) {
        VotingSession session = voteService.updateSessionOptions(options);
        return ResponseEntity.ok(ApiResponse.success("Voting options updated successfully", session));
    }

}
