# Omniwrench Knowledge Base & Memory

This document stores persistent project knowledge, architectural decisions, and operational memories across development sessions.

---

## 🧠 Architectural Decision Records (ADRs)

### ADR-0001: Dual Presentation Architecture (TUI + Web)
- **Status**: Accepted (2026-08-22)
- **Context**: Developers need fast, low-overhead CLI tools locally, while remote telemetry, graphical dashboards, and CI systems need HTTP/WebSocket access.
- **Decision**: Implemented Spring Boot dual runtime architecture where `OmniwrenchTuiDashboard` runs as an interactive `CommandLineRunner` while `AgentController` and `WebSocketConfig` provide standard HTTP/WS services concurrently on the same domain engine.

### ADR-0002: Strict Java Mission-Critical Coding Standards (CS-0010 to CS-0070)
- **Status**: Accepted (2026-08-22)
- **Context**: Large AI-assisted refactorings can introduce regressions, null pointers, and unbounded thread/memory leaks.
- **Decision**: Adopted the full suite of constraints (`CS-0010` through `CS-0070`) prohibiting `var`, enforcing defensive parameter validation upon method entry (`Objects.requireNonNull`), bounding thread pools, and requiring human clearance before git commits.

### ADR-0003: Single-Binary Documentation Kit via `mkdocs-kit`
- **Status**: Accepted (2026-08-22)
- **Context**: Need standalone documentation generation without requiring external Python environments.
- **Decision**: Standardized on `mkdocs-kit` to compile markdown and PlantUML diagrams directly into HTML5, PDF, and Unix Man pages.

---

## 📌 Industrial Constraints Alignment (2026-2029)
- **Annual Hours Budget**: 1,600 h/year maximum.
- **Hardware Capital Budget**: €400,000 total (€100,000/year target).
- **Core Engineering Staff**: 3.0 FTE (1.5 FTE internal + 1.5 FTE external).
- **Target OS Environments**: Debian 11 -> 12, SUSE Linux Enterprise Server (SLES) 15 SP6.
- **Diagram Tooling**: PlantUML v1.2020.02 compatibility standard.

### ADR-0004: Multi-Provider AI Adapter Layer
- **Status**: Accepted (2026-08-22)
- **Context**: Omniwrench requires flexible inference backends spanning cloud providers (Gemini, Claude, OpenAI) and local inference runtimes (Ollama, vLLM).
- **Decision**: Architected a pluggable multi-provider model adapter SPI supporting both cloud API streaming and local inference endpoints with standardized token streaming and tool schema serialization.

### ADR-0005: Full Multi-Pane Lanterna Terminal Dashboard
- **Status**: Accepted (2026-08-22)
- **Context**: The CLI requires an advanced, cyberpunk-styled TUI providing simultaneous visibility of agent reasoning, tool execution logs, workspace file tree, and system telemetry.
- **Decision**: Adopted Lanterna multi-pane windowing engine with split panels (Chat / Reasoning, Tool Logs, File Tree, Status Bar) coupled with an interactive readline prompt.

### ADR-0006: True Polyvalent Architecture - Specialization via Pluggable Extensions
- **Status**: Accepted (2026-08-22)
- **Context**: Omniwrench is not domain-constrained. It must serve as a universal base platform for code analysis, conversational coding, autonomous task planning, and infrastructure orchestration with equal capability.
- **Decision**: The core engine remains fully polyvalent. Specialization is achieved exclusively through pluggable tools, behaviors, protocols, agent personalities, and rule sets. All four primary capability domains are covered by the base engine: workspace scanning, conversational coding, autonomous task planning, and infrastructure operations.
- **Implications**:
  - `Tool` SPI must cover filesystem, shell, HTTP, git, and AST inspection operations.
  - `AgentEngine` must support multi-step reasoning loops, not just single-shot command dispatch.
  - Agent "personalities" and system prompts must be configurable per-session.
  - The multi-provider AI layer is essential — different tasks may benefit from different models.

### ADR-0007: Full Built-in Tool Set Coverage
- **Status**: Accepted (2026-08-22)
- **Decision**: All eight core tool domains are included in the Omniwrench standard tool set:
  1. **GitTool** — status, diff, log, commit (with CS-0070 human clearance guardrail), branch management.
  2. **AstAnalysisTool** — Java AST parse tree inspection, symbol resolution, dead code detection, refactoring suggestions (via JavaParser or spoon).
  3. **HttpClientTool** — external API calls, JSON schema validation, authentication token management.
  4. **DatabaseTool** — SQL query execution, schema inspection, migration management (PostgreSQL, MariaDB, SQLite) via JDBC.
  5. **OpcUaTool** — OPC-UA node browsing, read/write, subscription management, re-using Eclipse Milo patterns from Nunki.
  6. **SystemInspectionTool** — process listing (ps/top), systemd services, journalctl, dmesg, port inspection.
  7. **NotificationTool** — email, Slack, Teams, webhook dispatch for CI/CD event alerts.
  8. **DocumentGenerationTool** — Markdown writing, PlantUML compilation, PDF generation via mkdocs-kit.
- **Implementation strategy**: All tools implement the `Tool` SPI, registered as Spring `@Component` beans.

### ADR-0008: Hybrid Reasoning Loop Architecture
- **Status**: Accepted (2026-08-22)
- **Context**: Simple prompts (file reads, status checks) don't need multi-step planning overhead. Complex goals (refactoring, migration, infrastructure setup) require a hierarchical plan with parallel subtask execution.
- **Decision**: Hybrid reasoning engine:
  - **Single-step mode**: Direct tool dispatch for low-complexity prompts (heuristics: no multi-action keywords, no dependency chains detected, single tool required).
  - **Multi-step Plan-and-Execute mode**: For complex goals — a Planner generates a task DAG (`TSK-YYYYMMDD-XXX` tagged), Executor agents process subtasks using the bounded thread pool, results are aggregated and checkpointed in `SessionContext`.
  - **Auto-switching**: Prompt complexity heuristic based on word count, multi-verb detection, presence of conditional/dependency language, and tool overlap analysis.
- **Key classes**: `ReasoningMode` enum (SINGLE_STEP, PLAN_EXECUTE), `ComplexityHeuristic` strategy, `TaskDag` model, `PlanExecutorService`.

### ADR-0009: Local File-Based Session Persistence (One JSON File per Record)
- **Status**: Accepted (2026-08-22)
- **Context**: Zero external service dependencies required. Portability is essential across Debian 11, Debian 12, and SUSE 15 SP6 environments.
- **Decision**: All session state, conversation history, and task tracking is persisted as **individual JSON files** in a configurable `.omniwrench/` workspace directory:
  - **One file per session**: `.omniwrench/sessions/{sessionId}/session.json` (metadata only, no messages).
  - **One file per message**: `.omniwrench/sessions/{sessionId}/messages/{messageId}.json`.
  - **One file per tool invocation**: `.omniwrench/sessions/{sessionId}/tools/{callId}.json`.
  - **One file per task**: `.omniwrench/tasks/{taskId}.json` (corresponds to `TSK-YYYYMMDD-XXX` task docs).
- **Key principle**: No arrays inside JSON files. Each logical entity gets its own atomic file. Directory listing replaces array queries.
- **Benefits**: Human-readable, git-trackable, shell-inspectable, zero-dependency, no schema migration.

### ADR-0010: Plugin JAR Loading via Java ServiceLoader SPI
- **Status**: Accepted (2026-08-22)
- **Context**: Third-party tool extensions and agent behavior packs must be loadable without recompiling the core engine or modifying its classpath.
- **Decision**: Plugin JARs are discovered at startup from a configurable `plugins/` directory. Each plugin JAR includes a `META-INF/services/com.omniwrench.tools.Tool` file listing concrete `Tool` implementations. Java `ServiceLoader<Tool>` scans and loads them. The `ToolRegistry` receives all discovered plugins alongside built-in beans.
- **Isolation model**: Each plugin JAR gets its own `URLClassLoader` child of the application class loader, preventing version conflicts.
- **Plugin lifecycle**: `PluginManager` service handles discovery, loading, validation (metadata check), registration into `ToolRegistry`, and graceful unload.
- **Security**: Plugin JARs are validated against a SHA-256 checksum manifest (`.omniwrench/plugins/checksums.json`) before loading.

