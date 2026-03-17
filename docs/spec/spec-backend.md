<!-- ⚠️ 此檔案是從 Spec Repo 同步的唯讀快照，請勿直接修改。
     要修改 spec 請在 .spec-changes/ 建立 changeset。
     正本位置：spec-repo/backend/wedding-backend/spec-backend.md -->

# wedding-backend 後端技術規格書

<!--
=== META（供 RAG 系統使用）===
module: wedding-backend
type: backend-tech
version: 1.0.0
last_updated: 2026-03-18
owner: Lily Liao
audience: technical
repo: wedding-be
related_services: LINE Messaging API, Cloudflare R2, React Frontend
tags: wedding, line-bot, voting, lucky-draw, media, websocket
-->

---

## 文件資訊

| 欄位 | 說明              |
|------|-----------------|
| 微服務名稱 | wedding-backend |
| 負責人 | Lily Liao       |
| 最後更新日期 | 2026-03-18      |
| 版本 | 1.0.0           |
| Git Repository | wedding-be      |
| 部署位置 | local           |

---

## 1. 服務概述

<!-- RAG 提示：說明此微服務的職責邊界與在整體架構中的角色。適合回答「這個服務負責什麼？」類型的問題。 -->

### 1.1 服務職責

婚禮互動系統後端，提供婚禮現場四大互動功能：留言應援牆（透過 LINE Bot 收集賓客留言）、限時投票（婚禮現場互動問答）、幸運抽獎（從答對投票的賓客中抽出得獎者）、媒體播放方案管理（管理大螢幕輪播的圖片與影片）。

### 1.2 職責邊界

**本服務負責：**
- LINE Webhook 事件處理（文字訊息、Postback 按鈕）
- 賓客留言的收集、儲存與 WebSocket 即時推播
- 投票 Session 的狀態管理、選票收集與統計
- 幸運抽獎的抽取邏輯與中獎者管理
- 媒體方案的 CRUD 及 Cloudflare R2 Presigned URL 生成
- LINE 用戶狀態的多輪對話管理（IDLE / AWAITING_MESSAGE / AWAITING_VOTE）

**本服務不負責（由其他系統處理）：**
- 前端展示邏輯 → React Frontend
- LINE 用戶身份驗證 → LINE Platform
- 媒體檔案儲存 → Cloudflare R2
- 婚禮照片精選集 → Google Drive(TBC: change to cloudflare r2)

### 1.3 架構位置

```
LINE Platform (Webhook)          React Frontend
        ↓                               ↓
  wedding-backend  ──── REST API ──────→
        ↓                         WebSocket (/topic/messages)
  MySQL 8.0        Cloudflare R2
```

### 1.4 技術堆疊

| 項目 | 技術 | 版本 |
|------|------|------|
| 語言 / 框架 | Java + Spring Boot | Java 17 / Spring Boot 3.2.3 |
| 資料庫 | MySQL | 8.0 |
| 快取 | 無 | — |
| 訊息佇列 | 無（WebSocket 直推） | — |
| 物件儲存 | Cloudflare R2（AWS SDK v2 S3 相容） | software.amazon.awssdk:s3 |
| LINE 整合 | LINE Messaging API SDK | 8.4.x |
| ORM / Migration | JPA/Hibernate 6 + Flyway | Hibernate 6 / Flyway V1–V7 |
| 即時推播 | WebSocket + STOMP | — |
| API 文件 | SpringDoc OpenAPI | /swagger-ui.html |

---

## 2. API 規格

<!-- RAG 提示：本服務對外提供的所有 API。適合回答「有哪些 API？」、「XX API 的 request/response 是什麼？」類型的問題。 -->

### 2.1 API 總覽

