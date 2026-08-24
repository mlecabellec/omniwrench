# Modern Cyberpunk TUI Guide

The Omniwrench interactive terminal dashboard provides an ultra-responsive, glowing HUD interface.

```
╔══════════════════════════════════════════════════════════════════════════════╗
║  🛠️  O M N I W R E N C H  ::  AUTONOMOUS DUAL AGENT WORKBENCH               ║
║  Inspired by OpenCode & OpenClaw | Java 17+ Spring Boot                     ║
╚══════════════════════════════════════════════════════════════════════════════╝
 [MODE: DUAL] [SESSIONS: 1] [TOOLS: 2 READY] [WEB PORT: 8080] [JVM: 21]

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

## Built-in Slash Commands

| Command | Syntax / Options | Description |
|---|---|---|
| `/help` | `/help` | Displays command assistance and summary table. |
| `/run` | `/run <command>` | Executes a shell command inside the workspace via `CommandExecutionTool`. |
| `/cat` | `/cat <filepath>` | Reads and displays file contents via `FileOperationsTool` (alias: `/read`). |
| `/thinking` | `/thinking [on\|off\|low\|medium\|high\|max\|status]` | Toggles or configures internal reasoning stream effort level. |
| `/model list` | `/model list` | Lists all locally installed quantized GGUF models in `~/.omniwrench/models/`. |
| `/model search` | `/model search <query>` | Searches Ollama Library and HuggingFace Hub for quantized models. |
| `/model pull` | `/model pull <model_id[:tag]>` | Downloads, verifies SHA-256 integrity, and registers local model weights. |
| `/model rm` | `/model rm <model_id>` | Deletes model weights from local disk cache and updates catalog metadata. |
| `/tools` | `/tools` | Lists all active tools, capabilities, and parameters. |
| `exit` / `quit` | `exit` or `quit` | Gracefully shuts down the interactive TUI session. |

---

## Model Hub Management (`/model`)

Omniwrench integrates directly with the **Ollama Library** and **HuggingFace Model Hub** to discover, download, and manage local quantized GGUF weights.

### Model Search
Search remote repositories across both hubs:
```text
omniwrench> /model search gemma
Model Hub Search Results for 'gemma' (30):
 - gemma4                       [OLLAMA     ] | e2b, e4b, 12b, 26b, 31b | Gemma 4 models are designed to deliver frontier-level performance...
 - gemma3                       [OLLAMA     ] | 270m, 1b, 4b, 12b, 27b  | The current, most capable model that runs on a single GPU.
 - unsloth/gemma-4-E2B-it-GGUF  [HUGGING_FACE] | N/A                     | unsloth/gemma-4-E2B-it-GGUF
```

### Model Pull Syntax
- **Ollama Registry**: Use `<model_name>:<tag>` notation. For example, to pull the `e2b` variant of `gemma4`:
  ```text
  omniwrench> /model pull gemma4:e2b
  ```
- **HuggingFace Hub**: Use `<organization>/<model_repo>` notation:
  ```text
  omniwrench> /model pull unsloth/gemma-4-E2B-it-GGUF
  ```

### Model Listing & Removal
```text
omniwrench> /model list
Locally Installed Models (1):
 - gemma4:e2b                | E2B      | 7.2 GB   | /home/vortigern/.omniwrench/models/gemma4_e2b.gguf

omniwrench> /model rm gemma4:e2b
Successfully removed model: gemma4:e2b
```

---

## Reasoning & Thinking Stream Control (`/thinking`)

Omniwrench demultiplexes reasoning thoughts `<think>...</think>` from the final answer stream in real time.

```text
omniwrench> /thinking status
Reasoning mode is ENABLED (effort level: medium)

omniwrench> /thinking high
Reasoning mode ENABLED with effort level: high

omniwrench> /thinking off
Reasoning mode is now DISABLED.
```

---

## Headless CLI Prompt Execution

Omniwrench supports non-interactive execution for CI/CD automation and scripts:

### Single Direct Prompt (`-p` / `--prompt`)
```bash
omniwrench -p "hello"
```

### Structured JSON Output (`--json`)
```bash
omniwrench -p "hello" --json
```

### Standard Input Pipe (`-`)
```bash
cat task_prompt.txt | omniwrench -
```
