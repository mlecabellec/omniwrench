package com.omniwrench.tui;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Minimal inline stream-oriented TUI presentation engine preserving native terminal scrollback.
 * Inspired by Claude Code, Aider, and Antigravity CLI inline stream mode.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00088 (Dual Chat Mode Reasoning)</li>
 *   <li>Requirement: REQ-00099 (XTerm-256 Color Palette TUI Visual Theming Engine)</li>
 *   <li>Requirement: REQ-00100 (Dual-Mode Minimal Inline Stream &amp; Full-Screen Resizable TUI Workbench)</li>
 *   <li>Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine), FR-00046, FR-00047</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20261101-016, TSK-20261101-018</li>
 *   <li>Rules: CS-0010, CS-0030, CS-0040, CS-0050, CS-0060</li>
 * </ul>
 */
@Component
public class MinimalTuiModeEngine implements TuiViewModeEngine {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MinimalTuiModeEngine.class);

    /** Prefix length for `/btw ` command string per CS-0040.2. */
    private static final int BTW_PREFIX_LENGTH = 5;
    /** Prefix length for `/goal ` command string per CS-0040.2. */
    private static final int GOAL_PREFIX_LENGTH = 6;
    /** Prefix length for `/theme ` command string per CS-0040.2. */
    private static final int THEME_PREFIX_LENGTH = 7;
    /** Default HTTP server port fallback per CS-0040.2. */
    private static final int DEFAULT_SERVER_PORT = 8080;
    /** Terminal clear escape sequence per CS-0040.2. */
    private static final String ANSI_CLEAR_SCREEN = "\u001B[2J\u001B[H";
    /** Default initial session objective description per CS-0040.2. */
    private static final String DEFAULT_GOAL = "Autonomous pair-programming and tool assistance";

    /** Terminal widget renderer. */
    private final TerminalRenderer renderer;
    /** Autonomous agent engine. */
    private final AgentEngine agentEngine;
    /** Session lifecycle manager. */
    private final SessionManager sessionManager;
    /** Tool registry service. */
    private final ToolRegistry toolRegistry;
    /** Configuration properties. */
    private final OmniwrenchProperties properties;

    /** Web server port injected from Spring environment. */
    @Value("${server.port:8080}")
    private int serverPort = DEFAULT_SERVER_PORT;

    /** Active session goal string. */
    private String currentGoal = DEFAULT_GOAL;

    /**
     * Constructs the MinimalTuiModeEngine with required core and TUI services.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00047, CS-0050.1
     *
     * @param rendererVal terminal renderer component, must not be null
     * @param agentEngineVal agent reasoning engine, must not be null
     * @param sessionManagerVal session manager service, must not be null
     * @param toolRegistryVal tool registry service, must not be null
     * @param propertiesVal configuration properties, must not be null
     */
    @Autowired
    public MinimalTuiModeEngine(final TerminalRenderer rendererVal,
                                final AgentEngine agentEngineVal,
                                final SessionManager sessionManagerVal,
                                final ToolRegistry toolRegistryVal,
                                final OmniwrenchProperties propertiesVal) {
        // [REQ-00100, FR-00047, CS-0050.1] Validate constructor dependencies
        this.renderer = Objects.requireNonNull(rendererVal, "renderer must not be null");
        this.agentEngine = Objects.requireNonNull(agentEngineVal, "agentEngine must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManagerVal, "sessionManager must not be null");
        // [REQ-00100, FR-00047, CS-0050.1] Validate tool registry and properties
        this.toolRegistry = Objects.requireNonNull(toolRegistryVal, "toolRegistry must not be null");
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");
    }

    /**
     * Returns {@link TuiDisplayMode#MINIMAL} as the display mode served by this engine.
     *
     * <p>Traceability: REQ-00100, FR-00047, CS-0030.1
     *
     * @return {@link TuiDisplayMode#MINIMAL}
     */
    @Override
    public final TuiDisplayMode getDisplayMode() {
        // [REQ-00100, FR-00047] Identify engine as MINIMAL inline stream mode
        return TuiDisplayMode.MINIMAL;
    }

    /**
     * Returns the current session goal string.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00047
     *
     * @return current goal description
     */
    public final String getCurrentGoal() {
        // [REQ-00100, FR-00047] Return active session goal description
        return currentGoal;
    }

    /**
     * Updates the current session goal string.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00047, CS-0050.1
     *
     * @param goalVal new goal description, must not be null
     */
    public final void setCurrentGoal(final String goalVal) {
        // [REQ-00100, FR-00047, CS-0050.1] Validate and store non-null goal description
        this.currentGoal = Objects.requireNonNull(goalVal, "goalVal must not be null");
    }

    /**
     * Renders a non-interactive snapshot of the minimal stream TUI view for diagnostics and testing.
     *
     * <p>Traceability: REQ-00001, REQ-00099, REQ-00100, FR-00046, FR-00047, CS-0050.1, CS-0050.6
     *
     * @param session active session context, must not be null
     * @param env terminal environment metadata, must not be null
     * @return rendered multi-line ANSI snapshot string
     */
    @Override
    public final String renderSnapshot(final SessionContext session, final TerminalEnvironment env) {
        // [REQ-00100, FR-00047, CS-0050.1] Validate snapshot parameters
        final SessionContext nonNullSession = Objects.requireNonNull(session, "session must not be null");
        final TerminalEnvironment nonNullEnv = Objects.requireNonNull(env, "env must not be null");
        final TuiThemePalette palette = TuiThemePalette.resolve(properties.getTui().getTheme());

        // [REQ-00001, FR-00001] Render compact header banner and telemetry status bar
        final StringBuilder sb = new StringBuilder();
        sb.append(renderer.renderBanner(nonNullEnv)).append("\n");
        sb.append(renderer.renderStatusBar(
                properties.getMode(),
                sessionManager.getActiveSessionCount(),
                toolRegistry.getToolCount(),
                serverPort,
                nonNullEnv
        )).append("\n");

        // [REQ-00099, REQ-00100, FR-00046] Append active theme and goal summary line
        sb.append(palette.getMutedFg())
                .append(" [UI: MINIMAL STREAM] [THEME: ")
                .append(palette.getName().toUpperCase())
                .append("] [GOAL: ")
                .append(currentGoal)
                .append("]")
                .append(TuiThemePalette.RESET)
                .append("\n");

        // [REQ-00088, FR-00035, CS-0050.6] Render recorded messages in bounded inline transcript format
        final List<AgentMessage> messages = nonNullSession.getMessages();
        int renderedCount = 0;
        for (final AgentMessage msg : messages) {
            if (renderedCount >= MAX_INTERACTIVE_TURNS) {
                throw new IllegalStateException("Exceeded maximum transcript messages in snapshot");
            }
            renderedCount++;
            // [REQ-00088, FR-00035, CS-0060.3] Demultiplex thinking block when present
            if (msg.hasThinking()) {
                sb.append(renderer.renderThinkingBox(msg.getThinking())).append("\n");
            } else {
                LOGGER.trace("Message {} has no thinking block", msg.getId());
            }
            sb.append(renderer.renderMessageBubble(msg.getRole(), msg.getContent())).append("\n");
        }

        // [REQ-00100, FR-00047] Append prompt indicator at bottom of stream snapshot
        sb.append(renderer.renderPromptBox(""));
        return sb.toString();
    }

    /**
     * Starts the interactive minimal stream TUI command loop on standard input/output.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00047, UC-00001, CS-0050.1
     *
     * @param session active session context, must not be null
     * @param env terminal environment metadata, must not be null
     */
    @Override
    public final void start(final SessionContext session, final TerminalEnvironment env) {
        // [REQ-00100, FR-00047, CS-0050.1] Validate entry parameters
        final SessionContext nonNullSession = Objects.requireNonNull(session, "session must not be null");
        final TerminalEnvironment nonNullEnv = Objects.requireNonNull(env, "env must not be null");

        // [REQ-00001, FR-00001] Configure terminal window title and tmux window name
        System.out.print(nonNullEnv.formatWindowTitle("Omniwrench :: Minimal TUI"));
        System.out.print(nonNullEnv.formatTmuxWindowName("omniwrench"));
        System.out.flush();

        // [REQ-00001, REQ-00100, FR-00047] Render initial header and status bar
        System.out.println(renderer.renderBanner(nonNullEnv));
        System.out.println(renderer.renderStatusBar(
                properties.getMode(),
                sessionManager.getActiveSessionCount(),
                toolRegistry.getToolCount(),
                serverPort,
                nonNullEnv
        ));
        System.out.println("\nType your goal, prompt, or commands (/btw, /goal, /copy, /env, /theme, /tools, /clear, /help, exit):\n");

        // [REQ-00100, FR-00047, CS-0050.6] Execute bounded interactive command loop
        runMinimalCommandLoop(nonNullSession, nonNullEnv);
    }

    /**
     * Executes the bounded minimal TUI REPL loop over {@code System.in}.
     *
     * <p>Traceability: REQ-00100, FR-00047, CS-0050.6, CS-0060.3
     *
     * @param session active session context
     * @param env terminal environment metadata
     */
    private void runMinimalCommandLoop(final SessionContext session, final TerminalEnvironment env) {
        // [REQ-00100, FR-00047, CS-0050.6] Initialize turn counter and last response buffer
        String lastAgentContent = "";
        int turnCounter = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            // [REQ-00100, CS-0050.6] Enforce hard upper bound on interactive turns
            while (turnCounter < MAX_INTERACTIVE_TURNS) {
                turnCounter++;
                System.out.print(renderer.renderPromptBox(""));
                final String line = reader.readLine();
                // [REQ-00100, CS-0060.3] Handle EOF or explicit exit command vs prompt input
                if (line == null || isExitCommand(line.trim())) {
                    System.out.println("\nShutting down Omniwrench TUI. Goodbye!");
                    break;
                } else {
                    final String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    } else {
                        lastAgentContent = dispatchMinimalLine(session, env, trimmed, lastAgentContent);
                    }
                }
            }
            // [CS-0050.6] Throw on loop bound breach
            if (turnCounter >= MAX_INTERACTIVE_TURNS) {
                throw new IllegalStateException("Exceeded MAX_INTERACTIVE_TURNS bound in MinimalTuiModeEngine");
            }
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Error during minimal TUI interactive execution", ex);
        }
    }

    /**
     * Dispatches a single trimmed command or prompt line in minimal stream mode.
     *
     * <p>Traceability: REQ-00001, REQ-00099, REQ-00100, FR-00046, FR-00047, CS-0060.3
     *
     * @param session active session context
     * @param env terminal environment metadata
     * @param trimmed trimmed user input line
     * @param lastAgentContent previous assistant response text
     * @return updated last assistant response text
     */
    private String dispatchMinimalLine(final SessionContext session,
                                       final TerminalEnvironment env,
                                       final String trimmed,
                                       final String lastAgentContent) {
        // [REQ-00100, FR-00047, CS-0060.3] Route slash commands or regular prompts with total branching
        if ("/help".equalsIgnoreCase(trimmed)) {
            printHelp();
            return lastAgentContent;
        } else if ("/clear".equalsIgnoreCase(trimmed)) {
            System.out.print(ANSI_CLEAR_SCREEN);
            System.out.flush();
            return lastAgentContent;
        } else if ("/env".equalsIgnoreCase(trimmed)) {
            System.out.println(renderer.renderEnvironmentCard(env));
            return lastAgentContent;
        } else if (trimmed.startsWith("/goal")) {
            handleGoalCommand(trimmed);
            return lastAgentContent;
        } else if (trimmed.startsWith("/btw")) {
            handleBtwCommand(session, trimmed);
            return lastAgentContent;
        } else if ("/copy".equalsIgnoreCase(trimmed)) {
            handleCopyCommand(env, lastAgentContent);
            return lastAgentContent;
        } else if (trimmed.startsWith("/theme")) {
            handleThemeCommand(trimmed);
            return lastAgentContent;
        } else if ("/tools".equalsIgnoreCase(trimmed)) {
            handleToolsCommand();
            return lastAgentContent;
        } else {
            return executePromptTurn(session, trimmed);
        }
    }

    /**
     * Executes a standard user prompt turn through {@link AgentEngine} and renders thinking/response bubbles.
     *
     * <p>Traceability: REQ-00030, REQ-00088, FR-00003, FR-00035, CS-0060.3
     *
     * @param session active session context
     * @param trimmed user prompt string
     * @return assistant response content
     */
    private String executePromptTurn(final SessionContext session, final String trimmed) {
        // [REQ-00030, REQ-00088, FR-00035] Render user message bubble and dispatch prompt to AgentEngine
        System.out.println(renderer.renderMessageBubble("user", trimmed));
        final AgentMessage response = agentEngine.processPrompt(session, trimmed);
        if (response.hasThinking()) {
            System.out.println(renderer.renderThinkingBox(response.getThinking()));
        } else {
            LOGGER.trace("No explicit thinking stream for prompt turn");
        }
        // [REQ-00001, FR-00001] Render assistant response bubble
        final String content = response.getContent();
        System.out.println(renderer.renderMessageBubble("agent", content));
        System.out.println();
        return content;
    }

    /**
     * Checks whether the input string is a termination command.
     *
     * <p>Traceability: REQ-00100, FR-00047
     *
     * @param trimmed trimmed input line
     * @return true if exit or quit command
     */
    private boolean isExitCommand(final String trimmed) {
        // [REQ-00100, FR-00047] Match standard exit and quit commands
        return "exit".equalsIgnoreCase(trimmed)
                || "quit".equalsIgnoreCase(trimmed)
                || "/exit".equalsIgnoreCase(trimmed)
                || "/quit".equalsIgnoreCase(trimmed);
    }

    /**
     * Handles the `/goal` command to view or update the active session goal.
     *
     * <p>Traceability: REQ-00100, FR-00047, CS-0060.3
     *
     * @param trimmed full `/goal` command string
     */
    private void handleGoalCommand(final String trimmed) {
        // [REQ-00100, FR-00047, CS-0060.3] Update goal if argument provided, then render goal card
        if (trimmed.length() > GOAL_PREFIX_LENGTH) {
            this.currentGoal = trimmed.substring(GOAL_PREFIX_LENGTH).trim();
        } else {
            LOGGER.trace("Displaying current session goal without modification");
        }
        System.out.println(renderer.renderGoalCard(this.currentGoal));
    }

    /**
     * Handles the `/btw` side-channel question command inspired by OpenCode and OpenClaw.
     *
     * <p>Traceability: REQ-00100, FR-00047, CS-0060.3
     *
     * @param session active session context
     * @param trimmed full `/btw` command string
     */
    private void handleBtwCommand(final SessionContext session, final String trimmed) {
        // [REQ-00100, FR-00047, CS-0060.3] Execute ephemeral side-channel query or print usage
        if (trimmed.length() > BTW_PREFIX_LENGTH) {
            final String question = trimmed.substring(BTW_PREFIX_LENGTH).trim();
            final AgentMessage sideChannelResponse = agentEngine.processPrompt(
                    session, "[SIDE-CHANNEL QUESTION] " + question);
            System.out.println(renderer.renderBtwBox(question, sideChannelResponse.getContent()));
        } else {
            System.out.println("Usage: /btw <question>  (Ask an ephemeral side-channel question)");
        }
    }

    /**
     * Handles the `/copy` command to copy the last assistant response via OSC 52 clipboard sequence.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00047, CS-0060.3
     *
     * @param env terminal environment metadata
     * @param lastAgentContent last assistant response text
     */
    private void handleCopyCommand(final TerminalEnvironment env, final String lastAgentContent) {
        // [REQ-00001, FR-00047, CS-0060.3] Emit OSC 52 sequence if assistant content is available
        if (lastAgentContent.isEmpty()) {
            System.out.println("No assistant response available to copy yet.");
        } else {
            System.out.print(env.formatOsc52Clipboard(lastAgentContent));
            System.out.flush();
            final int byteCount = lastAgentContent.getBytes(StandardCharsets.UTF_8).length;
            System.out.println(renderer.renderCopyFeedback(byteCount));
        }
    }

    /**
     * Handles the `/theme` command to switch or cycle the active XTerm-256 color theme.
     *
     * <p>Traceability: REQ-00099, FR-00046, CS-0060.3
     *
     * @param trimmed full `/theme` command string
     */
    private void handleThemeCommand(final String trimmed) {
        // [REQ-00099, FR-00046, CS-0060.3] Resolve explicit theme name or cycle to next palette
        if (trimmed.length() > THEME_PREFIX_LENGTH) {
            final String requested = trimmed.substring(THEME_PREFIX_LENGTH).trim();
            final TuiThemePalette resolved = TuiThemePalette.resolve(requested);
            properties.getTui().setTheme(resolved.getName());
            System.out.println(resolved.getPrimaryFg() + TuiThemePalette.BOLD
                    + "Active TUI theme switched to: " + resolved.getName() + TuiThemePalette.RESET);
        } else {
            final TuiThemePalette next = TuiThemePalette.nextTheme(properties.getTui().getTheme());
            properties.getTui().setTheme(next.getName());
            System.out.println(next.getPrimaryFg() + TuiThemePalette.BOLD
                    + "Cycled TUI theme to: " + next.getName()
                    + " (available: cyberpunk, opencode, dracula, nord, matrix, high-contrast)"
                    + TuiThemePalette.RESET);
        }
    }

    /**
     * Handles the `/tools` command to list all registered tools in {@link ToolRegistry}.
     *
     * <p>Traceability: REQ-00015, REQ-00100, FR-00002, CS-0060.3
     */
    private void handleToolsCommand() {
        // [REQ-00015, FR-00002, CS-0060.3] Query ToolRegistry and print each tool definition
        final List<ToolDefinition> definitions = toolRegistry.getAllDefinitions();
        System.out.println("\nRegistered Omniwrench Tools (" + definitions.size() + "):");
        if (definitions.isEmpty()) {
            System.out.println("  (no tools registered)");
        } else {
            for (final ToolDefinition def : definitions) {
                System.out.println("  • " + def.getName() + " — " + def.getDescription());
            }
        }
        System.out.println();
    }

    /**
     * Prints interactive slash command help for minimal stream mode.
     *
     * <p>Traceability: REQ-00100, FR-00047
     */
    private void printHelp() {
        // [REQ-00100, FR-00047] Output structured command reference table
        System.out.println("\nAvailable Omniwrench Commands (Minimal Stream Mode):");
        System.out.println("  /help           - Display this help message");
        System.out.println("  /fullscreen     - Switch to Fullscreen Resizable TUI Workbench");
        System.out.println("  /btw <question> - Ephemeral side-channel prompt inspired by OpenCode");
        System.out.println("  /goal [goal]    - Display or update current session goal");
        // [REQ-00099, FR-00046] Output theme, environment, and tool commands
        System.out.println("  /theme [name]   - Cycle or select theme (cyberpunk, opencode, dracula, nord, matrix, high-contrast)");
        System.out.println("  /env            - Display terminal mode, SSH/tmux detection, and viewport telemetry");
        System.out.println("  /copy           - Copy last assistant response to clipboard using OSC 52");
        System.out.println("  /clear          - Clear terminal screen");
        System.out.println("  /run <command>  - Execute shell command via CommandExecutionTool");
        // [REQ-00088, REQ-00091, FR-00035, FR-00038] Output thinking and model hub commands
        System.out.println("  /cat <path>     - Read file contents via FileOperationsTool");
        System.out.println("  /thinking [cmd] - Configure reasoning mode (on, off, low, medium, high, max, status)");
        System.out.println("  /model list     - List locally downloaded quantized GGUF models");
        System.out.println("  /model search   - Search Ollama and HuggingFace repositories");
        System.out.println("  /model pull     - Pull and verify GGUF model weights");
        System.out.println("  /model rm       - Delete local model weights from disk");
        System.out.println("  /tools          - List registered tools and capabilities");
        System.out.println("  exit / quit     - Exit Omniwrench\n");
    }
}
