#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMAND="${1:-tui}"

# Auto-source GraalVM environment if present
if [[ -f "$SCRIPT_DIR/graalvm-env.sh" ]]; then
    source "$SCRIPT_DIR/graalvm-env.sh"
elif [[ -d "$HOME/.graalvm" ]]; then
    GRAALVM_CANDIDATE=$(find "$HOME/.graalvm" -maxdepth 2 -name "bin" -type d | grep -E "jdk|graalvm" | head -1)
    if [[ -n "$GRAALVM_CANDIDATE" ]]; then
        export GRAALVM_HOME="$(dirname "$GRAALVM_CANDIDATE")"
        export JAVA_HOME="$GRAALVM_HOME"
        export JDK_HOME="$GRAALVM_HOME"
        export PATH="$GRAALVM_HOME/bin:$PATH"
    fi
fi

case "$COMMAND" in
    "tui"|"cli")
        echo "Launching Omniwrench in interactive TUI mode..."
        OMNIWRENCH_MODE=tui mvn spring-boot:run -Dspring-boot.run.arguments="tui"
        ;;
    "web"|"server")
        echo "Launching Omniwrench in background Web Server mode..."
        OMNIWRENCH_MODE=web mvn spring-boot:run
        ;;
    "dual")
        echo "Launching Omniwrench in Dual TUI + Web Server mode..."
        OMNIWRENCH_MODE=dual mvn spring-boot:run -Dspring-boot.run.arguments="tui"
        ;;
    "test")
        echo "Executing Omniwrench verification test suite..."
        mvn clean test
        ;;
    "build-jvm"|"package")
        echo "Building Omniwrench JVM Fat JAR artifact..."
        mvn clean package -Pjvm-package -DskipTests
        ;;
    "build-native"|"native")
        echo "Building Omniwrench GraalVM Native Image artifact..."
        mvn clean package -Pnative -DskipTests
        ;;
    "setup-graalvm"|"graalvm")
        echo "Downloading and configuring latest GraalVM SDK..."
        shift || true
        python3 "$SCRIPT_DIR/helpers/download-graalvm.py" "$@"
        ;;
    "docs"|"doc")
        echo "Building Omniwrench documentation with mkdocs-kit..."
        shift || true
        exec "$SCRIPT_DIR/helpers/build-docs.sh" build "$@"
        ;;
    "serve-docs")
        echo "Serving live Omniwrench documentation..."
        shift || true
        exec "$SCRIPT_DIR/helpers/build-docs.sh" serve "$@"
        ;;
    "-h"|"--help"|"help")
        echo "Omniwrench Orchestration & Workbench Helper Tool"
        echo "Usage: $0 [tui|web|dual|test|build-jvm|build-native|setup-graalvm|docs|serve-docs]"
        ;;
    *)
        echo "Error: Unknown command '$COMMAND'" >&2
        exit 1
        ;;
esac

