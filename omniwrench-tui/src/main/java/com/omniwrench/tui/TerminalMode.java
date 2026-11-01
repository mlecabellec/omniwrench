package com.omniwrench.tui;

/**
 * Operating mode of the user's terminal session.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00100 (Dual-Mode Minimal Inline Stream &amp; Full-Screen Resizable TUI Workbench)</li>
 *   <li>Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine)</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20260822-003, TSK-20261101-016, TSK-20261101-018</li>
 *   <li>ADR: ADR-0001 (Unified Dual Architecture), ADR-0048 (Unified Tri-Interface Ingestion)</li>
 * </ul>
 */
public enum TerminalMode {

    /** Local interactive terminal (desktop console, Alacritty, Kitty, WezTerm, iTerm2, etc.). */
    STANDARD,

    /** Remote SSH session (SSH_CLIENT, SSH_CONNECTION, or SSH_TTY detected). */
    SSH,

    /** Terminal multiplexer session (tmux or GNU screen active). */
    TMUX
}
