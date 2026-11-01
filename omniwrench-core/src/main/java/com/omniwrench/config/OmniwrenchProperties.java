package com.omniwrench.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Configuration properties for Omniwrench runtime environment and subsystem profiles.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00002 (Configurable Runtime Profiles)</li>
 *   <li>Requirement: REQ-00099 (XTerm-256 Color Palette TUI Visual Theming Engine)</li>
 *   <li>Requirement: REQ-00100 (Dual-Mode Minimal Inline Stream &amp; Full-Screen Resizable TUI Workbench)</li>
 *   <li>Feature: FR-00009 (Hierarchical Configuration &amp; AES-256 Vault), FR-00046, FR-00047, FR-00050</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20260822-001, TSK-20261101-016, TSK-20261101-018</li>
 *   <li>ADR: ADR-0011 (Configurable Profiles), ADR-0031 (Configuration Layering), ADR-0063 (Dual-Mode TUI)</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "omniwrench")
public class OmniwrenchProperties {

    /** Default runtime execution mode (`dual`, `tui`, `web`, `headless`). */
    private String mode = "dual";
    /** Root workspace directory path. */
    private String workspacePath = ".";
    /** Nested TUI presentation properties. */
    private final TuiProperties tui = new TuiProperties();
    /** Nested autonomous engine execution properties. */
    private final EngineProperties engine = new EngineProperties();

    /**
     * Returns the active execution mode (e.g., dual, tui, headless).
     *
     * <p>Traceability: REQ-00002, FR-00009, CS-0030.1
     *
     * @return the execution mode
     */
    public String getMode() {
        // [REQ-00002, FR-00009] Return configured runtime mode
        return mode;
    }

    /**
     * Sets the active execution mode.
     *
     * <p>Traceability: REQ-00002, FR-00009, CS-0050.1
     *
     * @param modeVal the execution mode value, must not be null
     */
    public void setMode(final String modeVal) {
        // [REQ-00002, FR-00009, CS-0050.1] Enforce non-null mode precondition
        this.mode = Objects.requireNonNull(modeVal, "mode must not be null");
    }

    /**
     * Returns the workspace root path.
     *
     * <p>Traceability: REQ-00002, FR-00009, CS-0030.1
     *
     * @return the workspace root path
     */
    public String getWorkspacePath() {
        // [REQ-00002, FR-00009] Return configured workspace root directory
        return workspacePath;
    }

    /**
     * Sets the workspace root path.
     *
     * <p>Traceability: REQ-00002, FR-00009, CS-0050.1
     *
     * @param workspacePathVal the workspace root directory path, must not be null
     */
    public void setWorkspacePath(final String workspacePathVal) {
        // [REQ-00002, FR-00009, CS-0050.1] Enforce non-null workspace path precondition
        this.workspacePath = Objects.requireNonNull(workspacePathVal, "workspacePath must not be null");
    }

    /**
     * Returns the TUI visual configuration properties.
     *
     * <p>Traceability: REQ-00099, REQ-00100, FR-00046, FR-00047
     *
     * @return TUI configuration properties
     */
    public TuiProperties getTui() {
        // [REQ-00100, FR-00047] Return nested TUI configuration block
        return tui;
    }

    /**
     * Returns the agent engine execution properties.
     *
     * <p>Traceability: REQ-00030, FR-00003, CS-0030.1
     *
     * @return Engine configuration properties
     */
    public EngineProperties getEngine() {
        // [REQ-00030, FR-00003] Return nested Engine configuration block
        return engine;
    }

    /**
     * Configuration properties governing Terminal User Interface styling, display modes, and refresh rates.
     *
     * <p>Traceability: REQ-00071, REQ-00099, REQ-00100, FR-00046, FR-00047, FR-00050
     */
    public static final class TuiProperties {

