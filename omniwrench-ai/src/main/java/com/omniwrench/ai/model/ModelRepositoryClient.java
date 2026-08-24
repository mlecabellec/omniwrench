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
    /** SHA-256 prefix length for 'sha256:'. */
    private static final int SHA256_PREFIX_LENGTH = 7;

    /** Regex matching Ollama search model cards. */
    private static final java.util.regex.Pattern OLLAMA_CARD_PATTERN = java.util.regex.Pattern.compile(
            "<a\\s+href=\"/(library/[^\"]+|[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+)\"[^>]*>([\\s\\S]*?)</a>"
    );
    /** Regex matching Ollama model card description paragraph. */
    private static final java.util.regex.Pattern OLLAMA_DESC_PATTERN = java.util.regex.Pattern.compile(
            "<p class=\"[^\"]*text-neutral-800[^\"]*\">([^<]+)</p>"
    );
    /** Regex matching Ollama parameter size badge tags. */
    private static final java.util.regex.Pattern OLLAMA_TAG_PATTERN = java.util.regex.Pattern.compile(
            "text-blue-600[^\"]*\">([^<]+)</span>"
    );

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

        final HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(descriptor.downloadUrl()))
                .header("User-Agent", "Omniwrench/0.1.0 (Autonomous-Agent-Workbench)")
                .GET();
        appendAuthHeaderIfPresent(reqBuilder);

        final HttpRequest request = reqBuilder.build();

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
                throw new IllegalStateException("SHA-256 verification mismatch! Expected: "
                        + expected + ", calculated: " + calculatedSha256);
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
        try {
            final String queryStr = query == null ? "" : query.trim();
            final String encoded = URLEncoder.encode(queryStr, StandardCharsets.UTF_8);
            final String url = "https://ollama.com/search?q=" + encoded;
            final HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Omniwrench/0.1.0")
                    .timeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .GET()
                    .build();

            final HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == HTTP_OK_MIN) {
                final java.util.regex.Matcher matcher = OLLAMA_CARD_PATTERN.matcher(resp.body());
                while (matcher.find()) {
                    final String rawHref = matcher.group(1);
                    final String modelSlug = rawHref.replace("library/", "");
                    final String cardContent = matcher.group(2);

                    final java.util.regex.Matcher descMatcher = OLLAMA_DESC_PATTERN.matcher(cardContent);
                    final String desc = descMatcher.find() ? descMatcher.group(1).trim() : modelSlug;

                    final List<String> tags = new ArrayList<>();
                    final java.util.regex.Matcher tagMatcher = OLLAMA_TAG_PATTERN.matcher(cardContent);
                    while (tagMatcher.find()) {
                        tags.add(tagMatcher.group(1).trim());
                    }

                    final String paramSize = tags.isEmpty() ? "LATEST" : String.join(", ", tags);
                    list.add(new ModelDescriptor(
                            modelSlug,
                            desc,
                            ModelSource.OLLAMA,
                            "GGUF",
                            "Q4_K_M",
                            paramSize,
                            0L,
                            null,
                            null,
                            null,
                            false
                    ));
                }
            }
        } catch (final Exception e) {
            LOGGER.warn("Failed querying live Ollama search for '{}': {}", query, e.getMessage());
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
            final HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Omniwrench/0.1.0")
                    .timeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .GET();
            appendAuthHeaderIfPresent(reqBuilder);

            final HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
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
        final String repo;
        final String tag;
        if (modelId.contains(":")) {
            final String[] parts = modelId.split(":", 2);
            repo = parts[0].trim();
            tag = parts[1].trim();
        } else {
            repo = modelId.trim();
            tag = "latest";
        }

        try {
            final String manifestUrl = "https://registry.ollama.ai/v2/library/" + repo + "/manifests/" + tag;
            final HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(manifestUrl))
                    .header("Accept", "application/vnd.docker.distribution.manifest.v2+json")
                    .header("User-Agent", "Omniwrench/0.1.0")
                    .timeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .GET()
                    .build();

            final HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == HTTP_OK_MIN) {
                final JsonNode root = objectMapper.readTree(resp.body());
                final JsonNode layers = root.path("layers");
                if (layers.isArray()) {
                    for (final JsonNode layer : layers) {
                        final String mediaType = layer.path("mediaType").asText();
                        if ("application/vnd.ollama.image.model".equals(mediaType)) {
                            final String digest = layer.path("digest").asText();
                            final long size = layer.path("size").asLong(0L);
                            final String cleanSha = digest.startsWith("sha256:")
                                    ? digest.substring(SHA256_PREFIX_LENGTH) : digest;
                            final String blobUrl = "https://registry.ollama.ai/v2/library/" + repo + "/blobs/" + digest;

                            return Optional.of(new ModelDescriptor(
                                    modelId,
                                    repo + " (" + tag + ")",
                                    ModelSource.OLLAMA,
                                    "GGUF",
                                    "Q4_K_M",
                                    tag.toUpperCase(Locale.ROOT),
                                    size,
                                    blobUrl,
                                    cleanSha,
                                    null,
                                    false
                            ));
                        }
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.warn("Failed resolving Ollama registry manifest for '{}': {}", modelId, e.getMessage());
        }

        return Optional.empty();
    }

    private Optional<ModelDescriptor> getHuggingFaceMetadata(final String modelId) {
        try {
            final String treeUrl = "https://huggingface.co/api/models/" + modelId + "/tree/main";
            final HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(treeUrl))
                    .header("User-Agent", "Omniwrench/0.1.0")
                    .timeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .GET();
            appendAuthHeaderIfPresent(reqBuilder);

            final HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == HTTP_OK_MIN) {
                final JsonNode root = objectMapper.readTree(resp.body());
                if (root.isArray()) {
                    final GgufFileSelection selection = selectOptimalGguf(root);
                    if (selection != null) {
                        return Optional.of(new ModelDescriptor(
                                modelId,
                                modelId,
                                ModelSource.HUGGING_FACE,
                                "GGUF",
                                selection.quantization(),
                                selection.parameterSize(),
                                selection.sizeBytes(),
                                "https://huggingface.co/" + modelId + "/resolve/main/" + selection.filename(),
                                null,
                                null,
                                false
                        ));
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.warn("Failed resolving HuggingFace file tree for '{}': {}", modelId, e.getMessage());
        }

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

    private void appendAuthHeaderIfPresent(final HttpRequest.Builder builder) {
        final String envToken = System.getenv("HF_TOKEN");
        final String hubToken = System.getenv("HUGGING_FACE_HUB_TOKEN");
        final String propToken = System.getProperty("hf.token");
        String token = null;
        if (envToken != null && !envToken.isBlank()) {
            token = envToken.trim();
        } else if (hubToken != null && !hubToken.isBlank()) {
            token = hubToken.trim();
        } else if (propToken != null && !propToken.isBlank()) {
            token = propToken.trim();
        }

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
    }

    private GgufFileSelection selectOptimalGguf(final JsonNode treeArray) {
        GgufFileSelection best = null;
        int bestScore = -1;

        for (final JsonNode node : treeArray) {
            final String path = node.path("path").asText("");
            if (path.endsWith(".gguf") && !path.startsWith("mmproj")) {
                final long size = node.path("size").asLong(0L);
                final int score = scoreQuantization(path);
                if (score > bestScore) {
                    bestScore = score;
                    final String quant = extractQuantization(path);
                    final String paramSize = extractParameterSize(path);
                    best = new GgufFileSelection(path, quant, paramSize, size);
                }
            }
        }
        return best;
    }

    private int scoreQuantization(final String filename) {
        final String upper = filename.toUpperCase(Locale.ROOT);
        final int scoreQ4KM = 100;
        final int scoreQ40 = 90;
        final int scoreQ4KS = 85;
        final int scoreQ41 = 80;
        final int scoreQ5KM = 70;
        final int scoreQ3KM = 60;
        final int scoreQ3KS = 50;
        final int scoreIQ4 = 40;
        final int scoreFallback = 10;

        if (upper.contains("Q4_K_M")) {
            return scoreQ4KM;
        } else if (upper.contains("Q4_0")) {
            return scoreQ40;
        } else if (upper.contains("Q4_K_S")) {
            return scoreQ4KS;
        } else if (upper.contains("Q4_1")) {
            return scoreQ41;
        } else if (upper.contains("Q5_K_M") || upper.contains("Q5_0")) {
            return scoreQ5KM;
        } else if (upper.contains("Q3_K_M")) {
            return scoreQ3KM;
        } else if (upper.contains("Q3_K_S")) {
            return scoreQ3KS;
        } else if (upper.contains("IQ4")) {
            return scoreIQ4;
        }
        return scoreFallback;
    }

    private String extractQuantization(final String filename) {
        final String upper = filename.toUpperCase(Locale.ROOT);
        if (upper.contains("Q4_K_M")) {
            return "Q4_K_M";
        } else if (upper.contains("Q4_0")) {
            return "Q4_0";
        } else if (upper.contains("Q4_K_S")) {
            return "Q4_K_S";
        } else if (upper.contains("Q4_1")) {
            return "Q4_1";
        } else if (upper.contains("Q5_K_M")) {
            return "Q5_K_M";
        } else if (upper.contains("Q3_K_M")) {
            return "Q3_K_M";
        } else if (upper.contains("Q3_K_S")) {
            return "Q3_K_S";
        } else if (upper.contains("Q8_0")) {
            return "Q8_0";
        } else if (upper.contains("IQ4_NL")) {
            return "IQ4_NL";
        } else if (upper.contains("IQ4_XS")) {
            return "IQ4_XS";
        }
        return "GGUF";
    }

    private String extractParameterSize(final String filename) {
        final String upper = filename.toUpperCase(Locale.ROOT);
        if (upper.contains("E2B")) {
            return "E2B";
        } else if (upper.contains("E4B")) {
            return "E4B";
        } else if (upper.contains("1.5B")) {
            return "1.5B";
        } else if (upper.contains("2B")) {
            return "2B";
        } else if (upper.contains("3B")) {
            return "3B";
        } else if (upper.contains("7B")) {
            return "7B";
        } else if (upper.contains("8B")) {
            return "8B";
        } else if (upper.contains("12B")) {
            return "12B";
        } else if (upper.contains("26B")) {
            return "26B";
        } else if (upper.contains("31B")) {
            return "31B";
        }
        return "N/A";
    }

    private record GgufFileSelection(String filename, String quantization, String parameterSize, long sizeBytes) {
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
