# File Formats & State Schemas

In strict accordance with **ADR-0009**, Omniwrench stores state using **atomic, granular JSON files with no arrays** to prevent file corruption, support concurrent parallel writes, and optimize incremental syncing.

## 1. Session Metadata Format (`.omniwrench/sessions/{sessionId}/meta.json`)

```json
{
  "sessionId": "ce3c0523-4697-473c-b8f9-eb5620002282",
  "workspaceRoot": "/home/vortigern/git/omniwrench",
  "createdAt": "2026-08-22T20:30:00.123Z",
  "activeEpochId": "epoch-001",
  "turnCount": 42
}
```

## 2. Individual Turn Message (`.omniwrench/sessions/{sessionId}/turns/0001_msg-101.json`)

```json
{
  "messageId": "msg-101",
  "sessionId": "ce3c0523-4697-473c-b8f9-eb5620002282",
  "turnIndex": 1,
  "role": "assistant",
  "content": "Running test suite on omniwrench-core...",
  "timestamp": "2026-08-22T20:30:02.456Z"
}
```

## 3. Tool Invocation Output Record (`.omniwrench/sessions/{sessionId}/turns/0001_msg-101_tool-01.json`)

```json
{
  "invocationId": "tool-01",
  "messageId": "msg-101",
  "toolName": "run_command",
  "arguments": "{\"command\":\"mvn test -pl omniwrench-core\"}",
  "success": true,
  "exitCode": 0,
  "output": "Tests run: 4, Failures: 0, Errors: 0, Skipped: 0",
  "executedAt": "2026-08-22T20:30:03.789Z"
}
```

## 4. Task Plan & Step Checkpoint (`.omniwrench/tasks/TSK-20260822-001/steps/step-01.json`)

```json
{
  "stepId": "step-01",
  "taskId": "TSK-20260822-001",
  "description": "Compile parent BOM and sub-module POM files",
  "status": "COMPLETED",
  "safetyLevel": 2,
  "outputSummary": "Parent and 6 child POMs compiled cleanly",
  "executedAt": "2026-08-22T20:31:00.000Z"
}
```

## 5. Theme Definition (`.omniwrench/themes/cyberpunk.json`)

```json
{
  "themeName": "cyberpunk",
  "background": "#0d0f18",
  "primary": "#00ffcc",
  "accent": "#ff007f",
  "secondary": "#7928ca",
  "warning": "#ffb86c",
  "success": "#50fa7b",
  "text": "#f8f8f2"
}
```

## 6. MCP Server Registry (`.omniwrench/mcp-servers.json`)

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
      }
    },
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres", "postgresql://localhost/omniwrench"]
    }
  }
}
```

