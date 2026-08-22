package com.omniwrench.ai;

import java.util.Objects;

/**
 * Backend adapter SPI for AI inference engines.
 *
 * <p>Each concrete implementation supports one or more backends:
 * OpenAI-compatible endpoints, Torch, llama.cpp, TensorFlow, HuggingFace Transformers.
 * Adapters are discovered via Java {@code ServiceLoader} from plugin JARs (see ADR-0010).
 *
 * <p>See ADR-0015: Custom Future-Proof Multi-Modal AI Adapter SPI.
 *
 * @param <T> the MediaType this adapter processes
 */
public interface BackendAdapter<T extends MediaType> {

    /**
     * Returns the unique identifier of this backend adapter.
     *
     * <p>Examples: {@code "openai-compatible"}, {@code "llamacpp"}, {@code "huggingface"}.
     *
     * @return the non-null backend identifier
     */
    String getBackendId();

    /**
     * Returns true if this adapter can handle the given media type and execution mode.
     *
     * @param mediaType the request media type, must not be null
     * @param mode the execution mode, must not be null
     * @return true if this adapter supports the combination
     */
    default boolean supports(final MediaType mediaType, final ExecutionMode mode) {
        Objects.requireNonNull(mediaType, "mediaType must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        return false;
    }

    /**
     * Executes a model request and returns a typed response.
     *
     * @param request the request to execute, must not be null
     * @return the model response, never null
     * @throws BackendException if the backend returns an error
     */
    ModelResponse<T> execute(ModelRequest<T> request);
}