### ADR-0011: Spring Profile-Based Mode Separation (tui / web / dual)
- **Status**: Accepted (2026-08-22)
- **Decision**:
  - `--spring.profiles.active=tui` — Launches Lanterna TUI only. Web server auto-configuration (`spring-boot-starter-web`) is excluded via `@ConditionalOnProfile`. No HTTP port bound.
  - `--spring.profiles.active=web` — Launches Spring MVC + WebSocket server only. TUI `CommandLineRunner` is excluded. Port 8080 (configurable).
  - Default (no profile / `--spring.profiles.active=dual`) — Both TUI and Web server run concurrently in the same JVM, sharing `AgentEngine`, `SessionManager`, and `ToolRegistry`.
- **Helper script**: `omniwrench-helper.sh tui|web|dual` sets the appropriate profile flag automatically.
- **Implications**: `@Profile("tui")` on `TuiRunner` and `OmniwrenchTuiDashboard`; `@Profile("web")` on `AgentController` and `StatusController`; core beans are always active.

### ADR-0012: Spring Security with API Key + Optional JWT
- **Status**: Accepted (2026-08-22)
- **Decision**: Web API and WebSocket endpoints are protected by:
  - **Primary**: `X-Api-Key` header authentication. The key is configurable via `omniwrench.security.api-key` property or `OMNIWRENCH_API_KEY` environment variable. Requests without valid key receive `401 Unauthorized`.
  - **Optional**: JWT Bearer token support for multi-user deployments. JWT secret configurable via `omniwrench.security.jwt-secret`.
  - **Default**: Localhost-only binding (`server.address=127.0.0.1`) unless explicitly overridden.
  - **TUI mode**: No authentication — direct process access only.
- **Key classes**: `SecurityConfig` (Spring Security filter chain), `ApiKeyAuthFilter` (servlet filter), `JwtTokenProvider` (optional JWT validation).

### ADR-0013: Context-Adaptive TUI Layout (Terminal-Size-Driven Panel Collapse)
- **Status**: Accepted (2026-08-22)
- **Decision**: The Lanterna dashboard auto-detects terminal dimensions at startup and on SIGWINCH resize events:
  - **< 80 columns or < 24 rows**: Single-panel mode — full-screen chat/reasoning only. Status bar is a single condensed line. File tree and tool logs hidden.
  - **80–139 columns**: Two-panel mode — chat panel (left 70%) + tool logs (right 30%). File tree overlay via F1.
  - **140–159 columns**: Three-panel mode — session list (left 15%) + chat (center 55%) + tool logs (right 30%). Status bar on top.
  - **≥ 160 columns**: Four-panel mode — session list | chat/reasoning | file tree | tool logs. Full cyberpunk banner + status pills on top. Readline input at bottom.
- **Key classes**: `TerminalLayoutManager` (detects size and selects layout), `AdaptivePanel` (collapses/expands on resize), `ResizeListener` (Lanterna SIGWINCH hook).
- **Hotkeys**: F1=File Tree toggle, F2=Tool Logs toggle, F3=Session List, F4=Config, F5=Refresh/Redraw.

### ADR-0014: OpenTelemetry Structured Tracing
- **Status**: Accepted (2026-08-22)
- **Decision**: Java OpenTelemetry SDK instruments every agent turn, tool invocation, and reasoning step as structured spans:
  - **Span hierarchy**: `agent.session` > `agent.turn` > `tool.execute` > `tool.{name}`.
  - **Attributes**: sessionId, toolName, exitCode, tokenCount, durationMs, modelProvider.
  - **Default export**: local OTLP NDJSON file (`.omniwrench/telemetry/traces-{date}.ndjson`), zero external dependencies.
  - **Optional**: Remote OTLP endpoint configurable via `omniwrench.telemetry.endpoint` for Jaeger/Grafana Tempo integration.
  - **TUI display**: The Tool Logs panel shows live span durations and statuses during execution.

### ADR-0015: Custom Future-Proof Multi-Modal AI Adapter SPI
- **Status**: Accepted (2026-08-22)
- **Context**: No existing framework covers all required backends and media types simultaneously. The Omniwrench AI layer must be future-proof, modular, and extensible without framework lock-in.
- **Decision**: A fully custom, modular AI adapter SPI is designed within `com.omniwrench.ai`:
  - **`MediaType` sealed hierarchy**: `TextCompletion`, `ChatReasoning`, `ImageGeneration`, `ImageTransformation`, `AudioTranscription`, `EmbeddingGeneration`, `DataflowProcessing` — covering all AI media domains.
  - **`BackendAdapter` SPI**: Plugin interface for backends: `OpenAiCompatibleAdapter` (OpenAI, Ollama, vLLM, LM Studio, LocalAI), `TorchAdapter`, `LlamaCppAdapter`, `TensorFlowAdapter`, `HuggingFaceAdapter`.
  - **`ModelRequest<T extends MediaType>` / `ModelResponse<T>`**: Strongly typed generic request/response contracts, no raw maps.
  - **`ExecutionMode` enum**: `SYNCHRONOUS`, `STREAMING_SSE`, `ASYNCHRONOUS_FUTURE`, `BACKGROUND_TASK`, `PLANNED_SCHEDULED`, `TOOL_ENABLED`.
  - **`ModelRouter`**: Selects the appropriate backend and adapter based on `MediaType` + `ExecutionMode` + provider capability matrix.
  - **Tool schema injection**: `ToolSchemaSerializer` converts `ToolDefinition` to provider-specific JSON schemas (OpenAI function calling format, Anthropic tools format, Gemini function declarations).
- **Design principles**: Zero external framework dependency in the core SPI. Concrete backend adapters are separate modules/JARs loadable as plugins.

### ADR-0016: Maven Multi-Module Build Structure
- **Status**: Accepted (2026-08-22)
- **Decision**: The project is structured as a Maven multi-module build from the start:
  - **`omniwrench-core`**: Engine, domain models, SPIs (Tool, BackendAdapter, ModelRequest/Response, SessionContext, AgentEngine skeleton). No Spring dependencies.
  - **`omniwrench-tools`**: Built-in tool implementations (FileOps, Git, AST, HTTP, DB, OPC-UA, System, Notification, DocGen). Depends on `omniwrench-core`.
  - **`omniwrench-ai`**: Custom AI adapter SPI + concrete backend adapters (OpenAI-compatible, Torch, llama.cpp, TensorFlow, HuggingFace). Depends on `omniwrench-core`.
  - **`omniwrench-tui`**: Lanterna multi-pane adaptive TUI. Depends on `omniwrench-core`.
  - **`omniwrench-web`**: Spring Boot Web + WebSocket + Security. Depends on `omniwrench-core`.
  - **`omniwrench-app`**: Spring Boot assembly + main class + plugin loader. Depends on all other modules.
- **Parent POM**: `omniwrench` (root) defines BOM, shared plugin configuration, Checkstyle, PMD, Surefire, GraalVM native plugin.
- **Benefit**: `omniwrench-core` and `omniwrench-tools` are usable as standalone libraries without the full Spring Boot stack.

