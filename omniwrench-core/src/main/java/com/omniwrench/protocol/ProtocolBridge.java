package com.omniwrench.protocol;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Pluggable Protocol Bridge SPI.
 *
 * <p>Enables Omniwrench to connect to external event sources and communication
 * protocols (HTTP/REST, WebSocket, Home Assistant, MQTT, etc.) seamlessly
 * within the agent execution loop and event bus.
 *
 * <p>See ADR-0029 for architectural rationale.
 */
public interface ProtocolBridge {

    /**
     * Returns the unique identifier for this protocol bridge.
     *
     * <p>Examples: {@code "home-assistant"}, {@code "websocket-client"}, {@code "http-rest"}.
     *
     * @return the non-null bridge identifier
     */
    String getProtocolId();

    /**
     * Connects to the underlying protocol endpoint or broker.
     *
     * @return a future that completes when connection is established
     */
    CompletableFuture<Void> connect();

    /**
     * Disconnects from the protocol endpoint and releases resources.
     *
     * @return a future that completes when disconnected
     */
    CompletableFuture<Void> disconnect();

    /**
     * Publishes a message to the specified topic or channel.
     *
     * @param message the message to publish, must not be null
     * @return a future that completes with true if delivery succeeded
     */
    CompletableFuture<Boolean> publish(ProtocolMessage message);

    /**
     * Subscribes a listener callback to messages matching the given topic pattern.
     *
     * @param topicPattern the topic pattern or subscription filter
     * @param listener consumer called on each matching message
     * @return a subscription handle or ID
     */
    CompletableFuture<String> subscribe(String topicPattern, Consumer<ProtocolMessage> listener);

    /**
     * Returns true if the protocol bridge is currently connected and healthy.
     *
     * @return connection health status
     */
    boolean isConnected();
}
