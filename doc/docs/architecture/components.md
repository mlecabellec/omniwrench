# System Components

Omniwrench segregates duties cleanly between presentation, reasoning, tool execution, and session management.

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff
skinparam componentStyle uml2

package "Presentation Layer" {
  [OmniwrenchTuiDashboard] as TUI_DASH
  [TerminalRenderer] as TUI_REND
  [AgentController] as REST_CTRL
  [StatusController] as STAT_CTRL
  [WebSocketConfig] as WS_CFG
}

package "Core Domain & Engine" {
  [AgentEngine] as ENGINE
  [SessionManager] as SESS_MGR
  [ToolRegistry] as TOOL_REG
}

package "Tooling Plugins" {
  [FileOperationsTool] as FILE_TOOL
  [CommandExecutionTool] as CMD_TOOL
}

package "Domain Models" {
  [AgentMessage] as MSG
  [ToolDefinition] as TOOL_DEF
  [ToolInvocation] as TOOL_INV
  [SessionContext] as CTX
}

TUI_DASH --> TUI_REND : Formats ANSI
TUI_DASH --> ENGINE : Dispatches
REST_CTRL --> ENGINE : Dispatches
ENGINE --> TOOL_REG : Resolves Tools
ENGINE --> SESS_MGR : Updates State
TOOL_REG --> FILE_TOOL : Dispatches
TOOL_REG --> CMD_TOOL : Dispatches

ENGINE ..> MSG : Produces
ENGINE ..> CTX : Manages
TOOL_REG ..> TOOL_DEF : Registers
FILE_TOOL ..> TOOL_INV : Returns
@enduml
```

## Component Descriptions

1. **OmniwrenchTuiDashboard**: Provides an interactive terminal console with live status headers, prompt input, and colored chat panels.
2. **TerminalRenderer**: Low-level ANSI styling engine rendering glowing neon borders and status pills.
3. **AgentEngine**: Coordinates the reasoning cycle, command dispatches, and tool invocations within bounded thread limits.
4. **ToolRegistry**: Dynamic, thread-safe SPI registry storing registered tool capabilities.
5. **SessionManager**: Isolates session conversations and maps workspace roots.
6. **FileOperationsTool**: Safe filesystem operations supporting read, write, exists, and list actions.
7. **CommandExecutionTool**: Bounded shell process executor with timeout and output capture.
