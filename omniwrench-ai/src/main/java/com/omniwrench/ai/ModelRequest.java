package com.omniwrench.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Strongly typed AI model request.
 *
 * <p>Encapsulates media type, execution mode, model name, and provider-specific
 * extra parameters in a single immutable envelope.
 *
 * Traceability:
 * - Requirement: REQ-00040 (Custom Multi-Modal AI Adapter SPI)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - ADR: ADR-0015 (Future-Proof Multi-Modal SPI)
 *
 * @param <T> the MediaType classification of this request
 */
public final class ModelRequest<T extends MediaType> {

    /** The media type defining the nature of this request. */
    private final T mediaType;
    /** The execution strategy for dispatching this request. */
    private final ExecutionMode executionMode;
    /** The target model name or alias (e.g. "gpt-4o", "llama3.2", "gemini-2.0-flash"). */
    private final String modelName;
    /** Provider-specific extra parameters (temperature, top_p, etc.). */
    private final Map<String, Object> extraParams;

    /**
     * Constructs a model request with full parameter validation.
     *
     * @param mediaTypeVal the media type, must not be null
     * @param executionModeVal the execution mode, must not be null
     * @param modelNameVal the model identifier, must not be null or blank
     * @param extraParamsVal optional provider-specific parameters, may be null
     */
    public ModelRequest(
            final T mediaTypeVal,
            final ExecutionMode executionModeVal,
            final String modelNameVal,
            final Map<String, Object> extraParamsVal) {
        this.mediaType = Objects.requireNonNull(mediaTypeVal, "mediaType must not be null");
        this.executionMode = Objects.requireNonNull(executionModeVal, "executionMode must not be null");
        this.modelName = Objects.requireNonNull(modelNameVal, "modelName must not be null");
        this.extraParams = extraParamsVal != null
                ? Map.copyOf(new HashMap<>(extraParamsVal))
                : Map.of();
    }

    /**
     * Returns the media type of this request.
     *
     * @return the media type
     */
    public T getMediaType() {
        return mediaType;
    }

    /**
     * Returns the execution mode of this request.
     *
     * @return the execution mode
     */
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    /**
     * Returns the model name of this request.
     *
     * @return the model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Returns an unmodifiable copy of extra parameters.
     *
     * @return unmodifiable parameter map
     */
    public Map<String, Object> getExtraParams() {
        return extraParams;
    }
}
