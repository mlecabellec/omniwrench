package com.omniwrench;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Objects;

/**
 * Main application entry point for the Omniwrench Dual TUI/Web Agent Engineering Workbench.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI & Interactive TUI Presentation Engine), REQ-00002 (Configurable Runtime Profiles)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine), FR-00002 (Headless Continuous Execution Mode)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00004 (Headless CI/CD Automation Execution)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 * - ADR: ADR-0001 (Unified Dual Architecture), ADR-0011 (Configurable Execution Modes)
 */
@SpringBootApplication
@EnableConfigurationProperties
public class OmniwrenchApplication {

    /** Application logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OmniwrenchApplication.class);

    /**
     * Protected constructor to prevent direct instantiation while allowing framework proxies.
     */
    protected OmniwrenchApplication() {
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(final String[] args) {
        final String[] nonNullArgs = Objects.requireNonNull(args, "Command line arguments must not be null");
        LOGGER.info("Starting Omniwrench Dual Workbench Engine...");
        SpringApplication.run(OmniwrenchApplication.class, nonNullArgs);
    }
}
