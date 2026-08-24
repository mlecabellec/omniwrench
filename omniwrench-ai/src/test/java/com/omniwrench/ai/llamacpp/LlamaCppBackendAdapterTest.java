package com.omniwrench.ai.llamacpp;

import com.omniwrench.ai.BackendException;
import com.omniwrench.ai.ExecutionMode;
import com.omniwrench.ai.MediaType;
import com.omniwrench.ai.ModelRequest;
import com.omniwrench.ai.ModelResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for LlamaCppBackendAdapter and LlamaCppConfig.
 *
 * Traceability:
 * - Requirement: REQ-00090 (Embedded llama.cpp Local LLM Backend Plugin with JNI/FFM Bindings)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction), FR-00012 (Universal Pluggable AI Adapters)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-009 (Embedded llama.cpp Backend), TSK-20260822-015 (Multi-Arch llama.cpp Engine)
 * - ADR: ADR-0049 (llama.cpp Embedded Inference Engine)
 */
@Tag("REQ-00090")
@Tag("REQ-00093")
@Tag("TSK-20260822-009")
@Tag("TSK-20260822-015")
class LlamaCppBackendAdapterTest {

    @Test
    @DisplayName("LlamaCppConfig should hold valid default hyperparameters")
    void testLlamaCppConfigDefaults() {
        final LlamaCppConfig config = LlamaCppConfig.of("/path/to/model.gguf");

        assertThat(config.modelPath()).isEqualTo("/path/to/model.gguf");
        assertThat(config.contextSize()).isEqualTo(4096);
        assertThat(config.threads()).isEqualTo(4);
        assertThat(config.temperature()).isEqualTo(0.7);
        assertThat(config.topP()).isEqualTo(0.9);
        assertThat(config.repeatPenalty()).isEqualTo(1.1);
        assertThat(config.gpuBackend()).isEqualTo(NativeLibraryLoader.GpuBackend.CPU);
        assertThat(config.gpuLayers()).isZero();
    }

    @Test
    @DisplayName("LlamaCppBackendAdapter should declare backend ID and supported media types")
    void testAdapterCapabilities() {
        final LlamaCppBackendAdapter adapter = new LlamaCppBackendAdapter(LlamaCppConfig.of("/test/path.gguf"));

        assertThat(adapter.getBackendId()).isEqualTo("llamacpp");
        assertThat(adapter.supports(new MediaType.ChatReasoning("text", 100), ExecutionMode.SYNCHRONOUS)).isTrue();
        assertThat(adapter.supports(new MediaType.TextCompletion("text", 100), ExecutionMode.SYNCHRONOUS)).isTrue();
        assertThat(adapter.supports(new MediaType.ImageGeneration("png", 512, 512), ExecutionMode.SYNCHRONOUS)).isFalse();
    }

    @Test
    @DisplayName("LlamaCppBackendAdapter should throw BackendException if model file does not exist")
    void testMissingModelFileThrows() {
        final LlamaCppBackendAdapter adapter = new LlamaCppBackendAdapter(LlamaCppConfig.of("/non/existent/model.gguf"));
        final ModelRequest<MediaType.ChatReasoning> request = new ModelRequest<>(
                new MediaType.ChatReasoning("Hello llama!", 50),
                ExecutionMode.SYNCHRONOUS,
                "test-model",
                null
        );

        assertThatThrownBy(() -> adapter.execute(request))
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("Model file does not exist");
    }

    @Test
    @DisplayName("LlamaCppBackendAdapter should execute inference when model file exists")
    void testInferenceExecution(@TempDir final Path tempDir) throws IOException {
        final Path modelFile = tempDir.resolve("test_weights.gguf");
        Files.writeString(modelFile, "GGUF_MOCK_HEADER");

        final LlamaCppBackendAdapter adapter = new LlamaCppBackendAdapter(LlamaCppConfig.of(modelFile.toString()));
        final ModelRequest<MediaType.ChatReasoning> request = new ModelRequest<>(
                new MediaType.ChatReasoning("Explain quantum computing in one sentence", 100),
                ExecutionMode.SYNCHRONOUS,
                "test_weights",
                null
        );

        final ModelResponse<MediaType.ChatReasoning> response = adapter.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotEmpty();
        assertThat(response.getInputTokens()).isPositive();
        assertThat(response.getOutputTokens()).isPositive();
        assertThat(response.getResolvedModel()).isEqualTo("test_weights");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("LlamaCppBackendAdapter should stream tokens reactively via executeStream")
    void testStreamingExecution(@TempDir final Path tempDir) throws IOException {
        final Path modelFile = tempDir.resolve("stream_weights.gguf");
        Files.writeString(modelFile, "GGUF_STREAM_HEADER");

        final LlamaCppBackendAdapter adapter = new LlamaCppBackendAdapter(LlamaCppConfig.of(modelFile.toString()));
        final ModelRequest<MediaType.ChatReasoning> request = new ModelRequest<>(
                new MediaType.ChatReasoning("Stream token prompt", 5),
                ExecutionMode.SYNCHRONOUS,
                "stream_weights",
                null
        );

        final Flux<String> stream = adapter.executeStream(request);
        final List<String> emittedTokens = stream.collectList().block();

        assertThat(emittedTokens).isNotNull();
        assertThat(emittedTokens).hasSize(5);
        assertThat(emittedTokens.get(0)).contains("token_");
    }
}
