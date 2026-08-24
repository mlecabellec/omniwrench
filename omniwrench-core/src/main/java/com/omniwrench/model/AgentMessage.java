package com.omniwrench.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain carrier representing a message within the Omniwrench agent dialogue.
 *
 * Traceability:
 * - Requirement: REQ-00010 (Agent Message Contract & Memory Serialization), REQ-00088 (Dual Chat Mode Reasoning)
 * - Feature: FR-00004 (Multi-Module Clean Layering), FR-00011 (Multi-Modal Typed AI Abstraction)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-001 (Project Initialization), TSK-20260822-007 (Dual Chat Mode & Thinking Demux)
 */
public final class AgentMessage {

    /** Message unique identifier. */
    private final String id;
    /** Sender role. */
    private final String role;
    /** Text body. */
    private final String content;
    /** Internal reasoning thinking content (optional). */
    private final String thinking;
    /** Creation timestamp. */
    private final Instant timestamp;
    /** Tool execution records. */
    private final List<ToolInvocation> toolInvocations;

    /**
     * Constructs an immutable AgentMessage with explicit thinking content.
     *
     * @param idVal message unique identifier, must not be null
     * @param roleVal message sender role (e.g. user, assistant, system), must not be null
     * @param contentVal text body of the message, must not be null
     * @param thinkingVal internal reasoning thoughts, may be null
     * @param timestampVal creation timestamp, must not be null
     * @param toolInvocationsVal list of tool executions associated with this turn, may be null
     */
    public AgentMessage(final String idVal,
                        final String roleVal,
                        final String contentVal,
                        final String thinkingVal,
                        final Instant timestampVal,
                        final List<ToolInvocation> toolInvocationsVal) {
        this.id = Objects.requireNonNull(idVal, "id must not be null");
        this.role = Objects.requireNonNull(roleVal, "role must not be null");
        this.content = Objects.requireNonNull(contentVal, "content must not be null");
        this.thinking = thinkingVal != null ? thinkingVal : "";
        this.timestamp = Objects.requireNonNull(timestampVal, "timestamp must not be null");
        if (toolInvocationsVal == null) {
            this.toolInvocations = Collections.emptyList();
        } else {
            this.toolInvocations = List.copyOf(toolInvocationsVal);
        }
    }

    /**
     * Constructs an immutable AgentMessage without thinking content.
     *
     * @param idVal message unique identifier, must not be null
     * @param roleVal message sender role (e.g. user, assistant, system), must not be null
     * @param contentVal text body of the message, must not be null
     * @param timestampVal creation timestamp, must not be null
     * @param toolInvocationsVal list of tool executions associated with this turn, may be null
     */
    public AgentMessage(final String idVal,
                        final String roleVal,
                        final String contentVal,
                        final Instant timestampVal,
                        final List<ToolInvocation> toolInvocationsVal) {
        this(idVal, roleVal, contentVal, null, timestampVal, toolInvocationsVal);
    }

    /**
     * Factory method creating a message without tool invocations.
     *
     * @param role sender role
     * @param content text content
     * @return newly created AgentMessage
     */
    public static AgentMessage of(final String role, final String content) {
        return new AgentMessage(
                UUID.randomUUID().toString(),
                role,
                content,
                null,
                Instant.now(),
                Collections.emptyList()
        );
    }

    /**
     * Factory method creating a message with reasoning thoughts.
     *
     * @param role sender role
     * @param content text content
     * @param thinking internal thoughts
     * @return newly created AgentMessage
     */
    public static AgentMessage of(final String role, final String content, final String thinking) {
        return new AgentMessage(
                UUID.randomUUID().toString(),
                role,
                content,
                thinking,
                Instant.now(),
                Collections.emptyList()
        );
    }

    /**
     * Factory method creating a message with tool invocations.
     *
     * @param role sender role
     * @param content text content
     * @param tools list of tool invocations
     * @return newly created AgentMessage
     */
    public static AgentMessage of(final String role, final String content, final List<ToolInvocation> tools) {
        return new AgentMessage(
                UUID.randomUUID().toString(),
                role,
                content,
                null,
                Instant.now(),
                tools
        );
    }

    /**
     * Factory method creating a message with thoughts and tool invocations.
     *
     * @param role sender role
     * @param content text content
     * @param thinking internal thoughts
     * @param tools list of tool invocations
     * @return newly created AgentMessage
     */
    public static AgentMessage of(final String role,
                                  final String content,
                                  final String thinking,
                                  final List<ToolInvocation> tools) {
        return new AgentMessage(
                UUID.randomUUID().toString(),
                role,
                content,
                thinking,
                Instant.now(),
                tools
        );
    }

    /**
     * Returns message identifier.
     *
     * @return message ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns sender role.
     *
     * @return role
     */
    public String getRole() {
        return role;
    }

    /**
     * Returns text content.
     *
     * @return content
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns internal reasoning thinking text if available.
     *
     * @return thinking text or empty string
     */
    public String getThinking() {
        return thinking;
    }

    /**
     * Returns true if this message contains non-empty thinking content.
     *
     * @return true if thinking is present
     */
    public boolean hasThinking() {
        return !thinking.isBlank();
    }

    /**
     * Returns creation timestamp.
     *
     * @return timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns associated tool invocations.
     *
     * @return immutable list of tool invocations
     */
    public List<ToolInvocation> getToolInvocations() {
        return toolInvocations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AgentMessage)) {
            return false;
        }
        final AgentMessage that = (AgentMessage) o;
        return Objects.equals(id, that.id)
                && Objects.equals(role, that.role)
                && Objects.equals(content, that.content)
                && Objects.equals(thinking, that.thinking)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(toolInvocations, that.toolInvocations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role, content, thinking, timestamp, toolInvocations);
    }

    @Override
    public String toString() {
        return "AgentMessage{"
                + "id='" + id + '\''
                + ", role='" + role + '\''
                + ", content='" + content + '\''
                + ", hasThinking=" + hasThinking()
                + ", timestamp=" + timestamp
                + ", toolInvocations=" + toolInvocations.size()
                + '}';
    }
}
