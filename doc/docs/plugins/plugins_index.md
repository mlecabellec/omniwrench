# Plugins Architecture

Omniwrench provides an open SPI model allowing engineers to implement custom tools and subagents.

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

interface "Tool SPI" as SPI
component "FileOperationsTool" as F
component "CommandExecutionTool" as C
component "CustomGitPlugin" as G
component "CustomOpcUaPlugin" as O

SPI <|.. F
SPI <|.. C
SPI <|.. G
SPI <|.. O

[ToolRegistry] o-- SPI
@enduml
```

## Creating a Plugin
Implement `com.omniwrench.tools.Tool`, annotate the class with `@Component`, and Spring Boot will automatically discover and register it in `ToolRegistry`.
