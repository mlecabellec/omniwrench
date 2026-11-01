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
        echo "Launching Omniwrench in interactive Minimal TUI mode..."
        OMNIWRENCH_MODE=tui mvn spring-boot:run -Dspring-boot.run.arguments="tui"
        ;;
    "tui-full"|"fullscreen"|"fs")
        echo "Launching Omniwrench in Full-Screen Resizable TUI Workbench mode..."
        OMNIWRENCH_MODE=tui OMNIWRENCH_TUI_MODE=fullscreen mvn spring-boot:run -Dspring-boot.run.arguments="tui --fullscreen"
        ;;
    "tui-minimal"|"minimal")
        echo "Launching Omniwrench in Minimal Inline Stream TUI mode..."
        OMNIWRENCH_MODE=tui OMNIWRENCH_TUI_MODE=minimal mvn spring-boot:run -Dspring-boot.run.arguments="tui --minimal"
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
    "build-static-native"|"static-native"|"static")
        echo "Building Omniwrench Fully Static GraalVM Native Image (Approach 2)..."
        shift || true
        exec "$SCRIPT_DIR/helpers/build-static-native.sh" "$@"
        ;;
    "build-appimage"|"appimage"|"bundle")
        echo "Building Omniwrench Self-Contained AppImage Bundle (Approach 3)..."
        shift || true
        exec "$SCRIPT_DIR/helpers/build-appimage.sh" "$@"
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
        echo "Usage: $0 [tui|tui-full|fullscreen|tui-minimal|minimal|web|dual|test|build-jvm|build-native|build-static-native|build-appimage|setup-graalvm|docs|serve-docs]"
        ;;
    *)
        echo "Error: Unknown command '$COMMAND'" >&2
        exit 1
        ;;
esac

