package com.omniwrench.tools;

import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite verifying AdvancedFileOperationsTool operations: tree, grep, patch, slice, hex encoding, and async execution.
 *
 * Traceability:
 * - Requirement: REQ-00060 (Pluggable Tools), REQ-00093 (Multi-Arch Runtime), REQ-00094 (Async Background Tools)
 * - Task: TSK-20260822-011 (Advanced File Operations &amp; Background Tool Plugin)
 * - ADR: ADR-0006 (Polyvalent Tool Architecture), ADR-0052 (Asynchronous Tool Execution)
 */
@Tag("REQ-00060")
@Tag("REQ-00093")
@Tag("REQ-00094")
@Tag("TSK-20260822-011")
class AdvancedFileOperationsToolTest {

    @TempDir
    Path tempDir;

    private AdvancedFileOperationsTool tool;
    private SessionContext context;

    @BeforeEach
    void setUp() {
        tool = new AdvancedFileOperationsTool();
        context = SessionContext.createDefault(tempDir.toString());
    }

    @Test
    @DisplayName("Should walk directory tree with depth and pattern filtering")
    void testTreeWalking() throws IOException {
        final Path subDir = Files.createDirectory(tempDir.resolve("src"));
        Files.writeString(subDir.resolve("Main.java"), "class Main {}");
        Files.writeString(tempDir.resolve("README.md"), "# Omniwrench");

        final ToolInvocation result = tool.execute(context, Map.of(
                "action", "tree",
                "path", ".",
                "max_depth", 3
        ));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("📁 src").contains("📄 Main.java").contains("📄 README.md");
    }

    @Test
    @DisplayName("Should grep regex patterns across directory files")
    void testGrepSearch() throws IOException {
        final Path subDir = Files.createDirectory(tempDir.resolve("code"));
        Files.writeString(subDir.resolve("App.java"), "public class App {\n  String message = \"TARGET_VALUE\";\n}");

        final ToolInvocation result = tool.execute(context, Map.of(
                "action", "grep",
                "path", "code",
                "pattern", "TARGET_[A-Z]+"
        ));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("App.java:2: String message = \"TARGET_VALUE\";");
    }

    @Test
    @DisplayName("Should patch targeted content within a file")
    void testPatchApplication() throws IOException {
        final Path targetFile = tempDir.resolve("config.yaml");
        Files.writeString(targetFile, "server:\n  port: 8080\n  mode: dev\n");

        final ToolInvocation result = tool.execute(context, Map.of(
                "action", "patch",
                "path", "config.yaml",
                "target_content", "port: 8080",
                "replacement_content", "port: 9090"
        ));

        assertThat(result.isSuccess()).isTrue();
        final String updated = Files.readString(targetFile);
        assertThat(updated).contains("port: 9090").doesNotContain("port: 8080");
    }

    @Test
    @DisplayName("Should slice line range from file")
    void testSliceLines() throws IOException {
        final Path targetFile = tempDir.resolve("data.txt");
        Files.writeString(targetFile, "line 1\nline 2\nline 3\nline 4\nline 5\n");

        final ToolInvocation result = tool.execute(context, Map.of(
                "action", "slice",
                "path", "data.txt",
                "start_line", 2,
                "end_line", 4
        ));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("2: line 2").contains("3: line 3").contains("4: line 4")
                .doesNotContain("1: line 1").doesNotContain("5: line 5");
    }

    @Test
    @DisplayName("Should encode string to hex and decode hex back to string")
    void testHexEncodingAndDecoding() {
        final ToolInvocation encodeResult = tool.execute(context, Map.of(
                "action", "hex_encode",
                "input", "Omniwrench 2026"
        ));

        assertThat(encodeResult.isSuccess()).isTrue();
        final String hex = encodeResult.getOutput();
        assertThat(hex).isNotEmpty();

        final ToolInvocation decodeResult = tool.execute(context, Map.of(
                "action", "hex_decode",
                "input", hex
        ));

        assertThat(decodeResult.isSuccess()).isTrue();
        assertThat(decodeResult.getOutput()).isEqualTo("Omniwrench 2026");
    }

    @Test
    @DisplayName("Should execute tool asynchronously with reactive progress callbacks")
    void testAsyncToolExecution() throws Exception {
        final List<String> progressEvents = new ArrayList<>();

        final CompletableFuture<ToolInvocation> future = tool.executeAsync(
                context,
                Map.of("action", "hex_encode", "input", "AsyncPayload"),
                progressEvents::add
        );

        final ToolInvocation invocation = future.get(5, TimeUnit.SECONDS);
        assertThat(invocation).isNotNull();
        assertThat(invocation.isSuccess()).isTrue();
        assertThat(progressEvents).hasSize(2);
        assertThat(progressEvents.get(0)).contains("Starting");
        assertThat(progressEvents.get(1)).contains("Completed");
    }
}
