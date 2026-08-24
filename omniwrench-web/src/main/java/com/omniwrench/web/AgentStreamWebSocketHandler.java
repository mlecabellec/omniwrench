package com.omniwrench.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniwrench.core.AgentEngine;
import com.omniwrench.core.SessionManager;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reactive WebSocket handler streaming real-time thought deltas and chat deltas to Web HUD clients.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST &amp; WebSocket Server API), REQ-00052 (Real-Time WebSocket Streaming)
 * - Requirement: REQ-00088 (Dual Chat Mode Reasoning Demultiplexing)
 * - Feature: FR-00016 (Real-Time Reactive WebSocket Token Streaming)
 * - Use Case: UC-00003 (Web UI Agent Collaboration)
 * - Task: TSK-20260822-004 (Spring Web Server), TSK-20260822-007 (Dual Chat Mode &amp; Thinking Demux)
 * - ADR: ADR-0003 (WebSocket Architecture), ADR-0047 (Reasoning Demux)
 */
@Component
public final class AgentStreamWebSocketHandler extends TextWebSocketHandler {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentStreamWebSocketHandler.class);

    /** Agent reasoning engine. */
    private final AgentEngine agentEngine;
    /** Session lifecycle manager. */
    private final SessionManager sessionManager;
    /** JSON mapper. */
    private final ObjectMapper objectMapper;

    /** Active WebSocket sessions map. */
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Constructs AgentStreamWebSocketHandler with dependencies.
     *
     * @param agentEngineVal agent reasoning engine, must not be null
     * @param sessionManagerVal session manager, must not be null
     * @param objectMapperVal json object mapper, must not be null
     */
    public AgentStreamWebSocketHandler(final AgentEngine agentEngineVal,
                                       final SessionManager sessionManagerVal,
                                       final ObjectMapper objectMapperVal) {
        this.agentEngine = Objects.requireNonNull(agentEngineVal, "agentEngine must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManagerVal, "sessionManager must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapperVal, "objectMapper must not be null");
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) {
        activeSessions.put(session.getId(), session);
        LOGGER.info("WebSocket client connected to agent stream: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        activeSessions.remove(session.getId());
        LOGGER.info("WebSocket client disconnected: {} (status: {})", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) throws Exception {
        final String payload = message.getPayload();
        if (payload == null || payload.isBlank()) {
            return;
        }

        final Map<String, Object> requestMap = objectMapper.readValue(
                payload,
                new TypeReference<Map<String, Object>>() { }
        );
        final Object promptObj = requestMap.get("prompt");
        final String prompt = promptObj != null ? promptObj.toString() : "";

        final Object sessionObj = requestMap.get("sessionId");
        final String sessionId = sessionObj != null ? sessionObj.toString() : "";

        final SessionContext context = sessionManager.getSession(sessionId)
                .orElseGet(sessionManager::getDefaultSession);

        final AgentMessage response = agentEngine.processPrompt(context, prompt);

        // Emit thinking delta if present
        if (response.hasThinking()) {
            sendEvent(session, "thought.delta", Map.of(
                    "sessionId", context.getSessionId(),
                    "thinking", response.getThinking()
            ));
        }

        // Emit final chat content delta
        sendEvent(session, "chat.delta", Map.of(
                "sessionId", context.getSessionId(),
                "content", response.getContent(),
                "messageId", response.getId(),
                "role", response.getRole()
        ));

        // Emit completion frame
        sendEvent(session, "chat.complete", Map.of(
                "sessionId", context.getSessionId(),
                "status", "DONE"
        ));
    }

    private void sendEvent(final WebSocketSession session, final String eventType, final Map<String, Object> data) {
        try {
            final Map<String, Object> frame = Map.of(
                    "type", eventType,
                    "data", data
            );
            final String json = objectMapper.writeValueAsString(frame);
            session.sendMessage(new TextMessage(json));
        } catch (final IOException e) {
            LOGGER.error("Failed to send WebSocket event frame '{}' to session {}", eventType, session.getId(), e);
        }
    }
}
