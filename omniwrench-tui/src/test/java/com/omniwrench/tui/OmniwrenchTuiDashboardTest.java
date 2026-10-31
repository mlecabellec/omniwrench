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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying OmniwrenchTuiDashboard interactive command loop and OpenCode commands.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine), REQ-00043 (Hybrid Reasoning Loop)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design &amp; Integration)
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
    private TerminalEnvironmentDetector detector;
    private OmniwrenchTuiDashboard dashboard;

    @BeforeEach
    void setUp() {
        originalSystemIn = System.in;
        renderer = new TerminalRenderer();
        agentEngine = Mockito.mock(AgentEngine.class);
        properties = new OmniwrenchProperties();
        sessionManager = new SessionManager(properties);
        toolRegistry = new ToolRegistry(List.of());
        detector = new TerminalEnvironmentDetector();

        dashboard = new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, toolRegistry, properties, detector);
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
    @DisplayName("Should process /goal, /env, /btw, /copy and /clear commands")
    void shouldProcessOpenCodeCommands() {
        final String simulatedInput = "/env\n/goal Build robust microservice\n/goal\n/clear\n"
                + "/btw How do I run tests?\n"
                + "Regular prompt\n"
                + "/copy\n"
                + "exit\n";
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8)));

        when(agentEngine.processPrompt(any(SessionContext.class), contains("[SIDE-CHANNEL QUESTION]")))
                .thenReturn(AgentMessage.of("assistant", "Run mvn test."));

        when(agentEngine.processPrompt(any(SessionContext.class), eq("Regular prompt")))
                .thenReturn(AgentMessage.of("assistant", "Regular response."));

        dashboard.startInteractiveLoop();

        verify(agentEngine).processPrompt(any(SessionContext.class), contains("How do I run tests?"));
        verify(agentEngine).processPrompt(any(SessionContext.class), eq("Regular prompt"));
    }

    @Test
    @DisplayName("Should reject null constructor dependencies")
    void shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(null, agentEngine, sessionManager, toolRegistry, properties, detector));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, null, sessionManager, toolRegistry, properties, detector));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, null, toolRegistry, properties, detector));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, null, properties, detector));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, toolRegistry, null, detector));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, toolRegistry, properties, null));
    }
}
