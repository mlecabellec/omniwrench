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
 * - Requirement: REQ-00041 (Interactive Cyberpunk TUI Loop)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design & Integration)
 */
@Component
public class OmniwrenchTuiDashboard {

    private static final Logger LOGGER = LoggerFactory.getLogger(OmniwrenchTuiDashboard.class);

    private final TerminalRenderer renderer;
    private final AgentEngine agentEngine;
    private final SessionManager sessionManager;
    private final ToolRegistry toolRegistry;
    private final OmniwrenchProperties properties;

    @Value("${server.port:8080}")
    private int serverPort;

    public OmniwrenchTuiDashboard(final TerminalRenderer renderer,
                                  final AgentEngine agentEngine,
                                  final SessionManager sessionManager,
                                  final ToolRegistry toolRegistry,
                                  final OmniwrenchProperties properties) {
        this.renderer = Objects.requireNonNull(renderer, "renderer must not be null");
        this.agentEngine = Objects.requireNonNull(agentEngine, "agentEngine must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
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
        System.out.println("  /tools          - List registered tools and capabilities");
        System.out.println("  exit / quit     - Exit Omniwrench\n");
    }
}
