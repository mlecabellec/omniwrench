package com.omniwrench.core;

import com.omniwrench.model.ToolDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test suite verifying OpenApiToolSchemaGenerator schema synthesis across OpenAI, Gemini, and Anthropic formats.
 *
 * Traceability:
 * - Requirement: REQ-00095 (Ultra-Precise OpenAPI &amp; JSON Schema Function Descriptions)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI), FR-00011 (Multi-Modal Typed AI Abstraction)
 * - Task: TSK-20260822-012 (Ultra-Precise OpenAPI Function Calling Registry)
 * - ADR: ADR-0053 (Ultra-Precise OpenAPI Tool Descriptions)
 */
@Tag("REQ-00095")
@Tag("FR-00020")
@Tag("TSK-20260822-012")
class OpenApiToolSchemaGeneratorTest {

    @Test
    @DisplayName("Should generate OpenAI function calling schema and filter internal context parameters")
    void testOpenAiSchemaGeneration() {
        final ToolDefinition toolDef = new ToolDefinition(
                "file_search",
                "Searches files matching regex pattern in directory",
                Map.of(
                        "path", "Target directory path to inspect",
                        "max_depth", "Maximum integer depth for traversal (optional)",
                        "recursive", "Boolean flag for deep search",
                        "sessionContext", "Internal runtime context injected by engine"
                )
        );

        final Map<String, Object> schema = OpenApiToolSchemaGenerator.generateOpenApiSchema(toolDef);

        assertThat(schema).containsEntry("type", "function");
        @SuppressWarnings("unchecked")
        final Map<String, Object> function = (Map<String, Object>) schema.get("function");
        assertThat(function).containsEntry("name", "file_search");
        assertThat(function).containsEntry("description", "Searches files matching regex pattern in directory");

        @SuppressWarnings("unchecked")
        final Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
        @SuppressWarnings("unchecked")
        final Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");

        assertThat(properties).containsKey("path");
        assertThat(properties).containsKey("max_depth");
        assertThat(properties).containsKey("recursive");
        assertThat(properties).doesNotContainKey("sessionContext"); // Filtered

        @SuppressWarnings("unchecked")
        final Map<String, Object> maxDepthProp = (Map<String, Object>) properties.get("max_depth");
        assertThat(maxDepthProp).containsEntry("type", "integer");

        @SuppressWarnings("unchecked")
        final Map<String, Object> recursiveProp = (Map<String, Object>) properties.get("recursive");
        assertThat(recursiveProp).containsEntry("type", "boolean");
    }

    @Test
    @DisplayName("Should generate Gemini functionDeclarations format")
    void testGeminiSchemaGeneration() {
        final ToolDefinition toolDef = new ToolDefinition(
                "execute_cmd",
                "Executes system shell command",
                Map.of("command", "The shell command string to execute")
        );

        final Map<String, Object> geminiSchema = OpenApiToolSchemaGenerator.generateProviderSchema(
                toolDef,
                OpenApiToolSchemaGenerator.ProviderFormat.GEMINI
        );

        assertThat(geminiSchema).containsEntry("name", "execute_cmd");
        assertThat(geminiSchema).containsKey("parameters");
        assertThat(geminiSchema).doesNotContainKey("type");
    }

    @Test
    @DisplayName("Should generate Anthropic input_schema format")
    void testAnthropicSchemaGeneration() {
        final ToolDefinition toolDef = new ToolDefinition(
                "read_file",
                "Reads contents of a file",
                Map.of("path", "File path to read")
        );

        final Map<String, Object> anthropicSchema = OpenApiToolSchemaGenerator.generateProviderSchema(
                toolDef,
                OpenApiToolSchemaGenerator.ProviderFormat.ANTHROPIC
        );

        assertThat(anthropicSchema).containsEntry("name", "read_file");
        assertThat(anthropicSchema).containsKey("input_schema");
    }

    @Test
    @DisplayName("Should reject null arguments during schema generation")
    void testNullValidation() {
        assertThrows(NullPointerException.class, () -> OpenApiToolSchemaGenerator.generateOpenApiSchema(null));
        assertThrows(NullPointerException.class, () -> OpenApiToolSchemaGenerator.generateProviderSchema(null, OpenApiToolSchemaGenerator.ProviderFormat.OPENAI));
        assertThrows(NullPointerException.class, () -> OpenApiToolSchemaGenerator.generateProviderSchema(new ToolDefinition("t", "d", Map.of()), null));
    }
}
