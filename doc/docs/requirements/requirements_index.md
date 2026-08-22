# Requirements Register & Traceability Matrix

This register details all formal system requirements for Omniwrench with full traceability to Architectural Decision Records (ADRs), Mission-Critical Standards (`CS-*`), and verification methods.

## Core Runtime & Architecture Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| [`REQ-00001`](REQ-00001.md) | Dual Interface Runtime Initialization (TUI + Web) | ADR-0001 | Critical | Automated Integration Test | Implemented |
| [`REQ-00002`](REQ-00002.md) | Configurable Runtime Profiles (`tui`, `web`, `dual`) | ADR-0011 | High | Spring Boot Context Test | Implemented |
| [`REQ-00010`](REQ-00010.md) | Mission-Critical Coding Standards (`CS-0010` to `CS-0070`) | ADR-0002 | Critical | Checkstyle & PMD Static Gate | Implemented |
| [`REQ-00015`](REQ-00015.md) | Single-Binary Documentation Kit (`mkdocs-kit`) | ADR-0003 | High | `helpers/build-docs.sh` Execution | Implemented |
| [`REQ-00016`](REQ-00016.md) | Maven Multi-Module Modular Architecture (6 Modules) | ADR-0016 | Critical | `mvn clean test` Multi-Module Build | Implemented |
| [`REQ-00023`](REQ-00023.md) | Goal-, Task-, Requirement- and Test-Oriented Governance | ADR-0023 | High | Living Matrix & Task Audit | Implemented |
| [`REQ-00025`](REQ-00025.md) | Dual Distribution Packaging (GraalVM Native + Fat JAR) | ADR-0025 | High | AOT Native Build & JAR Verify | Specified |
| [`REQ-00028`](REQ-00028.md) | Comprehensive 5-Stage Verification Protocol | ADR-0028 | Critical | CI/CD Pipeline & Helper Verify | Implemented |
| [`REQ-00030`](REQ-00030.md) | Reactive EventBus Engine via Reactor Sinks | ADR-0030 | High | Reactive Event Multicast Test | Implemented |
| [`REQ-00031`](REQ-00031.md) | Hierarchical Config Layering & AES-256 Secrets Vault | ADR-0031 | High | Secret Masking & Encryption Test | Specified |
| [`REQ-00032`](REQ-00032.md) | Unified CI/CD Matrix Workflows (GitHub & Gitea Actions) | ADR-0032 | High | GitHub/Gitea CI Workflow Run | Implemented |

## AI Engine & Multi-Modal Abstraction Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| [`REQ-00040`](REQ-00040.md) | Custom Multi-Modal AI Adapter SPI (`MediaType` Sealed) | ADR-0015 | Critical | Typed Generic Serialization Test | Implemented |
| [`REQ-00041`](REQ-00041.md) | Multi-Provider Pluggable AI Adapters (Cloud & Local) | ADR-0004 | High | Provider Integration Test | Implemented |
| [`REQ-00042`](REQ-00042.md) | Cost & Latency Optimized Smart Model Router | ADR-0019 | High | Model Routing Tier Benchmark | Specified |
| [`REQ-00043`](REQ-00043.md) | Hybrid Reasoning Loop (Single-Step vs Plan-Execute DAG) | ADR-0008 | Critical | Complexity Heuristic & DAG Test | Specified |
| [`REQ-00044`](REQ-00044.md) | Generational Epoch Compaction & Context Dreaming | ADR-0033 | High | Token Threshold Compaction Test | Specified |
| [`REQ-00045`](REQ-00045.md) | Hybrid Local BM25 and Embedded Vector RAG | ADR-0027 | High | RRF Context Retrieval Test | Specified |

## Multi-Agent & Swarm Collaboration Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| [`REQ-00050`](REQ-00050.md) | Dynamic Hybrid Swarm Coordination Protocol | ADR-0017 | High | Swarm Concurrency & DAG Test | Specified |
| [`REQ-00051`](REQ-00051.md) | In-Memory Actor Channels with Structured Consensus | ADR-0035 | High | Quorum Voting & Timeout Test | Specified |
| [`REQ-00052`](REQ-00052.md) | Atomic Task Checkpointing & Resilient Auto-Resume | ADR-0021 | Critical | Process Interruption & Resume Test | Specified |

## Tooling, Protocols & Integration Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| [`REQ-00060`](REQ-00060.md) | Polyvalent Base Architecture with Pluggable Tools | ADR-0006 | Critical | Tool SPI Registry Test | Implemented |
| [`REQ-00061`](REQ-00061.md) | Java `ServiceLoader` Plugin Discovery & Isolation | ADR-0010 | High | Plugin URLClassLoader Test | Specified |
| [`REQ-00062`](REQ-00062.md) | JavaParser Core Static Analysis & AST Refactoring Tool | ADR-0024 | High | Lexical Preservation AST Test | Specified |
| [`REQ-00063`](REQ-00063.md) | Pluggable Protocol Bridge SPI & Home Assistant Bridge | ADR-0029 | High | WebSocket / REST Event Test | Implemented |
| [`REQ-00064`](REQ-00064.md) | Dual Model Context Protocol (MCP) Client & Server | ADR-0036 | High | Stdio/SSE MCP Handshake Test | Specified |
| [`REQ-00065`](REQ-00065.md) | Multi-Tier Security Guardrails (CS-0070 Clearance) | ADR-0020 | Critical | Command Safety Classifier Test | Specified |

