package com.omniwrench.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata definition and schema contract for an Omniwrench executable tool.
 * 
 * Traceability:
 * - Requirement: REQ-00011 (Extensible Tool Registry Contract)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
public final class ToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, String> parameterSchema;

    public ToolDefinition(final String name,
                          final String description,
                          final Map<String, String> parameterSchema) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        if (parameterSchema == null) {
            this.parameterSchema = Collections.emptyMap();
        } else {
            this.parameterSchema = Map.copyOf(parameterSchema);
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

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
