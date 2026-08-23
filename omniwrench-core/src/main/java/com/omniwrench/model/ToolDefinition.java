package com.omniwrench.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata definition and schema contract for an Omniwrench executable tool.
 *
 * Traceability:
 * - Requirement: REQ-00011 (Extensible Tool Registry Contract)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI)
 * - Use Case: UC-00009 (MCP External Server Tool Invocation)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
public final class ToolDefinition {

    /** Tool unique identifier name. */
    private final String name;
    /** Human readable tool description. */
    private final String description;
    /** Parameter names and type/doc schema mapping. */
    private final Map<String, String> parameterSchema;

    /**
     * Constructs a ToolDefinition with name, description, and parameter descriptions.
     *
     * @param nameVal tool unique name, must not be null
     * @param descriptionVal tool purpose description, must not be null
     * @param parameterSchemaVal parameter names and description mapping, may be null
     */
    public ToolDefinition(final String nameVal,
                          final String descriptionVal,
                          final Map<String, String> parameterSchemaVal) {
        this.name = Objects.requireNonNull(nameVal, "name must not be null");
        this.description = Objects.requireNonNull(descriptionVal, "description must not be null");
        if (parameterSchemaVal == null) {
            this.parameterSchema = Collections.emptyMap();
        } else {
            this.parameterSchema = Map.copyOf(parameterSchemaVal);
        }
    }

    /**
     * Returns tool name.
     *
     * @return tool name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns tool description.
     *
     * @return tool description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns parameter schema map.
     *
     * @return immutable parameter schema map
     */
    public Map<String, String> getParameterSchema() {
        return parameterSchema;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ToolDefinition)) {
            return false;
        }
        final ToolDefinition that = (ToolDefinition) o;
        return Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(parameterSchema, that.parameterSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, parameterSchema);
    }

    @Override
    public String toString() {
        return "ToolDefinition{"
                + "name='" + name + '\''
                + ", description='" + description + '\''
                + ", parameterSchema=" + parameterSchema
                + '}';
    }
}
