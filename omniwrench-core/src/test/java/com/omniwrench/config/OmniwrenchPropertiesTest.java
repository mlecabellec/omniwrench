package com.omniwrench.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying OmniwrenchProperties default values, configuration layering, and nested bean mutators.
 *
 * Traceability:
 * - Requirement: REQ-00002 (Configurable Runtime Profiles)
 * - Feature: FR-00009 (Hierarchical Configuration & AES-256 Vault)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 * - ADR: ADR-0011 (Configurable Profiles), ADR-0031 (Configuration Layering)
 */
@Tag("REQ-00002")
@Tag("FR-00009")
@Tag("UC-00001")
@Tag("TSK-20260822-001")
class OmniwrenchPropertiesTest {

    @Test
    @DisplayName("Should initialize with sensible default configuration values")
    void shouldInitializeWithDefaultValues() {
        final OmniwrenchProperties properties = new OmniwrenchProperties();

        assertThat(properties.getMode()).isEqualTo("dual");
        assertThat(properties.getWorkspacePath()).isEqualTo(".");

        assertThat(properties.getTui()).isNotNull();
        assertThat(properties.getTui().getTheme()).isEqualTo("cyberpunk");
        assertThat(properties.getTui().getFpsTarget()).isEqualTo(30);

        assertThat(properties.getEngine()).isNotNull();
        assertThat(properties.getEngine().getMaxReasoningSteps()).isEqualTo(50);
        assertThat(properties.getEngine().getDefaultTimeoutSeconds()).isEqualTo(120);
        assertThat(properties.getEngine().getMaxThreads()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should update root and nested configuration values via setters")
    void shouldAllowConfigurationUpdates() {
        final OmniwrenchProperties properties = new OmniwrenchProperties();

        properties.setMode("tui");
        properties.setWorkspacePath("/home/user/workspace");

        properties.getTui().setTheme("matrix-green");
        properties.getTui().setFpsTarget(120);

        properties.getEngine().setMaxReasoningSteps(100);
        properties.getEngine().setDefaultTimeoutSeconds(60);
        properties.getEngine().setMaxThreads(16);

        assertThat(properties.getMode()).isEqualTo("tui");
        assertThat(properties.getWorkspacePath()).isEqualTo("/home/user/workspace");
        assertThat(properties.getTui().getTheme()).isEqualTo("matrix-green");
        assertThat(properties.getTui().getFpsTarget()).isEqualTo(120);
        assertThat(properties.getEngine().getMaxReasoningSteps()).isEqualTo(100);
        assertThat(properties.getEngine().getDefaultTimeoutSeconds()).isEqualTo(60);
        assertThat(properties.getEngine().getMaxThreads()).isEqualTo(16);
    }

    @Test
    @DisplayName("Should reject invalid configuration arguments with IllegalArgumentException or NullPointerException")
    void shouldRejectInvalidArguments() {
        final OmniwrenchProperties properties = new OmniwrenchProperties();

        assertThrows(NullPointerException.class, () -> properties.setWorkspacePath(null));
        assertThrows(IllegalArgumentException.class, () -> properties.getTui().setFpsTarget(-1));
        assertThrows(IllegalArgumentException.class, () -> properties.getEngine().setMaxReasoningSteps(0));
        assertThrows(IllegalArgumentException.class, () -> properties.getEngine().setDefaultTimeoutSeconds(-10));
        assertThrows(IllegalArgumentException.class, () -> properties.getEngine().setMaxThreads(0));
    }
}
