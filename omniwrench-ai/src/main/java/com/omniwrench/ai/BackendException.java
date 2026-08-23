package com.omniwrench.ai;

import java.util.Objects;

/**
 * Unchecked exception thrown by a {@link BackendAdapter} when the backend
 * returns an error, times out, or cannot be reached.
 *
 * Traceability:
 * - Requirement: REQ-00040 (Custom Multi-Modal AI Adapter SPI)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming)
 * - ADR: ADR-0015 (Future-Proof Multi-Modal SPI)
 */
public final class BackendException extends RuntimeException {

    /** Serial version UID for serialization compatibility. */
    private static final long serialVersionUID = 1L;

    /** The backend identifier that raised this exception. */
    private final String backendId;

    /**
     * Constructs a backend exception with backend identifier and message.
     *
     * @param backendIdVal the backend that failed, must not be null
     * @param message the error message
     */
    public BackendException(final String backendIdVal, final String message) {
        super("[" + Objects.requireNonNull(backendIdVal, "backendId must not be null") + "] " + message);
        this.backendId = backendIdVal;
    }

    /**
     * Constructs a backend exception with backend identifier, message, and cause.
     *
     * @param backendIdVal the backend that failed, must not be null
     * @param message the error message
     * @param cause the root cause
     */
    public BackendException(final String backendIdVal, final String message, final Throwable cause) {
        super("[" + Objects.requireNonNull(backendIdVal, "backendId must not be null") + "] " + message, cause);
        this.backendId = backendIdVal;
    }

    /**
     * Returns the backend identifier that raised this exception.
     *
     * @return the backend identifier, never null
     */
    public String getBackendId() {
        return backendId;
    }
}
