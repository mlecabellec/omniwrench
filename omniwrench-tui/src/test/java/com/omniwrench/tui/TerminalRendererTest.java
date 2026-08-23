package com.omniwrench.tui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying Cyberpunk ANSI widget rendering, status bars, and message bubbles.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI & Interactive TUI Presentation Engine), REQ-00043 (Hybrid Reasoning Loop)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design & Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
@Tag("REQ-00001")
@Tag("FR-00001")
@Tag("UC-00001")
@Tag("TSK-20260822-003")
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
    @DisplayName("Should render user and agent speech bubbles with appropriate styling")
    void shouldRenderMessageBubbles() {
        final String userBubble = renderer.renderMessageBubble("user", "Hello World\nLine 2");
        assertThat(userBubble).contains("USER").contains("Hello World").contains("Line 2");

        final String agentBubble = renderer.renderMessageBubble("agent", "Agent Reply");
        assertThat(agentBubble).contains("AGENT / OMNIWRENCH").contains("Agent Reply");
    }

    @Test
    @DisplayName("Should render prompt box with custom command prefix")
    void shouldRenderPromptBox() {
        final String prompt = renderer.renderPromptBox("ls -la");
        assertThat(prompt).contains("omniwrench>").contains("ls -la");
    }

    @Test
    @DisplayName("Should reject null arguments during rendering")
    void shouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> renderer.renderPromptBox(null));
        assertThrows(NullPointerException.class, () -> renderer.renderMessageBubble(null, "content"));
        assertThrows(NullPointerException.class, () -> renderer.renderMessageBubble("user", null));
    }
}
