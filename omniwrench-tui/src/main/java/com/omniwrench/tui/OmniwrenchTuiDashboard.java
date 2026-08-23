package com.omniwrench.tui;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Interactive Terminal User Interface dashboard loop for local agent orchestration.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI & Interactive TUI Presentation Engine), REQ-00043 (Hybrid Reasoning Loop)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design & Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
@Component
public class OmniwrenchTuiDashboard {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OmniwrenchTuiDashboard.class);

    /** Presentation renderer. */
    private final TerminalRenderer renderer;
    /** Agent reasoning engine. */
    private final AgentEngine agentEngine;
    /** Session manager. */
    private final SessionManager sessionManager;
    /** Tool registry. */
    private final ToolRegistry toolRegistry;
    /** Runtime configuration properties. */
    private final OmniwrenchProperties properties;

    /** Web server port property. */
    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Constructs the TUI dashboard with required presentation, engine, and registry dependencies.
     *
     * @param rendererVal the terminal renderer, must not be null
     * @param agentEngineVal the agent reasoning engine, must not be null
     * @param sessionManagerVal the session manager, must not be null
     * @param toolRegistryVal the tool registry, must not be null
     * @param propertiesVal the configuration properties, must not be null
     */
    public OmniwrenchTuiDashboard(final TerminalRenderer rendererVal,
                                  final AgentEngine agentEngineVal,
                                  final SessionManager sessionManagerVal,
                                  final ToolRegistry toolRegistryVal,
                                  final OmniwrenchProperties propertiesVal) {
        this.renderer = Objects.requireNonNull(rendererVal, "renderer must not be null");
        this.agentEngine = Objects.requireNonNull(agentEngineVal, "agentEngine must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManagerVal, "sessionManager must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistryVal, "toolRegistry must not be null");
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");
    }

    /**
     * Starts the interactive CLI TUI loop.
     */
    public void startInteractiveLoop() {
        System.out.println(renderer.renderBanner());
        System.out.println(renderer.renderStatusBar(
                properties.getMode(),
                sessionManager.getActiveSessions().size(),
                toolRegistry.getToolCount(),
                serverPort
        ));
        System.out.println("\nType your goal, prompt, or commands (/run <cmd>, /cat <file>, /help, exit):\n");

        final SessionContext session = sessionManager.getDefaultSession();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print(renderer.renderPromptBox(""));
                final String line = reader.readLine();
                if (line == null || "exit".equalsIgnoreCase(line.trim()) || "quit".equalsIgnoreCase(line.trim())) {
                    System.out.println("\nShutting down Omniwrench TUI. Goodbye!");
                    break;
                }

                final String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if ("/help".equalsIgnoreCase(trimmed)) {
                    printHelp();
                    continue;
                }

                System.out.println(renderer.renderMessageBubble("user", trimmed));
                final AgentMessage response = agentEngine.processPrompt(session, trimmed);
                System.out.println(renderer.renderMessageBubble("agent", response.getContent()));
                System.out.println();
            }
        } catch (final Exception e) {
            LOGGER.error("Error during TUI interactive execution", e);
        }
    }

    private void printHelp() {
        System.out.println("\nAvailable Omniwrench Commands:");
        System.out.println("  /help           - Display this help message");
        System.out.println("  /run <command>  - Execute shell command via CommandExecutionTool");
        System.out.println("  /cat <path>     - Read file contents via FileOperationsTool");
        System.out.println("  /model list     - List locally downloaded quantized GGUF models");
        System.out.println("  /model search   - Search Ollama and HuggingFace repositories");
        System.out.println("  /model pull     - Pull and verify GGUF model weights");
        System.out.println("  /model rm       - Delete local model weights from disk");
        System.out.println("  /tools          - List registered tools and capabilities");
        System.out.println("  exit / quit     - Exit Omniwrench\n");
    }

}
