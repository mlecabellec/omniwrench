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
 * - Requirement: REQ-00042 (Spring Boot TUI Command Line Lifecycle)
 * - Task: TSK-20260822-003 (Modern Cyberpunk TUI Design & Integration)
 */
@Component
public class TuiRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TuiRunner.class);

    private final OmniwrenchProperties properties;
    private final OmniwrenchTuiDashboard dashboard;

    public TuiRunner(final OmniwrenchProperties properties, final OmniwrenchTuiDashboard dashboard) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.dashboard = Objects.requireNonNull(dashboard, "dashboard must not be null");
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
