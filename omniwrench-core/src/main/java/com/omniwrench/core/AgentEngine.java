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
 * - Requirement: REQ-00030 (Autonomous Agent Reasoning Loop)
 * - Task: TSK-20260822-005 (Pluggable Tool Registry & Agent Execution Loop)
 */
@Service
public class AgentEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentEngine.class);

    private final ToolRegistry toolRegistry;
    private final OmniwrenchProperties properties;
    private final ExecutorService agentThreadPool;

    public AgentEngine(final ToolRegistry toolRegistry, final OmniwrenchProperties properties) {
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");

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
        final String responseText;

        // Command dispatch simulation / deterministic parsing
        if (nonNullPrompt.startsWith("/run ")) {
            final String cmd = nonNullPrompt.substring(5).trim();
            final Optional<Tool> toolOpt = toolRegistry.getTool("run_command");
            if (toolOpt.isPresent()) {
                final ToolInvocation inv = toolOpt.get().execute(nonNullContext, Map.of("command", cmd));
                toolInvocations.add(inv);
                responseText = "Command executed:\n" + inv.getOutput();
            } else {
                responseText = "Error: run_command tool not found in registry.";
            }
        } else if (nonNullPrompt.startsWith("/cat ") || nonNullPrompt.startsWith("/read ")) {
            final String path = nonNullPrompt.substring(nonNullPrompt.indexOf(' ') + 1).trim();
            final Optional<Tool> toolOpt = toolRegistry.getTool("file_ops");
            if (toolOpt.isPresent()) {
                final ToolInvocation inv = toolOpt.get().execute(nonNullContext, Map.of("action", "read", "path", path));
                toolInvocations.add(inv);
                responseText = "File contents of " + path + ":\n" + inv.getOutput();
            } else {
                responseText = "Error: file_ops tool not found in registry.";
            }
        } else {
            responseText = "Omniwrench Agent acknowledged: '" + nonNullPrompt 
                    + "'. Registered tools available: " + toolRegistry.getToolCount() 
                    + " (file_ops, run_command). Ready for next autonomous cycle.";
        }

        final AgentMessage assistantMessage = new AgentMessage(
                UUID.randomUUID().toString(),
                "assistant",
                responseText,
                Instant.now(),
                toolInvocations
        );
        nonNullContext.addMessage(assistantMessage);
        return assistantMessage;
    }
}
