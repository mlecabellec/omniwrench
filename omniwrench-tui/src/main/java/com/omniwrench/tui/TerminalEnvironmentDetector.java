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
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design &amp; Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
@Component
public class TerminalEnvironmentDetector {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminalEnvironmentDetector.class);

    /** Default columns for standard terminals. */
    private static final int DEFAULT_COLUMNS = 80;
    /** Default rows for standard terminals. */
    private static final int DEFAULT_ROWS = 24;

    /**
     * Constructs a default TerminalEnvironmentDetector.
     */
    public TerminalEnvironmentDetector() {
    }

    /**
     * Detects terminal environment from the current host process environment.
     *
     * @return detected TerminalEnvironment instance
     */
    public TerminalEnvironment detect() {
        return detect(System.getenv());
    }

    /**
     * Detects terminal environment from a provided map of environment variables.
     *
     * @param env environment variable map, must not be null
     * @return detected TerminalEnvironment instance
     */
    public TerminalEnvironment detect(final Map<String, String> env) {
        final Map<String, String> nonNullEnv = Objects.requireNonNull(env, "env must not be null");

        final String tmuxVar = nonNullEnv.getOrDefault("TMUX", "").trim();
        final String tmuxPane = nonNullEnv.getOrDefault("TMUX_PANE", "").trim();
        final String termVar = nonNullEnv.getOrDefault("TERM", "").trim().toLowerCase(Locale.ROOT);
        final String termProgram = nonNullEnv.getOrDefault("TERM_PROGRAM", "").trim();
        final String termProgramVersion = nonNullEnv.getOrDefault("TERM_PROGRAM_VERSION", "").trim();

        final String sshClient = nonNullEnv.getOrDefault("SSH_CLIENT", "").trim();
        final String sshConnection = nonNullEnv.getOrDefault("SSH_CONNECTION", "").trim();
        final String sshTty = nonNullEnv.getOrDefault("SSH_TTY", "").trim();

        // 1. Classify operating mode: tmux > ssh > standard
        final TerminalMode mode;
        if (!tmuxVar.isEmpty() || !tmuxPane.isEmpty() || termVar.startsWith("tmux") || termVar.startsWith("screen")) {
            mode = TerminalMode.TMUX;
        } else if (!sshClient.isEmpty() || !sshConnection.isEmpty() || !sshTty.isEmpty()) {
            mode = TerminalMode.SSH;
        } else {
            mode = TerminalMode.STANDARD;
        }

        // 2. Resolve program descriptor
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

        // 3. Resolve SSH client connection details
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

        // 4. Resolve terminal dimensions
        final int columns = parsePositiveInteger(nonNullEnv.get("COLUMNS"), DEFAULT_COLUMNS);
        final int rows = parsePositiveInteger(nonNullEnv.get("LINES"), DEFAULT_ROWS);

        LOGGER.debug("Detected TerminalEnvironment: mode={}, program='{}', ssh='{}', tmuxPane='{}', dim={}x{}",
                mode, programName, effectiveSshClient, tmuxPane, columns, rows);

        return new TerminalEnvironment(mode, programName, effectiveSshClient, tmuxPane, columns, rows);
    }

    private int parsePositiveInteger(final String rawValue, final int fallback) {
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