### ADR-0017: Dynamic Hybrid Swarm Multi-Agent Coordination Protocol
- **Status**: Accepted (2026-08-22)
- **Context**: Complex multi-stage engineering operations (refactoring, infrastructure migration, test suite synthesis) require specialized subagents that can operate both under top-down task delegation and collaborate peer-to-peer to resolve cross-domain trade-offs.
- **Decision**: Implemented a **Dynamic Hybrid Swarm** coordination model:
  - **Hierarchical Tree by Default**: A `LeadOrchestrator` agent decomposes parent tasks into a `TaskDag`, assigns subtasks to specialized ephemeral worker agents (e.g. `CodeReviewer`, `TestRunner`, `InfrastructureSpecialist`), and aggregates their outputs into the root `SessionContext`.
  - **Dynamic Peer-to-Peer Consensus Channels**: Subagents can establish ephemeral direct communication channels (`SwarmChannel`) to negotiate and achieve consensus without routing all inter-agent traffic through the lead orchestrator.
  - **Tracing & Isolation**: Every subagent runs within its own sub-session context (`ChildSessionContext`), inheriting global rules and workspace security boundaries, with all inter-agent messaging instrumented as OpenTelemetry trace spans (ADR-0014).
  - **Lifecycle**: Managed by `SwarmCoordinator` with bounded concurrency matching the bounded thread pool (CS-0040).

### ADR-0018: Embedded Lightweight SPA Dashboard (Svelte / Vue 3)
- **Status**: Accepted (2026-08-22)
- **Context**: The web interface needs a responsive, modern cyberpunk dashboard providing real-time chat, tool execution streaming, live task graph visualization, and system telemetry matching the TUI aesthetic.
- **Decision**: The Web UI is structured as an embedded lightweight Single Page Application (SPA) compiled to static assets and bundled directly into `omniwrench-web/src/main/resources/static`:
  - **Reactivity & Communication**: Real-time bidirectional communication via WebSocket endpoint `/ws/agent-stream` and REST endpoints `/api/v1/*`.
  - **Zero-External-Server Runtime**: Spring Boot serves the static SPA bundle directly on port 8080 (or configured port) without requiring a separate Node.js server in production.
  - **Cyberpunk Dark Theme**: Matches the terminal TUI color palette (neon cyan `#00ffcc`, magenta `#ff007f`, purple `#7928ca`, dark background `#0d0f18`).
  - **Telemetry & Tracing View**: Visualizes live OpenTelemetry spans and Task DAG status in real time.

### ADR-0019: Cost & Latency Optimized Smart Model Router
- **Status**: Accepted (2026-08-22)
- **Context**: Relying exclusively on expensive frontier models for trivial tasks wastes budget and increases latency, while using small local models for complex reasoning compromises quality.
- **Decision**: Implemented a **Cost & Latency Optimized Smart Router** in `omniwrench-ai`:
  - **Task Complexity Classification**: Prompts are classified into complexity tiers (`TRIVIAL`, `STANDARD`, `COMPLEX`, `EXPERT`) based on prompt tokens, required tool count, multi-step dependencies, and domain flags.
  - **Dynamic Backend Routing**:
    * `TRIVIAL` (e.g., file search, simple shell execution, formatting): Routed to fast local/low-cost models (e.g., local Ollama / llama.cpp / lightweight cloud models) with sub-second latency.
    * `STANDARD` (e.g., unit test generation, single-file edits, REST client calls): Routed to balanced models.
    * `COMPLEX` / `EXPERT` (e.g., cross-module refactoring, architectural planning, multi-agent swarm coordination): Routed to frontier reasoning models (Claude 3.7 Sonnet, Gemini 2.0 Pro, GPT-4o).
  - **Budget & SLA Constraints**: Respects configured annual project limits and per-session cost thresholds.

### ADR-0020: Multi-Tier Security Guardrails and Command Safety Classification
- **Status**: Accepted (2026-08-22)
- **Context**: Autonomous and semi-autonomous AI agents executing filesystem operations and shell commands on production or developer machines pose high risks of accidental data loss or destructive modifications.
- **Decision**: Implemented a **Multi-Tier Security Guardrail** system in `omniwrench-core` and `omniwrench-tools`:
  - **Workspace Path Containment**: Strict boundary checks prevent file reads, writes, and deletions outside the configured `omniwrench.workspace-path` directory tree (canonical path resolution with symlink escape prevention).
  - **Command Safety Classifier (`CommandSafetyEvaluator`)**: Every shell command is categorized into safety tiers:
    * `SAFE_READ_ONLY` (e.g. `ls`, `cat`, `git status`, `mvn test`, `grep`): Auto-approved and executed directly.
    * `MUTATING_SAFE` (e.g. `mkdir`, `touch`, `git add`): Logged and executed with pre-execution workspace state snapshot.
    * `DESTRUCTIVE_HIGH_RISK` (e.g. `rm -rf`, `mkfs`, `sudo`, `dd`, `chmod -R 777`, `killall`, `git push`): Execution is blocked pending explicit interactive human confirmation in the TUI/Web HUD (enforcing CS-0070).
  - **Audit Logging**: Every security evaluation and human approval token is recorded with OpenTelemetry spans and stored in `.omniwrench/audit/security-events.jsonl`.

### ADR-0021: Atomic Task Checkpointing and Resilient Auto-Resume
- **Status**: Accepted (2026-08-22)
- **Context**: Long-running refactorings, multi-module builds, and infrastructure migrations may span hours or be interrupted by network disconnects, process restarts, or developer pauses.
- **Decision**: Implemented an **Atomic Task Checkpointing** engine in `omniwrench-core`:
  - **Single Entity File Store per Step**: Each node execution in a `TaskDag` writes an atomic JSON snapshot to `.omniwrench/tasks/{taskId}/steps/{stepId}.json` containing: step input, tool invocations, stdout/stderr captures, duration, exit code, and modified file hashes (strictly adhering to ADR-0009: no arrays in JSON files).
  - **Crash Detection & Auto-Resume**: On startup, `SessionManager` and `PlanExecutorService` scan `.omniwrench/tasks/` for uncompleted DAGs. The TUI/Web HUD prompts the user: `[R]esume from last checkpoint`, `[S]tep rollback to step N`, or `[A]bort task`.
  - **Idempotency & Replay**: Completed steps with unchanged file hashes are skipped during resume, avoiding redundant expensive tool operations or LLM inferences.

### ADR-0022: Unified Comprehensive Command Palette and Slash Commands
- **Status**: Accepted (2026-08-22)
- **Context**: Developers need rapid, deterministic control over reasoning modes, model backends, git workflows, subagent swarms, checkpoints, and documentation generation directly from the TUI prompt.
- **Decision**: Implemented a unified slash command router (`CommandDispatcher`) in `omniwrench-tui` and `omniwrench-core`:
  - **Core Workflow**:
    * `/plan <goal>`: Triggers Plan-and-Execute DAG synthesis without immediate execution.
    * `/run <command>`: Direct shell command execution with guardrails.
    * `/diff`: Displays colored interactive git workspace diff panel.
    * `/commit [msg]`: Initiates human clearance protocol per CS-0070 and commits staged changes.
  - **AI & Model Control**:
    * `/model <name>`: Switches active inference model (e.g. `claude-3-7-sonnet`, `gemini-2.0-flash`, `llama3.2:latest`).
    * `/backend <id>`: Switches active backend provider (`openai-compat`, `llamacpp`, `huggingface`, `torch`).
    * `/tokens` & `/cost`: Displays real-time session token consumption, breakdown, and estimated cost against project budget.
  - **Multi-Agent & Swarm**:
    * `/swarm [status|list]`: Displays active subagents and inter-agent communication channels.
    * `/tree`: Displays live hierarchical subagent tree.
  - **State & Checkpoints**:
    * `/checkpoint [name]`: Forces immediate atomic DAG state snapshot.
    * `/resume [taskId]`: Resumes interrupted execution from last step.
    * `/rollback [stepId]`: Reverts task execution state to specified step.
  - **Documentation & Reporting**:
    * `/doc-gen`: Updates and builds `mkdocs-kit` documentation suite.
    * `/export-pdf`: Triggers WeasyPrint PDF compilation of documentation.

