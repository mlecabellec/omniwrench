package com.omniwrench.tui;

import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * High-impact Cyberpunk terminal renderer providing ANSI styled widgets, neon banners,
 * environment-adaptive status bars, and OpenCode-inspired side-channel cards.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine), REQ-00088 (Dual Chat Mode Reasoning)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design), TSK-20260822-007 (Dual Chat Mode &amp; Thinking Demux)
 * - ADR: ADR-0001 (Unified Dual Architecture), ADR-0047 (Dual Chat Mode &amp; Explicit Reasoning Demux)
 */
@Component
public final class TerminalRenderer {

    /** ANSI code for resetting styles. */
    public static final String RESET = "\u001B[0m";
    /** ANSI code for bold text. */
    public static final String BOLD = "\u001B[1m";
    /** ANSI code for dim text. */
    public static final String DIM = "\u001B[2m";
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
    /** ANSI code for gray color. */
    public static final String GRAY = "\u001B[90m";
    /** ANSI code for dark background. */
    public static final String BG_DARK = "\u001B[48;5;234m";
    /** ANSI code for cyan background. */
    public static final String BG_CYAN = "\u001B[46;30m";
    /** ANSI code for magenta background. */
    public static final String BG_MAGENTA = "\u001B[45;30m";
    /** ANSI code for green background. */
    public static final String BG_GREEN = "\u001B[42;30m";
    /** ANSI code for yellow background. */
    public static final String BG_YELLOW = "\u001B[43;30m";

    /**
     * Renders standard wide ASCII banner header with Cyberpunk borders.
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
     * Renders an environment-adaptive ASCII banner header, switching to compact layout if narrow.
     *
     * @param env detected terminal environment, must not be null
     * @return formatted banner string
     */
    public String renderBanner(final TerminalEnvironment env) {
        final TerminalEnvironment nonNullEnv = Objects.requireNonNull(env, "env must not be null");
        if (nonNullEnv.isNarrow()) {
            return CYAN + BOLD + "⚡ OMNIWRENCH :: DUAL AGENT WORKBENCH ⚡" + RESET;
        } else {
            return renderBanner();
        }
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
        final String nonNullMode = Objects.requireNonNull(mode, "mode must not be null");
        return BG_DARK + CYAN + " [MODE: " + BOLD + nonNullMode.toUpperCase() + RESET + BG_DARK + CYAN + "]"
                + " [SESSIONS: " + BOLD + activeSessions + RESET + BG_DARK + CYAN + "]"
                + " [TOOLS: " + BOLD + toolsCount + " READY" + RESET + BG_DARK + CYAN + "]"
                + " [WEB PORT: " + BOLD + port + RESET + BG_DARK + CYAN + "]"
                + " [JVM: " + BOLD + Runtime.version().feature() + "]" + RESET;
    }

    /**
     * Renders telemetry status pills with environment pill (Standard, SSH, or TMUX).
     *
     * @param mode active execution mode, must not be null
     * @param activeSessions number of active sessions
     * @param toolsCount count of registered tools
     * @param port web server port
     * @param env terminal environment metadata, must not be null
     * @return formatted environment-aware status bar string
     */
    public String renderStatusBar(final String mode,
                                  final int activeSessions,
                                  final int toolsCount,
                                  final int port,
                                  final TerminalEnvironment env) {
        final String nonNullMode = Objects.requireNonNull(mode, "mode must not be null");
        final TerminalEnvironment nonNullEnv = Objects.requireNonNull(env, "env must not be null");

        final String envPill;
        if (nonNullEnv.getMode() == TerminalMode.TMUX) {
            final String pane = nonNullEnv.getTmuxPane();
            final String paneLabel = pane.isEmpty() ? "TMUX" : "TMUX:" + pane;
            envPill = BG_MAGENTA + " [" + paneLabel + "] " + RESET + " ";
        } else if (nonNullEnv.getMode() == TerminalMode.SSH) {
            final String client = nonNullEnv.getSshClient();
            final String clientLabel = client.isEmpty() ? "SSH" : "SSH:" + client;
            envPill = BG_YELLOW + " [" + clientLabel + "] " + RESET + " ";
        } else {
            envPill = BG_GREEN + " [LOCAL] " + RESET + " ";
        }

        return envPill + BG_DARK + CYAN
                + " [MODE: " + BOLD + nonNullMode.toUpperCase() + RESET + BG_DARK + CYAN + "]"
                + " [SESSIONS: " + BOLD + activeSessions + RESET + BG_DARK + CYAN + "]"
                + " [TOOLS: " + BOLD + toolsCount + " READY" + RESET + BG_DARK + CYAN + "]"
                + " [WEB PORT: " + BOLD + port + RESET + BG_DARK + CYAN + "]"
                + " [JVM: " + BOLD + Runtime.version().feature() + "]" + RESET;
    }

    /**
     * Renders user command input prompt.
     *
     * @param promptText initial prompt text, must not be null
     * @return formatted prompt string
     */
    public String renderPromptBox(final String promptText) {
        final String text = Objects.requireNonNull(promptText, "promptText must not be null");
        return MAGENTA + BOLD + "omniwrench> " + RESET + BRIGHT_WHITE + text;
    }

