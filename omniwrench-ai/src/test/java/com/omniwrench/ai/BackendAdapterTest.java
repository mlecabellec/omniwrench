package com.omniwrench.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying BackendAdapter SPI contracts, MediaType sealed hierarchy, and BackendException handling.
 *
 * Traceability:
 * - Requirement: REQ-00040 (Custom Multi-Modal AI Adapter SPI), REQ-00041 (Multi-Provider Pluggable AI Adapters)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction), FR-00012 (Universal Pluggable AI Adapters)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - ADR: ADR-0010 (Plugin Discovery & ClassLoader Isolation), ADR-0015 (Future-Proof Multi-Modal SPI)
 */
@Tag("REQ-00040")
@Tag("REQ-00041")
@Tag("FR-00011")
@Tag("FR-00012")
@Tag("UC-00001")
class BackendAdapterTest {

    @Test
    @DisplayName("Should execute request via mock BackendAdapter implementation")
    void shouldExecuteRequestViaAdapter() {
        final BackendAdapter<MediaType.TextCompletion> textAdapter = new BackendAdapter<>() {
            @Override
            public String getBackendId() {
                return "mock-openai";
            }

            @Override
            public boolean supports(final MediaType mediaType, final ExecutionMode mode) {
                return mediaType instanceof MediaType.TextCompletion && mode == ExecutionMode.SYNCHRONOUS;
            }

            @Override
            public ModelResponse<MediaType.TextCompletion> execute(final ModelRequest<MediaType.TextCompletion> request) {
                return new ModelResponse<>(
                        "Mock completion for: " + request.getMediaType().prompt(),
                        request.getMediaType(),
                        10,
                        25,
                        "gpt-4o-mock",
                        Instant.now()
                );
            }
        };

        assertThat(textAdapter.getBackendId()).isEqualTo("mock-openai");

        final MediaType.TextCompletion completion = new MediaType.TextCompletion("Explain quantum computing", 100);
        assertThat(textAdapter.supports(completion, ExecutionMode.SYNCHRONOUS)).isTrue();
        assertThat(textAdapter.supports(completion, ExecutionMode.STREAMING_SSE)).isFalse();

        final ModelRequest<MediaType.TextCompletion> request = new ModelRequest<>(
                completion,
                ExecutionMode.SYNCHRONOUS,
                "gpt-4o",
                Map.of("temperature", 0.7)
        );

        final ModelResponse<MediaType.TextCompletion> response = textAdapter.execute(request);
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("Mock completion for: Explain quantum computing");
        assertThat(response.getInputTokens()).isEqualTo(10);
        assertThat(response.getOutputTokens()).isEqualTo(25);
        assertThat(response.getResolvedModel()).isEqualTo("gpt-4o-mock");
    }

    @Test
    @DisplayName("Should verify BackendException constructors and getters")
    void shouldVerifyBackendException() {
        final BackendException ex1 = new BackendException("llamacpp", "Context overflow");
        assertThat(ex1.getBackendId()).isEqualTo("llamacpp");
        assertThat(ex1.getMessage()).isEqualTo("[llamacpp] Context overflow");

        final RuntimeException cause = new RuntimeException("Socket timeout");
        final BackendException ex2 = new BackendException("openai", "Connection failed", cause);
        assertThat(ex2.getBackendId()).isEqualTo("openai");
        assertThat(ex2.getMessage()).isEqualTo("[openai] Connection failed");
        assertThat(ex2.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should verify all sealed MediaType concrete records and execution modes")
    void shouldVerifyMediaTypesAndModes() {
        assertThat(ExecutionMode.values()).contains(
                ExecutionMode.SYNCHRONOUS,
                ExecutionMode.STREAMING_SSE,
                ExecutionMode.ASYNCHRONOUS_FUTURE,
                ExecutionMode.BACKGROUND_TASK,
                ExecutionMode.PLANNED_SCHEDULED,
                ExecutionMode.TOOL_ENABLED
        );

        final MediaType.TextCompletion tc = new MediaType.TextCompletion("prompt", 50);
        assertThat(tc.prompt()).isEqualTo("prompt");
        assertThat(tc.maxTokens()).isEqualTo(50);

        final MediaType.ChatReasoning cr = new MediaType.ChatReasoning("system", 100);
        assertThat(cr.systemPrompt()).isEqualTo("system");
        assertThat(cr.maxTokens()).isEqualTo(100);

        final MediaType.ImageGeneration ig = new MediaType.ImageGeneration("cat", 512, 512);
        assertThat(ig.prompt()).isEqualTo("cat");
        assertThat(ig.width()).isEqualTo(512);
        assertThat(ig.height()).isEqualTo(512);

        final MediaType.ImageTransformation it = new MediaType.ImageTransformation("enhance", new byte[]{1, 2, 3});
        assertThat(it.instruction()).isEqualTo("enhance");
        assertThat(it.sourceImage()).containsExactly(1, 2, 3);

        final MediaType.EmbeddingGeneration eg = new MediaType.EmbeddingGeneration("vectorize me", 1536);
        assertThat(eg.text()).isEqualTo("vectorize me");
        assertThat(eg.dimensions()).isEqualTo(1536);

        final MediaType.DataflowProcessing df = new MediaType.DataflowProcessing("json-schema", "{\"k\":\"v\"}");
        assertThat(df.schemaHint()).isEqualTo("json-schema");
        assertThat(df.payload()).isEqualTo("{\"k\":\"v\"}");

        assertThrows(IllegalArgumentException.class, () -> new MediaType.TextCompletion("prompt", -1));
        assertThrows(NullPointerException.class, () -> new MediaType.TextCompletion(null, 10));
    }
}
