# Configuration & Environment Guide

Omniwrench supports hierarchical configuration via CLI flags, environment variables, and `application.yml`.

## Setting Environment Variables

```bash
# GraalVM SDK and Java Home configuration
export GRAALVM_HOME=/path/to/graalvm-community-openjdk
export JAVA_HOME="$GRAALVM_HOME"
export JDK_HOME="$GRAALVM_HOME"
export PATH="$GRAALVM_HOME/bin:$PATH"

# Set custom web server port
export PORT=9090

# Set execution mode (dual, tui, web)
export OMNIWRENCH_MODE=tui

# Target specific workspace directory
export OMNIWRENCH_WORKSPACE=/home/m/git/omniwrench
```

## GraalVM Auto-Detection & Environment Setup Helper

Omniwrench includes an automated installer helper script in Python:

```bash
# Detect current OS and CPU architecture and download latest GraalVM SDK
python3 helpers/download-graalvm.py --target-dir ~/.graalvm

# Inspect detected architecture without downloading
python3 helpers/download-graalvm.py --detect-only

# Source generated environment variables
source graalvm-env.sh
```

