package com.omniwrench.ai.llamacpp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;

/**
 * Cross-platform detection and loader for embedded native llama.cpp / ggml binaries and shared libraries.
 *
 * Traceability:
 * - Requirement: REQ-00090 (Embedded llama.cpp Local LLM Backend Plugin), REQ-00093 (Multi-Architecture Embedded llama.cpp Runtime)
 * - Feature: FR-00011 (Multi-Modal Typed AI Abstraction), FR-00012 (Universal Pluggable AI Adapters)
 * - Task: TSK-20260822-015 (Multi-Architecture Embedded llama.cpp Engine)
 * - ADR: ADR-0049 (llama.cpp Embedded Inference Engine)
 */
public final class NativeLibraryLoader {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeLibraryLoader.class);

    /** Supported target operating systems. */
    public enum OperatingSystem {
        /** Linux OS. */
        LINUX,
        /** macOS / Darwin. */
        MACOS,
        /** Microsoft Windows. */
        WINDOWS,
        /** Unsupported / other OS. */
        UNKNOWN
    }

    /** Supported processor architectures. */
    public enum Architecture {
        /** x86_64 / AMD64. */
        X86_64,
        /** ARM64 / AArch64. */
        AARCH64,
        /** Unsupported architecture. */
        UNKNOWN
    }

    /** Supported GPU compute backends. */
    public enum GpuBackend {
        /** Pure CPU execution (SIMD AVX2 / Neon). */
        CPU,
        /** NVIDIA CUDA acceleration. */
        CUDA,
        /** AMD ROCm / HIP acceleration. */
        ROCM,
        /** Cross-platform Vulkan compute. */
        VULKAN,
        /** Apple Metal compute. */
        METAL
    }

    /** Private constructor for utility class. */
    private NativeLibraryLoader() {
    }

    /**
     * Detects the operating system of the host runtime.
     *
     * @return detected OperatingSystem
     */
    public static OperatingSystem detectOperatingSystem() {
        final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("linux")) {
            return OperatingSystem.LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return OperatingSystem.MACOS;
        } else if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        }
        return OperatingSystem.UNKNOWN;
    }

    /**
     * Detects the processor architecture of the host runtime.
     *
     * @return detected Architecture
     */
    public static Architecture detectArchitecture() {
        final String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            return Architecture.X86_64;
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            return Architecture.AARCH64;
        }
        return Architecture.UNKNOWN;
    }

    /**
     * Computes the canonical platform identifier string (e.g. "linux-x86_64", "macos-aarch64").
     *
     * @return platform identifier string
     */
    public static String getPlatformIdentifier() {
        final OperatingSystem os = detectOperatingSystem();
        final Architecture arch = detectArchitecture();
        return os.name().toLowerCase(Locale.ROOT) + "-" + arch.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Extracts an embedded native library from classpath resources to a temporary cached path.
     *
     * @param libraryBaseName base name (e.g. "llama", "ggml")
     * @return Optional containing absolute Path to the extracted shared library, or empty if not bundled
     */
    public static Optional<Path> extractBundledLibrary(final String libraryBaseName) {
        final String platform = getPlatformIdentifier();
        final OperatingSystem os = detectOperatingSystem();
        final String extension = switch (os) {
            case LINUX -> ".so";
            case MACOS -> ".dylib";
            case WINDOWS -> ".dll";
            case UNKNOWN -> ".so";
        };

        final String resourcePath = "/native/" + platform + "/" + libraryBaseName + extension;
        final InputStream is = NativeLibraryLoader.class.getResourceAsStream(resourcePath);
        if (is == null) {
            LOGGER.debug("Bundled native library not found on classpath: {}", resourcePath);
            return Optional.empty();
        }

        try {
            final Path tempDir = Files.createTempDirectory("omniwrench-native-");
            final Path targetFile = tempDir.resolve(libraryBaseName + extension);
            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
            targetFile.toFile().deleteOnExit();
            LOGGER.info("Extracted bundled native library '{}' to '{}'", resourcePath, targetFile);
            return Optional.of(targetFile);
        } catch (final IOException e) {
            LOGGER.warn("Failed to extract bundled native library '{}': {}", resourcePath, e.getMessage());
            return Optional.empty();
        }
    }
}
