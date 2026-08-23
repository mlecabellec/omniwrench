package com.omniwrench.tools;

import com.omniwrench.ai.model.ModelManager;
import com.omniwrench.ai.model.ModelRepositoryClient;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for ModelManagementTool.
 *
 * Traceability:
 * - Requirement: REQ-00060 (Modular Pluggable Tool SPI), REQ-00091 (Multi-Source Model Repository Manager)
 * - Feature: FR-00020 (Tool Discovery & Execution Engine)
 * - Task: TSK-20260822-010 (Model Hub Repository Manager)
 * - ADR: ADR-0050 (Model Repository Manager)
 */
@Tag("REQ-00060")
@Tag("REQ-00091")
@Tag("TSK-20260822-010")
class ModelManagementToolTest {

    private ModelManagementTool tool;
    private ModelManager modelManager;
    private SessionContext context;
    private Path tempModelsDir;

    @BeforeEach
    void setUp(@TempDir final Path tempDir) throws IOException {
        this.tempModelsDir = tempDir;
        this.modelManager = new ModelManager(tempModelsDir, new ModelRepositoryClient());
        this.tool = new ModelManagementTool(modelManager);
        this.context = new SessionContext("test-session-models", tempDir.toString());
    }

    @Test
    @DisplayName("Tool definition and metadata should be valid")
    void testToolDefinition() {
        assertThat(tool.getDefinition()).isNotNull();
        assertThat(tool.getDefinition().getName()).isEqualTo("model_manage");
        assertThat(tool.getDefinition().getParameterSchema()).containsKey("action");
    }

    @Test
    @DisplayName("model_manage 'list' action should show empty or installed models")
    void testListAction() throws IOException {
        final ToolInvocation emptyResult = tool.execute(context, Map.of("action", "list"));
        assertThat(emptyResult.isSuccess()).isTrue();
        assertThat(emptyResult.getOutput()).contains("No local models installed");

        // Create a model file
        final Path modelFile = tempModelsDir.resolve("gemma2_2b.gguf");
        Files.writeString(modelFile, "GGUF-MOCK");

        final ToolInvocation listResult = tool.execute(context, Map.of("action", "list"));
        assertThat(listResult.isSuccess()).isTrue();
        assertThat(listResult.getOutput()).contains("gemma2_2b");
    }

    @Test
    @DisplayName("model_manage 'search' action should return catalog results")
    void testSearchAction() {
        final ToolInvocation searchResult = tool.execute(context, Map.of("action", "search", "query", "gemma"));
        assertThat(searchResult.isSuccess()).isTrue();
        assertThat(searchResult.getOutput()).contains("gemma2:2b");
    }

    @Test
    @DisplayName("model_manage 'rm' action should delete local model and report status")
    void testRemoveAction() throws IOException {
        final Path modelFile = tempModelsDir.resolve("to_delete.gguf");
        Files.writeString(modelFile, "DUMMY");
        modelManager.refreshLocalCatalog();

        final ToolInvocation rmResult = tool.execute(context, Map.of("action", "rm", "query", "to_delete"));
        assertThat(rmResult.isSuccess()).isTrue();
        assertThat(rmResult.getOutput()).contains("Successfully removed model: to_delete");
        assertThat(Files.exists(modelFile)).isFalse();

        final ToolInvocation rmNonExistent = tool.execute(context, Map.of("action", "rm", "query", "unknown"));
        assertThat(rmNonExistent.getOutput()).contains("Model not found on disk");
    }
}
