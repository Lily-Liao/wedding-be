# Wedding Interactive System — Backend

婚禮互動系統後端，提供留言牆、互動投票、幸運抽獎、媒體管理等功能，透過 LINE Messaging API 與賓客互動，並搭配 React 前端展示。

---

## Tech Stack

| 層級 | 技術 |
|---|---|
| 語言 / 框架 | Java 17、Spring Boot 3.2.3 |
| 資料庫 | MySQL 8.0（JPA/Hibernate 6、Flyway 自動 migration） |
| 即時推播 | WebSocket + STOMP（留言牆） |
| LINE 整合 | LINE Messaging API SDK 8.4.x |
| 雲端儲存 | Cloudflare R2（AWS SDK v2 S3 相容） |
| API 文件 | SpringDoc OpenAPI（Swagger UI） |

---

## 快速啟動

### 前置需求

- Java 17+
- Maven 3.8+
- MySQL 8.0（建立 database `wedding_bot`）
- LINE Bot Channel（取得 Access Token 與 Channel Secret）
- Cloudflare R2 Bucket（需開啟 Public Access）

### 設定本地 Config

在 `src/main/resources/config/` 建立 `application.yml`（此目錄已加入 `.gitignore`）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/wedding_bot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei&characterEncoding=UTF-8
    username: your_db_user
    password: your_db_password

line:
  bot:
    channel-token: your_line_channel_access_token
    channel-secret: your_line_channel_secret

cloudflare:
  r2:
    account-id: your_account_id
    access-key-id: your_access_key_id
    secret-access-key: your_secret_access_key
    bucket-name: your_bucket_name
    public-url: https://your-r2-public-url

cors:
  allowed-origins:
    - http://localhost:3000
```

### 啟動

```bash
mvn spring-boot:run        # 啟動伺服器（port 222）
mvn compile                # 僅編譯
mvn test                   # 執行所有測試
```

啟動後 Flyway 會自動執行 `V1`–`V7` migration 建立所有資料表。

**Swagger UI：** `http://localhost:222/swagger-ui.html`

---

## 系統架構

### 四大模組

```
LINE Bot Webhook
      │
      ├─ 留言牆 ──────────── DB(messages) ──▶ WebSocket push ──▶ 前端
      │
      ├─ 互動投票 ─────────── DB(votes) ──────────────────────▶ 前端(REST輪詢)
      │
      ├─ 席位查詢 ─────────── (TODO)
      │
      └─ 精選片段 ─────────── Google Drive
```

### 1. 留言牆 `Wedding Wall`

賓客透過 LINE 傳送文字，後端儲存後透過 WebSocket 即時推播到前端大螢幕。

**LINE Bot 互動流程：**

```
賓客輸入「留言應援牆」
  └─ 回覆提示 + QuickReply（取消 / 新郎好帥 / 新娘好美 / 恭喜恭喜 / 太感動了）
        ├─ 輸入內容 → 儲存 DB → 回覆確認 → WebSocket 推播
        └─ 輸入「取消」→ 不儲存，回覆取消訊息
```

**API：**
- `GET /api/messages` — 取得所有留言（含 `pictureUrl`）

**WebSocket Topic：** `/topic/messages` → `message:new` 事件

---

### 2. 互動投票 `Interactive Voting`

三段式狀態切換，賓客透過 LINE FlexMessage 按鈕投票，每人限投一次。

**狀態流程：** `WAITING` → `START` → `CLOSED`（單向，不可逆）

**LINE Bot 互動流程：**

```
賓客輸入「限時票選」
  ├─ WAITING → 回覆「尚未開放」
  ├─ START   → 回覆 FlexMessage（A/B/C/D 色系按鈕，PostbackAction）
  │               └─ 點選按鈕 / 輸入 A-D → 記錄投票 → 回覆確認
  └─ CLOSED  → 回覆「已結束」
```

**API：**
- `GET /api/votes/options` — 取得各選項票數、百分比、正確答案
- `PATCH /admin/voting-session/status` — 切換投票狀態（Admin）
- `PUT /admin/voting-session/options` — 設定選項文字與顏色（WAITING 狀態）

**正確答案：** 直接寫入 DB `voting_sessions.correct_answer`（不透過 API）

---

### 3. 幸運抽獎 `Lucky Draw`

從**答對的參與者**中隨機抽出幸運兒，抽過的人永久移除出抽獎池（即使取消資格亦不回池）。

**前置條件：** `voting_sessions.correct_answer` 須已設定

**API：**
- `GET /api/v1/participants/eligible` — 查看目前抽獎池
- `POST /api/v1/winners` — 執行抽獎
- `GET /api/v1/winners` — 取得所有中獎記錄
- `DELETE /api/v1/winners/{id}` — 取消中獎資格（不回池）
- `DELETE /api/v1/winners` — 重置所有中獎記錄（可重新抽）

---

### 4. 媒體管理 `Media Schemes`

管理婚禮現場大螢幕的圖片 / 影片輪播方案，同一時間只有一個方案為 live。

**上傳流程：**
1. `POST /media/schemes/{id}/items/presign` — 取得預簽章 URL
2. 前端直接 `PUT` 上傳到 Cloudflare R2

**API：** 詳見 [`docs/FE_API_GUIDE.md`](docs/FE_API_GUIDE.md)

---

## LINE Bot 關鍵字總覽

| 中文關鍵字 | 英文關鍵字 | 功能 |
|---|---|---|
| 留言應援牆 | Message Wall | 進入留言流程 |
| 限時票選 | Live Poll | 進入投票流程 |
| 專屬席位 | Reserved Spot | 席位查詢（開發中） |
| 演出資訊 | Show Info | 回傳場地地圖 |
| 精選片段 | Highlights | 回傳精選照片 Carousel |

取消關鍵字：`取消` / `Cancel`

---

## DB Schema

| 資料表 | 說明 |
|---|---|
| `messages` | 留言牆訊息 |
| `voting_sessions` | 投票 session（含 `options` JSON、`correct_answer`） |
| `votes` | 每筆投票記錄 |
| `winners` | 抽獎中獎記錄 |
| `line_users` | LINE 用戶 profile 快取（displayName、pictureUrl、language） |
| `line_user_states` | LINE Bot 對話狀態機 |
| `media_schemes` | 媒體輪播方案 |
| `media_items` | 方案內的圖片 / 影片素材 |

Migration 檔案位於 `src/main/resources/db/migration/`（V1–V7）。

---

## 文件

- **前端 API 整合指南：** [`docs/FE_API_GUIDE.md`](docs/FE_API_GUIDE.md)
- **開發規範（Claude Code）：** [`CLAUDE.md`](CLAUDE.md)
