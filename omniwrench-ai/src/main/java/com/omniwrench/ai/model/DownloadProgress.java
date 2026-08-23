package com.omniwrench.ai.model;

/**
 * Download progress snapshot during model acquisition.
 *
 * Traceability:
 * - Requirement: REQ-00091 (Multi-Source Model Repository Manager)
 * - Task: TSK-20260822-010 (Model Hub Repository Manager)
 * - ADR: ADR-0050 (Model Repository Manager)
 *
 * @param modelId unique identifier of the model being downloaded
 * @param bytesDownloaded number of bytes received so far
 * @param totalBytes total expected byte size (or -1 if unknown)
 * @param percentage percent complete (0.0 to 100.0)
 * @param speedBytesPerSec current transfer rate in bytes per second
 * @param status current transfer status
 */
public record DownloadProgress(
        String modelId,
        long bytesDownloaded,
        long totalBytes,
        double percentage,
        long speedBytesPerSec,
        Status status
) {
    /**
     * Download execution lifecycle status.
     */
    public enum Status {
        /** Connecting to repository and inspecting metadata. */
        CONNECTING,
        /** Actively streaming bytes to temporary local file. */
        DOWNLOADING,
        /** Verifying SHA-256 cryptographic checksum. */
        VERIFYING,
        /** Successfully verified and stored in local model repository. */
        COMPLETED,
        /** Download or validation failed. */
        FAILED
    }
}
