# Developer Standards, Build & Verification Guide

Guidelines for building, testing, packaging, and contributing to the Omniwrench multi-module codebase.

---

## 🛠️ Build & Packaging Profiles Matrix

Omniwrench provides multiple Maven build profiles and helper commands tailored for development, JVM fat JAR distribution, and GraalVM Ahead-Of-Time (AOT) native image generation.

| Profile / Variant | Maven Invocation | Output Artifact | Startup Time | Use Case |
|---|---|---|---|---|
| **JVM Packaging** (`jvm-package`, default) | `mvn clean package -Pjvm-package` | `omniwrench-app/target/omniwrench-app-*.jar` | ~1.5s | Standard enterprise JVM deployment (OpenJDK 17/21/25) |
| **GraalVM Native Image** (`native`) | `mvn clean package -Pnative -DskipTests` | `omniwrench-app/target/omniwrench` | <20ms | Ultra-fast standalone binary for instant CLI/TUI pairing |
| **Clean Multi-Module Build** | `mvn clean compile` | Modular classes (`target/classes`) | N/A | Incremental compilation and type checking across all 6 modules |

### Quick Helper Script Commands

The repository provides [`omniwrench-helper.sh`](file:///home/m/git/omniwrench/omniwrench-helper.sh) for simplified lifecycle orchestration:

```bash
# 1. Automatically download, verify SHA-256, and configure latest GraalVM SDK
./omniwrench-helper.sh setup-graalvm

# 2. Activate environment in current terminal session (JAVA_HOME, JDK_HOME, GRAALVM_HOME)
source ./activate-env.sh

# 3. Build traditional JVM Spring Boot fat JAR
./omniwrench-helper.sh build-jvm

# 4. Build standalone GraalVM Native Image binary
./omniwrench-helper.sh build-native

# 5. Execute complete verification test suite
./omniwrench-helper.sh test
```

---

## 🧪 Testing Strategies & Execution Variants

Omniwrench uses JUnit 5 (Jupiter), AssertJ, and Mockito 5.18+ (with Byte Buddy 1.18.12) configured for complete zero-warning execution across OpenJDK 17, 21, and 25 runtimes.

### 1. Full Multi-Module Test Suite
Runs all unit and boundary tests, Checkstyle, and PMD validation across all 7 reactor artifacts:
```bash
mvn clean test
```

### 2. Targeted Single-Module Testing
To run tests for an isolated module:
```bash
# Test Core domain and models only
mvn test -pl omniwrench-core

# Test Tool implementations (sandboxed filesystem, subprocesses)
mvn test -pl omniwrench-tools

# Test TUI presentation rendering & command loop
mvn test -pl omniwrench-tui

# Test Web / REST controllers and WebSocket endpoints
mvn test -pl omniwrench-web

# Test Application Spring Boot bootstrapping
mvn test -pl omniwrench-app
```

### 3. Running Specific Test Classes or Methods
```bash
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

# Run tests covering Tool Registry features
mvn test -Dgroups="FR-00020"
```

---

## 🚦 Static Quality Gates & Documentation Builds

```bash
# Execute Checkstyle audit (0 violations required)
mvn checkstyle:check

# Execute PMD & CPD duplicate code analysis (0 violations required)
mvn pmd:check

# Build MkDocs-Kit HTML documentation and PDF manuals
./helpers/build-docs.sh build

# Serve live interactive documentation locally on port 8000
./helpers/build-docs.sh serve
```

---

## 📜 Workflow Rules & Governance
1. **Branch Naming**: Feature branches follow `feature/TSK-YYYYMMDD-XXX-description`.
2. **Pre-Commit Verification**: Execute `mvn clean test` and verify 100% pass rate before requesting commit clearance (`CS-0070.3`, `CS-0070.4`).
3. **Commit Authorization**: AI agents must request unambiguous clearance from the human developer before executing `git commit` (`CS-0070.1`).
4. **PlantUML Standards**: Maintain syntax compatibility with PlantUML v1.2020.02.

