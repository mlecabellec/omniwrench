package com.omniwrench.tui;

import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolInvocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Responsive, multi-pane fullscreen terminal layout engine inspired by OpenCode,
 * Antigravity CLI, OpenClaw, and Crush.
 *
 * <p>Supports 4 adaptive viewport breakpoints ({@code COMPACT}, {@code STANDARD_SPLIT},
 * {@code WIDE_STUDIO}, {@code ULTRAWIDE_WORKBENCH}), dual routes ({@code HOME} and {@code SESSION}),
 * collapsible reasoning and tool cards, OpenCode-style right telemetry sidebar, and centered
 * modal overlays ({@code COMMAND_PALETTE}, {@code THEME_PICKER}, {@code HELP}).
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00071 (Multi-Pane Terminal HUD), REQ-00084 (Markdown &amp; Code Highlighting)</li>
 *   <li>Requirement: REQ-00088 (Dual Chat Mode Reasoning), REQ-00099 (XTerm-256 Color Palette TUI Visual Theming Engine)</li>
 *   <li>Requirement: REQ-00100 (Dual-Mode Minimal Inline Stream &amp; Full-Screen Resizable TUI Workbench)</li>
 *   <li>Feature: FR-00001, FR-00046, FR-00047, FR-00048, FR-00049</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20261101-016, TSK-20261101-018</li>
 *   <li>ADR: ADR-0005 (Multi-Pane Dashboard), ADR-0013 (Context-Adaptive TUI Layout), ADR-0043 (HUD Telemetry), ADR-0063</li>
 * </ul>
 */
public final class FullscreenTuiLayout {

    /** Minimum supported viewport width in columns per CS-0040.2. */
    public static final int MIN_COLUMNS = 40;
    /** Minimum supported viewport height in rows per CS-0040.2. */
    public static final int MIN_ROWS = 14;
    /** Column threshold for standard 2-pane split layout per CS-0040.2. */
    public static final int SPLIT_THRESHOLD_COLS = 80;
    /** Column threshold for wide OpenCode studio layout per CS-0040.2. */
    public static final int WIDE_THRESHOLD_COLS = 120;
    /** Column threshold for ultrawide 3-pane workbench layout per CS-0040.2. */
    public static final int ULTRAWIDE_THRESHOLD_COLS = 160;
    /** Hard iteration cap for wrapping and line rendering loops per CS-0050.6. */
    public static final int MAX_LAYOUT_ITERATIONS = 10_000;

    /** Width of the right sidebar in standard split mode. */
    private static final int STANDARD_SIDEBAR_WIDTH = 30;
    /** Width of the right sidebar in wide studio mode (OpenCode default ~38-42). */
    private static final int WIDE_SIDEBAR_WIDTH = 38;
    /** Width of the left explorer pane in ultrawide mode. */
    private static final int LEFT_EXPLORER_WIDTH = 26;

    /** Fixed height of top header bar in rows. */
    private static final int HEADER_ROWS = 2;
    /** Fixed height of bottom prompt composer in rows. */
    private static final int COMPOSER_ROWS = 3;
    /** Fixed height of bottom telemetry footer bar in rows. */
    private static final int FOOTER_ROWS = 1;
    /** Minimum height of main body area in rows. */
    private static final int MIN_BODY_ROWS = 6;
    /** Border and padding overhead inside a framed panel (left border + space + space + right border). */
    private static final int PANEL_BORDER_OVERHEAD = 4;
    /** Border overhead for horizontal frame lines (left corner + right corner). */
    private static final int HORIZONTAL_CORNER_OVERHEAD = 2;
    /** Default HTTP server port. */
    private static final int DEFAULT_SERVER_PORT = 8080;
    /** Short session ID prefix length for compact HUD display. */
    private static final int SHORT_ID_LENGTH = 8;
    /** Context progress bar visual width in characters. */
    private static final int CONTEXT_BAR_WIDTH = 16;
    /** Nominal message count representing a full context bar in HUD. */
    private static final int NOMINAL_MAX_MESSAGES = 40;
    /** Modal overlay width for command palette and help dialogs. */
    private static final int MODAL_MAX_WIDTH = 68;
    /** Minimum padding around modal overlays. */
    private static final int MODAL_MARGIN = 6;
    /** Divisor for centering calculations. */
    private static final int CENTER_DIVISOR = 2;

    /**
     * Responsive viewport breakpoint classification (ADR-0013).
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     */
    public enum ResponsiveBreakpoint {
        /** Narrow single-column viewport (&lt; 80 cols). */
        COMPACT,
        /** Dual-pane split viewport (80..119 cols). */
        STANDARD_SPLIT,
        /** Wide studio viewport with rich sidebar (120..159 cols). */
        WIDE_STUDIO,
        /** Ultra-wide 3-pane workbench (&gt;= 160 cols). */
        ULTRAWIDE_WORKBENCH;

        /**
         * Classifies viewport column width into a responsive breakpoint.
         *
         * <p>Traceability: REQ-00071, REQ-00100, FR-00048, CS-0060.3
         *
         * @param columns terminal column count
         * @return corresponding ResponsiveBreakpoint
         */
        public static ResponsiveBreakpoint fromColumns(final int columns) {
            // [REQ-00071, REQ-00100, FR-00048, CS-0060.3] Classify columns across ADR-0013 thresholds
            if (columns >= ULTRAWIDE_THRESHOLD_COLS) {
                return ULTRAWIDE_WORKBENCH;
            } else if (columns >= WIDE_THRESHOLD_COLS) {
                return WIDE_STUDIO;
            } else if (columns >= SPLIT_THRESHOLD_COLS) {
                return STANDARD_SPLIT;
            } else {
                return COMPACT;
            }
        }
    }

    /**
     * Primary navigation route in the fullscreen TUI (OpenCode pattern).
     *
     * <p>Traceability: REQ-00100, FR-00048
     */
    public enum Route {
        /** Centered welcome and quick-start route. */
        HOME,
        /** Active conversation, reasoning, and tool transcript route. */
        SESSION
    }

    /**
     * Floating modal overlay state.
     *
     * <p>Traceability: REQ-00100, FR-00049
     */
    public enum OverlayModal {
        /** No active modal overlay. */
        NONE,
        /** Categorized fuzzy command palette overlay (/palette). */
        COMMAND_PALETTE,
        /** Live color theme picker overlay (/themes). */
        THEME_PICKER,
        /** Keyboard and slash command help overlay (/help). */
        HELP
    }

    /**
     * Constructs a FullscreenTuiLayout engine.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     */
    public FullscreenTuiLayout() {
    }

