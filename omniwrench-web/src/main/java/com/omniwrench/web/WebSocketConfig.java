package com.omniwrench.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket endpoint configuration for real-time agent token and telemetry streaming.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST & WebSocket Agent Server API), REQ-00052 (Real-time WebSocket Streaming)
 * - Feature: FR-00016 (Real-Time Reactive WebSocket Token Streaming)
 * - Use Case: UC-00003 (Web UI Agent Collaboration)
 * - Task: TSK-20260822-004 (Spring Web & Reactive WebSocket Server Engine)
 * - ADR: ADR-0003 (Spring Web MVC & Reactive WebSocket Streaming Architecture)
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public final void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(new TextWebSocketHandler(), "/ws/agent-stream")
                .setAllowedOrigins("*");
    }
}
