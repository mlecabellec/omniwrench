# Goal, Task & Requirements Governance

This document establishes the strict **Goal-, Task-, Requirement-, and Test-Oriented Governance** system for Omniwrench (ADR-0023), replacing fixed calendar schedules with rigorous outcome-based execution and traceability.

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff
skinparam roundCorner 8
skinparam shadowing false

package "Strategic Goals" {
  [Goal 1: Polyvalent Agent Core] as G1
  [Goal 2: Multi-Modal AI SPI] as G2
  [Goal 3: Adaptive Dual HUD] as G3
  [Goal 4: Infrastructure Operations] as G4
}

package "Requirements (REQ-*)" {
  [REQ-00001\nDual Runtime] as REQ1
  [REQ-00010\nMission Standards] as REQ2
  [REQ-00020\nTool Registry SPI] as REQ3
  [REQ-00030\nReasoning Engine] as REQ4
  [REQ-00040\nAI Adapter SPI] as REQ5
}

package "Tasks (TSK-*)" {
  [TSK-001\nMaven Skeleton] as T1 #2b5c3f
  [TSK-002\nDoc Kit & Rules] as T2 #2b5c3f
  [TSK-003\nCyberpunk TUI] as T3 #2b5c3f
  [TSK-004\nSpring Web & WS] as T4 #2b5c3f
  [TSK-005\nTool Registry] as T5 #2b5c3f
  [TSK-006\nMulti-Module Refactor] as T6 #2b5c3f
}

G1 --> REQ1
G1 --> REQ2
G1 --> REQ3
G1 --> REQ4
G2 --> REQ5
G3 --> REQ1

REQ1 --> T1
REQ2 --> T2
REQ1 --> T3
REQ1 --> T4
REQ3 --> T5
REQ4 --> T5
REQ1 --> T6
@enduml
```

---

## 🎯 Global Constraints & Resource Boundaries

* **Project Time Allocation**: Maximum **1,600 hours / year** allocated to infrastructure and framework engineering.
* **Hardware Capital Budget**: Total **€400,000** allocated over 2026-2029 (Target rate: **€100,000 / year**).
* **Team Allocation**: **3.0 FTE** (1.5 FTE Internal + 1.5 FTE External).
* **Critical Operational Tasks**:
  1. Debian 11 & SUSE 15 SP6 Operating System Migrations.
  2. NetApp Storage Infrastructure Replacement (2029).
* **Tooling Standard**: PlantUML (v1.2020.02 compatibility enforced), Python cost analysis modeling.

---

## 📋 Active Tasks Register

| Task Reference | Title | Owner | Status |
|---|---|---|---|
| [`TSK-20260822-001`](tasks/TSK-20260822-001.md) | Project Initialization & Maven Dual Skeleton Setup | AI Agent | Completed |
| [`TSK-20260822-002`](tasks/TSK-20260822-002.md) | Documentation Kit Setup & Quality Standards Incorporation | AI Agent | Completed |
| [`TSK-20260822-003`](tasks/TSK-20260822-003.md) | Modern Cyberpunk TUI Design & Integration | AI Agent | Completed |
| [`TSK-20260822-004`](tasks/TSK-20260822-004.md) | Spring Web & Reactive WebSocket Server Engine | AI Agent | Completed |
| [`TSK-20260822-005`](tasks/TSK-20260822-005.md) | Pluggable Tool Registry & Agent Execution Loop | AI Agent | Completed |
| [`TSK-20260822-006`](tasks/TSK-20260822-006.md) | Maven Multi-Module Modular Architecture Restructuring | AI Agent | Completed |
| [`TSK-20260822-007`](tasks/TSK-20260822-007.md) | Dual Chat Mode & Thinking Stream Demultiplexing | AI Agent | Completed |
| [`TSK-20260822-008`](tasks/TSK-20260822-008.md) | Unified Tri-Interface Prompting & E2E Test Suite | AI Agent | Completed |

| [`TSK-20260822-009`](tasks/TSK-20260822-009.md) | Embedded llama.cpp LLM Backend Plugin & Native FFM Binding | AI Agent | Completed |
| [`TSK-20260822-010`](tasks/TSK-20260822-010.md) | Ollama & HuggingFace Model Hub Repository Manager | AI Agent | Completed |
| [`TSK-20260822-011`](tasks/TSK-20260822-011.md) | Advanced File Operations & Background Tool Plugin | AI Agent | Completed |
| [`TSK-20260822-012`](tasks/TSK-20260822-012.md) | Ultra-Precise OpenAPI & JSON Schema Function Calling Registry | AI Agent | Completed |
| [`TSK-20260822-013`](tasks/TSK-20260822-013.md) | Zero-Mock Runtime Quality Mandate & Automated Gate | AI Agent | Completed |
| [`TSK-20260822-014`](tasks/TSK-20260822-014.md) | GraalVM SDK Integration & Dual Packaging Profiles | AI Agent | Completed |
| [`TSK-20260822-015`](tasks/TSK-20260822-015.md) | In-Process Embedded llama.cpp Engine for Various CPU/GPU Architectures | AI Agent | Completed |




