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
 * - Requirement: REQ-00020 (Pluggable Tool SPI & Registry)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 */
@Service
public class ToolRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, Tool> registeredTools = new ConcurrentHashMap<>();

    public ToolRegistry(final List<Tool> toolList) {
        final List<Tool> nonNullTools = Objects.requireNonNull(toolList, "toolList must not be null");
        for (final Tool tool : nonNullTools) {
            registerTool(tool);
        }
    }

    public void registerTool(final Tool tool) {
        final Tool nonNullTool = Objects.requireNonNull(tool, "tool must not be null");
        final String name = nonNullTool.getDefinition().getName();
        registeredTools.put(name, nonNullTool);
        LOGGER.info("Registered agent tool: '{}'", name);
    }

    public Optional<Tool> getTool(final String name) {
        final String nonNullName = Objects.requireNonNull(name, "tool name must not be null");
        return Optional.ofNullable(registeredTools.get(nonNullName));
    }

    public List<ToolDefinition> getAllDefinitions() {
        return registeredTools.values().stream()
                .map(Tool::getDefinition)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public int getToolCount() {
        return registeredTools.size();
    }
}
