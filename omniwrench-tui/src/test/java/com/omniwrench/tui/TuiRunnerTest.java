package com.omniwrench.tui;

import com.omniwrench.config.OmniwrenchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests verifying TuiRunner argument inspection and mode lifecycle transitions.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI & Interactive TUI Presentation Engine), REQ-00002 (Configurable Runtime Profiles)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design & Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
@Tag("REQ-00001")
@Tag("REQ-00002")
@Tag("FR-00001")
@Tag("UC-00001")
@Tag("TSK-20260822-003")
class TuiRunnerTest {

    private OmniwrenchProperties properties;
    private OmniwrenchTuiDashboard dashboard;
    private TuiRunner tuiRunner;

    @BeforeEach
    void setUp() {
        properties = new OmniwrenchProperties();
        dashboard = Mockito.mock(OmniwrenchTuiDashboard.class);
        tuiRunner = new TuiRunner(properties, dashboard);
    }

    @Test
    @DisplayName("Should launch interactive loop when CLI argument is passed")
    void shouldLaunchWhenCliArgPassed() throws Exception {
        tuiRunner.run("cli");
        verify(dashboard).startInteractiveLoop();
    }

    @Test
    @DisplayName("Should launch interactive loop when TUI argument is passed")
    void shouldLaunchWhenTuiArgPassed() throws Exception {
        tuiRunner.run("tui");
        verify(dashboard).startInteractiveLoop();
    }

    @Test
    @DisplayName("Should launch interactive loop when mode property is configured as tui")
    void shouldLaunchWhenModePropertyIsTui() throws Exception {
        properties.setMode("tui");
        tuiRunner.run();
        verify(dashboard).startInteractiveLoop();
    }

    @Test
    @DisplayName("Should remain on server standby when mode is dual and no CLI arguments are supplied")
    void shouldRemainStandbyInDualMode() throws Exception {
        properties.setMode("dual");
        tuiRunner.run();
        verify(dashboard, never()).startInteractiveLoop();
    }

    @Test
    @DisplayName("Should reject null constructor arguments")
    void shouldRejectNullConstructorArgs() {
        assertThrows(NullPointerException.class, () -> new TuiRunner(null, dashboard));
        assertThrows(NullPointerException.class, () -> new TuiRunner(properties, null));
    }
}
