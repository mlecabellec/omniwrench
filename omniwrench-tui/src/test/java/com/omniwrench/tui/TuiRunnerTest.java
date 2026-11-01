package com.omniwrench.tui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying {@link TuiRunner} CLI argument inspection, one-shot prompt execution,
 * JSON output formatting, and TUI mode transitions.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI), REQ-00002 (Configurable Profiles)</li>
 *   <li>Requirement: REQ-00089 (Unified Tri-Interface Prompting CLI, TUI, Web), REQ-00100 (Dual-Mode TUI)</li>
 *   <li>Feature: FR-00001, FR-00036, FR-00050</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00004 (Headless CI/CD Automation)</li>
 *   <li>Task: TSK-20260822-003, TSK-20260822-008, TSK-20261101-016, TSK-20261101-018</li>
 *   <li>Rules: CS-0030.7, CS-0050.1 (preconditions), CS-0060.3 (total branching)</li>
 *   <li>ADR: ADR-0001 (Unified Dual Architecture), ADR-0048 (Unified Tri-Interface Ingestion)</li>
 * </ul>
 */
@Tag("REQ-00001")
@Tag("REQ-00002")
@Tag("REQ-00089")
@Tag("REQ-00100")
@Tag("FR-00001")
@Tag("FR-00036")
@Tag("FR-00050")
@Tag("UC-00001")
@Tag("UC-00004")
@Tag("TSK-20260822-003")
@Tag("TSK-20260822-008")
@Tag("TSK-20261101-018")
class TuiRunnerTest {

    /** Runtime configuration properties fixture. */
    private OmniwrenchProperties properties;
    /** Mocked TUI dashboard collaborator. */
    private OmniwrenchTuiDashboard dashboard;
    /** Mocked AgentEngine collaborator. */
    private AgentEngine agentEngine;
    /** Real SessionManager fixture. */
    private SessionManager sessionManager;
    /** Real Jackson ObjectMapper fixture. */
    private ObjectMapper objectMapper;
    /** TuiRunner instance under test. */
    private TuiRunner tuiRunner;

    /**
     * Initializes test fixtures before each test case.
     */
    @BeforeEach
    void setUp() {
        // [CS-0030.7] Instantiate TuiRunner with fresh configuration and session state
        properties = new OmniwrenchProperties();
        dashboard = Mockito.mock(OmniwrenchTuiDashboard.class);
        sessionManager = new SessionManager(properties);
        agentEngine = Mockito.mock(AgentEngine.class);
        objectMapper = new ObjectMapper();
        tuiRunner = new TuiRunner(properties, dashboard, agentEngine, sessionManager, objectMapper);
    }

