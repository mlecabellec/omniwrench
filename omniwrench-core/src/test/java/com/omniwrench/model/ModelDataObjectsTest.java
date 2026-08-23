package com.omniwrench.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests verifying core domain entities equality contracts, serialization invariants, and null safety.
 *
 * Traceability:
 * - Requirement: REQ-00010 (Coding Standards & Immutability), REQ-00011 (Tool Metadata), REQ-00012 (Tool Results), REQ-00013 (Session Isolation)
 * - Feature: FR-00004 (Multi-Module Clean Layering), FR-00020 (Polyvalent Base Tool SPI)
 * - Use Case: UC-00001 (Interactive TUI Pair Programming), UC-00002 (Autonomous Goal Planning)
 * - Task: TSK-20260822-001 (Project Initialization & Dual Skeleton)
 */
@Tag("REQ-00010")
@Tag("FR-00004")
@Tag("UC-00001")
@Tag("TSK-20260822-001")
class ModelDataObjectsTest {

    @Test
    @DisplayName("Should verify AgentMessage contract, factory methods, and null handling")
    void shouldVerifyAgentMessage() {
        final Instant now = Instant.now();
        final ToolInvocation toolInv = new ToolInvocation("c-1", "file_ops", Map.of(), "ok", true, now);
        final AgentMessage msg1 = new AgentMessage("msg-1", "user", "Hello", now, List.of(toolInv));
        final AgentMessage msg2 = new AgentMessage("msg-1", "user", "Hello", now, List.of(toolInv));

        assertThat(msg1.getId()).isEqualTo("msg-1");
        assertThat(msg1.getRole()).isEqualTo("user");
        assertThat(msg1.getContent()).isEqualTo("Hello");
        assertThat(msg1.getTimestamp()).isEqualTo(now);
        assertThat(msg1.getToolInvocations()).containsExactly(toolInv);
        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
        assertThat(msg1.toString()).contains("id='msg-1'").contains("role='user'").contains("content='Hello'");

        final AgentMessage factoryMsg = AgentMessage.of("assistant", "Response text");
        assertThat(factoryMsg.getId()).isNotBlank();
        assertThat(factoryMsg.getRole()).isEqualTo("assistant");
        assertThat(factoryMsg.getContent()).isEqualTo("Response text");
        assertThat(factoryMsg.getToolInvocations()).isEmpty();

        final AgentMessage factoryWithTools = AgentMessage.of("assistant", "Calling tool", List.of(toolInv));
        assertThat(factoryWithTools.getToolInvocations()).containsExactly(toolInv);

        assertThrows(NullPointerException.class, () -> new AgentMessage(null, "user", "text", now, List.of()));
        assertThrows(NullPointerException.class, () -> new AgentMessage("id", null, "text", now, List.of()));
        assertThrows(NullPointerException.class, () -> new AgentMessage("id", "user", null, now, List.of()));
        assertThrows(NullPointerException.class, () -> new AgentMessage("id", "user", "text", null, List.of()));
    }

    @Test
    @DisplayName("Should verify SessionContext contract and thread-safe message accumulation")
    void shouldVerifySessionContext() {
        final SessionContext session = SessionContext.createDefault("/tmp/workspace");
        assertThat(session.getSessionId()).isNotBlank();
        assertThat(session.getWorkspaceRoot()).isEqualTo("/tmp/workspace");
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getMessages()).isEmpty();

        final AgentMessage msg = AgentMessage.of("assistant", "Ready to assist");
        session.addMessage(msg);
        assertThat(session.getMessages()).containsExactly(msg);
        assertThat(session.toString()).contains("sessionId='").contains("workspaceRoot='/tmp/workspace'");
        assertThat(session).isEqualTo(session);
        assertThat(session.hashCode()).isEqualTo(session.hashCode());

        assertThrows(NullPointerException.class, () -> session.addMessage(null));
        assertThrows(NullPointerException.class, () -> new SessionContext(null, "/tmp"));
        assertThrows(NullPointerException.class, () -> new SessionContext("id", null));
    }

    @Test
    @DisplayName("Should verify ToolDefinition schema contract and immutability")
    void shouldVerifyToolDefinition() {
        final Map<String, String> schema = Map.of("path", "file path to read", "format", "raw");
        final ToolDefinition toolDef1 = new ToolDefinition("file_read", "Reads a file", schema);
        final ToolDefinition toolDef2 = new ToolDefinition("file_read", "Reads a file", schema);

        assertThat(toolDef1.getName()).isEqualTo("file_read");
        assertThat(toolDef1.getDescription()).isEqualTo("Reads a file");
        assertThat(toolDef1.getParameterSchema()).isEqualTo(schema);
        assertThat(toolDef1).isEqualTo(toolDef2);
        assertThat(toolDef1.hashCode()).isEqualTo(toolDef2.hashCode());
        assertThat(toolDef1.toString()).contains("name='file_read'");

        final ToolDefinition emptySchemaDef = new ToolDefinition("noop", "No op", null);
        assertThat(emptySchemaDef.getParameterSchema()).isEmpty();

        assertThrows(NullPointerException.class, () -> new ToolDefinition(null, "desc", schema));
        assertThrows(NullPointerException.class, () -> new ToolDefinition("name", null, schema));
    }

    @Test
    @DisplayName("Should verify ToolInvocation lifecycle result record")
    void shouldVerifyToolInvocation() {
        final Instant now = Instant.now();
        final Map<String, Object> args = Map.of("path", "/tmp/demo.txt");
        final ToolInvocation inv1 = new ToolInvocation("call-1", "file_read", args, "content here", true, now);
        final ToolInvocation inv2 = new ToolInvocation("call-1", "file_read", args, "content here", true, now);

        assertThat(inv1.getCallId()).isEqualTo("call-1");
        assertThat(inv1.getToolName()).isEqualTo("file_read");
        assertThat(inv1.getArguments()).isEqualTo(args);
        assertThat(inv1.getOutput()).isEqualTo("content here");
        assertThat(inv1.isSuccess()).isTrue();
        assertThat(inv1.getExecutedAt()).isEqualTo(now);
        assertThat(inv1).isEqualTo(inv2);
        assertThat(inv1.hashCode()).isEqualTo(inv2.hashCode());
        assertThat(inv1.toString()).contains("callId='call-1'").contains("success=true");

        final ToolInvocation invNullArgs = new ToolInvocation("call-2", "ping", null, "pong", true, now);
        assertThat(invNullArgs.getArguments()).isEmpty();

        assertThrows(NullPointerException.class, () -> new ToolInvocation(null, "tool", args, "out", true, now));
        assertThrows(NullPointerException.class, () -> new ToolInvocation("id", null, args, "out", true, now));
        assertThrows(NullPointerException.class, () -> new ToolInvocation("id", "tool", args, null, true, now));
        assertThrows(NullPointerException.class, () -> new ToolInvocation("id", "tool", args, "out", true, null));
    }
}
