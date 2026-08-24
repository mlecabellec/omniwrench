package com.omniwrench.core;

import com.omniwrench.model.ToolDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Ultra-precise OpenAPI 3.1 and model provider JSON Schema function description generator.
 *
 * <p>Translates {@link ToolDefinition} instances into strict, unambiguous tool calling schemas
 * for OpenAI, Gemini, Anthropic, and local LLMs while stripping internal runtime injection parameters (ADR-0053).
 *
 * Traceability:
 * - Requirement: REQ-00060 (Pluggable Tool Registry), REQ-00095 (Ultra-Precise OpenAPI Function Schema Generator)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI), FR-00011 (Multi-Modal Typed AI Abstraction)
 * - Use Case: UC-00009 (MCP External Server Tool Invocation)
 * - Task: TSK-20260822-012 (Ultra-Precise OpenAPI &amp; JSON Schema Function Calling Registry)
 * - ADR: ADR-0053 (Ultra-Precise OpenAPI Tool Descriptions)
 */
public final class OpenApiToolSchemaGenerator {

    /** Internal runtime parameters filtered out from public model function calling schemas. */
    private static final Set<String> FILTERED_INTERNAL_PARAMS = Set.of(
            "context", "sessioncontext", "workspaceroot", "sessionid", "__internal__", "@injected"
    );

    /** Supported model provider schema dialect formats. */
    public enum ProviderFormat {
        /** Standard OpenAI Function Calling schema. */
        OPENAI,
        /** Google Gemini functionDeclarations format. */
        GEMINI,
        /** Anthropic Claude tool_use schema. */
        ANTHROPIC,
        /** Pure OpenAPI 3.1 schema. */
        OPENAPI_3_1
    }

    /** Private constructor for utility class. */
    private OpenApiToolSchemaGenerator() {
    }

    /**
     * Generates a standard OpenAI / OpenAPI 3.1 function calling schema for the given tool definition.
     *
     * @param toolDefinition tool descriptor contract, must not be null
     * @return non-null structured Map representing the function schema
     */
    public static Map<String, Object> generateOpenApiSchema(final ToolDefinition toolDefinition) {
        return generateProviderSchema(toolDefinition, ProviderFormat.OPENAI);
    }

    /**
     * Generates a provider-specific JSON function schema for the given tool definition.
     *
     * @param toolDefinition tool descriptor contract, must not be null
     * @param format target provider format, must not be null
     * @return non-null structured Map conforming to target provider schema
     */
    public static Map<String, Object> generateProviderSchema(final ToolDefinition toolDefinition,
                                                            final ProviderFormat format) {
        final ToolDefinition nonNullDef = Objects.requireNonNull(toolDefinition, "toolDefinition must not be null");
        final ProviderFormat nonNullFormat = Objects.requireNonNull(format, "format must not be null");

        final Map<String, Object> properties = new LinkedHashMap<>();
        final List<String> requiredList = new ArrayList<>();

        for (final Map.Entry<String, String> entry : nonNullDef.getParameterSchema().entrySet()) {
            final String paramName = entry.getKey();
            if (isInternalInjectedParam(paramName)) {
                continue;
            }

            final String paramDoc = entry.getValue() != null ? entry.getValue() : "";
            final Map<String, Object> propSchema = new LinkedHashMap<>();
            propSchema.put("type", inferParameterType(paramDoc));
            propSchema.put("description", paramDoc);

            // Check if required
            if (!paramDoc.toLowerCase(Locale.ROOT).contains("optional")) {
                requiredList.add(paramName);
            }

            properties.put(paramName, propSchema);
        }

        final Map<String, Object> parametersObject = new LinkedHashMap<>();
        parametersObject.put("type", "object");
        parametersObject.put("properties", properties);
        parametersObject.put("required", requiredList);

        return switch (nonNullFormat) {
            case OPENAI, OPENAPI_3_1 -> Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", nonNullDef.getName(),
                            "description", nonNullDef.getDescription(),
                            "parameters", parametersObject
                    )
            );
            case GEMINI -> Map.of(
                    "name", nonNullDef.getName(),
                    "description", nonNullDef.getDescription(),
                    "parameters", parametersObject
            );
            case ANTHROPIC -> Map.of(
                    "name", nonNullDef.getName(),
                    "description", nonNullDef.getDescription(),
                    "input_schema", parametersObject
            );
        };
    }

    private static boolean isInternalInjectedParam(final String paramName) {
        if (paramName == null) {
            return true;
        }
        final String normalized = paramName.trim().toLowerCase(Locale.ROOT);
        return FILTERED_INTERNAL_PARAMS.contains(normalized) || normalized.startsWith("__") || normalized.startsWith("@");
    }

    private static String inferParameterType(final String doc) {
        final String lower = doc.toLowerCase(Locale.ROOT);
        if (lower.contains("integer") || lower.contains("number") || lower.contains("count") || lower.contains("port")) {
            return "integer";
        }
        if (lower.contains("boolean") || lower.contains("flag") || lower.contains("true/false")) {
            return "boolean";
        }
        if (lower.contains("array") || lower.contains("list")) {
            return "array";
        }
        return "string";
    }
}
