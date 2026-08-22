package com.omniwrench.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Configuration properties for Omniwrench runtime.
 * 
 * Traceability:
 * - Requirement: REQ-00002 (Configurable Runtime Profiles)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
@Component
@ConfigurationProperties(prefix = "omniwrench")
public class OmniwrenchProperties {

    private String mode = "dual";
    private String workspacePath = ".";
    private final TuiProperties tui = new TuiProperties();
    private final EngineProperties engine = new EngineProperties();

    public String getMode() {
        return mode;
    }

    public void setMode(final String mode) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(final String workspacePath) {
        this.workspacePath = Objects.requireNonNull(workspacePath, "workspacePath must not be null");
    }

    public TuiProperties getTui() {
        return tui;
    }

    public EngineProperties getEngine() {
        return engine;
    }

    public static class TuiProperties {
        private String theme = "cyberpunk";
        private int fpsTarget = 30;

        public String getTheme() {
            return theme;
        }

        public void setTheme(final String theme) {
            this.theme = Objects.requireNonNull(theme, "theme must not be null");
        }

        public int getFpsTarget() {
            return fpsTarget;
        }

        public void setFpsTarget(final int fpsTarget) {
            if (fpsTarget <= 0) {
                throw new IllegalArgumentException("fpsTarget must be strictly positive");
            }
            this.fpsTarget = fpsTarget;
        }
    }

    public static class EngineProperties {
        private int maxReasoningSteps = 50;
        private int defaultTimeoutSeconds = 120;
        private int maxThreads = 8;

        public int getMaxReasoningSteps() {
            return maxReasoningSteps;
        }

        public void setMaxReasoningSteps(final int maxReasoningSteps) {
            if (maxReasoningSteps <= 0) {
                throw new IllegalArgumentException("maxReasoningSteps must be strictly positive");
            }
            this.maxReasoningSteps = maxReasoningSteps;
        }

        public int getDefaultTimeoutSeconds() {
            return defaultTimeoutSeconds;
        }

        public void setDefaultTimeoutSeconds(final int defaultTimeoutSeconds) {
            if (defaultTimeoutSeconds <= 0) {
                throw new IllegalArgumentException("defaultTimeoutSeconds must be strictly positive");
            }
            this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        }

        public int getMaxThreads() {
            return maxThreads;
        }

        public void setMaxThreads(final int maxThreads) {
            if (maxThreads <= 0) {
                throw new IllegalArgumentException("maxThreads must be strictly positive");
            }
            this.maxThreads = maxThreads;
        }
    }
}
