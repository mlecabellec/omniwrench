# Omniwrench Operations & Resumption Guide

This guide details the complete procedure for setting up, building, verifying, and resuming development or operations of Omniwrench on a fresh machine or environment.

---

## 1. System Requirements & Prerequisites

| Component | Minimum Version | Recommended Version | Purpose |
|---|---|---|---|
| **Operating System** | Linux (Debian 11/12, SLES 15 SP6, Arch, Ubuntu), macOS 13+, Windows (WSL2) | Debian 12 / SLES 15 SP6 / Arch Linux | Host runtime |
| **Java JDK** | OpenJDK 17 | Eclipse Temurin 21 or GraalVM for JDK 21 | Spring Boot compilation |
| **GraalVM** | GraalVM 21+ | Oracle GraalVM Community 21 | Ahead-Of-Time (AOT) Native Image |
| **Build Tool** | Maven 3.8.0+ | Maven 3.9+ (or `./mvnw`) | Multi-module reactor lifecycle |
| **Python** | Python 3.10+ | Python 3.11 / 3.12 | Helper scripts & `mkdocs-kit` doc engine |
| **Terminal** | VT100 / xterm-256color | Kitty, WezTerm, Alacritty, iTerm2, tmux 3.2+ | TUI Cyberpunk dashboard |

---

## 2. Quick Resumption Workflow

### Step 1: Clone Repository
```bash
git clone <git-url> omniwrench
cd omniwrench
```

### Step 2: GraalVM & Environment Provisioning
Omniwrench includes an automated helper to configure GraalVM without requiring root/sudo privileges:
```bash
./omniwrench-helper.sh setup-graalvm
source ./activate-env.sh
```

Verify that Java points to GraalVM 21+:
```bash
java -version
```

### Step 3: Compile & Execute Full Verification Suite
Run the full 7-reactor Maven build with Checkstyle and PMD static analysis:
```bash
mvn clean test
```
*Expected: All 7 modules (`omniwrench-bom`, `omniwrench-core`, `omniwrench-ai`, `omniwrench-tools`, `omniwrench-tui`, `omniwrench-web`, `omniwrench-app`) must pass with 0 test failures, 0 Checkstyle errors, and 0 PMD warnings.*

### Step 4: Build Standalone Executables
```bash
# Option A: Standalone GraalVM native binary (< 20ms startup time)
./omniwrench-helper.sh build-native

# Option B: Traditional Spring Boot fat executable JAR
./omniwrench-helper.sh build-jvm
```

### Step 5: Compile Documentation Site
```bash
./helpers/build-docs.sh build
```
The compiled HTML5 documentation and PDF manuals will be generated in `doc/site/`.

---

## 3. Running Omniwrench

### Mode 1: Cyberpunk Interactive TUI
Adapts automatically to local terminals, SSH sessions, or tmux vertical split panes:
```bash
./omniwrench-helper.sh tui
```
*Features: `/btw` side-channel prompt, `/goal` session tracker, `/env` runtime diagnostics, `/copy` remote OSC 52 clipboard tunneling, `/tools` capabilities inspector, `/clear` screen reset.*

### Mode 2: Web Single-Page HUD & WebSocket Stream
Starts the embedded Spring Web server and serves the OpenCode-inspired Web HUD:
```bash
./omniwrench-helper.sh web
```
Open your browser to:
```text
http://localhost:8080/
```
*Features: Real-time WebSocket streaming (`/ws/agent-stream`), multi-session management, collapsible thinking accordions, multi-line auto-resizing composer, and live diagnostics.*

### Mode 3: Headless CLI (Automation & CI/CD)
```bash
# Single prompt
./omniwrench-app/target/omniwrench -p "Verify system integrity"

# Structured JSON envelope
./omniwrench-app/target/omniwrench -p "Check tools status" --json

# Standard input pipe
cat task.txt | ./omniwrench-app/target/omniwrench -
```

---

## 4. Local Model Integration (llama.cpp & Ollama / HuggingFace)

Omniwrench can run 100% offline using local quantized GGUF weights:
1. **Search Models**:
   ```bash
   ./omniwrench-app/target/omniwrench -p "/model search gemma"
   ```
2. **Pull & Verify Model Weights**:
   ```bash
   ./omniwrench-app/target/omniwrench -p "/model pull gemma4:e2b"
   ```
   *Model files are verified by SHA-256 checksum and cached in `~/.omniwrench/models/`.*
3. **Execution**:
   - `llama-cli` on `$PATH` is automatically invoked by `LlamaCppBackendAdapter`.
   - In-process native library evaluation is handled by `LlamaCppNativeBridge` and protected by `LlamaCppSignalGuard`.
   - Ternary models such as **Bonsai 2** run out of the box via standard `Q2_0` packaging, or via Prism ML's customized `llama.cpp` builds for `PTQ1_0` / `PQ2_0`.

---

## 5. Architectural Invariants & Quality Rules

When contributing or modifying code, ensure strict compliance with:
- **CS-0010**: Hexagonal Architecture & Clean Separation of Concerns.
- **CS-0020**: REST API Design & Explicit DTO Contracts.
- **CS-0030**: Strict Java Mission-Critical Coding Standards (No `var`, defensive `Objects.requireNonNull`, complete Javadocs, no wildcard imports).
- **CS-0040**: Zero Magic Numbers (All numeric thresholds, timeouts, and columns as named `UPPER_SNAKE_CASE` constants).
- **CS-0050**: Documentation & Traceability (`REQ-*`, `FR-*`, `UC-*`, `TSK-*`, `ADR-*` tags).
- **CS-0055**: Zero Mocks in Production Code (`src/main/java`).
- **CS-0060**: Total Branching (Every `if` statement must have an explicit `else` branch).
- **CS-0070.1**: Human Clearance Required prior to `git commit` or `git push`.
