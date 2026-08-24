package com.omniwrench.core;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.model.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active sessions and contextual memory for Omniwrench agent conversations.
 *
 * Traceability:
 * - Requirement: REQ-00013 (Session Management & Multi-Tenant State Isolation)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
@Service
public class SessionManager {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionManager.class);

    /** Configuration properties instance. */
    private final OmniwrenchProperties properties;
    /** Map of active sessions indexed by session ID. */
    private final Map<String, SessionContext> activeSessions = new ConcurrentHashMap<>();
    /** Default primary session context. */
    private final SessionContext defaultSession;

    /**
     * Constructs a SessionManager with default runtime properties.
     *
     * @param propertiesVal configuration properties, must not be null
     */
    public SessionManager(final OmniwrenchProperties propertiesVal) {
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");
        this.defaultSession = SessionContext.createDefault(properties.getWorkspacePath());
        this.activeSessions.put(defaultSession.getSessionId(), defaultSession);
        LOGGER.info("Initialized SessionManager with primary default session: {}", defaultSession.getSessionId());
    }

    /**
     * Returns the primary default session.
     *
     * @return the default SessionContext
     */
    public SessionContext getDefaultSession() {
        return defaultSession;
    }

    /**
     * Creates and activates a new isolated session for the specified workspace path.
     *
     * @param workspacePath target workspace directory path, fallback to default if blank
     * @return the newly created SessionContext
     */
    public SessionContext createSession(final String workspacePath) {
        final String targetPath = workspacePath != null && !workspacePath.isBlank()
                ? workspacePath
                : properties.getWorkspacePath();
        final SessionContext session = SessionContext.createDefault(targetPath);
        activeSessions.put(session.getSessionId(), session);
        LOGGER.info("Created new session: {} for workspace: {}", session.getSessionId(), targetPath);
        return session;
    }

    /**
     * Retrieves an active session by its unique ID.
     *
     * @param sessionId the session identifier, must not be null
     * @return Optional containing the session if present
     */
    public Optional<SessionContext> getSession(final String sessionId) {
        final String nonNullId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        return Optional.ofNullable(activeSessions.get(nonNullId));
    }

    /**
     * Returns an unmodifiable map of all active sessions.
     *
     * @return unmodifiable map of active sessions
     */
    public Map<String, SessionContext> getActiveSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }

    /**
     * Returns count of active sessions.
     *
     * @return active session count
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
