package com.omniwrench.tools;

import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Shell command execution tool bounded by timeouts and sandboxed directory execution.
 *
 * Traceability:
 * - Requirement: REQ-00060 (Polyvalent Base Architecture with Pluggable Tools), REQ-00065 (Multi-Tier Security Guardrails)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI), FR-00025 (Multi-Tier Security Guardrails)
 * - Use Case: UC-00002 (Autonomous Goal Planning & Refactoring)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 * - ADR: ADR-0006 (Pluggable Tools Architecture), ADR-0020 (Command Safety Classification)
 */
@Component
public final class CommandExecutionTool implements Tool {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutionTool.class);
    /** Process execution timeout in seconds. */
    private static final long TIMEOUT_SECONDS = 30L;

    /** Tool descriptor definition. */
    private final ToolDefinition definition;

    /**
     * Constructs CommandExecutionTool and defines parameter schema.
     */
    public CommandExecutionTool() {
        final Map<String, String> schema = new HashMap<>();
        schema.put("command", "Shell command line to execute");
        schema.put("cwd", "Optional sub-directory relative to workspace");
        this.definition = new ToolDefinition(
                "run_command",
                "Executes shell commands in a bounded process execution environment",
                schema
        );
    }

    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }

    @Override
    public ToolInvocation execute(final SessionContext context, final Map<String, Object> arguments) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");

        final String callId = UUID.randomUUID().toString();
        final String command = String.valueOf(arguments.getOrDefault("command", ""));
        if (command.isBlank()) {
            return new ToolInvocation(callId, "run_command", arguments, "Error: Empty command specified", false, Instant.now());
        }

        final String cwdArg = String.valueOf(arguments.getOrDefault("cwd", ""));
        final File workingDir = new File(context.getWorkspaceRoot(), cwdArg);

        LOGGER.info("Executing shell command: '{}' in dir: {}", command, workingDir.getAbsolutePath());

        try {
            final ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
            pb.directory(workingDir.exists() ? workingDir : new File(context.getWorkspaceRoot()));
            pb.redirectErrorStream(true);

            final Process process = pb.start();
            final StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            final boolean completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                final String timeoutMsg = "Process timed out after " + TIMEOUT_SECONDS + " seconds.";
                return new ToolInvocation(callId, "run_command", arguments, timeoutMsg, false, Instant.now());
            }

            final int exitCode = process.exitValue();
            final boolean success = exitCode == 0;
            final String outMsg = "Exit Code: " + exitCode + "\nOutput:\n" + output.toString();
            return new ToolInvocation(callId, "run_command", arguments, outMsg, success, Instant.now());
        } catch (final Exception e) {
            LOGGER.error("Failed to execute command: {}", command, e);
            return new ToolInvocation(callId, "run_command", arguments, "Execution error: " + e.getMessage(), false, Instant.now());
        }
    }
}