### ADR-0023: Goal-, Task-, Requirement- and Test-Oriented Governance (No Fixed Calendar Schedule)
- **Status**: Accepted (2026-08-22)
- **Context**: Calendar-driven sprint schedules add artificial ceremony and friction. The project demands an agile, outcome-focused system driven purely by explicit goals, traceable requirements, and automated verification.
- **Decision**: Adopted a **Task-, Goal-, Requirement-, and Test-Oriented Governance System** managed natively through `mkdocs-kit` and markdown documentation:
  - **Zero Calendar Constraints**: No time-boxed sprints or artificial release dates. Progress is tracked strictly by requirement completion and test verification.
  - **Atomic Task Files**: All work is decomposed into `TSK-YYYYMMDD-NNN.md` documents containing:
    * Clear high-level **Goal**
    * Detailed **Context & Architecture Decisions**
    * Traceable **Requirements Checklist** (`[TSK-*.1]`, `[TSK-*.2]`)
    * Execution **Status** (`Not started`, `In progress`, `Completed`)
  - **Living Traceability Matrix**: PlantUML diagrams within `mkdocs-kit` dynamically map:
    `Goal` -> `Requirement (REQ-*)` -> `Architecture Component` -> `Task (TSK-*)` -> `Unit/Integration Test (TEST-*)`
  - **Quality Gates**: A task is only marked `Completed` when all associated tests pass (`mvn clean test`), PMD violations are 0, and documentation builds cleanly (`helpers/build-docs.sh build`).

### ADR-0024: JavaParser as Core Static Analysis and AST Refactoring Engine
- **Status**: Accepted (2026-08-22)
- **Context**: Code refactoring, automated imports cleanup, dead code pruning, and architectural rule verification require parsing source files into structured ASTs, resolving symbols, and outputting clean formatted source code without losing comments or formatting.
- **Decision**: Adopted **JavaParser (3.25+)** with JavaSymbolSolver in `omniwrench-tools`:
  - **Zero Native Dependencies**: Pure Java implementation running portably on Debian 11, Debian 12, and SLES 15 SP6 without JNI or FFM runtime prerequisites.
  - **Full Java 21 Syntax**: Parses records, sealed classes/interfaces, pattern matching for switch, and virtual threads.
  - **Lexical Preservation**: Uses `LexicalPreservingPrinter` to ensure AST refactorings modify only targeted code nodes while preserving developer comments, whitespace, and formatting intact (CS-0020).
  - **AstAnalysisTool Integration**: Implements methods for class hierarchy extraction, method symbol resolution, unused import removal, and nullability rule checking (`Objects.requireNonNull` insertion).

### ADR-0025: Dual Distribution Packaging (GraalVM Native Image + Executable Fat JAR)
- **Status**: Accepted (2026-08-22)
- **Context**: Interactive CLI and TUI workflows demand instantaneous sub-50ms process startup and low memory footprint, while industrial server environments (Debian 11/12, SLES 15 SP6) require reliable background service daemons with standard JRE diagnostics.
- **Decision**: Implemented a **Dual Distribution Packaging** strategy:
  - **GraalVM Native Image (`omniwrench` binary)**: Compiled ahead-of-time (AOT) via `native-maven-plugin` on GraalVM Java 21:
    * Sub-20ms instant startup for command-line and interactive Lanterna TUI operations.
    * Standalone static executable with zero external JRE prerequisites.
    * Configured with reflection configuration hints for Jackson, Lanterna, and Picocli.
  - **Spring Boot Executable Fat JAR (`omniwrench-app.jar`)**: Standard JVM deployment artifact:
    * Runs on standard OpenJDK 17/21 runtime.
    * Bundles embedded Web & WebSocket servers, Spring Security, and dynamic plugin classloaders.
  - **Industrial Systemd Unit Service (`omniwrench.service`)**: Standard daemon unit file managing background dual-mode server instances on Debian and SUSE servers.

### ADR-0026: Built-in Interactive Neon Diff Viewer with Hunk-by-Hunk Control
- **Status**: Accepted (2026-08-22)
- **Context**: Autonomous agent refactorings and code edits must be verified with extreme precision by developers before committing (CS-0070). Relying on plain text unified diffs is slow and error-prone for multi-file edits.
- **Decision**: Implemented a **Built-in Interactive Neon Diff Viewer** (`DiffViewerPanel` in `omniwrench-tui` and Monaco Diff in `omniwrench-web`):
  - **Side-by-Side & Unified Toggle**: Toggle between dual-column split view (Original vs Proposed) and single-column unified diff via `Tab` key.
  - **Syntax & Change Highlighting**: Cyberpunk neon color palette (Neon Cyan `#00ffcc` for additions, Hot Magenta `#ff007f` for deletions, Deep Violet `#7928ca` for line numbers).
  - **Hunk-Level Interactivity**: Keyboard shortcuts to navigate hunks (`j`/`k`, `n`/`p`), stage individual hunks (`s`), discard hunks (`d`), or edit in place (`e`).
  - **Clearance Modal (CS-0070 Enforcement)**: Pressing `c` opens a mandatory approval dialog requiring explicit `[Y]es / [N]o` confirmation with full impact summary and test results before `git commit` is dispatched.

### ADR-0027: Hybrid Local BM25 and Embedded Vector RAG
- **Status**: Accepted (2026-08-22)
- **Context**: Codebases contain both exact keywords/symbols (class names, method signatures, error messages) and semantic concepts (architectural intent, business logic, refactoring rationale). Relying solely on keyword search misses conceptual relationships, while pure vector search is inaccurate for exact identifier lookups.
- **Decision**: Implemented a **Hybrid Local BM25 + Embedded Vector RAG Engine** in `omniwrench-core` stored locally in `.omniwrench/index/`:
  - **Keyword / Symbol Indexing (BM25)**: SQLite FTS5 / Lucene indexes code tokens, file paths, class names, method signatures, and ADR documents with sub-millisecond lexical matching.
  - **Semantic Vector Store**: Local embedded vector indexing (via `sqlite-vec` or local ONNX runtime embeddings) generates dense embeddings for code chunks, doc sections, and task histories with zero cloud dependencies.
  - **Reciprocal Rank Fusion (RRF)**: Merges BM25 and Vector search result sets using RRF scoring:
    $$RRF(d) = \sum_{m \in \{BM25, Vector\}} \frac{1}{k + rank_m(d)}$$
  - **Context-Window Injector (`ContextInjector`)**: Automatically retrieves the top-K most relevant snippets and ADRs to augment prompt context before model dispatch.

### ADR-0028: Comprehensive 5-Stage Verification and Quality Gate Protocol
- **Status**: Accepted (2026-08-22)
- **Context**: Autonomous AI modifications can subtly introduce compiler regressions, styling drift, broken links in documentation, or unintended interface deletions. Strict end-to-end verification gates are mandatory across all tasks.
- **Decision**: Implemented the **Comprehensive 5-Stage Verification Gate** enforced by `QualityGateService` and `omniwrench-helper.sh verify`:
  - **Stage 1 — Clean Compilation (`mvn compile`)**: All modules must compile under Java 21 with `-parameters` and zero compilation warnings/errors.
  - **Stage 2 — Static Analysis Enforcement**:
    * Checkstyle must achieve 0 fatal violations against `checkstyle.xml` (enforcing CS-0030: Javadoc, no magic numbers, no variable hiding, no `var`).
    * PMD must achieve 0 violations against `pmd-ruleset.xml`.
  - **Stage 3 — Test Suite Execution (`mvn test`)**: 100% pass rate on all JUnit 5 unit and integration tests with zero skipped or failing tests.
  - **Stage 4 — Documentation Compilation (`helpers/build-docs.sh build`)**: Full `mkdocs-kit` markdown compilation, PlantUML diagram rendering, and PDF generation must complete with zero broken links and zero diagram errors.
  - **Stage 5 — Deletion & Impact Analysis (CS-0070)**: Automated diff analysis verifies that no public methods or interfaces have been deleted without explicit migration rationale.

