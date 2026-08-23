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
 * - Requirement: REQ-00013 (Session Management & Multi-Tenant State Isolation)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
public final class SessionContext {

    /** Unique session identifier. */
    private final String sessionId;
    /** Workspace root filesystem path. */
    private final String workspaceRoot;
    /** List of conversation turns. */
    private final List<AgentMessage> messages;
    /** Session creation timestamp. */
    private final Instant createdAt;

    /**
     * Constructs a SessionContext with session identifier and workspace directory.
     *
     * @param sessionIdVal unique session identifier, must not be null
     * @param workspaceRootVal absolute or relative path to the workspace root, must not be null
     */
    public SessionContext(final String sessionIdVal, final String workspaceRootVal) {
        this.sessionId = Objects.requireNonNull(sessionIdVal, "sessionId must not be null");
        this.workspaceRoot = Objects.requireNonNull(workspaceRootVal, "workspaceRoot must not be null");
        this.messages = new ArrayList<>();
        this.createdAt = Instant.now();
    }

    /**
     * Factory method creating a default SessionContext with random UUID.
     *
     * @param workspaceRoot workspace root path
     * @return newly created SessionContext
     */
    public static SessionContext createDefault(final String workspaceRoot) {
        return new SessionContext(UUID.randomUUID().toString(), workspaceRoot);
    }

    /**
     * Appends an agent dialogue message to this session history in a thread-safe manner.
     *
     * @param message the message to append, must not be null
     */
    public synchronized void addMessage(final AgentMessage message) {
        this.messages.add(Objects.requireNonNull(message, "message must not be null"));
    }

    /**
     * Returns an unmodifiable snapshot of messages recorded in this session.
     *
     * @return list of agent messages
     */
    public synchronized List<AgentMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    /**
     * Returns the unique session ID.
     *
     * @return session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the workspace root path for this session.
     *
     * @return workspace root path
     */
    public String getWorkspaceRoot() {
        return workspaceRoot;
    }

    /**
     * Returns session creation timestamp.
     *
     * @return creation instant
     */
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
