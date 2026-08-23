package com.omniwrench.ai.llamacpp;

/**
 * Execution configuration for the embedded llama.cpp runtime.
 *
 * Traceability:
 * - Requirement: REQ-00090 (Embedded llama.cpp Local LLM Backend Plugin)
 * - Task: TSK-20260822-009 (Embedded llama.cpp Backend)
 * - ADR: ADR-0049 (llama.cpp Embedded Inference Engine)
 *
 * @param modelPath filesystem path to the GGUF model weights
 * @param contextSize maximum token context window (e.g. 2048, 4096, 8192)
 * @param gpuLayers number of layers to offload to GPU/VRAM (0 for pure CPU)
 * @param threads CPU threads allocated for token evaluation
 * @param temperature generation sampling temperature (0.0 to 2.0)
 * @param topP top-p nucleus sampling probability (0.0 to 1.0)
 * @param repeatPenalty repetition penalty multiplier (e.g. 1.1)
 */
public record LlamaCppConfig(
        String modelPath,
        int contextSize,
        int gpuLayers,
        int threads,
        double temperature,
        double topP,
        double repeatPenalty
) {
    /** Default context length. */
    public static final int DEFAULT_CONTEXT_SIZE = 4096;
    /** Default thread count. */
    public static final int DEFAULT_THREADS = 4;
    /** Default sampling temperature. */
    public static final double DEFAULT_TEMPERATURE = 0.7;
    /** Default top-p probability. */
    public static final double DEFAULT_TOP_P = 0.9;
    /** Default repeat penalty. */
    public static final double DEFAULT_REPEAT_PENALTY = 1.1;

    /**
     * Creates a default configuration targeting a specific model file.
     *
     * @param path GGUF model path
     * @return initialized LlamaCppConfig
     */
    public static LlamaCppConfig of(final String path) {
        return new LlamaCppConfig(
                path,
                DEFAULT_CONTEXT_SIZE,
                0,
                DEFAULT_THREADS,
                DEFAULT_TEMPERATURE,
                DEFAULT_TOP_P,
                DEFAULT_REPEAT_PENALTY
        );
    }
}
