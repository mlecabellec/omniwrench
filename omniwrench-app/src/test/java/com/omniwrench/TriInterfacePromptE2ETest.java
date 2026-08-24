package com.omniwrench;

import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import com.omniwrench.tui.TuiRunner;
import com.omniwrench.web.AgentController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test verifying Unified Tri-Interface Prompting (CLI, TUI, Web) parity and session sync.
 *
 * Traceability:
 * - Requirement: REQ-00089 (Unified Tri-Interface Prompting CLI, TUI, and Web UI)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine), FR-00015 (RESTful Agent Dialogue)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00003 (Web UI Collaboration), UC-00004 (Headless CI/CD)
 * - Task: TSK-20260822-008 (Unified Tri-Interface Prompting &amp; E2E Test Suite)
 * - ADR: ADR-0048 (Unified Tri-Interface Prompt Ingestion)
 */
@SpringBootTest
@Tag("REQ-00089")
@Tag("FR-00001")
@Tag("UC-00001")
@Tag("UC-00003")
@Tag("UC-00004")
@Tag("TSK-20260822-008")
class TriInterfacePromptE2ETest {

    @Autowired
    private AgentEngine agentEngine;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private TuiRunner tuiRunner;

    @Autowired
    private AgentController agentController;

    @Test
    @DisplayName("Should execute prompts across CLI, TUI engine, and Web REST with unified session history")
    void testTriInterfaceParity() throws Exception {
        final SessionContext session = sessionManager.getDefaultSession();

        // 1. CLI Execution
        tuiRunner.run("-p", "CLI prompt execution test", "--json");

        // 2. TUI / Direct Engine Execution
        final AgentMessage tuiResponse = agentEngine.processPrompt(session, "TUI direct engine prompt test");
        assertThat(tuiResponse).isNotNull();
        assertThat(tuiResponse.getRole()).isEqualTo("assistant");
        assertThat(tuiResponse.getContent()).contains("Omniwrench Agent acknowledged");

        // 3. Web REST Execution
        final ResponseEntity<AgentMessage> webResponse = agentController.sendPrompt(
                session.getSessionId(),
                Map.of("prompt", "Web REST prompt execution test")
        );
        assertThat(webResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(webResponse.getBody()).isNotNull();
        assertThat(webResponse.getBody().getContent()).contains("Omniwrench Agent acknowledged");

        // Verify unified conversation history accumulation
        final List<AgentMessage> history = session.getMessages();
        assertThat(history).hasSize(6); // 3 user prompts + 3 assistant responses
    }
}
