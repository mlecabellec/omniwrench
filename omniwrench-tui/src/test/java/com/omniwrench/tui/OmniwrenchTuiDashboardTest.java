package com.omniwrench.tui;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying OmniwrenchTuiDashboard interactive command loop and lifecycle.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI & Interactive TUI Presentation Engine), REQ-00043 (Hybrid Reasoning Loop)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design & Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
@Tag("REQ-00001")
@Tag("FR-00001")
@Tag("UC-00001")
@Tag("TSK-20260822-003")
class OmniwrenchTuiDashboardTest {

    private InputStream originalSystemIn;
    private TerminalRenderer renderer;
    private AgentEngine agentEngine;
    private SessionManager sessionManager;
    private ToolRegistry toolRegistry;
    private OmniwrenchProperties properties;
    private OmniwrenchTuiDashboard dashboard;

    @BeforeEach
    void setUp() {
        originalSystemIn = System.in;
        renderer = new TerminalRenderer();
        agentEngine = Mockito.mock(AgentEngine.class);
        properties = new OmniwrenchProperties();
        sessionManager = new SessionManager(properties);
        toolRegistry = new ToolRegistry(List.of());

        dashboard = new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, toolRegistry, properties);
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalSystemIn);
    }

    @Test
    @DisplayName("Should process user input in interactive loop and exit cleanly on exit command")
    void shouldProcessInputAndExit() {
        final String simulatedInput = "/help\nHello Agent\nexit\n";
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8)));

        when(agentEngine.processPrompt(any(SessionContext.class), eq("Hello Agent")))
                .thenReturn(AgentMessage.of("assistant", "Greetings, Operator"));

        dashboard.startInteractiveLoop();

        verify(agentEngine).processPrompt(any(SessionContext.class), eq("Hello Agent"));
    }

    @Test
    @DisplayName("Should reject null constructor dependencies")
    void shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(null, agentEngine, sessionManager, toolRegistry, properties));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, null, sessionManager, toolRegistry, properties));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, null, toolRegistry, properties));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, null, properties));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, toolRegistry, null));
    }
}
