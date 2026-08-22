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
 * - Requirement: REQ-00052 (Real-time WebSocket Streaming)
 * - Task: TSK-20260822-004 (Spring Web & Reactive WebSocket Server Engine)
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(new TextWebSocketHandler(), "/ws/agent-stream")
                .setAllowedOrigins("*");
    }
}
