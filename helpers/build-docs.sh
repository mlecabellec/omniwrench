#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

MKDOCS_KIT_LOCAL="/home/vortigern/git/mkdocs-kit"
CACHE_DIR="$ROOT_DIR/.cache"
MKDOCS_KIT_DIR="$CACHE_DIR/mkdocs-kit"
MKDOCS_KIT_VENV="$MKDOCS_KIT_DIR/.venv"
MKDOCS_KIT_CLI="$MKDOCS_KIT_DIR/src/mkdocs_kit/cli.py"

COMMAND="${1:-build}"

setup_mkdocs_kit() {
    mkdir -p "$CACHE_DIR"
    
    if [[ -d "$MKDOCS_KIT_LOCAL/src" ]]; then
        MKDOCS_KIT_DIR="$MKDOCS_KIT_LOCAL"
        MKDOCS_KIT_VENV="$MKDOCS_KIT_LOCAL/.venv"
        MKDOCS_KIT_CLI="$MKDOCS_KIT_LOCAL/src/mkdocs_kit/cli.py"
    elif [[ ! -d "$MKDOCS_KIT_DIR/.git" ]]; then
        echo "[INFO] mkdocs-kit not found locally. Cloning from GitHub..."
        git clone https://github.com/mlecabellec/mkdocs-kit.git "$MKDOCS_KIT_DIR"
    fi
    
    if [[ ! -d "$MKDOCS_KIT_VENV" ]]; then
        echo "[INFO] Creating virtual environment for mkdocs-kit..."
        python3 -m venv "$MKDOCS_KIT_VENV"
        "$MKDOCS_KIT_VENV/bin/pip" install --upgrade pip
        "$MKDOCS_KIT_VENV/bin/pip" install "setuptools<82.0.0"
        "$MKDOCS_KIT_VENV/bin/pip" install mkdocs mkdocs-material weasyprint wireviz nwdiag bit_field
    fi
}

case "$COMMAND" in
    clean)
        echo "[INFO] Cleaning documentation outputs..."
        rm -rf "$ROOT_DIR/doc/site" "$ROOT_DIR/doc/documentation.pdf"
        echo "[SUCCESS] Documentation cleaned."
        ;;
    build)
        setup_mkdocs_kit
        echo "[INFO] Building documentation with mkdocs-kit..."
        cd "$ROOT_DIR/doc"
        PYTHONPATH="$MKDOCS_KIT_DIR/src" "$MKDOCS_KIT_VENV/bin/python3" "$MKDOCS_KIT_CLI" build
        echo "[SUCCESS] Documentation built successfully at: $ROOT_DIR/doc/site/"
        ;;
    serve)
        setup_mkdocs_kit
        echo "[INFO] Serving live documentation with mkdocs-kit..."
        cd "$ROOT_DIR/doc"
        PYTHONPATH="$MKDOCS_KIT_DIR/src" "$MKDOCS_KIT_VENV/bin/python3" "$MKDOCS_KIT_CLI" serve
        ;;
    *)
        echo "Usage: $0 [build|serve|clean]"
        exit 1
        ;;
esac
