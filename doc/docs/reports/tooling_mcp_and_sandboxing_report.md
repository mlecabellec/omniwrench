# Comparative Report: Tool Registry, Model Context Protocol (MCP) & Sandboxing

**Comparison Subjects**: Google Antigravity SDK, OpenClaw, OpenCode  
**Target Platform**: Omniwrench Java 21 / Spring Boot 3.2+ Architecture  
**Focus Area**: Tool definition, MCP client transports, schema sanitization, filesystem sandboxing, SSRF network filters, and host tool hooks.

---

## 1. Tool System Architecture Comparison

| Architectural Aspect | Google Antigravity SDK | OpenClaw | OpenCode | Omniwrench Target Architecture |
| :--- | :--- | :--- | :--- | :--- |
| **Tool Registration** | Python callables + TypeAdapter | Plugin Manifest + SDK Decorators | TypeScript schema functions | **Java MethodHandles + Jackson SPI** |
| **Context Parameter** | Stripped from schema; injected | Bound in context object | Injected via system context | **`@Injected ToolExecutionContext`** |
| **MCP Transports** | Stdio Subprocess & HTTP SSE | Stdio & Remote Gateways | Stdio JSON-RPC | **ProcessBuilder Stdio & Spring WebFlux SSE** |
| **Path Sandboxing** | `policy.workspace_only` | `agents-workspace.ts` | Working Directory Boundary | **Canonical Path Sandboxing Evaluator** |
| **Network Guardrails**| Transport timeout guards | `packages/net-policy` (SSRF filter) | Managed HTTP fetcher | **Private CIDR & Loopback Redaction** |
| **Output Bounding** | Stream chunks | Message line budget | Flat spill files (>50KB) | **Disk Spill Store + Head/Tail Preview** |

---

## 2. Pluggable MCP Client Architecture

Omniwrench can seamlessly consume external MCP servers (e.g. database connectors, GitHub, filesystem, Slack) via two standard transports:

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff
skinparam componentStyle uml2

package "Omniwrench Tool Registry" {
  [ToolRegistry] as REGISTRY
  [MCPClientManager] as MCP_MGR
  [SchemaSanitizer] as SANITIZER
}

package "MCP Transports" {
  [StdioMcpClient (ProcessBuilder)] as STDIO
  [SseMcpClient (Spring WebFlux SSE)] as SSE
}

cloud "External MCP Servers" {
  [Postgres MCP Server] as DB_MCP
  [GitHub MCP Server] as GH_MCP
  [Cloudflare MCP Server] as CF_MCP
}

REGISTRY --> MCP_MGR : Dispatches tool call
MCP_MGR --> STDIO : Local tool execution
MCP_MGR --> SSE : Remote tool execution
STDIO --> DB_MCP : stdio JSON-RPC
STDIO --> GH_MCP : stdio JSON-RPC
SSE --> CF_MCP : HTTP SSE stream
MCP_MGR --> SANITIZER : Normalizes schemas for Gemini / OpenAI
@enduml
```

### Protocol Flow:
1. **`initialize`**: Sends client capabilities (`tools`, `prompts`, `resources`).
2. **`tools/list`**: Fetches dynamic function declarations and JSON schemas.
3. **Filtering**: Applies `enabled_tools` / `disabled_tools` whitelist/blacklist rules to prevent context bloat.
4. **`tools/call`**: Invokes tool with arguments and returns structured `content` blocks.

---

## 3. Sandboxing & Security Guardrails

### 3.1 Filesystem Workspace Containment
To prevent unauthorized directory traversal (e.g. `../../etc/passwd`):
- All target paths are converted to canonical absolute paths (`Path.toRealPath()`).
- Evaluated against registered workspace root paths (`path.startsWith(workspaceRoot)`).
- Symbolic links escaping the workspace boundary are rejected unless explicitly allowed by an operator policy rule.

### 3.2 Network SSRF & Private IP Protection (`net-policy`)
Prevents agents using `web_fetch` or HTTP tools from attacking internal infrastructure:
- Blocks RFC 1918 private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
- Blocks loopback addresses (`127.0.0.0/8`, `::1`).
- Redacts embedded credentials from logged URLs (`http://user:password@host/` $ightarrow$ `http://***:***@host/`).

---

## 4. Host Tool Interceptors & Self-Healing Error Recovery

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

actor Agent
participant "ToolRunner" as Runner
participant "PreToolHook" as Pre
participant "Tool Implementation" as Tool
participant "OnErrorHook" as Err
participant "PostToolHook" as Post

Agent -> Runner: execute("bash", args)
Runner -> Pre: run(context, args)
Pre --> Runner: HookResult(allow=true)
Runner -> Tool: execute(args)

alt Successful Execution
  Tool --> Runner: Result string
  Runner -> Post: run(context, result)
  Post --> Runner: Modified/Logged result
  Runner --> Agent: Final ToolResult
else Execution Throws Exception
  Tool --x Runner: IOException / ProcessError
  Runner -> Err: run(context, exception)
  note over Err: Self-healing error message guidance
  Err --> Runner: "Command failed with exit code 1. Suggest running `git status` to check state."
  Runner --> Agent: Structured ToolResult (Recovered)
end
@enduml
```

---

## 5. Java 21 / Spring Boot 3 Implementation Blueprint for Omniwrench

### 5.1 Stdio MCP Client Implementation
```java
package com.omniwrench.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class StdioMcpClient implements AutoCloseable {

    private final Process process;
    private final BufferedWriter writer;
    private final BufferedReader reader;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    private final ExecutorService readerExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public StdioMcpClient(ProcessBuilder processBuilder) throws IOException {
        this.process = processBuilder.start();
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        startReaderLoop();
    }

    public CompletableFuture<JsonNode> callMethod(String method, JsonNode params) {
        long id = idGenerator.getAndIncrement();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        try {
            var requestNode = mapper.createObjectNode()
                    .put("jsonrpc", "2.0")
                    .put("id", id)
                    .put("method", method);
            if (params != null) {
                requestNode.set("params", params);
            }

            synchronized (writer) {
                writer.write(mapper.writeValueAsString(requestNode));
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private void startReaderLoop() {
        readerExecutor.submit(() -> {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    JsonNode response = mapper.readTree(line);
                    if (response.has("id")) {
                        long id = response.get("id").asLong();
                        CompletableFuture<JsonNode> future = pendingRequests.remove(id);
                        if (future != null) {
                            if (response.has("error")) {
                                future.completeExceptionally(new RuntimeException(response.get("error").toString()));
                            } else {
                                future.complete(response.get("result"));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                pendingRequests.values().forEach(f -> f.completeExceptionally(e));
            }
        });
    }

    @Override
    public void close() throws IOException {
        process.destroy();
        readerExecutor.shutdown();
    }
}
```

---

## 6. Summary Recommendations
1. **Implement Both Stdio and SSE MCP Clients** to integrate with the broader ecosystem of standard MCP tools.
2. **Enforce Canonical Path Workspace Containment** on all file tools to protect developer environments.
3. **Use OnToolError Interceptor Hooks** to provide actionable error explanations directly to the agent model for self-healing.
