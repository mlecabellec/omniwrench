package com.omniwrench.tui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying {@link TerminalRenderer} Cyberpunk ANSI widget rendering, status bars,
 * message bubbles, demultiplexed thinking boxes, and OpenCode-inspired side-channel cards.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00043 (Hybrid Reasoning Loop), REQ-00088 (Dual Chat Mode Reasoning)</li>
 *   <li>Requirement: REQ-00099 (XTerm-256 Color Palette TUI Visual Theming Engine), REQ-00100</li>
 *   <li>Feature: FR-00001, FR-00035, FR-00046, FR-00047</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20260822-003, TSK-20260822-007, TSK-20261101-016, TSK-20261101-018</li>
 *   <li>Rules: CS-0030.7, CS-0050.1 (non-null preconditions), CS-0060.3</li>
 *   <li>ADR: ADR-0001 (Unified Dual Architecture), ADR-0047 (Reasoning Demux)</li>
 * </ul>
 */
@Tag("REQ-00001")
@Tag("REQ-00088")
@Tag("REQ-00099")
@Tag("REQ-00100")
@Tag("FR-00001")
@Tag("FR-00035")
@Tag("FR-00047")
@Tag("UC-00001")
@Tag("TSK-20260822-003")
@Tag("TSK-20260822-007")
@Tag("TSK-20261101-018")
class TerminalRendererTest {

    /** TerminalRenderer instance under test. */
    private TerminalRenderer renderer;

    /**
     * Initializes a fresh {@link TerminalRenderer} before each test.
     */
    @BeforeEach
    void setUp() {
        // [CS-0030.7] Instantiate stateless TerminalRenderer
        renderer = new TerminalRenderer();
    }

