package com.omniwrench.web;

import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for session interaction, message querying, and autonomous prompt execution.
 * 
 * Traceability:
 * - Requirement: REQ-00051 (Agent Dialogue REST Endpoints)
 * - Task: TSK-20260822-004 (Spring Web & Reactive WebSocket Server Engine)
 */
@RestController
@RequestMapping("/api/v1")
public class AgentController {

    private final AgentEngine agentEngine;
    private final SessionManager sessionManager;
    private final ToolRegistry toolRegistry;

    public AgentController(final AgentEngine agentEngine,
                           final SessionManager sessionManager,
                           final ToolRegistry toolRegistry) {
        this.agentEngine = Objects.requireNonNull(agentEngine, "agentEngine must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
    }

    @GetMapping("/tools")
    public ResponseEntity<List<ToolDefinition>> listTools() {
        return ResponseEntity.ok(toolRegistry.getAllDefinitions());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<AgentMessage>> getMessages(@PathVariable final String sessionId) {
        final Optional<SessionContext> sessionOpt = sessionManager.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sessionOpt.get().getMessages());
    }

    @PostMapping("/sessions/{sessionId}/prompt")
    public ResponseEntity<AgentMessage> sendPrompt(@PathVariable final String sessionId,
                                                   @RequestBody final Map<String, String> payload) {
        final Optional<SessionContext> sessionOpt = sessionManager.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        final String prompt = payload.getOrDefault("prompt", "");
        if (prompt.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        final AgentMessage response = agentEngine.processPrompt(sessionOpt.get(), prompt);
        return ResponseEntity.ok(response);
    }
}
