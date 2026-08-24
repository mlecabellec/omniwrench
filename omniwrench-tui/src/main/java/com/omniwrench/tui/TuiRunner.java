package com.omniwrench.tui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Spring CommandLineRunner supporting non-interactive CLI prompting, stdin pipes, and interactive TUI launching.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine), REQ-00002 (Configurable Profiles)
 * - Requirement: REQ-00089 (Unified Tri-Interface Prompting CLI, TUI, Web)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00004 (Headless CI/CD Automation Execution)
 * - Task: TSK-20260822-003 (TUI Design), TSK-20260822-008 (Unified Tri-Interface Prompting)
 * - ADR: ADR-0001 (Unified Dual Architecture), ADR-0048 (Unified Tri-Interface Ingestion)
 */
@Component
public final class TuiRunner implements CommandLineRunner {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TuiRunner.class);

    /** Configuration properties instance. */
    private final OmniwrenchProperties properties;
    /** Interactive TUI dashboard component. */
    private final OmniwrenchTuiDashboard dashboard;
    /** Agent execution engine. */
    private final AgentEngine agentEngine;
    /** Session lifecycle manager. */
    private final SessionManager sessionManager;
    /** JSON object mapper. */
    private final ObjectMapper objectMapper;

    /**
     * Constructs TuiRunner with all required services.
     *
     * @param propertiesVal configuration properties, must not be null
     * @param dashboardVal interactive dashboard component, must not be null
     * @param agentEngineVal agent reasoning engine, must not be null
     * @param sessionManagerVal session manager, must not be null
     * @param objectMapperVal json mapper, must not be null
     */
    public TuiRunner(final OmniwrenchProperties propertiesVal,
                     final OmniwrenchTuiDashboard dashboardVal,
                     final AgentEngine agentEngineVal,
                     final SessionManager sessionManagerVal,
                     final ObjectMapper objectMapperVal) {
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");
        this.dashboard = Objects.requireNonNull(dashboardVal, "dashboard must not be null");
        this.agentEngine = Objects.requireNonNull(agentEngineVal, "agentEngine must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManagerVal, "sessionManager must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapperVal, "objectMapper must not be null");
    }

    @Override
    public void run(final String... args) throws Exception {
        final String[] nonNullArgs = (args != null) ? args : new String[0];
        LOGGER.info("TuiRunner inspecting startup arguments: {}", Arrays.toString(nonNullArgs));

        if (nonNullArgs.length > 0) {
            final String firstArg = nonNullArgs[0];
            if ("--help".equalsIgnoreCase(firstArg) || "-h".equalsIgnoreCase(firstArg)) {
                printUsage();
                return;
            } else if ("--version".equalsIgnoreCase(firstArg) || "-v".equalsIgnoreCase(firstArg)) {
                printVersion();
                return;
            } else if ("-p".equals(firstArg) || "--prompt".equals(firstArg)) {
                handleDirectPrompt(nonNullArgs);
                return;
            } else if ("-".equals(firstArg)) {
                handlePipedStdin(nonNullArgs);
                return;
            }
        }

        final String mode = properties.getMode();
        final boolean isCliExplicit = nonNullArgs.length > 0 && "cli".equalsIgnoreCase(nonNullArgs[0]);
        final boolean isTuiExplicit = nonNullArgs.length > 0 && "tui".equalsIgnoreCase(nonNullArgs[0]);

        if (isCliExplicit || isTuiExplicit || "tui".equalsIgnoreCase(mode)) {
            LOGGER.info("Launching full interactive TUI mode...");
            dashboard.startInteractiveLoop();
        } else {
            LOGGER.info("Omniwrench running in server/dual mode (Web UI on server port, TUI standby).");
        }
    }

    private void handleDirectPrompt(final String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Error: -p / --prompt requires a prompt text argument.");
            return;
        }

        final String promptText = args[1];
        final boolean jsonOutput = Arrays.asList(args).contains("--json");

        executeAndPrintPrompt(promptText, jsonOutput);
    }

    private void handlePipedStdin(final String[] args) throws Exception {
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }

        final String promptText = sb.toString().trim();
        if (promptText.isEmpty()) {
            System.err.println("Error: Empty prompt received on stdin.");
            return;
        }

        final boolean jsonOutput = Arrays.asList(args).contains("--json");
        executeAndPrintPrompt(promptText, jsonOutput);
    }

    private void executeAndPrintPrompt(final String promptText, final boolean jsonOutput) throws Exception {
        final SessionContext session = sessionManager.getDefaultSession();
        final AgentMessage response = agentEngine.processPrompt(session, promptText);

        if (jsonOutput) {
            final Map<String, Object> outputMap = Map.of(
                    "id", response.getId(),
                    "role", response.getRole(),
                    "content", response.getContent(),
                    "thinking", response.getThinking(),
                    "timestamp", response.getTimestamp().toString()
            );
            System.out.println(objectMapper.writeValueAsString(outputMap));
        } else {
            if (response.hasThinking()) {
                System.out.println("--- [THINKING] ---");
                System.out.println(response.getThinking());
                System.out.println("------------------");
            }
            System.out.println(response.getContent());
        }
    }

    private void printUsage() {
        System.out.println("Omniwrench - Autonomous Dual Agent Workbench");
        System.out.println("Usage:");
        System.out.println("  omniwrench                  Launch dual server & standby interactive mode");
        System.out.println("  omniwrench tui              Launch interactive cyberpunk TUI dashboard");
        System.out.println("  omniwrench -p <prompt>      Execute non-interactive single prompt");
        System.out.println("  omniwrench -p <prompt> --json Output response in structured JSON");
        System.out.println("  omniwrench -                Read prompt from stdin pipe");
        System.out.println("  omniwrench --version        Print version info");
        System.out.println("  omniwrench --help           Display this help message");
    }

    private void printVersion() {
        System.out.println("Omniwrench v0.1.0-SNAPSHOT (Java " + Runtime.version().feature() + ")");
    }
}
