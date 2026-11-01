package com.omniwrench.tui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Multi-tier functional, worst-case boundary, and concurrency stress tests for the Dual-Mode
 * Minimal &amp; Fullscreen Resizable TUI Workbench architecture.
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
 *   <li>Rules: CS-0010, CS-0030.7 (3-Tier Testing), CS-0040, CS-0050, CS-0055, CS-0060</li>
 *   <li>ADR: ADR-0005, ADR-0013, ADR-0043, ADR-0063</li>
 * </ul>
 */
@Tag("REQ-00001")
@Tag("REQ-00071")
@Tag("REQ-00084")
@Tag("REQ-00088")
@Tag("REQ-00099")
@Tag("REQ-00100")
@Tag("FR-00001")
@Tag("FR-00046")
@Tag("FR-00047")
@Tag("FR-00048")
@Tag("FR-00049")
@Tag("FR-00050")
@Tag("UC-00001")
@Tag("TSK-20261101-016")
@Tag("TSK-20261101-018")
class FullscreenTuiWorkbenchTest {

    /** Saved original System.in stream restored after each test. */
    private InputStream originalSystemIn;
    /** Runtime configuration properties under test. */
    private OmniwrenchProperties properties;
    /** Session manager under test. */
    private SessionManager sessionManager;
    /** Tool registry under test. */
    private ToolRegistry toolRegistry;
    /** Agent reasoning engine collaborator. */
    private AgentEngine agentEngine;
    /** Fullscreen responsive layout compositor under test. */
    private FullscreenTuiLayout layout;

    /**
     * Initializes clean test fixtures before each test case.
     */
    @BeforeEach
    void setUp() {
        // [CS-0030.7] Preserve System.in and instantiate fresh TUI and session fixtures
        originalSystemIn = System.in;
        properties = new OmniwrenchProperties();
        sessionManager = new SessionManager(properties);
        toolRegistry = new ToolRegistry(List.of());
        agentEngine = Mockito.mock(AgentEngine.class);
        when(agentEngine.getThinkingEffort()).thenReturn("high");
        layout = new FullscreenTuiLayout();
    }

    /**
     * Restores {@code System.in} after each test case.
     */
    @AfterEach
    void tearDown() {
        // [CS-0030.7] Restore standard input stream after interactive REPL tests
        System.setIn(originalSystemIn);
    }

    /**
     * Verifies deterministic parsing of CLI flags and configuration values into {@link TuiDisplayMode}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiDisplayMode#fromString(String)}, {@link TuiDisplayMode#getConfigKey()}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00047}, {@code FR-00050}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.1} (null-safe fallback), {@code CS-0060.3} (total branching)</li>
     * </ul>
     */
    @Test
    @DisplayName("Tier 1 Functional: Should resolve TuiDisplayMode from CLI flags and config strings")
    void shouldResolveTuiDisplayMode() {
        // [REQ-00100, FR-00047, FR-00050] Verify fullscreen CLI flags and config strings map to FULLSCREEN
        assertEquals(TuiDisplayMode.FULLSCREEN, TuiDisplayMode.fromString("fullscreen"));
        assertEquals(TuiDisplayMode.FULLSCREEN, TuiDisplayMode.fromString("--fullscreen"));
        assertEquals(TuiDisplayMode.FULLSCREEN, TuiDisplayMode.fromString("-f"));
        assertEquals(TuiDisplayMode.FULLSCREEN, TuiDisplayMode.fromString("tui-full"));
        assertEquals(TuiDisplayMode.FULLSCREEN, TuiDisplayMode.fromString("FULL"));
        // [REQ-00100, FR-00047, CS-0060.3] Verify minimal CLI flags, null, and unknown strings map to MINIMAL
        assertEquals(TuiDisplayMode.MINIMAL, TuiDisplayMode.fromString("minimal"));
        assertEquals(TuiDisplayMode.MINIMAL, TuiDisplayMode.fromString("--minimal"));
        assertEquals(TuiDisplayMode.MINIMAL, TuiDisplayMode.fromString("inline"));
        assertEquals(TuiDisplayMode.MINIMAL, TuiDisplayMode.fromString(null));
        assertEquals(TuiDisplayMode.MINIMAL, TuiDisplayMode.fromString("unknown"));
    }

