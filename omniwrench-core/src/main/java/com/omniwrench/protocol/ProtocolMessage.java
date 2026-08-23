package com.omniwrench.protocol;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable envelope for messages exchanged across pluggable protocol bridges.
 *
 * <p>Carries topic/channel identifier, payload, metadata headers, and timestamp.
 *
 * Traceability:
 * - Requirement: REQ-00063 (Pluggable Protocol Bridge SPI & Home Assistant Bridge)
 * - Feature: FR-00023 (Pluggable Protocol Bridge & Home Assistant)
 * - Use Case: UC-00008 (Home Assistant Telemetry & Automation)
 * - ADR: ADR-0029 (Home Assistant Protocol Bridge Integration)
 *
 * @param topic the routing key, endpoint, or topic name
 * @param payload the message payload as string or serialized JSON
 * @param headers unmodifiable metadata attributes
 * @param timestamp message creation instant
 */
public record ProtocolMessage(
        String topic,
        String payload,
        Map<String, String> headers,
        Instant timestamp
) {
    /**
     * Constructs a protocol message with non-null validations.
     *
     * @param topic the topic or route, must not be null
     * @param payload the payload, must not be null
     * @param headers metadata headers, may be null
     * @param timestamp the message timestamp, must not be null
     */
    public ProtocolMessage {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        headers = headers != null ? Map.copyOf(headers) : Map.of();
    }

    /**
     * Static factory for quick message construction.
     *
     * @param topic the target topic
     * @param payload the message payload
     * @return a new ProtocolMessage
     */
    public static ProtocolMessage of(final String topic, final String payload) {
        return new ProtocolMessage(topic, payload, Map.of(), Instant.now());
    }
}
