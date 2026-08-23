package com.omniwrench.ai.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable metadata descriptor of a quantized language model.
 *
 * Traceability:
 * - Requirement: REQ-00091 (Multi-Source Model Repository Manager)
 * - Task: TSK-20260822-010 (Model Hub Repository Manager)
 * - ADR: ADR-0015 (Multi-Modal AI Adapter SPI), ADR-0050 (Model Repository Manager)
 *
 * @param id canonical unique identifier (e.g. "gemma2:2b", "Qwen/Qwen2.5-Coder-1.5B-GGUF")
 * @param name human-friendly display name
 * @param source repository origin source
 * @param format model weight format (e.g. "GGUF", "SAFETENSORS")
 * @param quantization quantization scheme (e.g. "Q4_K_M", "Q8_0", "F16")
 * @param parameterSize parameter scale (e.g. "2B", "7B", "1.5B")
 * @param fileSizeBytes size in bytes
 * @param downloadUrl remote download location
 * @param sha256 expected or verified SHA-256 hash checksum
 * @param localPath absolute path if locally installed, null otherwise
 * @param installed true if present on local disk and ready for inference
 */
public record ModelDescriptor(
        String id,
        String name,
        ModelSource source,
        String format,
        String quantization,
        String parameterSize,
        long fileSizeBytes,
        String downloadUrl,
        String sha256,
        Path localPath,
        boolean installed
) {
    /**
     * Compact constructor validating non-null requirements.
     */
    public ModelDescriptor {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(quantization, "quantization must not be null");
        Objects.requireNonNull(parameterSize, "parameterSize must not be null");
    }

    /**
     * Creates a copy of this descriptor with updated local installation details.
     *
     * @param targetPath the downloaded local file path
     * @param verifiedSha256 the verified SHA-256 hash
     * @return updated ModelDescriptor
     */
    public ModelDescriptor withLocalPath(final Path targetPath, final String verifiedSha256) {
        return new ModelDescriptor(
                this.id,
                this.name,
                this.source,
                this.format,
                this.quantization,
                this.parameterSize,
                this.fileSizeBytes,
                this.downloadUrl,
                verifiedSha256 != null ? verifiedSha256 : this.sha256,
                Objects.requireNonNull(targetPath, "targetPath must not be null"),
                true
        );
    }
}
