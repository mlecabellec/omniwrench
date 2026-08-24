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
 * Unit tests verifying the AgentEngine reasoning cycle, thinking demuxing, and prompt execution.
 *
 * Traceability:
 * - Requirement: REQ-00043 (Hybrid Reasoning Loop), REQ-00060 (Pluggable Tools), REQ-00088 (Dual Chat Mode Reasoning)
 * - Feature: FR-00014 (Hybrid Reasoning Loop)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00002 (Autonomous Goal Planning)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry), TSK-20260822-007 (Dual Chat Mode & Thinking Demux)
 * - ADR: ADR-0008 (Autonomous Reasoning Loop), ADR-0047 (Reasoning Demux)
 */
@Tag("REQ-00043")
@Tag("REQ-00060")
@Tag("REQ-00088")
@Tag("FR-00014")
@Tag("UC-00001")
@Tag("TSK-20260822-005")
@Tag("TSK-20260822-007")
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
    @DisplayName("Should process standard user prompt and demux thinking when enabled")
    void shouldProcessStandardPrompt() {
        final SessionContext session = SessionContext.createDefault(tempDir.toString());
        final AgentMessage response = agentEngine.processPrompt(session, "Hello Omniwrench");

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("assistant");
        assertThat(response.getContent()).contains("Omniwrench Agent acknowledged");
        assertThat(response.hasThinking()).isTrue();
        assertThat(response.getThinking()).contains("Analyzing user request");
        assertThat(session.getMessages()).hasSize(2);
    }

    @Test
    @DisplayName("Should configure thinking mode and effort level via /thinking commands")
    void shouldConfigureThinkingMode() {
        final SessionContext session = SessionContext.createDefault(tempDir.toString());

        final AgentMessage statusMsg = agentEngine.processPrompt(session, "/thinking status");
        assertThat(statusMsg.getContent()).contains("Thinking mode is currently ENABLED");

        final AgentMessage offMsg = agentEngine.processPrompt(session, "/thinking off");
        assertThat(offMsg.getContent()).contains("DISABLED");
        assertThat(agentEngine.isThinkingEnabled()).isFalse();

        final AgentMessage promptWithoutThinking = agentEngine.processPrompt(session, "Prompt without thinking");
        assertThat(promptWithoutThinking.hasThinking()).isFalse();

        final AgentMessage highMsg = agentEngine.processPrompt(session, "/thinking high");
        assertThat(highMsg.getContent()).contains("high");
        assertThat(agentEngine.getThinkingEffort()).isEqualTo("high");
        assertThat(agentEngine.isThinkingEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should reject null arguments during prompt processing")
    void shouldRejectNullArguments() {
        final SessionContext session = SessionContext.createDefault(tempDir.toString());
        assertThrows(NullPointerException.class, () -> agentEngine.processPrompt(null, "prompt"));
        assertThrows(NullPointerException.class, () -> agentEngine.processPrompt(session, null));
    }
}
