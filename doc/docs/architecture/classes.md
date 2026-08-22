# Class Diagrams & Patterns

Omniwrench applies Object-Oriented patterns to manage lifecycle complexity in strict adherence to **CS-0030.6**.

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff
skinparam classAttributeIconSize 0

interface Tool {
  + getDefinition() : ToolDefinition
  + execute(context: SessionContext, args: Map<String, Object>) : ToolInvocation
}

class FileOperationsTool implements Tool {
  - definition : ToolDefinition
  + execute(context: SessionContext, args: Map<String, Object>) : ToolInvocation
}

class CommandExecutionTool implements Tool {
  - definition : ToolDefinition
  + execute(context: SessionContext, args: Map<String, Object>) : ToolInvocation
}

class ToolRegistry {
  - registeredTools : Map<String, Tool>
  + registerTool(tool: Tool) : void
  + getTool(name: String) : Optional<Tool>
  + getAllDefinitions() : List<ToolDefinition>
}

class AgentEngine {
  - toolRegistry : ToolRegistry
  - properties : OmniwrenchProperties
  - agentThreadPool : ExecutorService
  + processPrompt(context: SessionContext, prompt: String) : AgentMessage
}

class SessionContext {
  - sessionId : String
  - workspaceRoot : String
  - messages : List<AgentMessage>
  + addMessage(message: AgentMessage) : void
  + getMessages() : List<AgentMessage>
}

class AgentMessage {
  - id : String
  - role : String
  - content : String
  - timestamp : Instant
  - toolInvocations : List<ToolInvocation>
}

AgentEngine --> ToolRegistry : references
ToolRegistry o-- Tool : aggregates
AgentEngine ..> SessionContext : modifies
SessionContext o-- AgentMessage : contains
@enduml
```

## Pattern Applications
- **Strategy Pattern (`Tool`)**: Concrete execution strategies (`FileOperationsTool`, `CommandExecutionTool`) encapsulated behind a common interface.
- **Factory Pattern (`AgentMessage.of()`, `SessionContext.createDefault()`)**: Standardized, validated factory instantiation methods.
- **Immutability Contract**: Domain data carriers (`AgentMessage`, `ToolDefinition`, `ToolInvocation`) are strictly final with unmodifiable internal collections.