    /**
     * Renders an expandable/dimmed neon reasoning thinking box.
     *
     * @param thoughtContent internal reasoning thoughts, must not be null
     * @return formatted cyberpunk thought box
     */
    public String renderThinkingBox(final String thoughtContent) {
        final String nonNullThought = Objects.requireNonNull(thoughtContent, "thoughtContent must not be null");
        return YELLOW + BOLD + "┌── [REASONING / THINKING STREAM]" + RESET + "\n"
                + YELLOW + "│ " + GRAY + nonNullThought.replace("\n", "\n" + YELLOW + "│ " + GRAY) + "\n"
                + YELLOW + "└──" + RESET;
    }

    /**
     * Renders dialogue speech bubble for user or agent.
     *
     * @param role speaker role, must not be null
     * @param content message body content, must not be null
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

    /**
     * Renders an OpenCode-inspired side-channel ephemeral Q&amp;A box (/btw command).
     *
     * @param question user question, must not be null
     * @param answer side-channel answer, must not be null
     * @return formatted side-channel Q&amp;A box
     */
    public String renderBtwBox(final String question, final String answer) {
        final String nonNullQuestion = Objects.requireNonNull(question, "question must not be null");
        final String nonNullAnswer = Objects.requireNonNull(answer, "answer must not be null");

        return CYAN + BOLD + "┌── [BY THE WAY / SIDE-CHANNEL: " + nonNullQuestion + "]" + RESET + "\n"
                + CYAN + "│ " + BRIGHT_WHITE + nonNullAnswer.replace("\n", "\n" + CYAN + "│ " + BRIGHT_WHITE) + "\n"
                + CYAN + "└──" + RESET;
    }

    /**
     * Renders an OpenCode-inspired active goal card (/goal command).
     *
     * @param currentGoal current session goal, must not be null
     * @return formatted goal card string
     */
    public String renderGoalCard(final String currentGoal) {
        final String nonNullGoal = Objects.requireNonNull(currentGoal, "currentGoal must not be null");
        return MAGENTA + BOLD + "┌── [ACTIVE SESSION GOAL]" + RESET + "\n"
                + MAGENTA + "│ " + BRIGHT_WHITE + nonNullGoal.replace("\n", "\n" + MAGENTA + "│ " + BRIGHT_WHITE) + "\n"
                + MAGENTA + "└──" + RESET;
    }

    /**
     * Renders an OpenCode-inspired environment diagnostic card (/env command).
     *
     * @param env detected terminal environment, must not be null
     * @return formatted environment diagnostic card
     */
    public String renderEnvironmentCard(final TerminalEnvironment env) {
        final TerminalEnvironment nonNullEnv = Objects.requireNonNull(env, "env must not be null");
        final StringBuilder sb = new StringBuilder();
        sb.append(GREEN).append(BOLD).append("┌── [TERMINAL & RUNTIME ENVIRONMENT]").append(RESET).append("\n");
        sb.append(GREEN).append("│ ").append(BRIGHT_WHITE).append("Mode:           ").append(nonNullEnv.getMode()).append("\n");
        sb.append(GREEN).append("│ ").append(BRIGHT_WHITE).append("Viewport:       ").append(nonNullEnv.getColumns())
                .append("x").append(nonNullEnv.getRows()).append(nonNullEnv.isNarrow() ? " (narrow layout)" : " (standard layout)").append("\n");
        sb.append(GREEN).append("│ ").append(BRIGHT_WHITE).append("Program:        ").append(nonNullEnv.getTerminalProgram()).append("\n");

        if (nonNullEnv.getMode() == TerminalMode.TMUX) {
            sb.append(GREEN).append("│ ").append(BRIGHT_WHITE).append("Tmux Pane:      ")
                    .append(nonNullEnv.getTmuxPane().isEmpty() ? "none" : nonNullEnv.getTmuxPane()).append("\n");
            sb.append(GREEN).append("│ ").append(BRIGHT_WHITE).append("DCS Passthru:   Active (wrapped \\033Ptmux;...\\033\\\\)\n");
        } else if (nonNullEnv.getMode() == TerminalMode.SSH) {
            sb.append(GREEN).append("│ ").append(BRIGHT_WHITE).append("SSH Client:     ")
                    .append(nonNullEnv.getSshClient().isEmpty() ? "remote" : nonNullEnv.getSshClient()).append("\n");
        } else {
            sb.append(GREEN).append("│ ").append(BRIGHT_WHITE).append("Session:        Local desktop / direct console\n");
        }

        sb.append(GREEN).append("│ ").append(BRIGHT_WHITE).append("OS & Arch:      ")
                .append(System.getProperty("os.name", "Unknown")).append(" ")
                .append(System.getProperty("os.version", "")).append(" (")
                .append(System.getProperty("os.arch", "")).append(")\n");
        sb.append(GREEN).append("│ ").append(BRIGHT_WHITE).append("Java Runtime:   ")
                .append(System.getProperty("java.runtime.version", Runtime.version().toString())).append("\n");
        sb.append(GREEN).append("└──").append(RESET);

        return sb.toString();
    }

    /**
     * Renders feedback badge when content is copied to client clipboard via OSC 52.
     *
     * @param byteCount number of bytes copied
     * @return formatted copy feedback badge
     */
    public String renderCopyFeedback(final int byteCount) {
        return BG_CYAN + " [COPIED " + byteCount + " BYTES TO CLIPBOARD VIA OSC 52] " + RESET;
    }
}
