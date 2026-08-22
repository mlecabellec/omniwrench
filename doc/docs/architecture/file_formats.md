# File & State Formats

## 1. Session Memory Manifest Format (`session.json`)

```json
{
  "sessionId": "aa61b31d-0941-488c-b1ca-c48173cc9dee",
  "workspaceRoot": "/home/vortigern/git/omniwrench",
  "createdAt": "2026-08-22T14:30:00Z",
  "messages": [
    {
      "id": "msg-001",
      "role": "user",
      "content": "Initialize project structure",
      "timestamp": "2026-08-22T14:30:01Z",
      "toolInvocations": []
    },
    {
      "id": "msg-002",
      "role": "assistant",
      "content": "Project structure initialized successfully.",
      "timestamp": "2026-08-22T14:30:02Z",
      "toolInvocations": [
        {
          "callId": "call-101",
          "toolName": "file_ops",
          "arguments": {"action": "write", "path": "pom.xml"},
          "output": "Successfully written 4500 bytes",
          "success": true,
          "executedAt": "2026-08-22T14:30:02Z"
        }
      ]
    }
  ]
}
```