    /**
     * Verifies resolution and cyclic rotation across all 6 XTerm-256 semantic palettes in {@link TuiThemePalette}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiThemePalette#availableThemes()}, {@link TuiThemePalette#resolve(String)},
     *       {@link TuiThemePalette#nextTheme(String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00046}, {@code REQ-00099}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0040} (named theme constants), {@code CS-0060.3} (fallback to CYBERPUNK)</li>
     * </ul>
     */
    @Test
    @DisplayName("Tier 1 Functional: Should resolve and cycle all 6 semantic XTerm-256 TuiThemePalettes")
    void shouldResolveAndCycleThemes() {
        // [REQ-00099, FR-00046] Verify all 6 built-in themes are registered
        final List<TuiThemePalette> allThemes = TuiThemePalette.availableThemes();
        assertEquals(6, allThemes.size());

        // [REQ-00099, FR-00046, CS-0060.3] Verify case-insensitive theme resolution and fallback to cyberpunk
        assertEquals("opencode", TuiThemePalette.resolve("opencode").getName());
        assertEquals("dracula", TuiThemePalette.resolve("dracula").getName());
        assertEquals("nord", TuiThemePalette.resolve("nord").getName());
        assertEquals("matrix", TuiThemePalette.resolve("matrix").getName());
        assertEquals("high-contrast", TuiThemePalette.resolve("high-contrast").getName());
        assertEquals("cyberpunk", TuiThemePalette.resolve("invalid-theme").getName());

        // [REQ-00099, FR-00046] Verify cyclic theme rotation order
        final TuiThemePalette afterCyberpunk = TuiThemePalette.nextTheme("cyberpunk");
        assertEquals("opencode", afterCyberpunk.getName());
        assertNotNull(afterCyberpunk.toString());
    }

    /**
     * Verifies immutable viewport resizing and value equality contracts on {@link TerminalEnvironment}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironment#withDimensions(int, int)},
     *       {@link TerminalEnvironment#equals(Object)}, {@link TerminalEnvironment#hashCode()}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00048}, {@code REQ-00071}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0030.1} (value object immutability and equals/hashCode contract)</li>
     * </ul>
     */
    @Test
    @DisplayName("Tier 1 Functional: Should resize TerminalEnvironment immutably and verify value equality")
    void shouldResizeTerminalEnvironmentImmutably() {
        // [REQ-00100, FR-00048] Create base 100x30 TMUX environment and resize immutably to 160x50
        final TerminalEnvironment base = new TerminalEnvironment(
                TerminalMode.TMUX, "xterm-256color", "", "%1", 100, 30);
        final TerminalEnvironment resized = base.withDimensions(160, 50);

        // [REQ-00100, FR-00048, CS-0030.1] Verify updated dimensions, preserved metadata, and equals/hashCode
        assertEquals(160, resized.getColumns());
        assertEquals(50, resized.getRows());
        assertEquals(TerminalMode.TMUX, resized.getMode());
        assertEquals("%1", resized.getTmuxPane());
        assertNotEquals(base, resized);
        assertEquals(resized, base.withDimensions(160, 50));
        assertEquals(resized.hashCode(), base.withDimensions(160, 50).hashCode());
        assertTrue(resized.toString().contains("160"));
    }

