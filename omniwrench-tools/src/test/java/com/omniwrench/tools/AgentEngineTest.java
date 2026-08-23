package com.omniwrench.tools;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying the AgentEngine reasoning cycle and prompt execution.
 *
 * Traceability:
 * - Requirement: REQ-00043 (Hybrid Reasoning Loop), REQ-00060 (Polyvalent Base Architecture with Pluggable Tools)
 * - Feature: FR-00014 (Hybrid Reasoning Loop)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00002 (Autonomous Goal Planning)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 * - ADR: ADR-0008 (Autonomous Reasoning Loop)
 */
@Tag("REQ-00043")
@Tag("REQ-00060")
@Tag("FR-00014")
@Tag("UC-00001")
@Tag("TSK-20260822-005")
class AgentEngineTest {

    @TempDir
    Path tempDir;

    private AgentEngine agentEngine;
    private ToolRegistry toolRegistry;
    private OmniwrenchProperties properties;

    @BeforeEach
    void setUp() {
        final FileOperationsTool fileOps = new FileOperationsTool();
        toolRegistry = new ToolRegistry(List.of(fileOps));
        properties = new OmniwrenchProperties();
        properties.setWorkspacePath(tempDir.toString());
        agentEngine = new AgentEngine(toolRegistry, properties);
    }

    @Test
    @DisplayName("Should process standard user prompt and generate assistant reply")
    void shouldProcessStandardPrompt() {
        final SessionContext session = SessionContext.createDefault(tempDir.toString());
        final AgentMessage response = agentEngine.processPrompt(session, "Hello Omniwrench");

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("assistant");
        assertThat(response.getContent()).contains("Omniwrench Agent acknowledged");
        assertThat(session.getMessages()).hasSize(2);
    }

    @Test
    @DisplayName("Should reject null arguments during prompt processing")
    void shouldRejectNullArguments() {
        final SessionContext session = SessionContext.createDefault(tempDir.toString());
        assertThrows(NullPointerException.class, () -> agentEngine.processPrompt(null, "prompt"));
        assertThrows(NullPointerException.class, () -> agentEngine.processPrompt(session, null));
    }
}
