package com.omniwrench.tools;

import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying FileOperationsTool read, write, exists, and list actions.
 *
 * Traceability:
 * - Requirement: REQ-00060 (Polyvalent Base Architecture with Pluggable Tools)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00005 (AST Static Analysis & Comment-Safe Edits)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 * - ADR: ADR-0006 (Polyvalent Tool Architecture)
 */
@Tag("REQ-00060")
@Tag("FR-00020")
@Tag("UC-00001")
@Tag("TSK-20260822-005")
class FileOperationsToolTest {

    @TempDir
    Path tempDir;

    private FileOperationsTool fileTool;
    private SessionContext context;

    @BeforeEach
    void setUp() {
        fileTool = new FileOperationsTool();
        context = SessionContext.createDefault(tempDir.toString());
    }

    @Test
    @DisplayName("Should expose file_ops definition and schema")
    void shouldExposeDefinition() {
        final ToolDefinition definition = fileTool.getDefinition();
        assertThat(definition.getName()).isEqualTo("file_ops");
        assertThat(definition.getParameterSchema()).containsKey("action").containsKey("path");
    }

    @Test
    @DisplayName("Should perform write, read, and exists file operations")
    void shouldPerformWriteReadAndExists() throws IOException {
        final Map<String, Object> writeArgs = Map.of(
                "action", "write",
                "path", "sub/test.txt",
                "content", "Hello File Operations"
        );
        final ToolInvocation writeResult = fileTool.execute(context, writeArgs);
        assertThat(writeResult.isSuccess()).isTrue();
        assertThat(Files.exists(tempDir.resolve("sub/test.txt"))).isTrue();

        final Map<String, Object> existsArgs = Map.of("action", "exists", "path", "sub/test.txt");
        final ToolInvocation existsResult = fileTool.execute(context, existsArgs);
        assertThat(existsResult.isSuccess()).isTrue();
        assertThat(existsResult.getOutput()).isEqualTo("Exists: true");

        final Map<String, Object> readArgs = Map.of("action", "read", "path", "sub/test.txt");
        final ToolInvocation readResult = fileTool.execute(context, readArgs);
        assertThat(readResult.isSuccess()).isTrue();
        assertThat(readResult.getOutput()).isEqualTo("Hello File Operations");
    }

    @Test
    @DisplayName("Should list directory contents")
    void shouldListDirectory() throws IOException {
        Files.createFile(tempDir.resolve("alpha.txt"));
        Files.createFile(tempDir.resolve("beta.txt"));

        final Map<String, Object> listArgs = Map.of("action", "list", "path", ".");
        final ToolInvocation listResult = fileTool.execute(context, listArgs);
        assertThat(listResult.isSuccess()).isTrue();
        assertThat(listResult.getOutput()).contains("alpha.txt").contains("beta.txt");
    }

    @Test
    @DisplayName("Should handle missing file, missing directory, and unknown actions gracefully")
    void shouldHandleErrorsGracefully() {
        final ToolInvocation readMissing = fileTool.execute(context, Map.of("action", "read", "path", "missing.txt"));
        assertThat(readMissing.isSuccess()).isFalse();
        assertThat(readMissing.getOutput()).contains("Error: File not found");

        final ToolInvocation listMissing = fileTool.execute(context, Map.of("action", "list", "path", "missing_dir"));
        assertThat(listMissing.isSuccess()).isFalse();
        assertThat(listMissing.getOutput()).contains("Error: Directory not found");

        final ToolInvocation unknownAction = fileTool.execute(context, Map.of("action", "invalid_action"));
        assertThat(unknownAction.isSuccess()).isFalse();
        assertThat(unknownAction.getOutput()).contains("Unknown action: invalid_action");

        assertThrows(NullPointerException.class, () -> fileTool.execute(null, Map.of()));
        assertThrows(NullPointerException.class, () -> fileTool.execute(context, null));
    }
}
