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
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Spring CommandLineRunner supporting non-interactive CLI prompting, stdin pipes,
 * Minimal inline TUI launching, and Full-Screen dynamically resizable TUI workbench launching.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00002 (Configurable Profiles), REQ-00089 (Unified Tri-Interface Prompting CLI, TUI, Web)</li>
 *   <li>Requirement: REQ-00100 (Dual-Mode Minimal Inline Stream &amp; Full-Screen Resizable TUI Workbench)</li>
 *   <li>Feature: FR-00001, FR-00036, FR-00047, FR-00050</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00004 (Headless CI/CD Automation Execution)</li>
 *   <li>Task: TSK-20260822-003, TSK-20260822-008, TSK-20261101-016, TSK-20261101-018</li>
 *   <li>ADR: ADR-0001, ADR-0048, ADR-0063</li>
 * </ul>
 */
@Component
public final class TuiRunner implements CommandLineRunner {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TuiRunner.class);

    /** Minimum argument count required for `-p <prompt>` flag per CS-0040.2. */
    private static final int MIN_PROMPT_ARGS = 2;

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
     * <p>Traceability: REQ-00089, REQ-00100, FR-00036, FR-00050, CS-0050.1
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
        // [REQ-00089, REQ-00100, CS-0050.1] Validate non-null dependencies
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");
        this.dashboard = Objects.requireNonNull(dashboardVal, "dashboard must not be null");
        this.agentEngine = Objects.requireNonNull(agentEngineVal, "agentEngine must not be null");
        // [REQ-00089, REQ-00100, CS-0050.1] Validate sessionManager and objectMapper
        this.sessionManager = Objects.requireNonNull(sessionManagerVal, "sessionManager must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapperVal, "objectMapper must not be null");
    }

    /**
     * Evaluates CLI arguments to dispatch one-shot prompts, stdin pipes, fullscreen TUI, minimal TUI, or server standby.
     *
     * <p>Traceability: REQ-00001, REQ-00089, REQ-00100, FR-00036, FR-00047, FR-00050, CS-0060.3
     *
     * @param args CLI command-line arguments
     * @throws Exception if CLI prompt or JSON serialization fails
     */
    @Override
    public void run(final String... args) throws Exception {
        // [REQ-00089, REQ-00100, CS-0050.1] Normalize null args array to empty array
        final String[] nonNullArgs = (args != null) ? args : new String[0];
        LOGGER.info("TuiRunner inspecting startup arguments: {}", Arrays.toString(nonNullArgs));

        if (nonNullArgs.length > 0) {
            // [REQ-00089, REQ-00100, FR-00050, CS-0060.3] Evaluate primary CLI flags and launch options
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
            } else if (hasFullscreenLaunchOption(nonNullArgs)) {
                LOGGER.info("Launching Fullscreen Resizable TUI Workbench via CLI launch option...");
                dashboard.startFullscreenLoop();
                return;
            } else if (hasMinimalLaunchOption(nonNullArgs)) {
                LOGGER.info("Launching Minimal Inline Stream TUI via CLI launch option...");
                dashboard.startMinimalLoop();
                return;
            } else {
                LOGGER.debug("Proceeding to standard mode evaluation for firstArg={}", firstArg);
            }
        }

        // [REQ-00001, REQ-00002, REQ-00100, CS-0060.3] Evaluate configured runtime mode (tui vs dual/server)
        final String mode = properties.getMode();
        final boolean isCliExplicit = nonNullArgs.length > 0 && "cli".equalsIgnoreCase(nonNullArgs[0]);
        final boolean isTuiExplicit = nonNullArgs.length > 0 && "tui".equalsIgnoreCase(nonNullArgs[0]);

        if (isCliExplicit || isTuiExplicit || "tui".equalsIgnoreCase(mode)) {
            LOGGER.info("Launching interactive TUI mode...");
            dashboard.startInteractiveLoop();
        } else {
            LOGGER.info("Omniwrench running in server/dual mode (Web UI on server port, TUI standby).");
        }
    }

    /**
     * Checks whether any CLI argument requests the Fullscreen Resizable TUI Workbench.
     *
     * <p>Traceability: REQ-00100, FR-00050
     *
     * @param args CLI arguments array
     * @return true if a fullscreen launch option is present
     */
    private boolean hasFullscreenLaunchOption(final String[] args) {
        // [REQ-00100, FR-00050] Scan arguments for fullscreen launch flags
        final List<String> argList = Arrays.asList(args);
        for (final String token : argList) {
            if ("--fullscreen".equalsIgnoreCase(token)
                    || "-f".equalsIgnoreCase(token)
                    || "fullscreen".equalsIgnoreCase(token)
                    || "tui-full".equalsIgnoreCase(token)
                    || "fs".equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether any CLI argument explicitly requests the Minimal Inline Stream TUI.
     *
     * <p>Traceability: REQ-00100, FR-00050
     *
     * @param args CLI arguments array
     * @return true if a minimal launch option is present
     */
    private boolean hasMinimalLaunchOption(final String[] args) {
        // [REQ-00100, FR-00050] Scan arguments for minimal launch flags
        final List<String> argList = Arrays.asList(args);
        for (final String token : argList) {
            if ("--minimal".equalsIgnoreCase(token)
                    || "minimal".equalsIgnoreCase(token)
                    || "tui-minimal".equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes a non-interactive `-p` / `--prompt` CLI invocation.
     *
     * <p>Traceability: REQ-00089, FR-00036, UC-00004
     *
     * @param args CLI arguments array
     * @throws Exception if prompt execution fails
     */
    private void handleDirectPrompt(final String[] args) throws Exception {
        // [REQ-00089, FR-00036] Validate presence of prompt text argument
        if (args.length < MIN_PROMPT_ARGS) {
            System.err.println("Error: -p / --prompt requires a prompt text argument.");
            return;
        }

        // [REQ-00089, FR-00036] Extract prompt and optional --json flag
        final String promptText = args[1];
        final boolean jsonOutput = Arrays.asList(args).contains("--json");

        executeAndPrintPrompt(promptText, jsonOutput);
    }

    /**
     * Reads a prompt from standard input (`-` flag) and executes it non-interactively.
     *
     * <p>Traceability: REQ-00089, FR-00036, UC-00004
     *
     * @param args CLI arguments array
     * @throws Exception if stdin reading or prompt execution fails
     */
    private void handlePipedStdin(final String[] args) throws Exception {
        // [REQ-00089, FR-00036] Read all lines from System.in UTF-8 stream
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                sb.append(line).append("\n");
                line = reader.readLine();
            }
        }

        // [REQ-00089, FR-00036] Reject empty piped input
        final String promptText = sb.toString().trim();
        if (promptText.isEmpty()) {
            System.err.println("Error: Empty prompt received on stdin.");
            return;
        }

        final boolean jsonOutput = Arrays.asList(args).contains("--json");
        executeAndPrintPrompt(promptText, jsonOutput);
    }

    /**
     * Dispatches the prompt to {@link AgentEngine} and prints either JSON or plain text output.
     *
     * <p>Traceability: REQ-00088, REQ-00089, FR-00035, FR-00036, CS-0060.3
     *
     * @param promptText prompt text to process
     * @param jsonOutput true to emit structured JSON
     * @throws Exception if JSON serialization fails
     */
    private void executeAndPrintPrompt(final String promptText, final boolean jsonOutput) throws Exception {
        // [REQ-00089, FR-00036] Process prompt against default SessionContext
        final SessionContext session = sessionManager.getDefaultSession();
        final AgentMessage response = agentEngine.processPrompt(session, promptText);

        // [REQ-00088, REQ-00089, CS-0060.3] Format response as JSON or plain text with thinking demux
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
            } else {
                LOGGER.trace("Direct prompt response contained no explicit thinking block");
            }
            System.out.println(response.getContent());
        }
    }

    /**
     * Prints CLI usage help including Minimal and Fullscreen TUI launch options.
     *
     * <p>Traceability: REQ-00089, REQ-00100, FR-00036, FR-00050
     */
    private void printUsage() {
        // [REQ-00089, REQ-00100, FR-00050] Print CLI and TUI launch flags
        System.out.println("Omniwrench - Autonomous Dual Agent Workbench");
        System.out.println("Usage:");
        System.out.println("  omniwrench                      Launch dual server & standby interactive mode");
        System.out.println("  omniwrench tui                  Launch interactive TUI (minimal stream by default)");
        System.out.println("  omniwrench tui --fullscreen     Launch Full-Screen Resizable TUI Workbench");
        System.out.println("  omniwrench --fullscreen (-f)    Launch Full-Screen Resizable TUI Workbench directly");
        // [REQ-00089, FR-00036] Print non-interactive CLI flags
        System.out.println("  omniwrench tui --minimal        Launch Minimal Inline Stream TUI explicitly");
        System.out.println("  omniwrench -p <prompt>          Execute non-interactive single prompt");
        System.out.println("  omniwrench -p <prompt> --json   Output response in structured JSON");
        System.out.println("  omniwrench -                    Read prompt from stdin pipe");
        System.out.println("  omniwrench --version            Print version info");
        System.out.println("  omniwrench --help               Display this help message");
    }

    /**
     * Prints application version and active Java runtime feature version.
     *
     * <p>Traceability: REQ-00001, FR-00001
     */
    private void printVersion() {
        // [REQ-00001, FR-00001] Print semantic version and JVM feature release
        System.out.println("Omniwrench v0.1.0-SNAPSHOT (Java " + Runtime.version().feature() + ")");
    }
}
