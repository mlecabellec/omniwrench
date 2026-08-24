package com.omniwrench.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.core.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test suite verifying AgentStreamWebSocketHandler frame generation and reactive message dispatch.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST &amp; WebSocket Server API), REQ-00052 (Real-Time WebSocket Streaming)
 * - Requirement: REQ-00088 (Dual Chat Mode Reasoning Demultiplexing)
 * - Task: TSK-20260822-004 (Spring Web Server), TSK-20260822-007 (Dual Chat Mode &amp; Thinking Demux)
 * - ADR: ADR-0003 (WebSocket Architecture), ADR-0047 (Reasoning Demux)
 */
@Tag("REQ-00050")
@Tag("REQ-00052")
@Tag("REQ-00088")
@Tag("TSK-20260822-007")
class AgentStreamWebSocketHandlerTest {

    private AgentStreamWebSocketHandler handler;
    private AgentEngine agentEngine;
    private SessionManager sessionManager;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        final ToolRegistry toolRegistry = new ToolRegistry(List.of());
        final OmniwrenchProperties properties = new OmniwrenchProperties();
        sessionManager = new SessionManager(properties);
        agentEngine = new AgentEngine(toolRegistry, properties);
        objectMapper = new ObjectMapper();
        handler = new AgentStreamWebSocketHandler(agentEngine, sessionManager, objectMapper);
    }

    @Test
    @DisplayName("AgentStreamWebSocketHandler should track connection lifecycle and handle text messages")
    void testWebSocketLifecycleAndStreaming() throws Exception {
        final WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("test-session-ws-1");

        handler.afterConnectionEstablished(session);

        final String payload = objectMapper.writeValueAsString(Map.of(
                "prompt", "Explain AI safety",
                "sessionId", sessionManager.getDefaultSession().getSessionId()
        ));

        handler.handleTextMessage(session, new TextMessage(payload));

        final ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());

        final List<TextMessage> capturedMessages = captor.getAllValues();
        assertThat(capturedMessages).isNotEmpty();

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
    }
}