    /**
     * Renders a complete, resizable fullscreen frame for the given session, terminal environment,
     * theme palette, and view state.
     *
     * <p>Traceability: REQ-00071, REQ-00084, REQ-00099, REQ-00100, FR-00046, FR-00048, FR-00049, CS-0050.1
     *
     * @param session active session context, must not be null
     * @param env terminal environment metadata and dimensions, must not be null
     * @param palette active semantic color palette, must not be null
     * @param state current fullscreen view state, must not be null
     * @return complete multi-line ANSI string representing the fullscreen viewport
     */
    public String renderFrame(final SessionContext session,
                              final TerminalEnvironment env,
                              final TuiThemePalette palette,
                              final ViewState state) {
        // [REQ-00100, FR-00048, CS-0050.1] Validate non-null inputs
        final SessionContext nonNullSession = Objects.requireNonNull(session, "session must not be null");
        final TerminalEnvironment nonNullEnv = Objects.requireNonNull(env, "env must not be null");
        final TuiThemePalette nonNullPalette = Objects.requireNonNull(palette, "palette must not be null");
        final ViewState nonNullState = Objects.requireNonNull(state, "state must not be null");

        // [REQ-00071, REQ-00100, FR-00048] Compute clamped viewport dimensions and responsive breakpoint
        final int totalCols = Math.max(MIN_COLUMNS, nonNullEnv.getColumns());
        final int totalRows = Math.max(MIN_ROWS, nonNullEnv.getRows());
        final int bodyRows = Math.max(MIN_BODY_ROWS, totalRows - HEADER_ROWS - COMPOSER_ROWS - FOOTER_ROWS);
        final ResponsiveBreakpoint breakpoint = ResponsiveBreakpoint.fromColumns(totalCols);

        // [REQ-00071, REQ-00100, FR-00048] Determine sidebar visibility and pane widths based on policy and breakpoint
        final boolean showRightSidebar = isRightSidebarVisible(breakpoint, nonNullState.getSidebarPolicy());
        final boolean showLeftExplorer = isLeftExplorerVisible(breakpoint, nonNullState.getSidebarPolicy());
        final int sidebarWidthForMode = breakpoint == ResponsiveBreakpoint.STANDARD_SPLIT
                ? STANDARD_SIDEBAR_WIDTH
                : WIDE_SIDEBAR_WIDTH;
        final int rightWidth = showRightSidebar ? sidebarWidthForMode : 0;
        final int leftWidth = showLeftExplorer ? LEFT_EXPLORER_WIDTH : 0;
        final int mainWidth = Math.max(MIN_COLUMNS - STANDARD_SIDEBAR_WIDTH, totalCols - leftWidth - rightWidth);

        // [REQ-00100, FR-00048, CS-0060.3] Render main center canvas (HOME route or SESSION route)
        final List<String> mainCanvas;
        if (nonNullState.getRoute() == Route.HOME) {
            mainCanvas = renderHomeCanvas(mainWidth, bodyRows, nonNullEnv, nonNullPalette, nonNullState);
        } else {
            mainCanvas = renderSessionCanvas(
                    nonNullSession, mainWidth, bodyRows, nonNullPalette, nonNullState);
        }

        // [REQ-00071, REQ-00100, FR-00048] Stitch optional left explorer and right telemetry sidebar horizontally
        List<String> combinedBody = mainCanvas;
        if (showLeftExplorer) {
            final List<String> leftPane = renderLeftExplorerPane(
                    nonNullSession, leftWidth, bodyRows, nonNullPalette, nonNullState);
            combinedBody = stitchColumns(leftPane, combinedBody, bodyRows);
        }
        if (showRightSidebar) {
            final List<String> rightPane = renderRightSidebarPane(
                    nonNullSession, rightWidth, bodyRows, nonNullEnv, nonNullPalette, nonNullState);
            combinedBody = stitchColumns(combinedBody, rightPane, bodyRows);
        }

        // [REQ-00100, FR-00049] Composite floating modal overlay if active
        if (nonNullState.getOverlay() != OverlayModal.NONE) {
            combinedBody = applyModalOverlay(
                    combinedBody, totalCols, bodyRows, nonNullPalette, nonNullState.getOverlay());
        }

        // [REQ-00071, REQ-00100, FR-00048] Assemble final fullscreen frame: Header + Body + Composer + Footer
        final StringBuilder frame = new StringBuilder();
        frame.append(renderHeaderBar(totalCols, breakpoint, nonNullEnv, nonNullPalette, nonNullState)).append("\n");
        for (int i = 0; i < combinedBody.size(); i++) {
            frame.append(combinedBody.get(i)).append("\n");
        }
        frame.append(renderComposerBox(totalCols, nonNullPalette, nonNullState)).append("\n");
        frame.append(renderFooterBar(nonNullSession, totalCols, breakpoint, nonNullEnv, nonNullPalette, nonNullState));
        return frame.toString();
    }

    /**
     * Evaluates whether the right telemetry sidebar should be visible for the given breakpoint and policy.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048, CS-0060.3
     *
     * @param breakpoint active viewport breakpoint
     * @param policy sidebar policy (`auto`, `show`, `hide`)
     * @return true if right sidebar should be rendered
     */
    private boolean isRightSidebarVisible(final ResponsiveBreakpoint breakpoint, final String policy) {
        // [REQ-00071, REQ-00100, FR-00048, CS-0060.3] Evaluate hide/show/auto policy against breakpoint
        final String norm = policy.trim().toLowerCase(Locale.ROOT);
        if ("hide".equals(norm) || "off".equals(norm)) {
            return false;
        } else if ("show".equals(norm) || "on".equals(norm)) {
            return breakpoint != ResponsiveBreakpoint.COMPACT;
        } else {
            return breakpoint == ResponsiveBreakpoint.STANDARD_SPLIT
                    || breakpoint == ResponsiveBreakpoint.WIDE_STUDIO
                    || breakpoint == ResponsiveBreakpoint.ULTRAWIDE_WORKBENCH;
        }
    }

    /**
     * Evaluates whether the left explorer pane should be visible in ultrawide mode.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048, CS-0060.3
     *
     * @param breakpoint active viewport breakpoint
     * @param policy sidebar policy (`auto`, `show`, `hide`)
     * @return true if left explorer should be rendered
     */
    private boolean isLeftExplorerVisible(final ResponsiveBreakpoint breakpoint, final String policy) {
        // [REQ-00071, REQ-00100, FR-00048, CS-0060.3] Render left explorer only in ULTRAWIDE_WORKBENCH when not hidden
        final String norm = policy.trim().toLowerCase(Locale.ROOT);
        if ("hide".equals(norm) || "off".equals(norm)) {
            return false;
        } else {
            return breakpoint == ResponsiveBreakpoint.ULTRAWIDE_WORKBENCH;
        }
    }

    /**
     * Renders the 2-row top telemetry header bar (ADR-0043).
     *
     * <p>Traceability: REQ-00071, REQ-00099, REQ-00100, FR-00046, FR-00048
     *
     * @param totalCols viewport column width
     * @param breakpoint active responsive breakpoint
     * @param env terminal environment metadata
     * @param palette active color palette
     * @param state active view state
     * @return 2-line formatted ANSI header string
     */
    private String renderHeaderBar(final int totalCols,
                                   final ResponsiveBreakpoint breakpoint,
                                   final TerminalEnvironment env,
                                   final TuiThemePalette palette,
                                   final ViewState state) {
        // [REQ-00071, REQ-00100, FR-00048] Format top telemetry badges (ADR-0043)
        final String envBadge = formatEnvBadge(env);
        final String routeBadge = "[" + state.getRoute().name() + "]";
        final String sizeBadge = "[" + breakpoint.name() + " " + env.getColumns() + "x" + env.getRows() + "]";
        final String themeBadge = "[THEME:" + palette.getName().toUpperCase(Locale.ROOT) + "]";
        final String thinkBadge = "[THINK:" + (state.isShowThinking()
                ? state.getThinkingEffort().toUpperCase(Locale.ROOT) : "HIDDEN") + "]";

        // [REQ-00100, FR-00048] Adapt header prefix length for COMPACT viewports so badges remain visible
        final String brandPrefix = breakpoint == ResponsiveBreakpoint.COMPACT
                ? " ⚡ OMNIWRENCH "
                : " ⚡ OMNIWRENCH :: FULLSCREEN WORKBENCH  ";
        final String rawHeader = brandPrefix
                + routeBadge + " " + sizeBadge + " " + envBadge + " " + themeBadge + " " + thinkBadge;
        final String fittedHeader = fitPlainLine(rawHeader, totalCols);
        final String divider = repeatChar('═', totalCols);

        // [REQ-00099, FR-00046] Apply semantic header background and primary foreground palette tokens
        return palette.getHeaderBg() + palette.getPrimaryFg() + TuiThemePalette.BOLD
                + fittedHeader + TuiThemePalette.RESET + "\n"
                + palette.getMutedFg() + divider + TuiThemePalette.RESET;
    }

