# Omniwrench 🛠️ — Autonomous Dual Agent Engineering Workbench

**Omniwrench** is a high-integrity, autonomous AI agent framework and tri-interface developer workbench (Headless CLI, Dual-Mode Terminal UI, and Reactive Web HUD) engineered in Java 17/21/25 and Spring Boot 3.2+. It synthesizes the best UX and architectural patterns from **OpenCode**, **Antigravity CLI (`agy`)**, **OpenClaw**, **Claude Code**, **Aider**, and **Charmbracelet Crush**.

---

## ✨ Key Capabilities

1. **Dual-Mode Terminal UI (`omniwrench-tui`, `ADR-0063`)**:
   - **Minimal Inline Stream Mode (`tui` / `--minimal`)**: Preserves native terminal scrollback and mouse text selection (inspired by Claude Code, Aider, and Antigravity CLI inline mode) while rendering themed status pills, demultiplexed reasoning streams, `/btw` side-channel boxes, and OSC 52 `/copy` clipboard tunneling.
   - **Full-Screen Dynamically Resizable Workbench Mode (`--fullscreen` / `-f` / `tui-full`)**: Operates in the alternate screen buffer (`\033[?1049h` $\to$ `\033[?1049l`) with 4 responsive viewport breakpoints (`COMPACT <80`, `STANDARD_SPLIT 80..119`, `WIDE_STUDIO 120..159`, `ULTRAWIDE_WORKBENCH >=160`), dedicated `HOME` and `SESSION` routes, OpenCode-style dockable right `CONTEXT & TELEMETRY` sidebar, ultrawide left `EXPLORER`, live `/resize <cols> <rows>` reflow, code-fence-preserving word wrapping, and centered modal overlays (`/palette`, `/themes`, `/help`).
   - **Semantic XTerm-256 Theme Engine (`TuiThemePalette`)**: 6 built-in themes (`cyberpunk`, `opencode`, `dracula`, `nord`, `matrix`, `high-contrast`) compatible across local terminals, remote SSH shells, and `tmux` panes.
2. **Reactive Single-Page Web HUD (`omniwrench-web`, `ADR-0059`)**:
   - Embedded dark-mode SPA at `http://localhost:8080/` with real-time WebSocket streaming (`/ws/agent-stream` emitting `thought.delta`, `chat.delta`, `chat.complete`), multi-session management, collapsible thinking accordions, and REST APIs (`/api/v1/*`).
3. **Headless CLI & Pipe Automation (`REQ-00089`)**:
   - Single-shot prompt execution (`omniwrench -p "<prompt>"`), structured JSON output (`--json`), and standard input piping (`cat prompt.txt | omniwrench -`).
4. **In-Process & Subprocess Local LLM Engine (`omniwrench-ai`, `ADR-0049`–`0051`)**:
   - Embedded `llama.cpp` native bridge with hardware signal guard (`SIGSEGV`/`SIGBUS`/`SIGILL` fault containment), automatic CPU/GPU architecture detection (`CUDA`, `Metal`, `ROCm`, `Vulkan`, `AVX-512`), and integrated **Ollama Registry** + **HuggingFace Hub** GGUF model manager (`/model search`, `/model pull`, `/model list`, `/model rm`).
5. **Four-Tier Packaging & Distribution (`ADR-0060`–`0062`)**:
   - Executable Spring Boot Fat JAR (`build-jvm`), Dynamic GraalVM Native Image (`build-native`), Zero-Dependency Static `musl` Native Binary (`build-static-native`), and Self-Contained Linux `.AppImage` / `jlink` Bundle (`build-appimage`).

---

## 🚀 Quick Start

### 1. Launch Interactive Terminal UI (Minimal vs. Full-Screen)

```bash
# Launch Minimal Inline Stream TUI (default)
./omniwrench-helper.sh tui
# or explicitly:
./omniwrench-helper.sh minimal

# Launch Full-Screen Dynamically Resizable TUI Workbench
./omniwrench-helper.sh fullscreen
# or via CLI flags on the packaged JAR / native binary:
java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar tui --fullscreen
java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar -f
```

### 2. Launch Web HUD or Dual Mode

```bash
# Launch Web Server mode (http://localhost:8080/)
./omniwrench-helper.sh web

# Launch Dual TUI + Web Server mode
./omniwrench-helper.sh dual
```

### 3. Execute Non-Interactive CLI Prompts

```bash
# Direct single-shot prompt
java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar -p "Summarize workspace tools"

# Structured JSON output for CI/CD scripts
java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar -p "Run diagnostics" --json

# Standard input pipe
echo "List active capabilities" | java -jar omniwrench-app/target/omniwrench-app-0.1.0-SNAPSHOT.jar -
```

---

## 🏗️ Build, Test & Packaging Commands

```bash
# Run full multi-module test suite + Checkstyle + PMD/CPD quality gates (up to 6 threads)
mvn -T 6 clean test
# or via helper:
./omniwrench-helper.sh test

# Package executable JVM Fat JAR
./omniwrench-helper.sh build-jvm

# Package GraalVM Native Image binary
./omniwrench-helper.sh build-native

# Package Zero-Dependency Fully Static musl Native Binary
./omniwrench-helper.sh build-static-native

# Package Self-Contained Linux AppImage & jlink Distribution Bundle
./omniwrench-helper.sh build-appimage

# Compile HTML5 + A4 PDF Documentation via mkdocs-kit
./omniwrench-helper.sh docs
```

---

## 📚 Documentation & User Manual Structure

- **[User Manual Overview & Quick Start](doc/docs/user_guide/overview.md)**
- **[Dual-Mode Cyberpunk TUI User Manual (Minimal & Full-Screen Workbench)](doc/docs/user_guide/tui_guide.md)**
- **[AI Terminal Agents Comparative Analysis (`OpenCode`, `agy`, `OpenClaw`, `Claude Code`, `Aider`, `Crush`)](doc/docs/architecture/tui_agents_analysis.md)**
- **[Web HUD & Telemetry Dashboard Guide](doc/docs/user_guide/web_guide.md)**
- **[Configuration & Environment Reference](doc/docs/user_guide/configuration_guide.md)**
- **[Operations & Resumption Guide](doc/docs/user_guide/resuming_operations.md)**
- **[Developer Standards & Verification Guide](doc/docs/development/dev_rules.md)**
- **[Project Knowledge Base & ADRs (`ADR-0001`–`ADR-0063`)](doc/docs/knowledge/knowledge_base.md)**
