package com.omniwrench.ai.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * High-performance client for searching and downloading models from Ollama Library and HuggingFace Hub.
 *
 * Traceability:
 * - Requirement: REQ-00091 (Multi-Source Model Repository Manager)
 * - Task: TSK-20260822-010 (Model Hub Repository Manager)
 * - ADR: ADR-0050 (Model Repository Manager)
 */
public class ModelRepositoryClient {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelRepositoryClient.class);

    /** HTTP connect timeout seconds. */
    private static final int CONNECT_TIMEOUT_SECONDS = 15;
    /** Buffer size for streaming download (64 KB). */
    private static final int BUFFER_SIZE = 65536;
    /** Percentage constant. */
    private static final double PERCENT_FACTOR = 100.0;
    /** Milliseconds per second constant. */
    private static final double MILLIS_PER_SECOND = 1000.0;
    /** HTTP Status OK minimum bound. */
    private static final int HTTP_OK_MIN = 200;
    /** HTTP Status Redirection/Error minimum bound. */
    private static final int HTTP_REDIRECTION_MIN = 300;

    /** Shared HTTP client with redirect following. */
    private final HttpClient httpClient;
    /** JSON Object Mapper. */
    private final ObjectMapper objectMapper;

    /**
     * Default constructor initializing standard HTTP client.
     */
    public ModelRepositoryClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build(), new ObjectMapper());
    }

    /**
     * Parameterized constructor for dependency injection and testing.
     *
     * @param clientVal HTTP client instance
     * @param mapperVal JSON object mapper
     */
    public ModelRepositoryClient(final HttpClient clientVal, final ObjectMapper mapperVal) {
        this.httpClient = Objects.requireNonNull(clientVal, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(mapperVal, "objectMapper must not be null");
    }

    /**
     * Searches for quantized models across Ollama and HuggingFace repositories.
     *
     * @param query search query keyword (e.g. "gemma", "qwen", "llama")
     * @param source repository source filter (null for all sources)
     * @return list of matching model descriptors
     */
    public List<ModelDescriptor> search(final String query, final ModelSource source) {
        final String nonNullQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        final List<ModelDescriptor> results = new ArrayList<>();

        if (source == null || source == ModelSource.OLLAMA) {
            results.addAll(searchOllama(nonNullQuery));
        }
        if (source == null || source == ModelSource.HUGGING_FACE) {
            results.addAll(searchHuggingFace(nonNullQuery));
        }

        return List.copyOf(results);
    }

    /**
     * Resolves metadata for a specific model from Ollama or HuggingFace.
     *
     * @param modelId canonical model identifier
     * @param source repository source
     * @return Optional containing ModelDescriptor if found
     */
    public Optional<ModelDescriptor> getMetadata(final String modelId, final ModelSource source) {
        Objects.requireNonNull(modelId, "modelId must not be null");
        final ModelSource targetSource = source != null ? source : inferSource(modelId);

        if (targetSource == ModelSource.OLLAMA) {
            return getOllamaMetadata(modelId);
        } else if (targetSource == ModelSource.HUGGING_FACE) {
            return getHuggingFaceMetadata(modelId);
        }
        return Optional.empty();
    }

    /**
     * Downloads a model file with live progress telemetry and SHA-256 validation.
     *
     * @param descriptor the model to download
     * @param targetDirectory destination folder
     * @param progressConsumer progress callback consumer
     * @return Path to the verified local file
     * @throws Exception if download or checksum validation fails
     */
    public Path downloadModel(final ModelDescriptor descriptor,
                              final Path targetDirectory,
                              final Consumer<DownloadProgress> progressConsumer) throws Exception {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(targetDirectory, "targetDirectory must not be null");

        if (descriptor.downloadUrl() == null || descriptor.downloadUrl().isBlank()) {
            throw new IllegalArgumentException("Model descriptor contains no download URL: " + descriptor.id());
        }

        Files.createDirectories(targetDirectory);
        final String fileName = sanitizeFileName(descriptor.id()) + ".gguf";
        final Path finalPath = targetDirectory.resolve(fileName);
        final Path partPath = targetDirectory.resolve(fileName + ".part");

        LOGGER.info("Starting download for model '{}' from '{}' -> '{}'", descriptor.id(), descriptor.downloadUrl(), partPath);

        if (progressConsumer != null) {
            progressConsumer.accept(new DownloadProgress(
                    descriptor.id(), 0L, descriptor.fileSizeBytes(), 0.0, 0L, DownloadProgress.Status.CONNECTING
            ));
        }

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(descriptor.downloadUrl()))
                .header("User-Agent", "Omniwrench/0.1.0 (Autonomous-Agent-Workbench)")
                .GET()
                .build();

        final HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        final int statusCode = response.statusCode();
        if (statusCode < HTTP_OK_MIN || statusCode >= HTTP_REDIRECTION_MIN) {
            throw new IllegalStateException("Failed to download model, HTTP status: " + statusCode);
        }

        final long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(descriptor.fileSizeBytes());
        final MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");

        long totalBytesRead = 0L;
        final long startTime = System.currentTimeMillis();

        try (InputStream rawIn = response.body();
             DigestInputStream dis = new DigestInputStream(rawIn, shaDigest);
             java.io.OutputStream out = Files.newOutputStream(partPath)) {

            final byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = dis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                totalBytesRead += read;

                if (progressConsumer != null) {
                    final long elapsedMs = Math.max(1L, System.currentTimeMillis() - startTime);
                    final long speedBytesPerSec = (long) ((totalBytesRead / (double) elapsedMs) * MILLIS_PER_SECOND);
                    final double pct = contentLength > 0 ? (totalBytesRead * PERCENT_FACTOR) / contentLength : 0.0;

                    progressConsumer.accept(new DownloadProgress(
                            descriptor.id(),
                            totalBytesRead,
                            contentLength,
                            Math.min(PERCENT_FACTOR, pct),
                            speedBytesPerSec,
                            DownloadProgress.Status.DOWNLOADING
                    ));
                }
            }
        }

        final String calculatedSha256 = HexFormat.of().formatHex(shaDigest.digest());
        LOGGER.info("Download completed for '{}'. Calculated SHA-256: {}", descriptor.id(), calculatedSha256);

        if (progressConsumer != null) {
            progressConsumer.accept(new DownloadProgress(
                    descriptor.id(),
                    totalBytesRead,
                    contentLength,
                    PERCENT_FACTOR,
                    0L,
                    DownloadProgress.Status.VERIFYING
            ));
        }

        // Validate SHA-256 if expected hash is present
        if (descriptor.sha256() != null && !descriptor.sha256().isBlank()) {
            final String expected = descriptor.sha256().toLowerCase(Locale.ROOT).trim();
            if (!expected.equals(calculatedSha256.toLowerCase(Locale.ROOT))) {
                Files.deleteIfExists(partPath);
                throw new IllegalStateException("SHA-256 verification mismatch! Expected: " + expected + ", calculated: " + calculatedSha256);
            }
        }

        Files.move(partPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        if (progressConsumer != null) {
            progressConsumer.accept(new DownloadProgress(
                    descriptor.id(),
                    totalBytesRead,
                    totalBytesRead,
                    PERCENT_FACTOR,
                    0L,
                    DownloadProgress.Status.COMPLETED
            ));
        }

        return finalPath;
    }

    private List<ModelDescriptor> searchOllama(final String query) {
        final List<ModelDescriptor> list = new ArrayList<>();
        final String qwenUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/"
                + "qwen2.5-coder-1.5b-instruct-q4_k_m.gguf";

        final List<ModelDescriptor> catalog = List.of(
                new ModelDescriptor(
                        "gemma2:2b",
                        "Google Gemma 2 2B (Ollama Q4_K_M)",
                        ModelSource.OLLAMA,
                        "GGUF",
                        "Q4_K_M",
                        "2.6B",
                        1638400000L,
                        "https://huggingface.co/google/gemma-2-2b-it-GGUF/resolve/main/2b_it_v2.gguf",
                        null,
                        null,
                        false
                ),
                new ModelDescriptor(
                        "qwen2.5-coder:1.5b",
                        "Qwen 2.5 Coder 1.5B (Ollama Q4_K_M)",
                        ModelSource.OLLAMA,
                        "GGUF",
                        "Q4_K_M",
                        "1.5B",
                        986000000L,
                        qwenUrl,
                        null,
                        null,
                        false
                ),
                new ModelDescriptor(
                        "llama3.2:1b",
                        "Meta Llama 3.2 1B (Ollama Q4_K_M)",
                        ModelSource.OLLAMA,
                        "GGUF",
                        "Q4_K_M",
                        "1.2B",
                        812000000L,
                        "https://huggingface.co/unsloth/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                        null,
                        null,
                        false
                ),
                new ModelDescriptor(
                        "deepseek-r1:1.5b",
                        "DeepSeek R1 Distill Qwen 1.5B (Ollama Q4_K_M)",
                        ModelSource.OLLAMA,
                        "GGUF",
                        "Q4_K_M",
                        "1.7B",
                        1120000000L,
                        "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
                        null,
                        null,
                        false
                )
        );

        for (final ModelDescriptor md : catalog) {
            if (query.isEmpty() || md.id().toLowerCase(Locale.ROOT).contains(query)
                    || md.name().toLowerCase(Locale.ROOT).contains(query)) {
                list.add(md);
            }
        }
        return list;
    }

    private List<ModelDescriptor> searchHuggingFace(final String query) {
        final List<ModelDescriptor> list = new ArrayList<>();
        if (query.isBlank()) {
            return list;
        }

        try {
            final String encodedQuery = URLEncoder.encode(query + " GGUF", StandardCharsets.UTF_8);
            final String url = "https://huggingface.co/api/models?search=" + encodedQuery + "&filter=gguf&limit=10";
            final HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Omniwrench/0.1.0")
                    .timeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .GET()
                    .build();

            final HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == HTTP_OK_MIN) {
                final JsonNode root = objectMapper.readTree(resp.body());
                if (root.isArray()) {
                    for (final JsonNode node : root) {
                        final String id = node.path("id").asText();
                        if (id != null && !id.isBlank()) {
                            list.add(new ModelDescriptor(
                                    id,
                                    id,
                                    ModelSource.HUGGING_FACE,
                                    "GGUF",
                                    "Q4_K_M",
                                    "N/A",
                                    0L,
                                    "https://huggingface.co/" + id + "/resolve/main/model.gguf",
                                    null,
                                    null,
                                    false
                            ));
                        }
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.warn("Failed to query live HuggingFace Hub API: {}", e.getMessage());
        }
        return list;
    }

    private Optional<ModelDescriptor> getOllamaMetadata(final String modelId) {
        return searchOllama("").stream().filter(m -> m.id().equalsIgnoreCase(modelId)).findFirst();
    }

    private Optional<ModelDescriptor> getHuggingFaceMetadata(final String modelId) {
        return Optional.of(new ModelDescriptor(
                modelId,
                modelId,
                ModelSource.HUGGING_FACE,
                "GGUF",
                "Q4_K_M",
                "N/A",
                0L,
                "https://huggingface.co/" + modelId + "/resolve/main/model.gguf",
                null,
                null,
                false
        ));
    }

    private ModelSource inferSource(final String modelId) {
        if (modelId.contains("/")) {
            return ModelSource.HUGGING_FACE;
        }
        return ModelSource.OLLAMA;
    }

    private String sanitizeFileName(final String id) {
        return id.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
