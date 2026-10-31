package com.omniwrench.tui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying Cyberpunk ANSI widget rendering, status bars, message bubbles,
 * thinking boxes, and OpenCode-inspired side-channel cards.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine), REQ-00043 (Hybrid Reasoning Loop), REQ-00088 (Dual Chat Mode Reasoning)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design), TSK-20260822-007 (Dual Chat Mode &amp; Thinking Demux)
 * - ADR: ADR-0001 (Unified Dual Architecture), ADR-0047 (Reasoning Demux)
 */
@Tag("REQ-00001")
@Tag("REQ-00088")
@Tag("FR-00001")
@Tag("UC-00001")
@Tag("TSK-20260822-003")
@Tag("TSK-20260822-007")
class TerminalRendererTest {

    private TerminalRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new TerminalRenderer();
    }

    @Test
    @DisplayName("Should render ASCII cyberpunk banner containing project identity")
    void shouldRenderBanner() {
        final String banner = renderer.renderBanner();
        assertThat(banner).contains("O M N I W R E N C H").contains("AUTONOMOUS DUAL AGENT WORKBENCH");
    }

    @Test
    @DisplayName("Should render compact banner when viewport is narrow")
    void shouldRenderCompactBannerWhenNarrow() {
        final TerminalEnvironment narrowEnv = new TerminalEnvironment(TerminalMode.STANDARD, "xterm", "", "", 60, 20);
        final String banner = renderer.renderBanner(narrowEnv);
        assertThat(banner).contains("OMNIWRENCH :: DUAL AGENT WORKBENCH").doesNotContain("╔════");

        final TerminalEnvironment wideEnv = new TerminalEnvironment(TerminalMode.STANDARD, "xterm", "", "", 120, 40);
        final String wideBanner = renderer.renderBanner(wideEnv);
        assertThat(wideBanner).contains("╔════");
    }

    @Test
    @DisplayName("Should render status bar with active session count, tools count, and port")
    void shouldRenderStatusBar() {
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

    @Test
    @DisplayName("Should render status bar with environment pills for TMUX, SSH, and LOCAL")
    void shouldRenderEnvironmentStatusBar() {
        final TerminalEnvironment tmuxEnv = new TerminalEnvironment(TerminalMode.TMUX, "tmux", "", "%5", 100, 30);
        final String tmuxStatus = renderer.renderStatusBar("dual", 1, 4, 8080, tmuxEnv);
        assertThat(tmuxStatus).contains("[TMUX:%5]").contains("MODE:").contains("DUAL");

        final TerminalEnvironment sshEnv = new TerminalEnvironment(TerminalMode.SSH, "xterm", "10.0.0.1:22", "", 100, 30);
        final String sshStatus = renderer.renderStatusBar("headless", 1, 4, 8080, sshEnv);
        assertThat(sshStatus).contains("[SSH:10.0.0.1:22]").contains("MODE:").contains("HEADLESS");

        final TerminalEnvironment localEnv = new TerminalEnvironment(TerminalMode.STANDARD, "kitty", "", "", 100, 30);
        final String localStatus = renderer.renderStatusBar("tui", 1, 4, 8080, localEnv);
        assertThat(localStatus).contains("[LOCAL]").contains("MODE:").contains("TUI");
    }

    @Test
    @DisplayName("Should render user and agent speech bubbles with appropriate styling")
    void shouldRenderMessageBubbles() {
        final String userBubble = renderer.renderMessageBubble("user", "Hello World\nLine 2");
        assertThat(userBubble).contains("USER").contains("Hello World").contains("Line 2");

        final String agentBubble = renderer.renderMessageBubble("agent", "Agent Reply");
        assertThat(agentBubble).contains("AGENT / OMNIWRENCH").contains("Agent Reply");
    }

    @Test
    @DisplayName("Should render neon reasoning thinking box")
    void shouldRenderThinkingBox() {
        final String thoughtBox = renderer.renderThinkingBox("Analyzing code structures...\nSynthesizing answer.");
        assertThat(thoughtBox)
                .contains("REASONING / THINKING STREAM")
                .contains("Analyzing code structures...")
                .contains("Synthesizing answer.");
    }

    @Test
    @DisplayName("Should render prompt box with custom command prefix")
    void shouldRenderPromptBox() {
        final String prompt = renderer.renderPromptBox("ls -la");
        assertThat(prompt).contains("omniwrench>").contains("ls -la");
    }

    @Test
    @DisplayName("Should render OpenCode-inspired side-channel Q&A box")
    void shouldRenderBtwBox() {
        final String btwBox = renderer.renderBtwBox("What is this file?", "It is the main entry point.");
        assertThat(btwBox)
                .contains("BY THE WAY / SIDE-CHANNEL: What is this file?")
                .contains("It is the main entry point.");
    }

    @Test
    @DisplayName("Should render OpenCode-inspired goal card")
    void shouldRenderGoalCard() {
        final String goalCard = renderer.renderGoalCard("Refactor authentication module");
        assertThat(goalCard)
                .contains("ACTIVE SESSION GOAL")
                .contains("Refactor authentication module");
    }

    @Test
    @DisplayName("Should render environment diagnostic card")
    void shouldRenderEnvironmentCard() {
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

    @Test
    @DisplayName("Should render copy feedback badge")
    void shouldRenderCopyFeedback() {
        final String copyBadge = renderer.renderCopyFeedback(128);
        assertThat(copyBadge).contains("COPIED 128 BYTES TO CLIPBOARD VIA OSC 52");
    }

    @Test
    @DisplayName("Should reject null arguments during rendering")
    void shouldRejectNullArguments() {
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