## User Interface & Presentation Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| [`REQ-00070`](REQ-00070.md) | Lanterna Multi-Pane Cyberpunk Terminal HUD | ADR-0005 | High | TUI Rendering & Snapshot Test | Implemented |
| [`REQ-00071`](REQ-00071.md) | Context-Adaptive TUI Layout (Resize Panel Collapse) | ADR-0013 | High | SIGWINCH Resize Layout Test | Specified |
| [`REQ-00072`](REQ-00072.md) | Adaptive Multi-Theme Engine (24-bit TrueColor to 16-ANSI) | ADR-0034 | Medium | Palette Quantization Test | Specified |
| [`REQ-00073`](REQ-00073.md) | Unified Command Palette & Slash Commands (`/plan`, `/run`) | ADR-0022 | High | Command Dispatcher Test | Implemented |
| [`REQ-00074`](REQ-00074.md) | Built-in Interactive Neon Diff Viewer (Hunk Controls) | ADR-0026 | High | Split Diff & Staging Test | Specified |
| [`REQ-00075`](REQ-00075.md) | Embedded Lightweight SPA Dashboard (Svelte/Vue 3) | ADR-0018 | High | Web HUD & WebSocket Test | Implemented |
| [`REQ-00076`](REQ-00076.md) | Spring Security API Key Auth (`X-Api-Key`) + Optional JWT | ADR-0012 | High | HTTP 401/200 Security Test | Implemented |
| [`REQ-00077`](REQ-00077.md) | OpenTelemetry Distributed Tracing & NDJSON Export | ADR-0014 | High | OTLP Span Output Validation | Implemented |
| [`REQ-00078`](REQ-00078.md) | Vim-Inspired Modal Navigation & Function Keys (`F1-F6`) | ADR-0037 | High | Keymap & Focus Traversal Test | Specified |
| [`REQ-00079`](REQ-00079.md) | SQLite Relational Symbol Graph (`.omniwrench/symbols.db`) | ADR-0038 | High | SQL Symbol Relation Queries Test | Specified |
| [`REQ-00080`](REQ-00080.md) | Multi-Format Session Exporter (`md`, `html`, `pdf`, `json`) | ADR-0039 | High | Export Artifact Generation Test | Specified |
| [`REQ-00081`](REQ-00081.md) | Autonomous Local Air-Gapped Mode (`--offline`) | ADR-0040 | Critical | Zero Egress Socket Filter Test | Specified |
| [`REQ-00082`](REQ-00082.md) | CLI/TUI Dynamic Plugin Manager & Hot Reload (`/plugin`) | ADR-0041 | High | PluginClassLoader Dynamic Reload Test | Specified |
| [`REQ-00083`](REQ-00083.md) | Multi-Channel Notification Mesh (OSC 777, Bell, HA Push) | ADR-0042 | Medium | Terminal Escape & Event Push Test | Specified |
| [`REQ-00084`](REQ-00084.md) | Full Cyberpunk HUD Telemetry Layout (Header & Footer) | ADR-0043 | High | TUI Header/Footer Widget Render Test | Specified |
| [`REQ-00085`](REQ-00085.md) | Unclean Shutdown Detection & State Auto-Recovery | ADR-0044 | Critical | Crash Lockfile & Resume State Test | Specified |
| [`REQ-00086`](REQ-00086.md) | User-Configurable JSON Keymap Overrides & Leader Keys | ADR-0045 | High | Automated Keymap Parser & Dispatcher Test | Specified |
| [`REQ-00087`](REQ-00087.md) | TUI Autocompletion Popover & Inline Ghost Text | ADR-0046 | High | Automated Fuzzy Completion & Rendering Test | Specified |


## Advanced Autonomous Agent & Local AI Engine Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| [`REQ-00088`](REQ-00088.md) | Dual Chat Mode with Explicit Reasoning & Thinking Demux | ADR-0047 | Critical | Thinking Stream Extraction Test | Specified |
| [`REQ-00089`](REQ-00089.md) | Unified Tri-Interface Prompting (CLI, TUI, Web UI) | ADR-0048 | Critical | CLI / TUI / Web E2E Test | Specified |
| [`REQ-00090`](REQ-00090.md) | Embedded llama.cpp Local LLM Backend Plugin | ADR-0049 | Critical | llama.cpp In-Memory Inference Test | Specified |
| [`REQ-00091`](REQ-00091.md) | Multi-Source Model Hub Manager (Ollama & HuggingFace) | ADR-0050 | High | Ollama / HF Download & Gemma Test | Specified |
| [`REQ-00092`](REQ-00092.md) | True Implementation Quality Mandate (Zero-Mock Guarantee) | CS-0055 | Critical | Bytecode & Runtime No-Mock Static Audit | Specified |
| [`REQ-00093`](REQ-00093.md) | Advanced File Operations & Binary Transformation Tool Plugin | ADR-0051 | Critical | File Tree / Hex / Diff / Grep Test | Specified |
| [`REQ-00094`](REQ-00094.md) | Asynchronous Background Tool Execution & Callbacks | ADR-0052 | High | Virtual Thread Background Tool Test | Specified |
| [`REQ-00095`](REQ-00095.md) | Ultra-Precise OpenAPI & JSON Schema Function Descriptions | ADR-0053 | Critical | OpenAPI 3.1 Schema & Validation Test | Specified |
| [`REQ-00096`](REQ-00096.md) | AES-256-GCM Encrypted Secret Vault & OS Keyring | ADR-0054 | Critical | Automated Vault Encryption, Keyring & Fallback Test | Specified |
| [`REQ-00097`](REQ-00097.md) | ZeroMQ Transport Mesh with BSON Serialization | ADR-0055 | Critical | ZeroMQ Sockets & BSON Serialization Test | Specified |