### ADR-0029: Pluggable Protocol Abstraction SPI and Home Assistant Integration
- **Status**: Accepted (2026-08-22)
- **Context**: Specialized industrial protocols are deprioritized for the initial milestone in favor of universal web protocols (HTTP/HTTPS/REST/WebSocket) and home/environment automation (Home Assistant), backed by a future-proof pluggable protocol abstraction layer.
- **Decision**: Implemented the **Pluggable Protocol Abstraction SPI** (`ProtocolAdapter`) and **Home Assistant Bridge**:
  - **Universal Web Client (`HttpClientTool` / `WebSocketClientBridge`)**: Reactive, non-blocking HTTP/HTTPS/REST client with JSON schema validation, Bearer/Basic/API-Key authentication, and bi-directional WebSocket connection pooling.
  - **Home Assistant Interface (`HomeAssistantTool`)**:
    * Full integration with Home Assistant REST API (`/api/states`, `/api/services`, `/api/events`).
    * WebSocket API client subscribing to real-time state changes, device discovery, and automation trigger events.
    * Entity state querying, service calls (lights, climate, switches, covers), and telemetry ingestion.
  - **Pluggable Protocol SPI (`ProtocolBridge`)**:
    * Generic lifecycle contract (`connect()`, `disconnect()`, `subscribe()`, `publish()`, `invoke()`).
    * Protocol plugins (e.g. MQTT, Modbus, CoAP, BLE, Zigbee) register as Spring beans or ServiceLoader SPIs, seamlessly bridging into the agent tool registry and event bus.

### ADR-0030: Reactive EventBus Engine via Project Reactor Sinks
- **Status**: Accepted (2026-08-22)
- **Context**: Inter-module communication, subagent swarm notifications, protocol bridge streaming (Home Assistant, WebSocket), and real-time UI updates (TUI dashboard & Web HUD) require a non-blocking, type-safe event dispatcher supporting multicast replay and backpressure.
- **Decision**: Implemented a **Reactive EventBus** in `omniwrench-core` powered by Project Reactor:
  - **Type-Safe Multicast Sink (`ReactorEventBus`)**: Backed by `Sinks.many().multicast().onBackpressureBuffer(1024, false)`, providing non-blocking event publication (`tryEmitNext`) and decoupled consumer subscriptions (`asFlux().publishOn(Schedulers.boundedElastic())`).
  - **Zero-Blocking Architecture**: Thread isolation between publisher threads (e.g. AgentEngine reasoning loop) and slow consumers (e.g. ANSI terminal redraw, network WebSocket frame encoders).
  - **Topic Filtering & Subscriptions**: Type-safe filtering via `Flux.ofType(Class<T>)` and topic matching (`eventBus.onTopic("agent.turn.*")`).
  - **Replay & Late Subscriber Support**: Replay buffer allows newly attached UI sessions (e.g. browser page refresh or TUI panel switch) to receive the last N state updates immediately.

### ADR-0031: Hierarchical Configuration Layering and Secure Secrets Vault
- **Status**: Accepted (2026-08-22)
- **Context**: Omniwrench runs across heterogeneous environments (workstations, CI/CD pipelines, Debian/SUSE servers) and handles sensitive API keys (OpenAI, Claude, Gemini, Home Assistant Long-Lived Tokens, SSH passphrases). Configuration must be flexible while strictly guarding secrets from accidental leakage.
- **Decision**: Implemented **Hierarchical Configuration Layering** and **AES-256 Secrets Vault**:
  - **Precedence Hierarchy (Highest to Lowest)**:
    1. **Command-Line Arguments / Flags** (`--omniwrench.ai.model=...`, `-m ...`)
    2. **Environment Variables** (`OMNIWRENCH_AI_API_KEY`, `OMNIWRENCH_PORT`)
    3. **Workspace-Specific Configuration** (`.omniwrench/config.yml`)
    4. **User-Global Configuration** (`~/.config/omniwrench/config.yml`)
    5. **Application Default Package Defaults** (`omniwrench-app/src/main/resources/application.yml`)
  - **Secrets Masking & Vault**:
    * Sensitive fields (`api-key`, `jwt-secret`, `token`, `password`) are automatically masked in TUI displays, Web API responses, and SLF4J logs (`sk-***...***`).
    * Optional local encrypted secrets file (`.omniwrench/secrets.enc`) secured via AES-256-GCM, derived from a master passphrase or machine-id hash via PBKDF2/Argon2.

### ADR-0032: Unified Matrix CI/CD Workflows for GitHub Actions and Gitea Actions
- **Status**: Accepted (2026-08-22)
- **Context**: Omniwrench is hosted simultaneously on GitHub and self-hosted Gitea instances. Continuous integration workflows must be portable, resilient, and automatically enforce the 5-stage quality verification protocol on all pull requests and pushes.
- **Decision**: Implemented a **Unified Matrix CI/CD Workflow** declared under `.github/workflows/ci.yml` and `.gitea/workflows/ci.yml`:
  - **Job 1 — Code Quality & Static Linting**:
    * Runs on `ubuntu-latest` / Debian runner with Temurin OpenJDK 21.
    * Executes Checkstyle (`mvn checkstyle:check`) and PMD (`mvn pmd:check`) against zero-tolerance rulesets.
  - **Job 2 — Maven Multi-Module Test Suite**:
    * Compiles all 6 modules and executes 100% JUnit 5 test suite across Linux matrix.
    * Collects Surefire and JaCoCo coverage reports as workflow artifacts.
  - **Job 3 — Documentation & PlantUML Compilation**:
    * Executes `./helpers/build-docs.sh build` with `mkdocs-kit`.
    * Verifies zero broken links and compiles standalone HTML site and PDF manuals.
  - **Job 4 — Release Assembly (on tags/master)**:
    * Packages Spring Boot executable Fat JAR (`omniwrench-app.jar`).
    * Compiles GraalVM Native Image binary (`omniwrench`).
    * Publishes multi-module release bundle with SHA-256 checksum manifests.

### ADR-0033: Generational Epoch Compaction and Context Dreaming Engine
- **Status**: Accepted (2026-08-22)
- **Context**: Prolonged agent sessions (multi-day refactorings, extensive test generation, continuous monitoring) produce megabytes of conversation history and tool outputs, exceeding model context budgets and increasing API costs.
- **Decision**: Implemented **Generational Epoch Compaction (Dreaming)** in `omniwrench-core`:
  - **Token Threshold Trigger**: When active conversation tokens exceed a configurable budget (default: 75% of model context window or 64k tokens), a background compaction task is scheduled on the bounded thread pool.
  - **Context Distillation ("Dreaming")**: A lightweight, fast model distills conversational history into structured, non-lossy summary blocks:
    * Identified Goals & Decisions Made
    * Active Working Hypothesis & Current State
    * Modified Files & Symbol Locations
    * Unresolved Issues & Next Steps
  - **Generational Window Rotation**:
    * Current session window transitions to a new generation epoch (`epochId = UUID`), referencing `previousEpochId`.
    * Distilled summary block becomes the preamble for the new active window.
    * Raw previous epoch event files are moved to `.omniwrench/sessions/{sessionId}/archive/{epochId}/` and compressed via zstd/gzip.
  - **Zero Loss of Auditable Trace**: Historical turns remain indexed and searchable via BM25/FTS5 (ADR-0027) without consuming active context tokens.

