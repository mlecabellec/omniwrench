# Functional Features Register

Comprehensive catalog of user-facing and engine-level functional capabilities provided by Omniwrench.

## Runtime, Architecture & Governance Features

- **`FR-00001` Dual Headless & Interactive Presentation Engine**: Concurrently run a full Lanterna TUI and Spring Boot Web/WebSocket HUD from the same process (`ADR-0001`, `ADR-0011`).
- **`FR-00002` Mission-Critical Zero-Tolerance Quality Gates**: Strict Checkstyle and PMD verification enforcing no wildcards, explicit braces, and high maintainability (`ADR-0002`, `CS-0010` to `CS-0070`).
- **`FR-00003` Single-Binary Documentation Kit**: Embedded generation of HTML5 manuals, offline PDFs, and man pages using `mkdocs-kit` (`ADR-0003`).
- **`FR-00004` Multi-Module Clean Layering**: 6 decoupled modules separating engine, AI SPI, tools, presentation, and assembly (`ADR-0016`).
- **`FR-00005` Goal-Oriented Living Documentation**: Traceable `TSK-*` task register linked directly to code symbols, tests, and ADR decisions (`ADR-0023`).
- **`FR-00006` Dual Distribution Binaries**: Native GraalVM AOT executable (<20ms startup) and Spring Boot Executable Fat JAR (`ADR-0025`).
- **`FR-00007` 5-Stage Verification Protocol**: Automatic validation pipeline (Compile -> Lint -> Test -> Doc -> Deletion Impact) (`ADR-0028`).
- **`FR-00008` Reactive Multi-Tenant EventBus**: Project Reactor Sinks with multicast replay and non-blocking backpressure (`ADR-0030`).
- **`FR-00009` Hierarchical Configuration & AES-256 Vault**: Layered overrides (CLI > Env > YAML) with automated secret masking and AES-256-GCM encrypted vault (`ADR-0031`).
- **`FR-00010` Unified CI/CD Matrix Workflows**: Native GitHub Actions and Gitea Actions workflows testing JDK 21 across all targets (`ADR-0032`).

## AI Intelligence & Reasoning Features

- **`FR-00011` Multi-Modal Typed AI Abstraction**: Sealed `MediaType` hierarchy supporting chat, vision, embedding, audio, and dataflow streams (`ADR-0015`).
- **`FR-00012` Universal Pluggable AI Adapters**: Drop-in adapters for OpenAI, Anthropic, Gemini, Ollama, vLLM, and Llama.cpp (`ADR-0004`).
- **`FR-00013` Dynamic Cost & Latency Smart Router**: Automatic prompt tiering (`TRIVIAL` -> `STANDARD` -> `ADVANCED` -> `EXPERT`) minimizing latency and API budget (`ADR-0019`).
- **`FR-00014` Hybrid Reasoning Loop**: Heuristic switching between reactive single-step execution and multi-step Plan-and-Execute DAGs (`ADR-0008`).
- **`FR-00015` Generational Context Dreaming (Compaction)**: Automatic background summarization and session epoch rotation on large contexts (`ADR-0033`).
- **`FR-00016` Hybrid Local BM25 & Vector RAG**: Fast workspace symbol and documentation search with Reciprocal Rank Fusion (`ADR-0027`).

## Multi-Agent & Swarm Collaboration Features

- **`FR-00017` Dynamic Hybrid Swarm Coordination**: Hierarchical supervisor delegation combined with ephemeral peer-to-peer collaboration (`ADR-0017`).
- **`FR-00018` In-Memory Actor Channels & Consensus**: Virtual-thread actors exchanging typed envelopes with quorum voting rounds (`ADR-0035`).
- **`FR-00019` Atomic Task Checkpointing & Resilient Resume**: Zero-data-loss execution journals under `.omniwrench/tasks/` with auto-recovery (`ADR-0021`).

## Tooling, Protocols & Security Features

- **`FR-00020` Polyvalent Base Tool SPI**: Generic tool contract supporting arbitrary actions, inputs, outputs, and validation (`ADR-0006`, `ADR-0007`).
- **`FR-00021` Isolated Plugin Loader**: Hot-deployable JAR plugins loaded via `ServiceLoader` with dedicated classloaders (`ADR-0010`).
- **`FR-00022` JavaParser AST Static Analysis & Refactoring**: Non-destructive, comment-preserving code modifications (`ADR-0024`).
- **`FR-00023` Pluggable Protocol Bridge & Home Assistant**: WebSocket and REST client for Home Assistant device telemetry and control (`ADR-0029`).
- **`FR-00024` Dual Model Context Protocol (MCP) Support**: Connect to external MCP tools (Stdio/SSE) and expose Omniwrench as an MCP server (`ADR-0036`).
- **`FR-00025` Multi-Tier Security Guardrails**: Path containment and 9-level command safety classifier enforcing interactive confirmation for destructive operations per `CS-0070` (`ADR-0020`).

