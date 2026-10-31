package com.omniwrench.tui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying terminal mode detection (Standard, SSH, tmux) and escape sequence generation.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design &amp; Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
@Tag("REQ-00001")
@Tag("FR-00001")
@Tag("UC-00001")
@Tag("TSK-20260822-003")
class TerminalEnvironmentDetectorTest {

    private TerminalEnvironmentDetector detector;

    @BeforeEach
    void setUp() {
        detector = new TerminalEnvironmentDetector();
    }

    @Test
    @DisplayName("Should detect STANDARD mode when no tmux or SSH variables exist")
    void shouldDetectStandardMode() {
        final Map<String, String> env = Map.of(
                "TERM", "xterm-256color",
                "TERM_PROGRAM", "iTerm.app",
                "TERM_PROGRAM_VERSION", "3.5.0",
                "COLUMNS", "120",
                "LINES", "40"
        );

        final TerminalEnvironment result = detector.detect(env);

        assertThat(result.getMode()).isEqualTo(TerminalMode.STANDARD);
        assertThat(result.getTerminalProgram()).isEqualTo("iTerm.app 3.5.0");
        assertThat(result.getColumns()).isEqualTo(120);
        assertThat(result.getRows()).isEqualTo(40);
        assertThat(result.isNarrow()).isFalse();
        assertThat(result.describeEnvironment()).contains("iTerm.app 3.5.0").contains("local interactive");
    }

    @Test
    @DisplayName("Should detect SSH mode when SSH_CLIENT or SSH_CONNECTION is present")
    void shouldDetectSshMode() {
        final Map<String, String> env = Map.of(
                "SSH_CLIENT", "192.168.1.50 54321 22",
                "TERM", "xterm-256color",
                "COLUMNS", "100",
                "LINES", "30"
        );

        final TerminalEnvironment result = detector.detect(env);

        assertThat(result.getMode()).isEqualTo(TerminalMode.SSH);
        assertThat(result.getSshClient()).isEqualTo("192.168.1.50:54321");
        assertThat(result.describeEnvironment()).contains("via SSH").contains("192.168.1.50:54321");
    }

    @Test
    @DisplayName("Should detect TMUX mode when TMUX environment variable is set")
    void shouldDetectTmuxMode() {
        final Map<String, String> env = Map.of(
                "TMUX", "/tmp/tmux-1000/default,1234,0",
                "TMUX_PANE", "%3",
                "TERM", "tmux-256color",
                "COLUMNS", "60",
                "LINES", "20"
        );

        final TerminalEnvironment result = detector.detect(env);

        assertThat(result.getMode()).isEqualTo(TerminalMode.TMUX);
        assertThat(result.getTmuxPane()).isEqualTo("%3");
        assertThat(result.getColumns()).isEqualTo(60);
        assertThat(result.isNarrow()).isTrue();
        assertThat(result.describeEnvironment()).contains("in tmux (pane %3)");
    }

    @Test
    @DisplayName("Should detect TMUX mode when TERM starts with screen or tmux even without TMUX env")
    void shouldDetectTmuxModeFromTerm() {
        final Map<String, String> env = Map.of(
                "TERM", "screen-256color"
        );

        final TerminalEnvironment result = detector.detect(env);
        assertThat(result.getMode()).isEqualTo(TerminalMode.TMUX);
    }

    @Test
    @DisplayName("Should wrap OSC 52 clipboard in DCS passthrough when in TMUX mode")
    void shouldWrapOsc52InTmux() {
        final TerminalEnvironment tmuxEnv = new TerminalEnvironment(
                TerminalMode.TMUX, "tmux", "", "%1", 80, 24
        );

        final String osc52 = tmuxEnv.formatOsc52Clipboard("test payload");
        assertThat(osc52).startsWith("\u001BPtmux;\u001B").endsWith("\u001B\\");
        assertThat(osc52).contains("]52;c;");
    }

    @Test
    @DisplayName("Should format raw OSC 52 clipboard when in STANDARD or SSH mode")
    void shouldFormatRawOsc52InStandard() {
        final TerminalEnvironment standardEnv = new TerminalEnvironment(
                TerminalMode.STANDARD, "xterm", "", "", 80, 24
        );

        final String osc52 = standardEnv.formatOsc52Clipboard("test payload");
        assertThat(osc52).startsWith("\u001B]52;c;").endsWith("\u0007");
        assertThat(osc52).doesNotContain("Ptmux");
    }

    @Test
    @DisplayName("Should format window title with DCS passthrough in TMUX mode")
    void shouldFormatWindowTitleInTmux() {
        final TerminalEnvironment tmuxEnv = new TerminalEnvironment(
                TerminalMode.TMUX, "tmux", "", "%2", 80, 24
        );

        final String titleSeq = tmuxEnv.formatWindowTitle("My Title");
        assertThat(titleSeq).startsWith("\u001BPtmux;\u001B").endsWith("\u001B\\");
        assertThat(titleSeq).contains("My Title");
    }

    @Test
    @DisplayName("Should format tmux window rename escape sequence")
    void shouldFormatTmuxWindowName() {
        final TerminalEnvironment tmuxEnv = new TerminalEnvironment(
                TerminalMode.TMUX, "tmux", "", "%2", 80, 24
        );
        assertThat(tmuxEnv.formatTmuxWindowName("omniwrench")).isEqualTo("\u001Bkomniwrench\u001B\\");

        final TerminalEnvironment standardEnv = new TerminalEnvironment(
                TerminalMode.STANDARD, "xterm", "", "", 80, 24
        );
        assertThat(standardEnv.formatTmuxWindowName("omniwrench")).isEmpty();
    }

    @Test
    @DisplayName("Should fallback cleanly on missing or invalid dimension numbers")
    void shouldFallbackOnInvalidDimensions() {
        final Map<String, String> env = Map.of(
                "COLUMNS", "invalid",
                "LINES", "-5"
        );

        final TerminalEnvironment result = detector.detect(env);
        assertThat(result.getColumns()).isEqualTo(80);
        assertThat(result.getRows()).isEqualTo(24);
    }

    @Test
    @DisplayName("Should reject null environment map")
    void shouldRejectNullEnv() {
        assertThrows(NullPointerException.class, () -> detector.detect(null));
    }
}
