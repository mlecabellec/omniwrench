package com.omniwrench.core;

import com.omniwrench.model.ToolDefinition;
import com.omniwrench.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service managing registration, discovery, and lookup of all available Omniwrench tools.
 *
 * Traceability:
 * - Requirement: REQ-00060 (Polyvalent Base Architecture with Pluggable Tools)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI)
 * - Use Case: UC-00009 (MCP External Server Tool Invocation)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 * - ADR: ADR-0006 (Polyvalent Tool Architecture)
 */
@Service
public class ToolRegistry {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolRegistry.class);

    /** Map of registered tools indexed by unique name. */
    private final Map<String, Tool> registeredTools = new ConcurrentHashMap<>();

    /**
     * Constructs a ToolRegistry and registers the provided list of initial tools.
     *
     * @param toolList the list of tools to register, must not be null
     */
    public ToolRegistry(final List<Tool> toolList) {
        final List<Tool> nonNullTools = Objects.requireNonNull(toolList, "toolList must not be null");
        for (final Tool tool : nonNullTools) {
            registerTool(tool);
        }
    }

    /**
     * Registers a new tool instance in the registry.
     *
     * @param tool the tool to register, must not be null
     */
    public void registerTool(final Tool tool) {
        final Tool nonNullTool = Objects.requireNonNull(tool, "tool must not be null");
        final String name = nonNullTool.getDefinition().getName();
        registeredTools.put(name, nonNullTool);
        LOGGER.info("Registered agent tool: '{}'", name);
    }

    /**
     * Looks up a registered tool by its unique name.
     *
     * @param name tool identifier name, must not be null
     * @return Optional containing the tool if found
     */
    public Optional<Tool> getTool(final String name) {
        final String nonNullName = Objects.requireNonNull(name, "tool name must not be null");
        return Optional.ofNullable(registeredTools.get(nonNullName));
    }

    /**
     * Returns an unmodifiable list of all registered tool definitions.
     *
     * @return list of tool definitions
     */
    public List<ToolDefinition> getAllDefinitions() {
        return registeredTools.values().stream()
                .map(Tool::getDefinition)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    /**
     * Returns the total count of registered tools.
     *
     * @return registered tool count
     */
    public int getToolCount() {
        return registeredTools.size();
    }
}
