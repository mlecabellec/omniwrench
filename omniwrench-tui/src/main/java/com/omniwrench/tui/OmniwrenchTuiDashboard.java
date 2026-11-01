package com.omniwrench.tui;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Interactive dual-mode TUI orchestrator driving both the Minimal inline stream mode
 * ({@link MinimalTuiModeEngine}) and the Full-Screen dynamically resizable workbench mode
 * ({@link FullscreenTuiModeEngine}).
 *
 * <p>Inspired by OpenCode, Antigravity CLI ({@code agy}), OpenClaw, and Claude Code,
 * supporting responsive viewport adapting, SSH &amp; tmux multiplexer detection,
 * OSC 52 clipboard tunneling, side-channel {@code /btw} queries, {@code /goal} management,
 * semantic XTerm-256 themes, and centered modal command palettes.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00071 (Multi-Pane Terminal HUD), REQ-00084 (Markdown &amp; Code Highlighting)</li>
 *   <li>Requirement: REQ-00088 (Dual Chat Mode Reasoning), REQ-00099 (XTerm-256 Color Palette TUI Visual Theming Engine)</li>
 *   <li>Requirement: REQ-00100 (Dual-Mode Minimal Inline Stream &amp; Full-Screen Resizable TUI Workbench)</li>
 *   <li>Feature: FR-00001, FR-00046, FR-00047, FR-00048, FR-00049, FR-00050</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20260822-003, TSK-20261101-016, TSK-20261101-018</li>
 *   <li>ADR: ADR-0001, ADR-0047, ADR-0063</li>
 * </ul>
 */
