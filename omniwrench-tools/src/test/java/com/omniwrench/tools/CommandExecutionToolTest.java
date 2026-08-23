package com.omniwrench.tools;

import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying CommandExecutionTool bounded process execution and exit code assertions.
 *
 * Traceability:
 * - Requirement: REQ-00060 (Polyvalent Base Architecture with Pluggable Tools), REQ-00065 (Multi-Tier Security Guardrails)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI), FR-00025 (Multi-Tier Security Guardrails)
 * - Use Case: UC-00002 (Autonomous Goal Planning & Refactoring)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 * - ADR: ADR-0006 (Pluggable Tools Architecture), ADR-0020 (Command Safety Classification)
 */
@Tag("REQ-00060")
@Tag("REQ-00065")
@Tag("FR-00020")
@Tag("FR-00025")
@Tag("UC-00002")
@Tag("TSK-20260822-005")
class CommandExecutionToolTest {

    @TempDir
    Path tempDir;

    private CommandExecutionTool commandTool;
    private SessionContext context;

    @BeforeEach
    void setUp() {
        commandTool = new CommandExecutionTool();
        context = SessionContext.createDefault(tempDir.toString());
    }

    @Test
    @DisplayName("Should expose run_command definition and parameter schema")
    void shouldExposeDefinition() {
        final ToolDefinition definition = commandTool.getDefinition();
        assertThat(definition).isNotNull();
        assertThat(definition.getName()).isEqualTo("run_command");
        assertThat(definition.getParameterSchema()).containsKey("command");
    }

    @Test
    @DisplayName("Should execute basic shell command and return stdout")
    void shouldExecuteStandardCommand() {
        final Map<String, Object> args = Map.of("command", "echo 'Omniwrench Test'");
        final ToolInvocation result = commandTool.execute(context, args);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getToolName()).isEqualTo("run_command");
        assertThat(result.getOutput()).contains("Exit Code: 0").contains("Omniwrench Test");
    }

    @Test
    @DisplayName("Should capture non-zero exit codes as failure")
    void shouldCaptureCommandFailure() {
        final Map<String, Object> args = Map.of("command", "exit 1");
        final ToolInvocation result = commandTool.execute(context, args);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getOutput()).contains("Exit Code: 1");
    }

    @Test
    @DisplayName("Should reject empty commands and null arguments")
    void shouldHandleEmptyOrNullInput() {
        final ToolInvocation emptyResult = commandTool.execute(context, Map.of("command", "   "));
        assertThat(emptyResult.isSuccess()).isFalse();
        assertThat(emptyResult.getOutput()).contains("Error: Empty command specified");

        assertThrows(NullPointerException.class, () -> commandTool.execute(null, Map.of("command", "ls")));
        assertThrows(NullPointerException.class, () -> commandTool.execute(context, null));
    }
}
