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
 * - Requirement: REQ-00050 (Headless HTTP/REST & WebSocket Agent Server API), REQ-00051 (RESTful Agent Dialogue & Session Inspection API)
 * - Feature: FR-00015 (RESTful Agent Dialogue & Session Inspection API)
 * - Use Case: UC-00003 (Web UI Agent Collaboration), UC-00004 (Headless CI/CD Automation Execution)
 * - Task: TSK-20260822-004 (Spring Web & Reactive WebSocket Server Engine)
 * - ADR: ADR-0003 (Spring Web MVC & Reactive WebSocket Streaming Architecture)
 */
@RestController
@RequestMapping("/api/v1")
public final class AgentController {

    /** Agent reasoning engine service. */
    private final AgentEngine agentEngine;
    /** Session manager service. */
    private final SessionManager sessionManager;
    /** Tool registry service. */
    private final ToolRegistry toolRegistry;

    /**
     * Constructs AgentController with engine, session manager, and tool registry.
     *
     * @param agentEngineVal the agent reasoning engine, must not be null
     * @param sessionManagerVal the session manager, must not be null
     * @param toolRegistryVal the tool registry, must not be null
     */
    public AgentController(final AgentEngine agentEngineVal,
                           final SessionManager sessionManagerVal,
                           final ToolRegistry toolRegistryVal) {
        this.agentEngine = Objects.requireNonNull(agentEngineVal, "agentEngine must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManagerVal, "sessionManager must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistryVal, "toolRegistry must not be null");
    }

    /**
     * Lists all registered tool definitions.
     *
     * @return response entity containing list of tool definitions
     */
    @GetMapping("/tools")
    public ResponseEntity<List<ToolDefinition>> listTools() {
        return ResponseEntity.ok(toolRegistry.getAllDefinitions());
    }

    /**
     * Retrieves conversation message history for a specific session.
     *
     * @param sessionId session identifier
     * @return response entity containing list of messages, or 404 if session not found
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<AgentMessage>> getMessages(@PathVariable final String sessionId) {
        final Optional<SessionContext> sessionOpt = sessionManager.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sessionOpt.get().getMessages());
    }

    /**
     * Dispatches a user prompt to the agent engine for the specified session.
     *
     * @param sessionId session identifier
     * @param payload request body containing "prompt" key
     * @return response entity containing assistant reply message
     */
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
