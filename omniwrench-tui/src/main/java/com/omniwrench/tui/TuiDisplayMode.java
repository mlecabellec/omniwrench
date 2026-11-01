package com.omniwrench.tui;

import java.util.Locale;
import java.util.Objects;

/**
 * Presentation layout mode for the Omniwrench Terminal User Interface (TUI).
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00099 (XTerm-256 Color Palette TUI Visual Theming Engine)</li>
 *   <li>Requirement: REQ-00100 (Dual-Mode Minimal Inline Stream &amp; Full-Screen Resizable TUI Workbench)</li>
 *   <li>Feature: FR-00001 (Dual Headless &amp; Interactive Presentation Engine), FR-00048 (Dual-Mode TUI Workbench)</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20261101-016 (TUI Revamp: Minimal Skeleton &amp; Fullscreen Resizable Workbench)</li>
 *   <li>ADR: ADR-0063 (Dual-Mode Minimal &amp; Fullscreen Resizable TUI Architecture)</li>
 * </ul>
 */
public enum TuiDisplayMode {

    /**
     * Inline stream-oriented minimal TUI mode preserving native terminal scrollback buffer,
     * inspired by Claude Code, Aider, and Antigravity CLI inline mode (REQ-00100, FR-00048).
     */
    MINIMAL("minimal"),

    /**
     * Full-screen dynamically resizable multi-pane TUI mode with Home/Session routes,
     * collapsible sidebars, and modal command palette overlays, inspired by OpenCode,
     * Antigravity CLI, OpenClaw, and Crush (REQ-00100, FR-00048).
     */
    FULLSCREEN("fullscreen");

    /** Lowercase configuration key for this display mode (REQ-00002, FR-00009). */
    private final String configKey;

    /**
     * Constructs a TuiDisplayMode enum constant with its configuration key.
     *
     * @param configKeyVal lowercase configuration key, must not be null
     */
    TuiDisplayMode(final String configKeyVal) {
        // [REQ-00010, CS-0030.1.1] Validate non-null configuration key invariant
        this.configKey = Objects.requireNonNull(configKeyVal, "configKeyVal must not be null");
    }

    /**
     * Returns the canonical configuration key ({@code minimal} or {@code fullscreen}).
     *
     * <p>Traceability: REQ-00002 (Configurable Runtime Profiles), FR-00048 (Dual-Mode TUI Workbench).
     *
     * @return configuration key string
     */
    public String getConfigKey() {
        // [REQ-00002, FR-00048] Return canonical lowercase property token
        return configKey;
    }

    /**
     * Resolves a {@link TuiDisplayMode} from a configuration or CLI string token.
     *
     * <p>Traceability: REQ-00001, REQ-00100, FR-00048, CS-0060.3 (Total Branching).
     *
     * @param rawValue mode name or CLI launch flag, may be null (defaults to MINIMAL)
     * @return resolved TuiDisplayMode (defaults to MINIMAL if null or unrecognized)
     */
    public static TuiDisplayMode fromString(final String rawValue) {
        // [REQ-00100, FR-00048] Default null configuration token safely to MINIMAL mode
        if (rawValue == null) {
            return MINIMAL;
        }
        final String normalized = rawValue.trim().toLowerCase(Locale.ROOT);

        // [REQ-00100, FR-00048, CS-0060.3] Classify fullscreen vs minimal CLI/config tokens with total branching
        if ("fullscreen".equals(normalized)
                || "full".equals(normalized)
                || "fs".equals(normalized)
                || "--fullscreen".equals(normalized)
                || "-f".equals(normalized)
                || "tui-full".equals(normalized)) {
            return FULLSCREEN;
        } else if ("minimal".equals(normalized)
                || "min".equals(normalized)
                || "inline".equals(normalized)
                || "--minimal".equals(normalized)) {
            return MINIMAL;
        } else {
            return MINIMAL;
        }
    }
}