| API 名稱 | Method | Endpoint | 說明 | 認證方式 | 需要角色 |
|----------|--------|----------|------|---------|---------|
| 取得所有留言 | GET | `/api/messages` | 取得所有應援留言 | 無 | 公開 |
| 取得投票選項 | GET | `/api/votes/options` | 取得 A~D 選項票數與百分比 | 無 | 公開 |
| 切換投票狀態 | PATCH | `/admin/voting-session/status` | 切換 WAITING/START/CLOSED | 無（網路層控制） | Admin |
| 設定投票選項 | PUT | `/admin/voting-session/options` | 設定選項文字與顏色 | 無（網路層控制） | Admin |
| 取得抽獎池 | GET | `/api/v1/participants/eligible` | 列出合格參與者 | 無 | 公開 |
| 執行抽獎 | POST | `/api/v1/winners` | 隨機抽出一位中獎者 | 無（網路層控制） | Admin |
| 取得中獎者名單 | GET | `/api/v1/winners` | 歷次抽獎完整記錄 | 無 | 公開 |
| 取消中獎資格 | DELETE | `/api/v1/winners/{id}` | 標記中獎記錄為無效 | 無（網路層控制） | Admin |
| 重置抽獎 | DELETE | `/api/v1/winners` | 清空所有中獎記錄 | 無（網路層控制） | Admin |
| 取得所有媒體方案 | GET | `/api/media/schemes` | 所有方案及素材清單 | 無 | 公開 |
| 新增媒體方案 | POST | `/api/media/schemes` | 建立新方案 | 無（網路層控制） | Admin |
| 重新命名方案 | PUT | `/api/media/schemes/{id}/rename` | 修改方案名稱 | 無（網路層控制） | Admin |
| 刪除方案 | DELETE | `/api/media/schemes/{id}` | 刪除方案及所有素材 | 無（網路層控制） | Admin |
| 設定直播方案 | PUT | `/api/media/schemes/live` | 切換 live 方案 | 無（網路層控制） | Admin |
| 釘選素材 | PUT | `/api/media/schemes/{id}/pin` | 固定顯示單一素材 | 無（網路層控制） | Admin |
| 取得上傳 Presign URL | POST | `/api/media/schemes/{id}/items/presign` | 取得 R2 上傳網址 | 無（網路層控制） | Admin |
| 調整素材排序 | PUT | `/api/media/schemes/{id}/items/order` | 提交新排序 | 無（網路層控制） | Admin |
| 切換素材可見性 | PATCH | `/api/media/schemes/{id}/items/{itemId}/visibility` | 控制素材是否輪播 | 無（網路層控制） | Admin |
| 刪除素材 | DELETE | `/api/media/schemes/{id}/items/{itemId}` | 刪除素材含 R2 檔案 | 無（網路層控制） | Admin |

### 2.2 API 詳細規格

---

#### GET /api/messages

**說明：** 回傳所有已儲存的應援留言，依建立時間排序，供前端顯示牆輪播使用。

**認證：** 無
**權限：** 公開

