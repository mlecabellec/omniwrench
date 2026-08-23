package com.omniwrench.ai;

import java.util.Objects;

/**
 * Media type classification for AI model requests.
 *
 * <p>Sealed hierarchy covering all AI interaction domains supported by Omniwrench.
 * Each concrete type carries the parameters specific to that modality.
 *
 * Traceability:
 * - Requirement: REQ-00040 (Custom Multi-Modal AI Adapter SPI)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - ADR: ADR-0015 (Future-Proof Multi-Modal SPI)
 */
public sealed interface MediaType
        permits MediaType.TextCompletion,
                MediaType.ChatReasoning,
                MediaType.ImageGeneration,
                MediaType.ImageTransformation,
                MediaType.EmbeddingGeneration,
                MediaType.DataflowProcessing {

    /**
     * Single-turn text completion without message history.
     *
     * @param prompt prompt text
     * @param maxTokens maximum tokens to generate
     */
    record TextCompletion(String prompt, int maxTokens) implements MediaType {
        /**
         * Constructs a text completion request with validation.
         */
        public TextCompletion {
            Objects.requireNonNull(prompt, "prompt must not be null");
            if (maxTokens <= 0) {
                throw new IllegalArgumentException("maxTokens must be positive");
            }
        }
    }

    /**
     * Multi-turn chat reasoning with full conversation context.
     *
     * @param systemPrompt system instructions
     * @param maxTokens maximum tokens to generate
     */
    record ChatReasoning(String systemPrompt, int maxTokens) implements MediaType {
        /**
         * Constructs a chat reasoning request with validation.
         */
        public ChatReasoning {
            Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
            if (maxTokens <= 0) {
                throw new IllegalArgumentException("maxTokens must be positive");
            }
        }
    }

    /**
     * Image generation from a text prompt.
     *
     * @param prompt image prompt
     * @param width image width in pixels
     * @param height image height in pixels
     */
    record ImageGeneration(String prompt, int width, int height) implements MediaType {
        /**
         * Constructs an image generation request with validation.
         */
        public ImageGeneration {
            Objects.requireNonNull(prompt, "prompt must not be null");
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Image dimensions must be positive");
            }
        }
    }

    /**
     * Image transformation / editing (inpainting, style transfer, upscaling).
     *
     * @param instruction transformation instructions
     * @param sourceImage source image bytes
     */
    record ImageTransformation(String instruction, byte[] sourceImage) implements MediaType {
        /**
         * Constructs an image transformation request with validation.
         */
        public ImageTransformation {
            Objects.requireNonNull(instruction, "instruction must not be null");
            Objects.requireNonNull(sourceImage, "sourceImage must not be null");
        }
    }

    /**
     * Dense vector embedding generation for semantic search or RAG.
     *
     * @param text input text to embed
     * @param dimensions target vector dimensions
     */
    record EmbeddingGeneration(String text, int dimensions) implements MediaType {
        /**
         * Constructs an embedding generation request with validation.
         */
        public EmbeddingGeneration {
            Objects.requireNonNull(text, "text must not be null");
            if (dimensions <= 0) {
                throw new IllegalArgumentException("dimensions must be positive");
            }
        }
    }

    /**
     * Structured data processing and transformation via AI.
     *
     * @param schemaHint hint schema format
     * @param payload input payload
     */
    record DataflowProcessing(String schemaHint, String payload) implements MediaType {
        /**
         * Constructs a dataflow processing request with validation.
         */
        public DataflowProcessing {
            Objects.requireNonNull(schemaHint, "schemaHint must not be null");
            Objects.requireNonNull(payload, "payload must not be null");
        }
    }
}
