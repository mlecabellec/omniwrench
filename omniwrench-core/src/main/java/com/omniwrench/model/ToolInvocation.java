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
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
public final class ToolInvocation {

    private final String callId;
    private final String toolName;
    private final Map<String, Object> arguments;
    private final String output;
    private final boolean success;
    private final Instant executedAt;

    public ToolInvocation(final String callId,
                          final String toolName,
                          final Map<String, Object> arguments,
                          final String output,
                          final boolean success,
                          final Instant executedAt) {
        this.callId = Objects.requireNonNull(callId, "callId must not be null");
        this.toolName = Objects.requireNonNull(toolName, "toolName must not be null");
        if (arguments == null) {
            this.arguments = Collections.emptyMap();
        } else {
            this.arguments = Map.copyOf(arguments);
        }
        this.output = Objects.requireNonNull(output, "output must not be null");
        this.success = success;
        this.executedAt = Objects.requireNonNull(executedAt, "executedAt must not be null");
    }

    public String getCallId() {
        return callId;
    }

    public String getToolName() {
        return toolName;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public String getOutput() {
        return output;
    }

    public boolean isSuccess() {
        return success;
    }

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