**Response — 成功 (200)：**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "uuid",
      "lineUserId": "string",
      "name": "string",
      "content": "string",
      "createdAt": "2026-03-18T10:00:00.000+0800"
    }
  ]
}
```

---

#### GET /api/votes/options

**說明：** 回傳 A~D 四個投票選項，包含各選項目前的票數、百分比，以及 `correctAnswer`（正確答案，由 DB 直接寫入）。

**認證：** 無
**權限：** 公開

**Response — 成功 (200)：**
```json
{
  "success": true,
  "data": {
    "sessionStatus": "START",
    "correctAnswer": "A",
    "options": [
      { "key": "A", "label": "海風清透藍", "color": "#AAC6E6", "count": 10, "percentage": 40.0 },
      { "key": "B", "label": "霧色銀灰", "color": "#BDBDBD", "count": 8, "percentage": 32.0 },
      { "key": "C", "label": "暖棕沙漠", "color": "#C8A882", "count": 5, "percentage": 20.0 },
      { "key": "D", "label": "玫瑰粉霧", "color": "#F4B8C1", "count": 2, "percentage": 8.0 }
    ]
  }
}
```

---

#### PATCH /admin/voting-session/status

**說明：** 切換投票狀態。狀態流程：`WAITING → START → CLOSED`，不可逆轉。

**認證：** 無（依賴網路層/部署層控制）
**權限：** Admin

**Request Body：**
```json
{ "status": "START" }
```

**Response — 成功 (200)：** 回傳更新後的 `VotingSession` 物件。

**業務驗證規則：**
- `WAITING → START`：允許
- `START → CLOSED`：允許
- 其他轉換：No

---

#### PUT /admin/voting-session/options

**說明：** 自訂 A~D 四個選項的顯示文字與顏色。**只有在 WAITING 狀態下才能修改。**

**認證：** 無（依賴網路層/部署層控制）
**權限：** Admin

**Request Body：**
```json
[
  { "key": "A", "label": "海風清透藍", "color": "#AAC6E6" },
  { "key": "B", "label": "霧色銀灰", "color": "#BDBDBD" },
  { "key": "C", "label": "暖棕沙漠", "color": "#C8A882" },
  { "key": "D", "label": "玫瑰粉霧", "color": "#F4B8C1" }
]
```

---

#### GET /api/v1/participants/eligible

**說明：** 列出目前所有尚未中獎的合格參與者及總人數，供大螢幕顯示抽獎前的準備畫面。合格條件：投票答對且尚未被抽中。

**認證：** 無
**權限：** 公開

**Response — 成功 (200)：**
```json
{
  "success": true,
  "data": {
    "total": 15,
    "participants": [
      { "lineUserId": "U...", "displayName": "王大明", "optionKey": "A" }
    ]
  }
}
```

---

#### POST /api/v1/winners

**說明：** 從抽獎池中隨機抽出一位中獎者，同時將該用戶標記為已中獎（自動從池中移除，即使取消也不回池）。

**認證：** 無（依賴網路層/部署層控制）
**權限：** Admin

**Response — 成功 (201)：**
```json
{
  "success": true,
  "message": "Winner drawn successfully",
  "data": {
    "id": "uuid",
    "lineUserId": "U...",
    "displayName": "王大明",
    "optionKey": "A",
    "drawnAt": "2026-03-18T14:00:00.000+0800",
    "isActive": true
  }
}
```

**業務驗證規則：**
- 抽獎池為空時：No

---

#### DELETE /api/v1/winners/{id}

**說明：** 當中獎者棄權時，將該筆中獎記錄標記為無效（`is_active = false`）。**棄權後不會重新回到抽獎池。**

**Path Parameters：**
| 參數 | 型別 | 必填 | 說明 |
|------|------|------|------|
| id | UUID | 是 | 中獎記錄的 UUID |

---

#### DELETE /api/v1/winners

**說明：** 清空所有中獎記錄（`DELETE FROM winners`），讓整個抽獎環節可以重來。被抽過的人將重新回到抽獎池。

---

#### POST /api/media/schemes/{id}/items/presign

**說明：** 向 Cloudflare R2 請求預簽章 PUT URL，並在 DB 建立對應素材記錄。

**上傳流程：**
1. 呼叫此 API，取得 `uploadUrl`（60 分鐘有效）與 `readUrl`（永久有效）
2. 前端直接對 `uploadUrl` 發送 `PUT` 請求，Body 為檔案二進位，Header 加上 `Content-Type`
3. R2 回傳 200 後，即可用 `readUrl` 顯示

**Request Body：**
```json
{ "fileName": "photo.jpg", "contentType": "image/jpeg", "fileSize": 1024000 }
```

**Response — 成功 (200)：**
```json
{
  "success": true,
  "data": {
    "uploadUrl": "https://...(presigned PUT URL, 60分鐘有效)...",
    "readUrl": "https://r2-public-url/fileKey",
    "itemId": "uuid",
    "expiresInSeconds": 3600
  }
}
```

> `readUrl` 需要 Cloudflare R2 Bucket 已開啟 Public Access。

---

## 3. 資料模型

<!-- RAG 提示：本服務的資料庫設計。適合回答「XX 資料存在哪裡？」、「XX 表的結構是什麼？」類型的問題。 -->

### 3.1 ER 關聯總覽

```
line_users (1) ──→ (1) line_user_states   （line_user_id）
votes (N) ──→ (1) voting_sessions          （voting_session_id）
winners (1) ──→ (1) votes                  （vote_id）
media_items (N) ──→ (1) media_schemes      （scheme_id）
messages                                    （獨立，只存 line_user_id 字串，無 FK）
```

### 3.2 資料表定義

#### messages

**說明：** 儲存賓客透過 LINE Bot 送出的應援留言，供應援牆輪播顯示。

| 欄位 | 型別 | 約束 | 說明 | 索引 |
|------|------|------|------|------|
| id | char(36) | PK NOT NULL | UUID 主鍵 | Primary |
| line_user_id | varchar(100) | NULLABLE | LINE 用戶 ID | — |
| name | varchar(255) | NULLABLE | 顯示名稱 | — |
| content | TEXT | NOT NULL | 留言內容 | — |
| created_at | TIMESTAMP | NOT NULL | 建立時間（自動） | — |
| updated_at | TIMESTAMP | — | 最後更新時間（自動） | — |

---

#### voting_sessions

**說明：** 投票 Session，每次婚禮通常只有一筆。`correct_answer` 由管理員直接寫 DB，不透過 API。

| 欄位 | 型別 | 約束 | 說明 | 索引 |
|------|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主鍵 | Primary |
| status | varchar(20) | NOT NULL | WAITING / START / CLOSED | — |
| started_at | TIMESTAMP | NULLABLE | 投票開始時間 | — |
| closed_at | TIMESTAMP | NULLABLE | 投票結束時間 | — |
| correct_answer | char(1) | NULLABLE | 正確答案（A/B/C/D），直接寫 DB | — |
| options | JSON | NULLABLE | 選項設定陣列（key, label, color） | — |
| created_at | TIMESTAMP | NOT NULL | 建立時間（自動） | — |
| updated_at | TIMESTAMP | — | 最後更新時間（自動） | — |

---

#### votes

**說明：** 每位賓客的投票記錄，每個 LINE 用戶對同一個 session 只能投票一次（server-side 驗證）。

| 欄位 | 型別 | 約束 | 說明 | 索引 |
|------|------|------|------|------|
| id | char(36) | PK NOT NULL | UUID 主鍵 | Primary |
| voting_session_id | BIGINT | NOT NULL FK → voting_sessions.id | 所屬 Session | — |
| line_user_id | varchar(100) | NOT NULL | LINE 用戶 ID | — |
| line_display_name | varchar(255) | NULLABLE | 顯示名稱（快照） | — |
| option_key | char(1) | NOT NULL | 投票的選項 A/B/C/D | — |
| created_at | TIMESTAMP | NOT NULL | 投票時間（自動） | — |

---

#### winners

**說明：** 抽獎結果。`is_active = false` 代表棄權，但仍保留記錄。一旦被抽過（無論是否棄權），永遠不再回到抽獎池，除非執行全域 Reset（DELETE /api/v1/winners）。

| 欄位 | 型別 | 約束 | 說明 | 索引 |
|------|------|------|------|------|
| id | char(36) | PK NOT NULL | UUID 主鍵 | Primary |
| vote_id | char(36) | NOT NULL FK → votes.id | 關聯投票記錄 | — |
| line_user_id | varchar(100) | NOT NULL | LINE 用戶 ID | — |
| line_display_name | varchar(255) | NULLABLE | 顯示名稱（快照） | — |
| option_key | char(1) | NOT NULL | 投票選項（快照） | — |
| drawn_at | TIMESTAMP | NOT NULL | 抽獎時間（自動） | — |
| cancelled_at | TIMESTAMP | NULLABLE | 棄權時間 | — |
| is_active | TINYINT(1) | NOT NULL default=1 | 是否有效（棄權後設 false） | — |

---

#### media_schemes

**說明：** 媒體播放方案，每個方案包含一組圖片/影片輪播清單。同一時間只有一個方案的 `is_live = true`。

| 欄位 | 型別 | 約束 | 說明 | 索引 |
|------|------|------|------|------|
| id | char(36) | PK NOT NULL | UUID 主鍵 | Primary |
| name | varchar(255) | NOT NULL | 方案名稱 | — |
| is_live | TINYINT(1) | NOT NULL default=0 | 是否為目前直播中的方案 | — |
| is_pinned | TINYINT(1) | NOT NULL default=0 | 是否釘選單一素材（停止輪播） | — |
| sort_order | INT | NOT NULL default=0 | 方案排序 | — |
| created_at | TIMESTAMP | NOT NULL | 建立時間（自動） | — |
| updated_at | TIMESTAMP | — | 最後更新時間（自動） | — |

---

#### media_items

**說明：** 媒體方案內的單一素材（圖片或影片），`file_key` 對應 Cloudflare R2 的物件 Key，`read_url` 為永久公開讀取 URL。

| 欄位 | 型別 | 約束 | 說明 | 索引 |
|------|------|------|------|------|
| id | char(36) | PK NOT NULL | UUID 主鍵 | Primary |
| scheme_id | char(36) | NOT NULL FK → media_schemes.id | 所屬方案 | — |
| file_key | varchar(500) | NOT NULL | R2 Object Key | — |
| read_url | varchar(1000) | NOT NULL | 永久公開讀取 URL | — |
| file_name | varchar(255) | NOT NULL | 原始檔名 | — |
| content_type | varchar(100) | NULLABLE | MIME 類型（image/jpeg 等） | — |
| file_size | BIGINT | NULLABLE | 檔案大小（bytes） | — |
| sort_order | INT | NOT NULL default=0 | 素材排序 | — |
| is_visible | TINYINT(1) | NOT NULL default=1 | 是否在輪播中顯示 | — |
| created_at | TIMESTAMP | NOT NULL | 建立時間（自動） | — |
| updated_at | TIMESTAMP | — | 最後更新時間（自動） | — |

---

#### line_user_states

**說明：** 儲存每位 LINE 用戶的當前對話狀態（IDLE / AWAITING_MESSAGE / AWAITING_VOTE），支援多輪對話流程。`state_data` JSON 欄位目前存放 `{"locale": "zh-TW"}` 用於語系判斷。

| 欄位 | 型別 | 約束 | 說明 | 索引 |
|------|------|------|------|------|
| line_user_id | varchar(100) | PK | LINE 用戶 ID（主鍵） | Primary |
| current_state | varchar(50) | NOT NULL default=IDLE | 當前狀態 | — |
| state_data | JSON | NULLABLE | 附加狀態資料（目前存 locale） | — |
| updated_at | TIMESTAMP | — | 最後更新時間（自動） | — |

---

#### line_users

**說明：** 快取 LINE 用戶的個人資料（顯示名稱、頭貼 URL），從 LINE Platform API 取得後儲存，避免重複呼叫。

| 欄位 | 型別 | 約束 | 說明 | 索引 |
|------|------|------|------|------|
| line_user_id | varchar(100) | PK | LINE 用戶 ID（主鍵） | Primary |
| display_name | varchar(255) | NULLABLE | LINE 顯示名稱 | — |
| picture_url | varchar(500) | NULLABLE | 頭貼 URL | — |
| language | varchar(10) | NULLABLE | LINE App 語系 | — |
| created_at | TIMESTAMP | NOT NULL | 建立時間（自動） | — |
| updated_at | TIMESTAMP | — | 最後更新時間（自動） | — |

---

## 4. 狀態機

<!-- RAG 提示：本服務管理的核心實體狀態流轉。適合回答「XX 有哪些狀態？」、「從 A 狀態怎麼變成 B 狀態？」類型的問題。 -->

### 4.1 VotingSession 狀態定義

| 狀態 | 系統值 | 說明 | 是否為終態 |
|------|--------|------|-----------|
| 等待中 | `WAITING` | 婚禮尚未開始投票，可修改選項設定 | 否 |
| 投票進行中 | `START` | 賓客可以透過 LINE 投票 | 否 |
| 投票結束 | `CLOSED` | 投票截止，不再接受新投票 | 是 |

### 4.2 VotingSession 狀態流轉

```
WAITING → START
觸發方式：REST API
觸發來源：PATCH /admin/voting-session/status { status: "START" }
前置檢查：無
DB 操作：更新 status = START, started_at = now()
副作用：LINE Bot 回覆「投票已開始」訊息（當用戶觸發 限時票選 關鍵字）

