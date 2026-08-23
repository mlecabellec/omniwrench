package com.omniwrench.web;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying StatusController health metrics, JVM version reporting, and session statistics.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST & WebSocket Agent Server API), REQ-00051 (RESTful Agent Dialogue & Session Inspection API)
 * - Feature: FR-00015 (RESTful Agent Dialogue & Session Inspection API)
 * - Use Case: UC-00003 (Web UI Agent Collaboration), UC-00004 (Headless CI/CD Automation Execution)
 * - Task: TSK-20260822-004 (Spring Web & Reactive WebSocket Server Engine)
 * - ADR: ADR-0003 (Spring Web MVC & Reactive WebSocket Streaming Architecture)
 */
@Tag("REQ-00050")
@Tag("REQ-00051")
@Tag("FR-00015")
@Tag("UC-00003")
@Tag("TSK-20260822-004")
class StatusControllerTest {

    private OmniwrenchProperties properties;
    private ToolRegistry toolRegistry;
    private SessionManager sessionManager;
    private StatusController controller;

    @BeforeEach
    void setUp() {
        properties = new OmniwrenchProperties();
        toolRegistry = Mockito.mock(ToolRegistry.class);
        sessionManager = Mockito.mock(SessionManager.class);
        controller = new StatusController(properties, toolRegistry, sessionManager);
    }

    @Test
    @DisplayName("Should return 200 OK with health status and telemetry payload")
    void shouldReturnSystemStatus() {
        properties.setMode("dual");
        when(sessionManager.getActiveSessions()).thenReturn(Map.of());
        when(toolRegistry.getToolCount()).thenReturn(3);

        final ResponseEntity<Map<String, Object>> response = controller.getStatus();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        final Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("application")).isEqualTo("omniwrench");
        assertThat(body.get("status")).isEqualTo("HEALTHY");
        assertThat(body.get("mode")).isEqualTo("dual");
        assertThat(body.get("registeredTools")).isEqualTo(3);
        assertThat(body.get("jvmVersion")).isNotNull();
    }

    @Test
    @DisplayName("Should reject null constructor parameters")
    void shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class, () -> new StatusController(null, toolRegistry, sessionManager));
        assertThrows(NullPointerException.class, () -> new StatusController(properties, null, sessionManager));
        assertThrows(NullPointerException.class, () -> new StatusController(properties, toolRegistry, null));
    }
}
