package com.omniwrench.tools;

import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Advanced file manipulation, recursive tree exploration, multi-file regex searching, unified patching,
 * and binary hex conversion tool.
 *
 * Traceability:
 * - Requirement: REQ-00060 (Pluggable Tools), REQ-00093 (Multi-Architecture Runtime), REQ-00094 (Async Background Tools)
 * - Feature: FR-00020 (Polyvalent Base Tool SPI), FR-00021 (Sandboxed Execution Engine)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00002 (Autonomous Goal Planning)
 * - Task: TSK-20260822-011 (Advanced File Operations, Binary Manipulation &amp; Background Tool Plugin)
 * - ADR: ADR-0006 (Polyvalent Tool Architecture), ADR-0052 (Asynchronous Tool Execution)
 */
@Component
public final class AdvancedFileOperationsTool implements Tool {

    /** Tool identifier constant. */
    public static final String TOOL_NAME = "advanced_file_ops";

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedFileOperationsTool.class);

    /** Default tree search depth. */
    private static final int DEFAULT_TREE_DEPTH = 5;

    /** Tool definition schema. */
    private final ToolDefinition definition;

    /**
     * Constructs AdvancedFileOperationsTool with registered JSON schemas.
     */
    public AdvancedFileOperationsTool() {
        this.definition = new ToolDefinition(
                TOOL_NAME,
                "Advanced recursive file tree discovery, multi-file regex grep, unified patching, and binary hex conversions.",
                Map.of(
                        "action", "Action to execute: 'tree', 'grep', 'patch', 'slice', 'hex_encode', 'hex_decode'",
                        "path", "Filesystem path to target file or directory",
                        "pattern", "Regex or search pattern for 'grep' or 'tree'",
                        "max_depth", "Maximum depth for 'tree' walking (default: 5)",
                        "target_content", "Exact text block to replace during 'patch'",
                        "replacement_content", "Replacement text block during 'patch'",
                        "start_line", "Start line number (1-indexed) for 'slice'",
                        "end_line", "End line number (1-indexed, inclusive) for 'slice'",
                        "input", "Text string or hex string for 'hex_encode' or 'hex_decode'"
                )
        );
    }

    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }

    @Override
    public ToolInvocation execute(final SessionContext context, final Map<String, Object> arguments) {
        final SessionContext nonNullContext = Objects.requireNonNull(context, "context must not be null");
        final Map<String, Object> nonNullArgs = Objects.requireNonNull(arguments, "arguments must not be null");
        final String callId = UUID.randomUUID().toString();
        final Instant startTime = Instant.now();

        final String action = String.valueOf(nonNullArgs.getOrDefault("action", "")).trim();
        LOGGER.info("Executing AdvancedFileOperationsTool action '{}' in session {}", action, nonNullContext.getSessionId());

        try {
            final String output = switch (action) {
                case "tree" -> executeTree(nonNullContext, nonNullArgs);
                case "grep" -> executeGrep(nonNullContext, nonNullArgs);
                case "patch" -> executePatch(nonNullContext, nonNullArgs);
                case "slice" -> executeSlice(nonNullContext, nonNullArgs);
                case "hex_encode" -> executeHexEncode(nonNullArgs);
                case "hex_decode" -> executeHexDecode(nonNullArgs);
                default -> throw new IllegalArgumentException("Unknown or unsupported action: '" + action
                        + "'. Supported: tree, grep, patch, slice, hex_encode, hex_decode");
            };

            return new ToolInvocation(callId, TOOL_NAME, nonNullArgs, output, true, startTime);
        } catch (final Exception e) {
            LOGGER.error("AdvancedFileOperationsTool failed on action '{}': {}", action, e.getMessage(), e);
            return new ToolInvocation(callId, TOOL_NAME, nonNullArgs, "Error: " + e.getMessage(), false, startTime);
        }
    }

    private String executeTree(final SessionContext context, final Map<String, Object> args) throws IOException {
        final Path targetDir = resolvePath(context, String.valueOf(args.getOrDefault("path", ".")));
        if (!Files.isDirectory(targetDir)) {
            throw new IllegalArgumentException("Path is not a directory: " + targetDir);
        }

        final int maxDepth = parseInteger(args.get("max_depth"), DEFAULT_TREE_DEPTH);
        final String patternStr = (String) args.get("pattern");
        final Pattern pattern = (patternStr != null && !patternStr.isBlank()) ? Pattern.compile(patternStr) : null;

        final StringBuilder sb = new StringBuilder("Directory tree for: ").append(targetDir).append("\n");
        try (Stream<Path> stream = Files.walk(targetDir, maxDepth)) {
            stream.sorted().forEach(path -> {
                final String relative = targetDir.relativize(path).toString();
                if (relative.isEmpty()) {
                    return;
                }
                if (pattern == null || pattern.matcher(relative).find()) {
                    final int depth = path.getNameCount() - targetDir.getNameCount();
                    sb.append("  ".repeat(Math.max(0, depth - 1)))
                            .append(Files.isDirectory(path) ? "📁 " : "📄 ")
                            .append(path.getFileName())
                            .append("\n");
                }
            });
        }
        return sb.toString().trim();
    }

    private String executeGrep(final SessionContext context, final Map<String, Object> args) throws IOException {
        final Path targetDir = resolvePath(context, String.valueOf(args.getOrDefault("path", ".")));
        final String patternStr = String.valueOf(args.getOrDefault("pattern", ""));
        if (patternStr.isBlank()) {
            throw new IllegalArgumentException("Pattern must not be blank for grep");
        }

        final Pattern regex = Pattern.compile(patternStr);
        final List<String> matches = new ArrayList<>();

        if (Files.isRegularFile(targetDir)) {
            grepFile(targetDir, regex, matches);
        } else if (Files.isDirectory(targetDir)) {
            try (Stream<Path> stream = Files.walk(targetDir, DEFAULT_TREE_DEPTH)) {
                stream.filter(Files::isRegularFile).forEach(file -> grepFile(file, regex, matches));
            }
        } else {
            throw new IllegalArgumentException("Target path does not exist: " + targetDir);
        }

        if (matches.isEmpty()) {
            return "No matches found for pattern: " + patternStr;
        }
        return String.join("\n", matches);
    }

    private void grepFile(final Path file, final Pattern regex, final List<String> matches) {
        try {
            final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                final String line = lines.get(i);
                final Matcher m = regex.matcher(line);
                if (m.find()) {
                    matches.add(file.getFileName() + ":" + (i + 1) + ": " + line.trim());
                }
            }
        } catch (final Exception ignored) {
            // Skip binary or unreadable files gracefully
        }
    }

    private String executePatch(final SessionContext context, final Map<String, Object> args) throws IOException {
        final Path filePath = resolvePath(context, String.valueOf(args.getOrDefault("path", "")));
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Target file does not exist for patch: " + filePath);
        }

        final String targetContent = (String) args.get("target_content");
        final String replacementContent = (String) args.get("replacement_content");

        if (targetContent == null || replacementContent == null) {
            throw new IllegalArgumentException("Both 'target_content' and 'replacement_content' are required for patch");
        }

        final String fileContent = Files.readString(filePath, StandardCharsets.UTF_8);
        if (!fileContent.contains(targetContent)) {
            throw new IllegalArgumentException("Target content not found in file: " + filePath);
        }

        final String updated = fileContent.replace(targetContent, replacementContent);
        Files.writeString(filePath, updated, StandardCharsets.UTF_8);

        return "Successfully patched file: " + filePath;
    }

    private String executeSlice(final SessionContext context, final Map<String, Object> args) throws IOException {
        final Path filePath = resolvePath(context, String.valueOf(args.getOrDefault("path", "")));
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Target file does not exist for slice: " + filePath);
        }

        final List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        final int startLine = Math.max(1, parseInteger(args.get("start_line"), 1));
        final int endLine = Math.min(lines.size(), parseInteger(args.get("end_line"), lines.size()));

        if (startLine > lines.size() || startLine > endLine) {
            throw new IllegalArgumentException("Invalid slice bounds: start=" + startLine + ", end=" + endLine + ", total=" + lines.size());
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = startLine - 1; i < endLine; i++) {
            sb.append(i + 1).append(": ").append(lines.get(i)).append("\n");
        }
        return sb.toString().trim();
    }

    private String executeHexEncode(final Map<String, Object> args) {
        final String input = String.valueOf(args.getOrDefault("input", ""));
        final byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(bytes);
    }

    private String executeHexDecode(final Map<String, Object> args) {
        final String hex = String.valueOf(args.getOrDefault("input", "")).replaceAll("\\s+", "");
        final byte[] bytes = HexFormat.of().parseHex(hex);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Path resolvePath(final SessionContext context, final String rawPath) {
        final Path target = Path.of(rawPath);
        if (target.isAbsolute()) {
            return target.normalize();
        }
        return Path.of(context.getWorkspaceRoot(), rawPath).normalize();
    }

    private int parseInteger(final Object val, final int fallback) {
        if (val instanceof Number num) {
            return num.intValue();
        }
        if (val instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (final NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