        /** Default frames per second target. */
        public static final int DEFAULT_FPS = 30;
        /** Default TUI display mode identifier (`minimal`). */
        public static final String DEFAULT_DISPLAY_MODE = "minimal";
        /** Default TUI sidebar visibility mode (`auto`). */
        public static final String DEFAULT_SIDEBAR_MODE = "auto";

        /** Theme identifier (`cyberpunk`, `opencode`, `dracula`, `nord`, `matrix`, `high-contrast`). */
        private String theme = "cyberpunk";
        /** Frames per second target. */
        private int fpsTarget = DEFAULT_FPS;
        /** TUI display mode (`minimal` or `fullscreen`). */
        private String displayMode = DEFAULT_DISPLAY_MODE;
        /** Sidebar visibility mode (`auto`, `show`, or `hide`). */
        private String sidebarMode = DEFAULT_SIDEBAR_MODE;

        /**
         * Returns the active ANSI/XTerm-256 color theme.
         *
         * <p>Traceability: REQ-00099, FR-00046, CS-0030.1
         *
         * @return the theme identifier
         */
        public String getTheme() {
            // [REQ-00099, FR-00046] Return active TUI semantic theme name
            return theme;
        }

        /**
         * Sets the ANSI/XTerm-256 color theme.
         *
         * <p>Traceability: REQ-00099, FR-00046, CS-0050.1
         *
         * @param themeVal the theme identifier, must not be null
         */
        public void setTheme(final String themeVal) {
            // [REQ-00099, FR-00046, CS-0050.1] Enforce non-null theme identifier
            this.theme = Objects.requireNonNull(themeVal, "theme must not be null");
        }

        /**
         * Returns the target rendering frames per second.
         *
         * <p>Traceability: REQ-00001, FR-00001, CS-0030.1
         *
         * @return target FPS
         */
        public int getFpsTarget() {
            // [REQ-00001, FR-00001] Return target rendering FPS
            return fpsTarget;
        }

        /**
         * Sets the target rendering frames per second.
         *
         * <p>Traceability: REQ-00001, FR-00001, CS-0050.1
         *
         * @param fpsTargetVal target FPS, must be strictly positive
         */
        public void setFpsTarget(final int fpsTargetVal) {
            // [REQ-00001, FR-00001, CS-0050.1] Validate positive FPS value
            if (fpsTargetVal <= 0) {
                throw new IllegalArgumentException("fpsTarget must be strictly positive");
            }
            this.fpsTarget = fpsTargetVal;
        }

        /**
         * Returns the active TUI display mode (`minimal` or `fullscreen`).
         *
         * <p>Traceability: REQ-00100, FR-00047, FR-00050
         *
         * @return TUI display mode
         */
        public String getDisplayMode() {
            // [REQ-00100, FR-00047] Return configured TUI display mode
            return displayMode;
        }

        /**
         * Sets the active TUI display mode (`minimal` or `fullscreen`).
         *
         * <p>Traceability: REQ-00100, FR-00047, FR-00050, CS-0050.1
         *
         * @param displayModeVal TUI display mode, must not be null
         */
        public void setDisplayMode(final String displayModeVal) {
            // [REQ-00100, FR-00047, CS-0050.1] Validate non-null display mode for TSK-20261101-016
            this.displayMode = Objects.requireNonNull(displayModeVal, "displayMode must not be null");
        }

        /**
         * Returns the sidebar visibility policy (`auto`, `show`, or `hide`).
         *
         * <p>Traceability: REQ-00071, REQ-00100, FR-00048
         *
         * @return sidebar visibility mode
         */
        public String getSidebarMode() {
            // [REQ-00071, REQ-00100, FR-00048] Return configured sidebar docking policy
            return sidebarMode;
        }

        /**
         * Sets the sidebar visibility policy (`auto`, `show`, or `hide`).
         *
         * <p>Traceability: REQ-00071, REQ-00100, FR-00048, CS-0050.1
         *
         * @param sidebarModeVal sidebar visibility mode, must not be null
         */
        public void setSidebarMode(final String sidebarModeVal) {
            // [REQ-00071, REQ-00100, FR-00048, CS-0050.1] Validate non-null sidebar policy
            this.sidebarMode = Objects.requireNonNull(sidebarModeVal, "sidebarMode must not be null");
        }
    }

