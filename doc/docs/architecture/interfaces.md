# Interfaces & SPIs

Core interfaces enabling runtime extensibility for tools, subagent engines, and session storage.

## 1. Tool SPI (`com.omniwrench.tools.Tool`)

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

## 2. Session Context Contract (`com.omniwrench.model.SessionContext`)
Exposes thread-safe methods for querying conversation messages and workspace path resolution.