    /**
     * Verifies that {@link TerminalRenderer#renderBanner()} outputs the full-width Cyberpunk ASCII header.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderBanner()}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}, {@code REQ-00099}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0001}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render ASCII cyberpunk banner containing project identity")
    void shouldRenderBanner() {
        // [REQ-00001, REQ-00099, FR-00001] Render banner and verify project identity strings
        final String banner = renderer.renderBanner();
        assertThat(banner).contains("O M N I W R E N C H").contains("AUTONOMOUS DUAL AGENT WORKBENCH");
    }

    /**
     * Verifies that {@link TerminalRenderer#renderBanner(TerminalEnvironment)} switches between
     * compact and full-width banners based on viewport column width.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderBanner(TerminalEnvironment)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00047}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}, {@code ADR-0013}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render compact banner when viewport is narrow")
    void shouldRenderCompactBannerWhenNarrow() {
        // [REQ-00001, REQ-00100, FR-00047] Verify narrow 60-col viewport renders single-line banner
        final TerminalEnvironment narrowEnv = new TerminalEnvironment(TerminalMode.STANDARD, "xterm", "", "", 60, 20);
        final String banner = renderer.renderBanner(narrowEnv);
        assertThat(banner).contains("OMNIWRENCH :: DUAL AGENT WORKBENCH").doesNotContain("╔════");

        // [REQ-00001, REQ-00100, FR-00047] Verify wide 120-col viewport renders full box-drawing banner
        final TerminalEnvironment wideEnv = new TerminalEnvironment(TerminalMode.STANDARD, "xterm", "", "", 120, 40);
        final String wideBanner = renderer.renderBanner(wideEnv);
        assertThat(wideBanner).contains("╔════");
    }

    /**
     * Verifies that {@link TerminalRenderer#renderStatusBar(String, int, int, int)} renders all telemetry pills.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderStatusBar(String, int, int, int)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0043} (HUD Telemetry)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render status bar with active session count, tools count, and port")
    void shouldRenderStatusBar() {
        // [REQ-00001, FR-00001, ADR-0043] Render status bar and assert mode, sessions, tools, and port pills
        final String statusBar = renderer.renderStatusBar("dual", 2, 5, 8080);
        assertThat(statusBar)
                .contains("MODE:")
                .contains("DUAL")
                .contains("SESSIONS:")
                .contains("2")
                .contains("TOOLS:")
                .contains("5 READY")
                .contains("WEB PORT:")
                .contains("8080");
    }

    /**
     * Verifies that {@link TerminalRenderer#renderStatusBar(String, int, int, int, TerminalEnvironment)}
     * prepends the appropriate `[TMUX]`, `[SSH]`, or `[LOCAL]` environment pill.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderStatusBar(String, int, int, int, TerminalEnvironment)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00047}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3} (total branching across TerminalMode)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render status bar with environment pills for TMUX, SSH, and LOCAL")
    void shouldRenderEnvironmentStatusBar() {
        // [REQ-00001, FR-00047] Verify TMUX environment pill
        final TerminalEnvironment tmuxEnv = new TerminalEnvironment(TerminalMode.TMUX, "tmux", "", "%5", 100, 30);
        final String tmuxStatus = renderer.renderStatusBar("dual", 1, 4, 8080, tmuxEnv);
        assertThat(tmuxStatus).contains("[TMUX:%5]").contains("MODE:").contains("DUAL");

        // [REQ-00001, FR-00047] Verify SSH environment pill
        final TerminalEnvironment sshEnv = new TerminalEnvironment(TerminalMode.SSH, "xterm", "10.0.0.1:22", "", 100, 30);
        final String sshStatus = renderer.renderStatusBar("headless", 1, 4, 8080, sshEnv);
        assertThat(sshStatus).contains("[SSH:10.0.0.1:22]").contains("MODE:").contains("HEADLESS");

        // [REQ-00001, FR-00047] Verify LOCAL environment pill
        final TerminalEnvironment localEnv = new TerminalEnvironment(TerminalMode.STANDARD, "kitty", "", "", 100, 30);
        final String localStatus = renderer.renderStatusBar("tui", 1, 4, 8080, localEnv);
        assertThat(localStatus).contains("[LOCAL]").contains("MODE:").contains("TUI");
    }

    /**
     * Verifies rendering of user and assistant speech bubbles in {@link TerminalRenderer#renderMessageBubble(String, String)}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderMessageBubble(String, String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render user and agent speech bubbles with appropriate styling")
    void shouldRenderMessageBubbles() {
        // [REQ-00001, FR-00001] Render user and agent message bubbles and assert headers and content
        final String userBubble = renderer.renderMessageBubble("user", "Hello World\nLine 2");
        assertThat(userBubble).contains("USER").contains("Hello World").contains("Line 2");

        final String agentBubble = renderer.renderMessageBubble("agent", "Agent Reply");
        assertThat(agentBubble).contains("AGENT / OMNIWRENCH").contains("Agent Reply");
    }

    /**
     * Verifies rendering of the demultiplexed reasoning stream box in {@link TerminalRenderer#renderThinkingBox(String)}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderThinkingBox(String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00035}, {@code REQ-00088}</li>
     *   <li><b>Rules Enforced</b>: {@code ADR-0047} (Dual Chat Mode &amp; Explicit Reasoning Demux)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render neon reasoning thinking box")
    void shouldRenderThinkingBox() {
        // [REQ-00088, FR-00035, ADR-0047] Render thinking box and verify header and multi-line thoughts
        final String thoughtBox = renderer.renderThinkingBox("Analyzing code structures...\nSynthesizing answer.");
        assertThat(thoughtBox)
                .contains("REASONING / THINKING STREAM")
                .contains("Analyzing code structures...")
                .contains("Synthesizing answer.");
    }

    /**
     * Verifies rendering of the interactive command prompt prefix in {@link TerminalRenderer#renderPromptBox(String)}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderPromptBox(String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.1}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render prompt box with custom command prefix")
    void shouldRenderPromptBox() {
        // [REQ-00001, FR-00001] Render prompt box and verify prefix and command text
        final String prompt = renderer.renderPromptBox("ls -la");
        assertThat(prompt).contains("omniwrench>").contains("ls -la");
    }

    /**
     * Verifies rendering of the OpenCode-inspired `/btw` side-channel Q&amp;A card.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderBtwBox(String, String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00047}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.1}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render OpenCode-inspired side-channel Q&A box")
    void shouldRenderBtwBox() {
        // [REQ-00100, FR-00047] Render /btw box and verify question and answer text
        final String btwBox = renderer.renderBtwBox("What is this file?", "It is the main entry point.");
        assertThat(btwBox)
                .contains("BY THE WAY / SIDE-CHANNEL: What is this file?")
                .contains("It is the main entry point.");
    }

    /**
     * Verifies rendering of the active session goal card (`/goal` command).
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderGoalCard(String)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00047}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.1}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render OpenCode-inspired goal card")
    void shouldRenderGoalCard() {
        // [REQ-00100, FR-00047] Render goal card and verify header and goal description
        final String goalCard = renderer.renderGoalCard("Refactor authentication module");
        assertThat(goalCard)
                .contains("ACTIVE SESSION GOAL")
                .contains("Refactor authentication module");
    }

    /**
     * Verifies rendering of the terminal and runtime environment diagnostic card (`/env` command).
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderEnvironmentCard(TerminalEnvironment)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code FR-00047}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0060.3}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render environment diagnostic card")
    void shouldRenderEnvironmentCard() {
        // [REQ-00001, REQ-00100, FR-00047] Render environment card for TMUX mode and verify diagnostic fields
        final TerminalEnvironment env = new TerminalEnvironment(
                TerminalMode.TMUX, "tmux-256color", "", "%1", 80, 24
        );
        final String envCard = renderer.renderEnvironmentCard(env);
        assertThat(envCard)
                .contains("TERMINAL & RUNTIME ENVIRONMENT")
                .contains("TMUX")
                .contains("Tmux Pane:")
                .contains("%1");
    }

    /**
     * Verifies rendering of the OSC 52 clipboard copy feedback badge (`/copy` command).
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: {@link TerminalRenderer#renderCopyFeedback(int)}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00047}, {@code REQ-00001}, {@code REQ-00100}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0030.1}</li>
     * </ul>
     */
    @Test
    @DisplayName("Should render copy feedback badge")
    void shouldRenderCopyFeedback() {
        // [REQ-00001, FR-00047] Render OSC 52 copy badge and verify byte count
        final String copyBadge = renderer.renderCopyFeedback(128);
        assertThat(copyBadge).contains("COPIED 128 BYTES TO CLIPBOARD VIA OSC 52");
    }