### ADR-0034: Adaptive Multi-Theme Engine with Dynamic Color Depth Fallback
- **Status**: Accepted (2026-08-22)
- **Context**: Terminals run in various environments (modern GPU-accelerated terminals with 24-bit TrueColor, SSH sessions with 256 colors, Linux virtual consoles `/dev/tty1` with 16 basic ANSI colors). Developers also have diverse aesthetic preferences for visual themes.
- **Decision**: Implemented an **Adaptive Multi-Theme Engine** in `omniwrench-tui`:
  - **Dynamic Color Depth Negotiation**:
    * Inspects `COLORTERM=truecolor|24bit` and `TERM=xterm-256color|...` at startup.
    * Automatically quantizes 24-bit RGB values to the nearest 256-color palette index or 16-color ANSI code using Euclidean RGB distance when running on legacy or remote SSH terminals.
  - **Default Signature Theme**: *Cyberpunk Neon* (Background `#0d0f18`, Primary Cyan `#00ffcc`, Accent Magenta `#ff007f`, Secondary Violet `#7928ca`, Warning Amber `#ffb86c`, Success Emerald `#50fa7b`).
  - **User & Custom Themes**:
    * Loads custom JSON color schemes from `.omniwrench/themes/{name}.json` (e.g. `dracula.json`, `nord.json`, `matrix.json`, `solarized-dark.json`).
    * Hot-switchable at runtime via `/theme <name>` command or `F6` shortcut without restarting the application.

### ADR-0035: In-Memory Actor Channels with Structured Consensus for Subagent Swarms
- **Status**: Accepted (2026-08-22)
- **Context**: Subagents in the Dynamic Hybrid Swarm (ADR-0017) require robust message-passing semantics for multi-agent discussions, code reviews, and architectural trade-off negotiations without race conditions or deadlocks.
- **Decision**: Implemented **In-Memory Actor Channels with Structured Consensus** in `omniwrench-core`:
  - **Virtual Thread Actor Loop**: Each subagent runs in an isolated virtual thread actor with a dedicated mailbox (`LinkedTransferQueue<SwarmEnvelope>`).
  - **Immutable Swarm Envelopes (`SwarmEnvelope`)**:
    * Fields: `messageId`, `senderAgentId`, `recipientAgentId` (or `"swarm:broadcast"`), `conversationTopic`, `messageType` (`PROPOSAL`, `CRITIQUE`, `VOTE`, `QUERY`, `OBSERVATION`), `payload`, `timestamp`.
  - **Consensus Voting Protocol (`ConsensusCoordinator`)**:
    * When subagents face architectural decisions (e.g. library selection, database schema design, refactoring strategy), the lead orchestrator or initiator opens a `ConsensusRound`.
    * Subagents register votes with structured rationale (`Vote(approve=bool, confidence=0.0..1.0, rationale=str)`).
    * Quorum threshold (e.g. $\ge 66\%$ weighted confidence) resolves the consensus into the parent `SessionContext`.
  - **Deadlock & Timeout Guards**: Maximum turn and wall-clock time limits per discussion thread (default: 5 rounds or 60 seconds), failing safely back to the `LeadOrchestrator` decision.

### ADR-0036: Dual Model Context Protocol (MCP) Client and Server Engine
- **Status**: Accepted (2026-08-22)
- **Context**: The open Model Context Protocol (MCP) standard enables universal tool discovery and context sharing across AI ecosystems. Omniwrench must both leverage external tool servers (GitHub, PostgreSQL, Docker, Filesystem) and expose its unique capabilities (AST inspection, Home Assistant, multi-agent engine) to external IDEs.
- **Decision**: Implemented a **Dual MCP Client & Server Engine** in `omniwrench-core` and `omniwrench-tools`:
  - **MCP Client (`McpClientManager`)**:
    * Reads configured external MCP servers from `.omniwrench/mcp-servers.json` (or `~/.config/omniwrench/mcp-servers.json`).
    * Supports both `stdio` (spawning subprocesses with stdin/stdout JSON-RPC framing) and `sse` (HTTP Server-Sent Events with HTTP POST message endpoints) transports.
    * Dynamically registers external MCP tools into Omniwrench's `ToolRegistry` with namespace prefixes (e.g. `mcp:github/create_issue`, `mcp:postgres/query`).
  - **MCP Server (`McpServerHost`)**:
    * Exposes Omniwrench's registered tools, prompt templates, and resource URI schemes (`omniwrench://sessions`, `omniwrench://tasks`) as an MCP server.
    * Operates over Stdio mode (`omniwrench mcp-server --stdio`) for integration into Claude Desktop, Cursor, and VS Code, or over SSE on a dedicated HTTP route (`/mcp/sse`).
  - **Security & Authorization**: All MCP tool executions pass through the 9-level policy engine (ADR-0020) and require human clearance for mutating/destructive operations.

### ADR-0037: Vim-Inspired Modal Navigation and Dedicated Function Keys in TUI
- **Status**: Accepted (2026-08-22)
- **Context**: Efficient keyboard ergonomics are essential for high-velocity software engineering without requiring mouse interaction in terminal environments.
- **Decision**: Implemented **Vim-Inspired Modal Navigation and Dedicated Function Keys** in `omniwrench-tui`:
  - **Modal States**:
    * **Normal / Command Mode (`Esc`)**: `h`/`j`/`k`/`l` or Arrow keys navigate between terminal panels (Chat, Tool Output, Status HUD, Diff Viewer). `Tab`/`Shift+Tab` cycle focus sequentially.
    * **Insert / Prompt Mode (`i` / `Enter`)**: Focuses the input box with full readline editing, history recall (`Up`/`Down`), and syntax highlighting.
  - **Slash Command Trigger (`/`)**: Directly activates the fuzzy-searchable Command Palette modal.
  - **Dedicated Function Key Map**:
    * `F1`: Interactive Help Overlay & Keybinding cheat sheet.
    * `F2`: Smart Model Router Tier Switcher (`TRIVIAL` $\leftrightarrow$ `EXPERT`).
    * `F3`: Workspace File Tree Explorer.
    * `F4`: Dynamic Subagent Swarm Inspector.
    * `F5`: Interactive Neon Diff Viewer (hunk staging `s` / revert `r`).
    * `F6`: Dynamic Theme Switcher modal.
    * `Ctrl+C`: Gracefully aborts current LLM generation or long-running tool subprocess.

### ADR-0038: Embedded SQLite Relational Symbol Graph for AST and Code Intelligence
- **Status**: Accepted (2026-08-22)
- **Context**: Code intelligence, call hierarchy tracing, symbol lookups, and refactoring impact analysis require fast relational queries across classes, interfaces, methods, annotations, and dependencies without re-parsing source files on every prompt.
- **Decision**: Implemented an **Embedded SQLite Relational Symbol Graph** in `.omniwrench/symbols.db`:
  - **Relational Schema**:
    * `files (id, path, last_modified_epoch, sha256_hash)`
    * `symbols (id, file_id, kind, qualified_name, name, start_line, end_line, visibility, return_type)`
    * `symbol_relations (source_symbol_id, target_symbol_id, relation_kind)` (e.g. `EXTENDS`, `IMPLEMENTS`, `CALLS`, `ANNOTATED_WITH`, `OVERRIDES`)
    * `symbols_fts` (SQLite FTS5 virtual table for full-text symbol search)
  - **Incremental File-Watcher Updates**:
    * An asynchronous file system watcher (`WatchService` / inotify) tracks modifications in the workspace.
    * Changed files trigger single-file AST parsing via JavaParser (ADR-0024), incrementally refreshing relational entries inside an atomic SQLite transaction.
  - **Complex Graph Queries**: Enables sub-millisecond queries for caller/callee trees, interface implementation finders, and transitive refactoring impact analysis.

