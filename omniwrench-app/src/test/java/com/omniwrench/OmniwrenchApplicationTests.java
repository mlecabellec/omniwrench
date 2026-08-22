package com.omniwrench;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Spring context initialization test.
 * 
 * Traceability:
 * - Requirement: REQ-00001 (Core Runtime Initialization)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
@SpringBootTest
class OmniwrenchApplicationTests {

    @Test
    void contextLoads() {
        assertDoesNotThrow(() -> {
            // Context loaded successfully
        });
    }
}
