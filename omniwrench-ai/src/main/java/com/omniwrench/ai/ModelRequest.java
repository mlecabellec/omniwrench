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
     * @param mediaType the media type, must not be null
     * @param executionMode the execution mode, must not be null
     * @param modelName the model identifier, must not be null or blank
     * @param extraParams optional provider-specific parameters, may be null
     */
    public ModelRequest(
            final T mediaType,
            final ExecutionMode executionMode,
            final String modelName,
            final Map<String, Object> extraParams) {
        this.mediaType = Objects.requireNonNull(mediaType, "mediaType must not be null");
        this.executionMode = Objects.requireNonNull(executionMode, "executionMode must not be null");
        this.modelName = Objects.requireNonNull(modelName, "modelName must not be null");
        this.extraParams = extraParams != null
                ? Map.copyOf(new HashMap<>(extraParams))
                : Map.of();
    }

    /** Returns the media type of this request. */
    public T getMediaType() {
        return mediaType;
    }

    /** Returns the execution mode of this request. */
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    /** Returns the model name of this request. */
    public String getModelName() {
        return modelName;
    }

    /** Returns an unmodifiable copy of extra parameters. */
    public Map<String, Object> getExtraParams() {
        return extraParams;
    }
}
