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
 * Unit tests verifying {@link OmniwrenchTuiDashboard} interactive command loop orchestration
 * and OpenCode-inspired slash commands (`/goal`, `/env`, `/btw`, `/copy`, `/clear`).
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00043 (Hybrid Reasoning Loop), REQ-00100 (Dual-Mode Minimal &amp; Fullscreen TUI)</li>
 *   <li>Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine), FR-00047</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20260822-003, TSK-20261101-016, TSK-20261101-018</li>
 *   <li>Rules: CS-0030.7, CS-0050.1 (non-null preconditions), CS-0050.6 (bounded loop)</li>
 *   <li>ADR: ADR-0001 (Unified Dual Architecture), ADR-0063 (Dual-Mode TUI)</li>
 * </ul>
 */
@Tag("REQ-00001")
@Tag("REQ-00100")
@Tag("FR-00001")
@Tag("FR-00047")
@Tag("UC-00001")
@Tag("TSK-20260822-003")
@Tag("TSK-20261101-018")
class OmniwrenchTuiDashboardTest {

    /** Saved standard input stream restored after each test. */
    private InputStream originalSystemIn;
    /** Terminal widget renderer fixture. */
    private TerminalRenderer renderer;
    /** Agent reasoning engine collaborator. */
    private AgentEngine agentEngine;
    /** Session lifecycle manager fixture. */
    private SessionManager sessionManager;
    /** Tool registry fixture. */
    private ToolRegistry toolRegistry;
    /** Configuration properties fixture. */
    private OmniwrenchProperties properties;
    /** Terminal environment detector fixture. */
    private TerminalEnvironmentDetector detector;
    /** Dashboard orchestrator under test. */
    private OmniwrenchTuiDashboard dashboard;

    /**
     * Initializes clean test fixtures before each test method.
     */
    @BeforeEach
    void setUp() {
        // [CS-0030.7] Preserve System.in and construct OmniwrenchTuiDashboard with real collaborators
        originalSystemIn = System.in;
        renderer = new TerminalRenderer();
        agentEngine = Mockito.mock(AgentEngine.class);
        properties = new OmniwrenchProperties();
        sessionManager = new SessionManager(properties);
        toolRegistry = new ToolRegistry(List.of());
        detector = new TerminalEnvironmentDetector();

        dashboard = new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, toolRegistry, properties, detector);
    }

    /**
     * Restores standard input after each test method.
     */
    @AfterEach
    void tearDown() {
        // [CS-0030.7] Restore original System.in stream
        System.setIn(originalSystemIn);
    }

    /**
     * Verifies that {@link OmniwrenchTuiDashboard#startInteractiveLoop()} processes user prompts
     * and exits cleanly upon receiving {@code exit}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link OmniwrenchTuiDashboard#startInteractiveLoop()},
     *       {@link MinimalTuiModeEngine#start(SessionContext, TerminalEnvironment)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00047}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.6} (bounded REPL termination)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should process user input in interactive loop and exit cleanly on exit command")
    void shouldProcessInputAndExit() {
        // [REQ-00001, REQ-00100, FR-00047] Simulate /help, a user prompt, and exit command
        final String simulatedInput = "/help\nHello Agent\nexit\n";
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8)));

        when(agentEngine.processPrompt(any(SessionContext.class), eq("Hello Agent")))
                .thenReturn(AgentMessage.of("assistant", "Greetings, Operator"));

        // [REQ-00100, FR-00047] Execute interactive loop and verify AgentEngine invocation
        dashboard.startInteractiveLoop();

        verify(agentEngine).processPrompt(any(SessionContext.class), eq("Hello Agent"));
    }

    /**
     * Verifies that OpenCode-inspired slash commands (`/env`, `/goal`, `/clear`, `/btw`, `/copy`)
     * are routed and executed accurately within the interactive loop.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link OmniwrenchTuiDashboard#startInteractiveLoop()},
     *       {@link MinimalTuiModeEngine#start(SessionContext, TerminalEnvironment)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00047}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3} (deterministic command dispatch)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should process /goal, /env, /btw, /copy and /clear commands")
    void shouldProcessOpenCodeCommands() {
        // [REQ-00001, REQ-00100, FR-00047] Simulate OpenCode slash commands and regular prompt
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

        // [REQ-00100, FR-00047] Run interactive loop and verify both side-channel and regular prompts
        dashboard.startInteractiveLoop();

        verify(agentEngine).processPrompt(any(SessionContext.class), contains("How do I run tests?"));
        verify(agentEngine).processPrompt(any(SessionContext.class), eq("Regular prompt"));
    }

    /**
     * Verifies that {@link OmniwrenchTuiDashboard} rejects {@code null} constructor dependencies.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link OmniwrenchTuiDashboard#OmniwrenchTuiDashboard(TerminalRenderer, AgentEngine, SessionManager, ToolRegistry, OmniwrenchProperties, TerminalEnvironmentDetector)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.1} (strict non-null parameter preconditions)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should reject null constructor dependencies")
    void shouldRejectNullDependencies() {
        // [CS-0050.1] Verify each null constructor argument throws NullPointerException
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(null, agentEngine, sessionManager, toolRegistry, properties, detector));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, null, sessionManager, toolRegistry, properties, detector));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, null, toolRegistry, properties, detector));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, null, properties, detector));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, toolRegistry, null, detector));
        assertThrows(NullPointerException.class, () -> new OmniwrenchTuiDashboard(renderer, agentEngine, sessionManager, toolRegistry, properties, null));
    }
}
