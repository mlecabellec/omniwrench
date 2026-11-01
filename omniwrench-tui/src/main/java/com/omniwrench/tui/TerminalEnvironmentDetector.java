package com.omniwrench.tui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Detector analyzing environment variables and terminal streams to classify Standard, SSH, and tmux modes.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00071 (Multi-Pane Terminal HUD), REQ-00100 (Dual-Mode Minimal &amp; Fullscreen TUI)</li>
 *   <li>Feature: FR-00001, FR-00047, FR-00048</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20260822-003, TSK-20261101-016, TSK-20261101-018</li>
 *   <li>ADR: ADR-0001, ADR-0013</li>
 * </ul>
 */
@Component
public class TerminalEnvironmentDetector {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminalEnvironmentDetector.class);

    /** Default columns for standard terminals per CS-0040.2. */
    private static final int DEFAULT_COLUMNS = 80;
    /** Default rows for standard terminals per CS-0040.2. */
    private static final int DEFAULT_ROWS = 24;

    /**
     * Constructs a default TerminalEnvironmentDetector.
     *
     * <p>Traceability: REQ-00001, FR-00001
     */
    public TerminalEnvironmentDetector() {
    }

    /**
     * Detects terminal environment from the current host process environment.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00001
     *
     * @return detected TerminalEnvironment instance
     */
    public TerminalEnvironment detect() {
        // [REQ-00001, FR-00001] Inspect host process System.getenv() map
        return detect(System.getenv());
    }

    /**
     * Detects terminal environment from a provided map of environment variables.
     *
     * <p>Traceability: REQ-00001, REQ-00071, REQ-00100, FR-00001, FR-00048, CS-0050.1, CS-0060.3
     *
     * @param env environment variable map, must not be null
     * @return detected TerminalEnvironment instance
     */
    public TerminalEnvironment detect(final Map<String, String> env) {
        // [REQ-00001, FR-00001, CS-0050.1] Validate non-null environment map and extract terminal variables
        final Map<String, String> nonNullEnv = Objects.requireNonNull(env, "env must not be null");
        final String tmuxVar = nonNullEnv.getOrDefault("TMUX", "").trim();
        final String tmuxPane = nonNullEnv.getOrDefault("TMUX_PANE", "").trim();
        final String termVar = nonNullEnv.getOrDefault("TERM", "").trim().toLowerCase(Locale.ROOT);
        final String termProgram = nonNullEnv.getOrDefault("TERM_PROGRAM", "").trim();
        final String termProgramVersion = nonNullEnv.getOrDefault("TERM_PROGRAM_VERSION", "").trim();

        // [REQ-00001, FR-00001] Extract SSH session environment variables
        final String sshClient = nonNullEnv.getOrDefault("SSH_CLIENT", "").trim();
        final String sshConnection = nonNullEnv.getOrDefault("SSH_CONNECTION", "").trim();
        final String sshTty = nonNullEnv.getOrDefault("SSH_TTY", "").trim();

        // [REQ-00001, FR-00001, CS-0060.3] 1. Classify operating mode: tmux > ssh > standard
        final TerminalMode mode;
        if (!tmuxVar.isEmpty() || !tmuxPane.isEmpty() || termVar.startsWith("tmux") || termVar.startsWith("screen")) {
            mode = TerminalMode.TMUX;
        } else if (!sshClient.isEmpty() || !sshConnection.isEmpty() || !sshTty.isEmpty()) {
            mode = TerminalMode.SSH;
        } else {
            mode = TerminalMode.STANDARD;
        }

        // [REQ-00001, FR-00001, CS-0060.3] 2. Resolve terminal program descriptor
        final String programName;
        if (!termProgram.isEmpty()) {
            if (!termProgramVersion.isEmpty()) {
                programName = termProgram + " " + termProgramVersion;
            } else {
                programName = termProgram;
            }
        } else if (!termVar.isEmpty()) {
            programName = termVar;
        } else {
            programName = "xterm-256color";
        }

        // [REQ-00001, FR-00001, CS-0060.3] 3. Resolve SSH client connection details
        final String effectiveSshClient;
        if (!sshClient.isEmpty()) {
            final String[] parts = sshClient.split("\\s+");
            if (parts.length >= 2) {
                effectiveSshClient = parts[0] + ":" + parts[1];
            } else {
                effectiveSshClient = sshClient;
            }
        } else if (!sshConnection.isEmpty()) {
            final String[] parts = sshConnection.split("\\s+");
            if (parts.length >= 2) {
                effectiveSshClient = parts[0] + ":" + parts[1];
            } else {
                effectiveSshClient = sshConnection;
            }
        } else {
            effectiveSshClient = "";
        }

        // [REQ-00071, REQ-00100, FR-00048] 4. Resolve terminal viewport dimensions
        final int columns = parsePositiveInteger(nonNullEnv.get("COLUMNS"), DEFAULT_COLUMNS);
        final int rows = parsePositiveInteger(nonNullEnv.get("LINES"), DEFAULT_ROWS);

        LOGGER.debug("Detected TerminalEnvironment: mode={}, program='{}', ssh='{}', tmuxPane='{}', dim={}x{}",
                mode, programName, effectiveSshClient, tmuxPane, columns, rows);

        return new TerminalEnvironment(mode, programName, effectiveSshClient, tmuxPane, columns, rows);
    }

    /**
     * Parses a positive integer string or returns the fallback constant if missing/invalid.
     *
     * <p>Traceability: REQ-00071, REQ-00100, CS-0060.3
     *
     * @param rawValue raw string value from environment map
     * @param fallback fallback integer value
     * @return positive parsed integer or fallback
     */
    private int parsePositiveInteger(final String rawValue, final int fallback) {
        // [REQ-00071, REQ-00100, CS-0060.3] Parse positive integer with total branching
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return fallback;
        } else {
            try {
                final int parsed = Integer.parseInt(rawValue.trim());
                if (parsed > 0) {
                    return parsed;
                } else {
                    return fallback;
                }
            } catch (final NumberFormatException ex) {
                return fallback;
            }
        }
    }
}
