# REST & WebSocket API Specifications

Omniwrench exposes a high-performance REST and WebSocket API secured via `X-Api-Key` and optional JWT bearer tokens (**ADR-0012**).

## 1. Authentication & Security Headers

All requests to `/api/**` and WebSocket handshakes require the API key header:
```http
X-Api-Key: sk-omniwrench-local-key-xyz123
Authorization: Bearer <jwt-token> (optional)
```

## 2. REST API Endpoints

### System Health & Telemetry
`GET /api/v1/status`
- **Response**: `200 OK`
```json
{
  "application": "omniwrench",
  "version": "0.1.0-SNAPSHOT",
  "status": "HEALTHY",
  "mode": "dual",
  "activeSessions": 1,
  "registeredTools": 7,
  "activeEpoch": "epoch-001",
  "uptimeSeconds": 1420,
  "timestamp": "2026-08-22T20:45:00Z"
}
```

### Tool Registry Discovery
`GET /api/v1/tools`
- **Response**: `200 OK` (list of registered `ToolDefinition` objects including schema parameters).

### Conversation Sessions & Prompts
- `GET /api/v1/sessions` — List active sessions.
- `POST /api/v1/sessions` — Create a new session with isolated workspace.
- `GET /api/v1/sessions/{sessionId}/history` — Retrieve session turns and tool logs.
- `POST /api/v1/sessions/{sessionId}/prompt` — Submit prompt to the reasoning engine:
```json
{
  "prompt": "/refactor OrderService",
  "stream": true,
  "modelTier": "AUTO"
}
```

### Task Plan & Checkpoints
- `GET /api/v1/tasks/{taskId}` — Get task status, steps DAG, and execution summary.
- `POST /api/v1/tasks/{taskId}/resume` — Resume an interrupted task from the last atomic checkpoint (`ADR-0021`).

### Subagent Swarm Management
- `POST /api/v1/swarm/delegate` — Spawn a dynamic swarm for a delegated goal (`ADR-0017`).
- `GET /api/v1/swarm/active` — List live virtual-thread swarm worker actors.

### Model Context Protocol (MCP) SSE Route
- `GET /mcp/sse` — MCP SSE transport stream endpoint (`ADR-0036`).
- `POST /mcp/message` — MCP JSON-RPC message receiver.

## 3. Real-Time WebSocket & STOMP Protocol

- **Connect URL**: `ws://localhost:8080/ws/telemetry`
- **Subscription Topics**:
  - `/topic/telemetry/reasoning/{sessionId}`: Real-time LLM token streaming and reasoning steps.
  - `/topic/telemetry/tools/{sessionId}`: Live tool invocation outputs, exit codes, and diffs.
  - `/topic/telemetry/swarm`: Multi-agent swarm consensus voting rounds and messages.
  - `/topic/telemetry/ha`: Home Assistant real-time entity state changes and events.
  - `/topic/telemetry/system`: CPU, virtual-thread pool saturation, and memory telemetry.