## User Experience & Interface Features

- **`FR-00026` Multi-Pane Cyberpunk Terminal HUD**: Real-time Lanterna TUI with live status badges, chat stream, and tool panel (`ADR-0005`).
- **`FR-00027` Context-Adaptive TUI Layout**: Automatic panel collapse on terminal window resize (<80, 80-139, 140-159, $\ge$160 columns) (`ADR-0013`).
- **`FR-00028` Adaptive Multi-Theme Engine**: 24-bit TrueColor to 256/16 ANSI color quantization with custom JSON themes (`ADR-0034`).
- **`FR-00029` Unified Command Palette**: Slash commands (`/plan`, `/run`, `/diff`, `/commit`, `/model`, `/swarm`, `/theme`) (`ADR-0022`).
- **`FR-00030` Built-in Interactive Neon Diff Viewer**: Side-by-side terminal diff viewer with hunk-by-hunk staging and revert hotkeys (`ADR-0026`).
- **`FR-00031` Lightweight Embedded SPA Web HUD**: Bundled Svelte/Vue 3 web interface accessible over HTTP and WebSocket (`ADR-0018`).
- **`FR-00032` Secure Web API (`X-Api-Key` & JWT)**: Role-based HTTP authentication and WebSocket session protection (`ADR-0012`).
- **`FR-00033` OpenTelemetry Distributed Tracing**: High-precision OTLP NDJSON span recording for every LLM turn and tool execution (`ADR-0014`).
- **`FR-00034` Vim-Inspired Modal Navigation & Function Keys**: Seamless keyboard navigation via `Esc`/`i`/`h/j/k/l` and dedicated `F1-F6` hotkeys (`ADR-0037`).
- **`FR-00035` SQLite Relational Symbol Graph**: Embedded relational database (`symbols.db`) with FTS5 search and inotify incremental updates (`ADR-0038`).
- **`FR-00036` Multi-Format Session Exporter**: Export pairing sessions to Markdown, standalone HTML5, publication PDF, and JSON audit bundles (`ADR-0039`).
- **`FR-00037` Autonomous Local Air-Gapped Mode**: 100% offline operation (`--offline`) with zero network egress and local LLM routing (`ADR-0040`).
- **`FR-00038` Dynamic Plugin Manager with Hot Reloading**: Install, remove, and hot-reload plugins via `/plugin` without process restarts (`ADR-0041`).
- **`FR-00039` Multi-Channel Notification Mesh**: Terminal OSC 777 desktop notifications, ANSI bell, and Home Assistant push alerts (`ADR-0042`).
- **`FR-00040` Full Cyberpunk HUD Telemetry**: Top header and bottom footer widgets displaying real-time session, model, token, git, swarm, and JVM telemetry (`ADR-0043`).
- **`FR-00041` Unclean Shutdown Auto-Recovery**: Automatic crash detection with interactive `/recover` prompt restoring exact conversation turns and in-flight tasks (`ADR-0044`).
- **`FR-00042` Configurable JSON Keymaps & Leader Keys**: Chained leader sequences (`Space f f`, `Space g d`) with dynamic hot-reload from `.omniwrench/keymaps/` (`ADR-0045`).
- **`FR-00043` TUI Autocompletion Popover & Ghost Text**: Floating fuzzy popover and inline ghost text completions for slash commands, files, and model tiers (`ADR-0046`).
- **`FR-00044` Authenticated Secret Vault & OS Keyring**: Argon2id key derivation and AES-256-GCM encrypted `.omniwrench/vault.enc` with transparent OS keyring fallback (`ADR-0054`).
- **`FR-00045` ZeroMQ & BSON Swarm IPC Mesh**: High-performance multi-agent messaging over ZeroMQ (PUB/SUB, XPUB/XSUB, REQ/REP, PUSH/PULL, multicast) with binary BSON serialization (`ADR-0055`).
- **`FR-00046` Debounced Workspace File Watcher**: Asynchronous Java NIO `WatchService` with 150ms trailing debounce and `.gitignore` glob filtering (`ADR-0056`).

