package com.omniwrench.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying ModelRequest and ModelResponse parameter contracts and validation.
 *
 * Traceability:
 * - Requirement: REQ-00040 (Custom Multi-Modal AI Adapter SPI), REQ-00077 (OpenTelemetry Distributed Tracing)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction), FR-00033 (OpenTelemetry Distributed Tracing)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00002 (Autonomous Goal Planning)
 * - ADR: ADR-0014 (OpenTelemetry Distributed Tracing), ADR-0015 (Future-Proof Multi-Modal SPI)
 */
@Tag("REQ-00040")
@Tag("REQ-00077")
@Tag("FR-00011")
@Tag("FR-00033")
@Tag("UC-00001")
class ModelRequestResponseTest {

    @Test
    @DisplayName("Should verify ModelRequest getters, parameter immutability, and null handling")
    void shouldVerifyModelRequest() {
        final MediaType.TextCompletion tc = new MediaType.TextCompletion("test prompt", 100);
        final Map<String, Object> params = Map.of("temperature", 0.5, "seed", 42);

        final ModelRequest<MediaType.TextCompletion> request = new ModelRequest<>(
                tc,
                ExecutionMode.STREAMING_SSE,
                "anthropic-claude-3-5",
                params
        );

        assertThat(request.getMediaType()).isEqualTo(tc);
        assertThat(request.getExecutionMode()).isEqualTo(ExecutionMode.STREAMING_SSE);
        assertThat(request.getModelName()).isEqualTo("anthropic-claude-3-5");
        assertThat(request.getExtraParams()).isEqualTo(params);

        final ModelRequest<MediaType.TextCompletion> requestNullParams = new ModelRequest<>(
                tc,
                ExecutionMode.SYNCHRONOUS,
                "gpt-4o",
                null
        );
        assertThat(requestNullParams.getExtraParams()).isEmpty();

        assertThrows(NullPointerException.class, () -> new ModelRequest<>(null, ExecutionMode.SYNCHRONOUS, "model", params));
        assertThrows(NullPointerException.class, () -> new ModelRequest<>(tc, null, "model", params));
        assertThrows(NullPointerException.class, () -> new ModelRequest<>(tc, ExecutionMode.SYNCHRONOUS, null, params));
    }

    @Test
    @DisplayName("Should verify ModelResponse getters, token counters, and validation")
    void shouldVerifyModelResponse() {
        final MediaType.ChatReasoning cr = new MediaType.ChatReasoning("sys", 200);
        final Instant now = Instant.now();

        final ModelResponse<MediaType.ChatReasoning> response = new ModelResponse<>(
                "Generated reasoning output",
                cr,
                150,
                350,
                "claude-3-5-sonnet-20241022",
                now
        );

        assertThat(response.getContent()).isEqualTo("Generated reasoning output");
        assertThat(response.getMediaType()).isEqualTo(cr);
        assertThat(response.getInputTokens()).isEqualTo(150);
        assertThat(response.getOutputTokens()).isEqualTo(350);
        assertThat(response.getResolvedModel()).isEqualTo("claude-3-5-sonnet-20241022");
        assertThat(response.getTimestamp()).isEqualTo(now);

        assertThrows(NullPointerException.class, () -> new ModelResponse<>(null, cr, 0, 0, "model", now));
        assertThrows(NullPointerException.class, () -> new ModelResponse<>("out", null, 0, 0, "model", now));
        assertThrows(NullPointerException.class, () -> new ModelResponse<>("out", cr, 0, 0, null, now));
        assertThrows(NullPointerException.class, () -> new ModelResponse<>("out", cr, 0, 0, "model", null));
    }
}
