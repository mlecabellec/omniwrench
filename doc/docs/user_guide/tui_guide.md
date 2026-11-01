# Dual-Mode Cyberpunk TUI User Manual (Minimal & Full-Screen Workbench)

Omniwrench provides a dual-mode interactive Terminal User Interface (TUI) engineered in `omniwrench-tui` (`ADR-0063`) and inspired by **OpenCode**, **Antigravity CLI (`agy`)**, **OpenClaw**, **Claude Code**, **Aider**, and **Charmbracelet Crush**. See the [AI Terminal Agents Comparative Analysis](../architecture/tui_agents_analysis.md) for the complete source-level and visual comparison.

---

## 1. Launching Minimal vs. Full-Screen Resizable TUI

Operators can choose between two distinct presentation engines at launch time or via configuration (`omniwrench.tui.display-mode`):

| Mode | Engine Class | CLI Launch Options | Helper Script Command | Core UX Paradigm |
|---|---|---|---|---|
| **Minimal Inline Stream** *(default)* | `MinimalTuiModeEngine` | `omniwrench tui`<br>`omniwrench tui --minimal`<br>`omniwrench --minimal`<br>`omniwrench minimal` | `./omniwrench-helper.sh tui`<br>`./omniwrench-helper.sh minimal` | Operates in the primary screen buffer; preserves native terminal scrollback, mouse selection, and command history. |
| **Full-Screen Resizable Workbench** | `FullscreenTuiModeEngine` + `FullscreenTuiLayout` | `omniwrench --fullscreen` (`-f`)<br>`omniwrench tui --fullscreen`<br>`omniwrench tui -f`<br>`omniwrench tui-full`<br>`omniwrench fullscreen` | `./omniwrench-helper.sh fullscreen`<br>`./omniwrench-helper.sh tui-full` | Operates in the alternate screen buffer (`\033[?1049h` $\to$ `\033[?1049l`); provides multi-pane docking, Home/Session routes, live `/resize` reflow, and centered modal overlays. |

---

## 2. Full-Screen Resizable TUI Workbench Manual (`--fullscreen` / `-f`)

### 2.1 Alternate Screen Lifecycle & Zero-Flicker Redraw
When launched in Full-Screen mode, Omniwrench enters the terminal's alternate screen buffer (`\033[?1049h\033[2J\033[H`), sets the terminal window title (`Omniwrench :: Fullscreen Agent Workbench`) and `tmux` window name (`omniwrench-fs`), and guarantees restoration of the original screen buffer (`\033[?1049l`) upon exit (`exit` / `quit` / `EOF`).

### 2.2 Four Responsive Viewport Breakpoints (`ADR-0013` & `ADR-0063`)
`FullscreenTuiLayout` continuously classifies the terminal's column width into 4 responsive layout topologies:

| Breakpoint | Column Width | Active Panes & Docking Behavior |
|---|---|---|
| **`COMPACT`** | `< 80` cols | **1-Pane Mobile/Split Layout**: Concise header (`⚡ OMNIWRENCH`), full-width center canvas, 3-row composer, and single-line footer. Both sidebars auto-collapse so narrow `tmux` splits remain clutter-free. |
| **`STANDARD_SPLIT`** | `80 .. 119` cols | **2-Pane Split Layout**: Center canvas (`cols - 30`) + Right `CONTEXT & TELEMETRY` Sidebar (`30` cols). |
| **`WIDE_STUDIO`** | `120 .. 159` cols | **OpenCode Studio Layout**: Center canvas (`cols - 38`) + Wide Right `CONTEXT & TELEMETRY` Sidebar (`38` cols) showing session ID, visual context usage bar (`[████░░░░]`), active goal, registered tools, and shortcuts. |
| **`ULTRAWIDE_WORKBENCH`** | `>= 160` cols | **3-Pane Engineering Workbench**: Left `EXPLORER` pane (`26` cols) + Center canvas (`cols - 64`) + Right `CONTEXT & TELEMETRY` Sidebar (`38` cols). |

