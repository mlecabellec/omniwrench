# Interfaces & Service Provider Interfaces (SPIs)

Omniwrench is architected around clean, decoupled Java SPI contracts to enable runtime extensibility for tools, multi-modal AI inference engines, network protocols, reactive event broadcasting, and storage backends.

## 1. Tool SPI (`com.omniwrench.tools.Tool`)

Enables dynamic tool registration via Spring `@Component` scanning or Java `ServiceLoader` JAR loading (`ADR-0007`, `ADR-0010`).

```java
package com.omniwrench.tools;

import com.omniwrench.model.SessionContext;
import com.omniwrench.model.ToolDefinition;
import com.omniwrench.model.ToolInvocation;
import java.util.Map;

public interface Tool {
    ToolDefinition getDefinition();
    ToolInvocation execute(SessionContext context, Map<String, Object> arguments);
}
```

## 2. Multi-Modal AI Backend Adapter SPI (`com.omniwrench.ai.BackendAdapter<T>`)

Type-safe generic adapter SPI supporting diverse media types and inference execution modes (`ADR-0015`).

```java
package com.omniwrench.ai;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface BackendAdapter<T extends MediaType> {
    String getProviderId();
    ModelResponse<T> execute(ModelRequest<T> request) throws BackendException;
    CompletableFuture<ModelResponse<T>> executeAsync(ModelRequest<T> request);
    void executeStream(ModelRequest<T> request, Consumer<ModelResponse<T>> chunkConsumer);
    boolean supports(Class<? extends MediaType> mediaTypeClass, ExecutionMode mode);
}
```

## 3. Multi-Modal MediaType Hierarchy (`com.omniwrench.ai.MediaType`)

Sealed type hierarchy ensuring compile-time exhaustiveness across all AI media formats (`ADR-0015`).

```java
package com.omniwrench.ai;

public sealed interface MediaType permits
        MediaType.TextCompletion,
        MediaType.ChatReasoning,
        MediaType.ImageGeneration,
        MediaType.ImageTransformation,
        MediaType.EmbeddingGeneration,
        MediaType.DataflowProcessing {

    record TextCompletion(String prompt, String completion) implements MediaType {}
    record ChatReasoning(String systemPrompt, String query, String reasoningChain) implements MediaType {}
    record ImageGeneration(String prompt, int width, int height, byte[] imageData) implements MediaType {}
    record ImageTransformation(byte[] sourceImage, String instruction, byte[] outputImage) implements MediaType {}
    record EmbeddingGeneration(String textContent, float[] vector) implements MediaType {}
    record DataflowProcessing(String pipelineId, byte[] inputPayload, byte[] processedOutput) implements MediaType {}
}
```

## 4. Pluggable Protocol Bridge SPI (`com.omniwrench.protocol.ProtocolBridge`)

Enables external protocol integrations (HTTP/REST, WebSocket, Home Assistant, MQTT) to publish and subscribe directly within the agent event mesh (`ADR-0029`).

```java
package com.omniwrench.protocol;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface ProtocolBridge {
    String getProtocolId();
    CompletableFuture<Void> connect();
    CompletableFuture<Void> disconnect();
    CompletableFuture<Boolean> publish(ProtocolMessage message);
    CompletableFuture<String> subscribe(String topicPattern, Consumer<ProtocolMessage> listener);
    boolean isConnected();
}
```

## 5. Reactive EventBus Contract (`com.omniwrench.core.ReactorEventBus`)

High-throughput, zero-blocking multicast event stream backed by Project Reactor Sinks (`ADR-0030`).

```java
package com.omniwrench.core;

import reactor.core.publisher.Flux;

public interface EventBus {
    <E> void publish(E event);
    <E> Flux<E> onEvent(Class<E> eventType);
    Flux<Object> onTopic(String topicPattern);
}
```

## 6. Model Context Protocol (MCP) Host Contract (`com.omniwrench.mcp.McpServerHost`)

Exposes Omniwrench tools and session resources to external IDEs over Stdio or SSE transport (`ADR-0036`).

```java
package com.omniwrench.mcp;

import java.util.List;

public interface McpServerHost {
    void startStdio();
    void startSse(int port);
    List<McpToolDescriptor> listTools();
    McpToolResult invokeTool(String toolName, String argumentsJson);
    void stop();
}
```

