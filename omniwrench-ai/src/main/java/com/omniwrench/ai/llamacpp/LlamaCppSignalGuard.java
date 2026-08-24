package com.omniwrench.ai.llamacpp;

import com.omniwrench.ai.BackendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Signal and fault guard protecting the JVM from native memory corruptions, segfaults, and unhandled errors.
 *
 * <p>Wraps in-process native llama.cpp execution inside defensive boundaries to convert native
 * faults, segmentation violations, and out-of-memory errors into structured {@link BackendException}s.
 *
 * Traceability:
 * - Requirement: REQ-00090 (Embedded llama.cpp Local LLM Backend Plugin), REQ-00093 (Multi-Architecture Embedded llama.cpp Runtime)
 * - Task: TSK-20260822-015 (In-Process Embedded llama.cpp Engine)
 * - ADR: ADR-0049 (llama.cpp Embedded Inference Engine)
 */
public final class LlamaCppSignalGuard {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LlamaCppSignalGuard.class);

    /** Backend identifier for exception reporting. */
    private static final String BACKEND_ID = "llamacpp";

    /** Private constructor for utility class. */
    private LlamaCppSignalGuard() {
    }

    /**
     * Executes a native operation within a guarded boundary, catching and translating native faults.
     *
     * @param <V> return value type
     * @param action the callable action to execute, must not be null
     * @return the result of the guarded action
     * @throws BackendException if the action throws a native error, runtime exception, or fault
     */
    public static <V> V runGuarded(final Callable<V> action) {
        Objects.requireNonNull(action, "action must not be null");
        try {
            return action.call();
        } catch (final BackendException be) {
            throw be;
        } catch (final OutOfMemoryError oom) {
            LOGGER.error("Native out-of-memory error during llama.cpp execution", oom);
            throw new BackendException("Native out-of-memory: context size or model weights exceed RAM/VRAM", BACKEND_ID);
        } catch (final NullPointerException npe) {
            LOGGER.error("Null pointer dereference intercepted in native llama.cpp boundary", npe);
            throw new BackendException("Null pointer dereference in native llama.cpp boundary: " + npe.getMessage(), BACKEND_ID);
        } catch (final Exception e) {
            LOGGER.error("Guarded llama.cpp execution failed: {}", e.getMessage(), e);
            throw new BackendException("Native llama.cpp execution error: " + e.getMessage(), BACKEND_ID);
        } catch (final Throwable t) {
            LOGGER.error("Fatal native error trapped by LlamaCppSignalGuard: {}", t.getMessage(), t);
            throw new BackendException("Fatal native fault trapped in llama.cpp: " + t.getMessage(), BACKEND_ID);
        }
    }
}
