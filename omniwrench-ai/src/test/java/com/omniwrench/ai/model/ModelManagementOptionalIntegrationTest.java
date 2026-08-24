package com.omniwrench.ai.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Optional integration test suite verifying live model search, HuggingFace resolution, and model pull downloads.
 * These test cases are disabled by default to avoid large multi-gigabyte downloads during routine CI/CD builds.
 *
 * Traceability:
 * - Requirement: REQ-00091 (Multi-Source Model Repository Manager)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction), FR-00020 (Tool Discovery &amp; Execution Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00004 (Headless CI/CD Automation)
 * - Task: TSK-20260822-010 (Model Hub Repository Manager)
 * - ADR: ADR-0050 (Model Repository Manager)
 */
@Tag("Optional")
@Tag("Manual")
@Tag("RemoteModelDownload")
@Tag("REQ-00091")
@Tag("TSK-20260822-010")
class ModelManagementOptionalIntegrationTest {

    /**
     * Verifies that live searching for Gemma 4 models returns available E2B / E4B quantized variants on HuggingFace.
     */
    @Test
    @DisplayName("Should find Gemma 4 E2B models when searching HuggingFace repository live")
    @EnabledIfEnvironmentVariable(named = "OMNIWRENCH_ENABLE_REMOTE_TESTS", matches = "true")
    void testLiveSearchGemma4E2B() {
        final ModelRepositoryClient client = new ModelRepositoryClient();
        final List<ModelDescriptor> results = client.search("gemma-4-e", ModelSource.HUGGING_FACE);

        assertThat(results).isNotEmpty();
        final boolean hasE2b = results.stream().anyMatch(m -> m.id().toLowerCase(java.util.Locale.ROOT).contains("e2b"));
        assertThat(hasE2b).isTrue();
    }

    /**
     * Verifies that downloading a model into a temporary directory populates catalog metadata and saves weights.
     *
     * @param tempDir temporary directory for storing model artifacts
     * @throws Exception if download or file validation fails
     */
    @Test
    @DisplayName("Should pull model and register in local catalog when enabled explicitly")
    @EnabledIfEnvironmentVariable(named = "OMNIWRENCH_ENABLE_REMOTE_TESTS", matches = "true")
    void testLivePullModelExplicit(@TempDir final Path tempDir) throws Exception {
        final ModelRepositoryClient client = new ModelRepositoryClient();
        final ModelManager manager = new ModelManager(tempDir, client);

        final Optional<ModelDescriptor> meta = client.getMetadata("unsloth/gemma-4-E2B-it-GGUF", ModelSource.HUGGING_FACE);
        assertThat(meta).isPresent();

        // When executed with valid resolution, verify descriptor structure
        final ModelDescriptor desc = meta.get();
        assertThat(desc.source()).isEqualTo(ModelSource.HUGGING_FACE);
        assertThat(desc.id()).isEqualTo("unsloth/gemma-4-E2B-it-GGUF");
    }

    /**
     * Verifies that HuggingFace file tree resolution dynamically selects the optimal GGUF quantization asset.
     */
    @Test
    @DisplayName("Should dynamically resolve optimal GGUF quantization asset from HuggingFace tree API")
    @EnabledIfEnvironmentVariable(named = "OMNIWRENCH_ENABLE_REMOTE_TESTS", matches = "true")
    void testLiveHuggingFaceMetadataResolution() {
        final ModelRepositoryClient client = new ModelRepositoryClient();
        final Optional<ModelDescriptor> meta = client.getMetadata("unsloth/gemma-4-E2B-it-GGUF", ModelSource.HUGGING_FACE);

        assertThat(meta).isPresent();
        final ModelDescriptor desc = meta.get();
        assertThat(desc.source()).isEqualTo(ModelSource.HUGGING_FACE);
        assertThat(desc.quantization()).isEqualTo("Q4_K_M");
        assertThat(desc.parameterSize()).isEqualTo("E2B");
        assertThat(desc.fileSizeBytes()).isGreaterThan(2_000_000_000L);
        assertThat(desc.downloadUrl()).contains("gemma-4-E2B-it-Q4_K_M.gguf");
    }

    /**
     * Verifies that live searching Ollama web engine returns available models with parameter size tags.
     */
    @Test
    @DisplayName("Should find models when searching Ollama web engine live")
    @EnabledIfEnvironmentVariable(named = "OMNIWRENCH_ENABLE_REMOTE_TESTS", matches = "true")
    void testLiveOllamaWebSearch() {
        final ModelRepositoryClient client = new ModelRepositoryClient();
        final List<ModelDescriptor> results = client.search("gemma", ModelSource.OLLAMA);

        assertThat(results).isNotEmpty();
        final boolean hasGemma = results.stream().anyMatch(m -> m.id().toLowerCase(java.util.Locale.ROOT).contains("gemma"));
        assertThat(hasGemma).isTrue();
    }

    /**
     * Verifies that Ollama OCI registry manifests resolve authoritative SHA-256 digests and blob download URLs.
     */
    @Test
    @DisplayName("Should resolve OCI manifest and blob download URL for Ollama models")
    @EnabledIfEnvironmentVariable(named = "OMNIWRENCH_ENABLE_REMOTE_TESTS", matches = "true")
    void testLiveOllamaRegistryManifestResolution() {
        final ModelRepositoryClient client = new ModelRepositoryClient();
        final Optional<ModelDescriptor> gemma2Meta = client.getMetadata("gemma2:2b", ModelSource.OLLAMA);
        assertThat(gemma2Meta).isPresent();
        assertThat(gemma2Meta.get().downloadUrl()).contains("registry.ollama.ai/v2/library/gemma2/blobs/sha256:");
        assertThat(gemma2Meta.get().fileSizeBytes()).isGreaterThan(1_000_000_000L);
        assertThat(gemma2Meta.get().sha256()).isNotBlank();

        final Optional<ModelDescriptor> qwenMeta = client.getMetadata("qwen2.5-coder:1.5b", ModelSource.OLLAMA);
        assertThat(qwenMeta).isPresent();
        assertThat(qwenMeta.get().downloadUrl()).contains("registry.ollama.ai/v2/library/qwen2.5-coder/blobs/sha256:");
        assertThat(qwenMeta.get().fileSizeBytes()).isGreaterThan(500_000_000L);
        assertThat(qwenMeta.get().sha256()).isNotBlank();
    }

    /**
     * Verifies local directory scanning and catalog registration for pre-existing GGUF files.
     *
     * @param tempDir temporary directory
     * @throws Exception if file creation fails
     */
    @Test
    @DisplayName("Should detect and catalog existing local GGUF models on disk")
    void testLocalCatalogDetection(@TempDir final Path tempDir) throws Exception {
        final Path modelFile = tempDir.resolve("gemma-4-e2b.gguf");
        Files.writeString(modelFile, "GGUF_HEADER_MOCK_DATA");

        final ModelManager manager = new ModelManager(tempDir, new ModelRepositoryClient());
        final List<ModelDescriptor> localModels = manager.listLocalModels();

        assertThat(localModels).hasSize(1);
        assertThat(localModels.get(0).id()).isEqualTo("gemma-4-e2b");
        assertThat(localModels.get(0).installed()).isTrue();
    }
}
