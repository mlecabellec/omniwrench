package com.omniwrench.tui;

import com.omniwrench.config.OmniwrenchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

/**
 * Spring CommandLineRunner that starts the TUI when executed in interactive CLI/Dual mode.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI & Interactive TUI Presentation Engine), REQ-00002 (Configurable Runtime Profiles)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design & Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
@Component
public final class TuiRunner implements CommandLineRunner {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TuiRunner.class);

    /** Configuration properties instance. */
    private final OmniwrenchProperties properties;
    /** Interactive TUI dashboard component. */
    private final OmniwrenchTuiDashboard dashboard;

    /**
     * Constructs TuiRunner with configuration properties and dashboard loop.
     *
     * @param propertiesVal configuration properties, must not be null
     * @param dashboardVal interactive dashboard component, must not be null
     */
    public TuiRunner(final OmniwrenchProperties propertiesVal, final OmniwrenchTuiDashboard dashboardVal) {
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");
        this.dashboard = Objects.requireNonNull(dashboardVal, "dashboard must not be null");
    }

    @Override
    public void run(final String... args) throws Exception {
        final String[] nonNullArgs = (args != null) ? args : new String[0];
        LOGGER.info("TuiRunner inspecting startup arguments: {}", Arrays.toString(nonNullArgs));

        final String mode = properties.getMode();
        final boolean isCliExplicit = nonNullArgs.length > 0 && "cli".equalsIgnoreCase(nonNullArgs[0]);
        final boolean isTuiExplicit = nonNullArgs.length > 0 && "tui".equalsIgnoreCase(nonNullArgs[0]);

        if (isCliExplicit || isTuiExplicit || "tui".equalsIgnoreCase(mode)) {
            LOGGER.info("Launching full interactive TUI mode...");
            dashboard.startInteractiveLoop();
        } else {
            LOGGER.info("Omniwrench running in server/dual mode (Web UI on server port, TUI standby).");
        }
    }
}