START → CLOSED
觸發方式：REST API
觸發來源：PATCH /admin/voting-session/status { status: "CLOSED" }
前置檢查：無
DB 操作：更新 status = CLOSED, closed_at = now()
副作用：LINE Bot 回覆「投票已結束」訊息
```

### 4.3 不允許的轉換

| 起始狀態 | 目標狀態 | 原因 |
|---------|---------|------|
| CLOSED | 任何狀態 | 終態，不可逆轉 |
| START | WAITING | 不可回退 |
| WAITING | CLOSED | 必須先經過 START |

---

### 4.4 LineUserState 狀態定義

| 狀態 | 系統值 | 說明 | 是否為終態 |
|------|--------|------|-----------|
| 閒置 | `IDLE` | 無進行中的多輪對話 | 否 |
| 等待留言輸入 | `AWAITING_MESSAGE` | 用戶觸發「留言應援牆」後，等待輸入留言內容 | 否 |
| 等待投票輸入 | `AWAITING_VOTE` | 用戶觸發「限時票選」後，等待輸入 A/B/C/D | 否 |

### 4.5 LineUserState 狀態流轉

```
IDLE → AWAITING_MESSAGE
觸發方式：LINE 文字訊息關鍵字「留言應援牆」或「Message Wall」
DB 操作：更新 current_state = AWAITING_MESSAGE, state_data = {"locale": "zh-TW"}