    /**
     * Verifies that all {@link TerminalRenderer} methods reject {@code null} arguments with {@link NullPointerException}.
     *
     * <p><ul>
     *   <li><b>Code Under Test</b>: All public methods on {@link TerminalRenderer}</li>
     *   <li><b>Features &amp; Requirements</b>: {@code FR-00001}, {@code REQ-00001}</li>
     *   <li><b>Rules Enforced</b>: {@code CS-0050.1} (strict non-null parameter preconditions)</li>
     * </ul>
     */
    @Test
    @DisplayName("Should reject null arguments during rendering")
    void shouldRejectNullArguments() {
        // [CS-0050.1] Verify NullPointerException on every nullable parameter across TerminalRenderer
        assertThrows(NullPointerException.class, () -> renderer.renderPromptBox(null));
        assertThrows(NullPointerException.class, () -> renderer.renderThinkingBox(null));
        assertThrows(NullPointerException.class, () -> renderer.renderMessageBubble(null, "content"));
        assertThrows(NullPointerException.class, () -> renderer.renderMessageBubble("user", null));
        assertThrows(NullPointerException.class, () -> renderer.renderBanner(null));
        assertThrows(NullPointerException.class, () -> renderer.renderStatusBar(null, 1, 1, 8080));
        assertThrows(NullPointerException.class, () -> renderer.renderStatusBar("mode", 1, 1, 8080, null));
        assertThrows(NullPointerException.class, () -> renderer.renderBtwBox(null, "answer"));
        assertThrows(NullPointerException.class, () -> renderer.renderBtwBox("q", null));
        assertThrows(NullPointerException.class, () -> renderer.renderGoalCard(null));
        assertThrows(NullPointerException.class, () -> renderer.renderEnvironmentCard(null));
    }
}