@Component
public final class OmniwrenchTuiDashboard {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OmniwrenchTuiDashboard.class);

    /** Default HTTP server port per CS-0040.2. */
    private static final int DEFAULT_SERVER_PORT = 8080;

    /** Terminal renderer component. */
    private final TerminalRenderer renderer;
    /** Agent execution engine. */
    private final AgentEngine agentEngine;
    /** Session manager service. */
    private final SessionManager sessionManager;
    /** Tool registry service. */
    private final ToolRegistry toolRegistry;
    /** Runtime configuration properties. */
    private final OmniwrenchProperties properties;
    /** Terminal environment detector. */
    private final TerminalEnvironmentDetector environmentDetector;
    /** Minimal inline TUI mode engine. */
    private final MinimalTuiModeEngine minimalEngine;
    /** Fullscreen resizable TUI mode engine. */
    private final FullscreenTuiModeEngine fullscreenEngine;

    /** Web server port injected from Spring environment. */
    @Value("${server.port:8080}")
    private int serverPort = DEFAULT_SERVER_PORT;

    /**
     * Constructs the TUI dashboard with required services, defaulting to standard environment detector.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00047, CS-0050.1
     *
     * @param rendererVal terminal renderer component, must not be null
     * @param agentEngineVal agent reasoning engine, must not be null
     * @param sessionManagerVal session lifecycle manager, must not be null
     * @param toolRegistryVal tool registry service, must not be null
     * @param propertiesVal configuration properties, must not be null
     */
    public OmniwrenchTuiDashboard(final TerminalRenderer rendererVal,
                                  final AgentEngine agentEngineVal,
                                  final SessionManager sessionManagerVal,
                                  final ToolRegistry toolRegistryVal,
                                  final OmniwrenchProperties propertiesVal) {
        // [REQ-00100, FR-00047] Delegate to primary constructor with default TerminalEnvironmentDetector
        this(rendererVal, agentEngineVal, sessionManagerVal, toolRegistryVal, propertiesVal, new TerminalEnvironmentDetector());
    }

    /**
     * Constructs the TUI dashboard with required services and explicit environment detector.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00047, CS-0050.1
     *
     * @param rendererVal terminal renderer component, must not be null
     * @param agentEngineVal agent reasoning engine, must not be null
     * @param sessionManagerVal session lifecycle manager, must not be null
     * @param toolRegistryVal tool registry service, must not be null
     * @param propertiesVal configuration properties, must not be null
     * @param environmentDetectorVal terminal environment detector, must not be null
     */
    @Autowired
    public OmniwrenchTuiDashboard(final TerminalRenderer rendererVal,
                                  final AgentEngine agentEngineVal,
                                  final SessionManager sessionManagerVal,
                                  final ToolRegistry toolRegistryVal,
                                  final OmniwrenchProperties propertiesVal,
                                  final TerminalEnvironmentDetector environmentDetectorVal) {
        // [REQ-00100, FR-00047, CS-0050.1] Validate constructor dependencies
        this.renderer = Objects.requireNonNull(rendererVal, "renderer must not be null");
        this.agentEngine = Objects.requireNonNull(agentEngineVal, "agentEngine must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManagerVal, "sessionManager must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistryVal, "toolRegistry must not be null");
        // [REQ-00100, FR-00047, CS-0050.1] Validate configuration, detector, and instantiate mode engines
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");
        this.environmentDetector = Objects.requireNonNull(environmentDetectorVal, "environmentDetector must not be null");
        this.minimalEngine = new MinimalTuiModeEngine(
                this.renderer, this.agentEngine, this.sessionManager, this.toolRegistry, this.properties);
        this.fullscreenEngine = new FullscreenTuiModeEngine(
                this.agentEngine, this.sessionManager, this.toolRegistry, this.properties);
    }

    /**
     * Starts the interactive TUI loop using the configured display mode
     * ({@code minimal} by default, or {@code fullscreen} when configured).
     *
     * <p>Traceability: REQ-00100, FR-00047, FR-00050, CS-0060.3
     */
    public void startInteractiveLoop() {
        // [REQ-00100, FR-00047, FR-00050, CS-0060.3] Route to fullscreen or minimal loop based on configuration
        final TuiDisplayMode configuredMode = TuiDisplayMode.fromString(properties.getTui().getDisplayMode());
        LOGGER.info("Starting interactive TUI loop in {} mode (port {})", configuredMode, serverPort);
        if (configuredMode == TuiDisplayMode.FULLSCREEN) {
            startFullscreenLoop();
        } else {
            startMinimalLoop();
        }
    }

    /**
     * Starts the minimal inline stream-oriented TUI mode.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00047
     */
    public void startMinimalLoop() {
        // [REQ-00100, FR-00047] Detect terminal environment, synchronize property, and start minimal engine
        final TerminalEnvironment env = environmentDetector.detect();
        final SessionContext session = sessionManager.getDefaultSession();
        properties.getTui().setDisplayMode(TuiDisplayMode.MINIMAL.getConfigKey());
        minimalEngine.start(session, env);
    }

    /**
     * Starts the full-screen dynamically resizable TUI workbench mode.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00047, FR-00048
     */
    public void startFullscreenLoop() {
        // [REQ-00100, FR-00047, FR-00048] Detect terminal environment, synchronize property, and start fullscreen engine
        final TerminalEnvironment env = environmentDetector.detect();
        final SessionContext session = sessionManager.getDefaultSession();
        properties.getTui().setDisplayMode(TuiDisplayMode.FULLSCREEN.getConfigKey());
        fullscreenEngine.start(session, env);
    }

    /**
     * Renders a non-interactive snapshot of the minimal TUI view for diagnostics or testing.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00047
     *
     * @return rendered minimal TUI snapshot string
     */
    public String renderMinimalSnapshot() {
        // [REQ-00100, FR-00047] Render deterministic minimal TUI snapshot
        final TerminalEnvironment env = environmentDetector.detect();
        final SessionContext session = sessionManager.getDefaultSession();
        return minimalEngine.renderSnapshot(session, env);
    }

    /**
     * Renders a non-interactive snapshot of the fullscreen TUI view for diagnostics or testing.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     *
     * @return rendered fullscreen TUI snapshot string
     */
    public String renderFullscreenSnapshot() {
        // [REQ-00071, REQ-00100, FR-00048] Render deterministic fullscreen TUI snapshot
        final TerminalEnvironment env = environmentDetector.detect();
        final SessionContext session = sessionManager.getDefaultSession();
        return fullscreenEngine.renderSnapshot(session, env);
    }
}
