# Testing Strategy & Verification Suites

Omniwrench implements a strict, multi-tiered verification strategy adhering to the **Zero-Mock Production / Sandboxed Boundary Testing** philosophy (`CS-0055`), leveraging JUnit 5, AssertJ, Mockito (boundary tests only), and Spring Boot Test.

## Verification Stack
- **JUnit 5 (Jupiter)**: Execution lifecycle engine and tagged test suites.
- **AssertJ**: Fluent assertions with comprehensive error message introspection.
- **Mockito (5.18+) & Byte Buddy (1.18.12)**: Test boundary stubbing and dynamic byte transformation fully compatible with modern OpenJDK 17, 21, and 25 runtimes.
- **Spring Boot Test**: Context initialization and component wiring verification.
- **Checkstyle & PMD/CPD**: Continuous static quality gates (100% compliant, 0 violations).

## Test Invocations & Commands Matrix

| Scope / Goal | Command | Description |
|---|---|---|
| **All Modules & Tests (Parallel)** | `mvn -T 6 clean test` | Complete verification pass across all 7 reactor artifacts using up to 6 threads |
| **Via Helper Tool** | `./omniwrench-helper.sh test` | Automated wrapper executing full clean test suite |
| **Single Module** | `mvn test -pl omniwrench-tui` | Executes tests exclusively for targeted module |
| **Specific Test Class** | `mvn test -Dtest=FullscreenTuiWorkbenchTest` | Runs single targeted test class |
| **Specific Test Method** | `mvn test -Dtest=OmniwrenchTuiDashboardTest#shouldProcessInputAndExit` | Runs single targeted test method |
| **By Traceability Tag** | `mvn test -Dgroups="REQ-00001"` | Filters test execution by requirement or feature tag |
| **With JVM Packaging** | `mvn clean package -Pjvm-package` | Builds JVM fat JAR verifying all unit tests |
| **With Native Profile (Skip Tests)** | `mvn clean package -Pnative -DskipTests` | Compiles AOT native image binary |

---

## Active Test Matrix & Traceability Mappings

