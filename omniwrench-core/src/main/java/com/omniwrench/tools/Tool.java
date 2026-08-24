package com.omniwrench.tools;

import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Fundamental interface for all executable agent capabilities and tooling plugins.
 *
 * <p>Supports synchronous execution and non-blocking asynchronous execution with reactive progress callbacks (ADR-0052).
 *
 * Traceability:
 * - Requirement: REQ-00060 (Polyvalent Base Architecture with Pluggable Tools), REQ-00094 (Async Background Tool Execution)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI)
 * - Use Case: UC-00009 (MCP External Server Tool Invocation)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry), TSK-20260822-011 (Advanced File Operations & Background Tool)
 * - ADR: ADR-0006 (Polyvalent Tool Architecture), ADR-0052 (Asynchronous Tool Execution)
 */
public interface Tool {

    /**
     * Returns the formal descriptor and JSON-schema definition of the tool.
     *
     * @return non-null tool definition
     */
    ToolDefinition getDefinition();

    /**
     * Executes the tool with the given arguments within a session context.
     *
     * @param context active session context, must not be null
     * @param arguments map of argument keys to parameter values, must not be null
     * @return tool invocation record detailing success/failure and execution output
     */
    ToolInvocation execute(SessionContext context, Map<String, Object> arguments);

    /**
     * Executes the tool asynchronously on a background thread.
     *
     * @param context active session context, must not be null
     * @param arguments map of argument keys to parameter values, must not be null
     * @return CompletableFuture resolving to the completed ToolInvocation
     */
    default CompletableFuture<ToolInvocation> executeAsync(final SessionContext context, final Map<String, Object> arguments) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");
        return CompletableFuture.supplyAsync(() -> execute(context, arguments));
    }

    /**
     * Executes the tool asynchronously with a reactive progress callback for long-running jobs.
     *
     * @param context active session context, must not be null
     * @param arguments map of argument keys to parameter values, must not be null
     * @param progressCallback consumer invoked as intermediate progress events occur
     * @return CompletableFuture resolving to the completed ToolInvocation
     */
    default CompletableFuture<ToolInvocation> executeAsync(final SessionContext context,
                                                           final Map<String, Object> arguments,
                                                           final Consumer<String> progressCallback) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");
        return CompletableFuture.supplyAsync(() -> {
            if (progressCallback != null) {
                progressCallback.accept("Starting asynchronous execution of tool: " + getDefinition().getName());
            }
            final ToolInvocation result = execute(context, arguments);
            if (progressCallback != null) {
                progressCallback.accept("Completed asynchronous execution of tool: " + getDefinition().getName());
            }
            return result;
        });
    }
}
