#!/usr/bin/env bash
# ==============================================================================
# Omniwrench - Approach 3: Self-Contained AppImage & Native Bundle Builder
#
# Packages Omniwrench, custom minimal JRE runtime (or native binary), bundled
# native shared libraries (llama.cpp/ggml), and CLI tools into a single
# standalone, relocatable Linux AppImage (.AppImage) and .tar.gz bundle.
#
# Usage:
#   ./helpers/build-appimage.sh [options]
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CACHE_DIR="$ROOT_DIR/.cache"
BUILD_DIR="$ROOT_DIR/target/appimage-build"
APP_DIR="$BUILD_DIR/Omniwrench.AppDir"
OUTPUT_DIR="$ROOT_DIR/dist"

MODE="jvm"            # "jvm" (Fat JAR + minimal JRE) or "native" (GraalVM binary)
BUNDLE_ONLY=false     # If true, creates .tar.gz bundle without running appimagetool
DO_CLEAN=false
CUSTOM_LLAMA_CLI=""
CUSTOM_NATIVE_LIBS=""
APP_VERSION="0.1.0-SNAPSHOT"

usage() {
    cat <<EOF
Usage: $0 [options]

Options:
  -m, --mode <jvm|native>   Packaging payload mode (default: jvm)
                            - jvm: bundles Spring Boot Fat JAR + stripped JRE (jlink)
                            - native: bundles GraalVM AOT native binary
  -b, --bundle-only         Produce only relocatable tarball without generating .AppImage
  -o, --output-dir <path>   Output directory (default: $OUTPUT_DIR)
  --llama-cli <path>        Path to native 'llama-cli' binary to bundle into package
  --native-libs <path>      Path to directory containing native .so libraries to embed
  -c, --clean               Clean previous build artifacts before packaging
  -v, --version <str>       Version string (default: $APP_VERSION)
  -h, --help                Show this help message

Examples:
  $0                        # Build AppImage with custom minimal JRE + Fat JAR
  $0 --mode native          # Build AppImage with GraalVM native binary payload
  $0 --llama-cli /usr/bin/llama-cli --native-libs /usr/local/lib
EOF
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -m|--mode)
            MODE="$2"
            shift 2
            ;;
        -b|--bundle-only)
            BUNDLE_ONLY=true
            shift
            ;;
        -o|--output-dir)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --llama-cli)
            CUSTOM_LLAMA_CLI="$2"
            shift 2
            ;;
        --native-libs)
            CUSTOM_NATIVE_LIBS="$2"
            shift 2
            ;;
        -c|--clean)
            DO_CLEAN=true
            shift
            ;;
        -v|--version)
            APP_VERSION="$2"
            shift 2
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

if [[ "$DO_CLEAN" == true ]]; then
    echo "[INFO] Cleaning packaging directories..."
    rm -rf "$BUILD_DIR"
fi

mkdir -p "$BUILD_DIR" "$OUTPUT_DIR" "$CACHE_DIR"

# ------------------------------------------------------------------------------
# 1. Build Payload Artifact (Fat JAR or Native Binary)
# ------------------------------------------------------------------------------
if [[ -f "$ROOT_DIR/graalvm-env.sh" ]]; then
    # shellcheck disable=SC1091
    source "$ROOT_DIR/graalvm-env.sh"
fi

if [[ "$MODE" == "jvm" ]]; then
    JAR_FILE=$(find "$ROOT_DIR/omniwrench-app/target" -name "omniwrench-app-*.jar" ! -name "*plain*" ! -name "*.original" 2>/dev/null | head -1 || true)
    if [[ -z "$JAR_FILE" || ! -f "$JAR_FILE" ]]; then
        echo "[INFO] Compiling Spring Boot executable Fat JAR..."
        mvn clean package -Pjvm-package -DskipTests --batch-mode -q
        JAR_FILE=$(find "$ROOT_DIR/omniwrench-app/target" -name "omniwrench-app-*.jar" ! -name "*plain*" ! -name "*.original" | head -1)
    fi
    echo "[INFO] Using JAR payload: $JAR_FILE"
elif [[ "$MODE" == "native" ]]; then
    NATIVE_BIN="$ROOT_DIR/omniwrench-app/target/omniwrench"
    if [[ ! -f "$NATIVE_BIN" ]]; then
        echo "[INFO] Compiling GraalVM native binary..."
        mvn clean package -Pnative -DskipTests --batch-mode
    fi
    echo "[INFO] Using Native Binary payload: $NATIVE_BIN"
