package com.omniwrench.core;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying SessionManager lifecycle, session isolation, and memory tracking.
 *
 * Traceability:
 * - Requirement: REQ-00013 (Session Management & Multi-Tenant State Isolation)
 * - Feature: FR-00001 (Dual Headless & Interactive Presentation Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 * - ADR: ADR-0001 (Unified Dual Architecture)
 */
@Tag("REQ-00013")
@Tag("FR-00001")
@Tag("UC-00001")
@Tag("TSK-20260822-001")
class SessionManagerTest {

    private OmniwrenchProperties properties;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        properties = new OmniwrenchProperties();
        properties.setWorkspacePath("/tmp/test-workspace");
        sessionManager = new SessionManager(properties);
    }

    @Test
    @DisplayName("Should initialize with a valid default session")
    void shouldInitializeWithDefaultSession() {
        final SessionContext defaultSession = sessionManager.getDefaultSession();
        assertThat(defaultSession).isNotNull();
        assertThat(defaultSession.getSessionId()).isNotBlank();
        assertThat(defaultSession.getWorkspaceRoot()).isEqualTo("/tmp/test-workspace");

        final Optional<SessionContext> session = sessionManager.getSession(defaultSession.getSessionId());
        assertThat(session).isPresent();
        assertThat(session.get()).isEqualTo(defaultSession);
    }

    @Test
    @DisplayName("Should create and retrieve dynamic isolated sessions")
    void shouldCreateAndRetrieveSession() {
        final SessionContext created = sessionManager.createSession("/tmp/custom-workspace");
        assertThat(created).isNotNull();
        assertThat(created.getSessionId()).isNotBlank();

        final Optional<SessionContext> retrieved = sessionManager.getSession(created.getSessionId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getWorkspaceRoot()).isEqualTo("/tmp/custom-workspace");

        final Map<String, SessionContext> activeSessions = sessionManager.getActiveSessions();
        assertThat(activeSessions).hasSize(2);
        assertThat(activeSessions).containsKey(created.getSessionId());
    }

    @Test
    @DisplayName("Should fallback to default workspace path when null or blank provided")
    void shouldFallbackToDefaultWorkspacePath() {
        final SessionContext s1 = sessionManager.createSession(null);
        assertThat(s1.getWorkspaceRoot()).isEqualTo("/tmp/test-workspace");

        final SessionContext s2 = sessionManager.createSession("   ");
        assertThat(s2.getWorkspaceRoot()).isEqualTo("/tmp/test-workspace");
    }

    @Test
    @DisplayName("Should reject null sessionId and return empty for non-existent session")
    void shouldHandleBoundaryAndNullConditions() {
        assertThat(sessionManager.getSession("non-existent-id")).isEmpty();
        assertThrows(NullPointerException.class, () -> sessionManager.getSession(null));
        assertThrows(NullPointerException.class, () -> new SessionManager(null));
    }
}
