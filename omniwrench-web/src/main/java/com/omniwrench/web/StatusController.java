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
 * - Requirement: REQ-00050 (Web Telemetry and Status API)
 * - Task: TSK-20260822-004 (Spring Web & Reactive WebSocket Server Engine)
 */
@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    private final OmniwrenchProperties properties;
    private final ToolRegistry toolRegistry;
    private final SessionManager sessionManager;

    public StatusController(final OmniwrenchProperties properties,
                            final ToolRegistry toolRegistry,
                            final SessionManager sessionManager) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
    }

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