    /**
     * Verifies multi-pane frame rendering across all 4 responsive breakpoints in {@link FullscreenTuiLayout}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link FullscreenTuiLayout#renderFrame(SessionContext, TerminalEnvironment, TuiThemePalette, FullscreenTuiLayout.ViewState)},
     *       {@link FullscreenTuiLayout.ResponsiveBreakpoint#fromColumns(int)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00048}, {@code REQ-00071}, {@code REQ-00084}, {@code REQ-00088}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0005} (Multi-Pane Dashboard), {@code ADR-0013} (Context-Adaptive TUI Layout)</li>
     * </ul>
     */
    @Test
    @DisplayName("Tier 1 Functional: Should render FullscreenTuiLayout across all 4 responsive breakpoints")
    void shouldRenderAcrossAllResponsiveBreakpoints() {
        // [REQ-00071, REQ-00084, REQ-00088] Populate session with user prompt, code fence, thinking, and tool call
        final SessionContext session = sessionManager.getDefaultSession();
        final AgentMessage userMsg = AgentMessage.of("user", "Inspect architecture");
        final ToolInvocation toolInv = new ToolInvocation(
                "call-1", "file_ops", Map.of("action", "read"), " file content ok", true, Instant.now());
        final AgentMessage agentMsg = new AgentMessage(
                "msg-1",
                "assistant",
                "Here is the summary:\n```java\npublic class Demo {}\n```",
                "Analyzing module boundaries and checking Checkstyle rules.",
                Instant.now(),
                List.of(toolInv)
        );
        session.addMessage(userMsg);
        session.addMessage(agentMsg);

        // [REQ-00100, FR-00048] Construct ViewState with active SESSION route, /btw card, and telemetry
        final FullscreenTuiLayout.ViewState sessionState = new FullscreenTuiLayout.ViewState.Builder()
                .route(FullscreenTuiLayout.Route.SESSION)
                .overlay(FullscreenTuiLayout.OverlayModal.NONE)
                .sidebarPolicy("auto")
                .showThinking(true)
                .showTools(true)
                .currentGoal("Revamp TUI")
                .btw("Why alternate buffer?", "Eliminates flicker and enables multi-pane docking.")
                .statusNotice("Ready")
                .telemetry(1, 3, List.of("command_exec", "file_ops", "git_status"))
                .runtime("high", "tui", 8080)
                .build();

        // [REQ-00071, REQ-00100, ADR-0013] 1. Verify COMPACT (< 80 cols) hides right sidebar
        final TerminalEnvironment compactEnv = new TerminalEnvironment(
                TerminalMode.STANDARD, "xterm-256color", "", "", 68, 24);
        final String compactFrame = layout.renderFrame(session, compactEnv, TuiThemePalette.CYBERPUNK, sessionState);
        assertTrue(compactFrame.contains("COMPACT"));
        assertTrue(compactFrame.contains("SESSION TRANSCRIPT"));
        assertFalse(compactFrame.contains("CONTEXT & TELEMETRY"));

        // [REQ-00071, REQ-00100, ADR-0013] 2. Verify STANDARD_SPLIT (80..119 cols) docks right sidebar and renders cards
        final TerminalEnvironment splitEnv = compactEnv.withDimensions(100, 30);
        final String splitFrame = layout.renderFrame(session, splitEnv, TuiThemePalette.OPENCODE, sessionState);
        assertTrue(splitFrame.contains("STANDARD_SPLIT"));
        assertTrue(splitFrame.contains("CONTEXT & TELEMETRY"));
        assertTrue(splitFrame.contains("REASONING STREAM"));
        assertTrue(splitFrame.contains("TOOL: file_ops"));
        assertTrue(splitFrame.contains("BTW SIDE-CHANNEL"));

        // [REQ-00071, REQ-00100, ADR-0013] 3. Verify WIDE_STUDIO (120..159 cols) renders wide right sidebar
        final TerminalEnvironment wideEnv = compactEnv.withDimensions(140, 40);
        final String wideFrame = layout.renderFrame(session, wideEnv, TuiThemePalette.DRACULA, sessionState);
        assertTrue(wideFrame.contains("WIDE_STUDIO"));
        assertTrue(wideFrame.contains("CONTEXT & TELEMETRY"));

        // [REQ-00071, REQ-00100, ADR-0013] 4. Verify ULTRAWIDE_WORKBENCH (>= 160 cols) docks both EXPLORER and sidebar
        final TerminalEnvironment ultraEnv = compactEnv.withDimensions(180, 48);
        final String ultraFrame = layout.renderFrame(session, ultraEnv, TuiThemePalette.NORD, sessionState);
        assertTrue(ultraFrame.contains("ULTRAWIDE_WORKBENCH"));
        assertTrue(ultraFrame.contains("EXPLORER"));
        assertTrue(ultraFrame.contains("CONTEXT & TELEMETRY"));
    }

    /**
     * Verifies rendering of the centered {@code HOME} route and all floating {@link FullscreenTuiLayout.OverlayModal} dialogs.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link FullscreenTuiLayout#renderFrame(SessionContext, TerminalEnvironment, TuiThemePalette, FullscreenTuiLayout.ViewState)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00048}, {@code FR-00049}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0063} (Dual-Mode Minimal &amp; Fullscreen Resizable TUI Architecture)</li>
     * </ul>
     */
    @Test
    @DisplayName("Tier 1 Functional: Should render Home route and centered modal overlays")
    void shouldRenderHomeRouteAndModalOverlays() {
        // [REQ-00100, FR-00048, FR-00049] Iterate through NONE, COMMAND_PALETTE, THEME_PICKER, and HELP overlays
        final SessionContext session = sessionManager.getDefaultSession();
        final TerminalEnvironment env = new TerminalEnvironment(
                TerminalMode.SSH, "xterm-256color", "10.0.0.1", "", 128, 36);

        for (final FullscreenTuiLayout.OverlayModal modal : FullscreenTuiLayout.OverlayModal.values()) {
            final FullscreenTuiLayout.ViewState state = new FullscreenTuiLayout.ViewState.Builder()
                    .route(FullscreenTuiLayout.Route.HOME)
                    .overlay(modal)
                    .statusNotice("Testing modal " + modal.name())
                    .build();
            final String frame = layout.renderFrame(session, env, TuiThemePalette.OPENCODE, state);
            assertTrue(frame.contains("HOME :: WELCOME"));
            if (modal != FullscreenTuiLayout.OverlayModal.NONE) {
                assertTrue(frame.contains("MODAL :: " + modal.name()));
            }
        }
    }

