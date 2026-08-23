package com.omniwrench;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Spring context initialization test verifying unified dual workbench bootstrap.
 * 
 * Traceability:
 * - Requirement: REQ-00001 (Dual Headless CLI & Interactive TUI Presentation Engine), REQ-00002 (Configurable Runtime Profiles)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine), FR-00002 (Headless Continuous Execution Mode)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00004 (Headless CI/CD Automation Execution)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 * - ADR: ADR-0001 (Unified Dual Architecture), ADR-0011 (Configurable Execution Modes)
 */
@SpringBootTest
@Tag("REQ-00001")
@Tag("REQ-00002")
@Tag("FR-00001")
@Tag("FR-00002")
@Tag("UC-00001")
@Tag("TSK-20260822-001")
class OmniwrenchApplicationTests {

    @Test
    @DisplayName("Should bootstrap Spring Application Context without exceptions")
    void contextLoads() {
        assertDoesNotThrow(() -> {
            // Context loaded successfully
        });
    }
}
