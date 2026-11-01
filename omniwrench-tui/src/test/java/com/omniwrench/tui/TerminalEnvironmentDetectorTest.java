package com.omniwrench.tui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying terminal mode detection ({@link TerminalMode#STANDARD}, {@link TerminalMode#SSH},
 * {@link TerminalMode#TMUX}) and ANSI/OSC/DCS escape sequence generation in {@link TerminalEnvironmentDetector}
 * and {@link TerminalEnvironment}.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00071 (Multi-Pane Terminal HUD), REQ-00100 (Dual-Mode Minimal &amp; Fullscreen TUI)</li>
 *   <li>Feature: FR-00001, FR-00047, FR-00048</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20260822-003, TSK-20261101-016, TSK-20261101-018</li>
 *   <li>Rules: CS-0030.7, CS-0050.1 (non-null preconditions), CS-0060.3 (total branching)</li>
 *   <li>ADR: ADR-0001 (Unified Dual Architecture), ADR-0013 (Context-Adaptive TUI Layout)</li>
 * </ul>
 */
@Tag("REQ-00001")
@Tag("REQ-00071")
@Tag("REQ-00100")
@Tag("FR-00001")
@Tag("FR-00048")
@Tag("UC-00001")
@Tag("TSK-20260822-003")
@Tag("TSK-20261101-018")
class TerminalEnvironmentDetectorTest {

    /** TerminalEnvironmentDetector instance under test. */
    private TerminalEnvironmentDetector detector;

    /**
     * Initializes a fresh {@link TerminalEnvironmentDetector} before each test.
     */
    @BeforeEach
    void setUp() {
        // [CS-0030.7] Instantiate stateless TerminalEnvironmentDetector
        detector = new TerminalEnvironmentDetector();
    }

    /**
     * Verifies detection of {@link TerminalMode#STANDARD} when no tmux or SSH variables are present.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironmentDetector#detect(Map)},
     *       {@link TerminalEnvironment#describeEnvironment()}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00048}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should detect STANDARD mode when no tmux or SSH variables exist")
    void shouldDetectStandardMode() {
        // [REQ-00001, REQ-00100, FR-00001] Provide standard iTerm2 environment map (120x40)
        final Map<String, String> env = Map.of(
                "TERM", "xterm-256color",
                "TERM_PROGRAM", "iTerm.app",
                "TERM_PROGRAM_VERSION", "3.5.0",
                "COLUMNS", "120",
                "LINES", "40"
        );

        final TerminalEnvironment result = detector.detect(env);

        // [REQ-00001, FR-00001] Verify STANDARD mode, program version, dimensions, and description
        assertThat(result.getMode()).isEqualTo(TerminalMode.STANDARD);
        assertThat(result.getTerminalProgram()).isEqualTo("iTerm.app 3.5.0");
        assertThat(result.getColumns()).isEqualTo(120);
        assertThat(result.getRows()).isEqualTo(40);
        assertThat(result.isNarrow()).isFalse();
        assertThat(result.describeEnvironment()).contains("iTerm.app 3.5.0").contains("local interactive");
    }

    /**
     * Verifies detection of {@link TerminalMode#SSH} when {@code SSH_CLIENT} is present.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironmentDetector#detect(Map)},
     *       {@link TerminalEnvironment#getSshClient()}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should detect SSH mode when SSH_CLIENT or SSH_CONNECTION is present")
    void shouldDetectSshMode() {
        // [REQ-00001, FR-00001] Provide SSH_CLIENT environment variable
        final Map<String, String> env = Map.of(
                "SSH_CLIENT", "192.168.1.50 54321 22",
                "TERM", "xterm-256color",
                "COLUMNS", "100",
                "LINES", "30"
        );

        final TerminalEnvironment result = detector.detect(env);

        // [REQ-00001, FR-00001] Verify SSH mode and formatted client host:port endpoint
        assertThat(result.getMode()).isEqualTo(TerminalMode.SSH);
        assertThat(result.getSshClient()).isEqualTo("192.168.1.50:54321");
        assertThat(result.describeEnvironment()).contains("via SSH").contains("192.168.1.50:54321");
    }

    /**
     * Verifies detection of {@link TerminalMode#TMUX} and narrow viewport classification when {@code TMUX} is set.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironmentDetector#detect(Map)},
     *       {@link TerminalEnvironment#isNarrow()}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00048}, {@code REQ-00001}, {@code REQ-00071}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}, {@code ADR-0013}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should detect TMUX mode when TMUX environment variable is set")
    void shouldDetectTmuxMode() {
        // [REQ-00001, REQ-00071] Provide TMUX and TMUX_PANE environment variables with 60x20 narrow dimensions
        final Map<String, String> env = Map.of(
                "TMUX", "/tmp/tmux-1000/default,1234,0",
                "TMUX_PANE", "%3",
                "TERM", "tmux-256color",
                "COLUMNS", "60",
                "LINES", "20"
        );

        final TerminalEnvironment result = detector.detect(env);

        // [REQ-00001, REQ-00071] Verify TMUX mode, pane ID, and narrow viewport flag
        assertThat(result.getMode()).isEqualTo(TerminalMode.TMUX);
        assertThat(result.getTmuxPane()).isEqualTo("%3");
        assertThat(result.getColumns()).isEqualTo(60);
        assertThat(result.isNarrow()).isTrue();
        assertThat(result.describeEnvironment()).contains("in tmux (pane %3)");
    }

    /**
     * Verifies detection of {@link TerminalMode#TMUX} when {@code TERM} starts with {@code screen} or {@code tmux}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironmentDetector#detect(Map)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should detect TMUX mode when TERM starts with screen or tmux even without TMUX env")
    void shouldDetectTmuxModeFromTerm() {
        // [REQ-00001, FR-00001] Provide screen-256color TERM variable
        final Map<String, String> env = Map.of(
                "TERM", "screen-256color"
        );

        final TerminalEnvironment result = detector.detect(env);
        assertThat(result.getMode()).isEqualTo(TerminalMode.TMUX);
    }

    /**
     * Verifies that {@link TerminalEnvironment#formatOsc52Clipboard(String)} wraps the OSC 52 sequence
     * in a tmux DCS passthrough envelope when in {@link TerminalMode#TMUX}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironment#formatOsc52Clipboard(String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00047}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should wrap OSC 52 clipboard in DCS passthrough when in TMUX mode")
    void shouldWrapOsc52InTmux() {
        // [REQ-00001, REQ-00100, FR-00047] Format OSC 52 clipboard sequence in TMUX mode and verify DCS passthrough
        final TerminalEnvironment tmuxEnv = new TerminalEnvironment(
                TerminalMode.TMUX, "tmux", "", "%1", 80, 24
        );

        final String osc52 = tmuxEnv.formatOsc52Clipboard("test payload");
        assertThat(osc52).startsWith("\u001BPtmux;\u001B").endsWith("\u001B\\");
        assertThat(osc52).contains("]52;c;");
    }

    /**
     * Verifies that {@link TerminalEnvironment#formatOsc52Clipboard(String)} emits raw OSC 52
     * when in {@link TerminalMode#STANDARD} mode.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironment#formatOsc52Clipboard(String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00047}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should format raw OSC 52 clipboard when in STANDARD or SSH mode")
    void shouldFormatRawOsc52InStandard() {
        // [REQ-00001, FR-00047] Format OSC 52 clipboard sequence in STANDARD mode and verify raw sequence
        final TerminalEnvironment standardEnv = new TerminalEnvironment(
                TerminalMode.STANDARD, "xterm", "", "", 80, 24
        );

        final String osc52 = standardEnv.formatOsc52Clipboard("test payload");
        assertThat(osc52).startsWith("\u001B]52;c;").endsWith("\u0007");
        assertThat(osc52).doesNotContain("Ptmux");
    }

    /**
     * Verifies that {@link TerminalEnvironment#formatWindowTitle(String)} wraps OSC 0 in tmux DCS passthrough
     * when in {@link TerminalMode#TMUX}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironment#formatWindowTitle(String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should format window title with DCS passthrough in TMUX mode")
    void shouldFormatWindowTitleInTmux() {
        // [REQ-00001, FR-00001] Format window title in TMUX mode and verify DCS passthrough wrapper
        final TerminalEnvironment tmuxEnv = new TerminalEnvironment(
                TerminalMode.TMUX, "tmux", "", "%2", 80, 24
        );

        final String titleSeq = tmuxEnv.formatWindowTitle("My Title");
        assertThat(titleSeq).startsWith("\u001BPtmux;\u001B").endsWith("\u001B\\");
        assertThat(titleSeq).contains("My Title");
    }

    /**
     * Verifies that {@link TerminalEnvironment#formatTmuxWindowName(String)} emits the tmux rename sequence
     * in {@link TerminalMode#TMUX} and an empty string in {@link TerminalMode#STANDARD}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironment#formatTmuxWindowName(String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should format tmux window rename escape sequence")
    void shouldFormatTmuxWindowName() {
        // [REQ-00001, FR-00001] Verify ESC k <name> ESC \ in TMUX mode and empty string in STANDARD mode
        final TerminalEnvironment tmuxEnv = new TerminalEnvironment(
                TerminalMode.TMUX, "tmux", "", "%2", 80, 24
        );
        assertThat(tmuxEnv.formatTmuxWindowName("omniwrench")).isEqualTo("\u001Bkomniwrench\u001B\\");

        final TerminalEnvironment standardEnv = new TerminalEnvironment(
                TerminalMode.STANDARD, "xterm", "", "", 80, 24
        );
        assertThat(standardEnv.formatTmuxWindowName("omniwrench")).isEmpty();
    }

    /**
     * Verifies that invalid or negative {@code COLUMNS} and {@code LINES} values fall back to {@code 80x24}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironmentDetector#detect(Map)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00048}, {@code REQ-00071}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0040.2} (named default constants), {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should fallback cleanly on missing or invalid dimension numbers")
    void shouldFallbackOnInvalidDimensions() {
        // [REQ-00071, REQ-00100, CS-0040.2] Provide non-numeric COLUMNS and negative LINES
        final Map<String, String> env = Map.of(
                "COLUMNS", "invalid",
                "LINES", "-5"
        );

        final TerminalEnvironment result = detector.detect(env);
        assertThat(result.getColumns()).isEqualTo(80);
        assertThat(result.getRows()).isEqualTo(24);
    }

    /**
     * Verifies that {@link TerminalEnvironmentDetector#detect(Map)} rejects a {@code null} map with {@link NullPointerException}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalEnvironmentDetector#detect(Map)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.1} (strict non-null parameter preconditions)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should reject null environment map")
    void shouldRejectNullEnv() {
        // [CS-0050.1] Verify NullPointerException when environment map is null
        assertThrows(NullPointerException.class, () -> detector.detect(null));
    }
}