AWAITING_MESSAGE → IDLE
觸發方式：用戶輸入留言內容（正常），或輸入「取消」/「Cancel」
DB 操作：更新 current_state = IDLE
副作用（正常）：建立 Message 記錄，LINE Bot 回覆確認訊息，WebSocket 推播新留言

IDLE → AWAITING_VOTE
觸發方式：LINE 文字訊息關鍵字「限時票選」或「Live Poll」（目前已註解停用）
前置檢查：投票狀態必須為 START，且用戶尚未投票

AWAITING_VOTE → IDLE
觸發方式：用戶輸入 A/B/C/D（正常），PostbackEvent 按鈕，或投票已結束
副作用（正常）：建立 Vote 記錄
```

---

## 5. 業務規則與驗證邏輯

<!-- RAG 提示：本服務實作的業務判斷邏輯。適合回答「XX 的規則是什麼？」、「什麼條件下會 OO？」類型的問題。 -->

### 5.1 投票去重規則

**規則編號：BR-VOTE-001**
**規則名稱：** 每位 LINE 用戶對同一 Session 只能投票一次
**實作位置：** `VoteService.castVote()`

```
IF votes 表中已存在 (voting_session_id = currentSession.id AND line_user_id = userId)
    THEN 拋出 BusinessException（投票重複）
