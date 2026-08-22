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
