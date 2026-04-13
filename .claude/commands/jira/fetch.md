---
name: "Jira: Fetch Requirement"
description: 從 Jira 拉取需求描述，萃取 Summary / Description / Acceptance Criteria
category: Workflow
tags: [jira, workflow, requirements]
allowed-tools: mcp__atlassian__getAccessibleAtlassianResources, mcp__atlassian__getJiraIssue, AskUserQuestion
---

## Input

Ticket number: $ARGUMENTS

## Task

從 Jira 拉取需求描述，整理出核心內容供後續工作流程ㄙㄟ


### Step 1: 確認 ticket number

若 `$ARGUMENTS` 為空，用 AskUserQuestion 詢問：
> 請輸入 Jira ticket number（例如：KAN-1）

### Step 2: 取得 cloudId

呼叫 `getAccessibleAtlassianResources`，取得 cloudId。

### Step 3: 取得 issue 內容

呼叫 `getJiraIssue`，傳入 cloudId 與 ticket number。

### Step 4: 萃取關鍵欄位

從 issue 中**只**萃取以下欄位：

- **Summary**（標題）
- **Description**（需求描述核心段落）
- **Acceptance Criteria**（若有）

**忽略**以下欄位，不輸出任何相關內容：
- Comments
- Changelog / 狀態變更歷史
- Worklog / 時間紀錄
- Attachments
- Assignee、Reporter 等人員資訊

### Step 5: 處理超長 Description

若 Description 超過 2000 字元，將其摘要為重點條列（bullet points），保留核心需求，省略細節贅述。

### Step 6: 輸出需求摘要

以以下格式輸出整理後的內容：

```
## [TICKET-NUMBER] Summary 標題

### Description
（核心需求描述或摘要）

### Acceptance Criteria
（若有，逐條列出；若無，標注「無」）
```
