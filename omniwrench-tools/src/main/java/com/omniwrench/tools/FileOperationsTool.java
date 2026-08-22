package com.omniwrench.tools;

import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * File viewing, reading, and creation tool for agent workspace interactions.
 * 
 * Traceability:
 * - Requirement: REQ-00021 (Workspace File Operations Tool)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 */
@Component
public class FileOperationsTool implements Tool {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileOperationsTool.class);

    private final ToolDefinition definition;

    public FileOperationsTool() {
        final Map<String, String> schema = new HashMap<>();
        schema.put("action", "Action to perform: 'read', 'write', 'list', 'exists'");
        schema.put("path", "Relative or absolute file/directory path");
        schema.put("content", "Content to write (required if action='write')");
        this.definition = new ToolDefinition("file_ops", "Performs filesystem read, write, and inspection actions", schema);
    }

    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }

    @Override
    public ToolInvocation execute(final SessionContext context, final Map<String, Object> arguments) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");

        final String callId = UUID.randomUUID().toString();
        final String action = String.valueOf(arguments.getOrDefault("action", "read"));
        final String rawPath = String.valueOf(arguments.getOrDefault("path", "."));

        final Path targetPath = Paths.get(context.getWorkspaceRoot()).resolve(rawPath).normalize();
        LOGGER.debug("Executing file_ops action: {} on path: {}", action, targetPath);

        try {
            final String output;
            switch (action) {
                case "read":
                    if (!Files.exists(targetPath)) {
                        return new ToolInvocation(callId, "file_ops", arguments, "Error: File not found at " + targetPath, false, Instant.now());
                    }
                    output = Files.readString(targetPath);
                    break;
                case "write":
                    final String content = String.valueOf(arguments.getOrDefault("content", ""));
                    if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                        Files.createDirectories(targetPath.getParent());
                    }
                    Files.writeString(targetPath, content);
                    output = "Successfully written " + content.length() + " bytes to " + targetPath;
                    break;
                case "exists":
                    output = "Exists: " + Files.exists(targetPath);
                    break;
                case "list":
                    if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
                        return new ToolInvocation(callId, "file_ops", arguments, "Error: Directory not found at " + targetPath, false, Instant.now());
                    }
                    final StringBuilder sb = new StringBuilder();
                    try (Stream<Path> stream = Files.list(targetPath)) {
                        stream.forEach(p -> sb.append(p.getFileName().toString()).append("\n"));
                    }
                    output = sb.toString();
                    break;
                default:
                    return new ToolInvocation(callId, "file_ops", arguments, "Unknown action: " + action, false, Instant.now());
            }

            return new ToolInvocation(callId, "file_ops", arguments, output, true, Instant.now());
        } catch (final IOException e) {
            LOGGER.error("File operations failed on path: {}", targetPath, e);
            return new ToolInvocation(callId, "file_ops", arguments, "IOException: " + e.getMessage(), false, Instant.now());
        }
    }
}