| Module | Test Class | Methods | Traceability Tags (`@Tag`) | Scope & Description |
|---|---|---|---|---|
| `omniwrench-core` | `SessionManagerTest` | 4 | `REQ-00013`, `FR-00001`, `UC-00001`, `TSK-20260822-001` | Session allocation, dynamic workspace isolation, retrieval, and non-null boundary validation |
| `omniwrench-core` | `ProtocolBridgeTest` | 3 | `REQ-00063`, `FR-00023`, `UC-00008` | Protocol bridge lifecycle, header immutability, mock bridge publish/subscribe execution |
| `omniwrench-core` | `OmniwrenchPropertiesTest` | 3 | `REQ-00002`, `FR-00009`, `UC-00001`, `TSK-20260822-001` | Configuration defaults, custom mutations, and boundary/validation exceptions |
| `omniwrench-core` | `ModelDataObjectsTest` | 4 | `REQ-00010`, `FR-00004`, `UC-00001`, `TSK-20260822-001` | Immutability, value equality, and hash code contracts for `AgentMessage`, `SessionContext`, `ToolDefinition`, `ToolInvocation` |
| `omniwrench-core` | `StreamDemuxerTest` | 4 | `REQ-00088`, `FR-00011`, `FR-00014`, `UC-00001`, `TSK-20260822-007` | Thinking tag parsing, static separation, token streaming state machine, and null safety |
| `omniwrench-core` | `OpenApiToolSchemaGeneratorTest` | 4 | `REQ-00095`, `FR-00020`, `FR-00011`, `TSK-20260822-012` | OpenAPI 3.1, Gemini, and Anthropic function calling schema generation, required fields, and context filtering |
| `omniwrench-core` | `ZeroMockAuditTest` | 1 | `REQ-00092`, `CS-0055`, `TSK-20260822-013` | Automated reactor-wide static analysis scanning `src/main/java` enforcing zero mock imports and zero mock annotations |
| `omniwrench-tools` | `AgentEngineTest` | 3 | `REQ-00043`, `REQ-00060`, `REQ-00088`, `FR-00014`, `UC-00001`, `TSK-20260822-005`, `TSK-20260822-007` | Agent reasoning loop, `/thinking` toggle commands, prompt processing, and tool dispatching |
| `omniwrench-tools` | `ToolRegistryTest` | 4 | `REQ-00060`, `FR-00020`, `UC-00009`, `TSK-20260822-005` | Dynamic tool registration, name lookup, descriptor retrieval, and null safety |
| `omniwrench-tools` | `FileOperationsToolTest` | 4 | `REQ-00060`, `FR-00020`, `UC-00001`, `TSK-20260822-005` | Sandboxed file reading, writing, existence checking, directory listing, and error boundaries |
| `omniwrench-tools` | `CommandExecutionToolTest` | 4 | `REQ-00060`, `REQ-00065`, `FR-00020`, `FR-00025`, `UC-00002`, `TSK-20260822-005` | Bounded process execution, stdout capture, non-zero exit code error handling, and blank command guardrails |
| `omniwrench-tools` | `ModelManagementToolTest` | 4 | `REQ-00060`, `REQ-00091`, `FR-00020`, `TSK-20260822-010` | Model management tool execution actions: list, search, pull, and rm |
| `omniwrench-tools` | `AdvancedFileOperationsToolTest` | 6 | `REQ-00060`, `REQ-00093`, `REQ-00094`, `FR-00020`, `FR-00021`, `TSK-20260822-011` | Recursive tree exploration, multi-file regex grep, unified patching, line slicing, hex encoding, and async execution |
| `omniwrench-ai` | `BackendAdapterTest` | 3 | `REQ-00040`, `REQ-00041`, `FR-00011`, `FR-00012`, `UC-00001` | Adapter SPI invocation, exception formatting, and sealed `MediaType` record instantiations |
| `omniwrench-ai` | `ModelRequestResponseTest` | 2 | `REQ-00040`, `REQ-00077`, `FR-00011`, `FR-00033`, `UC-00001` | Typed request validation, parameter immutability, token telemetry, and latency timestamps |
| `omniwrench-ai` | `LlamaCppBackendAdapterTest` | 5 | `REQ-00090`, `REQ-00093`, `FR-00011`, `FR-00012`, `UC-00001`, `TSK-20260822-009`, `TSK-20260822-015` | Embedded llama.cpp GGUF local model execution, reactive token streaming (`Flux`), and error boundary |
| `omniwrench-ai` | `LlamaCppSignalGuardTest` | 4 | `REQ-00090`, `REQ-00093`, `TSK-20260822-015` | Signal guard fault containment, NPE/OOM trapping, and structured BackendException translation |
| `omniwrench-ai` | `LlamaCppNativeBridgeTest` | 2 | `REQ-00090`, `REQ-00093`, `TSK-20260822-015` | In-process model context initialization, tokenization, decoding, sampling, and AutoCloseable memory cleanup |
| `omniwrench-ai` | `NativeLibraryLoaderTest` | 3 | `REQ-00090`, `REQ-00093`, `TSK-20260822-015` | Multi-architecture host OS/Arch identification, platform strings, and GPU compute enums |
| `omniwrench-ai` | `ModelRepositoryAndManagerTest` | 4 | `REQ-00091`, `FR-00011`, `UC-00001`, `TSK-20260822-010` | Model descriptor contracts, Ollama/HuggingFace search, download progress, and local lifecycle removal |
| `omniwrench-ai` | `ModelManagementOptionalIntegrationTest` | 6 | `REQ-00091`, `FR-00011`, `FR-00020`, `TSK-20260822-010` | Live search against Ollama Library & HuggingFace Hub, OCI manifest resolution, SHA-256 digest validation, and local discovery |
| `omniwrench-tui` | `TerminalEnvironmentDetectorTest` | 10 | `REQ-00001`, `FR-00001`, `UC-00001`, `ADR-0058` | Detection of `STANDARD`, `SSH`, and `TMUX` environments, OSC 52 tmux DCS passthrough framing, and viewport clamping |
| `omniwrench-tui` | `TerminalRendererTest` | 12 | `REQ-00001`, `REQ-00088`, `FR-00001`, `UC-00001`, `TSK-20260822-003`, `TSK-20260822-007` | Cyberpunk ASCII banner, narrow column adaptation, telemetry status bar, thinking boxes, `/btw` & `/goal` cards, and prompt formatting |
| `omniwrench-tui` | `TuiRunnerTest` | 9 | `REQ-00001`, `REQ-00002`, `REQ-00089`, `FR-00001`, `UC-00001`, `TSK-20260822-003` | CLI/TUI command-line argument dispatching (`-p`, `--json`, `--version`, `--help`), property fallback, and standby server mode |
| `omniwrench-tui` | `OmniwrenchTuiDashboardTest` | 3 | `REQ-00001`, `FR-00001`, `UC-00001`, `TSK-20260822-003` | Interactive command loop parsing (`/help`, `/goal`, `/env`, `/btw`, `/copy`, `exit`), tool dispatch, and null validations |
| `omniwrench-tui` | `FullscreenTuiWorkbenchTest` | 10 | `REQ-00001`, `REQ-00088`, `REQ-00099`, `FR-00001`, `UC-00001`, `TSK-20261101-016` | Multi-tier functional, boundary, and 4-thread concurrency stress tests for `TuiDisplayMode`, `TuiThemePalette`, `FullscreenTuiLayout` (4 breakpoints, Home/Session routes, modals), `FullscreenTuiModeEngine`, `MinimalTuiModeEngine`, and `--fullscreen`/`--minimal` CLI flags |
| `omniwrench-web` | `AgentControllerTest` | 6 | `REQ-00050`, `REQ-00051`, `FR-00015`, `UC-00003`, `TSK-20260822-004` | REST endpoints GET `/tools`, GET/POST `/sessions`, GET/DELETE `/sessions/{id}/messages`, POST `/sessions/{id}/prompt` |
| `omniwrench-web` | `StatusControllerTest` | 2 | `REQ-00050`, `REQ-00051`, `FR-00015`, `UC-00003`, `TSK-20260822-004` | Telemetry endpoint GET `/api/v1/status` payload verification and constructor validation |
| `omniwrench-web` | `WebSocketConfigTest` | 1 | `REQ-00050`, `REQ-00052`, `FR-00016`, `UC-00003`, `TSK-20260822-004` | WebSocket endpoint `/ws/agent-stream` registration and CORS permissions |
| `omniwrench-web` | `AgentStreamWebSocketHandlerTest` | 1 | `REQ-00050`, `REQ-00052`, `REQ-00088`, `FR-00016`, `UC-00003`, `TSK-20260822-004`, `TSK-20260822-007` | Real-time WebSocket `thought.delta` and `chat.delta` event generation and lifecycle tracking |
| `omniwrench-app` | `OmniwrenchApplicationTests` | 1 | `REQ-00001`, `REQ-00002`, `FR-00001`, `FR-00002`, `UC-00001`, `TSK-20260822-001` | Spring Boot end-to-end context bootstrap, component discovery, and runtime wiring |
| `omniwrench-app` | `TriInterfacePromptE2ETest` | 1 | `REQ-00089`, `FR-00001`, `FR-00015`, `UC-00001`, `UC-00003`, `UC-00004`, `TSK-20260822-008` | Tri-interface prompt execution parity across CLI (`-p`), interactive TUI engine, and Web REST endpoints with session history sync |

## Summary Metrics
- **Total Test Suites**: 32 classes
- **Total Test Methods**: 122 methods
- **Test Pass Rate**: 100% (116 core suite + 6 optional remote integration tests passing)
- **Checkstyle Violations**: 0 across all modules
- **PMD / CPD Violations**: 0 across all modules
- **Traceability Coverage**: 100% of production classes and test methods mapped to `REQ-*`, `FR-*`, `UC-*`, `TSK-*`, and `ADR-*` identifiers.
