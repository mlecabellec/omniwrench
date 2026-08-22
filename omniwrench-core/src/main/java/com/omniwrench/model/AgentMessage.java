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
 * - Requirement: REQ-00010 (Agent Message Contract & Memory Serialization)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
public final class AgentMessage {

    private final String id;
    private final String role;
    private final String content;
    private final Instant timestamp;
    private final List<ToolInvocation> toolInvocations;

    public AgentMessage(final String id,
                        final String role,
                        final String content,
                        final Instant timestamp,
                        final List<ToolInvocation> toolInvocations) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        if (toolInvocations == null) {
            this.toolInvocations = Collections.emptyList();
        } else {
            this.toolInvocations = List.copyOf(toolInvocations);
        }
    }

    public static AgentMessage of(final String role, final String content) {
        return new AgentMessage(
                UUID.randomUUID().toString(),
                role,
                content,
                Instant.now(),
                Collections.emptyList()
        );
    }

    public static AgentMessage of(final String role, final String content, final List<ToolInvocation> tools) {
        return new AgentMessage(
                UUID.randomUUID().toString(),
                role,
                content,
                Instant.now(),
                tools
        );
    }

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

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
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(toolInvocations, that.toolInvocations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role, content, timestamp, toolInvocations);
    }

    @Override
    public String toString() {
        return "AgentMessage{"
                + "id='" + id + '\''
                + ", role='" + role + '\''
                + ", content='" + content + '\''
                + ", timestamp=" + timestamp
                + ", toolInvocations=" + toolInvocations.size()
                + '}';
    }
}
