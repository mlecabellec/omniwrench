package com.omniwrench.ai;

import java.util.Objects;

/**
 * Backend adapter SPI for AI inference engines.
 *
 * <p>Each concrete implementation supports one or more backends:
 * OpenAI-compatible endpoints, Torch, llama.cpp, TensorFlow, HuggingFace Transformers.
 * Adapters are discovered via Java {@code ServiceLoader} from plugin JARs (see ADR-0010).
 *
 * Traceability:
 * - Requirement: REQ-00040 (Custom Multi-Modal AI Adapter SPI), REQ-00041 (Multi-Provider Pluggable AI Adapters)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction), FR-00012 (Universal Pluggable AI Adapters)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - ADR: ADR-0010 (Plugin Discovery & ClassLoader Isolation), ADR-0015 (Future-Proof Multi-Modal SPI)
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
