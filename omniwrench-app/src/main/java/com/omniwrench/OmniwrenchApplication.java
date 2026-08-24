package com.omniwrench;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.IntrospectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.Objects;

/**
 * Main application entry point for the Omniwrench Dual TUI/Web Agent Engineering Workbench.
 *
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI & Interactive TUI Presentation Engine)
 * - Requirement: REQ-00002 (Configurable Runtime Profiles), REQ-00025 (Dual Distribution Packaging)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine), FR-00002 (Headless Continuous Execution Mode)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00004 (Headless CI/CD Automation Execution)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton), TSK-20260822-014 (GraalVM SDK Integration)
 * - ADR: ADR-0001 (Unified Dual Architecture), ADR-0011 (Configurable Execution Modes), ADR-0025 (Dual Distribution Packaging)
 */
@SpringBootApplication
@EnableConfigurationProperties
@ImportRuntimeHints(OmniwrenchApplication.TomcatRuntimeHints.class)
public class OmniwrenchApplication {

    /**
     * Runtime hints registrar providing reflection metadata for Tomcat and dynamic connectors under GraalVM Native Image.
     */
    static class TomcatRuntimeHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
            final MemberCategory[] allMemberCategories = MemberCategory.values();
            hints.reflection().registerType(AbstractProtocol.class, allMemberCategories);
            hints.reflection().registerType(AbstractHttp11Protocol.class, allMemberCategories);
            hints.reflection().registerType(Http11NioProtocol.class, allMemberCategories);
            hints.reflection().registerType(Connector.class, allMemberCategories);
            hints.reflection().registerType(IntrospectionUtils.class, allMemberCategories);
        }
    }


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
