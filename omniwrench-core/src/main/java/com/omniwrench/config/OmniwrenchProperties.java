package com.omniwrench.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Configuration properties for Omniwrench runtime environment and subsystem profiles.
 *
 * Traceability:
 * - Requirement: REQ-00002 (Configurable Runtime Profiles)
 * - Feature: FR-00009 (Hierarchical Configuration & AES-256 Vault)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 * - ADR: ADR-0011 (Configurable Profiles), ADR-0031 (Configuration Layering)
 */
@Component
@ConfigurationProperties(prefix = "omniwrench")
public class OmniwrenchProperties {

    /** Default runtime execution mode. */
    private String mode = "dual";
    /** Root workspace directory path. */
    private String workspacePath = ".";
    /** Nested TUI properties. */
    private final TuiProperties tui = new TuiProperties();
    /** Nested engine execution properties. */
    private final EngineProperties engine = new EngineProperties();

    /**
     * Returns the active execution mode (e.g., dual, tui, headless).
     *
     * @return the execution mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * Sets the active execution mode.
     *
     * @param modeVal the execution mode value
     */
    public void setMode(final String modeVal) {
        this.mode = Objects.requireNonNull(modeVal, "mode must not be null");
    }

    /**
     * Returns the workspace root path.
     *
     * @return the workspace root path
     */
    public String getWorkspacePath() {
        return workspacePath;
    }

    /**
     * Sets the workspace root path.
     *
     * @param workspacePathVal the workspace root directory path
     */
    public void setWorkspacePath(final String workspacePathVal) {
        this.workspacePath = Objects.requireNonNull(workspacePathVal, "workspacePath must not be null");
    }

    /**
     * Returns the TUI visual configuration properties.
     *
     * @return TUI configuration properties
     */
    public TuiProperties getTui() {
        return tui;
    }

    /**
     * Returns the agent engine execution properties.
     *
     * @return Engine configuration properties
     */
    public EngineProperties getEngine() {
        return engine;
    }

    /**
     * Configuration properties governing Terminal User Interface styling and refresh rates.
     */
    public static final class TuiProperties {

        /** Default frames per second target. */
        public static final int DEFAULT_FPS = 30;

        /** Theme identifier. */
        private String theme = "cyberpunk";
        /** Frames per second target. */
        private int fpsTarget = DEFAULT_FPS;

        /**
         * Returns the active ANSI color theme.
         *
         * @return the theme identifier
         */
        public String getTheme() {
            return theme;
        }

        /**
         * Sets the ANSI color theme.
         *
         * @param themeVal the theme identifier
         */
        public void setTheme(final String themeVal) {
            this.theme = Objects.requireNonNull(themeVal, "theme must not be null");
        }

        /**
         * Returns the target rendering frames per second.
         *
         * @return target FPS
         */
        public int getFpsTarget() {
            return fpsTarget;
        }

        /**
         * Sets the target rendering frames per second.
         *
         * @param fpsTargetVal target FPS, must be positive
         */
        public void setFpsTarget(final int fpsTargetVal) {
            if (fpsTargetVal <= 0) {
                throw new IllegalArgumentException("fpsTarget must be strictly positive");
            }
            this.fpsTarget = fpsTargetVal;
        }
    }

    /**
     * Configuration properties governing the autonomous agent reasoning engine.
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
         * @return max reasoning steps
         */
        public int getMaxReasoningSteps() {
            return maxReasoningSteps;
        }

        /**
         * Sets the maximum reasoning steps per task.
         *
         * @param maxReasoningStepsVal maximum reasoning steps, must be positive
         */
        public void setMaxReasoningSteps(final int maxReasoningStepsVal) {
            if (maxReasoningStepsVal <= 0) {
                throw new IllegalArgumentException("maxReasoningSteps must be strictly positive");
            }
            this.maxReasoningSteps = maxReasoningStepsVal;
        }

        /**
         * Returns the default execution timeout in seconds.
         *
         * @return default timeout seconds
         */
        public int getDefaultTimeoutSeconds() {
            return defaultTimeoutSeconds;
        }

        /**
         * Sets the default execution timeout in seconds.
         *
         * @param defaultTimeoutSecondsVal timeout in seconds, must be positive
         */
        public void setDefaultTimeoutSeconds(final int defaultTimeoutSecondsVal) {
            if (defaultTimeoutSecondsVal <= 0) {
                throw new IllegalArgumentException("defaultTimeoutSeconds must be strictly positive");
            }
            this.defaultTimeoutSeconds = defaultTimeoutSecondsVal;
        }

        /**
         * Returns the maximum worker threads for agent execution.
         *
         * @return maximum worker threads
         */
        public int getMaxThreads() {
            return maxThreads;
        }

        /**
         * Sets the maximum worker threads for agent execution.
         *
         * @param maxThreadsVal maximum worker threads, must be positive
         */
        public void setMaxThreads(final int maxThreadsVal) {
            if (maxThreadsVal <= 0) {
                throw new IllegalArgumentException("maxThreads must be strictly positive");
            }
            this.maxThreads = maxThreadsVal;
        }
    }
}
