#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMAND="${1:-tui}"

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
        echo "Usage: $0 [tui|web|dual|test|docs|serve-docs]"
        ;;
    *)
        echo "Error: Unknown command '$COMMAND'" >&2
        exit 1
        ;;
esac
