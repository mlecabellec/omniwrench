# Plugins & Extensibility Architecture

Omniwrench provides a dual plugin architecture supporting both compile-time Spring `@Component` scanning and runtime dynamic JAR loading via Java `ServiceLoader` SPI with child classloader isolation (**ADR-0010**).

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

interface "Tool SPI" as SPI
interface "ProtocolBridge SPI" as PROTO_SPI
interface "BackendAdapter SPI" as AI_SPI

component "FileOperationsTool" as F
component "JavaParserAstTool" as AST
component "HomeAssistantTool" as HA
component "ExternalJarPlugin" as EXT_JAR

SPI <|.. F
SPI <|.. AST
SPI <|.. EXT_JAR
PROTO_SPI <|.. HA

[ToolRegistry] o-- SPI
[ProtocolRegistry] o-- PROTO_SPI
[SmartModelRouter] o-- AI_SPI
[PluginClassLoader] --> EXT_JAR : loads from plugins/
@enduml
```

## Plugin Types & Extension Points

### 1. Tool SPI Plugins (`com.omniwrench.tools.Tool`)
- **Built-in Plugins**: Filesystem, Bounded Shell Execution, AST analysis via JavaParser (`ADR-0024`), Git operations, Home Assistant.
- **External Drop-in JARs**: Drop any `.jar` implementing `Tool` and containing `META-INF/services/com.omniwrench.tools.Tool` into the `plugins/` directory. Omniwrench dynamically loads them into isolated `URLClassLoader` instances.

### 2. Protocol Bridge Plugins (`com.omniwrench.protocol.ProtocolBridge`)
- Implement `ProtocolBridge` to connect new message brokers, IoT endpoints, or custom network services (MQTT, CoAP, BLE, Modbus) into the reactive `ReactorEventBus` mesh (`ADR-0029`).

### 3. AI Backend Adapters (`com.omniwrench.ai.BackendAdapter<T>`)
- Implement custom inference engines (e.g. specialized fine-tuned models, local GGML/GGUF runtimes, TensorRT-LLM, PyTorch bindings) conforming to the typed `MediaType` contract (`ADR-0015`).

### 4. Model Context Protocol (MCP) Plugins (`ADR-0036`)
- External tool servers configured in `.omniwrench/mcp-servers.json` are dynamically discovered over Stdio / SSE JSON-RPC and registered into `ToolRegistry`.