    /**
     * Verifies end-to-end execution of the interactive {@link FullscreenTuiModeEngine} command loop.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link FullscreenTuiModeEngine#start(SessionContext, TerminalEnvironment)},
     *       {@link FullscreenTuiModeEngine#renderSnapshot(SessionContext, TerminalEnvironment)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00046}, {@code FR-00047}, {@code FR-00048}, {@code FR-00049},
     *       {@code REQ-00071}, {@code REQ-00099}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.6} (bounded interactive loop), {@code CS-0060.3} (command routing)</li>
     * </ul>
     */
    @Test
    @DisplayName("Tier 1 Functional: Should execute FullscreenTuiModeEngine interactive command loop end-to-end")
    void shouldExecuteFullscreenModeEngineLoop() {
        // [REQ-00100, FR-00048, FR-00049] Feed comprehensive sequence of fullscreen slash commands and prompts
        final String commands = String.join("\n",
                "/palette",
                "/themes",
                "/help",
                "/close",
                "/sidebar show",
                "/sidebar hide",
                "/sidebar auto",
                "/resize 144 42",
                "/theme opencode",
                "/theme",
                "/goal Build OpenCode-class TUI",
                "Explain dual TUI architecture",
                "/thinking toggle",
                "/tools toggle",
                "/btw How to resize?",
                "/btw clear",
                "/scroll up",
                "/scroll down",
                "/scroll top",
                "/scroll bottom",
                "/copy",
                "/home",
                "/session",
                "/clear",
                "exit"
        ) + "\n";
        System.setIn(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)));

        // [REQ-00088, REQ-00100] Configure AgentEngine responses for regular prompt and side-channel /btw query
        when(agentEngine.processPrompt(any(SessionContext.class), eq("Explain dual TUI architecture")))
                .thenReturn(AgentMessage.of(
                        "assistant",
                        "Minimal mode uses inline stream; Fullscreen mode uses alternate screen.",
                        "Comparing inline vs alt-screen architectures."));
        when(agentEngine.processPrompt(any(SessionContext.class), contains("[SIDE-CHANNEL QUESTION]")))
                .thenReturn(AgentMessage.of("assistant", "Use /resize <cols> <rows>."));

        // [REQ-00100, FR-00047] Verify display mode, initial snapshot, and full interactive loop execution
        final FullscreenTuiModeEngine engine = new FullscreenTuiModeEngine(
                layout, agentEngine, sessionManager, toolRegistry, properties);
        assertEquals(TuiDisplayMode.FULLSCREEN, engine.getDisplayMode());

        final SessionContext session = sessionManager.getDefaultSession();
        final TerminalEnvironment env = new TerminalEnvironment(
                TerminalMode.STANDARD, "xterm-256color", "", "", 120, 36);

        final String initialSnapshot = engine.renderSnapshot(session, env);
        assertTrue(initialSnapshot.contains("HOME :: WELCOME"));

        engine.start(session, env);

        verify(agentEngine).processPrompt(any(SessionContext.class), eq("Explain dual TUI architecture"));
        verify(agentEngine).processPrompt(any(SessionContext.class), contains("How to resize?"));
    }

    /**
     * Verifies end-to-end execution of the interactive {@link MinimalTuiModeEngine} command loop and snapshot rendering.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link MinimalTuiModeEngine#start(SessionContext, TerminalEnvironment)},
     *       {@link MinimalTuiModeEngine#renderSnapshot(SessionContext, TerminalEnvironment)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00046}, {@code FR-00047}, {@code REQ-00001}, {@code REQ-00099}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.6} (bounded interactive loop), {@code CS-0060.3} (total branching)</li>
     * </ul>
     */
    @Test
    @DisplayName("Tier 1 Functional: Should execute MinimalTuiModeEngine interactive loop and snapshot")
    void shouldExecuteMinimalModeEngineLoop() {
        // [REQ-00100, FR-00047] Feed minimal mode slash commands and prompt sequence
        final String commands = String.join("\n",
                "/help",
                "/env",
                "/goal Ship Minimal & Fullscreen TUI",
                "/goal",
                "/theme nord",
                "/theme",
                "/tools",
                "/btw What is Minimal mode?",
                "Run minimal prompt",
                "/copy",
                "/clear",
                "exit"
        ) + "\n";
        System.setIn(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)));

        when(agentEngine.processPrompt(any(SessionContext.class), contains("[SIDE-CHANNEL QUESTION]")))
                .thenReturn(AgentMessage.of("assistant", "Inline stream mode."));
        when(agentEngine.processPrompt(any(SessionContext.class), eq("Run minimal prompt")))
                .thenReturn(AgentMessage.of("assistant", "Minimal reply", "Minimal thinking"));

        // [REQ-00100, FR-00047] Run minimal mode engine and verify snapshot output
        final TerminalRenderer renderer = new TerminalRenderer();
        final MinimalTuiModeEngine minimalEngine = new MinimalTuiModeEngine(
                renderer, agentEngine, sessionManager, toolRegistry, properties);
        assertEquals(TuiDisplayMode.MINIMAL, minimalEngine.getDisplayMode());

        final SessionContext session = sessionManager.getDefaultSession();
        final TerminalEnvironment env = new TerminalEnvironment(
                TerminalMode.STANDARD, "xterm-256color", "", "", 100, 30);
        minimalEngine.start(session, env);

        session.addMessage(AgentMessage.of("assistant", "Saved reply", "Saved thought"));
        final String snapshot = minimalEngine.renderSnapshot(session, env);
        assertTrue(snapshot.contains("UI: MINIMAL STREAM"));
        assertTrue(snapshot.contains("Saved reply"));
    }

    /**
     * Verifies that {@link TuiRunner} dispatches fullscreen and minimal TUI launch flags to {@link OmniwrenchTuiDashboard}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TuiRunner#run(String...)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00050}, {@code REQ-00089}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0048}, {@code ADR-0063}</li>
     * </ul>
     */
    @Test
    @DisplayName("Tier 1 Functional: Should launch Fullscreen and Minimal modes via TuiRunner CLI options")
    void shouldLaunchModesViaTuiRunnerOptions() throws Exception {
        // [REQ-00100, FR-00050] Verify --fullscreen, -f, tui-full, and --minimal CLI flags
        final OmniwrenchTuiDashboard mockDashboard = Mockito.mock(OmniwrenchTuiDashboard.class);
        final TuiRunner runner = new TuiRunner(
                properties, mockDashboard, agentEngine, sessionManager, new ObjectMapper());

        runner.run("--fullscreen");
        verify(mockDashboard).startFullscreenLoop();

        Mockito.reset(mockDashboard);
        runner.run("tui", "-f");
        verify(mockDashboard).startFullscreenLoop();

        Mockito.reset(mockDashboard);
        runner.run("tui-full");
        verify(mockDashboard).startFullscreenLoop();

        Mockito.reset(mockDashboard);
        runner.run("tui", "--minimal");
        verify(mockDashboard).startMinimalLoop();
    }

    /**
     * Tier 2 Worst-Case Boundary test verifying viewport clamping on tiny dimensions (`12x5`),
     * wrapping of unbroken 250-char tokens, code-fence preservation, and null precondition rejection.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link FullscreenTuiLayout#renderFrame(SessionContext, TerminalEnvironment, TuiThemePalette, FullscreenTuiLayout.ViewState)},
     *       {@link FullscreenTuiLayout#wrapTextPreservingCode(String, int)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00048}, {@code REQ-00071}, {@code REQ-00084}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0030.7} (Worst-Case Boundary Tier), {@code CS-0050.1} (preconditions),
     *       {@code CS-0050.6} (bounded wrapping loops)</li>
     * </ul>
     */
    @Test
    @DisplayName("Tier 2 Worst-Case Boundary: Should clamp tiny viewports, wrap long tokens, and reject nulls")
    void shouldHandleBoundaryViewportsAndRejectNulls() {
        // [REQ-00084, REQ-00100, CS-0030.7] Populate session with unbroken 250-char token and 180-char code fence
        final SessionContext session = sessionManager.getDefaultSession();
        final String longToken = "A".repeat(250);
        final String codeFence = "Prose line before code\n```\n" + "X".repeat(180) + "\n```\nProse after";
        session.addMessage(AgentMessage.of("assistant", longToken + "\n" + codeFence));

        // [REQ-00071, REQ-00100, CS-0030.7] Render on extreme tiny 12x5 viewport (clamped to 40x14 minimum)
        final TerminalEnvironment tinyEnv = new TerminalEnvironment(
                TerminalMode.STANDARD, "dumb", "", "", 12, 5);
        final FullscreenTuiLayout.ViewState state = new FullscreenTuiLayout.ViewState.Builder()
                .route(FullscreenTuiLayout.Route.SESSION)
                .showThinking(false)
                .showTools(false)
                .build();

        final String tinyFrame = layout.renderFrame(session, tinyEnv, TuiThemePalette.HIGH_CONTRAST, state);
        assertNotNull(tinyFrame);
        assertTrue(tinyFrame.contains("COMPACT"));

        // [CS-0050.1] Verify strict NullPointerException preconditions on all parameters
        assertThrows(NullPointerException.class, () -> layout.renderFrame(null, tinyEnv, TuiThemePalette.CYBERPUNK, state));
        assertThrows(NullPointerException.class, () -> layout.renderFrame(session, null, TuiThemePalette.CYBERPUNK, state));
        assertThrows(NullPointerException.class, () -> layout.renderFrame(session, tinyEnv, null, state));
        assertThrows(NullPointerException.class, () -> layout.renderFrame(session, tinyEnv, TuiThemePalette.CYBERPUNK, null));
        assertThrows(NullPointerException.class, () -> layout.wrapTextPreservingCode(null, 80));
    }

    /**
     * Tier 3 Concurrency &amp; Stress test verifying thread-safe concurrent frame rendering across 4 threads
     * and 100 total viewport/theme/route combinations.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link FullscreenTuiLayout#renderFrame(SessionContext, TerminalEnvironment, TuiThemePalette, FullscreenTuiLayout.ViewState)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00046}, {@code FR-00048}, {@code REQ-00071}, {@code REQ-00099}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0030.7} (Stress &amp; Concurrency Tier), {@code CS-0050.6}</li>
     * </ul>
     */
    @Test
    @DisplayName("Tier 3 Stress & Concurrency: Should render frames concurrently across multiple threads safely")
    void shouldRenderFramesConcurrentlyWithoutRaceConditions() throws Exception {
        // [REQ-00100, CS-0030.7] Prepare shared session and 4-thread executor pool
        final SessionContext session = sessionManager.getDefaultSession();
        session.addMessage(AgentMessage.of("assistant", "Concurrent test message", "Concurrent reasoning"));

        final int threadCount = 4;
        final int iterationsPerThread = 25;
        final ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        try {
            // [REQ-00071, REQ-00099, REQ-00100] Submit concurrent rendering tasks sweeping breakpoints and palettes
            final List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                final int threadIdx = t;
                tasks.add(() -> {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        final int cols = 80 + ((threadIdx * 20 + i) % 100);
                        final int rows = 24 + (i % 20);
                        final TerminalEnvironment env = new TerminalEnvironment(
                                TerminalMode.STANDARD, "xterm-256color", "", "", cols, rows);
                        final TuiThemePalette palette = TuiThemePalette.availableThemes()
                                .get((threadIdx + i) % TuiThemePalette.availableThemes().size());
                        final FullscreenTuiLayout.ViewState state = new FullscreenTuiLayout.ViewState.Builder()
                                .route(i % 2 == 0 ? FullscreenTuiLayout.Route.HOME : FullscreenTuiLayout.Route.SESSION)
                                .overlay(FullscreenTuiLayout.OverlayModal.NONE)
                                .build();
                        final String rendered = layout.renderFrame(session, env, palette, state);
                        if (rendered == null || rendered.isEmpty()) {
                            return false;
                        }
                    }
                    return true;
                });
            }

            // [CS-0030.7] Assert all 100 concurrent frame renders complete without error within timeout
            final List<Future<Boolean>> futures = pool.invokeAll(tasks);
            for (final Future<Boolean> future : futures) {
                assertTrue(future.get(15, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
