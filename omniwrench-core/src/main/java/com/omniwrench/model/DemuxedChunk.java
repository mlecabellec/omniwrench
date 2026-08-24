package com.omniwrench.model;

import java.util.Objects;

/**
 * Represents a demultiplexed chunk of text identified as either reasoning thoughts or final response.
 *
 * Traceability:
 * - Requirement: REQ-00088 (Dual Chat Mode with Explicit Reasoning & Thinking Demultiplexing)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction), FR-00014 (Hybrid Reasoning Loop)
 * - Task: TSK-20260822-007 (Dual Chat Mode & Thinking Stream Demultiplexing)
 * - ADR: ADR-0047 (Dual Chat Mode & Explicit Reasoning Demux)
 *
 * @param text the text content of the chunk, must not be null
 * @param thought true if this chunk represents internal reasoning/thinking, false if final answer
 */
public record DemuxedChunk(String text, boolean thought) {

    /**
     * Compact constructor validating non-null requirements.
     */
    public DemuxedChunk {
        Objects.requireNonNull(text, "text must not be null");
    }
}
