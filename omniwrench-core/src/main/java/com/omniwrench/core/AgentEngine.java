package com.omniwrench.core;

import com.omniwrench.config.OmniwrenchProperties;
import com.omniwrench.model.AgentMessage;
import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolInvocation;
import com.omniwrench.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Autonomous agent reasoning loop, tool dispatcher, and multi-step execution coordinator.
 *
 * Traceability:
 * - Requirement: REQ-00043 (Hybrid Reasoning Loop), REQ-00088 (Dual Chat Mode Reasoning Demux)
 * - Feature: FR-00014 (Hybrid Reasoning Loop), FR-00011 (Multi-Modal Typed AI Abstraction)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00002 (Autonomous Goal Planning)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry), TSK-20260822-007 (Dual Chat Mode & Thinking Demux)
 * - ADR: ADR-0008 (Autonomous Reasoning Loop), ADR-0047 (Dual Chat Mode & Reasoning Demux)
 */
@Service
public class AgentEngine {

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentEngine.class);

    /** Prefix length for /run command. */
    private static final int RUN_PREFIX_LENGTH = 5;
    /** Prefix length for /model command. */
    private static final int MODEL_PREFIX_LENGTH = 6;
    /** Prefix length for /thinking command. */
    private static final int THINKING_PREFIX_LENGTH = 9;

    /** Tool registry service. */
    private final ToolRegistry toolRegistry;
    /** Runtime configuration properties. */
    private final OmniwrenchProperties properties;
    /** Thread pool for concurrent background reasoning and tool execution. */
    private final ExecutorService agentThreadPool;

    /** Active thinking effort level. */
    private volatile String thinkingEffort = "medium";
    /** Dual chat mode thinking toggle flag. */
    private volatile boolean thinkingEnabled = true;

    /**
     * Constructs an AgentEngine with tool registry and configuration properties.
     *
     * @param toolRegistryVal tool registry service, must not be null
     * @param propertiesVal configuration properties, must not be null
     */
    public AgentEngine(final ToolRegistry toolRegistryVal, final OmniwrenchProperties propertiesVal) {
        this.toolRegistry = Objects.requireNonNull(toolRegistryVal, "toolRegistry must not be null");
        this.properties = Objects.requireNonNull(propertiesVal, "properties must not be null");

        final int maxThreads = properties.getEngine().getMaxThreads();
        this.agentThreadPool = Executors.newFixedThreadPool(maxThreads, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = new Thread(r, "omniwrench-agent-worker-" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });

        LOGGER.info("Initialized AgentEngine with bounded thread pool size: {}", maxThreads);
    }

    /**
     * Returns true if reasoning thinking mode is active.
     *
     * @return true if thinking is enabled
     */
    public boolean isThinkingEnabled() {
        return thinkingEnabled;
    }

    /**
     * Sets thinking mode enablement.
     *
     * @param enabledVal true to enable thinking mode
     */
    public void setThinkingEnabled(final boolean enabledVal) {
        this.thinkingEnabled = enabledVal;
    }

    /**
     * Returns current thinking effort level (low, medium, high, max).
     *
     * @return thinking effort string
     */
    public String getThinkingEffort() {
        return thinkingEffort;
    }

    /**
     * Sets thinking effort level.
     *
     * @param effortVal effort string (low, medium, high, max)
     */
    public void setThinkingEffort(final String effortVal) {
        this.thinkingEffort = Objects.requireNonNull(effortVal, "effort must not be null");
    }

    /**
     * Executes a step in the dialogue, resolving user prompts and evaluating tool execution plans.
     *
     * @param context session context
     * @param userPrompt user input prompt
     * @return resulting agent message
     */
    public AgentMessage processPrompt(final SessionContext context, final String userPrompt) {
        final SessionContext nonNullContext = Objects.requireNonNull(context, "context must not be null");
        final String nonNullPrompt = Objects.requireNonNull(userPrompt, "userPrompt must not be null");

        LOGGER.info("AgentEngine processing prompt in session {}: '{}'", nonNullContext.getSessionId(), nonNullPrompt);

        final AgentMessage userMessage = AgentMessage.of("user", nonNullPrompt);
        nonNullContext.addMessage(userMessage);

        final List<ToolInvocation> toolInvocations = new ArrayList<>();
        String rawResponseText;

        // Command dispatch simulation / deterministic parsing
        if (nonNullPrompt.startsWith("/run ")) {
            final String cmd = nonNullPrompt.substring(RUN_PREFIX_LENGTH).trim();
            final Optional<Tool> toolOpt = toolRegistry.getTool("run_command");
            if (toolOpt.isPresent()) {
                final ToolInvocation inv = toolOpt.get().execute(nonNullContext, Map.of("command", cmd));
                toolInvocations.add(inv);
                rawResponseText = "Command executed:\n" + inv.getOutput();
            } else {
                rawResponseText = "Error: run_command tool not found in registry.";
            }
        } else if (nonNullPrompt.startsWith("/cat ") || nonNullPrompt.startsWith("/read ")) {
            final String path = nonNullPrompt.substring(nonNullPrompt.indexOf(' ') + 1).trim();
            final Optional<Tool> toolOpt = toolRegistry.getTool("file_ops");
            if (toolOpt.isPresent()) {
                final ToolInvocation inv = toolOpt.get().execute(nonNullContext, Map.of("action", "read", "path", path));
                toolInvocations.add(inv);
                rawResponseText = "File contents of " + path + ":\n" + inv.getOutput();
            } else {
                rawResponseText = "Error: file_ops tool not found in registry.";
            }
        } else if (nonNullPrompt.startsWith("/model ") || "/model".equalsIgnoreCase(nonNullPrompt)) {
            final String subCommand = nonNullPrompt.length() > MODEL_PREFIX_LENGTH
                    ? nonNullPrompt.substring(MODEL_PREFIX_LENGTH).trim() : "list";
            final String[] parts = subCommand.split("\\s+", 2);
            final String action = parts[0];
            final String query = parts.length > 1 ? parts[1] : "";
            final Optional<Tool> toolOpt = toolRegistry.getTool("model_manage");
            if (toolOpt.isPresent()) {
                final ToolInvocation inv = toolOpt.get().execute(nonNullContext, Map.of("action", action, "query", query));
                toolInvocations.add(inv);
                rawResponseText = inv.getOutput();
            } else {
                rawResponseText = "Error: model_manage tool not found in registry.";
            }
        } else if (nonNullPrompt.startsWith("/thinking ") || "/thinking".equalsIgnoreCase(nonNullPrompt)) {
            rawResponseText = handleThinkingCommand(nonNullPrompt);
        } else {
            if (thinkingEnabled) {
                rawResponseText = "<think>\nAnalyzing user request: '" + nonNullPrompt + "'\n"
                        + "Evaluating tool registry (" + toolRegistry.getToolCount() + " tools available)\n"
                        + "Selected strategy: Deterministic acknowledgment and autonomous reasoning state update (effort="
                        + thinkingEffort + ")\n</think>\n"
                        + "Omniwrench Agent acknowledged: '" + nonNullPrompt
                        + "'. Registered tools available: " + toolRegistry.getToolCount()
                        + " (file_ops, run_command, model_manage). Ready for next autonomous cycle.";
            } else {
                rawResponseText = "Omniwrench Agent acknowledged: '" + nonNullPrompt
                        + "'. Registered tools available: " + toolRegistry.getToolCount()
                        + " (file_ops, run_command, model_manage). Ready for next autonomous cycle.";
            }
        }

        // Demux thoughts from response
        final StreamDemuxer.DemuxResult demux = StreamDemuxer.parse(rawResponseText);
        final String thoughtContent = demux.thought();
        final String cleanContent = demux.answer();

        final AgentMessage assistantMessage = new AgentMessage(
                UUID.randomUUID().toString(),
                "assistant",
                cleanContent,
                thoughtContent,
                Instant.now(),
                toolInvocations
        );
        nonNullContext.addMessage(assistantMessage);
        return assistantMessage;
    }

    private String handleThinkingCommand(final String prompt) {
        final String param = prompt.length() > THINKING_PREFIX_LENGTH
                ? prompt.substring(THINKING_PREFIX_LENGTH).trim().toLowerCase(Locale.ROOT) : "";

        if (param.isEmpty() || "status".equals(param)) {
            return "Thinking mode is currently " + (thinkingEnabled ? "ENABLED" : "DISABLED")
                    + " (effort level: " + thinkingEffort + ").";
        } else if ("on".equals(param) || "true".equals(param) || "enable".equals(param)) {
            this.thinkingEnabled = true;
            return "Thinking mode has been ENABLED (effort level: " + thinkingEffort + ").";
        } else if ("off".equals(param) || "false".equals(param) || "disable".equals(param)) {
            this.thinkingEnabled = false;
            return "Thinking mode has been DISABLED.";
        } else if ("low".equals(param) || "medium".equals(param) || "high".equals(param) || "max".equals(param)) {
            this.thinkingEnabled = true;
            this.thinkingEffort = param;
            return "Thinking mode effort set to '" + param + "' (ENABLED).";
        } else {
            return "Unknown thinking command: '" + param + "'. Usage: /thinking [on|off|low|medium|high|max|status]";
        }
    }
}