ELSE
    INSERT INTO votes(...)
```

---

### 5.2 抽獎資格條件

**規則編號：BR-DRAW-001**
**規則名稱：** 只有投票答對的賓客才能參與抽獎
**實作位置：** `LuckyDrawService.getEligibleParticipants()` / `drawWinner()`

```
合格條件：
  1. 在 votes 表中有投票記錄，且 option_key = voting_sessions.correct_answer
  2. 在 winners 表中「沒有」對應的記錄（無論 is_active 狀態）

注意：correct_answer 需由管理員直接寫入 DB，不透過 API
```

---

### 5.3 中獎後不回池規則

**規則編號：BR-DRAW-002**
**規則名稱：** 一旦被抽中，即使棄權也不會重新回到抽獎池（除非全域 Reset）
**實作位置：** `LuckyDrawService.cancelWinner()`

```
cancelWinner：
  SET is_active = false, cancelled_at = now()
  NOT DELETE → 保留記錄用於排除資格

resetWinners：
  DELETE FROM winners（全部清空，所有人回池）
```

---

### 5.4 媒體方案 Live 互斥規則

**規則編號：BR-MEDIA-001**
**規則名稱：** 同一時間只有一個媒體方案可以是 live 狀態
**實作位置：** `MediaService.setLiveScheme()`

```
IF 設定新的 live 方案
    SET 所有方案 is_live = false
    SET 指定方案 is_live = true
```

---

### 5.5 LINE Bot 關鍵字 Dispatch 規則

**規則編號：BR-LINE-001**
**規則名稱：** 收到 LINE 訊息時，AWAITING 狀態優先於關鍵字 dispatch
**實作位置：** `LineWebhookController.handleTextMessage()`

```
IF currentState == AWAITING_MESSAGE
    THEN 處理留言輸入流程（或取消）
