# Module & Package Dependencies

The following diagram illustrates Maven module dependencies and package boundaries within Omniwrench (per ADR-0016):

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff
skinparam componentStyle uml2

package "omniwrench-app (Assembly)" as MOD_APP {
  [OmniwrenchApplication] as APP_MAIN
}

package "omniwrench-tui (Presentation)" as MOD_TUI {
  [OmniwrenchTuiDashboard] as TUI_DASH
  [TerminalRenderer] as TUI_REND
  [TuiRunner] as TUI_RUN
}

package "omniwrench-web (Presentation)" as MOD_WEB {
  [AgentController] as REST_CTRL
  [StatusController] as STAT_CTRL
  [WebSocketConfig] as WS_CFG
}

package "omniwrench-ai (AI SPI & Adapters)" as MOD_AI {
  [BackendAdapter] as AI_SPI
  [MediaType] as AI_MEDIA
  [ExecutionMode] as AI_MODE
  [ModelRequest] as AI_REQ
  [ModelResponse] as AI_RESP
}

package "omniwrench-tools (Tool Implementations)" as MOD_TOOLS {
  [FileOperationsTool] as FILE_TOOL
  [CommandExecutionTool] as CMD_TOOL
}

package "omniwrench-core (Engine & Domain)" as MOD_CORE {
  [AgentEngine] as ENGINE
  [SessionManager] as SESS_MGR
  [ToolRegistry] as TOOL_REG
  [Tool] as TOOL_SPI
  [SessionContext] as SESS_CTX
  [AgentMessage] as MSG_MODEL
  [OmniwrenchProperties] as PROPS
}

MOD_APP --> MOD_CORE
MOD_APP --> MOD_TOOLS
MOD_APP --> MOD_AI
MOD_APP --> MOD_TUI
MOD_APP --> MOD_WEB

MOD_TUI --> MOD_CORE
MOD_WEB --> MOD_CORE
MOD_TOOLS --> MOD_CORE
MOD_AI --> MOD_CORE
@enduml
```

## Coupling & Design Principles
- **Clean Inversion**: The `omniwrench-core` module contains pure engine logic, domain models, and SPIs without heavy runtime dependencies.
- **Pluggable AI SPI**: The `omniwrench-ai` module defines the `BackendAdapter` and `MediaType` sealed hierarchy without framework lock-in (ADR-0015).
- **Pluggable Tools**: The `omniwrench-tools` module implements the `Tool` SPI and is discovered dynamically via Spring or `ServiceLoader` (ADR-0010).
- **Dual Presentation**: Both `omniwrench-tui` and `omniwrench-web` act as peer presentation consumers of `omniwrench-core` (ADR-0001, ADR-0011).

