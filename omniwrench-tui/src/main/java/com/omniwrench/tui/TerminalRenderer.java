package com.omniwrench.tui;

import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * High-impact Cyberpunk terminal renderer providing ANSI styled widgets, neon banners, and status pills.
 * 
 * Traceability:
 * - Requirement: REQ-00040 (Modern Cyberpunk Terminal UI Styling)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design & Integration)
 */
@Component
public class TerminalRenderer {

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String CYAN = "\u001B[36m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";
    public static final String BRIGHT_WHITE = "\u001B[97m";
    public static final String BG_DARK = "\u001B[48;5;234m";
    public static final String BG_CYAN = "\u001B[46;30m";
    public static final String BG_MAGENTA = "\u001B[45;30m";

    public String renderBanner() {
        return CYAN + BOLD
                + "╔══════════════════════════════════════════════════════════════════════════════╗\n"
                + "║  🛠️  O M N I W R E N C H  ::  AUTONOMOUS DUAL AGENT WORKBENCH               ║\n"
                + "║  Inspired by OpenCode & OpenClaw | Java 17+ Spring Boot                     ║\n"
                + "╚══════════════════════════════════════════════════════════════════════════════╝"
                + RESET;
    }

    public String renderStatusBar(final String mode, final int activeSessions, final int toolsCount, final int port) {
        return BG_DARK + CYAN + " [MODE: " + BOLD + mode.toUpperCase() + RESET + BG_DARK + CYAN + "]"
                + " [SESSIONS: " + BOLD + activeSessions + RESET + BG_DARK + CYAN + "]"
                + " [TOOLS: " + BOLD + toolsCount + " READY" + RESET + BG_DARK + CYAN + "]"
                + " [WEB PORT: " + BOLD + port + RESET + BG_DARK + CYAN + "]"
                + " [JVM: " + BOLD + Runtime.version().feature() + "]" + RESET;
    }

    public String renderPromptBox(final String promptText) {
        final String text = Objects.requireNonNull(promptText, "promptText must not be null");
        return MAGENTA + BOLD + "omniwrench> " + RESET + BRIGHT_WHITE + text;
    }

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