### ADR-0039: Multi-Format Session Exporter and Report Generator
- **Status**: Accepted (2026-08-22)
- **Context**: Pairing sessions, architectural deliberations, code reviews, and autonomous refactoring plans produce valuable technical knowledge that must be shared with human stakeholders, CI pipelines, and external documentation.
- **Decision**: Implemented a **Multi-Format Session Exporter** invoked via `/export <format>`:
  - **1. Markdown & PlantUML (`/export md`)**:
    * Generates a clean GitHub-flavored markdown document containing conversational history, decisions, code diffs, and auto-generated PlantUML sequence/activity diagrams.
    * Written to `doc/docs/reports/{sessionId}.md` and automatically indexed in `mkdocs-kit`.
  - **2. Standalone HTML5 Bundle (`/export html`)**:
    * Single-file standalone HTML5 page with embedded CSS, syntax highlighting, and SVG diagrams for offline viewing without a web server.
  - **3. Printable PDF Manual (`/export pdf`)**:
    * Compiles the session report into a publication-quality PDF with page numbers, table of contents, and vectorized diagrams via `mkdocs-kit` and WeasyPrint.
  - **4. Structured JSON Audit Bundle (`/export json`)**:
    * Complete machine-readable audit trail containing all turn timestamps, LLM prompts/completions, tool inputs/outputs, OpenTelemetry trace IDs, and git commit hashes.

### ADR-0040: Autonomous Local Air-Gapped Mode (--offline / --local)
- **Status**: Accepted (2026-08-22)
- **Context**: Industrial installations, confidential codebases, and disconnected environments require 100% air-gapped execution with guaranteed zero data exfiltration and zero dependency on external cloud services.
- **Decision**: Implemented **Autonomous Local Air-Gapped Mode** (`--offline` / `--local` / `OMNIWRENCH_OFFLINE=true`):
  - **Local Model Routing**: Smart Model Router redirects all inference requests to local OpenAI-compatible or Ollama/llama.cpp/vLLM endpoints (e.g. `http://localhost:11434/v1` or `http://localhost:8000/v1`).
  - **Local-Only Code Intelligence**:
    * AST code intelligence operates entirely via local JavaParser and the embedded SQLite symbol graph (`.omniwrench/symbols.db`).
    * Knowledge retrieval uses local BM25 indexing without remote vector cloud dependencies.
  - **Zero Outbound Network Egress**:
    * Disables all external telemetry, remote update checks, and cloud analytics.
    * OpenTelemetry traces write strictly to the local NDJSON file (`.omniwrench/traces.ndjson`).
    * Enforces strict socket connection filters allowing only localhost loopback interfaces (`127.0.0.1`, `::1`) and explicitly configured local network bridges (e.g. Home Assistant LAN IP).

### ADR-0041: CLI/TUI Plugin Manager with Dynamic Hot Reloading
- **Status**: Accepted (2026-08-22)
- **Context**: Developers need to install, test, upgrade, and reload custom domain tools (AST analyzers, custom protocols, database connectors) on long-running instances without stopping the server or interrupting active pairing sessions.
- **Decision**: Implemented a **CLI/TUI Plugin Manager with Dynamic Hot Reloading** in `omniwrench-core`:
  - **Commands**:
    * `/plugin list` — Display all loaded tools and their source (`BUILT_IN`, `CLASSPATH`, `DYNAMIC_JAR`).
    * `/plugin install <url|path>` — Download or copy plugin JAR to `plugins/`, verify integrity (SHA-256 and SPI manifest), and load into a dedicated child `PluginClassLoader`.
    * `/plugin reload [id]` — Dispose existing classloader, re-scan `plugins/`, and re-instantiate SPI instances into `ToolRegistry` and `ReactorEventBus`.
    * `/plugin remove <id>` — Unregister tool capabilities, release classloader resources, and purge JAR from `plugins/`.
  - **Zero Downtime**: Active sessions maintain references to core engine services while newly reloaded tools become immediately available for subsequent turns.

### ADR-0042: Multi-Channel Notification Mesh for Long-Running Tasks and Clearance Alerts
- **Status**: Accepted (2026-08-22)
- **Context**: Long-running goals (full test suites, multi-module refactorings, documentation builds) or safety clearance prompts (CS-0070) occur asynchronously while developers may have the terminal in the background or be away from their workstations.
- **Decision**: Implemented a **Multi-Channel Notification Mesh** in `omniwrench-core` and `omniwrench-tui`:
  - **1. Terminal OSC Escape Codes**: Emits standard OSC 777 (`\e]777;notify;Omniwrench;{message}\e\\`) and OSC 9 desktop notification sequences recognized by Linux/macOS terminal emulators (WezTerm, Kitty, Ghostty, Alacritty, iTerm2, GNOME Terminal).
  - **2. Terminal Bell**: Emits ANSI bell (`\a`) on completion or failure.
  - **3. Home Assistant Push Notification**: When configured, dispatches `notify.notify` service calls over the `HomeAssistantTool` protocol bridge to send actionable push notifications to mobile companion apps.
  - **4. Subtle Cyberpunk Audio Chimes**: Configurable subtle synthesizer chimes on task completion (`/notify.wav`) and clearance modal display (`/alert.wav`).
  - **Configurable Suppression**: Enabled by default; toggleable via `/notify on|off` or `omniwrench.tui.notifications.enabled=false`.

### ADR-0043: Full Cyberpunk HUD Telemetry Layout on TUI Header and Footer
- **Status**: Accepted (2026-08-22)
- **Context**: Real-time awareness of system state, LLM token expenditure, active subagent swarms, git workspace status, and protocol links provides developers with complete observability during intense coding workflows.
- **Decision**: Implemented **Full Cyberpunk HUD Telemetry** in `omniwrench-tui`:
  - **Top Header Bar**:
    * `[OMNIWRENCH v0.1.0]` (Signature Neon Cyan badge)
    * `Session: {shortSessionId}` (UUID prefix)
    * `Mode: DUAL|TUI|WEB` (Pill badge)
    * `Theme: {themeName}` (F6)
    * `Model: {provider}/{modelName} [{tier}]` (F2)
    * `Tokens: {inputTokens}in / {outputTokens}out (${estimatedCost})`
  - **Bottom Footer Status Bar**:
    * `Git: {branch} [{*dirty|clean}]`
    * `Task: {taskId} [{currentStep}/{totalSteps}]`
    * `Swarm: {activeWorkers} actors`
    * `JVM: {usedMb}/{maxMb}MB | {activeVirtualThreads} vThreads`
    * `HA: CONNECTED|DISCONNECTED` (Home Assistant status pill)
    * `FPS: {currentFps}` (Frame render rate)

### ADR-0044: Unclean Shutdown Detection, Crash Journaling, and State Auto-Recovery
- **Status**: Accepted (2026-08-22)
- **Context**: Unexpected process termination (power loss, terminal emulator crash, SIGKILL, OOM) can leave in-flight tasks and conversation sessions in an interrupted state. Recovery must be effortless, deterministic, and safe.
- **Decision**: Implemented an **Unclean Shutdown Detection and State Auto-Recovery Engine** in `omniwrench-core`:
  - **Active Session Lock & Heartbeat**:
    * An atomic lockfile `.omniwrench/active.lock` is created on startup containing `processId`, `sessionId`, `activeTaskId`, and last heartbeat timestamp (`Instant.now()`).
    * Clean shutdown hook (`Runtime.getRuntime().addShutdownHook`) safely flushes buffers, deletes `active.lock`, and writes `.omniwrench/sessions/{id}/meta.json`.
  - **Crash Journaling (`.omniwrench/crash/{timestamp}.json`)**:
    * JVM uncaught exception handler records thread dumps, memory metrics, active tool calls, and in-flight DAG step state.
  - **Startup Interactive Recovery Prompt**:
    * On startup, if `active.lock` is present and stale (or points to an aborted session), Omniwrench displays an interactive recovery modal:
      > *"⚠️ Unclean shutdown detected from session [abc-123] on task [TSK-20260822-005]. Would you like to resume? [Y/n]"*
    * Accepting restores exact conversation turns, reinstantiates in-flight task DAG steps, and continues execution seamlessly.