else
    echo "Error: Invalid mode '$MODE'. Supported modes: 'jvm', 'native'." >&2
    exit 1
fi

# ------------------------------------------------------------------------------
# 2. Stage AppDir Hierarchy
# ------------------------------------------------------------------------------
echo "[INFO] Staging AppDir filesystem structure..."
rm -rf "$APP_DIR"
mkdir -p "$APP_DIR/usr/bin" \
         "$APP_DIR/usr/lib/omniwrench" \
         "$APP_DIR/usr/lib/native" \
         "$APP_DIR/usr/share/icons/hicolor/scalable/apps"

# Desktop Specification & Icon
cat <<'EOF' > "$APP_DIR/omniwrench.desktop"
[Desktop Entry]
Type=Application
Name=Omniwrench
Comment=Autonomous Dual TUI & Web Agent Engineering Workbench
Exec=AppRun %F
Icon=omniwrench
Categories=Development;IDE;
Terminal=true
StartupNotify=true
EOF

# Generate 256x256 PNG icon via Python stdlib
python3 -c "
import struct, zlib
width, height = 256, 256
header = b'\x89PNG\r\n\x1a\n'
ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
ihdr = struct.pack('>I', len(ihdr_data)) + b'IHDR' + ihdr_data + struct.pack('>I', zlib.crc32(b'IHDR' + ihdr_data))
raw = bytearray()
for y in range(height):
    raw.append(0)
    for x in range(width):
        r = int(x / width * 180)
        g = int(y / height * 120)
        b = 255
        raw.extend([r, g, b, 255])
compressed = zlib.compress(bytes(raw))
idat = struct.pack('>I', len(compressed)) + b'IDAT' + compressed + struct.pack('>I', zlib.crc32(b'IDAT' + compressed))
iend = struct.pack('>I', 0) + b'IEND' + struct.pack('>I', zlib.crc32(b'IEND'))
with open('$APP_DIR/omniwrench.png', 'wb') as f:
    f.write(header + ihdr + idat + iend)
"
cp "$APP_DIR/omniwrench.png" "$APP_DIR/.DirIcon"
cp "$APP_DIR/omniwrench.png" "$APP_DIR/usr/share/icons/hicolor/scalable/apps/omniwrench.png"

