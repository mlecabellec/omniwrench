# Requirements Register & Traceability Matrix

This register details all formal system requirements for Omniwrench with full traceability to Architectural Decision Records (ADRs), Mission-Critical Standards (`CS-*`), and verification methods.

## Core Runtime & Architecture Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| `REQ-00001` | Dual Interface Runtime Initialization (TUI + Web) | ADR-0001 | Critical | Automated Integration Test | Implemented |
| `REQ-00002` | Configurable Runtime Profiles (`tui`, `web`, `dual`) | ADR-0011 | High | Spring Boot Context Test | Implemented |
| `REQ-00010` | Mission-Critical Coding Standards (`CS-0010` to `CS-0070`) | ADR-0002 | Critical | Checkstyle & PMD Static Gate | Implemented |
| `REQ-00015` | Single-Binary Documentation Kit (`mkdocs-kit`) | ADR-0003 | High | `helpers/build-docs.sh` Execution | Implemented |
| `REQ-00016` | Maven Multi-Module Modular Architecture (6 Modules) | ADR-0016 | Critical | `mvn clean test` Multi-Module Build | Implemented |
| `REQ-00023` | Goal-, Task-, Requirement- and Test-Oriented Governance | ADR-0023 | High | Living Matrix & Task Audit | Implemented |
| `REQ-00025` | Dual Distribution Packaging (GraalVM Native + Fat JAR) | ADR-0025 | High | AOT Native Build & JAR Verify | Specified |
| `REQ-00028` | Comprehensive 5-Stage Verification Protocol | ADR-0028 | Critical | CI/CD Pipeline & Helper Verify | Implemented |
| `REQ-00030` | Reactive EventBus Engine via Reactor Sinks | ADR-0030 | High | Reactive Event Multicast Test | Implemented |
| `REQ-00031` | Hierarchical Config Layering & AES-256 Secrets Vault | ADR-0031 | High | Secret Masking & Encryption Test | Specified |
| `REQ-00032` | Unified CI/CD Matrix Workflows (GitHub & Gitea Actions) | ADR-0032 | High | GitHub/Gitea CI Workflow Run | Implemented |

## AI Engine & Multi-Modal Abstraction Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| `REQ-00040` | Custom Multi-Modal AI Adapter SPI (`MediaType` Sealed) | ADR-0015 | Critical | Typed Generic Serialization Test | Implemented |
| `REQ-00041` | Multi-Provider Pluggable AI Adapters (Cloud & Local) | ADR-0004 | High | Provider Integration Test | Implemented |
| `REQ-00042` | Cost & Latency Optimized Smart Model Router | ADR-0019 | High | Model Routing Tier Benchmark | Specified |
| `REQ-00043` | Hybrid Reasoning Loop (Single-Step vs Plan-Execute DAG) | ADR-0008 | Critical | Complexity Heuristic & DAG Test | Specified |
| `REQ-00044` | Generational Epoch Compaction & Context Dreaming | ADR-0033 | High | Token Threshold Compaction Test | Specified |
| `REQ-00045` | Hybrid Local BM25 and Embedded Vector RAG | ADR-0027 | High | RRF Context Retrieval Test | Specified |

## Multi-Agent & Swarm Collaboration Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| `REQ-00050` | Dynamic Hybrid Swarm Coordination Protocol | ADR-0017 | High | Swarm Concurrency & DAG Test | Specified |
| `REQ-00051` | In-Memory Actor Channels with Structured Consensus | ADR-0035 | High | Quorum Voting & Timeout Test | Specified |
| `REQ-00052` | Atomic Task Checkpointing & Resilient Auto-Resume | ADR-0021 | Critical | Process Interruption & Resume Test | Specified |

## Tooling, Protocols & Integration Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| `REQ-00060` | Polyvalent Base Architecture with Pluggable Tools | ADR-0006 | Critical | Tool SPI Registry Test | Implemented |
| `REQ-00061` | Java `ServiceLoader` Plugin Discovery & Isolation | ADR-0010 | High | Plugin URLClassLoader Test | Specified |
| `REQ-00062` | JavaParser Core Static Analysis & AST Refactoring Tool | ADR-0024 | High | Lexical Preservation AST Test | Specified |
| `REQ-00063` | Pluggable Protocol Bridge SPI & Home Assistant Bridge | ADR-0029 | High | WebSocket / REST Event Test | Implemented |
| `REQ-00064` | Dual Model Context Protocol (MCP) Client & Server | ADR-0036 | High | Stdio/SSE MCP Handshake Test | Specified |
| `REQ-00065` | Multi-Tier Security Guardrails (CS-0070 Clearance) | ADR-0020 | Critical | Command Safety Classifier Test | Specified |

## User Interface & Presentation Requirements

| ID | Title | Traceability | Priority | Verification Method | Status |
|---|---|---|---|---|---|
| `REQ-00070` | Lanterna Multi-Pane Cyberpunk Terminal HUD | ADR-0005 | High | TUI Rendering & Snapshot Test | Implemented |
| `REQ-00071` | Context-Adaptive TUI Layout (Resize Panel Collapse) | ADR-0013 | High | SIGWINCH Resize Layout Test | Specified |
| `REQ-00072` | Adaptive Multi-Theme Engine (24-bit TrueColor to 16-ANSI) | ADR-0034 | Medium | Palette Quantization Test | Specified |
| `REQ-00073` | Unified Command Palette & Slash Commands (`/plan`, `/run`) | ADR-0022 | High | Command Dispatcher Test | Implemented |
| `REQ-00074` | Built-in Interactive Neon Diff Viewer (Hunk Controls) | ADR-0026 | High | Split Diff & Staging Test | Specified |
| `REQ-00075` | Embedded Lightweight SPA Dashboard (Svelte/Vue 3) | ADR-0018 | High | Web HUD & WebSocket Test | Implemented |
| `REQ-00076` | Spring Security API Key Auth (`X-Api-Key`) + Optional JWT | ADR-0012 | High | HTTP 401/200 Security Test | Implemented |
| `REQ-00077` | OpenTelemetry Distributed Tracing & NDJSON Export | ADR-0014 | High | OTLP Span Output Validation | Implemented |

