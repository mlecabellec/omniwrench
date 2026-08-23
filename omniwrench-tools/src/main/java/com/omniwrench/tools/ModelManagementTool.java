package com.omniwrench.tools;

import com.omniwrench.ai.model.DownloadProgress;
import com.omniwrench.ai.model.ModelDescriptor;
import com.omniwrench.ai.model.ModelManager;
import com.omniwrench.ai.model.ModelSource;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Built-in agent tool for searching, pulling, listing, and removing local AI models.
 *
 * Traceability:
 * - Requirement: REQ-00060 (Modular Pluggable Tool SPI), REQ-00091 (Multi-Source Model Repository Manager)
 * - Feature: FR-00020 (Tool Discovery & Execution Engine)
 * - Task: TSK-20260822-010 (Model Hub Repository Manager)
 * - ADR: ADR-0015 (Multi-Modal AI Adapter SPI), ADR-0050 (Model Repository Manager)
 */
@Component
public final class ModelManagementTool implements Tool {

    /** Unique tool name. */
    public static final String TOOL_NAME = "model_manage";

    /** Kilobyte divisor constant. */
    private static final double BYTES_IN_KB = 1024.0;

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelManagementTool.class);

    /** Underlying model manager service. */
    private final ModelManager modelManager;

    /**
     * Constructs the tool with model manager dependency.
     *
     * @param modelManagerVal model manager instance
     */
    public ModelManagementTool(final ModelManager modelManagerVal) {
        this.modelManager = Objects.requireNonNull(modelManagerVal, "modelManager must not be null");
    }

    /**
     * Default constructor for Spring / ServiceLoader bootstrap.
     */
    public ModelManagementTool() {
        this(new ModelManager());
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                TOOL_NAME,
                "Searches, pulls, lists, or removes local GGUF models across Ollama and HuggingFace repositories.",
                Map.of(
                        "action", "Action to perform: 'search', 'pull', 'list', 'rm'",
                        "query", "Search term or model ID (e.g. 'gemma2:2b', 'qwen2.5-coder:1.5b')",
                        "source", "Optional repository source: 'ollama', 'huggingface'"
                )
        );
    }

    @Override
    public ToolInvocation execute(final SessionContext context, final Map<String, Object> parameters) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");

        final String action = Objects.toString(parameters.get("action"), "list").trim().toLowerCase(java.util.Locale.ROOT);
        final String query = Objects.toString(parameters.get("query"), "").trim();
        final String sourceStr = Objects.toString(parameters.get("source"), "").trim().toUpperCase(java.util.Locale.ROOT);

        ModelSource source = null;
        if ("OLLAMA".equals(sourceStr)) {
            source = ModelSource.OLLAMA;
        } else if ("HUGGINGFACE".equals(sourceStr) || "HUGGING_FACE".equals(sourceStr)) {
            source = ModelSource.HUGGING_FACE;
        }

        final Instant startTime = Instant.now();
        final StringBuilder output = new StringBuilder();

        try {
            switch (action) {
                case "list" -> {
                    final List<ModelDescriptor> localModels = modelManager.listLocalModels();
                    if (localModels.isEmpty()) {
                        output.append("No local models installed in ").append(modelManager.getModelsDirectory()).append("\n");
                    } else {
                        output.append("Locally Installed Models (").append(localModels.size()).append("):\n");
                        for (final ModelDescriptor md : localModels) {
                            output.append(String.format(" - %-25s | %-8s | %-8s | %s%n",
                                    md.id(), md.parameterSize(), formatBytes(md.fileSizeBytes()), md.localPath()));
                        }
                    }
                }
                case "search" -> {
                    final List<ModelDescriptor> results = modelManager.searchRemoteModels(query, source);
                    output.append("Model Hub Search Results for '").append(query).append("' (").append(results.size()).append("):\n");
                    for (final ModelDescriptor md : results) {
                        output.append(String.format(" - %-28s [%-11s] | %-6s | %s%n",
                                md.id(), md.source(), md.parameterSize(), md.name()));
                    }
                }
                case "pull" -> {
                    if (query.isEmpty()) {
                        output.append("Error: 'query' (modelId) parameter required for 'pull' action.");
                    } else {
                        output.append("Pulling model '").append(query).append("'...\n");
                        final ModelDescriptor downloaded = modelManager.pullModel(query, source, (DownloadProgress p) -> {
                            LOGGER.debug("Pulling {}: {}%", p.modelId(), String.format("%.1f", p.percentage()));
                        });
                        output.append("Successfully downloaded and installed: ").append(downloaded.id())
                                .append(" -> ").append(downloaded.localPath());
                    }
                }
                case "rm", "delete", "remove" -> {
                    if (query.isEmpty()) {
                        output.append("Error: 'query' (modelId) parameter required for 'rm' action.");
                    } else {
                        final boolean removed = modelManager.removeModel(query);
                        if (removed) {
                            output.append("Successfully removed model: ").append(query);
                        } else {
                            output.append("Model not found on disk: ").append(query);
                        }
                    }
                }
                default -> output.append("Unknown action: ").append(action).append(". Valid actions: list, search, pull, rm");
            }
        } catch (final Exception e) {
            output.append("Model management error: ").append(e.getMessage());
            LOGGER.error("Model management failed", e);
        }

        final boolean success = !output.toString().contains("Error");
        return new ToolInvocation(
                java.util.UUID.randomUUID().toString(),
                TOOL_NAME,
                parameters,
                output.toString().trim(),
                success,
                startTime
        );
    }

    private String formatBytes(final long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        final int digitGroups = (int) (Math.log10(bytes) / Math.log10(BYTES_IN_KB));
        return String.format("%.1f %s", bytes / Math.pow(BYTES_IN_KB, digitGroups), units[digitGroups]);
    }
}
