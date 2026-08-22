# Testing Strategy & Suites

Omniwrench implements a tiered verification strategy incorporating Unit, Integration, and Mock testing suites.

## Testing Stack
- **JUnit 5 (Jupiter)**: Execution framework.
- **AssertJ**: Fluent, readable assertions.
- **Mockito**: Boundary mock generation and stubbing.
- **Spring Boot Test**: Context loading and integration tests.

## Test Matrix
| Test Class | Scope | Description |
|---|---|---|
| `OmniwrenchApplicationTests` | Integration | Validates Spring Boot context boot and component discovery |
| `ToolRegistryTest` | Unit | Verifies dynamic tool registration, name lookup, and null safety |
| `AgentEngineTest` | Unit | Tests reasoning loop, prompt processing, and message history |
