package com.omniwrench.web;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * REST controller exposing system health, active profile status, and agent workbench telemetry.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST & WebSocket Agent Server API), REQ-00051 (RESTful Agent Dialogue & Session Inspection API)
 * - Feature: FR-00015 (RESTful Agent Dialogue & Session Inspection API)
 * - Use Case: UC-00003 (Web UI Agent Collaboration), UC-00004 (Headless CI/CD Automation Execution)
 * - Task: TSK-20260822-004 (Spring Web & Reactive WebSocket Server Engine)
 * - ADR: ADR-0003 (Spring Web MVC & Reactive WebSocket Streaming Architecture)
 */
@RestController
@RequestMapping("/api/v1/status")
public final class StatusController {

    /** Runtime properties instance. */
    private final OmniwrenchProperties properties;
    /** Tool registry service. */
    private final ToolRegistry toolRegistry;
    /** Session manager service. */
    private final SessionManager sessionManager;

    /**
     * Constructs StatusController with properties, tool registry, and session manager.
     *
     * @param propertiesVal configuration properties, must not be null
     * @param toolRegistryVal tool registry, must not be null
     * @param sessionManagerVal session manager, must not be null
     */
    public StatusController(final OmniwrenchProperties propertiesVal,
                            final ToolRegistry toolRegistryVal,
                            final SessionManager sessionManagerVal) {
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistryVal, "toolRegistry must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManagerVal, "sessionManager must not be null");
    }

    /**
     * Returns workbench health status and telemetry payload.
     *
     * @return response entity containing status map
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        final Map<String, Object> status = new HashMap<>();
        status.put("application", "omniwrench");
        status.put("version", "0.1.0-SNAPSHOT");
        status.put("status", "HEALTHY");
        status.put("mode", properties.getMode());
        status.put("activeSessions", sessionManager.getActiveSessions().size());
        status.put("registeredTools", toolRegistry.getToolCount());
        status.put("timestamp", Instant.now().toString());
        status.put("jvmVersion", Runtime.version().toString());
        return ResponseEntity.ok(status);
    }
}