### 2.3 Navigation Routes (`HOME` vs. `SESSION`)
1. **`Route.HOME` (`HOME :: WELCOME`)**:
   - Displayed when starting a fresh session or when invoking `/home`.
   - Renders a centered welcome banner, active session goal, detected terminal mode/dimensions, active color theme, and a framed Quick-Start Command Card.
   - Submitting any conversational prompt or `/btw` query automatically transitions the workbench from `HOME` to `SESSION`.
2. **`Route.SESSION` (`SESSION TRANSCRIPT`)**:
   - Displays the scrollable conversation transcript (`● USER` and `◆ OMNIWRENCH AGENT` cards).
   - **Demultiplexed Reasoning Stream**: Renders `<think>` blocks inside collapsible `┌─ ⚡ REASONING STREAM [<effort>]` boxes (`/thinking toggle` expands or collapses reasoning boxes across the transcript).
   - **Tool Execution Cards**: Renders executed tool invocations (`┌─ 🔧 TOOL: <name> [OK|ERR] (<callId>)`) with collapsible output (`/tools toggle`).
   - **Inline `/btw` Side-Channel Card**: Displays ephemeral side-channel Q&A at the bottom of the transcript without polluting primary task history (`/btw clear` dismisses the card).
   - **Code-Fence-Preserving Word Wrapping**: Uses `FullscreenTuiLayout.wrapTextPreservingCode()` (inspired by OpenClaw's `transformOutsideCode`) so prose wraps cleanly to the pane width while fenced code blocks (```` ``` ```` / `~~~`) are never corrupted by word wrapping.

```text
 ⚡ OMNIWRENCH :: FULLSCREEN WORKBENCH  [SESSION] [WIDE_STUDIO 140x40] [LOCAL] [THEME:OPENCODE] [THINK:HIGH]
════════════════════════════════════════════════════════════════════════════════════════════════════════════
╭─ SESSION TRANSCRIPT [msgs: 2 | scroll: 0] ─────────────╮╭─ CONTEXT & TELEMETRY ──────────────╮
│ ╭─ ● USER                                              ││ ◆ SESSION & CONTEXT                │
│ │ Verify fullscreen workbench live runtime             ││   ID:   0fee3da2                   │
│ ╰─                                                     ││   Mode: DUAL (:8080)               │
│ ╭─ ◆ OMNIWRENCH AGENT                                  ││   Term: STANDARD 140x40            │
│ │  ┌─ ⚡ REASONING STREAM [high]                       ││   Context Usage:                   │
│ │  │ Analyzing user request and evaluating 4 tools...  ││   [██░░░░░░░░░░░░░░] 2 turns       │
│ │  └─                                                  ││ ◆ ACTIVE GOAL                      │
│ │  ┌─ 🔧 TOOL: file_ops [OK] (call-1)                  ││   Autonomous pair-programming      │
│ │  └─                                                  ││ ◆ TOOLS (4 READY)                  │
│ │ Omniwrench Agent acknowledged request.               ││   • run_command / file_ops         │
│ ╰─                                                     ││ ◆ SHORTCUTS (/palette, /sidebar)   │
╰────────────────────────────────────────────────────────╯╰────────────────────────────────────╯
╭─ ❯ PROMPT COMPOSER [Enter: submit | /palette: commands | /sidebar: toggle | /help] ──────────╮
│ omniwrench (session) ❯ Type a prompt or /command...                                          │
╰──────────────────────────────────────────────────────────────────────────────────────────────╯
 📁 /home/m/git/omniwrench │ • 4 Tools │ ⚙ Think:high │ 🖥 140x40 (WIDE_STUDIO) │ /palette  exit
```

### 2.4 Centered Modal Overlays (`COMMAND_PALETTE`, `THEME_PICKER`, `HELP`)
Full-Screen mode supports floating modal dialogs composited directly over the center canvas:
- **`/palette` (or `/p` or `?`)**: Opens `MODAL :: COMMAND_PALETTE` with categorized commands (`[Suggested]`, `[Agent & Reasoning]`, `[View & System]`).
- **`/themes`**: Opens `MODAL :: THEME_PICKER` listing all 6 semantic XTerm-256 palettes.
- **`/help`**: Opens `MODAL :: HELP` summarizing fullscreen viewport controls.
- **`/close` (or `/dismiss`)**: Closes the active modal overlay (submitting any prompt also auto-dismisses open modals).

---

## 3. Minimal Inline Stream TUI Manual (`tui` / `--minimal`)

When launched via `omniwrench tui` (default) or `omniwrench --minimal`, `MinimalTuiModeEngine` renders directly into the standard terminal stream:

```text
╔══════════════════════════════════════════════════════════════════════════════╗
║  🛠️  O M N I W R E N C H  ::  AUTONOMOUS DUAL AGENT WORKBENCH               ║
║  Inspired by OpenCode & OpenClaw | Java 17+ Spring Boot                     ║
╚══════════════════════════════════════════════════════════════════════════════╝
 [LOCAL]  [MODE: DUAL] [SESSIONS: 1] [TOOLS: 4 READY] [WEB PORT: 8080] [JVM: 21]

omniwrench> /run ls -la
┌── [USER]
│ /run ls -la
└──
┌── [AGENT / OMNIWRENCH]
│ Command executed:
│ Exit Code: 0
│ Output:
│ pom.xml
│ src
│ doc
└──
```

---

## 4. Semantic XTerm-256 Color Themes (`TuiThemePalette`)

Omniwrench includes **6 built-in semantic color themes** (`REQ-00099`, `ADR-0057`, `ADR-0063`) engineered with XTerm-256 (`\033[38;5;<n>m` / `\033[48;5;<n>m`) and ANSI sequences for universal terminal compatibility:

| Theme Name | Command | Primary Accent | Secondary Accent | Visual Character |
|---|---|---|---|---|
| **`cyberpunk`** *(default)* | `/theme cyberpunk` | Neon Cyan (`51`) | Neon Magenta (`201`) | High-contrast synthwave HUD |
| **`opencode`** | `/theme opencode` | Amber Gold (`214`) | Sky Blue (`111`) | Warm studio aesthetic inspired by OpenCode |
| **`dracula`** | `/theme dracula` | Purple (`141`) | Pink (`212`) | Classic Dracula dark palette |
| **`nord`** | `/theme nord` | Frost Cyan (`110`) | Aurora Blue (`109`) | Arctic Nord polar palette |
| **`matrix`** | `/theme matrix` | Phosphor Green (`46`) | Emerald (`40`) | Monochromatic CRT terminal green |
| **`high-contrast`** | `/theme high-contrast` | Pure White (`231`) | Bright Yellow (`226`) | Maximum legibility accessibility theme |

Tip: Running `/theme` without arguments cycles to the next theme in the list!

---

## 5. Terminal Environment Detection & OSC 52 Clipboard Tunneling

Omniwrench automatically inspects environment variables (`TMUX`, `TMUX_PANE`, `SSH_CLIENT`, `SSH_CONNECTION`, `SSH_TTY`, `TERM`, `COLUMNS`, `LINES`) via `TerminalEnvironmentDetector`:

| Mode | Environment Condition | Visual Indicator | Special Capabilities |
|---|---|---|---|
| **`STANDARD`** | Direct desktop console (Alacritty, Kitty, WezTerm, iTerm2, GNOME Terminal) | `[LOCAL]` | Standard OSC window titles, full XTerm-256 / ANSI palette. |
| **`SSH`** | Remote shell (`SSH_CLIENT` or `SSH_CONNECTION` detected) | `[SSH]` / `[SSH: <ip:port>]` | OSC 52 clipboard writes tunneled directly to the local client clipboard. |
| **`TMUX`** | Multiplexer session (`TMUX` or `TMUX_PANE` active, or `TERM=tmux*`) | `[TMUX: %<pane>]` | Tmux DCS passthrough (`\033Ptmux;...\033\\`), tmux window renaming (`\033k...`), automatic narrow column adaptation. |

---

## 6. Complete Slash Command Reference

| Command | Syntax / Options | Supported Modes | Description |
|---|---|---|---|
| `/palette` | `/palette`, `/p`, `?` | Fullscreen | Opens the centered **Command Palette Modal** overlay. |
| `/themes` | `/themes` | Fullscreen | Opens the centered **Theme Picker Modal** overlay. |
| `/help` | `/help` | Minimal & Fullscreen | Prints command summary (Minimal) or opens the **Help Modal** (Fullscreen). |
| `/close` | `/close`, `/dismiss` | Fullscreen | Dismisses the active modal overlay. |
| `/home` | `/home` | Fullscreen | Switches the center canvas to the centered **Home Welcome** route. |
| `/session` | `/session`, `/chat` | Fullscreen | Switches the center canvas to the **Session Transcript** route. |
| `/sidebar` | `/sidebar [auto\|show\|hide\|toggle]` | Fullscreen | Controls visibility of the Right Context & Telemetry Sidebar and Left Explorer. |
| `/resize` | `/resize <cols> <rows>` | Fullscreen | Dynamically updates viewport dimensions and reflows all panes across breakpoints. |
| `/scroll` | `/scroll <up\|down\|top\|bottom>` | Fullscreen | Scrolls the session transcript history up, down, to top, or back to bottom. |
| `/theme` | `/theme [name]` | Minimal & Fullscreen | Switches to a named theme (`cyberpunk`, `opencode`, `dracula`, `nord`, `matrix`, `high-contrast`) or cycles to the next theme. |
| `/thinking` | `/thinking [toggle\|on\|off\|low\|medium\|high\|max\|status]` | Minimal & Fullscreen | Toggles reasoning box visibility in Fullscreen (`toggle`) or configures `AgentEngine` reasoning effort. |
| `/tools` | `/tools [toggle]` | Minimal & Fullscreen | Lists registered tools (Minimal) or toggles tool execution output expansion (Fullscreen). |
| `/btw` | `/btw <question>` / `/btw clear` | Minimal & Fullscreen | Executes an ephemeral side-channel query without polluting the main conversation thread. |
| `/goal` | `/goal [objective]` | Minimal & Fullscreen | Displays or updates the active session goal in the HUD / sidebar. |
| `/env` | `/env` | Minimal | Displays operating system, architecture, terminal mode, dimensions, and JVM runtime details. |
| `/copy` | `/copy` | Minimal & Fullscreen | Copies the last assistant response to your system clipboard using OSC 52 tunneling. |
| `/clear` | `/clear` | Minimal & Fullscreen | Clears the terminal screen (`\033[2J\033[H`) or resets Fullscreen notices/scroll. |
| `/run` | `/run <command>` | Minimal & Fullscreen | Executes a shell command inside the workspace via `CommandExecutionTool`. |
| `/cat` | `/cat <filepath>` | Minimal & Fullscreen | Reads and displays file contents via `FileOperationsTool` (alias: `/read`). |
| `/model` | `/model <list\|search\|pull\|rm> [arg]` | Minimal & Fullscreen | Manages local quantized GGUF models from Ollama Library and HuggingFace Hub. |
| `exit` / `quit` | `exit` or `quit` | Minimal & Fullscreen | Gracefully exits the TUI session (restoring the normal screen buffer from Fullscreen mode). |

---

## 7. Model Hub Management (`/model`)

Omniwrench integrates directly with the **Ollama Library** and **HuggingFace Model Hub** to discover, download, and manage local quantized GGUF weights:

```text
omniwrench> /model search gemma
Model Hub Search Results for 'gemma' (30):
 - gemma4                       [OLLAMA     ] | e2b, e4b, 12b, 26b, 31b | Gemma 4 models...
 - unsloth/gemma-4-E2B-it-GGUF  [HUGGING_FACE] | N/A                     | unsloth/gemma-4-E2B-it-GGUF

omniwrench> /model pull gemma4:e2b
omniwrench> /model list
omniwrench> /model rm gemma4:e2b
```

---

## 8. Headless CLI Prompt Execution

Omniwrench supports non-interactive execution for CI/CD automation and scripts:

```bash
# Single direct prompt (-p / --prompt)
omniwrench -p "hello"

# Structured JSON output (--json)
omniwrench -p "hello" --json

# Standard input pipe (-)
cat task_prompt.txt | omniwrench -
```
