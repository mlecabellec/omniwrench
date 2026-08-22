package com.omniwrench.ai;

/**
 * Execution mode for AI model requests.
 *
 * <p>Controls how the model response is delivered and scheduled.
 * The {@code ModelRouter} selects the appropriate backend adapter
 * based on the requested execution mode and provider capability matrix.
 *
 * <p>See ADR-0015: Custom Future-Proof Multi-Modal AI Adapter SPI.
 */
public enum ExecutionMode {

    /**
     * Blocking synchronous call: waits for the full response before returning.
     * Suitable for short completions and batch API calls.
     */
    SYNCHRONOUS,

    /**
     * Server-Sent Events streaming: tokens are pushed to a callback as they are generated.
     * Suitable for interactive TUI and web chat interfaces.
     */
    STREAMING_SSE,

    /**
     * Asynchronous future: returns a {@code CompletableFuture} immediately.
     * Suitable for background tasks where the caller polls for completion.
     */
    ASYNCHRONOUS_FUTURE,

    /**
     * Fire-and-forget background task: dispatched to the bounded thread pool.
     * Result is written to the session context and local file store when complete.
     */
    BACKGROUND_TASK,

    /**
     * Scheduled/planned task: deferred to a future time or event trigger.
     * Lifecycle managed by the {@code TaskScheduler}.
     */
    PLANNED_SCHEDULED,

    /**
     * Tool-enabled mode: the model may call tools via the Tool SPI during generation.
     * The engine handles tool call/response cycles automatically.
     */
    TOOL_ENABLED
}
