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
import java.util.Objects;

/**
 * Interactive full-screen / line-oriented TUI dashboard driving the Cyberpunk pair-programming loop.
 * Features OpenCode-inspired responsive viewport adapting, SSH &amp; tmux multiplexer detection,
 * OSC 52 clipboard tunneling, side-channel `/btw` queries, `/goal` management, and `/env` diagnostics.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine), REQ-00088 (Dual Chat Mode Reasoning)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design &amp; Integration), TSK-20260822-007 (Dual Chat Mode Reasoning Demux)
 * - ADR: ADR-0001 (Unified Dual Architecture), ADR-0047 (Dual Chat Mode &amp; Explicit Reasoning Demux)
 */
@Component
public class OmniwrenchTuiDashboard {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OmniwrenchTuiDashboard.class);

    /** Prefix length for `/btw ` command string. */
    private static final int BTW_PREFIX_LENGTH = 5;
    /** Prefix length for `/goal ` command string. */
    private static final int GOAL_PREFIX_LENGTH = 6;
    /** Terminal clear escape sequence. */
    private static final String ANSI_CLEAR_SCREEN = "\u001B[2J\u001B[H";

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

    /** Web server port injected from Spring environment. */
    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Constructs the TUI dashboard with required services, defaulting to standard environment detector.
     *
     * @param rendererVal terminal renderer component
     * @param agentEngineVal agent reasoning engine
     * @param sessionManagerVal session lifecycle manager
     * @param toolRegistryVal tool registry service
     * @param propertiesVal configuration properties
     */
    public OmniwrenchTuiDashboard(final TerminalRenderer rendererVal,
                                   final AgentEngine agentEngineVal,
                                   final SessionManager sessionManagerVal,
                                   final ToolRegistry toolRegistryVal,
                                   final OmniwrenchProperties propertiesVal) {
        this(rendererVal, agentEngineVal, sessionManagerVal, toolRegistryVal, propertiesVal, new TerminalEnvironmentDetector());
    }

    /**
     * Constructs the TUI dashboard with required services and explicit environment detector.
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
        this.renderer = Objects.requireNonNull(rendererVal, "renderer must not be null");
        this.agentEngine = Objects.requireNonNull(agentEngineVal, "agentEngine must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManagerVal, "sessionManager must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistryVal, "toolRegistry must not be null");
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");
        this.environmentDetector = Objects.requireNonNull(environmentDetectorVal, "environmentDetector must not be null");
    }

    /**
     * Starts the interactive command loop on standard input/output.
     */
    public void startInteractiveLoop() {
        final TerminalEnvironment env = environmentDetector.detect();

        // Set terminal window title and tmux window name if multiplexer is active
        System.out.print(env.formatWindowTitle("Omniwrench :: Dual Agent Workbench"));
        System.out.print(env.formatTmuxWindowName("omniwrench"));
        System.out.flush();

        System.out.println(renderer.renderBanner(env));
        System.out.println(renderer.renderStatusBar(
                properties.getMode(),
                sessionManager.getActiveSessionCount(),
                toolRegistry.getToolCount(),
                serverPort,
                env
        ));
        System.out.println("\nType your goal, prompt, or commands (/btw, /goal, /copy, /env, /clear, /run <cmd>, /cat <file>, /help, exit):\n");

        final SessionContext session = sessionManager.getDefaultSession();
        String currentGoal = "Autonomous pair-programming and tool assistance";
        String lastAgentContent = "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print(renderer.renderPromptBox(""));
                final String line = reader.readLine();
                if (line == null || "exit".equalsIgnoreCase(line.trim()) || "quit".equalsIgnoreCase(line.trim())) {
                    System.out.println("\nShutting down Omniwrench TUI. Goodbye!");
                    break;
                } else {
                    final String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    } else if ("/help".equalsIgnoreCase(trimmed)) {
                        printHelp();
                        continue;
                    } else if ("/clear".equalsIgnoreCase(trimmed)) {
                        System.out.print(ANSI_CLEAR_SCREEN);
                        System.out.flush();
                        continue;
                    } else if ("/env".equalsIgnoreCase(trimmed)) {
                        System.out.println(renderer.renderEnvironmentCard(env));
                        continue;
                    } else if (trimmed.startsWith("/goal")) {
                        currentGoal = handleGoalCommand(trimmed, currentGoal);
                        continue;
                    } else if (trimmed.startsWith("/btw")) {
                        handleBtwCommand(session, trimmed);
                        continue;
                    } else if ("/copy".equalsIgnoreCase(trimmed)) {
                        handleCopyCommand(env, lastAgentContent);
                        continue;
                    } else {
                        // Regular prompt execution
                        System.out.println(renderer.renderMessageBubble("user", trimmed));
                        final AgentMessage response = agentEngine.processPrompt(session, trimmed);
                        if (response.hasThinking()) {
                            System.out.println(renderer.renderThinkingBox(response.getThinking()));
                        } else {
                            // No explicit thinking stream
                        }
                        lastAgentContent = response.getContent();
                        System.out.println(renderer.renderMessageBubble("agent", response.getContent()));
                        System.out.println();
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Error during TUI interactive execution", e);
        }
    }

    private String handleGoalCommand(final String trimmed, final String currentGoal) {
        if (trimmed.length() > GOAL_PREFIX_LENGTH) {
            final String newGoal = trimmed.substring(GOAL_PREFIX_LENGTH).trim();
            System.out.println(renderer.renderGoalCard(newGoal));
            return newGoal;
        } else {
            System.out.println(renderer.renderGoalCard(currentGoal));
            return currentGoal;
        }
    }

    private void handleBtwCommand(final SessionContext session, final String trimmed) {
        if (trimmed.length() > BTW_PREFIX_LENGTH) {
            final String question = trimmed.substring(BTW_PREFIX_LENGTH).trim();
            final AgentMessage sideChannelResponse = agentEngine.processPrompt(session, "[SIDE-CHANNEL QUESTION] " + question);
            System.out.println(renderer.renderBtwBox(question, sideChannelResponse.getContent()));
        } else {
            System.out.println("Usage: /btw <question>  (Ask an ephemeral side-channel question)");
        }
    }

    private void handleCopyCommand(final TerminalEnvironment env, final String lastAgentContent) {
        if (lastAgentContent.isEmpty()) {
            System.out.println("No assistant response available to copy yet.");
        } else {
            System.out.print(env.formatOsc52Clipboard(lastAgentContent));
            System.out.flush();
            final int byteCount = lastAgentContent.getBytes(StandardCharsets.UTF_8).length;
            System.out.println(renderer.renderCopyFeedback(byteCount));
        }
    }

    private void printHelp() {
        System.out.println("\nAvailable Omniwrench Commands:");
        System.out.println("  /help           - Display this help message");
        System.out.println("  /btw <question> - Ephemeral side-channel prompt inspired by OpenCode");
        System.out.println("  /goal [goal]    - Display or update current session goal");
        System.out.println("  /env            - Display terminal mode, SSH/tmux detection, and viewport telemetry");
        System.out.println("  /copy           - Copy last assistant response to clipboard using OSC 52");
        System.out.println("  /clear          - Clear terminal screen");
        System.out.println("  /run <command>  - Execute shell command via CommandExecutionTool");
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