ELSE IF currentState == AWAITING_VOTE
    THEN 處理投票輸入流程（或取消）
ELSE
    查找 keywordHandlers Map，找到則執行對應 handler
    找不到則忽略（不回覆）
```

---

## 6. 排程與背景任務

<!-- RAG 提示：本服務的定時任務與背景處理。適合回答「有哪些排程？」、「排程失敗會怎樣？」類型的問題。 -->

[本服務未使用排程功能]

---

## 7. 事件與非同步處理

<!-- RAG 提示：本服務發送與接收的事件。適合回答「XX 事件誰發的？誰會收？」、「事件失敗怎麼處理？」類型的問題。 -->

### 7.1 WebSocket 推播（Producer）

| Topic | 觸發時機 | Payload | 消費者 |
|-------|---------|---------|--------|
| `/topic/messages` | 新留言建立後（`MessageService.createMessage()`） | WebSocketMessage（含留言內容） | React Frontend |

本服務不使用 Message Queue，改以 WebSocket + STOMP 直接推播給前端。

### 7.2 接收的事件（Consumer）

[本服務未使用 Message Queue]

LINE Webhook 事件透過 LINE SDK 的 `/webhook` 路徑接收，由 `LineWebhookController` 處理。

---

## 8. 第三方服務整合

<!-- RAG 提示：本服務串接的外部服務。適合回答「串了哪些第三方？」、「第三方掛了怎麼辦？」類型的問題。 -->

### 8.1 整合清單

| 服務名稱 | 用途 | 串接方式 | 環境切換 |
|---------|------|---------|---------|
| LINE Messaging API | 接收 Webhook、發送回覆訊息 | LINE Bot SDK 8.4.x | 同一 channel，開發/正式共用 |
| Cloudflare R2 | 媒體檔案物件儲存 | AWS SDK v2 S3 相容 API | 環境變數切換 Bucket/URL |

### 8.2 整合詳細說明

#### LINE Messaging API

- **SDK：** `com.linecorp.bot:line-bot-messaging-api-client:8.4.x`（Java records，無 `.builder()`）
- **Webhook 路徑：** `/webhook`（由 SDK 自動掛載）
- **認證方式：** Channel Secret（請求簽章驗證，由 SDK 處理）
- **回覆方式：** `MessagingApiClient.replyMessage(ReplyMessageRequest)`
- **降級策略：** 回覆失敗只記錄 ERROR log，不拋出異常（避免影響後續流程）

#### Cloudflare R2

- **API：** AWS SDK v2 S3 相容（`software.amazon.awssdk:s3`）
- **Endpoint：** `https://{account-id}.r2.cloudflarestorage.com`
- **認證方式：** Access Key ID + Secret Access Key
- **Presigned URL 有效期：** 60 分鐘（`presign-expiry-minutes: 60`）
- **Public URL：** `r2Properties.publicUrl + "/" + fileKey`（需 Bucket 開啟 Public Access）
- **降級策略：** No

---

## 9. 錯誤代碼定義

<!-- RAG 提示：本服務定義的所有錯誤代碼。適合回答「錯誤 XX 是什麼意思？」、「什麼情況會回傳 OO 錯誤？」類型的問題。 -->

本服務未定義結構化錯誤代碼，統一使用 `ApiResponse` 的 `message` 欄位傳遞錯誤訊息：

| 錯誤情境 | HTTP Status | 觸發條件 | 前端建議處理方式 |
|---------|-------------|---------|----------------|
| 資源不存在 | 404 | `ResourceNotFoundException`（找不到方案、素材、中獎記錄等） | 顯示錯誤提示，重新整理列表 |
| 業務邏輯錯誤 | 400 | `BusinessException`（重複投票、狀態不允許的操作等） | 顯示 toast 錯誤訊息 |
| 輸入驗證失敗 | 400 | `MethodArgumentNotValidException`（Bean Validation 失敗） | 標示對應欄位錯誤 |
| 參數格式錯誤 | 400 | `IllegalArgumentException` | 顯示錯誤提示 |
| 系統內部錯誤 | 500 | 未預期的 `Exception` | 顯示「系統發生錯誤」並通知管理員 |

