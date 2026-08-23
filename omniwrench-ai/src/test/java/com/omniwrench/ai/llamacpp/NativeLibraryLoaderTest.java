package com.omniwrench.ai.llamacpp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for NativeLibraryLoader platform detection and multi-arch discovery.
 *
 * Traceability:
 * - Requirement: REQ-00090 (Embedded llama.cpp Local LLM Backend Plugin), REQ-00093 (Multi-Architecture Embedded llama.cpp Runtime)
 * - Task: TSK-20260822-015 (Multi-Architecture Embedded llama.cpp Engine)
 * - ADR: ADR-0049 (llama.cpp Embedded Inference Engine)
 */
@Tag("REQ-00090")
@Tag("REQ-00093")
@Tag("TSK-20260822-015")
class NativeLibraryLoaderTest {

    @Test
    @DisplayName("NativeLibraryLoader should detect host OS and Architecture accurately")
    void testPlatformDetection() {
        final NativeLibraryLoader.OperatingSystem os = NativeLibraryLoader.detectOperatingSystem();
        final NativeLibraryLoader.Architecture arch = NativeLibraryLoader.detectArchitecture();
        final String platformId = NativeLibraryLoader.getPlatformIdentifier();

        assertThat(os).isNotEqualTo(NativeLibraryLoader.OperatingSystem.UNKNOWN);
        assertThat(arch).isNotEqualTo(NativeLibraryLoader.Architecture.UNKNOWN);
        assertThat(platformId).isNotEmpty();
        assertThat(platformId).contains("-");
    }

    @Test
    @DisplayName("NativeLibraryLoader should safely return empty Optional when library is not bundled")
    void testExtractNonExistentLibrary() {
        final Optional<Path> extracted = NativeLibraryLoader.extractBundledLibrary("non_existent_lib");
        assertThat(extracted).isEmpty();
    }

    @Test
    @DisplayName("GpuBackend enum should contain all acceleration targets")
    void testGpuBackendEnum() {
        assertThat(NativeLibraryLoader.GpuBackend.values()).contains(
                NativeLibraryLoader.GpuBackend.CPU,
                NativeLibraryLoader.GpuBackend.CUDA,
                NativeLibraryLoader.GpuBackend.ROCM,
                NativeLibraryLoader.GpuBackend.VULKAN,
                NativeLibraryLoader.GpuBackend.METAL
        );
    }
}