    /**
     * Configuration properties governing the autonomous agent reasoning engine.
     *
     * <p>Traceability: REQ-00030, FR-00003, CS-0050.6
     */
    public static final class EngineProperties {

        /** Default maximum reasoning steps. */
        public static final int DEFAULT_MAX_REASONING_STEPS = 50;
        /** Default timeout in seconds. */
        public static final int DEFAULT_TIMEOUT_SECONDS = 120;
        /** Default thread pool worker count. */
        public static final int DEFAULT_MAX_THREADS = 8;

        /** Maximum reasoning steps. */
        private int maxReasoningSteps = DEFAULT_MAX_REASONING_STEPS;
        /** Default timeout seconds. */
        private int defaultTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        /** Maximum threads. */
        private int maxThreads = DEFAULT_MAX_THREADS;

        /**
         * Returns the maximum reasoning steps per task.
         *
         * <p>Traceability: REQ-00030, FR-00003, CS-0050.6
         *
         * @return max reasoning steps
         */
        public int getMaxReasoningSteps() {
            // [REQ-00030, FR-00003, CS-0050.6] Return bounded reasoning steps limit
            return maxReasoningSteps;
        }

        /**
         * Sets the maximum reasoning steps per task.
         *
         * <p>Traceability: REQ-00030, FR-00003, CS-0050.1
         *
         * @param maxReasoningStepsVal maximum reasoning steps, must be positive
         */
        public void setMaxReasoningSteps(final int maxReasoningStepsVal) {
            // [REQ-00030, FR-00003, CS-0050.1] Validate positive maxReasoningSteps
            if (maxReasoningStepsVal <= 0) {
                throw new IllegalArgumentException("maxReasoningSteps must be strictly positive");
            }
            this.maxReasoningSteps = maxReasoningStepsVal;
        }

        /**
         * Returns the default execution timeout in seconds.
         *
         * <p>Traceability: REQ-00030, FR-00003
         *
         * @return default timeout seconds
         */
        public int getDefaultTimeoutSeconds() {
            // [REQ-00030, FR-00003] Return default execution timeout in seconds
            return defaultTimeoutSeconds;
        }

        /**
         * Sets the default execution timeout in seconds.
         *
         * <p>Traceability: REQ-00030, FR-00003, CS-0050.1
         *
         * @param defaultTimeoutSecondsVal timeout in seconds, must be positive
         */
        public void setDefaultTimeoutSeconds(final int defaultTimeoutSecondsVal) {
            // [REQ-00030, FR-00003, CS-0050.1] Validate positive timeout seconds
            if (defaultTimeoutSecondsVal <= 0) {
                throw new IllegalArgumentException("defaultTimeoutSeconds must be strictly positive");
            }
            this.defaultTimeoutSeconds = defaultTimeoutSecondsVal;
        }

        /**
         * Returns the maximum worker threads for agent execution.
         *
         * <p>Traceability: REQ-00030, FR-00003
         *
         * @return maximum worker threads
         */
        public int getMaxThreads() {
            // [REQ-00030, FR-00003] Return maximum worker thread count
            return maxThreads;
        }

        /**
         * Sets the maximum worker threads for agent execution.
         *
         * <p>Traceability: REQ-00030, FR-00003, CS-0050.1
         *
         * @param maxThreadsVal maximum worker threads, must be positive
         */
        public void setMaxThreads(final int maxThreadsVal) {
            // [REQ-00030, FR-00003, CS-0050.1] Validate positive worker thread count
            if (maxThreadsVal <= 0) {
                throw new IllegalArgumentException("maxThreads must be strictly positive");
            }
            this.maxThreads = maxThreadsVal;
        }
    }
}
