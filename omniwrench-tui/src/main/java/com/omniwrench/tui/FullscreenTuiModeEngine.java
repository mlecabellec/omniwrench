package com.omniwrench.tui;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Full-screen dynamically resizable TUI presentation engine inspired by OpenCode,
 * Antigravity CLI ({@code agy}), OpenClaw, and Claude Code.
 *
 * <p>Operates inside the terminal's alternate screen buffer ({@code \033[?1049h}) with guaranteed
 * restoration on exit ({@code \033[?1049l}), providing:
 * <ul>
 *   <li>Dedicated Home and Session routes with responsive multi-pane docking</li>
 *   <li>Live viewport reflow via {@code /resize <cols> <rows>} or environment re-detection</li>
 *   <li>Centered modal overlays for Command Palette ({@code /palette}), Theme Picker ({@code /themes}),
 *       and Help ({@code /help})</li>
 *   <li>Collapsible reasoning stream ({@code /thinking}) and tool output badges ({@code /tools})</li>
 *   <li>Side-channel {@code /btw} cards and OSC 52 clipboard copying ({@code /copy})</li>
 * </ul>
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00071 (Multi-Pane Terminal HUD), REQ-00084 (Markdown &amp; Code Highlighting)</li>
 *   <li>Requirement: REQ-00088 (Dual Chat Mode Reasoning), REQ-00099 (XTerm-256 Color Palette TUI Visual Theming Engine)</li>
 *   <li>Requirement: REQ-00100 (Dual-Mode Minimal Inline Stream &amp; Full-Screen Resizable TUI Workbench)</li>
 *   <li>Feature: FR-00001, FR-00046, FR-00047, FR-00048, FR-00049, FR-00050</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20261101-016, TSK-20261101-018</li>
 *   <li>ADR: ADR-0005, ADR-0013, ADR-0043, ADR-0063</li>
 * </ul>
 */
@Component
public final class FullscreenTuiModeEngine implements TuiViewModeEngine {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FullscreenTuiModeEngine.class);

    /** Enter alternate screen buffer and home cursor escape sequence per CS-0040.2. */
    private static final String ENTER_ALT_SCREEN = "\u001B[?1049h\u001B[2J\u001B[H";
    /** Clear alternate screen and move cursor to top-left home position per CS-0040.2. */
    private static final String CLEAR_AND_HOME = "\u001B[2J\u001B[H";
    /** Exit alternate screen buffer and restore main screen escape sequence per CS-0040.2. */
    private static final String EXIT_ALT_SCREEN = "\u001B[?1049l";

    /** Prefix length for `/btw ` command string per CS-0040.2. */
    private static final int BTW_PREFIX_LENGTH = 5;
    /** Prefix length for `/goal ` command string per CS-0040.2. */
    private static final int GOAL_PREFIX_LENGTH = 6;
    /** Prefix length for `/theme ` command string per CS-0040.2. */
    private static final int THEME_PREFIX_LENGTH = 7;
    /** Prefix length for `/resize ` command string per CS-0040.2. */
    private static final int RESIZE_PREFIX_LENGTH = 8;
    /** Prefix length for `/scroll ` command string per CS-0040.2. */
    private static final int SCROLL_PREFIX_LENGTH = 8;
    /** Prefix length for `/sidebar ` command string per CS-0040.2. */
    private static final int SIDEBAR_PREFIX_LENGTH = 9;
    /** Default scroll step lines per `/scroll up` or `/scroll down` per CS-0040.2. */
    private static final int DEFAULT_SCROLL_STEP = 5;
    /** Expected token count for `/resize <cols> <rows>` arguments per CS-0040.2. */
    private static final int RESIZE_ARG_COUNT = 2;

    /** Default HTTP server port per CS-0040.2. */
    private static final int DEFAULT_SERVER_PORT = 8080;

    /** Fullscreen layout compositor. */
    private final FullscreenTuiLayout layout;
    /** Agent reasoning engine. */
    private final AgentEngine agentEngine;
    /** Session lifecycle manager. */
    private final SessionManager sessionManager;
    /** Tool registry service. */
    private final ToolRegistry toolRegistry;
    /** Runtime configuration properties. */
    private final OmniwrenchProperties properties;

    /** HTTP server port. */
    @Value("${server.port:8080}")
    private int serverPort = DEFAULT_SERVER_PORT;

    /**
     * Constructs the FullscreenTuiModeEngine with default FullscreenTuiLayout.
     *
     * <p>Traceability: REQ-00100, FR-00047, FR-00048, CS-0050.1
     *
     * @param agentEngineVal agent reasoning engine, must not be null
     * @param sessionManagerVal session manager, must not be null
     * @param toolRegistryVal tool registry, must not be null
     * @param propertiesVal configuration properties, must not be null
     */
    @Autowired
    public FullscreenTuiModeEngine(final AgentEngine agentEngineVal,
                                   final SessionManager sessionManagerVal,
                                   final ToolRegistry toolRegistryVal,
                                   final OmniwrenchProperties propertiesVal) {
        // [REQ-00100, FR-00047] Delegate to primary constructor with default FullscreenTuiLayout
        this(new FullscreenTuiLayout(), agentEngineVal, sessionManagerVal, toolRegistryVal, propertiesVal);
    }

    /**
     * Constructs the FullscreenTuiModeEngine with explicit FullscreenTuiLayout.
     *
     * <p>Traceability: REQ-00100, FR-00047, FR-00048, CS-0050.1
     *
     * @param layoutVal fullscreen layout compositor, must not be null
     * @param agentEngineVal agent reasoning engine, must not be null
     * @param sessionManagerVal session manager, must not be null
     * @param toolRegistryVal tool registry, must not be null
     * @param propertiesVal configuration properties, must not be null
     */
    public FullscreenTuiModeEngine(final FullscreenTuiLayout layoutVal,
                                   final AgentEngine agentEngineVal,
                                   final SessionManager sessionManagerVal,
                                   final ToolRegistry toolRegistryVal,
                                   final OmniwrenchProperties propertiesVal) {
        // [REQ-00100, FR-00047, CS-0050.1] Validate non-null layout and core dependencies
        this.layout = Objects.requireNonNull(layoutVal, "layout must not be null");
        this.agentEngine = Objects.requireNonNull(agentEngineVal, "agentEngine must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManagerVal, "sessionManager must not be null");
        // [REQ-00100, FR-00047, CS-0050.1] Validate non-null toolRegistry and properties
        this.toolRegistry = Objects.requireNonNull(toolRegistryVal, "toolRegistry must not be null");
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");
    }

    /**
     * Returns {@link TuiDisplayMode#FULLSCREEN} as the display mode served by this engine.
     *
     * <p>Traceability: REQ-00100, FR-00047, CS-0030.1
     *
     * @return {@link TuiDisplayMode#FULLSCREEN}
     */
    @Override
    public TuiDisplayMode getDisplayMode() {
        // [REQ-00100, FR-00047] Identify engine as FULLSCREEN workbench mode
        return TuiDisplayMode.FULLSCREEN;
    }

    /**
     * Renders a deterministic, non-interactive fullscreen frame snapshot for diagnostics or automated testing.
     *
     * <p>Traceability: REQ-00071, REQ-00099, REQ-00100, FR-00046, FR-00048, CS-0050.1
     *
     * @param session active session context, must not be null
     * @param environment terminal environment metadata, must not be null
     * @return complete multi-line ANSI string representing the fullscreen frame
     */
    @Override
    public String renderSnapshot(final SessionContext session, final TerminalEnvironment environment) {
        // [REQ-00100, FR-00048, CS-0050.1] Validate non-null session and environment
        final SessionContext nonNullSession = Objects.requireNonNull(session, "session must not be null");
        final TerminalEnvironment nonNullEnv = Objects.requireNonNull(environment, "environment must not be null");
        final TuiThemePalette palette = TuiThemePalette.resolve(properties.getTui().getTheme());
        // [REQ-00100, FR-00048] Select HOME route when session is empty, otherwise SESSION route
        final FullscreenTuiLayout.Route initialRoute = nonNullSession.getMessages().isEmpty()
                ? FullscreenTuiLayout.Route.HOME
                : FullscreenTuiLayout.Route.SESSION;
        final FullscreenTuiLayout.ViewState state = populateTelemetryAndRuntime(
                new FullscreenTuiLayout.ViewState.Builder()
                        .route(initialRoute)
                        .overlay(FullscreenTuiLayout.OverlayModal.NONE)
                        .sidebarPolicy(properties.getTui().getSidebarMode())
                        .showThinking(true)
                        .showTools(true)
                        .scrollOffset(0)
                        .currentGoal("Autonomous pair-programming and tool assistance")
        ).build();
        // [REQ-00071, REQ-00100, FR-00048] Delegate frame composition to FullscreenTuiLayout
        return layout.renderFrame(nonNullSession, nonNullEnv, palette, state);
    }

    /**
     * Enters the terminal alternate screen buffer and runs the interactive fullscreen workbench loop.
     *
     * <p>Traceability: REQ-00071, REQ-00099, REQ-00100, FR-00047, FR-00048, FR-00049, CS-0050.1, CS-0050.6
     *
     * @param session active session context, must not be null
     * @param environment terminal environment metadata, must not be null
     */
    @Override
    public void start(final SessionContext session, final TerminalEnvironment environment) {
        // [REQ-00100, FR-00047, CS-0050.1] Validate non-null session and environment
        final SessionContext nonNullSession = Objects.requireNonNull(session, "session must not be null");
        TerminalEnvironment currentEnv = Objects.requireNonNull(environment, "environment must not be null");

        // [REQ-00100, FR-00047] Emit window title and enter alternate screen buffer (\033[?1049h)
        System.out.print(currentEnv.formatWindowTitle("Omniwrench :: Fullscreen Agent Workbench"));
        System.out.print(currentEnv.formatTmuxWindowName("omniwrench-fs"));
        System.out.print(ENTER_ALT_SCREEN);
        System.out.flush();

        // [REQ-00100, FR-00048, FR-00049] Initialize mutable interactive session state
        FullscreenTuiLayout.Route currentRoute = nonNullSession.getMessages().isEmpty()
                ? FullscreenTuiLayout.Route.HOME
                : FullscreenTuiLayout.Route.SESSION;
        FullscreenTuiLayout.OverlayModal activeOverlay = FullscreenTuiLayout.OverlayModal.NONE;
        String sidebarPolicy = properties.getTui().getSidebarMode();
        boolean showThinking = true;
        boolean showTools = true;
        int scrollOffset = 0;
        String currentGoal = "Autonomous pair-programming and tool assistance";
        String btwQuestion = "";
        String btwAnswer = "";
        String statusNotice = "Fullscreen TUI ready. Type a prompt or /palette for commands.";
        String lastAgentContent = "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            // [REQ-00100, CS-0050.6] Enforce bounded interactive turn loop
            int turnCount = 0;
            while (turnCount < MAX_INTERACTIVE_TURNS) {
                turnCount++;
                final TuiThemePalette palette = TuiThemePalette.resolve(properties.getTui().getTheme());
                final FullscreenTuiLayout.ViewState state = populateTelemetryAndRuntime(
                        new FullscreenTuiLayout.ViewState.Builder()
                                .route(currentRoute)
                                .overlay(activeOverlay)
                                .sidebarPolicy(sidebarPolicy)
                                .showThinking(showThinking)
                                .showTools(showTools)
                                .scrollOffset(scrollOffset)
                                .currentGoal(currentGoal)
                                .btw(btwQuestion, btwAnswer)
                                .statusNotice(statusNotice)
                ).build();

                // [REQ-00071, REQ-00100, FR-00048] Redraw entire fullscreen frame and prompt cursor
                System.out.print(CLEAR_AND_HOME);
                System.out.println(layout.renderFrame(nonNullSession, currentEnv, palette, state));
                System.out.print(" ❯ ");
                System.out.flush();

                // [REQ-00100, FR-00047] Read user input line and check for termination
                final String line = reader.readLine();
                if (line == null || "exit".equalsIgnoreCase(line.trim()) || "quit".equalsIgnoreCase(line.trim())) {
                    break;
                }

                final String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    statusNotice = "";
                    continue;
                }

                // [REQ-00100, FR-00049, CS-0060.3] Dispatch interactive fullscreen commands or regular prompts
                if ("/palette".equalsIgnoreCase(trimmed) || "/p".equalsIgnoreCase(trimmed) || "?".equals(trimmed)) {
                    activeOverlay = FullscreenTuiLayout.OverlayModal.COMMAND_PALETTE;
                    statusNotice = "Command Palette opened. Type /close to dismiss.";
                } else if ("/themes".equalsIgnoreCase(trimmed)) {
                    activeOverlay = FullscreenTuiLayout.OverlayModal.THEME_PICKER;
                    statusNotice = "Theme Picker opened. Type /theme <name> or /close.";
                } else if ("/help".equalsIgnoreCase(trimmed)) {
                    activeOverlay = FullscreenTuiLayout.OverlayModal.HELP;
                    statusNotice = "Help modal opened. Type /close to dismiss.";
                } else if ("/close".equalsIgnoreCase(trimmed) || "/dismiss".equalsIgnoreCase(trimmed)) {
                    activeOverlay = FullscreenTuiLayout.OverlayModal.NONE;
                    statusNotice = "Modal dismissed.";
                } else if ("/home".equalsIgnoreCase(trimmed)) {
                    activeOverlay = FullscreenTuiLayout.OverlayModal.NONE;
                    currentRoute = FullscreenTuiLayout.Route.HOME;
                    statusNotice = "Switched to Home view.";
                } else if ("/session".equalsIgnoreCase(trimmed) || "/chat".equalsIgnoreCase(trimmed)) {
                    activeOverlay = FullscreenTuiLayout.OverlayModal.NONE;
                    currentRoute = FullscreenTuiLayout.Route.SESSION;
                    statusNotice = "Switched to Session transcript view.";
                } else if (trimmed.toLowerCase(Locale.ROOT).startsWith("/sidebar")) {
                    // [REQ-00071, FR-00048] Update sidebar visibility policy
                    sidebarPolicy = resolveNextSidebarPolicy(trimmed, sidebarPolicy);
                    properties.getTui().setSidebarMode(sidebarPolicy);
                    statusNotice = "Sidebar visibility policy set to: " + sidebarPolicy;
                } else if (trimmed.toLowerCase(Locale.ROOT).startsWith("/resize")) {
                    // [REQ-00100, FR-00048] Dynamically reflow fullscreen viewport dimensions
                    currentEnv = handleResizeCommand(trimmed, currentEnv);
                    statusNotice = "Viewport reflowed to " + currentEnv.getColumns() + "x" + currentEnv.getRows();
                } else if (trimmed.toLowerCase(Locale.ROOT).startsWith("/scroll")) {
                    // [REQ-00071, FR-00048] Adjust transcript scroll offset
                    scrollOffset = handleScrollCommand(trimmed, scrollOffset);
                    statusNotice = "Transcript scroll offset: " + scrollOffset;
                } else if (trimmed.toLowerCase(Locale.ROOT).startsWith("/theme")) {
                    // [REQ-00099, FR-00046] Switch or cycle XTerm-256 color theme
                    final String updatedTheme = handleThemeCommand(trimmed);
                    activeOverlay = FullscreenTuiLayout.OverlayModal.NONE;
                    statusNotice = "Active theme switched to: " + updatedTheme;
                } else if ("/thinking toggle".equalsIgnoreCase(trimmed)) {
                    // [REQ-00088, FR-00035] Toggle reasoning stream expansion
                    showThinking = !showThinking;
                    statusNotice = "Reasoning stream visibility: " + (showThinking ? "EXPANDED" : "COLLAPSED");
                } else if ("/tools toggle".equalsIgnoreCase(trimmed) || "/tools".equalsIgnoreCase(trimmed)) {
                    // [REQ-00071, FR-00048] Toggle tool invocation output expansion
                    showTools = !showTools;
                    statusNotice = "Tool execution output visibility: " + (showTools ? "EXPANDED" : "COLLAPSED");
                } else if (trimmed.toLowerCase(Locale.ROOT).startsWith("/goal")) {
                    currentGoal = extractGoalText(trimmed, currentGoal);
                    statusNotice = "Updated session goal.";
                } else if ("/btw clear".equalsIgnoreCase(trimmed)) {
                    btwQuestion = "";
                    btwAnswer = "";
                    statusNotice = "Cleared side-channel card.";
                } else if (trimmed.toLowerCase(Locale.ROOT).startsWith("/btw")) {
                    // [REQ-00100, FR-00048] Execute ephemeral side-channel query and display inline card
                    if (trimmed.length() > BTW_PREFIX_LENGTH) {
                        btwQuestion = trimmed.substring(BTW_PREFIX_LENGTH).trim();
                        final AgentMessage sideMsg = agentEngine.processPrompt(
                                nonNullSession, "[SIDE-CHANNEL QUESTION] " + btwQuestion);
                        btwAnswer = sideMsg.getContent();
                        currentRoute = FullscreenTuiLayout.Route.SESSION;
                        statusNotice = "Side-channel /btw response received.";
                    } else {
                        statusNotice = "Usage: /btw <question> or /btw clear";
                    }
                } else if ("/copy".equalsIgnoreCase(trimmed)) {
                    statusNotice = handleCopyCommand(currentEnv, lastAgentContent);
                } else if ("/clear".equalsIgnoreCase(trimmed)) {
                    activeOverlay = FullscreenTuiLayout.OverlayModal.NONE;
                    scrollOffset = 0;
                    btwQuestion = "";
                    btwAnswer = "";
                    statusNotice = "Screen state cleared.";
                } else {
                    // [REQ-00030, REQ-00100, FR-00003] Regular prompt or engine slash command
                    activeOverlay = FullscreenTuiLayout.OverlayModal.NONE;
                    currentRoute = FullscreenTuiLayout.Route.SESSION;
                    scrollOffset = 0;
                    final AgentMessage response = agentEngine.processPrompt(nonNullSession, trimmed);
                    lastAgentContent = response.getContent();
                    statusNotice = "Turn completed (" + nonNullSession.getMessages().size() + " messages in session).";
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Error during Fullscreen TUI execution", e);
        } finally {
            // [REQ-00100, FR-00047] Always restore normal screen buffer (\033[?1049l) on exit
            System.out.print(EXIT_ALT_SCREEN);
            System.out.println("Exited Omniwrench Fullscreen TUI. Goodbye!");
            System.out.flush();
        }
    }

    /**
     * Populates live telemetry and runtime configuration onto the given {@link FullscreenTuiLayout.ViewState.Builder}.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     *
     * @param builder view state builder
     * @return populated builder
     */
    private FullscreenTuiLayout.ViewState.Builder populateTelemetryAndRuntime(
            final FullscreenTuiLayout.ViewState.Builder builder) {
        // [REQ-00071, REQ-00100, FR-00048] Attach runtime telemetry and tool registry metadata
        final String effortName = agentEngine.getThinkingEffort() != null
                ? agentEngine.getThinkingEffort().toLowerCase(Locale.ROOT)
                : "medium";
        return builder
                .telemetry(
                        sessionManager.getActiveSessionCount(),
                        toolRegistry.getToolCount(),
                        toolRegistry.getAllDefinitions().stream().map(t -> t.getName()).toList()
                )
                .runtime(
                        effortName,
                        properties.getMode(),
                        serverPort
                );
    }

    /**
     * Resolves the next sidebar visibility policy (`auto`, `show`, or `hide`) from a `/sidebar` command.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048, CS-0060.3
     *
     * @param trimmed full `/sidebar` command string
     * @param currentPolicy current sidebar policy
     * @return next sidebar policy string
     */
    private String resolveNextSidebarPolicy(final String trimmed, final String currentPolicy) {
        // [REQ-00071, FR-00048, CS-0060.3] Parse explicit policy argument or toggle current state
        if (trimmed.length() > SIDEBAR_PREFIX_LENGTH) {
            final String arg = trimmed.substring(SIDEBAR_PREFIX_LENGTH).trim().toLowerCase(Locale.ROOT);
            if ("show".equals(arg) || "on".equals(arg)) {
                return "show";
            } else if ("hide".equals(arg) || "off".equals(arg)) {
                return "hide";
            } else if ("auto".equals(arg)) {
                return "auto";
            } else {
                return "hide".equalsIgnoreCase(currentPolicy) ? "show" : "hide";
            }
        } else {
            return "hide".equalsIgnoreCase(currentPolicy) ? "show" : "hide";
        }
    }

    /**
     * Parses a `/resize <cols> <rows>` command and returns an updated {@link TerminalEnvironment}.
     *
     * <p>Traceability: REQ-00100, FR-00048
     *
     * @param trimmed full `/resize` command string
     * @param currentEnv current terminal environment
     * @return resized terminal environment, or unchanged environment if arguments are invalid
     */
    private TerminalEnvironment handleResizeCommand(final String trimmed, final TerminalEnvironment currentEnv) {
        // [REQ-00100, FR-00048] Validate argument presence and token count
        if (trimmed.length() <= RESIZE_PREFIX_LENGTH) {
            return currentEnv;
        }
        final String[] parts = trimmed.substring(RESIZE_PREFIX_LENGTH).trim().split("\\s+");
        if (parts.length < RESIZE_ARG_COUNT) {
            return currentEnv;
        }
        // [REQ-00100, FR-00048] Parse integer columns and rows and return updated TerminalEnvironment
        try {
            final int newCols = Integer.parseInt(parts[0]);
            final int newRows = Integer.parseInt(parts[1]);
            return currentEnv.withDimensions(newCols, newRows);
        } catch (final NumberFormatException e) {
            LOGGER.debug("Ignored invalid /resize dimensions: {}", trimmed);
            return currentEnv;
        }
    }

    /**
     * Computes the updated transcript scroll offset for a `/scroll` command.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048, CS-0060.3
     *
     * @param trimmed full `/scroll` command string
     * @param currentOffset current scroll offset from bottom
     * @return updated non-negative scroll offset
     */
    private int handleScrollCommand(final String trimmed, final int currentOffset) {
        // [REQ-00071, FR-00048, CS-0060.3] Evaluate scroll direction token
        if (trimmed.length() <= SCROLL_PREFIX_LENGTH) {
            return currentOffset;
        }
        final String direction = trimmed.substring(SCROLL_PREFIX_LENGTH).trim().toLowerCase(Locale.ROOT);
        if ("up".equals(direction)) {
            return currentOffset + DEFAULT_SCROLL_STEP;
        } else if ("down".equals(direction)) {
            return Math.max(0, currentOffset - DEFAULT_SCROLL_STEP);
        } else if ("bottom".equals(direction) || "reset".equals(direction)) {
            return 0;
        } else if ("top".equals(direction)) {
            return FullscreenTuiLayout.MAX_LAYOUT_ITERATIONS;
        } else {
            return currentOffset;
        }
    }

    /**
     * Switches or cycles the active XTerm-256 theme palette from a `/theme` command.
     *
     * <p>Traceability: REQ-00099, FR-00046, CS-0060.3
     *
     * @param trimmed full `/theme` command string
     * @return name of the newly activated theme
     */
    private String handleThemeCommand(final String trimmed) {
        // [REQ-00099, FR-00046, CS-0060.3] Resolve requested theme name or cycle to next theme
        if (trimmed.length() > THEME_PREFIX_LENGTH) {
            final String requested = trimmed.substring(THEME_PREFIX_LENGTH).trim();
            final TuiThemePalette resolved = TuiThemePalette.resolve(requested);
            properties.getTui().setTheme(resolved.getName());
            return resolved.getName();
        } else {
            final TuiThemePalette next = TuiThemePalette.nextTheme(properties.getTui().getTheme());
            properties.getTui().setTheme(next.getName());
            return next.getName();
        }
    }

    /**
     * Extracts updated goal text from a `/goal` command, or returns the existing goal if none provided.
     *
     * <p>Traceability: REQ-00100, FR-00048
     *
     * @param trimmed full `/goal` command string
     * @param currentGoal existing goal description
     * @return updated or existing goal description
     */
    private String extractGoalText(final String trimmed, final String currentGoal) {
        // [REQ-00100, FR-00048] Extract substring after `/goal ` prefix when present
        if (trimmed.length() > GOAL_PREFIX_LENGTH) {
            return trimmed.substring(GOAL_PREFIX_LENGTH).trim();
        }
        return currentGoal;
    }

    /**
     * Copies the last assistant response to the client clipboard using OSC 52.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00048
     *
     * @param env terminal environment metadata
     * @param lastAgentContent last assistant response content
     * @return status notice describing copy outcome
     */
    private String handleCopyCommand(final TerminalEnvironment env, final String lastAgentContent) {
        // [REQ-00001, FR-00048] Emit OSC 52 sequence and return byte count notice
        if (lastAgentContent.isEmpty()) {
            return "No assistant response available to copy yet.";
        }
        System.out.print(env.formatOsc52Clipboard(lastAgentContent));
        System.out.flush();
        final int byteCount = lastAgentContent.getBytes(StandardCharsets.UTF_8).length;
        return "Copied " + byteCount + " bytes to system clipboard via OSC 52.";
    }
}
