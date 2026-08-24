package com.omniwrench.core;

import com.omniwrench.model.DemuxedChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite verifying StreamDemuxer reasoning separation, tag buffering, and edge case resilience.
 *
 * Traceability:
 * - Requirement: REQ-00088 (Dual Chat Mode with Explicit Reasoning & Thinking Demultiplexing)
 * - Task: TSK-20260822-007 (Dual Chat Mode & Thinking Stream Demultiplexing)
 * - ADR: ADR-0047 (Dual Chat Mode & Explicit Reasoning Demux)
 */
@Tag("REQ-00088")
@Tag("TSK-20260822-007")
class StreamDemuxerTest {

    @Test
    @DisplayName("StreamDemuxer.parse should separate <think> tags from final answer in static text")
    void testStaticParseWithThinkTags() {
        final String rawText = "<think>\nStep 1: Check inputs.\nStep 2: Formulate response.\n</think>\nHere is the final answer.";
        final StreamDemuxer.DemuxResult result = StreamDemuxer.parse(rawText);

        assertThat(result.thought()).isEqualTo("Step 1: Check inputs.\nStep 2: Formulate response.");
        assertThat(result.answer()).isEqualTo("Here is the final answer.");
    }

    @Test
    @DisplayName("StreamDemuxer.parse should handle content without think tags gracefully")
    void testStaticParseWithoutThinkTags() {
        final String rawText = "Simple plain text answer without thoughts.";
        final StreamDemuxer.DemuxResult result = StreamDemuxer.parse(rawText);

        assertThat(result.thought()).isEmpty();
        assertThat(result.answer()).isEqualTo("Simple plain text answer without thoughts.");
    }

    @Test
    @DisplayName("StreamDemuxer.parse should throw NullPointerException on null input")
    void testParseNullThrows() {
        assertThatThrownBy(() -> StreamDemuxer.parse(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rawContent must not be null");
    }

    @Test
    @DisplayName("StreamDemuxer streaming token processing should classify chunks across token boundaries")
    void testStreamingTokenProcessing() {
        final StreamDemuxer demuxer = new StreamDemuxer();

        final List<DemuxedChunk> chunk1 = demuxer.processToken("<th");
        assertThat(chunk1).isEmpty(); // buffered

        final List<DemuxedChunk> chunk2 = demuxer.processToken("ink>Reasoning token 1 ");
        assertThat(chunk2).isNotEmpty();
        assertThat(chunk2.get(0).thought()).isTrue();
        assertThat(chunk2.get(0).text()).contains("Reasoning token 1 ");

        final List<DemuxedChunk> chunk3 = demuxer.processToken("Reasoning token 2 </think>Final response");
        assertThat(chunk3).isNotEmpty();

        final List<DemuxedChunk> flushed = demuxer.flush();
        assertThat(flushed).isEmpty();
    }
}