    /**
     * Formats the environment badge (`[TMUX]`, `[SSH]`, or `[LOCAL]`).
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00048, CS-0060.3
     *
     * @param env terminal environment metadata
     * @return badge string
     */
    private String formatEnvBadge(final TerminalEnvironment env) {
        // [REQ-00001, REQ-00100, CS-0060.3] Select badge for TMUX, SSH, or LOCAL terminal mode
        if (env.getMode() == TerminalMode.TMUX) {
            final String pane = env.getTmuxPane();
            return pane.isEmpty() ? "[TMUX]" : "[TMUX:" + pane + "]";
        } else if (env.getMode() == TerminalMode.SSH) {
            return "[SSH]";
        } else {
            return "[LOCAL]";
        }
    }

    /**
     * Renders the centered Home welcome canvas inspired by OpenCode and Antigravity CLI.
     *
     * <p>Traceability: REQ-00100, FR-00048
     *
     * @param width canvas width in columns
     * @param height canvas height in rows
     * @param env terminal environment metadata
     * @param palette active color palette
     * @param state active view state
     * @return list of framed rows
     */
    private List<String> renderHomeCanvas(final int width,
                                          final int height,
                                          final TerminalEnvironment env,
                                          final TuiThemePalette palette,
                                          final ViewState state) {
        // [REQ-00100, FR-00048] Build OpenCode & Antigravity CLI inspired centered Home screen banner
        final int innerWidth = Math.max(1, width - PANEL_BORDER_OVERHEAD);
        final List<String> contentLines = new ArrayList<>();
        contentLines.add("");
        contentLines.add(centerPlain("╔══════════════════════════════════════════════════╗", innerWidth));
        contentLines.add(centerPlain("║   🛠️   O M N I W R E N C H   W O R K B E N C H   ║", innerWidth));
        contentLines.add(centerPlain("║   Autonomous Dual CLI / TUI / Web Agent Engine   ║", innerWidth));
        contentLines.add(centerPlain("╚══════════════════════════════════════════════════╝", innerWidth));
        // [REQ-00100, FR-00048] Append active goal, environment, and quick-start command card
        contentLines.add("");
        contentLines.add(centerPlain("Active Goal: " + state.getCurrentGoal(), innerWidth));
        contentLines.add(centerPlain("Environment: " + env.getMode() + " (" + env.getColumns() + "x" + env.getRows()
                + ") | Theme: " + palette.getName(), innerWidth));
        contentLines.add("");
        contentLines.add(centerPlain("╭── Quick Start Commands & Shortcuts ──────────────╮", innerWidth));
        contentLines.add(centerPlain("│  /palette      Open Command Palette Modal        │", innerWidth));
        contentLines.add(centerPlain("│  /session      Switch to Session Transcript View │", innerWidth));
        contentLines.add(centerPlain("│  /sidebar      Toggle Context & Telemetry Drawer │", innerWidth));
        contentLines.add(centerPlain("│  /theme [name] Switch Color Palette Live         │", innerWidth));
        contentLines.add(centerPlain("│  /btw <query>  Ephemeral Side-Channel Question   │", innerWidth));
        contentLines.add(centerPlain("│  /run <cmd>    Execute Workspace Shell Command   │", innerWidth));
        contentLines.add(centerPlain("│  /resize <c> <r> Dynamically Reflow Viewport     │", innerWidth));
        contentLines.add(centerPlain("╰──────────────────────────────────────────────────╯", innerWidth));

        // [REQ-00100, FR-00048] Append transient status notice if present
        if (!state.getStatusNotice().isEmpty()) {
            contentLines.add("");
            contentLines.add(centerPlain("Notice: " + state.getStatusNotice(), innerWidth));
        }

        return framePanel("HOME :: WELCOME", contentLines, width, height, palette.getPrimaryFg(), palette.getTextFg());
    }

    /**
     * Renders the scrollable Session transcript canvas with collapsible thinking and tool cards.
     *
     * <p>Traceability: REQ-00071, REQ-00084, REQ-00088, REQ-00100, FR-00035, FR-00048, CS-0060.3
     *
     * @param session active session context
     * @param width canvas width in columns
     * @param height canvas height in rows
     * @param palette active color palette
     * @param state active view state
     * @return list of framed rows
     */
    private List<String> renderSessionCanvas(final SessionContext session,
                                             final int width,
                                             final int height,
                                             final TuiThemePalette palette,
                                             final ViewState state) {
        // [REQ-00071, REQ-00100, FR-00048, CS-0060.3] Render scrollable conversation, reasoning, and tool transcript
        final int innerWidth = Math.max(1, width - PANEL_BORDER_OVERHEAD);
        final List<String> transcriptLines = new ArrayList<>();
        final List<AgentMessage> messages = session.getMessages();

        if (messages.isEmpty()) {
            transcriptLines.add("No messages in session yet. Type a prompt below or /home for welcome view.");
        } else {
            appendMessagesToTranscript(messages, innerWidth, state, transcriptLines);
        }

        // [REQ-00100, FR-00048] Append optional inline /btw side-channel card (OpenClaw BtwInlineMessage pattern)
        if (!state.getBtwQuestion().isEmpty()) {
            transcriptLines.add("");
            transcriptLines.add("╭── [BTW SIDE-CHANNEL: " + state.getBtwQuestion() + "]");
            for (final String wrapped : wrapTextPreservingCode(state.getBtwAnswer(), innerWidth - HORIZONTAL_CORNER_OVERHEAD)) {
                transcriptLines.add("│ " + wrapped);
            }
            transcriptLines.add("╰── (type /btw clear to dismiss)");
        }

        // [REQ-00100, FR-00048] Append transient status notice and slice transcript for scroll offset
        if (!state.getStatusNotice().isEmpty()) {
            transcriptLines.add("");
            transcriptLines.add("» " + state.getStatusNotice());
        }

        final int contentRows = Math.max(1, height - HORIZONTAL_CORNER_OVERHEAD);
        final List<String> visibleSlice = sliceForScroll(transcriptLines, contentRows, state.getScrollOffset());
        final String title = "SESSION TRANSCRIPT [msgs: " + messages.size()
                + " | scroll: " + state.getScrollOffset() + "]";
        return framePanel(title, visibleSlice, width, height, palette.getPrimaryFg(), palette.getTextFg());
    }

