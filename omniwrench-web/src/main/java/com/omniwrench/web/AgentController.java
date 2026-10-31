package com.omniwrench.web;

import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for session interaction, message querying, and autonomous prompt execution.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST &amp; WebSocket Agent Server API), REQ-00051 (RESTful Agent Dialogue &amp; Session Inspection API)
 * - Feature: FR-00015 (RESTful Agent Dialogue &amp; Session Inspection API)
 * - Use Case: UC-00003 (Web UI Agent Collaboration), UC-00004 (Headless CI/CD Automation Execution)
 * - Task: TSK-20260822-004 (Spring Web &amp; Reactive WebSocket Server Engine)
 * - ADR: ADR-0003 (Spring Web MVC &amp; Reactive WebSocket Streaming Architecture)
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
     * Lists metadata for all active sessions.
     *
     * @return response entity containing list of session metadata summaries
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> listSessions() {
        final List<Map<String, Object>> summaries = new ArrayList<>();
        for (final SessionContext session : sessionManager.getActiveSessions().values()) {
            final Map<String, Object> summary = new HashMap<>();
            summary.put("sessionId", session.getSessionId());
            summary.put("workspaceRoot", session.getWorkspaceRoot());
            summary.put("messageCount", session.getMessages().size());
            summary.put("createdAt", session.getCreatedAt().toString());
            summaries.add(summary);
        }
        return ResponseEntity.ok(summaries);
    }

    /**
     * Creates a new isolated session for the workbench.
     *
     * @param payload optional JSON payload with "workspacePath" key
     * @return response entity containing newly created session summary
     */
    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody(required = false) final Map<String, String> payload) {
        final String workspacePath;
        if (payload != null && payload.containsKey("workspacePath")) {
            workspacePath = payload.get("workspacePath");
        } else {
            workspacePath = "";
        }

        final SessionContext newSession = sessionManager.createSession(workspacePath);
        final Map<String, Object> summary = new HashMap<>();
        summary.put("sessionId", newSession.getSessionId());
        summary.put("workspaceRoot", newSession.getWorkspaceRoot());
        summary.put("messageCount", 0);
        summary.put("createdAt", newSession.getCreatedAt().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
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
        } else {
            return ResponseEntity.ok(sessionOpt.get().getMessages());
        }
    }

    /**
     * Clears all conversation message history for a specific session.
     *
     * @param sessionId session identifier
     * @return response entity with 204 No Content, or 404 if session not found
     */
    @DeleteMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<Void> clearMessages(@PathVariable final String sessionId) {
        final Optional<SessionContext> sessionOpt = sessionManager.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            sessionOpt.get().clearMessages();
            return ResponseEntity.noContent().build();
        }
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
        } else {
            final String prompt = payload.getOrDefault("prompt", "");
            if (prompt.isBlank()) {
                return ResponseEntity.badRequest().build();
            } else {
                final AgentMessage response = agentEngine.processPrompt(sessionOpt.get(), prompt);
                return ResponseEntity.ok(response);
            }
        }
    }
}
