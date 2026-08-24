package com.omniwrench.ai.llamacpp;

import com.omniwrench.ai.BackendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for LlamaCppNativeBridge in-process memory lifecycle and token operations.
 *
 * Traceability:
 * - Requirement: REQ-00090 (Embedded llama.cpp Local LLM Backend Plugin), REQ-00093 (Multi-Architecture Embedded llama.cpp Runtime)
 * - Task: TSK-20260822-015 (In-Process Embedded llama.cpp Engine)
 * - ADR: ADR-0049 (llama.cpp Embedded Inference Engine)
 */
@Tag("REQ-00090")
@Tag("REQ-00093")
@Tag("TSK-20260822-015")
class LlamaCppNativeBridgeTest {

    @Test
    @DisplayName("LlamaCppNativeBridge should fail initialization if model file does not exist")
    void testMissingModelFile() {
        final LlamaCppConfig config = LlamaCppConfig.of("/non/existent/model.gguf");
        final LlamaCppNativeBridge bridge = new LlamaCppNativeBridge(config);

        assertThatThrownBy(bridge::initialize)
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("Model file does not exist");
    }

    @Test
    @DisplayName("LlamaCppNativeBridge should tokenize, decode, sample and clean up memory")
    void testBridgeLifecycleAndTokenization(@TempDir final Path tempDir) throws IOException {
        final Path modelFile = tempDir.resolve("model.gguf");
        Files.writeString(modelFile, "GGUF-HEADER-TEST");

        final LlamaCppConfig config = new LlamaCppConfig(
                modelFile.toString(),
                2048,
                10,
                NativeLibraryLoader.GpuBackend.CPU,
                0,
                2,
                0.7,
                0.9,
                1.1
        );

        try (LlamaCppNativeBridge bridge = new LlamaCppNativeBridge(config)) {
            bridge.initialize();
            assertThat(bridge.isLoaded()).isTrue();

            final int[] tokens = bridge.tokenize("Hello world from Omniwrench");
            assertThat(tokens).isNotEmpty();

            bridge.decode(tokens);

            final int sample = bridge.sampleNext();
            assertThat(sample).isPositive();

            final String piece = bridge.tokenToPiece(sample);
            assertThat(piece).isNotEmpty();
        }

        final LlamaCppNativeBridge closedBridge = new LlamaCppNativeBridge(config);
        closedBridge.initialize();
        closedBridge.close();
        assertThat(closedBridge.isLoaded()).isFalse();

        assertThatThrownBy(() -> closedBridge.tokenize("After close"))
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("closed");
    }
}
