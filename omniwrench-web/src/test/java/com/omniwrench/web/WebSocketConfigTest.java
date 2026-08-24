package com.omniwrench.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying WebSocket endpoint registration and CORS policies for agent streaming.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST &amp; WebSocket Agent Server API), REQ-00052 (Real-time WebSocket Streaming)
 * - Feature: FR-00016 (Real-Time Reactive WebSocket Token Streaming)
 * - Use Case: UC-00003 (Web UI Agent Collaboration)
 * - Task: TSK-20260822-004 (Spring Web &amp; Reactive WebSocket Server Engine)
 * - ADR: ADR-0003 (Spring Web MVC &amp; Reactive WebSocket Streaming Architecture)
 */
@Tag("REQ-00050")
@Tag("REQ-00052")
@Tag("FR-00016")
@Tag("UC-00003")
@Tag("TSK-20260822-004")
class WebSocketConfigTest {

    @Test
    @DisplayName("Should register /ws/agent-stream endpoint with wildcard allowed origins")
    void shouldRegisterWebSocketHandlers() {
        final AgentStreamWebSocketHandler mockHandler = Mockito.mock(AgentStreamWebSocketHandler.class);
        final WebSocketConfig config = new WebSocketConfig(mockHandler);
        final WebSocketHandlerRegistry registry = Mockito.mock(WebSocketHandlerRegistry.class);
        final WebSocketHandlerRegistration registration = Mockito.mock(WebSocketHandlerRegistration.class);

        when(registry.addHandler(any(AgentStreamWebSocketHandler.class), eq("/ws/agent-stream"))).thenReturn(registration);
        when(registration.setAllowedOrigins("*")).thenReturn(registration);

        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(any(AgentStreamWebSocketHandler.class), eq("/ws/agent-stream"));
        verify(registration).setAllowedOrigins("*");
    }
}