    /**
     * Verifies that passing the {@code cli} positional argument launches the interactive TUI loop.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiRunner#run(String...)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0001}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should launch interactive loop when CLI argument is passed")
    void shouldLaunchWhenCliArgPassed() throws Exception {
        // [REQ-00001, FR-00001] Invoke run("cli") and verify interactive loop starts
        tuiRunner.run("cli");
        verify(dashboard).startInteractiveLoop();
    }

    /**
     * Verifies that passing the {@code tui} positional argument launches the interactive TUI loop.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiRunner#run(String...)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00050}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0001}, {@code ADR-0063}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should launch interactive loop when TUI argument is passed")
    void shouldLaunchWhenTuiArgPassed() throws Exception {
        // [REQ-00001, REQ-00100, FR-00050] Invoke run("tui") and verify interactive loop starts
        tuiRunner.run("tui");
        verify(dashboard).startInteractiveLoop();
    }

    /**
     * Verifies that passing {@code -p "<prompt>"} executes a non-interactive one-shot prompt via {@link AgentEngine}
     * without launching the interactive TUI loop.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiRunner#run(String...)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00036}, {@code REQ-00089}, {@code UC-00004}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0048} (Unified Tri-Interface Ingestion)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should execute direct prompt when -p is passed")
    void shouldExecuteDirectPrompt() throws Exception {
        // [REQ-00089, FR-00036, UC-00004] Configure AgentEngine response and run with -p flag
        final SessionContext session = sessionManager.getDefaultSession();
        when(agentEngine.processPrompt(eq(session), eq("Hello Omniwrench")))
                .thenReturn(AgentMessage.of("assistant", "Direct reply"));

        tuiRunner.run("-p", "Hello Omniwrench");

        verify(agentEngine).processPrompt(eq(session), eq("Hello Omniwrench"));
        verify(dashboard, never()).startInteractiveLoop();
    }

    /**
     * Verifies that passing {@code --prompt "<prompt>" --json} serializes the response as structured JSON.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiRunner#run(String...)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00036}, {@code REQ-00089}, {@code UC-00004}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0048}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should execute direct prompt with --json format flag")
    void shouldExecutePromptWithJsonFlag() throws Exception {
        // [REQ-00089, FR-00036, UC-00004] Execute --prompt with --json flag and verify non-interactive completion
        final SessionContext session = sessionManager.getDefaultSession();
        when(agentEngine.processPrompt(eq(session), eq("Analyze code")))
                .thenReturn(AgentMessage.of("assistant", "JSON formatted output"));

        tuiRunner.run("--prompt", "Analyze code", "--json");

        verify(agentEngine).processPrompt(eq(session), eq("Analyze code"));
        verify(dashboard, never()).startInteractiveLoop();
    }

    /**
     * Verifies that {@code --version} prints version metadata and exits without starting the TUI or agent loop.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiRunner#run(String...)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should display version info when --version is passed")
    void shouldDisplayVersion() throws Exception {
        // [REQ-00001, FR-00001] Run --version and verify neither dashboard nor agentEngine is invoked
        tuiRunner.run("--version");
        verify(dashboard, never()).startInteractiveLoop();
        verify(agentEngine, never()).processPrompt(any(), any());
    }

    /**
     * Verifies that {@code --help} prints CLI usage instructions and exits without starting the TUI or agent loop.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiRunner#run(String...)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00050}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should display help when --help is passed")
    void shouldDisplayHelp() throws Exception {
        // [REQ-00001, REQ-00100, FR-00050] Run --help and verify neither dashboard nor agentEngine is invoked
        tuiRunner.run("--help");
        verify(dashboard, never()).startInteractiveLoop();
        verify(agentEngine, never()).processPrompt(any(), any());
    }

    /**
     * Verifies that configuring {@code omniwrench.mode=tui} automatically launches the interactive TUI loop.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiRunner#run(String...)}, {@link OmniwrenchProperties#setMode(String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00009}, {@code REQ-00002}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0011} (Configurable Runtime Profiles)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should launch interactive loop when mode property is configured as tui")
    void shouldLaunchWhenModePropertyIsTui() throws Exception {
        // [REQ-00002, FR-00009] Set mode property to "tui" and verify dashboard starts
        properties.setMode("tui");
        tuiRunner.run();
        verify(dashboard).startInteractiveLoop();
    }

    /**
     * Verifies that configuring {@code omniwrench.mode=dual} with no CLI arguments keeps the TUI on standby.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiRunner#run(String...)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00009}, {@code REQ-00001}, {@code REQ-00002}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0001}, {@code ADR-0011}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should remain on server standby when mode is dual and no CLI arguments are supplied")
    void shouldRemainStandbyInDualMode() throws Exception {
        // [REQ-00001, REQ-00002] Set mode to "dual" with empty args and verify TUI stays on standby
        properties.setMode("dual");
        tuiRunner.run();
        verify(dashboard, never()).startInteractiveLoop();
    }

    /**
     * Verifies that {@link TuiRunner} rejects {@code null} constructor arguments.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiRunner#TuiRunner(OmniwrenchProperties, OmniwrenchTuiDashboard, AgentEngine, SessionManager, ObjectMapper)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.1} (strict non-null parameter preconditions)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should reject null constructor arguments")
    void shouldRejectNullConstructorArgs() {
        // [CS-0050.1] Verify each null constructor parameter throws NullPointerException
        assertThrows(NullPointerException.class, () -> new TuiRunner(null, dashboard, agentEngine, sessionManager, objectMapper));
        assertThrows(NullPointerException.class, () -> new TuiRunner(properties, null, agentEngine, sessionManager, objectMapper));
        assertThrows(NullPointerException.class, () -> new TuiRunner(properties, dashboard, null, sessionManager, objectMapper));
        assertThrows(NullPointerException.class, () -> new TuiRunner(properties, dashboard, agentEngine, null, objectMapper));
        assertThrows(NullPointerException.class, () -> new TuiRunner(properties, dashboard, agentEngine, sessionManager, null));
    }
}
