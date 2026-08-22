# Use Cases Register

Use case diagrams and scenarios for developer interactions with Omniwrench.

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

left to right direction
actor "Software Engineer" as DEV
actor "CI/CD Pipeline" as CI

rectangle "Omniwrench System" {
  usecase "UC-00001: Interactive Code Pairing in TUI" as UC1
  usecase "UC-00002: Autonomous Refactoring Loop" as UC2
  usecase "UC-00003: Headless Batch Tool Invocation" as UC3
  usecase "UC-00004: Remote Web Dashboard Telemetry" as UC4
  usecase "UC-00005: Documentation Generation" as UC5
}

DEV --> UC1
DEV --> UC2
DEV --> UC4
DEV --> UC5

CI --> UC3
CI --> UC5
@enduml
```

## Detailed Scenarios

### `UC-00001`: Interactive Code Pairing in TUI
The developer opens a terminal, launches `./omniwrench-helper.sh tui`, issues instructions to inspect or modify classes, observes realtime tool outputs, and reviews changes.

### `UC-00002`: Autonomous Refactoring Loop
The developer provides a high-level goal. The agent builds a plan with `TSK-*` tags, runs tests, fixes violations, validates Checkstyle/PMD gates, and requests clearance to commit.
