package com.omniwrench.tools;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import com.omniwrench.tools.FileOperationsTool;
import org.junit.jupiter.api.BeforeEach;
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
 * - Requirement: REQ-00030 (Autonomous Agent Reasoning Loop)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 */
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
    void shouldProcessStandardPrompt() {
        final SessionContext session = SessionContext.createDefault(tempDir.toString());
        final AgentMessage response = agentEngine.processPrompt(session, "Hello Omniwrench");

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("assistant");
        assertThat(response.getContent()).contains("Omniwrench Agent acknowledged");
        assertThat(session.getMessages()).hasSize(2);
    }

    @Test
    void shouldRejectNullArguments() {
        final SessionContext session = SessionContext.createDefault(tempDir.toString());
        assertThrows(NullPointerException.class, () -> agentEngine.processPrompt(null, "prompt"));
        assertThrows(NullPointerException.class, () -> agentEngine.processPrompt(session, null));
    }
}
