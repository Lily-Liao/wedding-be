# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Wedding Interactive System Backend** — Java Spring Boot service paired with a React frontend. Full requirements spec: `wedding-be-prd/BE System 30a3067bca0580518c81cd0a8049cdf7.md` (Chinese).

### 預設工作流程：OpenSpec + Superpowers
收到實作任務（新功能、bug 修復、重構、程式碼修改、對齊需求 {ticket-number} ）時，開始工作前依序確認以下這幾件事：

### 第一步：確認流程

> 要使用 **OpenSpec + Superpowers** 流程嗎？

- **是**：進入第二步
- **否**：直接以標準方式進行
- **瑣碎任務**（改 typo、一行修改、簡單問答）：跳過詢問，直接進行

### 第二步：確認規模

根據任務複雜度建議並等使用者確認：

- **小型流程**（`opsx:ff`）：一次產生所有 artifact 後直接實作。適合範圍明確、改動不大的任務
- **大型流程**（`opsx:new` → `opsx:continue`）：逐步產生 artifact，每步可調整。適合複雜、需要多輪討論的任務

### 第三步：確認推進模式

- **逐步確認**：每個 skill 結束後等使用者說「繼續」再推進
- **自動推進**：做完一步直接下一步，只在關鍵點暫停

> **注意：三步確認階段只問以上三件事。** 不要在確認階段詢問可選步驟（如 `jira:fetch`、`environment:check`），這些屬於下方執行階段，進入執行時再依序詢問。

### 核心流程

三步確認完成後進入執行階段（`[]` 為可選步驟，執行到該步驟時再詢問是否需要）：

**首先選擇是否要執行環境安裝檢查:**
[environment:check]

**小型：**
[jira:fetch] -> superpowers:brainstorming -> opsx:ff → opsx:apply → [simplify] → superpowers:verification-before-completion → opsx:verify → opsx:archive

**大型：**
[jira:fetch] -> superpowers:brainstorming -> opsx:new → opsx:continue（重複）→ superpowers:writing-plans → opsx:apply → [simplify] → superpowers:verification-before-completion → opsx:verify → opsx:archive

## Commands

```bash
mvn spring-boot:run                          # start server (port 8080)
mvn compile                                  # compile only
mvn test                                     # all tests
mvn test -Dtest=LineWebhookControllerTest   # single test class
```

Local config override lives in `src/main/resources/config/application.yml` (gitignored, not `.env`).

## Tech Stack

- **Java 17**, Spring Boot 3.2.3, MySQL 8.0
- **ORM:** JPA/Hibernate 6 with Flyway migrations (`V1`–`V5` in `src/main/resources/db/migration/`)
- **Real-time:** WebSocket + STOMP (`/topic/messages` only — vote updates use REST polling)
- **External:** LINE Messaging API (SDK 8.4.x — uses Java **records**, no `.builder()`)
- **Storage:** Cloudflare R2 via AWS SDK v2 S3 (`software.amazon.awssdk:s3`)
- **Docs:** SpringDoc OpenAPI at `http://localhost:8080/swagger-ui.html`

## Architecture: Four Core Modules

### 1. Wedding Wall (`/api/messages`)
- LINE Webhook → guest messages → DB → WebSocket push
- `GET /api/messages` — all messages

### 2. Interactive Voting
- Three states: `WAITING` → `START` → `CLOSED` (`PATCH /admin/voting-session/status`)
- `GET /api/votes/options` — options A/B/C/D with counts, percentages, and `correctAnswer`
- `PUT /admin/voting-session/options` — customise label/color per option (WAITING only)
- `correct_answer` column on `voting_sessions` — written directly to DB, not via API
- No duplicate votes per LINE user; validated server-side
- LINE Bot sends FlexMessage with PostbackAction buttons when voting is active; handles both PostbackEvent (button tap) and text input fallback

### 3. Lucky Draw (`/api/v1/`)
- Draws only from voters who answered correctly (`correct_answer` must be set in DB first)
- Once drawn, a user is permanently excluded from the pool (even after cancellation)
- `POST /api/v1/winners` — random draw; `DELETE /api/v1/winners/{id}` — cancel (no re-entry)
- `DELETE /api/v1/winners` — reset all winners, restores full eligible pool

### 4. Media Schemes (`/media/schemes`)
- Playlists of images/videos for the display wall; one scheme is "live" at a time
- **Upload flow:** `POST /media/schemes/{id}/items/presign` → returns `uploadUrl` (presigned PUT, 60-min expiry) + `readUrl` (permanent public URL stored in DB)
- `readUrl = r2Properties.publicUrl + "/" + fileKey` — requires R2 bucket Public Access enabled

## LINE Bot Keyword Dispatch

`LineWebhookController` uses a `Map<String, HandlerEntry>` (registered in `@PostConstruct initKeywordHandlers()`) instead of if-chains. Each entry holds `(KeywordHandler handler, Locale locale)`.

**Supported keywords — both Chinese and English registered for every trigger:**

| Chinese | English | Action |
|---|---|---|
| 留言應援牆 | Message Wall | → `AWAITING_MESSAGE` state |
| 限時投票 | Live Poll | → `AWAITING_VOTE` state (if voting is `START`) |
| 席位查詢 | Reserved Spot | → `AWAITING_SEAT_QUERY` state |
| 婚禮資訊 | Show Info | one-shot reply |
| 精選片段 | Highlights | one-shot reply |

**Locale handling:** Chinese keywords → `Locale.TAIWAN`, English keywords → `Locale.ENGLISH`. Locale is persisted in `LineUserState.stateData` (`Map<String, Object>` JSON column, key `"locale"`) so multi-turn AWAITING flows reply in the same language. Cancel keywords: `"取消"` and `"Cancel"` both accepted.

**Adding a new keyword:** add one `register(zhKeyword, enKeyword, this::handler)` call in `initKeywordHandlers()`, implement the handler method, add i18n keys to both `messages.properties` and `messages_zh_TW.properties`.

## Key Patterns

### YAML list binding — use `@ConfigurationProperties`, not `@Value`
`@Value` cannot bind a YAML list to `List<String>`. Use `CorsProperties` (existing pattern) for list-valued config.

### Hibernate 6 + MySQL column types
Always add explicit `columnDefinition` to avoid Hibernate 6 type mismatches with MySQL:
- `UUID` PK → `columnDefinition = "char(36)"` **+** `hibernate.type.preferred_uuid_jdbc_type: CHAR` in config (columnDefinition 只影響 DDL，不影響 JDBC binding；少了後者會寫入 binary 亂碼)
- `@Enumerated(STRING)` → `columnDefinition = "varchar(N)"` (NOT `enum`)
- `char(1)` fields → `columnDefinition = "char(1)"`
- JSON fields → `columnDefinition = "json"`

### LINE SDK 8.x (Java records — no builders)
```java
new TextMessage("text")
new TextMessage(quickReply, null, "text", null, null)   // with QuickReply
new QuickReplyItem(new MessageAction("label", "text"))
new ReplyMessageRequest(replyToken, messages, false)
```

### Flyway
`baseline-on-migrate: true`, `baseline-version: '0'` — required because the schema was pre-existing when Flyway was introduced.

## i18n

`MessageSource` with `AcceptHeaderLocaleResolver`. Files: `messages.properties` (English fallback) + `messages_zh_TW.properties`. Default locale: `Locale.TAIWAN`.
