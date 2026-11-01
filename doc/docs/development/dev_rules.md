# Developer Standards, Build & Verification Guide

Guidelines for building, testing, packaging, and contributing to the Omniwrench multi-module codebase.

---

## 🛠️ Build & Packaging Profiles Matrix

Omniwrench provides four packaging approaches and helper commands tailored for development, JVM fat JAR distribution, dynamic GraalVM Ahead-Of-Time (AOT) native image generation, zero-dependency static `musl` compilation, and self-contained Linux `.AppImage` bundling.

| Profile / Variant | Maven / Helper Invocation | Output Artifact | Startup Time | Use Case |
|---|---|---|---|---|
| **JVM Packaging** (`jvm-package`, default) | `./omniwrench-helper.sh build-jvm`<br>(`mvn clean package -Pjvm-package`) | `omniwrench-app/target/omniwrench-app-*.jar` | ~1.0s | Standard enterprise JVM deployment (OpenJDK 17/21/25) |
| **GraalVM Native Image** (`native`) | `./omniwrench-helper.sh build-native`<br>(`mvn clean package -Pnative -DskipTests`) | `omniwrench-app/target/omniwrench` | <20ms | Ultra-fast standalone binary for instant CLI/TUI pairing |
| **Static `musl` Native Image** (`native-static`) | `./omniwrench-helper.sh build-static-native` | `omniwrench-app/target/omniwrench-static` | <20ms | Zero-glibc portable Linux static ELF executable (`ADR-0060`) |
| **Linux AppImage & `jlink` Bundle** | `./omniwrench-helper.sh build-appimage` | `dist/Omniwrench-*.AppImage` & `.tar.gz` | ~0.8s | Self-contained desktop/server bundle with custom 38 MB `jlink` JRE (`ADR-0061`) |
| **Clean Multi-Module Build** | `mvn -T 6 clean compile` | Modular classes (`target/classes`) | N/A | Parallel compilation and type checking across all 7 reactor artifacts |

### Quick Helper Script Commands (`omniwrench-helper.sh`)

The repository provides `omniwrench-helper.sh` at the project root for simplified lifecycle orchestration:

```bash
# 1. Automatically download, verify SHA-256, and configure latest GraalVM SDK
./omniwrench-helper.sh setup-graalvm

# 2. Activate environment in current terminal session (JAVA_HOME, JDK_HOME, GRAALVM_HOME)
source ./activate-env.sh

# 3. Launch Interactive TUI in Minimal Inline Stream mode (default)
./omniwrench-helper.sh tui
# or explicitly:
./omniwrench-helper.sh minimal

# 4. Launch Interactive TUI in Full-Screen Dynamically Resizable Workbench mode
./omniwrench-helper.sh fullscreen

# 5. Build traditional JVM Spring Boot fat JAR
./omniwrench-helper.sh build-jvm

# 6. Build standalone GraalVM Native Image binary
./omniwrench-helper.sh build-native

# 7. Build zero-dependency static musl binary or AppImage bundle
./omniwrench-helper.sh build-static-native
./omniwrench-helper.sh build-appimage

# 8. Execute complete verification test suite
./omniwrench-helper.sh test
```

---

## 🧪 Testing Strategies & Execution Variants

Omniwrench uses JUnit 5 (Jupiter), AssertJ, and Mockito 5.18+ (with Byte Buddy 1.18.12) configured for complete zero-warning execution across OpenJDK 17, 21, and 25 runtimes.

### 1. Full Multi-Module Test Suite
Runs all unit, boundary, and concurrency stress tests, Checkstyle, and PMD/CPD validation across all 7 reactor artifacts (up to 6 threads allowed):
```bash
mvn -T 6 clean test
# or single-threaded:
mvn clean test
```

### 2. Targeted Single-Module Testing
To run tests for an isolated module:
```bash
# Test Core domain, configuration, and models only
mvn test -pl omniwrench-core

# Test Tool implementations (sandboxed filesystem, subprocesses)
mvn test -pl omniwrench-tools

# Test TUI presentation rendering, Minimal & Fullscreen workbench loops
mvn test -pl omniwrench-tui

# Test Web / REST controllers and WebSocket endpoints
mvn test -pl omniwrench-web

# Test Application Spring Boot bootstrapping and E2E tri-interface prompting
mvn test -pl omniwrench-app
```

### 3. Running Specific Test Classes or Methods
```bash
# Run the Fullscreen TUI Workbench multi-tier test suite
mvn test -Dtest=FullscreenTuiWorkbenchTest

# Run a single test class
mvn test -Dtest=OmniwrenchTuiDashboardTest

# Run a specific test method within a class
mvn test -Dtest=OmniwrenchTuiDashboardTest#shouldProcessInputAndExit
```

### 4. Running with Traceability Tag Filtering
Tests are tagged with requirement IDs (`REQ-*`, `FR-*`, `UC-*`, `TSK-*`). Filter test execution by tag:
```bash
# Run tests covering REQ-00001 (Dual Interface Runtime)
mvn test -Dgroups="REQ-00001"

# Run tests covering TSK-20261101-016 (Dual-Mode Minimal & Fullscreen TUI)
mvn test -Dgroups="TSK-20261101-016"
```

---

## 🚦 Static Quality Gates & Documentation Builds

```bash
# Execute Checkstyle audit (0 violations required; max 7 params/method, max 140 chars/line)
mvn checkstyle:check

# Execute PMD & CPD duplicate code analysis (0 violations required)
mvn pmd:check

# Build MkDocs-Kit HTML5 documentation and A4 PDF manual
./helpers/build-docs.sh build

# Serve live interactive documentation locally on port 8000
./helpers/build-docs.sh serve
```

---

## 📜 Workflow Rules & Governance
1. **Branch Naming**: Feature branches follow `feature/TSK-YYYYMMDD-XXX-description`.
2. **Pre-Commit Verification**: Execute `mvn clean test` and verify 100% pass rate before requesting commit clearance (`CS-0070.3`, `CS-0070.4`).
3. **Commit Authorization**: AI agents must request unambiguous clearance from the human developer before executing `git commit` or `git push` (`CS-0070.1`).
4. **PlantUML Standards**: Maintain syntax compatibility with PlantUML v1.2020.02.
