package com.omniwrench.tui;

import com.omniwrench.model.SessionContext;

/**
 * Strategy interface for Omniwrench TUI presentation engines supporting both
 * Minimal (inline stream) and Fullscreen (resizable multi-pane) execution modes.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00099 (XTerm-256 Color Palette TUI Visual Theming Engine)</li>
 *   <li>Requirement: REQ-00100 (Dual-Mode Minimal Inline Stream &amp; Full-Screen Resizable TUI Workbench)</li>
 *   <li>Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine), FR-00048 (Dual-Mode TUI Workbench)</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20261101-016 (TUI Revamp: Minimal Skeleton &amp; Fullscreen Resizable Workbench)</li>
 *   <li>Rule: CS-0030.6.2 (Strategy Interface Pattern), CS-0050.6 (Bounded Iteration Limits)</li>
 *   <li>ADR: ADR-0063 (Dual-Mode Minimal &amp; Fullscreen Resizable TUI Architecture)</li>
 * </ul>
 */
public interface TuiViewModeEngine {

    /** Maximum interactive turns allowed per session loop to satisfy CS-0050.6 bounded loop rules. */
    int MAX_INTERACTIVE_TURNS = 100_000;

    /**
     * Returns the display mode handled by this engine.
     *
     * <p>Traceability: REQ-00100, FR-00048 (Dual-Mode TUI Strategy Identification).
     *
     * @return TuiDisplayMode constant
     */
    TuiDisplayMode getDisplayMode();

    /**
     * Starts the interactive loop for the specified session and terminal environment.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00001, FR-00048, UC-00001.
     *
     * @param session active session context, must not be null
     * @param env detected terminal environment, must not be null
     */
    void start(SessionContext session, TerminalEnvironment env);

    /**
     * Renders a complete, deterministic visual snapshot of the current UI state for the given
     * session and viewport dimensions.
     *
     * <p>Traceability: REQ-00070, REQ-00071, REQ-00100, FR-00026, FR-00048.
     *
     * @param session active session context, must not be null
     * @param env terminal environment with viewport dimensions, must not be null
     * @return rendered ANSI frame string
     */
    String renderSnapshot(SessionContext session, TerminalEnvironment env);
}
