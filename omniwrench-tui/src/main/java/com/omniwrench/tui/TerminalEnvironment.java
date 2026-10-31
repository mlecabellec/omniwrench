package com.omniwrench.tui;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Terminal environment metadata, capabilities, and escape sequence formatting for Standard, SSH, and tmux modes.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design &amp; Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
public final class TerminalEnvironment {

    /** Default fallback column count. */
    public static final int DEFAULT_COLUMNS = 80;
    /** Default fallback row count. */
    public static final int DEFAULT_ROWS = 24;
    /** Threshold width in columns below which the terminal layout is considered narrow. */
    public static final int NARROW_THRESHOLD_COLUMNS = 80;

    /** Operating mode of the terminal session. */
    private final TerminalMode mode;
    /** Reported terminal program name or TERM value. */
    private final String terminalProgram;
    /** Remote SSH client connection string, or empty if not in SSH. */
    private final String sshClient;
    /** Tmux pane identifier, or empty if not in tmux. */
    private final String tmuxPane;
    /** Viewport width in columns. */
    private final int columns;
    /** Viewport height in rows. */
    private final int rows;

    /**
     * Constructs a TerminalEnvironment with validated parameters.
     *
     * @param modeVal operating mode, must not be null
     * @param terminalProgramVal terminal program or TERM name, must not be null
     * @param sshClientVal SSH client connection string, must not be null
     * @param tmuxPaneVal tmux pane ID, must not be null
     * @param columnsVal viewport columns count
     * @param rowsVal viewport rows count
     */
    public TerminalEnvironment(final TerminalMode modeVal,
                               final String terminalProgramVal,
                               final String sshClientVal,
                               final String tmuxPaneVal,
                               final int columnsVal,
                               final int rowsVal) {
        this.mode = Objects.requireNonNull(modeVal, "mode must not be null");
        this.terminalProgram = Objects.requireNonNull(terminalProgramVal, "terminalProgram must not be null");
        this.sshClient = Objects.requireNonNull(sshClientVal, "sshClient must not be null");
        this.tmuxPane = Objects.requireNonNull(tmuxPaneVal, "tmuxPane must not be null");
        this.columns = (columnsVal > 0) ? columnsVal : DEFAULT_COLUMNS;
        this.rows = (rowsVal > 0) ? rowsVal : DEFAULT_ROWS;
    }

    /**
     * Returns the terminal operating mode.
     *
     * @return active terminal mode
     */
    public TerminalMode getMode() {
        return mode;
    }

    /**
     * Returns the terminal program name.
     *
     * @return program name or TERM value
     */
    public String getTerminalProgram() {
        return terminalProgram;
    }

    /**
     * Returns the SSH client connection string.
     *
     * @return SSH client details, or empty string
     */
    public String getSshClient() {
        return sshClient;
    }

    /**
     * Returns the tmux pane identifier.
     *
     * @return tmux pane identifier, or empty string
     */
    public String getTmuxPane() {
        return tmuxPane;
    }

    /**
     * Returns the terminal viewport width in columns.
     *
     * @return column count
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Returns the terminal viewport height in rows.
     *
     * @return row count
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns true if the terminal viewport width is narrow (less than 80 columns).
     *
     * @return true if width is less than NARROW_THRESHOLD_COLUMNS
     */
    public boolean isNarrow() {
        return columns < NARROW_THRESHOLD_COLUMNS;
    }

    /**
     * Formats an OSC 0 / OSC 2 window title sequence, wrapped in DCS passthrough if in tmux mode.
     *
     * @param title window title string, must not be null
     * @return ANSI escape sequence to set terminal window title
     */
    public String formatWindowTitle(final String title) {
        final String nonNullTitle = Objects.requireNonNull(title, "title must not be null");
        final String rawOsc = "\u001B]0;" + nonNullTitle + "\u0007";
        if (mode == TerminalMode.TMUX) {
            // Tmux DCS passthrough: \033Ptmux;\033 + escaped OSC + \033\\
            return "\u001BPtmux;\u001B" + rawOsc.replace("\u001B", "\u001B\u001B") + "\u001B\\";
        } else {
            return rawOsc;
        }
    }

    /**
     * Formats an OSC 52 clipboard copy sequence, wrapped in DCS passthrough if in tmux mode.
     * This allows copying text directly to the remote client's clipboard over SSH and tmux tunnels.
     *
     * @param text text to copy, must not be null
     * @return ANSI escape sequence for OSC 52 clipboard write
     */
    public String formatOsc52Clipboard(final String text) {
        final String nonNullText = Objects.requireNonNull(text, "text must not be null");
        final String base64Payload = Base64.getEncoder().encodeToString(nonNullText.getBytes(StandardCharsets.UTF_8));
        final String rawOsc52 = "\u001B]52;c;" + base64Payload + "\u0007";
        if (mode == TerminalMode.TMUX) {
            return "\u001BPtmux;\u001B" + rawOsc52.replace("\u001B", "\u001B\u001B") + "\u001B\\";
        } else {
            return rawOsc52;
        }
    }

    /**
     * Formats a tmux window name sequence.
     *
     * @param windowName tmux window name, must not be null
     * @return tmux window rename escape sequence
     */
    public String formatTmuxWindowName(final String windowName) {
        final String nonNullName = Objects.requireNonNull(windowName, "windowName must not be null");
        if (mode == TerminalMode.TMUX) {
            return "\u001Bk" + nonNullName + "\u001B\\";
        } else {
            return "";
        }
    }

    /**
     * Generates a descriptive string of the current terminal and operating environment,
     * inspired by OpenCode system diagnostic reports.
     *
     * @return formatted diagnostic description
     */
    public String describeEnvironment() {
        final StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("os.name", "Unknown OS"))
                .append(" ")
                .append(System.getProperty("os.version", ""))
                .append(" (")
                .append(System.getProperty("os.arch", ""))
                .append("), ")
                .append(terminalProgram)
                .append(" [")
                .append(columns)
                .append("x")
                .append(rows)
                .append("]");

        if (mode == TerminalMode.TMUX) {
            sb.append(" in tmux");
            if (!tmuxPane.isEmpty()) {
                sb.append(" (pane ").append(tmuxPane).append(")");
            } else {
                sb.append(" (session)");
            }
        } else if (mode == TerminalMode.SSH) {
            sb.append(" via SSH");
            if (!sshClient.isEmpty()) {
                sb.append(" (from ").append(sshClient).append(")");
            } else {
                sb.append(" (remote session)");
            }
        } else {
            sb.append(" (local interactive)");
        }

        return sb.toString();
    }
}
