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
 * - Requirement: REQ-00013 (Session Isolation & Workspace Scope)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
@Service
public class SessionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionManager.class);

    private final OmniwrenchProperties properties;
    private final Map<String, SessionContext> activeSessions = new ConcurrentHashMap<>();
    private final SessionContext defaultSession;

    public SessionManager(final OmniwrenchProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.defaultSession = SessionContext.createDefault(properties.getWorkspacePath());
        this.activeSessions.put(defaultSession.getSessionId(), defaultSession);
        LOGGER.info("Initialized SessionManager with primary default session: {}", defaultSession.getSessionId());
    }

    public SessionContext getDefaultSession() {
        return defaultSession;
    }

    public SessionContext createSession(final String workspacePath) {
        final String targetPath = workspacePath != null && !workspacePath.isBlank()
                ? workspacePath
                : properties.getWorkspacePath();
        final SessionContext session = SessionContext.createDefault(targetPath);
        activeSessions.put(session.getSessionId(), session);
        LOGGER.info("Created new session: {} for workspace: {}", session.getSessionId(), targetPath);
        return session;
    }

    public Optional<SessionContext> getSession(final String sessionId) {
        final String nonNullId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        return Optional.ofNullable(activeSessions.get(nonNullId));
    }

    public Map<String, SessionContext> getActiveSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }
}
