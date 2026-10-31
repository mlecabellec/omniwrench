package com.omniwrench.tui;

/**
 * Operating mode of the user's terminal session.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)
 * - Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design &amp; Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture), ADR-0048 (Unified Tri-Interface Ingestion)
 */
public enum TerminalMode {

    /** Local interactive terminal (desktop console, Alacritty, Kitty, WezTerm, iTerm2, etc.). */
    STANDARD,

    /** Remote SSH session (SSH_CLIENT, SSH_CONNECTION, or SSH_TTY detected). */
    SSH,

    /** Terminal multiplexer session (tmux or GNU screen active). */
    TMUX
}
