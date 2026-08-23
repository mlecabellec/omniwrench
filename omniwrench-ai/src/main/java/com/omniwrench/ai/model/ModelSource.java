package com.omniwrench.ai.model;

/**
 * Enumeration of model repository sources supported by Omniwrench.
 *
 * Traceability:
 * - Requirement: REQ-00091 (Multi-Source Model Repository Manager)
 * - Task: TSK-20260822-010 (Model Hub Repository Manager)
 * - ADR: ADR-0015 (Multi-Modal AI Adapter SPI), ADR-0050 (Model Repository Manager)
 */
public enum ModelSource {
    /** Ollama model registry and library (registry.ollama.ai / ollama.com). */
    OLLAMA,
    /** HuggingFace Model Hub (huggingface.co). */
    HUGGING_FACE,
    /** Local on-device cached models directory. */
    LOCAL
}