**統一回傳格式：**
```json
{ "success": false, "message": "錯誤說明", "data": null }
```

---

## 10. 快取策略

<!-- RAG 提示：本服務的快取使用方式。適合回答「XX 有快取嗎？」、「快取什麼時候更新？」類型的問題。 -->

[本服務未使用快取（Redis 或其他）]

所有資料直接讀取 MySQL，以 Hikari 連線池（最大 10 連線）管理 DB 連線。

---

## 11. 認證與授權

<!-- RAG 提示：本服務的認證與授權機制。適合回答「怎麼驗證身份？」、「權限怎麼控管？」類型的問題。 -->

### 11.1 認證機制

- **認證方式：** 無（本服務未實作 Spring Security）
- **Admin API 保護：** 依賴網路層/部署層控制（例如：VPN、IP 白名單、Nginx 路由限制）
- **LINE Webhook 驗證：** 由 LINE Bot SDK 自動驗證 Channel Secret 簽章

### 11.2 授權模型

- **授權方式：** 無（程式碼層無 RBAC/ABAC）
- **實際權限控制：** No

---

## 12. 效能與監控

### 12.1 效能基準

[無實際測量數據]

### 12.2 監控告警

[無監控系統設定]

### 12.3 Log 規範

| 事件 | Log Level | 必要欄位 | 範例 |
|------|-----------|---------|------|
| LINE 文字訊息收到 | INFO | userId, text | `Received message from user: U123, text: 留言應援牆` |
| LINE Postback 收到 | INFO | userId, data | `Received postback from user: U123, data: A` |
| LINE 回覆失敗 | ERROR | replyToken, exception | `Failed to send reply for token: xxx` |
| 未預期錯誤 | ERROR | exception stack trace | `Unexpected error occurred` |
| 資源不存在 | WARN | message | `Resource not found: ...` |
| 業務邏輯錯誤 | WARN | message | `Business exception: ...` |

Log level 設定：`com.wedding.backend: DEBUG`（開發），`org.springframework.web: INFO`

### 12.4 Health Check

- **Endpoint：** `GET /actuator/health`
- **檢查項目：** DB 連線（Spring Boot Actuator 自動）
- **其他暴露端點：** `/actuator/info`, `/actuator/metrics`

---

## 13. 開發指引

### 13.1 本地環境建置

```bash
# 前置需求
# - Java 17
# - Maven
# - MySQL 8.0（本地或 Docker）
# - 建立 wedding_bot 資料庫

# 設定本地設定（gitignored）
cp application.yml.example src/main/resources/config/application.yml
# 填入 LINE Bot credentials、R2 credentials

# 啟動服務
mvn spring-boot:run

# 服務埠號：222
# Swagger UI：http://localhost:222/swagger-ui.html
```

### 13.2 環境差異

| 設定項目 | Development           | Production |
|---------|-----------------------|------------|
| server.port | 222                   | **         |
| DB host | localhost:3306        | **         |
| CORS origins | http://localhost:3000 | **         |
| LINE credentials | 測試用 channel           | 正式 channel |
| R2 bucket | **                    | **         |

### 13.3 測試策略

| 測試類型 | 覆蓋範圍 | 工具 | 覆蓋率要求 | 執行時機 |
|---------|------|------|-------|---------|
| 單元測試 | No   | JUnit 5 + Mockito | No    | mvn test |

### 13.4 部署流程

[待補充]

### 13.5 新增 LINE Bot 關鍵字

1. 在 `LineWebhookController` 的 `initKeywordHandlers()` 新增 `register(zhKeyword, enKeyword, this::handler)` 呼叫
2. 實作對應的 handler 方法
3. 在 `messages.properties`（英文）和 `messages_zh_TW.properties`（繁體中文）新增 i18n key

---

## 14. 變更紀錄

| 日期 | 版本 | 變更內容 | 變更人 |
|------|------|---------|--------|
| 2026-03-18 | 1.0.0 | 初版建立 | spec-keeper auto-generated |
