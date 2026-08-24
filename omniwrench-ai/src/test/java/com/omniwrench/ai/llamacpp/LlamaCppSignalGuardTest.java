package com.omniwrench.ai.llamacpp;

import com.omniwrench.ai.BackendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for LlamaCppSignalGuard error containment and fault interception.
 *
 * Traceability:
 * - Requirement: REQ-00090 (Embedded llama.cpp Local LLM Backend Plugin), REQ-00093 (Multi-Architecture Embedded llama.cpp Runtime)
 * - Task: TSK-20260822-015 (In-Process Embedded llama.cpp Engine)
 * - ADR: ADR-0049 (llama.cpp Embedded Inference Engine)
 */
@Tag("REQ-00090")
@Tag("REQ-00093")
@Tag("TSK-20260822-015")
class LlamaCppSignalGuardTest {

    @Test
    @DisplayName("LlamaCppSignalGuard should successfully return result for valid callable action")
    void testSuccessfulAction() {
        final String result = LlamaCppSignalGuard.runGuarded(() -> "success_value");
        assertThat(result).isEqualTo("success_value");
    }

    @Test
    @DisplayName("LlamaCppSignalGuard should propagate BackendException unmodified")
    void testBackendExceptionPropagation() {
        assertThatThrownBy(() -> LlamaCppSignalGuard.runGuarded(() -> {
            throw new BackendException("Explicit backend failure", "llamacpp");
        })).isInstanceOf(BackendException.class)
                .hasMessageContaining("Explicit backend failure");
    }

    @Test
    @DisplayName("LlamaCppSignalGuard should trap NullPointerException and wrap into BackendException")
    void testNullPointerTrapping() {
        assertThatThrownBy(() -> LlamaCppSignalGuard.runGuarded(() -> {
            final String nullStr = null;
            return nullStr.length();
        })).isInstanceOf(BackendException.class)
                .hasMessageContaining("Null pointer dereference");
    }

    @Test
    @DisplayName("LlamaCppSignalGuard should trap OutOfMemoryError and wrap into BackendException")
    void testOutOfMemoryTrapping() {
        assertThatThrownBy(() -> LlamaCppSignalGuard.runGuarded(() -> {
            throw new OutOfMemoryError("Native VRAM allocation failure");
        })).isInstanceOf(BackendException.class)
                .hasMessageContaining("Native out-of-memory");
    }
}
