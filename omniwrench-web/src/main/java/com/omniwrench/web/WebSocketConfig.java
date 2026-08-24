package com.omniwrench.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Objects;

/**
 * WebSocket endpoint configuration for real-time agent token and telemetry streaming.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST &amp; WebSocket Server API), REQ-00052 (Real-time WebSocket Streaming)
 * - Requirement: REQ-00088 (Dual Chat Mode Reasoning Demultiplexing)
 * - Feature: FR-00016 (Real-Time Reactive WebSocket Token Streaming)
 * - Use Case: UC-00003 (Web UI Agent Collaboration)
 * - Task: TSK-20260822-004 (Spring Web &amp; Reactive WebSocket Server Engine)
 * - Task: TSK-20260822-007 (Dual Chat Mode &amp; Thinking Demux)
 * - ADR: ADR-0003 (Spring Web MVC &amp; Reactive WebSocket Streaming Architecture), ADR-0047 (Reasoning Demux)
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /** Handler for streaming text and thinking tokens. */
    private final AgentStreamWebSocketHandler agentStreamWebSocketHandler;

    /**
     * Constructs WebSocketConfig with injected stream handler.
     *
     * @param handler the stream websocket handler, must not be null
     */
    public WebSocketConfig(final AgentStreamWebSocketHandler handler) {
        this.agentStreamWebSocketHandler = Objects.requireNonNull(handler, "handler must not be null");
    }

    @Override
    public final void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(agentStreamWebSocketHandler, "/ws/agent-stream")
                .setAllowedOrigins("*");
    }
}
