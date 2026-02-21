package com.wedding.backend.controller;

import com.wedding.backend.dto.*;
import com.wedding.backend.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/media/schemes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "媒體方案 Media Schemes", description = "管理多組圖片／影片播放清單，支援排序、可見性控制與 Cloudflare R2 上傳")
public class MediaController {

    private final MediaService mediaService;

    @GetMapping
    @Operation(
        summary = "取得所有媒體方案",
        description = "回傳所有媒體方案及其素材清單，包含排序、可見性與是否為直播中的方案。"
    )
    public ResponseEntity<ApiResponse<List<MediaSchemeResponse>>> getAllSchemes() {
        return ResponseEntity.ok(ApiResponse.success(mediaService.getAllSchemes()));
    }

    @PostMapping
    @Operation(
        summary = "新增媒體方案",
        description = "建立一個新的空媒體方案，建立後可再透過 presign API 上傳素材。"
    )
    public ResponseEntity<ApiResponse<MediaSchemeResponse>> createScheme(
            @Valid @RequestBody MediaSchemeRequest request) {
        MediaSchemeResponse response = mediaService.createScheme(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}/rename")
    @Operation(
        summary = "重新命名方案",
        description = "修改指定方案的名稱，方案內的素材不受影響。"
    )
    public ResponseEntity<ApiResponse<MediaSchemeResponse>> renameScheme(
            @Parameter(description = "方案 UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody MediaSchemeRequest request) {
        MediaSchemeResponse response = mediaService.renameScheme(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "刪除方案",
        description = "刪除指定方案及其底下所有素材（含 Cloudflare R2 上的檔案）。"
    )
    public ResponseEntity<ApiResponse<Void>> deleteScheme(
            @Parameter(description = "方案 UUID", required = true) @PathVariable UUID id) {
        mediaService.deleteScheme(id);
        return ResponseEntity.ok(ApiResponse.success("Scheme deleted successfully", null));
    }

    @PutMapping("/live")
    @Operation(
        summary = "設定直播方案",
        description = "切換目前在應援牆輪播的方案，同一時間只有一個方案為 live 狀態。"
    )
    public ResponseEntity<ApiResponse<MediaSchemeResponse>> setLiveScheme(
            @RequestBody MediaSchemeIdRequest request) {
        MediaSchemeResponse response = mediaService.setLiveScheme(request.getSchemeId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/pin")
    @Operation(
        summary = "釘選／取消釘選素材",
        description = "固定顯示方案中的單一素材（停止輪播）。傳入 null 則恢復輪播模式。"
    )
    public ResponseEntity<ApiResponse<MediaSchemeResponse>> togglePin(
            @Parameter(description = "方案 UUID", required = true) @PathVariable UUID id) {
        MediaSchemeResponse response = mediaService.togglePin(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/items/presign")
    @Operation(
        summary = "取得上傳預簽章 URL",
        description = """
            向 Cloudflare R2 請求預簽章 PUT URL，並在資料庫建立對應的素材記錄。

            **上傳流程**
            1. 呼叫此 API，取得 `uploadUrl`（60 分鐘有效）與 `readUrl`（永久有效）
            2. 前端直接對 `uploadUrl` 發送 `PUT` 請求，Body 為檔案二進位內容，Header 加上 `Content-Type`
            3. R2 回傳 `200` 後，即可用 `readUrl` 顯示圖片／播放影片

            **回傳欄位說明**
            - `uploadUrl`：預簽章上傳網址，僅限 PUT，60 分鐘後失效
            - `readUrl`：永久公開讀取網址（`publicUrl/fileKey`），可直接放入 `<img src>` 或影片播放器
            - `itemId`：已建立的素材 UUID，可用於後續排序、可見性、刪除等操作
            - `expiresInSeconds`：uploadUrl 的剩餘秒數（預設 3600）

            > `readUrl` 需要 Cloudflare R2 Bucket 已開啟 **Public Access**。
            """
    )
    public ResponseEntity<ApiResponse<PresignResponse>> presignUpload(
            @Parameter(description = "方案 UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody PresignRequest request) {
        PresignResponse response = mediaService.presignUpload(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/items/order")
    @Operation(
        summary = "調整素材排序",
        description = "根據拖拽結果提交新的素材順序，傳入所有素材 ID 的排列陣列。"
    )
    public ResponseEntity<ApiResponse<Void>> updateItemOrder(
            @Parameter(description = "方案 UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody ItemOrderRequest request) {
        mediaService.updateItemOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success("Item order updated successfully", null));
    }

    @PatchMapping("/{id}/items/{itemId}/visibility")
    @Operation(
        summary = "切換素材可見性",
        description = "控制單一素材是否出現在輪播中，隱藏後不刪除檔案，隨時可重新啟用。"
    )
    public ResponseEntity<ApiResponse<MediaSchemeResponse.MediaItemResponse>> updateItemVisibility(
            @Parameter(description = "方案 UUID", required = true) @PathVariable UUID id,
            @Parameter(description = "素材 UUID", required = true) @PathVariable UUID itemId,
            @Valid @RequestBody ItemVisibilityRequest request) {
        MediaSchemeResponse.MediaItemResponse response = mediaService.updateItemVisibility(id, itemId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(
        summary = "刪除素材",
        description = "從方案中移除指定素材，同時刪除 Cloudflare R2 上對應的檔案。"
    )
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @Parameter(description = "方案 UUID", required = true) @PathVariable UUID id,
            @Parameter(description = "素材 UUID", required = true) @PathVariable UUID itemId) {
        mediaService.deleteItem(id, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item deleted successfully", null));
    }
}
