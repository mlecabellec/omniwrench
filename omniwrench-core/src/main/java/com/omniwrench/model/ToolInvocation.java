package com.omniwrench.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Domain record representing an executed tool call and its observed result.
 *
 * Traceability:
 * - Requirement: REQ-00012 (Tool Execution Lifecycle and Results)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00002 (Autonomous Goal Planning)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
public final class ToolInvocation {

    /** Unique invocation call identifier. */
    private final String callId;
    /** Tool name that was executed. */
    private final String toolName;
    /** Arguments map passed to the tool. */
    private final Map<String, Object> arguments;
    /** Standard execution output or error message. */
    private final String output;
    /** Flag indicating whether the tool succeeded. */
    private final boolean success;
    /** Execution timestamp. */
    private final Instant executedAt;

    /**
     * Constructs a ToolInvocation record capturing execution parameters and output.
     *
     * @param callIdVal unique call identifier, must not be null
     * @param toolNameVal name of the tool executed, must not be null
     * @param argumentsVal input argument map, may be null
     * @param outputVal execution standard output or error message, must not be null
     * @param successVal true if tool execution exited normally
     * @param executedAtVal execution timestamp, must not be null
     */
    public ToolInvocation(final String callIdVal,
                          final String toolNameVal,
                          final Map<String, Object> argumentsVal,
                          final String outputVal,
                          final boolean successVal,
                          final Instant executedAtVal) {
        this.callId = Objects.requireNonNull(callIdVal, "callId must not be null");
        this.toolName = Objects.requireNonNull(toolNameVal, "toolName must not be null");
        if (argumentsVal == null) {
            this.arguments = Collections.emptyMap();
        } else {
            this.arguments = Map.copyOf(argumentsVal);
        }
        this.output = Objects.requireNonNull(outputVal, "output must not be null");
        this.success = successVal;
        this.executedAt = Objects.requireNonNull(executedAtVal, "executedAt must not be null");
    }

    /**
     * Returns invocation call ID.
     *
     * @return call ID
     */
    public String getCallId() {
        return callId;
    }

    /**
     * Returns tool name.
     *
     * @return tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Returns arguments map.
     *
     * @return unmodifiable arguments map
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Returns output text.
     *
     * @return execution output
     */
    public String getOutput() {
        return output;
    }

    /**
     * Returns execution status flag.
     *
     * @return true if execution succeeded
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns execution timestamp.
     *
     * @return timestamp
     */
    public Instant getExecutedAt() {
        return executedAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ToolInvocation)) {
            return false;
        }
        final ToolInvocation that = (ToolInvocation) o;
        return success == that.success
                && Objects.equals(callId, that.callId)
                && Objects.equals(toolName, that.toolName)
                && Objects.equals(arguments, that.arguments)
                && Objects.equals(output, that.output)
                && Objects.equals(executedAt, that.executedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callId, toolName, arguments, output, success, executedAt);
    }

    @Override
    public String toString() {
        return "ToolInvocation{"
                + "callId='" + callId + '\''
                + ", toolName='" + toolName + '\''
                + ", success=" + success
                + ", executedAt=" + executedAt
                + '}';
    }
}
