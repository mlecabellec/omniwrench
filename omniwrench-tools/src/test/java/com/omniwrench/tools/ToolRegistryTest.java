package com.omniwrench.tools;

import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying ToolRegistry lifecycle, lookup semantics, and dynamic registration.
 *
 * Traceability:
 * - Requirement: REQ-00060 (Polyvalent Base Architecture with Pluggable Tools)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI)
 * - Use Case: UC-00009 (MCP External Server Tool Invocation)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 * - ADR: ADR-0006 (Polyvalent Tool Architecture)
 */
@Tag("REQ-00060")
@Tag("FR-00020")
@Tag("UC-00009")
@Tag("TSK-20260822-005")
class ToolRegistryTest {

    private ToolRegistry toolRegistry;
    private FileOperationsTool fileOpsTool;

    @BeforeEach
    void setUp() {
        fileOpsTool = new FileOperationsTool();
        final List<Tool> tools = new ArrayList<>();
        tools.add(fileOpsTool);
        toolRegistry = new ToolRegistry(tools);
    }

    @Test
    @DisplayName("Should find registered tool by unique name")
    void shouldFindRegisteredTool() {
        final Optional<Tool> tool = toolRegistry.getTool("file_ops");
        assertThat(tool).isPresent();
        assertThat(tool.get().getDefinition().getName()).isEqualTo("file_ops");
        assertThat(toolRegistry.getAllDefinitions()).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty Optional for unknown tool name")
    void shouldReturnEmptyForUnknownTool() {
        final Optional<Tool> tool = toolRegistry.getTool("unknown_tool");
        assertThat(tool).isEmpty();
    }

    @Test
    @DisplayName("Should register dynamic custom tool at runtime")
    void shouldRegisterDynamicTool() {
        final Tool customTool = new Tool() {
            @Override
            public ToolDefinition getDefinition() {
                return new ToolDefinition("custom_echo", "Echoes input", Map.of("text", "text to echo"));
            }

            @Override
            public ToolInvocation execute(final SessionContext context, final Map<String, Object> arguments) {
                return new ToolInvocation("id-1", "custom_echo", arguments, "echoed", true, Instant.now());
            }
        };

        toolRegistry.registerTool(customTool);
        assertThat(toolRegistry.getTool("custom_echo")).isPresent();
        assertThat(toolRegistry.getToolCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should reject null tool registration or null lookup parameter")
    void shouldRejectNullRegistration() {
        assertThrows(NullPointerException.class, () -> toolRegistry.registerTool(null));
        assertThrows(NullPointerException.class, () -> toolRegistry.getTool(null));
    }
}
