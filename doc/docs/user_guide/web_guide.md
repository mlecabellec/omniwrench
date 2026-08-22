# Web & Telemetry Dashboard

Omniwrench embeds a high-throughput Spring Web & WebSocket server for remote monitoring, telemetry visualization, and browser-based agent pairing.

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

participant "Web Browser" as B
participant "REST Controller (/api/v1)" as R
participant "WebSocket Stream (/ws/agent-stream)" as WS
participant "Omniwrench Core" as C

B -> R : GET /api/v1/status
R --> B : 200 OK {status: "HEALTHY", activeSessions: 1, registeredTools: 2}

B -> WS : Connect WebSocket
WS --> B : Connection Established

B -> R : POST /api/v1/sessions/{id}/prompt {"prompt":"Run build"}
R -> C : Dispatch to AgentEngine
C --> WS : Stream step updates & tool events
C --> R : Return final AgentMessage
R --> B : 200 OK (JSON AgentMessage)
@enduml
```

## REST Endpoints
- `GET /api/v1/status`: Health check, active sessions, registered tool count, JVM version.
- `GET /api/v1/tools`: List of registered tool definitions and schemas.
- `GET /api/v1/sessions/{id}/messages`: Full conversation transcript for a session.
- `POST /api/v1/sessions/{id}/prompt`: Submit a prompt to the agent reasoning engine.
