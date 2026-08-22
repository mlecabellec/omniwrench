# API Specification

## 1. REST API Endpoints

### Status Check
`GET /api/v1/status`
- **Response**: `200 OK`
```json
{
  "application": "omniwrench",
  "version": "0.1.0-SNAPSHOT",
  "status": "HEALTHY",
  "mode": "dual",
  "activeSessions": 1,
  "registeredTools": 2,
  "timestamp": "2026-08-22T14:30:00Z"
}
```

### List Tools
`GET /api/v1/tools`
- **Response**: `200 OK` with array of `ToolDefinition`.

### Submit Prompt
`POST /api/v1/sessions/{sessionId}/prompt`
- **Body**: `{"prompt": "string"}`
- **Response**: `200 OK` with resulting `AgentMessage`.

## 2. WebSocket Streaming Protocol
- **Endpoint**: `/ws/agent-stream`
- Transmits JSON events for streaming tokens, reasoning step updates, and tool execution logs.
