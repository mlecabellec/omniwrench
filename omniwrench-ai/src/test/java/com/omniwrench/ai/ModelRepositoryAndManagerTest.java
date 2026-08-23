package com.omniwrench.ai.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for ModelRepositoryClient and ModelManager.
 *
 * Traceability:
 * - Requirement: REQ-00091 (Multi-Source Model Repository Manager)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction), FR-00012 (Universal Pluggable AI Adapters)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-010 (Model Hub Repository Manager)
 * - ADR: ADR-0050 (Model Repository Manager)
 */
@Tag("REQ-00091")
@Tag("TSK-20260822-010")
class ModelRepositoryAndManagerTest {

    @Test
    @DisplayName("ModelDescriptor record should enforce non-null invariants and support withLocalPath copy")
    void testModelDescriptorContract(@TempDir final Path tempDir) {
        final ModelDescriptor desc = new ModelDescriptor(
                "gemma2:2b",
                "Google Gemma 2 2B",
                ModelSource.OLLAMA,
                "GGUF",
                "Q4_K_M",
                "2.6B",
                1600000000L,
                "https://example.com/gemma2.gguf",
                "sha256-test",
                null,
                false
        );

        assertThat(desc.id()).isEqualTo("gemma2:2b");
        assertThat(desc.installed()).isFalse();
        assertThat(desc.localPath()).isNull();

        final Path localFile = tempDir.resolve("gemma2_2b.gguf");
        final ModelDescriptor installed = desc.withLocalPath(localFile, "verified-sha256");

        assertThat(installed.installed()).isTrue();
        assertThat(installed.localPath()).isEqualTo(localFile);
        assertThat(installed.sha256()).isEqualTo("verified-sha256");
    }

    @Test
    @DisplayName("ModelRepositoryClient should search Ollama and HuggingFace models")
    void testRepositoryClientSearch() {
        final ModelRepositoryClient client = new ModelRepositoryClient();

        final List<ModelDescriptor> ollamaResults = client.search("gemma", ModelSource.OLLAMA);
        assertThat(ollamaResults).isNotEmpty();
        assertThat(ollamaResults.get(0).source()).isEqualTo(ModelSource.OLLAMA);
        assertThat(ollamaResults.get(0).id()).contains("gemma");

        final List<ModelDescriptor> allResults = client.search("llama", null);
        assertThat(allResults).isNotEmpty();

        final Optional<ModelDescriptor> metadataOpt = client.getMetadata("gemma2:2b", ModelSource.OLLAMA);
        assertThat(metadataOpt).isPresent();
        assertThat(metadataOpt.get().quantization()).isEqualTo("Q4_K_M");
    }

    @Test
    @DisplayName("ModelManager should discover, list, and delete local GGUF models on disk")
    void testModelManagerLocalLifecycle(@TempDir final Path tempDir) throws IOException {
        final Path modelFile1 = tempDir.resolve("qwen2.5-coder_1.5b.gguf");
        final Path modelFile2 = tempDir.resolve("deepseek-r1_1.5b.gguf");

        Files.writeString(modelFile1, "GGUF-MOCK-WEIGHTS-1");
        Files.writeString(modelFile2, "GGUF-MOCK-WEIGHTS-2");

        final ModelManager manager = new ModelManager(tempDir, new ModelRepositoryClient());
        final List<ModelDescriptor> localModels = manager.listLocalModels();

        assertThat(localModels).hasSize(2);
        assertThat(manager.getLocalModel("qwen2.5-coder_1.5b")).isPresent();
        assertThat(manager.getLocalModel("deepseek-r1_1.5b")).isPresent();
        assertThat(manager.getLocalModel("non-existent")).isEmpty();

        // Test model removal
        final boolean removed = manager.removeModel("qwen2.5-coder_1.5b");
        assertThat(removed).isTrue();
        assertThat(Files.exists(modelFile1)).isFalse();
        assertThat(manager.listLocalModels()).hasSize(1);

        // Deleting non-existent should safely return false
        assertThat(manager.removeModel("non-existent")).isFalse();
    }

    @Test
    @DisplayName("DownloadProgress record should properly hold metrics")
    void testDownloadProgressRecord() {
        final DownloadProgress progress = new DownloadProgress(
                "gemma2:2b",
                500000000L,
                1000000000L,
                50.0,
                25000000L,
                DownloadProgress.Status.DOWNLOADING
        );

        assertThat(progress.modelId()).isEqualTo("gemma2:2b");
        assertThat(progress.percentage()).isEqualTo(50.0);
        assertThat(progress.status()).isEqualTo(DownloadProgress.Status.DOWNLOADING);
    }
}
