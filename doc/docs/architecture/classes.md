# Class Diagrams & Architectural Design Patterns

Omniwrench applies robust Object-Oriented and Functional patterns across its domain, engine, AI adapters, and tooling layers in strict adherence to **CS-0030.6**.

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
  + execute(context: SessionContext, args: Map<String, Object>) : ToolInvocation
}

class JavaParserAstTool implements Tool {
  + execute(context: SessionContext, args: Map<String, Object>) : ToolInvocation
}

interface BackendAdapter<T> {
  + getProviderId() : String
  + execute(request: ModelRequest<T>) : ModelResponse<T>
  + executeAsync(request: ModelRequest<T>) : CompletableFuture<ModelResponse<T>>
}

class OpenAiCompatibleAdapter implements BackendAdapter {
  + execute(request: ModelRequest<ChatReasoning>) : ModelResponse<ChatReasoning>
}

class SmartModelRouter {
  - adapters : Map<String, BackendAdapter<?>>
  + route(request: ModelRequest<?>) : BackendAdapter<?>
}

interface ProtocolBridge {
  + getProtocolId() : String
  + connect() : CompletableFuture<Void>
  + publish(message: ProtocolMessage) : CompletableFuture<Boolean>
}

class HomeAssistantTool implements ProtocolBridge, Tool {
  + connect() : CompletableFuture<Void>
  + execute(context: SessionContext, args: Map<String, Object>) : ToolInvocation
}

class ReactorEventBus {
  - eventSink : Sinks.Many<Object>
  + publish(event: Object) : void
  + onEvent(type: Class<E>) : Flux<E>
}

class SwarmCoordinator {
  - activeActors : Map<String, SwarmWorker>
  + broadcast(envelope: SwarmEnvelope) : void
  + initiateConsensus(topic: String) : CompletableFuture<ConsensusResult>
}

class AgentEngine {
  - toolRegistry : ToolRegistry
  - smartRouter : SmartModelRouter
  - eventBus : ReactorEventBus
  - sessionManager : SessionManager
  + processPrompt(context: SessionContext, prompt: String) : AgentMessage
}

AgentEngine --> ToolRegistry : resolves
AgentEngine --> SmartModelRouter : routes
AgentEngine --> ReactorEventBus : publishes
AgentEngine --> SwarmCoordinator : delegates
SmartModelRouter o-- BackendAdapter : delegates
ToolRegistry o-- Tool : aggregates
@enduml
```

## Design Pattern Applications

- **Strategy Pattern (`Tool`, `BackendAdapter`)**: Concrete execution strategies (`FileOperationsTool`, `JavaParserAstTool`, `OpenAiCompatibleAdapter`) encapsulated behind typed SPI contracts.
- **Router Pattern (`SmartModelRouter`)**: Dynamic rule-based request routing optimizing between cloud API cost, latency, and reasoning capability.
- **Actor Pattern (`SwarmCoordinator`, `SwarmWorker`)**: Virtual-thread actor channels exchanging immutable envelopes (`SwarmEnvelope`) with isolated mailboxes and deadlock timeouts.
- **Reactive Observer Pattern (`ReactorEventBus`)**: Decoupled, non-blocking pub/sub message mesh backed by Project Reactor multicast Sinks.
- **Factory & Record Immutability**: All data transfer objects (`AgentMessage`, `ProtocolMessage`, `ModelRequest`, `ToolDefinition`) are implemented as immutable records with defensive copying.

