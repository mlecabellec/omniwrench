package com.omniwrench.tools;

import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;

import java.util.Map;

/**
 * Fundamental interface for all executable agent capabilities and tooling plugins.
 * 
 * Traceability:
 * - Requirement: REQ-00020 (Pluggable Tool SPI)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
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
     * @param context active session context
     * @param arguments map of argument keys to parameter values
     * @return tool invocation record detailing success/failure and execution output
     */
    ToolInvocation execute(SessionContext context, Map<String, Object> arguments);
}
