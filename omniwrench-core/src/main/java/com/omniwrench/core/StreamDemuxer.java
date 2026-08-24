package com.omniwrench.core;

import com.omniwrench.model.DemuxedChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stream demultiplexer that separates internal model reasoning thoughts from final response content.
 *
 * <p>Supports both batch extraction and streaming token-by-token demultiplexing for DeepSeek-R1,
 * OpenAI o1/o3, Gemma, Qwen, and custom thinking models (ADR-0047).
 *
 * Traceability:
 * - Requirement: REQ-00088 (Dual Chat Mode with Explicit Reasoning & Thinking Demultiplexing)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction), FR-00014 (Hybrid Reasoning Loop)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-007 (Dual Chat Mode & Thinking Stream Demultiplexing)
 * - ADR: ADR-0047 (Dual Chat Mode & Explicit Reasoning Demux)
 */
public final class StreamDemuxer {

    /** Regex pattern matching &lt;think&gt;...&lt;/think&gt; or [THOUGHT]...[/THOUGHT] tags across lines. */
    private static final Pattern THINK_TAG_PATTERN = Pattern.compile(
            "(?s)(<think>|\\[THOUGHT\\]|<reasoning>)(.*?)(</think>|\\[/THOUGHT\\]|</reasoning>|$)"
    );

    /** Thinking start tag. */
    public static final String TAG_THINK_OPEN = "<think>";
    /** Thinking end tag. */
    public static final String TAG_THINK_CLOSE = "</think>";

    /**
     * Immutable result holding separated thought content and final answer text.
     *
     * @param thought extracted reasoning thoughts (or empty string if none)
     * @param answer final answer content with thinking tags removed
     */
    public record DemuxResult(String thought, String answer) {
        /**
         * Compact constructor validating non-null requirements.
         */
        public DemuxResult {
            Objects.requireNonNull(thought, "thought must not be null");
            Objects.requireNonNull(answer, "answer must not be null");
        }
    }

    /** Stream state tracker. */
    private boolean insideThinkingBlock;
    /** Incomplete token buffer. */
    private final StringBuilder tokenBuffer = new StringBuilder();

    /**
     * Constructs a new stateful StreamDemuxer instance for streaming demultiplexing.
     */
    public StreamDemuxer() {
        this.insideThinkingBlock = false;
    }

    /**
     * Parses a complete static response string and separates thoughts from final answer.
     *
     * @param rawContent raw model response text, must not be null
     * @return non-null DemuxResult containing thought text and cleaned answer text
     */
    public static DemuxResult parse(final String rawContent) {
        final String nonNullRaw = Objects.requireNonNull(rawContent, "rawContent must not be null");

        final StringBuilder thoughts = new StringBuilder();
        final StringBuilder answer = new StringBuilder();

        final Matcher matcher = THINK_TAG_PATTERN.matcher(nonNullRaw);
        int lastEnd = 0;

        while (matcher.find()) {
            // Append preceding regular text
            answer.append(nonNullRaw, lastEnd, matcher.start());

            // Append thought text
            final String extractedThought = matcher.group(2);
            if (extractedThought != null && !extractedThought.isBlank()) {
                if (thoughts.length() > 0) {
                    thoughts.append("\n");
                }
                thoughts.append(extractedThought.trim());
            }

            lastEnd = matcher.end();
        }

        // Append remaining text after last match
        if (lastEnd < nonNullRaw.length()) {
            answer.append(nonNullRaw.substring(lastEnd));
        }

        return new DemuxResult(thoughts.toString().trim(), answer.toString().trim());
    }

    /**
     * Ingests a single token chunk in a streaming session and emits classified demuxed chunks.
     *
     * @param token newly received token chunk, must not be null
     * @return list of classified DemuxedChunks
     */
    public synchronized List<DemuxedChunk> processToken(final String token) {
        final String nonNullToken = Objects.requireNonNull(token, "token must not be null");
        final List<DemuxedChunk> result = new ArrayList<>();

        tokenBuffer.append(nonNullToken);
        final String current = tokenBuffer.toString();

        if (!insideThinkingBlock) {
            final int openIndex = current.indexOf(TAG_THINK_OPEN);
            if (openIndex != -1) {
                if (openIndex > 0) {
                    result.add(new DemuxedChunk(current.substring(0, openIndex), false));
                }
                insideThinkingBlock = true;
                tokenBuffer.delete(0, openIndex + TAG_THINK_OPEN.length());
            } else {
                // Check if buffer ends with a prefix of <think>
                if (isPrefixOf(current, TAG_THINK_OPEN)) {
                    // Retain in buffer for next token
                    return result;
                }
                result.add(new DemuxedChunk(current, false));
                tokenBuffer.setLength(0);
            }
        }

        if (insideThinkingBlock) {
            final String thoughtText = tokenBuffer.toString();
            final int closeIndex = thoughtText.indexOf(TAG_THINK_CLOSE);
            if (closeIndex != -1) {
                if (closeIndex > 0) {
                    result.add(new DemuxedChunk(thoughtText.substring(0, closeIndex), true));
                }
                insideThinkingBlock = false;
                tokenBuffer.delete(0, closeIndex + TAG_THINK_CLOSE.length());
                if (tokenBuffer.length() > 0) {
                    result.add(new DemuxedChunk(tokenBuffer.toString(), false));
                    tokenBuffer.setLength(0);
                }
            } else {
                if (isPrefixOf(thoughtText, TAG_THINK_CLOSE)) {
                    return result;
                }
                result.add(new DemuxedChunk(thoughtText, true));
                tokenBuffer.setLength(0);
            }
        }

        return List.copyOf(result);
    }

    /**
     * Flushes any remaining text in the internal buffer upon stream completion.
     *
     * @return remaining DemuxedChunks if buffer non-empty
     */
    public synchronized List<DemuxedChunk> flush() {
        final List<DemuxedChunk> result = new ArrayList<>();
        if (tokenBuffer.length() > 0) {
            result.add(new DemuxedChunk(tokenBuffer.toString(), insideThinkingBlock));
            tokenBuffer.setLength(0);
        }
        return List.copyOf(result);
    }

    /**
     * Returns true if the test string ends with a prefix of the target tag.
     */
    private boolean isPrefixOf(final String text, final String tag) {
        for (int i = 1; i < tag.length(); i++) {
            if (text.endsWith(tag.substring(0, i))) {
                return true;
            }
        }
        return false;
    }
}
