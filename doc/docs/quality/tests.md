# Testing Strategy & Suites

Omniwrench implements a tiered verification strategy incorporating Unit, Integration, and Mock testing suites.

## Testing Stack
- **JUnit 5 (Jupiter)**: Execution framework.
- **AssertJ**: Fluent, readable assertions.
- **Mockito**: Boundary mock generation and stubbing.
- **Spring Boot Test**: Context loading and integration tests.

## Test Matrix
| Test Class | Scope | Description |
|---|---|---|
| `OmniwrenchApplicationTests` | Integration | Validates Spring Boot context boot and component discovery |
| `ToolRegistryTest` | Unit | Verifies dynamic tool registration, name lookup, and null safety |
| `AgentEngineTest` | Unit | Tests reasoning loop, prompt processing, and message history |

## Advanced Feature Test Matrix
| Test Class | Scope | Description |
|---|---|---|
| `ThinkingStreamDemuxerTest` | Unit | Verifies realtime extraction of thinking blocks and token separation |
| `CliPromptExecutionTest` | Integration | Tests CLI single-turn execution, piped stdin, and exit codes |
| `WebUiE2EPlaywrightTest` | E2E | Automated browser test for Web UI chat, WebSockets, and prompt interaction |
| `TuiVirtualScreenTest` | Unit / TUI | Automated headless Lanterna screen render and layout assertions |
| `LlamaCppInferenceTest` | Integration | Real in-memory token generation using native `llama.cpp` binding |
| `ModelRepositoryManagerTest` | Integration | Search, filter, and metadata extraction for Ollama and HuggingFace Hub |
| `GemmaE2BModelDownloadAndRunTest` | E2E Real Test | Downloads Gemma E2B, executes real prompt completions via CLI, and cleans up weights |
| `AdvancedFileOperationsToolTest` | Unit / Real I/O | Tests tree walking, regex grep, diff, patch, binary inspection, and hex conversion |
| `AsyncBackgroundJobToolTest` | Integration | Tests asynchronous background tool execution with progress callbacks on Virtual Threads |
| `OpenApiToolSchemaGeneratorTest` | Unit | Verifies schema generation, parameter constraints, and `@Injected` context stripping |
| `ZeroMockArchitectureStaticTest` | Static Gate | ArchUnit static analysis rule scanning `src/main` to enforce zero mocks in production |
