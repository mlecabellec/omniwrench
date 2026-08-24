package com.omniwrench.tui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying TuiRunner argument inspection, prompt execution, and mode transitions.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI), REQ-00002 (Configurable Profiles)
 * - Requirement: REQ-00089 (Unified Tri-Interface Prompting CLI, TUI, Web)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00004 (Headless CI/CD Automation)
 * - Task: TSK-20260822-003 (TUI Design), TSK-20260822-008 (Unified Tri-Interface Prompting)
 * - ADR: ADR-0001 (Unified Dual Architecture), ADR-0048 (Unified Tri-Interface Ingestion)
 */
@Tag("REQ-00001")
@Tag("REQ-00002")
@Tag("REQ-00089")
@Tag("FR-00001")
@Tag("UC-00001")
@Tag("TSK-20260822-003")
@Tag("TSK-20260822-008")
class TuiRunnerTest {

    private OmniwrenchProperties properties;
    private OmniwrenchTuiDashboard dashboard;
    private AgentEngine agentEngine;
    private SessionManager sessionManager;
    private ObjectMapper objectMapper;
    private TuiRunner tuiRunner;

    @BeforeEach
    void setUp() {
        properties = new OmniwrenchProperties();
        dashboard = Mockito.mock(OmniwrenchTuiDashboard.class);
        final ToolRegistry toolRegistry = new ToolRegistry(List.of());
        sessionManager = new SessionManager(properties);
        agentEngine = Mockito.mock(AgentEngine.class);
        objectMapper = new ObjectMapper();
        tuiRunner = new TuiRunner(properties, dashboard, agentEngine, sessionManager, objectMapper);
    }

    @Test
    @DisplayName("Should launch interactive loop when CLI argument is passed")
    void shouldLaunchWhenCliArgPassed() throws Exception {
        tuiRunner.run("cli");
        verify(dashboard).startInteractiveLoop();
    }

    @Test
    @DisplayName("Should launch interactive loop when TUI argument is passed")
    void shouldLaunchWhenTuiArgPassed() throws Exception {
        tuiRunner.run("tui");
        verify(dashboard).startInteractiveLoop();
    }

    @Test
    @DisplayName("Should execute direct prompt when -p is passed")
    void shouldExecuteDirectPrompt() throws Exception {
        final SessionContext session = sessionManager.getDefaultSession();
        when(agentEngine.processPrompt(eq(session), eq("Hello Omniwrench")))
                .thenReturn(AgentMessage.of("assistant", "Direct reply"));

        tuiRunner.run("-p", "Hello Omniwrench");

        verify(agentEngine).processPrompt(eq(session), eq("Hello Omniwrench"));
        verify(dashboard, never()).startInteractiveLoop();
    }

    @Test
    @DisplayName("Should execute direct prompt with --json format flag")
    void shouldExecutePromptWithJsonFlag() throws Exception {
        final SessionContext session = sessionManager.getDefaultSession();
        when(agentEngine.processPrompt(eq(session), eq("Analyze code")))
                .thenReturn(AgentMessage.of("assistant", "JSON formatted output"));

        tuiRunner.run("--prompt", "Analyze code", "--json");

        verify(agentEngine).processPrompt(eq(session), eq("Analyze code"));
        verify(dashboard, never()).startInteractiveLoop();
    }

    @Test
    @DisplayName("Should display version info when --version is passed")
    void shouldDisplayVersion() throws Exception {
        tuiRunner.run("--version");
        verify(dashboard, never()).startInteractiveLoop();
        verify(agentEngine, never()).processPrompt(any(), any());
    }

    @Test
    @DisplayName("Should display help when --help is passed")
    void shouldDisplayHelp() throws Exception {
        tuiRunner.run("--help");
        verify(dashboard, never()).startInteractiveLoop();
        verify(agentEngine, never()).processPrompt(any(), any());
    }

    @Test
    @DisplayName("Should launch interactive loop when mode property is configured as tui")
    void shouldLaunchWhenModePropertyIsTui() throws Exception {
        properties.setMode("tui");
        tuiRunner.run();
        verify(dashboard).startInteractiveLoop();
    }

    @Test
    @DisplayName("Should remain on server standby when mode is dual and no CLI arguments are supplied")
    void shouldRemainStandbyInDualMode() throws Exception {
        properties.setMode("dual");
        tuiRunner.run();
        verify(dashboard, never()).startInteractiveLoop();
    }

    @Test
    @DisplayName("Should reject null constructor arguments")
    void shouldRejectNullConstructorArgs() {
        assertThrows(NullPointerException.class, () -> new TuiRunner(null, dashboard, agentEngine, sessionManager, objectMapper));
        assertThrows(NullPointerException.class, () -> new TuiRunner(properties, null, agentEngine, sessionManager, objectMapper));
        assertThrows(NullPointerException.class, () -> new TuiRunner(properties, dashboard, null, sessionManager, objectMapper));
        assertThrows(NullPointerException.class, () -> new TuiRunner(properties, dashboard, agentEngine, null, objectMapper));
        assertThrows(NullPointerException.class, () -> new TuiRunner(properties, dashboard, agentEngine, sessionManager, null));
    }
}
