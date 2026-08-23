package com.omniwrench.ai.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Local model manager providing persistent caching, catalog registry, pull downloads, and removal.
 *
 * Traceability:
 * - Requirement: REQ-00091 (Multi-Source Model Repository Manager)
 * - Task: TSK-20260822-010 (Model Hub Repository Manager)
 * - ADR: ADR-0050 (Model Repository Manager)
 */
public class ModelManager {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelManager.class);

    /** Default subdirectory under user home or workspace. */
    private static final String DEFAULT_MODELS_DIR_NAME = ".omniwrench/models";
    /** Catalog metadata JSON filename. */
    private static final String CATALOG_FILE_NAME = "catalog.json";

    /** Storage directory path. */
    private final Path modelsDirectory;
    /** Model repository client. */
    private final ModelRepositoryClient repositoryClient;
    /** JSON mapper. */
    private final ObjectMapper objectMapper;
    /** In-memory cached catalog. */
    private final Map<String, ModelDescriptor> installedCatalog = new ConcurrentHashMap<>();

    /**
     * Constructs a ModelManager with default directory (~/.omniwrench/models).
     */
    public ModelManager() {
        this(resolveDefaultDirectory(), new ModelRepositoryClient());
    }

    /**
     * Parameterized constructor.
     *
     * @param modelsDirVal path to store models
     * @param clientVal model repository client
     */
    public ModelManager(final Path modelsDirVal, final ModelRepositoryClient clientVal) {
        this.modelsDirectory = Objects.requireNonNull(modelsDirVal, "modelsDirectory must not be null");
        this.repositoryClient = Objects.requireNonNull(clientVal, "repositoryClient must not be null");
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        try {
            Files.createDirectories(this.modelsDirectory);
        } catch (final IOException e) {
            LOGGER.error("Failed to create models directory at '{}'", this.modelsDirectory, e);
        }
        refreshLocalCatalog();
    }

    /**
     * Returns the models directory path.
     *
     * @return Path to models directory
     */
    public Path getModelsDirectory() {
        return modelsDirectory;
    }

    /**
     * Lists all locally installed and available models.
     *
     * @return immutable list of ModelDescriptors
     */
    public List<ModelDescriptor> listLocalModels() {
        refreshLocalCatalog();
        return List.copyOf(installedCatalog.values());
    }

    /**
     * Retrieves a local model descriptor by ID if installed.
     *
     * @param modelId model identifier
     * @return Optional containing ModelDescriptor if found locally
     */
    public Optional<ModelDescriptor> getLocalModel(final String modelId) {
        if (modelId == null) {
            return Optional.empty();
        }
        refreshLocalCatalog();
        return Optional.ofNullable(installedCatalog.get(modelId.toLowerCase(java.util.Locale.ROOT)));
    }

    /**
     * Searches remote repositories for available models.
     *
     * @param query search query
     * @param source repository filter
     * @return list of matching models
     */
    public List<ModelDescriptor> searchRemoteModels(final String query, final ModelSource source) {
        return repositoryClient.search(query, source);
    }

    /**
     * Downloads and installs a model into the local repository with live progress tracking.
     *
     * @param modelId model identifier to pull
     * @param source source repository
     * @param progressConsumer progress update callback
     * @return verified local ModelDescriptor
     * @throws Exception if download or checksum validation fails
     */
    public ModelDescriptor pullModel(final String modelId,
                                     final ModelSource source,
                                     final Consumer<DownloadProgress> progressConsumer) throws Exception {
        Objects.requireNonNull(modelId, "modelId must not be null");

        final Optional<ModelDescriptor> metaOpt = repositoryClient.getMetadata(modelId, source);
        if (metaOpt.isEmpty()) {
            throw new IllegalArgumentException("Model not found in repository: " + modelId);
        }

        final ModelDescriptor meta = metaOpt.get();
        final Path targetFile = repositoryClient.downloadModel(meta, modelsDirectory, progressConsumer);
        final ModelDescriptor installed = meta.withLocalPath(targetFile, null);

        installedCatalog.put(installed.id().toLowerCase(java.util.Locale.ROOT), installed);
        saveCatalogMetadata();
        return installed;
    }

    /**
     * Removes a local model and deletes its weight file from disk.
     *
     * @param modelId model identifier
     * @return true if deleted, false if not found
     */
    public boolean removeModel(final String modelId) {
        if (modelId == null) {
            return false;
        }
        final String key = modelId.toLowerCase(java.util.Locale.ROOT);
        final ModelDescriptor descriptor = installedCatalog.remove(key);
        if (descriptor != null && descriptor.localPath() != null) {
            try {
                Files.deleteIfExists(descriptor.localPath());
                saveCatalogMetadata();
                LOGGER.info("Successfully deleted model '{}' from disk at '{}'", modelId, descriptor.localPath());
                return true;
            } catch (final IOException e) {
                LOGGER.error("Failed to delete model file for '{}'", modelId, e);
                return false;
            }
        }
        return false;
    }

    /**
     * Re-scans the local models directory and synchronizes metadata.
     */
    public synchronized void refreshLocalCatalog() {
        if (!Files.exists(modelsDirectory)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modelsDirectory, "*.gguf")) {
            for (final Path file : stream) {
                final String fileName = file.getFileName().toString();
                final String modelId = fileName.substring(0, fileName.length() - ".gguf".length());
                final String key = modelId.toLowerCase(java.util.Locale.ROOT);

                if (!installedCatalog.containsKey(key)) {
                    final long size = Files.size(file);
                    final ModelDescriptor desc = new ModelDescriptor(
                            modelId,
                            modelId,
                            ModelSource.LOCAL,
                            "GGUF",
                            "QUANTIZED",
                            "LOCAL",
                            size,
                            null,
                            null,
                            file,
                            true
                    );
                    installedCatalog.put(key, desc);
                }
            }
        } catch (final IOException e) {
            LOGGER.warn("Failed scanning models directory '{}': {}", modelsDirectory, e.getMessage());
        }
    }

    private void saveCatalogMetadata() {
        final Path catalogPath = modelsDirectory.resolve(CATALOG_FILE_NAME);
        try {
            final List<ModelDescriptor> list = new ArrayList<>(installedCatalog.values());
            objectMapper.writeValue(catalogPath.toFile(), list);
        } catch (final IOException e) {
            LOGGER.warn("Failed writing model catalog metadata to '{}': {}", catalogPath, e.getMessage());
        }
    }

    private static Path resolveDefaultDirectory() {
        final String userHome = System.getProperty("user.home", ".");
        return Paths.get(userHome, DEFAULT_MODELS_DIR_NAME);
    }
}
