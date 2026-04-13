---
allowed-tools: Bash, AskUserQuestion, Read, Edit, Write
---

# Check Plugins Environment

檢查 OpenSpec、Superpowers、Jira MCP、Code Simplifier 是否已安裝並正確設定。逐步確認，缺少時詢問使用者是否要安裝。

## Task

依序執行以下四個檢查腳本，每一步先回報狀態，若未安裝則用 AskUserQuestion 詢問使用者是否要安裝，再執行安裝。

---

### Step 1: OpenSpec CLI

執行檢查腳本：

```bash
bash scripts/ensure-openspec.sh
```

- **成功（exit 0）**：回報已安裝及版本，進入 Step 2
- **失敗（exit 非 0）**：向使用者顯示錯誤訊息，用 AskUserQuestion 詢問：
  > OpenSpec CLI 尚未安裝，是否要現在安裝？
    - **是**：執行 `npm install -g @fission-ai/openspec`，然後重新執行 `bash scripts/ensure-openspec.sh`
    - **否**：標記為跳過，繼續下一步

---

### Step 2: Superpowers Plugin

執行檢查腳本：

```bash
bash scripts/ensure-superpowers.sh
```

- **成功（exit 0）**：回報已安裝，進入 Step 3
- **失敗（exit 1）**：Superpowers 是 Claude Code plugin，無法透過 bash 自動安裝。用 AskUserQuestion 詢問：
  > Superpowers 尚未安裝。這是 Claude Code plugin，需透過內建指令安裝。是否要顯示安裝步驟？
    - **是**：顯示以下安裝指令，請使用者手動執行：
      ```
      方法一（官方 marketplace）：
        /plugin install superpowers@claude-plugins-official
  
      方法二（社群 marketplace）：
        /plugin marketplace add obra/superpowers-marketplace
        /plugin install superpowers@superpowers-marketplace
      ```
      提醒：安裝後需重啟 Claude Code session 才會生效。
    - **否**：標記為跳過，繼續下一步

---

### Step 3: Jira MCP (Atlassian)

執行檢查腳本：

```bash
bash scripts/ensure-jira-mcp.sh
```

- **成功（exit 0）**：回報已設定，進入 Step 4
- **失敗（exit 1）**：用 AskUserQuestion 詢問：
  > Jira MCP 尚未設定完成，是否要現在設定？
    - **是**：根據腳本輸出判斷缺少的部分：
        - 若輸出包含 `NEEDS_MCP_CONFIG`：建立或更新 `.mcp.json`，加入：
          ```json
          {
            "mcpServers": {
              "atlassian": {
                "type": "http",
                "url": "https://mcp.atlassian.com/v1/mcp"
              }
            }
          }
          ```
        - 若輸出包含 `NEEDS_SETTINGS_UPDATE`：讀取 `.claude/settings.local.json`，確保包含：
          ```json
          "enableAllProjectMcpServers": true,
          "enabledMcpjsonServers": ["atlassian"]
          ```
        - 設定完成後重新執行 `bash scripts/ensure-jira-mcp.sh` 驗證
    - **否**：標記為跳過，繼續下一步

---

### Step 4: Code Simplifier Plugin

執行檢查腳本：

```bash
bash scripts/ensure-code-simplifier.sh
```

- **成功（exit 0）**：回報已安裝，進入總結
- **失敗（exit 1）**：Code Simplifier 是 Claude Code plugin，無法透過 bash 自動安裝。用 AskUserQuestion 詢問：
  > Code Simplifier 尚未安裝。這是 Claude Code plugin，需透過內建指令安裝。是否要顯示安裝步驟？
    - **是**：顯示以下安裝指令，請使用者手動執行：
      ```
      /plugin install code-simplifier@claude-plugins-official
      ```
      提醒：安裝後需重啟 Claude Code session 才會生效。
    - **否**：標記為跳過，繼續總結

---

### Step 5: 輸出總結

以表格形式輸出最終結果：

| Plugin | 狀態 | 備註 |
|--------|------|------|
| OpenSpec CLI | ✅ / ❌ / ⏭️ 跳過 | 版本號或錯誤訊息 |
| Superpowers | ✅ / ⚠️ 需手動安裝 / ⏭️ 跳過 | 安裝狀態 |
| Jira MCP | ✅ / ❌ / ⏭️ 跳過 | 設定狀態 |
| Code Simplifier | ✅ / ⚠️ 需手動安裝 / ⏭️ 跳過 | 安裝狀態 |

- 若全部就緒：「環境準備完成，可以開始使用 OpenSpec + Superpowers 工作流程。」
- 若有跳過或失敗：列出需要後續處理的項目