# ------------------------------------------------------------------------------
# 3. Populate Runtime and Dependencies
# ------------------------------------------------------------------------------
if [[ "$MODE" == "jvm" ]]; then
    # Generate minimal lightweight custom JRE using jlink
    JLINK_BIN=$(command -v jlink || echo "")
    if [[ -z "$JLINK_BIN" && -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/jlink" ]]; then
        JLINK_BIN="${JAVA_HOME}/bin/jlink"
    fi

    if [[ -n "$JLINK_BIN" && -x "$JLINK_BIN" ]]; then
        echo "[INFO] Generating minimal JRE runtime via jlink..."
        "$JLINK_BIN" \
            --no-header-files \
            --no-man-pages \
            --strip-debug \
            --compress=2 \
            --add-modules java.base,java.desktop,java.logging,java.naming,java.management,java.security.jgss,java.instrument,java.net.http,java.sql,jdk.unsupported \
            --output "$APP_DIR/usr/lib/jvm"
    else
        echo "[WARN] jlink binary not found. Packaging without bundled JRE."
    fi

    cp "$JAR_FILE" "$APP_DIR/usr/lib/omniwrench/omniwrench-app.jar"
else
    cp "$NATIVE_BIN" "$APP_DIR/usr/bin/omniwrench"
fi

# Bundle llama-cli and native .so libraries if available
if [[ -n "$CUSTOM_LLAMA_CLI" && -f "$CUSTOM_LLAMA_CLI" ]]; then
    echo "[INFO] Bundling custom llama-cli binary: $CUSTOM_LLAMA_CLI"
    cp -f "$CUSTOM_LLAMA_CLI" "$APP_DIR/usr/bin/llama-cli"
    chmod +x "$APP_DIR/usr/bin/llama-cli"
elif command -v llama-cli &>/dev/null; then
    SYSTEM_LLAMA_CLI=$(command -v llama-cli)
    echo "[INFO] Bundling system llama-cli binary: $SYSTEM_LLAMA_CLI"
    cp -f "$SYSTEM_LLAMA_CLI" "$APP_DIR/usr/bin/llama-cli"
    chmod +x "$APP_DIR/usr/bin/llama-cli"
fi

if [[ -n "$CUSTOM_NATIVE_LIBS" && -d "$CUSTOM_NATIVE_LIBS" ]]; then
    echo "[INFO] Bundling native libraries from: $CUSTOM_NATIVE_LIBS"
    cp -rf "$CUSTOM_NATIVE_LIBS"/* "$APP_DIR/usr/lib/native/" 2>/dev/null || true
fi

# ------------------------------------------------------------------------------
# 4. Generate AppRun Entrypoint Script
# ------------------------------------------------------------------------------
cat <<'EOF' > "$APP_DIR/AppRun"
#!/usr/bin/env bash
set -e

APPDIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export APPDIR
export PATH="$APPDIR/usr/bin:$PATH"
export LD_LIBRARY_PATH="$APPDIR/usr/lib/native:$APPDIR/usr/lib:$LD_LIBRARY_PATH"

if [[ -d "$APPDIR/usr/lib/jvm" ]]; then
    export JAVA_HOME="$APPDIR/usr/lib/jvm"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# Launch mode selection
if [[ -f "$APPDIR/usr/bin/omniwrench" ]]; then
    exec "$APPDIR/usr/bin/omniwrench" "$@"
elif [[ -f "$APPDIR/usr/lib/omniwrench/omniwrench-app.jar" ]]; then
    JAVA_EXEC="java"
    if [[ -x "$APPDIR/usr/lib/jvm/bin/java" ]]; then
        JAVA_EXEC="$APPDIR/usr/lib/jvm/bin/java"
    fi
    exec "$JAVA_EXEC" -jar "$APPDIR/usr/lib/omniwrench/omniwrench-app.jar" "$@"
else
    echo "Error: No Omniwrench executable found in AppDir!" >&2
    exit 1
fi
EOF
chmod +x "$APP_DIR/AppRun"

# ------------------------------------------------------------------------------
# 5. Package as AppImage / Relocatable Tarball
# ------------------------------------------------------------------------------
BUNDLE_ARCH="x86_64"
DIST_TARBALL="$OUTPUT_DIR/omniwrench-${APP_VERSION}-bundle-linux-${BUNDLE_ARCH}.tar.gz"
echo "[INFO] Creating relocatable tarball: $DIST_TARBALL"
tar -czf "$DIST_TARBALL" -C "$BUILD_DIR" Omniwrench.AppDir

if [[ "$BUNDLE_ONLY" == true ]]; then
    echo "[SUCCESS] Relocatable application bundle created at: $DIST_TARBALL"
    exit 0
fi

APPIMAGETOOL=""
if command -v appimagetool &>/dev/null; then
    APPIMAGETOOL="appimagetool"
elif [[ -x "$CACHE_DIR/appimagetool" ]]; then
    APPIMAGETOOL="$CACHE_DIR/appimagetool"
else
    echo "[INFO] Downloading standalone appimagetool..."
    APPIMAGETOOL_URL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage"
    if command -v curl &>/dev/null; then
        curl -fSL -o "$CACHE_DIR/appimagetool" "$APPIMAGETOOL_URL" || true
    elif command -v wget &>/dev/null; then
        wget -q -O "$CACHE_DIR/appimagetool" "$APPIMAGETOOL_URL" || true
    fi
    if [[ -f "$CACHE_DIR/appimagetool" ]]; then
        chmod +x "$CACHE_DIR/appimagetool"
        APPIMAGETOOL="$CACHE_DIR/appimagetool"
    fi
fi

TARGET_APPIMAGE="$OUTPUT_DIR/Omniwrench-${APP_VERSION}-${BUNDLE_ARCH}.AppImage"

if [[ -n "$APPIMAGETOOL" && -x "$APPIMAGETOOL" ]]; then
    echo "[INFO] Packaging AppImage using $APPIMAGETOOL..."
    ARCH="$BUNDLE_ARCH" "$APPIMAGETOOL" --appimage-extract-and-run "$APP_DIR" "$TARGET_APPIMAGE" || {
        echo "[WARN] Packaging with --appimage-extract-and-run failed, trying direct execution..."
        ARCH="$BUNDLE_ARCH" "$APPIMAGETOOL" "$APP_DIR" "$TARGET_APPIMAGE" || true
    }
fi

if [[ -f "$TARGET_APPIMAGE" ]]; then
    chmod +x "$TARGET_APPIMAGE"
    echo "[SUCCESS] AppImage package built successfully: $TARGET_APPIMAGE"
else
    echo "[INFO] appimagetool was unable to run in this environment (requires FUSE or AppImage runtime)."
    echo "[INFO] The relocatable standalone bundle is ready and available at: $DIST_TARBALL"
fi