    /**
     * Appends formatted user and agent messages (including demultiplexed reasoning and tool cards) to the transcript.
     *
     * <p>Traceability: REQ-00071, REQ-00084, REQ-00088, REQ-00100, FR-00035, FR-00048, CS-0050.6, CS-0060.3
     *
     * @param messages session messages list
     * @param innerWidth available text width inside panel
     * @param state active view state
     * @param transcriptLines output lines accumulator
     */
    private void appendMessagesToTranscript(final List<AgentMessage> messages,
                                            final int innerWidth,
                                            final ViewState state,
                                            final List<String> transcriptLines) {
        // [REQ-00071, REQ-00100, CS-0050.6] Iterate through session messages with bounded loop check
        int msgCount = 0;
        for (final AgentMessage msg : messages) {
            if (msgCount >= MAX_LAYOUT_ITERATIONS) {
                throw new IllegalStateException("Exceeded MAX_LAYOUT_ITERATIONS while rendering transcript");
            }
            msgCount++;
            final boolean isUser = "user".equalsIgnoreCase(msg.getRole());
            final String roleLabel = isUser ? "● USER" : "◆ OMNIWRENCH AGENT";
            transcriptLines.add("╭─ " + roleLabel);

            // [REQ-00088, FR-00035, CS-0060.3] Render demultiplexed reasoning stream if present and enabled
            if (msg.hasThinking()) {
                if (state.isShowThinking()) {
                    transcriptLines.add("│  ┌─ ⚡ REASONING STREAM [" + state.getThinkingEffort() + "]");
                    for (final String tLine : wrapTextPreservingCode(
                            msg.getThinking(), Math.max(1, innerWidth - PANEL_BORDER_OVERHEAD))) {
                        transcriptLines.add("│  │ " + tLine);
                    }
                    transcriptLines.add("│  └─");
                } else {
                    transcriptLines.add("│  [⚡ Reasoning stream collapsed — type /thinking toggle to expand]");
                }
            }

            // [REQ-00071, REQ-00084, FR-00048] Render tool execution badges and wrapped message content
            appendToolInvocations(msg.getToolInvocations(), innerWidth, state.isShowTools(), transcriptLines);

            for (final String cLine : wrapTextPreservingCode(
                    msg.getContent(), Math.max(1, innerWidth - HORIZONTAL_CORNER_OVERHEAD))) {
                transcriptLines.add("│ " + cLine);
            }
            transcriptLines.add("╰─");
        }
    }

    /**
     * Appends structured tool invocation cards to the transcript.
     *
     * <p>Traceability: REQ-00015, REQ-00071, REQ-00100, FR-00002, FR-00048
     *
     * @param tools tool invocations attached to message
     * @param innerWidth available text width
     * @param showTools whether tool outputs are expanded
     * @param transcriptLines output lines accumulator
     */
    private void appendToolInvocations(final List<ToolInvocation> tools,
                                       final int innerWidth,
                                       final boolean showTools,
                                       final List<String> transcriptLines) {
        // [REQ-00071, FR-00048] Skip when message has no tool invocations
        if (tools.isEmpty()) {
            return;
        }
        // [REQ-00015, REQ-00071, FR-00048] Render tool badge and optional expanded output lines
        for (final ToolInvocation inv : tools) {
            final String statusLabel = inv.isSuccess() ? "OK" : "ERR";
            transcriptLines.add("│  ┌─ 🔧 TOOL: " + inv.getToolName() + " [" + statusLabel + "] ("
                    + inv.getCallId() + ")");
            if (showTools && !inv.getOutput().isEmpty()) {
                for (final String oLine : wrapTextPreservingCode(
                        inv.getOutput(), Math.max(1, innerWidth - PANEL_BORDER_OVERHEAD))) {
                    transcriptLines.add("│  │ " + oLine);
                }
            }
            transcriptLines.add("│  └─");
        }
    }

    /**
     * Renders the Ultrawide left Session &amp; Workspace Explorer pane (`>= 160` columns).
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     *
     * @param session active session context
     * @param width pane width in columns
     * @param height pane height in rows
     * @param palette active color palette
     * @param state active view state
     * @return list of framed rows
     */
    private List<String> renderLeftExplorerPane(final SessionContext session,
                                                final int width,
                                                final int height,
                                                final TuiThemePalette palette,
                                                final ViewState state) {
        // [REQ-00071, REQ-00100, FR-00048] Render Ultrawide Left Session & Workspace Explorer metadata
        final int innerWidth = Math.max(1, width - PANEL_BORDER_OVERHEAD);
        final List<String> lines = new ArrayList<>();
        final String shortId = shortenSessionId(session.getSessionId());
        lines.add("• Active Session:");
        lines.add("  " + shortId);
        lines.add("  Msgs: " + session.getMessages().size());
        lines.add("");
        lines.add("• Workspace Root:");
        for (final String wLine : wrapTextPreservingCode(session.getWorkspaceRoot(), innerWidth)) {
            lines.add("  " + wLine);
        }
        // [REQ-00100, FR-00048] Append navigation shortcuts and active runtime mode
        lines.add("");
        lines.add("• Navigation:");
        lines.add("  /home     Home View");
        lines.add("  /session  Chat View");
        lines.add("  /palette  Commands");
        lines.add("  /sidebar  Drawer");
        lines.add("  /themes   Themes");
        lines.add("  /minimal  Inline UI");
        lines.add("");
        lines.add("• Active Mode:");
        lines.add("  " + state.getRuntimeMode().toUpperCase(Locale.ROOT));
        return framePanel("EXPLORER", lines, width, height, palette.getSecondaryFg(), palette.getMutedFg());
    }

    /**
     * Renders the OpenCode-inspired Right Context, Goal &amp; Telemetry Sidebar.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048, CS-0060.3
     *
     * @param session active session context
     * @param width sidebar width in columns
     * @param height sidebar height in rows
     * @param env terminal environment metadata
     * @param palette active color palette
     * @param state active view state
     * @return list of framed rows
     */
    private List<String> renderRightSidebarPane(final SessionContext session,
                                                final int width,
                                                final int height,
                                                final TerminalEnvironment env,
                                                final TuiThemePalette palette,
                                                final ViewState state) {
        // [REQ-00071, REQ-00100, FR-00048] Render session ID, runtime mode, viewport, and context bar
        final int innerWidth = Math.max(1, width - PANEL_BORDER_OVERHEAD);
        final List<String> lines = new ArrayList<>();
        final String shortId = shortenSessionId(session.getSessionId());

        lines.add("◆ SESSION & CONTEXT");
        lines.add("  ID:   " + shortId);
        lines.add("  Mode: " + state.getRuntimeMode().toUpperCase(Locale.ROOT) + " (:" + state.getServerPort() + ")");
        lines.add("  Term: " + env.getMode() + " " + env.getColumns() + "x" + env.getRows());
        lines.add("  Context Usage:");
        lines.add("  " + renderContextBar(session.getMessages().size()));
        lines.add("");

        // [REQ-00100, FR-00048, CS-0060.3] Render active goal and registered tools list
        lines.add("◆ ACTIVE GOAL");
        for (final String gLine : wrapTextPreservingCode(state.getCurrentGoal(), innerWidth)) {
            lines.add("  " + gLine);
        }
        lines.add("");

        lines.add("◆ TOOLS (" + state.getToolsCount() + " READY)");
        if (state.getToolNames().isEmpty()) {
            lines.add("  • (none registered)");
        } else {
            for (final String toolName : state.getToolNames()) {
                lines.add("  • " + toolName);
            }
        }
        lines.add("");

        // [REQ-00100, FR-00049] Render quick interactive shortcut hints
        lines.add("◆ SHORTCUTS");
        lines.add("  /palette  Command Menu");
        lines.add("  /sidebar  Hide/Show");
        lines.add("  /thinking Toggle Think");
        lines.add("  /theme    Switch Theme");
        lines.add("  /copy     OSC52 Copy");

        return framePanel("CONTEXT & TELEMETRY", lines, width, height, palette.getSecondaryFg(), palette.getTextFg());
    }

