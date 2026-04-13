#!/usr/bin/env bash
# ensure-superpowers.sh — 檢查 Superpowers plugin 安裝狀態
# Plugin 實際載入是從 ~/.claude/plugins/cache/ 讀取
# Exit code: 0 = 已安裝, 1 = 未安裝（需手動安裝）

set -euo pipefail

CLAUDE_DIR="${HOME}/.claude"
INSTALLED_PLUGINS="${CLAUDE_DIR}/plugins/installed_plugins.json"
CACHE_DIR="${CLAUDE_DIR}/plugins/cache"
PLUGIN_NAME="superpowers"

# --- 方法一：檢查 cache 目錄是否存在 plugin 檔案 ---
CACHE_MATCH=$(find "${CACHE_DIR}" -mindepth 2 -maxdepth 2 -type d -name "${PLUGIN_NAME}" 2>/dev/null | head -1)

if [ -n "${CACHE_MATCH}" ]; then
    # 取得最新版本目錄
    VERSION_DIR=$(ls -1d "${CACHE_MATCH}/"*/ 2>/dev/null | sort -V | tail -1)
    if [ -n "${VERSION_DIR}" ] && [ -d "${VERSION_DIR}" ]; then
        VERSION=$(basename "${VERSION_DIR}")
        echo "[${PLUGIN_NAME}] Plugin found in cache (v${VERSION})"
        echo "[${PLUGIN_NAME}] Cache path: ${CACHE_MATCH}/${VERSION}"
        echo ""
        echo "[${PLUGIN_NAME}] Superpowers is installed."
        exit 0
    fi
fi

# --- 方法二：fallback 檢查 installed_plugins.json ---
if [ -f "${INSTALLED_PLUGINS}" ]; then
    if grep -qi "${PLUGIN_NAME}" "${INSTALLED_PLUGINS}" 2>/dev/null; then
        echo "[${PLUGIN_NAME}] Plugin found in installed_plugins.json but cache files missing."
        echo "[${PLUGIN_NAME}] Try reinstalling the plugin."
        exit 1
    fi
fi

# --- 未安裝 ---
echo "[${PLUGIN_NAME}] Superpowers is NOT installed."
echo ""
echo "Superpowers 是 Claude Code plugin，需透過 Claude Code 內建指令安裝："
echo ""
echo "  方法一（官方 marketplace）："
echo "    /plugin install superpowers@claude-plugins-official"
echo ""
echo "  方法二（社群 marketplace）："
echo "    /plugin marketplace add obra/superpowers-marketplace"
echo "    /plugin install superpowers@superpowers-marketplace"
echo ""
echo "  安裝後需重啟 Claude Code session。"
exit 1
