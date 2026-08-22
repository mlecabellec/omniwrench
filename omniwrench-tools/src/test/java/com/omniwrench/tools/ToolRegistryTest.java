package com.omniwrench.tools;

import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;
import com.omniwrench.tools.FileOperationsTool;
import com.omniwrench.tools.Tool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying ToolRegistry lifecycle and lookup semantics.
 * 
 * Traceability:
 * - Requirement: REQ-00020 (Pluggable Tool SPI & Registry)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 */
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
    void shouldFindRegisteredTool() {
        final Optional<Tool> tool = toolRegistry.getTool("file_ops");
        assertThat(tool).isPresent();
        assertThat(tool.get().getDefinition().getName()).isEqualTo("file_ops");
    }

    @Test
    void shouldReturnEmptyForUnknownTool() {
        final Optional<Tool> tool = toolRegistry.getTool("unknown_tool");
        assertThat(tool).isEmpty();
    }

    @Test
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
    void shouldRejectNullRegistration() {
        assertThrows(NullPointerException.class, () -> toolRegistry.registerTool(null));
        assertThrows(NullPointerException.class, () -> toolRegistry.getTool(null));
    }
}
