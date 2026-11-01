# Omniwrench Operations & Resumption Guide

This guide details the complete procedure for setting up, building, verifying, and resuming development or operations of Omniwrench on a fresh machine or environment.

---

## 1. System Requirements & Prerequisites

| Component | Minimum Version | Recommended Version | Purpose |
|---|---|---|---|
| **Operating System** | Linux (Debian 11/12/13, SLES 15 SP6, Arch, Ubuntu), macOS 13+, Windows (WSL2) | Debian 12/13 / SLES 15 SP6 / Arch Linux | Host runtime |
| **Java JDK** | OpenJDK 17 | Eclipse Temurin 21/25 or GraalVM for JDK 21+ | Spring Boot compilation |
| **GraalVM** | GraalVM 21+ | Oracle GraalVM Community 21+ | Ahead-Of-Time (AOT) Native Image |
| **Build Tool** | Maven 3.8.0+ | Maven 3.9+ | Multi-module reactor lifecycle (supports up to `-T 6`) |
| **Python** | Python 3.10+ | Python 3.11 / 3.12 | Helper scripts & `mkdocs-kit` doc engine |
| **Terminal** | VT100 / xterm-256color | Kitty, WezTerm, Alacritty, iTerm2, tmux 3.2+ | Minimal Stream & Fullscreen Resizable TUI |

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

Verify that Java points to GraalVM / OpenJDK 21+:
```bash
java -version
```

### Step 3: Compile & Execute Full Verification Suite
Run the full 7-reactor Maven build with Checkstyle and PMD static analysis (up to 6 threads allowed):
```bash
mvn -T 6 clean test
# or via helper script:
./omniwrench-helper.sh test
```
*Expected: All 7 modules (`omniwrench`, `omniwrench-core`, `omniwrench-ai`, `omniwrench-tools`, `omniwrench-tui`, `omniwrench-web`, `omniwrench-app`) must pass with 0 test failures, 0 Checkstyle errors, and 0 PMD warnings.*

### Step 4: Build Standalone Executables & Bundles
```bash
# Option A: Traditional Spring Boot fat executable JAR
./omniwrench-helper.sh build-jvm

# Option B: Standalone dynamic GraalVM native binary (< 20ms startup time)
./omniwrench-helper.sh build-native

# Option C: Zero-dependency fully static musl native binary
./omniwrench-helper.sh build-static-native

# Option D: Self-contained Linux AppImage & jlink distribution bundle
./omniwrench-helper.sh build-appimage
```

### Step 5: Compile Documentation Site & A4 PDF Manual
```bash
./helpers/build-docs.sh build
```
The compiled HTML5 documentation (`doc/site/`), A4 PDF manual (`doc/site/documentation.pdf`), and summary diagnostics (`doc/mkdocs-kit.log`) are generated automatically.

---

## 3. Running Omniwrench

### Mode 1A: Full-Screen Dynamically Resizable TUI Workbench (`--fullscreen` / `-f`)
Operates inside the terminal's alternate screen buffer (`\033[?1049h`) with `HOME` and `SESSION` routes, a dockable right `CONTEXT & TELEMETRY` sidebar, 4 responsive viewport breakpoints (`COMPACT`, `STANDARD_SPLIT`, `WIDE_STUDIO`, `ULTRAWIDE_WORKBENCH`), live `/resize <cols> <rows>` reflow, and centered modal overlays (`/palette`, `/themes`, `/help`):
```bash
./omniwrench-helper.sh fullscreen
# or directly via JAR / binary:
java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar tui --fullscreen
java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar -f
```

### Mode 1B: Minimal Inline Stream TUI (`tui` / `--minimal`)
Adapts automatically to local terminals, SSH sessions, or `tmux` panes while preserving native terminal scrollback:
```bash
./omniwrench-helper.sh tui
# or explicitly:
./omniwrench-helper.sh minimal
```
*Features: `/btw` side-channel prompt, `/goal` session tracker, `/theme` XTerm-256 theme switcher, `/env` runtime diagnostics, `/copy` remote OSC 52 clipboard tunneling, `/tools` capabilities inspector, `/clear` screen reset.*

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
java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar -p "Verify system integrity"

# Structured JSON envelope
java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar -p "Check tools status" --json

# Standard input pipe
cat task.txt | java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar -
```

---

## 4. Local Model Integration (`llama.cpp` & Ollama / HuggingFace)

Omniwrench can run 100% offline using local quantized GGUF weights:
1. **Search Models**:
   ```bash
   java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar -p "/model search gemma"
   ```
2. **Pull & Verify Model Weights**:
   ```bash
   java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar -p "/model pull gemma4:e2b"
   ```
   *Model files are verified by SHA-256 checksum and cached in `~/.omniwrench/models/`.*
3. **Execution**:
   - `llama-cli` on `$PATH` is automatically invoked by `LlamaCppBackendAdapter`.
   - In-process native library evaluation is handled by `LlamaCppNativeBridge` and protected by `LlamaCppSignalGuard`.

---

## 5. Architectural Invariants & Quality Rules

When contributing or modifying code, ensure strict compliance with:
- **CS-0010**: Quality Standards, Traceability References, and Naming Conventions.
- **CS-0020**: AI Agent Operational Rules & Empirical Multi-Method Verification.
- **CS-0030**: Strict Java Mission-Critical Coding Standards (No `var`, defensive `Objects.requireNonNull`, complete Javadocs, max 7 parameters per method/constructor, max 140 chars per line).
- **CS-0040**: Zero Magic Numbers (All numeric thresholds, timeouts, and columns as named `UPPER_SNAKE_CASE` constants).
- **CS-0050**: Verifiable Integrity, Bounded Loops, and Resource Safety.
- **CS-0055**: Zero Mocks in Production Code (`src/main/java`).
- **CS-0060**: High-Integrity Deterministic Logic & Total Branching.
- **CS-0070.1**: Human Clearance Required prior to `git commit` or `git push`.