    /**
     * Renders an ASCII progress bar representing session message count relative to nominal capacity.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     *
     * @param messageCount number of messages in session
     * @return formatted progress bar string
     */
    private String renderContextBar(final int messageCount) {
        // [REQ-00071, FR-00048] Compute filled and empty segments for context utilization bar
        final int filled = Math.min(CONTEXT_BAR_WIDTH, (messageCount * CONTEXT_BAR_WIDTH) / NOMINAL_MAX_MESSAGES);
        final int empty = CONTEXT_BAR_WIDTH - filled;
        return "[" + repeatChar('█', filled) + repeatChar('░', empty) + "] " + messageCount + " turns";
    }

    /**
     * Renders the 3-row bottom prompt composer box.
     *
     * <p>Traceability: REQ-00071, REQ-00099, REQ-00100, FR-00046, FR-00048
     *
     * @param totalCols viewport column width
     * @param palette active color palette
     * @param state active view state
     * @return 3-line formatted ANSI composer box string
     */
    private String renderComposerBox(final int totalCols,
                                     final TuiThemePalette palette,
                                     final ViewState state) {
        // [REQ-00071, REQ-00100, FR-00048] Render 3-row bottom prompt composer box with shortcut hints
        final int innerWidth = Math.max(1, totalCols - HORIZONTAL_CORNER_OVERHEAD);
        final String topLabel = "─ ❯ PROMPT COMPOSER [Enter: submit | /palette: commands | /sidebar: toggle | /help] ";
        final String topBorder = "╭" + fitPlainLineWithFill(topLabel, innerWidth, '─') + "╮";
        final String promptHint = " omniwrench (" + state.getRoute().name().toLowerCase(Locale.ROOT)
                + ") ❯ Type a prompt or /command...";
        final String middleRow = "│" + fitPlainLine(promptHint, innerWidth) + "│";
        final String bottomBorder = "╰" + repeatChar('─', innerWidth) + "╯";

        return palette.getAccentFg() + topBorder + TuiThemePalette.RESET + "\n"
                + palette.getTextFg() + middleRow + TuiThemePalette.RESET + "\n"
                + palette.getAccentFg() + bottomBorder + TuiThemePalette.RESET;
    }

    /**
     * Renders the single-line authoritative bottom telemetry footer bar.
     *
     * <p>Traceability: REQ-00071, REQ-00099, REQ-00100, FR-00046, FR-00048
     *
     * @param session active session context
     * @param totalCols viewport column width
     * @param breakpoint active responsive breakpoint
     * @param env terminal environment metadata
     * @param palette active color palette
     * @param state active view state
     * @return single-line formatted ANSI footer string
     */
    private String renderFooterBar(final SessionContext session,
                                   final int totalCols,
                                   final ResponsiveBreakpoint breakpoint,
                                   final TerminalEnvironment env,
                                   final TuiThemePalette palette,
                                   final ViewState state) {
        // [REQ-00071, REQ-00100, FR-00048] Render OpenCode & OpenClaw inspired single-line authoritative footer
        final String footerText = " 📁 " + session.getWorkspaceRoot()
                + " │ • " + state.getToolsCount() + " Tools"
                + " │ ⚙ Think:" + state.getThinkingEffort()
                + " │ 🖥 " + env.getColumns() + "x" + env.getRows() + " (" + breakpoint.name() + ")"
                + " │ /palette  /theme  /minimal  exit ";
        return palette.getFooterBg() + palette.getMutedFg()
                + fitPlainLine(footerText, totalCols) + TuiThemePalette.RESET;
    }

    /**
     * Composites a centered floating modal dialog over the body rows.
     *
     * <p>Traceability: REQ-00100, FR-00049
     *
     * @param baseRows underlying body rows
     * @param totalCols viewport column width
     * @param bodyRows body height in rows
     * @param palette active color palette
     * @param modal active overlay modal type
     * @return body rows with centered modal composited
     */
    private List<String> applyModalOverlay(final List<String> baseRows,
                                           final int totalCols,
                                           final int bodyRows,
                                           final TuiThemePalette palette,
                                           final OverlayModal modal) {
        // [REQ-00100, FR-00049] Build framed modal lines based on active overlay type
        final int modalWidth = Math.min(MODAL_MAX_WIDTH, Math.max(MIN_COLUMNS - MODAL_MARGIN, totalCols - MODAL_MARGIN));
        final List<String> modalContent = buildModalContent(modal);
        final int modalHeight = Math.min(bodyRows, modalContent.size() + HORIZONTAL_CORNER_OVERHEAD);
        final List<String> framedModal = framePanel(
                "MODAL :: " + modal.name(),
                modalContent,
                modalWidth,
                modalHeight,
                palette.getAccentFg(),
                palette.getTextFg()
        );

        // [REQ-00100, FR-00049] Composite modal vertically centered in bodyRows
        final List<String> result = new ArrayList<>(baseRows);
        final int startRow = Math.max(0, (bodyRows - modalHeight) / CENTER_DIVISOR);
        for (int i = 0; i < framedModal.size() && (startRow + i) < result.size(); i++) {
            result.set(startRow + i, framedModal.get(i));
        }
        return result;
    }

    /**
     * Builds the interior text lines for the requested {@link OverlayModal}.
     *
     * <p>Traceability: REQ-00099, REQ-00100, FR-00046, FR-00049, CS-0060.3
     *
     * @param modal overlay modal type
     * @return list of modal content lines
     */
    private List<String> buildModalContent(final OverlayModal modal) {
        // [REQ-00100, FR-00049, CS-0060.3] Populate categorized command palette, theme picker, or help modal
        final List<String> lines = new ArrayList<>();
        if (modal == OverlayModal.COMMAND_PALETTE) {
            lines.add("[Suggested]");
            lines.add("  /session          Switch to Session Transcript View");
            lines.add("  /home             Switch to Centered Home View");
            lines.add("  /sidebar toggle   Toggle Right Telemetry Sidebar");
            lines.add("[Agent & Reasoning]");
            lines.add("  /thinking <lvl>   Set effort (low|medium|high|max|toggle)");
            lines.add("  /btw <question>   Ask ephemeral side-channel question");
            lines.add("  /goal <text>      Update active session goal");
            lines.add("  /model <cmd>      Manage local GGUF models (list|search|pull)");
            lines.add("[View & System]");
            lines.add("  /theme <name>     Switch theme (cyberpunk|opencode|dracula|nord)");
            lines.add("  /resize <c> <r>   Resize fullscreen viewport dimensions");
            lines.add("  /minimal          Switch to Minimal Inline Stream TUI");
            lines.add("  /close            Dismiss modal overlay");
        } else if (modal == OverlayModal.THEME_PICKER) {
            // [REQ-00099, FR-00046] List all registered XTerm-256 semantic themes
            lines.add("Available XTerm-256 & ANSI Semantic Themes:");
            for (final TuiThemePalette t : TuiThemePalette.availableThemes()) {
                lines.add("  • /theme " + t.getName());
            }
            lines.add("Type '/theme <name>' to apply or '/close' to dismiss.");
        } else {
            lines.add("Fullscreen Resizable TUI Controls & Commands:");
            lines.add("  /palette (/p)     Open Command Palette Overlay");
            lines.add("  /themes           Open Theme Picker Overlay");
            lines.add("  /sidebar [mode]   auto | show | hide | toggle");
            lines.add("  /scroll <dir>     up | down | top | bottom");
            lines.add("  /resize <W> <H>   Reflow fullscreen layout to WxH");
            lines.add("  /copy             Copy last response via OSC 52");
            lines.add("  /close            Close active overlay modal");
            lines.add("  exit / quit       Exit Fullscreen TUI");
        }
        return lines;
    }

