package com.wedding.backend.controller;

import com.wedding.backend.dto.ApiResponse;
import com.wedding.backend.dto.MessageResponse;
import com.wedding.backend.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "留言牆 Wedding Wall", description = "賓客透過 LINE 送出的應援留言")
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    @Operation(
        summary = "取得所有留言",
        description = "回傳所有已儲存的應援留言，依建立時間排序，供前端顯示牆輪播使用。"
    )
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getAllMessages() {
        List<MessageResponse> messages = messageService.getAllMessages();
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}
