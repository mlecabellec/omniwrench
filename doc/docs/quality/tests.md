# Testing Strategy & Verification Suites

Omniwrench implements a strict, multi-tiered verification strategy adhering to the **Zero-Mock Production / Sandboxed Boundary Testing** philosophy, leveraging JUnit 5, AssertJ, Mockito (boundary tests only), and Spring Boot Test.

## Verification Stack
- **JUnit 5 (Jupiter)**: Execution lifecycle engine and tagged test suites.
- **AssertJ**: Fluent assertions with comprehensive error message introspection.
- **Mockito**: Test boundary stubbing for external protocol and process lifecycles.
- **Spring Boot Test**: Context initialization and component wiring verification.
- **Checkstyle & PMD/CPD**: Continuous static quality gates (100% compliant, 0 violations).

## Active Test Matrix & Traceability Mappings

| Module | Test Class | Methods | Traceability Tags (`@Tag`) | Scope & Description |
|---|---|---|---|---|
| `omniwrench-core` | [`SessionManagerTest`](file:///home/vortigern/git/omniwrench/omniwrench-core/src/test/java/com/omniwrench/core/SessionManagerTest.java) | 4 | `REQ-00013`, `FR-00001`, `UC-00001`, `TSK-20260822-001` | Session allocation, dynamic workspace isolation, retrieval, and non-null boundary validation |
| `omniwrench-core` | [`ProtocolBridgeTest`](file:///home/vortigern/git/omniwrench/omniwrench-core/src/test/java/com/omniwrench/protocol/ProtocolBridgeTest.java) | 3 | `REQ-00063`, `FR-00023`, `UC-00008` | Protocol bridge lifecycle, header immutability, mock bridge publish/subscribe execution |
| `omniwrench-core` | [`OmniwrenchPropertiesTest`](file:///home/vortigern/git/omniwrench/omniwrench-core/src/test/java/com/omniwrench/config/OmniwrenchPropertiesTest.java) | 3 | `REQ-00002`, `FR-00009`, `UC-00001`, `TSK-20260822-001` | Configuration defaults, custom mutations, and boundary/validation exceptions |
| `omniwrench-core` | [`ModelDataObjectsTest`](file:///home/vortigern/git/omniwrench/omniwrench-core/src/test/java/com/omniwrench/model/ModelDataObjectsTest.java) | 4 | `REQ-00010`, `FR-00004`, `UC-00001`, `TSK-20260822-001` | Immutability, value equality, and hash code contracts for `AgentMessage`, `SessionContext`, `ToolDefinition`, `ToolInvocation` |
| `omniwrench-tools` | [`AgentEngineTest`](file:///home/vortigern/git/omniwrench/omniwrench-tools/src/test/java/com/omniwrench/tools/AgentEngineTest.java) | 2 | `REQ-00043`, `REQ-00060`, `FR-00014`, `UC-00001`, `TSK-20260822-005` | Agent reasoning loop, prompt processing, command dispatching, and conversation memory |
| `omniwrench-tools` | [`ToolRegistryTest`](file:///home/vortigern/git/omniwrench/omniwrench-tools/src/test/java/com/omniwrench/tools/ToolRegistryTest.java) | 4 | `REQ-00060`, `FR-00020`, `UC-00009`, `TSK-20260822-005` | Dynamic tool registration, name lookup, descriptor retrieval, and null safety |
| `omniwrench-tools` | [`FileOperationsToolTest`](file:///home/vortigern/git/omniwrench/omniwrench-tools/src/test/java/com/omniwrench/tools/FileOperationsToolTest.java) | 4 | `REQ-00060`, `FR-00020`, `UC-00001`, `TSK-20260822-005` | Sandboxed file reading, writing, existence checking, directory listing, and error boundaries |
| `omniwrench-tools` | [`CommandExecutionToolTest`](file:///home/vortigern/git/omniwrench/omniwrench-tools/src/test/java/com/omniwrench/tools/CommandExecutionToolTest.java) | 4 | `REQ-00060`, `REQ-00065`, `FR-00020`, `FR-00025`, `UC-00002`, `TSK-20260822-005` | Bounded process execution, stdout capture, non-zero exit code error handling, and blank command guardrails |
| `omniwrench-ai` | [`BackendAdapterTest`](file:///home/vortigern/git/omniwrench/omniwrench-ai/src/test/java/com/omniwrench/ai/BackendAdapterTest.java) | 3 | `REQ-00040`, `REQ-00041`, `FR-00011`, `FR-00012`, `UC-00001` | Adapter SPI invocation, exception formatting, and sealed `MediaType` record instantiations |
| `omniwrench-ai` | [`ModelRequestResponseTest`](file:///home/vortigern/git/omniwrench/omniwrench-ai/src/test/java/com/omniwrench/ai/ModelRequestResponseTest.java) | 2 | `REQ-00040`, `REQ-00077`, `FR-00011`, `FR-00033`, `UC-00001` | Typed request validation, parameter immutability, token telemetry, and latency timestamps |
| `omniwrench-tui` | [`TerminalRendererTest`](file:///home/vortigern/git/omniwrench/omniwrench-tui/src/test/java/com/omniwrench/tui/TerminalRendererTest.java) | 5 | `REQ-00001`, `FR-00001`, `UC-00001`, `TSK-20260822-003` | Cyberpunk ASCII banner, telemetry status bar, user/agent bubbles, and prompt box formatting |
| `omniwrench-tui` | [`TuiRunnerTest`](file:///home/vortigern/git/omniwrench/omniwrench-tui/src/test/java/com/omniwrench/tui/TuiRunnerTest.java) | 5 | `REQ-00001`, `REQ-00002`, `FR-00001`, `UC-00001`, `TSK-20260822-003` | CLI/TUI command-line argument dispatching, property fallback, and standby server mode |
| `omniwrench-tui` | [`OmniwrenchTuiDashboardTest`](file:///home/vortigern/git/omniwrench/omniwrench-tui/src/test/java/com/omniwrench/tui/OmniwrenchTuiDashboardTest.java) | 2 | `REQ-00001`, `FR-00001`, `UC-00001`, `TSK-20260822-003` | Interactive command loop parsing (`/help`, `exit`), tool dispatch, and null validations |
| `omniwrench-web` | [`AgentControllerTest`](file:///home/vortigern/git/omniwrench/omniwrench-web/src/test/java/com/omniwrench/web/AgentControllerTest.java) | 4 | `REQ-00050`, `REQ-00051`, `FR-00015`, `UC-00003`, `TSK-20260822-004` | REST endpoints GET `/tools`, GET `/sessions/{id}/messages`, POST `/sessions/{id}/prompt` (200 OK, 400 Bad Request, 404 Not Found) |
| `omniwrench-web` | [`StatusControllerTest`](file:///home/vortigern/git/omniwrench/omniwrench-web/src/test/java/com/omniwrench/web/StatusControllerTest.java) | 2 | `REQ-00050`, `REQ-00051`, `FR-00015`, `UC-00003`, `TSK-20260822-004` | Telemetry endpoint GET `/api/v1/status` payload verification and constructor validation |
| `omniwrench-web` | [`WebSocketConfigTest`](file:///home/vortigern/git/omniwrench/omniwrench-web/src/test/java/com/omniwrench/web/WebSocketConfigTest.java) | 1 | `REQ-00050`, `REQ-00052`, `FR-00016`, `UC-00003`, `TSK-20260822-004` | WebSocket endpoint `/ws/agent-stream` registration and CORS permissions |
| `omniwrench-app` | [`OmniwrenchApplicationTests`](file:///home/vortigern/git/omniwrench/omniwrench-app/src/test/java/com/omniwrench/OmniwrenchApplicationTests.java) | 1 | `REQ-00001`, `REQ-00002`, `FR-00001`, `FR-00002`, `UC-00001`, `TSK-20260822-001` | Spring Boot end-to-end context bootstrap, component discovery, and runtime wiring |

## Summary Metrics
- **Total Test Suites**: 17 classes
- **Total Test Methods**: 53 methods
- **Test Pass Rate**: 100% (53 / 53 passed)
- **Checkstyle Violations**: 0 across all modules
- **PMD / CPD Violations**: 0 across all modules
- **Traceability Coverage**: 100% of production classes and test methods mapped to `REQ-*`, `FR-*`, `UC-*`, `TSK-*`, and `ADR-*` identifiers.
