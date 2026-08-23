package com.omniwrench.tui;

import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * High-impact Cyberpunk terminal renderer providing ANSI styled widgets, neon banners, and status pills.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI & Interactive TUI Presentation Engine), REQ-00043 (Hybrid Reasoning Loop)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design & Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
@Component
public final class TerminalRenderer {

    /** ANSI code for resetting styles. */
    public static final String RESET = "\u001B[0m";
    /** ANSI code for bold text. */
    public static final String BOLD = "\u001B[1m";
    /** ANSI code for cyan color. */
    public static final String CYAN = "\u001B[36m";
    /** ANSI code for magenta color. */
    public static final String MAGENTA = "\u001B[35m";
    /** ANSI code for green color. */
    public static final String GREEN = "\u001B[32m";
    /** ANSI code for yellow color. */
    public static final String YELLOW = "\u001B[33m";
    /** ANSI code for red color. */
    public static final String RED = "\u001B[31m";
    /** ANSI code for bright white color. */
    public static final String BRIGHT_WHITE = "\u001B[97m";
    /** ANSI code for dark background. */
    public static final String BG_DARK = "\u001B[48;5;234m";
    /** ANSI code for cyan background. */
    public static final String BG_CYAN = "\u001B[46;30m";
    /** ANSI code for magenta background. */
    public static final String BG_MAGENTA = "\u001B[45;30m";

    /**
     * Renders ASCII banner header with Cyberpunk borders.
     *
     * @return formatted banner string
     */
    public String renderBanner() {
        return CYAN + BOLD
                + "╔══════════════════════════════════════════════════════════════════════════════╗\n"
                + "║  🛠️  O M N I W R E N C H  ::  AUTONOMOUS DUAL AGENT WORKBENCH               ║\n"
                + "║  Inspired by OpenCode & OpenClaw | Java 17+ Spring Boot                     ║\n"
                + "╚══════════════════════════════════════════════════════════════════════════════╝"
                + RESET;
    }

    /**
     * Renders telemetry status pills.
     *
     * @param mode active execution mode
     * @param activeSessions number of active sessions
     * @param toolsCount count of registered tools
     * @param port web server port
     * @return formatted status bar string
     */
    public String renderStatusBar(final String mode, final int activeSessions, final int toolsCount, final int port) {
        return BG_DARK + CYAN + " [MODE: " + BOLD + mode.toUpperCase() + RESET + BG_DARK + CYAN + "]"
                + " [SESSIONS: " + BOLD + activeSessions + RESET + BG_DARK + CYAN + "]"
                + " [TOOLS: " + BOLD + toolsCount + " READY" + RESET + BG_DARK + CYAN + "]"
                + " [WEB PORT: " + BOLD + port + RESET + BG_DARK + CYAN + "]"
                + " [JVM: " + BOLD + Runtime.version().feature() + "]" + RESET;
    }

    /**
     * Renders user command input prompt.
     *
     * @param promptText initial prompt text
     * @return formatted prompt string
     */
    public String renderPromptBox(final String promptText) {
        final String text = Objects.requireNonNull(promptText, "promptText must not be null");
        return MAGENTA + BOLD + "omniwrench> " + RESET + BRIGHT_WHITE + text;
    }

    /**
     * Renders dialogue speech bubble for user or agent.
     *
     * @param role speaker role
     * @param content message body content
     * @return formatted message bubble string
     */
    public String renderMessageBubble(final String role, final String content) {
        final String nonNullRole = Objects.requireNonNull(role, "role must not be null");
        final String nonNullContent = Objects.requireNonNull(content, "content must not be null");

        if ("user".equalsIgnoreCase(nonNullRole)) {
            return CYAN + BOLD + "┌── [USER]" + RESET + "\n"
                    + CYAN + "│ " + BRIGHT_WHITE + nonNullContent.replace("\n", "\n" + CYAN + "│ " + BRIGHT_WHITE) + "\n"
                    + CYAN + "└──" + RESET;
        } else {
            return MAGENTA + BOLD + "┌── [AGENT / OMNIWRENCH]" + RESET + "\n"
                    + MAGENTA + "│ " + BRIGHT_WHITE + nonNullContent.replace("\n", "\n" + MAGENTA + "│ " + BRIGHT_WHITE) + "\n"
                    + MAGENTA + "└──" + RESET;
        }
    }
}
