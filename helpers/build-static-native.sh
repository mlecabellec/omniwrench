#!/usr/bin/env bash
# ==============================================================================
# Omniwrench - Approach 2: Fully Static GraalVM Native Image Build Helper
#
# Produces a 100% self-contained, statically linked ELF native executable with
# zero dynamic library dependencies (--static --libc=musl).
#
# Requirements:
#   - Local mode: GraalVM 21+ with native-image and musl toolchain bundle installed.
#   - Container mode: Docker or Podman with ghcr.io/graalvm/native-image-community:21-muslib
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

USE_DOCKER=false
CONTAINER_ENGINE=""
OUTPUT_PATH=""
DO_CLEAN=false
DOCKER_IMAGE="ghcr.io/graalvm/native-image-community:21-muslib"

usage() {
    cat <<EOF
Usage: $0 [options]

Options:
  --docker                  Build using a containerized GraalVM environment with musl libc
  --image <name>            Container image (default: $DOCKER_IMAGE)
  --engine <docker|podman>  Explicit container engine (default: auto-detected)
  -o, --output <path>       Copy the resulting static binary to specified destination
  -c, --clean               Perform a clean build
  -h, --help                Show this help message

Examples:
  $0                        # Local build with host GraalVM + musl
  $0 --docker               # Containerized build using docker/podman
  $0 --docker -o /usr/local/bin/omniwrench
EOF
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --docker)
            USE_DOCKER=true
            shift
            ;;
        --image)
            DOCKER_IMAGE="$2"
            USE_DOCKER=true
            shift 2
            ;;
        --engine)
            CONTAINER_ENGINE="$2"
            USE_DOCKER=true
            shift 2
            ;;
        -o|--output)
            OUTPUT_PATH="$2"
            shift 2
            ;;
        -c|--clean)
            DO_CLEAN=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "Error: Unknown option '$1'" >&2
            usage
            ;;
    esac
done

cd "$ROOT_DIR"

# Auto-detect container engine if container mode requested
if [[ "$USE_DOCKER" == true ]]; then
    if [[ -z "$CONTAINER_ENGINE" ]]; then
        if command -v docker &>/dev/null; then
            CONTAINER_ENGINE="docker"
        elif command -v podman &>/dev/null; then
            CONTAINER_ENGINE="podman"
        else
            echo "Error: Neither 'docker' nor 'podman' was found on PATH." >&2
            exit 1
        fi
    fi
    echo "[INFO] Building static native binary in container using $CONTAINER_ENGINE ($DOCKER_IMAGE)..."

    BUILD_CMD="mvn"
    if [[ "$DO_CLEAN" == true ]]; then
        BUILD_CMD="$BUILD_CMD clean"
    fi
    BUILD_CMD="$BUILD_CMD package -Pnative-static -DskipTests --batch-mode"

    mkdir -p "$HOME/.m2"
    "$CONTAINER_ENGINE" run --rm \
        -v "$ROOT_DIR:/workspace" \
        -v "$HOME/.m2:/root/.m2" \
        -w /workspace \
        "$DOCKER_IMAGE" \
        bash -c "$BUILD_CMD"
else
    # Sourcing local GraalVM environment
    if [[ -f "$ROOT_DIR/graalvm-env.sh" ]]; then
        # shellcheck disable=SC1091
        source "$ROOT_DIR/graalvm-env.sh"
    fi

    if ! command -v native-image &>/dev/null; then
        echo "[WARN] 'native-image' not found on host PATH. Falling back to container mode..."
        USE_DOCKER=true
        exec "$0" --docker ${DO_CLEAN:+--clean} ${OUTPUT_PATH:+-o "$OUTPUT_PATH"}
    fi

    echo "[INFO] Using local GraalVM toolchain:"
    echo "       Java: $(java -version 2>&1 | head -1)"
    echo "       Native Image: $(native-image --version 2>&1 | head -1)"

    MAVEN_GOALS="package"
    if [[ "$DO_CLEAN" == true ]]; then
        MAVEN_GOALS="clean package"
    fi

    echo "[INFO] Compiling static native binary (profile: native-static)..."
    # Execute single-threaded build per host resource constraints
    mvn $MAVEN_GOALS -Pnative-static -DskipTests --batch-mode
fi

STATIC_BIN="$ROOT_DIR/omniwrench-app/target/omniwrench-static"

if [[ ! -f "$STATIC_BIN" ]]; then
    # In case native-image produced 'omniwrench' without '-static' suffix
    if [[ -f "$ROOT_DIR/omniwrench-app/target/omniwrench" ]]; then
        STATIC_BIN="$ROOT_DIR/omniwrench-app/target/omniwrench"
    else
        echo "[ERROR] Native binary was not generated in omniwrench-app/target/" >&2
        exit 1
    fi
fi

echo "[SUCCESS] Static native binary built successfully: $STATIC_BIN"

# Check binary linkage properties
if command -v file &>/dev/null; then
    echo "          $(file "$STATIC_BIN")"
fi
if command -v ldd &>/dev/null; then
    echo "          Linkage status:"
    ldd "$STATIC_BIN" 2>&1 || true
fi

# Optional copy to output destination
if [[ -n "$OUTPUT_PATH" ]]; then
    mkdir -p "$(dirname "$OUTPUT_PATH")"
    cp -f "$STATIC_BIN" "$OUTPUT_PATH"
    chmod +x "$OUTPUT_PATH"
    echo "[SUCCESS] Binary copied to: $OUTPUT_PATH"
fi
