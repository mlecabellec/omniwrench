package com.omniwrench.web;

import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying AgentController REST endpoints, HTTP status codes, and prompt dispatching.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST &amp; WebSocket Agent Server API), REQ-00051 (RESTful Agent Dialogue &amp; Session Inspection API)
 * - Feature: FR-00015 (RESTful Agent Dialogue &amp; Session Inspection API)
 * - Use Case: UC-00003 (Web UI Agent Collaboration), UC-00004 (Headless CI/CD Automation Execution)
 * - Task: TSK-20260822-004 (Spring Web &amp; Reactive WebSocket Server Engine)
 * - ADR: ADR-0003 (Spring Web MVC &amp; Reactive WebSocket Streaming Architecture)
 */
@Tag("REQ-00050")
@Tag("REQ-00051")
@Tag("FR-00015")
@Tag("UC-00003")
@Tag("TSK-20260822-004")
class AgentControllerTest {

    private AgentEngine agentEngine;
    private SessionManager sessionManager;
    private ToolRegistry toolRegistry;
    private AgentController controller;

    @BeforeEach
    void setUp() {
        agentEngine = Mockito.mock(AgentEngine.class);
        sessionManager = Mockito.mock(SessionManager.class);
        toolRegistry = Mockito.mock(ToolRegistry.class);
        controller = new AgentController(agentEngine, sessionManager, toolRegistry);
    }

    @Test
    @DisplayName("Should list all registered tools via GET /api/v1/tools")
    void shouldListTools() {
        final ToolDefinition toolDef = new ToolDefinition("file_ops", "File operations", Map.of());
        when(toolRegistry.getAllDefinitions()).thenReturn(List.of(toolDef));

        final ResponseEntity<List<ToolDefinition>> response = controller.listTools();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(toolDef);
    }

    @Test
    @DisplayName("Should list all active sessions via GET /api/v1/sessions")
    void shouldListSessions() {
        final SessionContext session = SessionContext.createDefault("/tmp/workspace");
        when(sessionManager.getActiveSessions()).thenReturn(Map.of(session.getSessionId(), session));

        final ResponseEntity<List<Map<String, Object>>> response = controller.listSessions();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0)).containsEntry("sessionId", session.getSessionId());
    }

    @Test
    @DisplayName("Should create new session via POST /api/v1/sessions")
    void shouldCreateSession() {
        final SessionContext newSession = SessionContext.createDefault("/custom/path");
        when(sessionManager.createSession("/custom/path")).thenReturn(newSession);

        final ResponseEntity<Map<String, Object>> response = controller.createSession(Map.of("workspacePath", "/custom/path"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("sessionId", newSession.getSessionId());
    }

    @Test
    @DisplayName("Should retrieve messages for valid session or 404 for unknown session")
    void shouldRetrieveSessionMessages() {
        final SessionContext session = SessionContext.createDefault("/tmp");
        final AgentMessage message = AgentMessage.of("user", "Hello");
        session.addMessage(message);

        when(sessionManager.getSession("valid-id")).thenReturn(Optional.of(session));
        when(sessionManager.getSession("unknown-id")).thenReturn(Optional.empty());

        final ResponseEntity<List<AgentMessage>> okResp = controller.getMessages("valid-id");
        assertThat(okResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(okResp.getBody()).containsExactly(message);

        final ResponseEntity<List<AgentMessage>> notFoundResp = controller.getMessages("unknown-id");
        assertThat(notFoundResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should clear messages for valid session via DELETE /api/v1/sessions/{id}/messages")
    void shouldClearMessages() {
        final SessionContext session = SessionContext.createDefault("/tmp");
        session.addMessage(AgentMessage.of("user", "To be cleared"));

        when(sessionManager.getSession("valid-id")).thenReturn(Optional.of(session));
        when(sessionManager.getSession("unknown-id")).thenReturn(Optional.empty());

        final ResponseEntity<Void> okResp = controller.clearMessages("valid-id");
        assertThat(okResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(session.getMessages()).isEmpty();

        final ResponseEntity<Void> notFoundResp = controller.clearMessages("unknown-id");
        assertThat(notFoundResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should process valid prompt and return 200 OK or 400 Bad Request on blank prompt")
    void shouldSendPrompt() {
        final SessionContext session = SessionContext.createDefault("/tmp");
        when(sessionManager.getSession("valid-id")).thenReturn(Optional.of(session));
        when(sessionManager.getSession("unknown-id")).thenReturn(Optional.empty());

        final AgentMessage agentReply = AgentMessage.of("assistant", "Processed successfully");
        when(agentEngine.processPrompt(eq(session), eq("Refactor code"))).thenReturn(agentReply);

        final ResponseEntity<AgentMessage> okResp = controller.sendPrompt("valid-id", Map.of("prompt", "Refactor code"));
        assertThat(okResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(okResp.getBody()).isEqualTo(agentReply);

        final ResponseEntity<AgentMessage> badReqResp = controller.sendPrompt("valid-id", Map.of("prompt", "  "));
        assertThat(badReqResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        final ResponseEntity<AgentMessage> notFoundResp = controller.sendPrompt("unknown-id", Map.of("prompt", "Hello"));
        assertThat(notFoundResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should reject null constructor dependencies")
    void shouldRejectNullConstructorArgs() {
        assertThrows(NullPointerException.class, () -> new AgentController(null, sessionManager, toolRegistry));
        assertThrows(NullPointerException.class, () -> new AgentController(agentEngine, null, toolRegistry));
        assertThrows(NullPointerException.class, () -> new AgentController(agentEngine, sessionManager, null));
    }
}
