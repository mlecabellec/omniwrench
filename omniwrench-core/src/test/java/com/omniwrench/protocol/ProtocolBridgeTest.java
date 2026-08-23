package com.omniwrench.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying ProtocolMessage immutable envelope and ProtocolBridge SPI contracts.
 *
 * Traceability:
 * - Requirement: REQ-00063 (Pluggable Protocol Bridge SPI & Home Assistant Bridge)
 * - Feature: FR-00023 (Pluggable Protocol Bridge & Home Assistant)
 * - Use Case: UC-00008 (Home Assistant Telemetry & Automation)
 * - ADR: ADR-0029 (Home Assistant Protocol Bridge Integration)
 */
@Tag("REQ-00063")
@Tag("FR-00023")
@Tag("UC-00008")
class ProtocolBridgeTest {

    @Test
    @DisplayName("Should construct ProtocolMessage via of() factory method")
    void shouldConstructViaFactoryMethod() {
        final ProtocolMessage message = ProtocolMessage.of("telemetry.temperature", "{\"celsius\": 21.5}");

        assertThat(message.topic()).isEqualTo("telemetry.temperature");
        assertThat(message.payload()).isEqualTo("{\"celsius\": 21.5}");
        assertThat(message.headers()).isEmpty();
        assertThat(message.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce immutability on headers map and reject null timestamp")
    void shouldEnforceHeaderImmutabilityAndNonNull() {
        final Map<String, String> mutableHeaders = new HashMap<>();
        mutableHeaders.put("source", "home-assistant");

        final ProtocolMessage message = new ProtocolMessage("events.motion", "detected", mutableHeaders, Instant.now());
        assertThat(message.headers()).containsEntry("source", "home-assistant");

        assertThrows(UnsupportedOperationException.class, () -> message.headers().put("new", "val"));
        assertThrows(NullPointerException.class, () -> new ProtocolMessage("topic", "payload", mutableHeaders, null));
    }

    @Test
    @DisplayName("Should implement custom ProtocolBridge and handle dispatch lifecycle")
    void shouldSupportCustomProtocolBridgeImplementation() {
        final List<ProtocolMessage> sentMessages = new ArrayList<>();
        final Map<String, Consumer<ProtocolMessage>> subscribers = new HashMap<>();

        final ProtocolBridge mockBridge = new ProtocolBridge() {
            private boolean connected;

            @Override
            public String getProtocolId() {
                return "home-assistant-ws";
            }

            @Override
            public CompletableFuture<Void> connect() {
                this.connected = true;
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> disconnect() {
                this.connected = false;
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Boolean> publish(final ProtocolMessage message) {
                sentMessages.add(message);
                return CompletableFuture.completedFuture(true);
            }

            @Override
            public CompletableFuture<String> subscribe(final String topicPattern, final Consumer<ProtocolMessage> listener) {
                subscribers.put(topicPattern, listener);
                return CompletableFuture.completedFuture("sub-1");
            }

            @Override
            public boolean isConnected() {
                return this.connected;
            }
        };

        assertThat(mockBridge.getProtocolId()).isEqualTo("home-assistant-ws");
        assertThat(mockBridge.isConnected()).isFalse();

        mockBridge.connect().join();
        assertThat(mockBridge.isConnected()).isTrue();

        final ProtocolMessage msg = ProtocolMessage.of("light.living_room.state", "ON");
        final boolean published = mockBridge.publish(msg).join();
        assertThat(published).isTrue();
        assertThat(sentMessages).containsExactly(msg);

        final List<ProtocolMessage> receivedMessages = new ArrayList<>();
        final String subId = mockBridge.subscribe("light.living_room.state", receivedMessages::add).join();
        assertThat(subId).isEqualTo("sub-1");
        assertThat(subscribers).containsKey("light.living_room.state");

        subscribers.get("light.living_room.state").accept(msg);
        assertThat(receivedMessages).containsExactly(msg);

        mockBridge.disconnect().join();
        assertThat(mockBridge.isConnected()).isFalse();
    }
}
