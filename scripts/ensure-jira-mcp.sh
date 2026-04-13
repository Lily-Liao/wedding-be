#!/usr/bin/env bash
# ensure-jira-mcp.sh — 檢查/設定 Jira MCP (Atlassian) 連線
# Exit code: 0 = 成功, 1 = 需手動處理

set -euo pipefail

MCP_FILE=".mcp.json"
SETTINGS_FILE=".claude/settings.local.json"

# --- Step 1: 檢查 .mcp.json 是否存在且包含 atlassian ---
if [ -f "$MCP_FILE" ]; then
    if grep -q '"atlassian"' "$MCP_FILE" 2>/dev/null; then
        echo "[jira-mcp] .mcp.json already contains atlassian server config."
    else
        echo "[jira-mcp] .mcp.json exists but missing atlassian config."
        echo "[jira-mcp] NEEDS_MCP_CONFIG"
        exit 1
    fi
else
    echo "[jira-mcp] .mcp.json not found."
    echo "[jira-mcp] NEEDS_MCP_CONFIG"
    exit 1
fi

# --- Step 2: 檢查 .claude/settings.local.json ---
if [ -f "$SETTINGS_FILE" ]; then
    if grep -q '"atlassian"' "$SETTINGS_FILE" 2>/dev/null; then
        echo "[jira-mcp] settings.local.json already has atlassian enabled."
    else
        echo "[jira-mcp] settings.local.json exists but atlassian not enabled."
        echo "[jira-mcp] NEEDS_SETTINGS_UPDATE"
        exit 1
    fi
else
    echo "[jira-mcp] settings.local.json not found."
    echo "[jira-mcp] NEEDS_SETTINGS_UPDATE"
    exit 1
fi

echo ""
echo "[jira-mcp] Done. Jira MCP is ready."
exit 0
