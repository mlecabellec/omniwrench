package com.omniwrench.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying SecurityConfig instantiation and null rejection.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST &amp; WebSocket Agent Server API)
 * - Feature: FR-00015 (RESTful Agent Dialogue &amp; Session Inspection API)
 * - Use Case: UC-00003 (Web UI Agent Collaboration)
 * - Task: TSK-20260822-004 (Spring Web &amp; Reactive WebSocket Server Engine)
 * - ADR: ADR-0003 (Spring Web MVC &amp; Reactive WebSocket Streaming Architecture)
 */
@Tag("REQ-00050")
@Tag("FR-00015")
@Tag("UC-00003")
@Tag("TSK-20260822-004")
class SecurityConfigTest {

    @Test
    @DisplayName("Should instantiate SecurityConfig successfully")
    void shouldInstantiateSecurityConfig() {
        final SecurityConfig config = new SecurityConfig();
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("Should reject null HttpSecurity argument")
    void shouldRejectNullHttpSecurity() {
        final SecurityConfig config = new SecurityConfig();
        assertThrows(NullPointerException.class, () -> config.securityFilterChain(null));
    }
}