    /**
     * Frames a list of content lines inside a Unicode rounded box (`╭─...─╮`, `│ ... │`, `╰─...─╯`).
     *
     * <p>Traceability: REQ-00071, REQ-00099, REQ-00100, FR-00046, FR-00048
     *
     * @param title panel title
     * @param contentLines interior content lines
     * @param width total panel width in columns
     * @param height total panel height in rows
     * @param borderColor ANSI border color sequence
     * @param textColor ANSI interior text color sequence
     * @return list of framed rows
     */
    private List<String> framePanel(final String title,
                                    final List<String> contentLines,
                                    final int width,
                                    final int height,
                                    final String borderColor,
                                    final String textColor) {
        // [REQ-00071, REQ-00100, FR-00048] Compute interior dimensions and top border line
        final int innerLineWidth = Math.max(1, width - HORIZONTAL_CORNER_OVERHEAD);
        final int innerTextWidth = Math.max(1, width - PANEL_BORDER_OVERHEAD);
        final int bodyHeight = Math.max(1, height - HORIZONTAL_CORNER_OVERHEAD);

        final List<String> rows = new ArrayList<>(height);
        final String headerFill = fitPlainLineWithFill("─ " + title + " ", innerLineWidth, '─');
        rows.add(borderColor + "╭" + headerFill + "╮" + TuiThemePalette.RESET);

        // [REQ-00071, REQ-00100, FR-00048] Pad or truncate each interior row to exact panel width
        for (int i = 0; i < bodyHeight; i++) {
            final String raw = i < contentLines.size() ? contentLines.get(i) : "";
            final String fitted = fitPlainLine(raw, innerTextWidth);
            rows.add(borderColor + "│ " + TuiThemePalette.RESET
                    + textColor + fitted + TuiThemePalette.RESET
                    + borderColor + " │" + TuiThemePalette.RESET);
        }

        rows.add(borderColor + "╰" + repeatChar('─', innerLineWidth) + "╯" + TuiThemePalette.RESET);
        return rows;
    }

    /**
     * Horizontally stitches two equal-height column panes row by row.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     *
     * @param leftRows left pane rows
     * @param rightRows right pane rows
     * @param targetHeight expected row count
     * @return stitched rows
     */
    private List<String> stitchColumns(final List<String> leftRows,
                                       final List<String> rightRows,
                                       final int targetHeight) {
        // [REQ-00071, REQ-00100, FR-00048] Concatenate left and right pane rows horizontally
        final List<String> stitched = new ArrayList<>(targetHeight);
        final int total = Math.max(leftRows.size(), rightRows.size());
        for (int i = 0; i < total; i++) {
            final String left = i < leftRows.size() ? leftRows.get(i) : "";
            final String right = i < rightRows.size() ? rightRows.get(i) : "";
            stitched.add(left + right);
        }
        return stitched;
    }

    /**
     * Extracts the visible vertical window of transcript lines for the current scroll offset.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     *
     * @param allLines full transcript lines
     * @param visibleCount number of visible rows in panel
     * @param scrollOffset scroll offset from bottom
     * @return visible slice of lines
     */
    private List<String> sliceForScroll(final List<String> allLines,
                                        final int visibleCount,
                                        final int scrollOffset) {
        // [REQ-00071, REQ-00100, FR-00048] Return full list if smaller than viewport, otherwise clamp scroll window
        if (allLines.size() <= visibleCount) {
            return allLines;
        }
        final int maxOffset = Math.max(0, allLines.size() - visibleCount);
        final int clampedOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
        final int endIndex = allLines.size() - clampedOffset;
        final int startIndex = Math.max(0, endIndex - visibleCount);
        return allLines.subList(startIndex, endIndex);
    }

