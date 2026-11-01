# Configuration & Environment Guide

Omniwrench supports hierarchical configuration via CLI flags, environment variables, and `application.yml`.

## 1. Environment Variables Reference

```bash
# GraalVM SDK and Java Home configuration
export GRAALVM_HOME=/path/to/graalvm-community-openjdk
export JAVA_HOME="$GRAALVM_HOME"
export JDK_HOME="$GRAALVM_HOME"
export PATH="$GRAALVM_HOME/bin:$PATH"

# Set custom web server port
export PORT=9090

# Set runtime execution mode (dual, tui, web)
export OMNIWRENCH_MODE=tui

# Set TUI display layout mode (minimal, fullscreen)
export OMNIWRENCH_TUI_MODE=fullscreen

# Set Fullscreen TUI right sidebar visibility policy (auto, show, hide)
export OMNIWRENCH_TUI_SIDEBAR=auto

# Set active XTerm-256 semantic color theme (cyberpunk, opencode, dracula, nord, matrix, high-contrast)
export OMNIWRENCH_TUI_THEME=opencode

# Target specific workspace directory
export OMNIWRENCH_WORKSPACE=/home/m/git/omniwrench

# Optional HuggingFace Token for gated model downloads
export HF_TOKEN="hf_xxxxxxxxxxxxxxxxxxxx"
# Or alternatively:
export HUGGING_FACE_HUB_TOKEN="hf_xxxxxxxxxxxxxxxxxxxx"
```

---

## 2. Spring Boot `application.yml` Properties

```yaml
server:
  port: ${PORT:8080}

omniwrench:
  mode: ${OMNIWRENCH_MODE:dual}
  workspace-path: ${OMNIWRENCH_WORKSPACE:.}
  ai:
    default-provider: ${OMNIWRENCH_AI_PROVIDER:openai}
    model: ${OMNIWRENCH_AI_MODEL:gpt-4o}
  tui:
    display-mode: ${OMNIWRENCH_TUI_MODE:minimal}
    sidebar-mode: ${OMNIWRENCH_TUI_SIDEBAR:auto}
    theme: ${OMNIWRENCH_TUI_THEME:cyberpunk}
    fps-target: ${OMNIWRENCH_TUI_FPS:30}
  engine:
    max-reasoning-steps: ${OMNIWRENCH_MAX_STEPS:50}
    max-threads: ${OMNIWRENCH_MAX_THREADS:8}
```

---

## 3. GraalVM Auto-Detection & Environment Setup Helper

Omniwrench includes an automated installer helper script in Python:

```bash
# Detect current OS and CPU architecture and download latest GraalVM SDK
python3 helpers/download-graalvm.py --target-dir ~/.graalvm

# Inspect detected architecture without downloading
python3 helpers/download-graalvm.py --detect-only

# Source generated environment variables
source graalvm-env.sh
```
