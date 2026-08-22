package com.omniwrench.ai;

/**
 * Unchecked exception thrown by a {@link BackendAdapter} when the backend
 * returns an error, times out, or cannot be reached.
 *
 * <p>See ADR-0015: Custom Future-Proof Multi-Modal AI Adapter SPI.
 */
public final class BackendException extends RuntimeException {

    /** Serial version UID for serialization compatibility. */
    private static final long serialVersionUID = 1L;

    /** The backend identifier that raised this exception. */
    private final String backendId;

    /**
     * Constructs a backend exception with backend identifier and message.
     *
     * @param backendId the backend that failed, must not be null
     * @param message the error message
     */
    public BackendException(final String backendId, final String message) {
        super("[" + backendId + "] " + message);
        this.backendId = backendId;
    }

    /**
     * Constructs a backend exception with backend identifier, message, and cause.
     *
     * @param backendId the backend that failed, must not be null
     * @param message the error message
     * @param cause the root cause
     */
    public BackendException(final String backendId, final String message, final Throwable cause) {
        super("[" + backendId + "] " + message, cause);
        this.backendId = backendId;
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
