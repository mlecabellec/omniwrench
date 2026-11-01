package com.omniwrench.tui;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Terminal environment metadata, capabilities, and escape sequence formatting for Standard, SSH, and tmux modes.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00071 (Multi-Pane Terminal HUD), REQ-00100 (Dual-Mode Minimal &amp; Fullscreen TUI)</li>
 *   <li>Feature: FR-00001, FR-00047, FR-00048</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20260822-003, TSK-20261101-016, TSK-20261101-018</li>
 *   <li>ADR: ADR-0001, ADR-0013, ADR-0063</li>
 * </ul>
 */
public final class TerminalEnvironment {

    /** Default fallback column count per CS-0040.2. */
    public static final int DEFAULT_COLUMNS = 80;
    /** Default fallback row count per CS-0040.2. */
    public static final int DEFAULT_ROWS = 24;
    /** Threshold width in columns below which the terminal layout is considered narrow per CS-0040.2. */
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
     * <p>Traceability: REQ-00001, REQ-00100, FR-00001, CS-0050.1
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
        // [REQ-00001, FR-00001, CS-0050.1] Validate non-null terminal metadata parameters
        this.mode = Objects.requireNonNull(modeVal, "mode must not be null");
        this.terminalProgram = Objects.requireNonNull(terminalProgramVal, "terminalProgram must not be null");
        this.sshClient = Objects.requireNonNull(sshClientVal, "sshClient must not be null");
        this.tmuxPane = Objects.requireNonNull(tmuxPaneVal, "tmuxPane must not be null");
        // [REQ-00001, REQ-00100] Normalize non-positive dimensions to 80x24 defaults
        this.columns = columnsVal > 0 ? columnsVal : DEFAULT_COLUMNS;
        this.rows = rowsVal > 0 ? rowsVal : DEFAULT_ROWS;
    }

    /**
     * Returns a copy of this TerminalEnvironment with updated viewport dimensions.
     *
     * <p>Traceability: REQ-00100, FR-00048
     *
     * @param newColumns updated column width
     * @param newRows updated row height
     * @return new TerminalEnvironment instance with updated dimensions
     */
    public TerminalEnvironment withDimensions(final int newColumns, final int newRows) {
        // [REQ-00100, FR-00048] Construct resized immutable TerminalEnvironment for live reflow
        return new TerminalEnvironment(mode, terminalProgram, sshClient, tmuxPane, newColumns, newRows);
    }

    /**
     * Returns the terminal operating mode.
     *
     * <p>Traceability: REQ-00001, FR-00001
     *
     * @return active terminal mode
     */
    public TerminalMode getMode() {
        // [REQ-00001, FR-00001] Return detected terminal mode
        return mode;
    }

    /**
     * Returns the terminal program name.
     *
     * <p>Traceability: REQ-00001, FR-00001
     *
     * @return program name or TERM value
     */
    public String getTerminalProgram() {
        // [REQ-00001, FR-00001] Return terminal emulator program descriptor
        return terminalProgram;
    }

    /**
     * Returns the SSH client connection string.
     *
     * <p>Traceability: REQ-00001, FR-00001
     *
     * @return SSH client details, or empty string
     */
    public String getSshClient() {
        // [REQ-00001, FR-00001] Return SSH client endpoint string
        return sshClient;
    }

    /**
     * Returns the tmux pane identifier.
     *
     * <p>Traceability: REQ-00001, FR-00001
     *
     * @return tmux pane identifier, or empty string
     */
    public String getTmuxPane() {
        // [REQ-00001, FR-00001] Return tmux pane identifier
        return tmuxPane;
    }

    /**
     * Returns the terminal viewport width in columns.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     *
     * @return column count
     */
    public int getColumns() {
        // [REQ-00071, REQ-00100] Return viewport column width
        return columns;
    }

    /**
     * Returns the terminal viewport height in rows.
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     *
     * @return row count
     */
    public int getRows() {
        // [REQ-00071, REQ-00100] Return viewport row height
        return rows;
    }

    /**
     * Returns true if the terminal viewport width is narrow (less than 80 columns).
     *
     * <p>Traceability: REQ-00071, REQ-00100, FR-00048
     *
     * @return true if width is less than NARROW_THRESHOLD_COLUMNS
     */
    public boolean isNarrow() {
        // [REQ-00071, REQ-00100, FR-00048] Check whether columns fall below 80-column threshold
        return columns < NARROW_THRESHOLD_COLUMNS;
    }

    /**
     * Formats an OSC 0 / OSC 2 window title sequence, wrapped in DCS passthrough if in tmux mode.
     *
     * <p>Traceability: REQ-00001, FR-00001, CS-0050.1, CS-0060.3
     *
     * @param title window title string, must not be null
     * @return ANSI escape sequence to set terminal window title
     */
    public String formatWindowTitle(final String title) {
        // [REQ-00001, FR-00001, CS-0050.1] Validate non-null title and build OSC 0 sequence
        final String nonNullTitle = Objects.requireNonNull(title, "title must not be null");
        final String rawOsc = "\u001B]0;" + nonNullTitle + "\u0007";
        // [REQ-00001, CS-0060.3] Wrap in tmux DCS passthrough when running inside tmux
        if (mode == TerminalMode.TMUX) {
            return "\u001BPtmux;\u001B" + rawOsc.replace("\u001B", "\u001B\u001B") + "\u001B\\";
        } else {
            return rawOsc;
        }
    }

    /**
     * Formats an OSC 52 clipboard copy sequence, wrapped in DCS passthrough if in tmux mode.
     * This allows copying text directly to the remote client's clipboard over SSH and tmux tunnels.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00001, CS-0050.1, CS-0060.3
     *
     * @param text text to copy, must not be null
     * @return ANSI escape sequence for OSC 52 clipboard write
     */
    public String formatOsc52Clipboard(final String text) {
        // [REQ-00001, FR-00001, CS-0050.1] Encode clipboard payload in UTF-8 Base64 for OSC 52
        final String nonNullText = Objects.requireNonNull(text, "text must not be null");
        final String base64Payload = Base64.getEncoder().encodeToString(nonNullText.getBytes(StandardCharsets.UTF_8));
        final String rawOsc52 = "\u001B]52;c;" + base64Payload + "\u0007";
        // [REQ-00001, CS-0060.3] Wrap in tmux DCS passthrough when running inside tmux
        if (mode == TerminalMode.TMUX) {
            return "\u001BPtmux;\u001B" + rawOsc52.replace("\u001B", "\u001B\u001B") + "\u001B\\";
        } else {
            return rawOsc52;
        }
    }

    /**
     * Formats a tmux window name sequence.
     *
     * <p>Traceability: REQ-00001, FR-00001, CS-0050.1, CS-0060.3
     *
     * @param windowName tmux window name, must not be null
     * @return tmux window rename escape sequence
     */
    public String formatTmuxWindowName(final String windowName) {
        // [REQ-00001, FR-00001, CS-0050.1, CS-0060.3] Emit ESC k ... ESC \ sequence only in tmux mode
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
     * <p>Traceability: REQ-00001, REQ-00100, FR-00001, CS-0060.3
     *
     * @return formatted diagnostic description
     */
    public String describeEnvironment() {
        // [REQ-00001, FR-00001] Format OS, architecture, terminal program, and viewport dimensions
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

        // [REQ-00001, FR-00001, CS-0060.3] Append session multiplexer or SSH remote indicator
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

    /**
     * Compares this TerminalEnvironment to another object for value equality.
     *
     * <p>Traceability: REQ-00001, CS-0030.1
     *
     * @param o object to compare
     * @return true if all fields match
     */
    @Override
    public boolean equals(final Object o) {
        // [REQ-00001, CS-0030.1] Verify reference and type equality
        if (this == o) {
            return true;
        }
        if (!(o instanceof TerminalEnvironment)) {
            return false;
        }
        // [REQ-00001, CS-0030.1] Compare all immutable value fields
        final TerminalEnvironment that = (TerminalEnvironment) o;
        return columns == that.columns
                && rows == that.rows
                && mode == that.mode
                && Objects.equals(terminalProgram, that.terminalProgram)
                && Objects.equals(sshClient, that.sshClient)
                && Objects.equals(tmuxPane, that.tmuxPane);
    }

    /**
     * Computes hash code across all immutable fields.
     *
     * <p>Traceability: REQ-00001, CS-0030.1
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        // [REQ-00001, CS-0030.1] Compute hash across all immutable fields
        return Objects.hash(mode, terminalProgram, sshClient, tmuxPane, columns, rows);
    }

    /**
     * Returns diagnostic description of this terminal environment.
     *
     * <p>Traceability: REQ-00001, FR-00001
     *
     * @return diagnostic environment string
     */
    @Override
    public String toString() {
        // [REQ-00001, FR-00001] Delegate to describeEnvironment()
        return describeEnvironment();
    }
}
