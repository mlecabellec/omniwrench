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
 * - Requirement: REQ-00001 (Core Runtime Initialization)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
@SpringBootApplication
@EnableConfigurationProperties
public class OmniwrenchApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(OmniwrenchApplication.class);

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