### ADR-0045: User-Configurable JSON Keymap Overrides and Leader Key Engine
- **Status**: Accepted (2026-08-22)
- **Context**: Power developers frequently require personalized keyboard shortcuts, Leader key bindings (e.g. `Space f f` to find files, `Space g d` for diff viewer), or alternate key schemes without modifying source code.
- **Decision**: Implemented **User-Configurable JSON Keymap Overrides and Leader Key Engine** in `omniwrench-tui`:
  - **Keymap Directory**: `.omniwrench/keymaps/{profile}.json` (e.g. `default.json`, `vim-leader.json`, `emacs.json`).
  - **Leader Key Sequences**:
    * Supports configurable Leader key (default: `Space` in normal mode).
    * Chained key sequences (e.g. `<leader>ff` -> `/find`, `<leader>gd` -> `/diff`, `<leader>mm` -> `/model`, `<leader>sw` -> `/swarm`).
  - **Action Mapping**: Maps keystrokes directly to internal TUI commands, focus shifts, or slash command invocations.
  - **Hot Reloading**: Editing keymap JSON files triggers instant hot-reload via `WatchService` without restarting the application.

### ADR-0046: TUI Autocompletion Popover and Inline Ghost Text Engine
- **Status**: Accepted (2026-08-22)
- **Context**: Fast, error-free interactive coding in terminal environments requires modern autocompletion for slash commands, relative file paths, model tiers, and subagent roles without cognitive strain.
- **Decision**: Implemented **TUI Autocompletion Popover and Inline Ghost Text Engine** in `omniwrench-tui`:
  - **Inline Ghost Text**:
    * Renders subtle, dimmed inline autocomplete suggestions directly ahead of the cursor for instant visual feedback.
    * `Tab` or `Right Arrow` accepts the completion.
  - **Floating Autocompletion Popover**:
    * When typing `/`, `@` (mentions/roles), or file paths (`./` or relative paths), a floating popup box appears anchored above the prompt input box.
    * Supports fuzzy character matching (`FuzzyMatcher`) with matched substring highlighting.
    * `Up`/`Down` arrows or `Ctrl+N`/`Ctrl+P` navigate candidate entries; `Enter` or `Tab` selects the active candidate.
  - **Context-Aware Completion Providers**:
    * **Slash Commands Provider**: `/plan`, `/run`, `/diff`, `/commit`, `/model`, `/swarm`, `/export`, `/plugin`, `/theme`, `/recover`.
    * **Workspace File Path Provider**: Fuzzy-searches workspace repository files via cached symbol graph.
    * **Model & Swarm Provider**: Autocompletes configured model tiers (`TRIVIAL`..`EXPERT`) and subagent roles.

### ADR-0054: AES-256-GCM Encrypted Secret Vault and OS Keyring Integration
- **Status**: Accepted (2026-08-22)
- **Context**: Omniwrench requires access to various sensitive credentials (OpenAI, Anthropic, Gemini, DeepSeek, GitHub/Gitea tokens, Home Assistant access tokens). Storing plain API keys in source control or cleartext files is a critical security vulnerability.
- **Decision**: Implemented **AES-256-GCM Encrypted Secret Vault and OS Keyring Engine** in `omniwrench-core`:
  - **Vault Storage File**: `.omniwrench/vault.enc` storing authenticated AES-256-GCM encrypted key-value pairs.
  - **Key Derivation Function**: **Argon2id** (memory-hard, GPU/ASIC resistant) deriving the 256-bit AES master encryption key from user passphrase.
  - **Transparent Fallback Hierarchy**:
    1. Direct Environment Variable lookup (e.g. `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`).
    2. OS Native Keyring lookup (via `libsecret` on Linux / GNOME / KDE, macOS Keychain, Windows Credential Manager).
    3. Encrypted `.omniwrench/vault.enc` file (prompts for master password on first access or caches in session memory).
  - **CLI / TUI Secret Management**: Interactive `/vault set <key> <value>`, `/vault list`, `/vault delete <key>`.

### ADR-0055: ZeroMQ Transport Mesh with BSON Binary Serialization for Swarm IPC
- **Status**: Accepted (2026-08-22)
- **Context**: Autonomous subagents and multi-agent swarms require high-throughput, low-latency, resilient inter-process and inter-thread messaging supporting diverse messaging patterns and network topologies.
- **Decision**: Implemented **ZeroMQ Transport Mesh with BSON Serialization** (`omniwrench-core` / `omniwrench-protocol`):
  - **Serialization Engine**: **BSON** (Binary JSON) using Jackson BSON (`jackson-dataformat-bson`) for high-speed schema-flexible binary serialization with zero cleartext serialization overhead.
  - **ZeroMQ Socket Topologies & Patterns**:
    * **PUB / SUB & XPUB / XSUB**: Broadcast event bus and dynamic broker proxies for agent status & telemetry streams.
    * **REQ / REP & DEALER / ROUTER**: Asynchronous request-reply for subagent task dispatching and result retrieval.
    * **PUSH / PULL**: Parallel pipeline fan-out / fan-in for batch task decomposition across worker pools.
  - **Supported Transport Transports**:
    * `inproc://`: High-speed in-memory communication between Virtual Threads.
    * `ipc://`: POSIX domain socket communication between local subagent subprocesses.
    * `tcp://`: Network socket communication for distributed multi-node subagent clusters.
    * `epgm://` / `pgm://` / UDP: Pragmatic General Multicast for zero-loss multicast agent synchronization.
  - **ZeroMQ Implementation**: Pure Java **JeroMQ** engine for 100% portability with zero native C library dependencies, with optional native libzmq acceleration.

### ADR-0056: Asynchronous Java NIO WatchService and Debounced Workspace File Watcher
- **Status**: Accepted (2026-08-22)
- **Context**: Real-time symbol indexing, live diagnostic linting, and TUI file-tree HUD synchronization require instant detection of workspace file modifications without CPU spikes or duplicate event thrashing during rapid saves / batch builds.
- **Decision**: Implemented **Asynchronous Java NIO WatchService and Debounced Workspace File Watcher** in `omniwrench-core`:
  - **Asynchronous Watch Loop**: Dedicated daemon Virtual Thread running Java NIO `WatchService` monitoring project workspace root and recursively registered directories.
  - **Trailing Debounce Aggregator**: 150ms trailing debounce window (`DEBOUNCE_WINDOW_MS = 150`) aggregating rapid bursts of file modification events into single consolidated change sets.
  - **GitIgnore & Artifact Exclusion Filter**: Transparently parses root and subfolder `.gitignore` files + default exclude patterns (`target/`, `.git/`, `.idea/`, `.omniwrench/cache/`, `node_modules/`).
  - **Incremental Indexing Pipeline**: Triggers AST symbol re-parsing (`JavaParserAstTool`), SQLite graph updates (`symbols.db`), and reactive event emission via `ReactorEventBus`.

### ADR-0057: XTerm-256 Color Palette for TUI Visual Theming Engine
- **Status**: Accepted (2026-08-22)
- **Context**: TUI must render consistently across diverse terminal emulators (iTerm2, GNOME Terminal, PuTTY, xterm, tmux/screen multiplexers, WSL terminals) without relying on 24-bit true-color ANSI support that is absent in many server and legacy environments.
- **Decision**: Implemented **XTerm-256 Color Palette Theming Engine** in `omniwrench-tui`:
  - **Color Depth Target**: xterm-256 (`TERM=xterm-256color`) supporting 256-color indexed palette, guaranteeing compatibility across all modern and legacy SSH/terminal environments.
  - **Bundled Themes**: Four named themes defined in JSON files (`.omniwrench/themes/{name}.json`): `default`, `dark`, `solarized`, `high-contrast`.
  - **Live Theme Switch**: `F6` key opens a floating theme-picker popup with XTerm-256 palette preview swatches.
  - **Semantic Color Tokens**: Each theme maps semantic tokens (`primary`, `secondary`, `error`, `warning`, `border`, `selection`, `ghost`) to XTerm-256 color indices for consistent Lanterna attribute application.
  - **Hot-Reload**: Theme JSON files are watched by the file watcher (`ADR-0056`) and applied live without restart.
