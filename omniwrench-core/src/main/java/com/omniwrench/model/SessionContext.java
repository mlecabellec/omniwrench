package com.omniwrench.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Encapsulates the conversation history, workspace directory, and state for an active Omniwrench session.
 * 
 * Traceability:
 * - Requirement: REQ-00013 (Session Isolation & Workspace Scope)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
public final class SessionContext {

    private final String sessionId;
    private final String workspaceRoot;
    private final List<AgentMessage> messages;
    private final Instant createdAt;

    public SessionContext(final String sessionId, final String workspaceRoot) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        this.workspaceRoot = Objects.requireNonNull(workspaceRoot, "workspaceRoot must not be null");
        this.messages = new ArrayList<>();
        this.createdAt = Instant.now();
    }

    public static SessionContext createDefault(final String workspaceRoot) {
        return new SessionContext(UUID.randomUUID().toString(), workspaceRoot);
    }

    public synchronized void addMessage(final AgentMessage message) {
        this.messages.add(Objects.requireNonNull(message, "message must not be null"));
    }

    public synchronized List<AgentMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getWorkspaceRoot() {
        return workspaceRoot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SessionContext)) {
            return false;
        }
        final SessionContext that = (SessionContext) o;
        return Objects.equals(sessionId, that.sessionId)
                && Objects.equals(workspaceRoot, that.workspaceRoot)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, workspaceRoot, createdAt);
    }

    @Override
    public String toString() {
        return "SessionContext{"
                + "sessionId='" + sessionId + '\''
                + ", workspaceRoot='" + workspaceRoot + '\''
                + ", messageCount=" + messages.size()
                + ", createdAt=" + createdAt
                + '}';
    }
}