    /**
     * Wraps multi-line text to the target column width while preserving fenced code block lines intact.
     *
     * <p>Traceability: REQ-00084, REQ-00100, FR-00048, CS-0050.1, CS-0060.3
     *
     * @param text input text, must not be null
     * @param maxWidth target column width
     * @return wrapped lines list
     */
    public List<String> wrapTextPreservingCode(final String text, final int maxWidth) {
        // [REQ-00084, REQ-00100, FR-00048, CS-0050.1] Validate non-null text and initialize fence state
        final String nonNullText = Objects.requireNonNull(text, "text must not be null");
        final int safeWidth = Math.max(1, maxWidth);
        final List<String> result = new ArrayList<>();
        final String[] rawLines = nonNullText.split("\n", -1);
        boolean inCodeFence = false;

        // [REQ-00084, FR-00048, CS-0060.3] Preserve fenced code blocks while word-wrapping prose lines
        for (final String rawLine : rawLines) {
            final String trimmed = rawLine.trim();
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inCodeFence = !inCodeFence;
                result.add(fitPlainLine(rawLine, safeWidth));
            } else if (inCodeFence || rawLine.length() <= safeWidth) {
                result.add(rawLine.length() <= safeWidth ? rawLine : rawLine.substring(0, safeWidth));
            } else {
                wrapSingleProseLine(rawLine, safeWidth, result);
            }
        }
        return result;
    }

    /**
     * Word-wraps a single prose line into segments of at most {@code width} characters.
     *
     * <p>Traceability: REQ-00084, REQ-00100, FR-00048, CS-0050.6, CS-0060.3
     *
     * @param line single line of prose text
     * @param width maximum column width
     * @param target output list receiving wrapped lines
     */
    private void wrapSingleProseLine(final String line, final int width, final List<String> target) {
        // [REQ-00084, FR-00048, CS-0050.6] Word-wrap prose line with bounded loop guard
        int pos = 0;
        int iterations = 0;
        while (pos < line.length() && iterations < MAX_LAYOUT_ITERATIONS) {
            iterations++;
            final int remaining = line.length() - pos;
            if (remaining <= width) {
                target.add(line.substring(pos));
                break;
            }
            // [REQ-00084, CS-0060.3] Break at last whitespace within width, or hard-split long token
            final int breakIdx = line.lastIndexOf(' ', pos + width);
            if (breakIdx <= pos) {
                target.add(line.substring(pos, pos + width));
                pos += width;
            } else {
                target.add(line.substring(pos, breakIdx));
                pos = breakIdx + 1;
            }
        }
    }

    /**
     * Shortens a session UUID to 8 characters for compact HUD display.
     *
     * <p>Traceability: REQ-00071, FR-00048
     *
     * @param sessionId full session ID
     * @return shortened session ID prefix
     */
    private String shortenSessionId(final String sessionId) {
        // [REQ-00071, FR-00048] Truncate session ID to SHORT_ID_LENGTH
        if (sessionId.length() <= SHORT_ID_LENGTH) {
            return sessionId;
        }
        return sessionId.substring(0, SHORT_ID_LENGTH);
    }

    /**
     * Centers a plain text string within {@code width} columns using space padding.
     *
     * <p>Traceability: REQ-00100, FR-00048
     *
     * @param text plain text string
     * @param width target width in columns
     * @return centered string of length {@code width}
     */
    private String centerPlain(final String text, final int width) {
        // [REQ-00100, FR-00048] Compute symmetric left and right space padding
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        final int totalPad = width - text.length();
        final int leftPad = totalPad / CENTER_DIVISOR;
        final int rightPad = totalPad - leftPad;
        return repeatChar(' ', leftPad) + text + repeatChar(' ', rightPad);
    }

    /**
     * Pads or truncates a plain string with spaces to match {@code width} columns exactly.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048, CS-0060.3
     *
     * @param text plain text string
     * @param width target column width
     * @return fitted string of length {@code width}
     */
    private String fitPlainLine(final String text, final int width) {
        // [REQ-00071, FR-00048, CS-0060.3] Sanitize tabs/CRs and pad or truncate to exact width
        final String sanitized = text.replace('\t', ' ').replace('\r', ' ');
        if (sanitized.length() > width) {
            return sanitized.substring(0, width);
        } else if (sanitized.length() < width) {
            return sanitized + repeatChar(' ', width - sanitized.length());
        } else {
            return sanitized;
        }
    }

    /**
     * Pads or truncates a plain string using {@code fillChar} to match {@code width} columns.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048, CS-0060.3
     *
     * @param text plain text string
     * @param width target column width
     * @param fillChar padding character
     * @return fitted string of length {@code width}
     */
    private String fitPlainLineWithFill(final String text, final int width, final char fillChar) {
        // [REQ-00071, FR-00048, CS-0060.3] Truncate or pad with fillChar to exact width
        if (text.length() > width) {
            return text.substring(0, width);
        } else if (text.length() < width) {
            return text + repeatChar(fillChar, width - text.length());
        } else {
            return text;
        }
    }

    /**
     * Repeats a character {@code count} times.
     *
     * <p>Traceability: REQ-00071, FR-00048
     *
     * @param ch character to repeat
     * @param count repetition count
     * @return repeated character string
     */
    private String repeatChar(final char ch, final int count) {
        // [REQ-00071, FR-00048] Return empty string for non-positive count or repeat character
        if (count <= 0) {
            return "";
        }
        return String.valueOf(ch).repeat(count);
    }

    /**
     * Immutable view state snapshot for fullscreen rendering.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048, FR-00049, CS-0050.1
     */
    public static final class ViewState {
        /** Active navigation route. */
        private final Route route;
        /** Active modal overlay. */
        private final OverlayModal overlay;
        /** Sidebar visibility policy (auto, show, hide). */
        private final String sidebarPolicy;
        /** Whether thinking boxes are expanded. */
        private final boolean showThinking;
        /** Whether tool outputs are expanded. */
        private final boolean showTools;
        /** Transcript scroll offset from bottom. */
        private final int scrollOffset;
        /** Current session goal string. */
        private final String currentGoal;
        /** Active side-channel question. */
        private final String btwQuestion;
        /** Active side-channel answer. */
        private final String btwAnswer;
        /** Transient status notice. */
        private final String statusNotice;
        /** Count of active sessions. */
        private final int activeSessionsCount;
        /** Count of registered tools. */
        private final int toolsCount;
        /** Registered tool names. */
        private final List<String> toolNames;
        /** Active thinking effort level. */
        private final String thinkingEffort;
        /** Runtime mode (dual, tui, web). */
        private final String runtimeMode;
        /** HTTP server port. */
        private final int serverPort;

        /**
         * Constructs a ViewState from a Builder.
         *
         * <p>Traceability: REQ-00100, FR-00048, CS-0050.1
         *
         * @param builder populated builder instance, must not be null
         */
        public ViewState(final Builder builder) {
            // [REQ-00100, FR-00048, CS-0050.1] Validate builder and route/overlay/sidebar fields
            final Builder b = Objects.requireNonNull(builder, "builder must not be null");
            this.route = Objects.requireNonNull(b.route, "route must not be null");
            this.overlay = Objects.requireNonNull(b.overlay, "overlay must not be null");
            this.sidebarPolicy = Objects.requireNonNull(b.sidebarPolicy, "sidebarPolicy must not be null");
            this.showThinking = b.showThinking;
            this.showTools = b.showTools;
            // [REQ-00100, FR-00048, CS-0050.1] Validate goal, btw, and telemetry fields
            this.scrollOffset = Math.max(0, b.scrollOffset);
            this.currentGoal = Objects.requireNonNull(b.currentGoal, "currentGoal must not be null");
            this.btwQuestion = Objects.requireNonNull(b.btwQuestion, "btwQuestion must not be null");
            this.btwAnswer = Objects.requireNonNull(b.btwAnswer, "btwAnswer must not be null");
            this.statusNotice = Objects.requireNonNull(b.statusNotice, "statusNotice must not be null");
            this.activeSessionsCount = Math.max(0, b.activeSessionsCount);
            this.toolsCount = Math.max(0, b.toolsCount);
            this.toolNames = List.copyOf(Objects.requireNonNull(b.toolNames, "toolNames must not be null"));
            this.thinkingEffort = Objects.requireNonNull(b.thinkingEffort, "thinkingEffort must not be null");
            this.runtimeMode = Objects.requireNonNull(b.runtimeMode, "runtimeMode must not be null");
            this.serverPort = b.serverPort;
        }

        /**
         * Returns active route.
         *
         * @return Route
         */
        public Route getRoute() {
            // [REQ-00100, FR-00048] Return active route
            return route;
        }

        /**
         * Returns active overlay modal.
         *
         * @return OverlayModal
         */
        public OverlayModal getOverlay() {
            // [REQ-00100, FR-00049] Return active modal overlay
            return overlay;
        }

        /**
         * Returns sidebar visibility policy.
         *
         * @return sidebar policy string
         */
        public String getSidebarPolicy() {
            // [REQ-00071, FR-00048] Return sidebar visibility policy
            return sidebarPolicy;
        }

        /**
         * Returns true if thinking stream is expanded.
         *
         * @return true if thinking shown
         */
        public boolean isShowThinking() {
            // [REQ-00088, FR-00035] Return whether thinking boxes are expanded
            return showThinking;
        }

        /**
         * Returns true if tool execution details are expanded.
         *
         * @return true if tools shown
         */
        public boolean isShowTools() {
            // [REQ-00071, FR-00048] Return whether tool outputs are expanded
            return showTools;
        }

        /**
         * Returns scroll offset from bottom.
         *
         * @return scroll offset
         */
        public int getScrollOffset() {
            // [REQ-00071, FR-00048] Return transcript scroll offset
            return scrollOffset;
        }

        /**
         * Returns current session goal.
         *
         * @return goal description
         */
        public String getCurrentGoal() {
            // [REQ-00100, FR-00048] Return current session goal
            return currentGoal;
        }

        /**
         * Returns active side-channel question.
         *
         * @return btw question
         */
        public String getBtwQuestion() {
            // [REQ-00100, FR-00048] Return side-channel question
            return btwQuestion;
        }

        /**
         * Returns active side-channel answer.
         *
         * @return btw answer
         */
        public String getBtwAnswer() {
            // [REQ-00100, FR-00048] Return side-channel answer
            return btwAnswer;
        }

        /**
         * Returns transient status notice.
         *
         * @return status notice
         */
        public String getStatusNotice() {
            // [REQ-00100, FR-00048] Return transient status notice
            return statusNotice;
        }

        /**
         * Returns active session count.
         *
         * @return session count
         */
        public int getActiveSessionsCount() {
            // [REQ-00071, FR-00048] Return active session count
            return activeSessionsCount;
        }

        /**
         * Returns registered tool count.
         *
         * @return tools count
         */
        public int getToolsCount() {
            // [REQ-00071, FR-00048] Return registered tool count
            return toolsCount;
        }

        /**
         * Returns unmodifiable list of tool names.
         *
         * @return tool names list
         */
        public List<String> getToolNames() {
            // [REQ-00071, FR-00048] Return unmodifiable list of tool names
            return toolNames;
        }

        /**
         * Returns active thinking effort level.
         *
         * @return thinking effort
         */
        public String getThinkingEffort() {
            // [REQ-00088, FR-00035] Return active thinking effort level
            return thinkingEffort;
        }

        /**
         * Returns runtime mode.
         *
         * @return runtime mode
         */
        public String getRuntimeMode() {
            // [REQ-00002, FR-00009] Return active runtime mode
            return runtimeMode;
        }

        /**
         * Returns HTTP server port.
         *
         * @return server port
         */
        public int getServerPort() {
            // [REQ-00071, FR-00048] Return HTTP server port
            return serverPort;
        }

        /**
         * Builder for {@link ViewState}.
         *
         * <p>Traceability: REQ-00100, FR-00048, CS-0050.1
         */
        public static final class Builder {
            /** Route field. */
            private Route route = Route.HOME;
            /** Overlay field. */
            private OverlayModal overlay = OverlayModal.NONE;
            /** Sidebar policy field. */
            private String sidebarPolicy = "auto";
            /** Show thinking flag. */
            private boolean showThinking = true;
            /** Show tools flag. */
            private boolean showTools = true;
            /** Scroll offset. */
            private int scrollOffset;
            /** Current goal. */
            private String currentGoal = "Autonomous pair-programming and tool assistance";
            /** Btw question. */
            private String btwQuestion = "";
            /** Btw answer. */
            private String btwAnswer = "";
            /** Status notice. */
            private String statusNotice = "";
            /** Active sessions count. */
            private int activeSessionsCount = 1;
            /** Tools count. */
            private int toolsCount;
            /** Tool names. */
            private List<String> toolNames = Collections.emptyList();
            /** Thinking effort. */
            private String thinkingEffort = "medium";
            /** Runtime mode. */
            private String runtimeMode = "dual";
            /** Server port. */
            private int serverPort = DEFAULT_SERVER_PORT;

            /**
             * Constructs a default ViewState Builder.
             */
            public Builder() {
            }

            /**
             * Sets route.
             *
             * @param val route, must not be null
             * @return this builder
             */
            public Builder route(final Route val) {
                // [REQ-00100, FR-00048, CS-0050.1] Validate non-null route
                this.route = Objects.requireNonNull(val, "route must not be null");
                return this;
            }

            /**
             * Sets overlay modal.
             *
             * @param val overlay, must not be null
             * @return this builder
             */
            public Builder overlay(final OverlayModal val) {
                // [REQ-00100, FR-00049, CS-0050.1] Validate non-null overlay modal
                this.overlay = Objects.requireNonNull(val, "overlay must not be null");
                return this;
            }

            /**
             * Sets sidebar policy.
             *
             * @param val sidebar policy, must not be null
             * @return this builder
             */
            public Builder sidebarPolicy(final String val) {
                // [REQ-00071, FR-00048, CS-0050.1] Validate non-null sidebar policy
                this.sidebarPolicy = Objects.requireNonNull(val, "sidebarPolicy must not be null");
                return this;
            }

            /**
             * Sets showThinking flag.
             *
             * @param val showThinking
             * @return this builder
             */
            public Builder showThinking(final boolean val) {
                // [REQ-00088, FR-00035] Set reasoning stream visibility flag
                this.showThinking = val;
                return this;
            }

            /**
             * Sets showTools flag.
             *
             * @param val showTools
             * @return this builder
             */
            public Builder showTools(final boolean val) {
                // [REQ-00071, FR-00048] Set tool invocation output visibility flag
                this.showTools = val;
                return this;
            }

            /**
             * Sets scrollOffset.
             *
             * @param val scrollOffset
             * @return this builder
             */
            public Builder scrollOffset(final int val) {
                // [REQ-00071, FR-00048] Clamp scroll offset to non-negative value
                this.scrollOffset = Math.max(0, val);
                return this;
            }

            /**
             * Sets currentGoal.
             *
             * @param val goal string, must not be null
             * @return this builder
             */
            public Builder currentGoal(final String val) {
                // [REQ-00100, FR-00048, CS-0050.1] Validate non-null goal description
                this.currentGoal = Objects.requireNonNull(val, "currentGoal must not be null");
                return this;
            }

            /**
             * Sets btw Q&amp;A.
             *
             * @param questionVal question, must not be null
             * @param answerVal answer, must not be null
             * @return this builder
             */
            public Builder btw(final String questionVal, final String answerVal) {
                // [REQ-00100, FR-00048, CS-0050.1] Validate non-null side-channel Q&A strings
                this.btwQuestion = Objects.requireNonNull(questionVal, "questionVal must not be null");
                this.btwAnswer = Objects.requireNonNull(answerVal, "answerVal must not be null");
                return this;
            }

            /**
             * Sets statusNotice.
             *
             * @param val notice, must not be null
             * @return this builder
             */
            public Builder statusNotice(final String val) {
                // [REQ-00100, FR-00048, CS-0050.1] Validate non-null status notice
                this.statusNotice = Objects.requireNonNull(val, "statusNotice must not be null");
                return this;
            }

            /**
             * Sets telemetry counts and tool names.
             *
             * @param sessionsVal active sessions count
             * @param toolsVal registered tools count
             * @param namesVal tool names list, must not be null
             * @return this builder
             */
            public Builder telemetry(final int sessionsVal, final int toolsVal, final List<String> namesVal) {
                // [REQ-00071, FR-00048, CS-0050.1] Store telemetry counts and defensive copy of tool names
                this.activeSessionsCount = sessionsVal;
                this.toolsCount = toolsVal;
                this.toolNames = List.copyOf(Objects.requireNonNull(namesVal, "namesVal must not be null"));
                return this;
            }

            /**
             * Sets runtime metadata.
             *
             * @param effortVal thinking effort, must not be null
             * @param modeVal runtime mode, must not be null
             * @param portVal server port
             * @return this builder
             */
            public Builder runtime(final String effortVal, final String modeVal, final int portVal) {
                // [REQ-00088, REQ-00100, CS-0050.1] Store thinking effort, runtime mode, and port
                this.thinkingEffort = Objects.requireNonNull(effortVal, "effortVal must not be null");
                this.runtimeMode = Objects.requireNonNull(modeVal, "modeVal must not be null");
                this.serverPort = portVal;
                return this;
            }

            /**
             * Builds the immutable ViewState.
             *
             * @return new ViewState instance
             */
            public ViewState build() {
                // [REQ-00100, FR-00048] Construct immutable ViewState snapshot
                return new ViewState(this);
            }
        }
    }
}
