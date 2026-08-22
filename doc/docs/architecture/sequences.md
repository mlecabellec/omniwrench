# Execution Sequences

Detailed sequence diagrams illustrating message flows, tool executions, AI streaming, swarm consensus, and protocol bridges across Omniwrench.

## 1. Interactive TUI Reasoning & Smart Router Sequence

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

actor User as U
participant "OmniwrenchTuiDashboard" as TUI
participant "AgentEngine" as ENG
participant "SmartModelRouter" as ROUTER
participant "OpenAiCompatibleAdapter" as ADAPT
participant "ToolRegistry" as REG
participant "JavaParserAstTool" as AST

U -> TUI : Inputs prompt: "/refactor OrderService"
activate TUI
TUI -> ENG : processPrompt(session, prompt)
activate ENG

ENG -> ROUTER : route(request)
ROUTER -> ROUTER : evaluateComplexity(EXPERT)
ROUTER --> ENG : OpenAiCompatibleAdapter

ENG -> ADAPT : executeStream(request, chunkConsumer)
activate ADAPT
ADAPT --> ENG : Stream token chunks ("Planning refactor...")
ENG -> REG : getTool("ast_refactor")
activate REG
REG --> ENG : Optional<JavaParserAstTool>
deactivate REG

ENG -> AST : execute(context, {action:"modifyMethod"})
activate AST
AST -> AST : LexicalPreservingPrinter.print(...)
AST --> ENG : ToolInvocation(success=true)
deactivate AST

ADAPT --> ENG : Complete response
deactivate ADAPT
ENG --> TUI : AgentMessage(response)
deactivate ENG
TUI --> U : Displays result with Neon Diff
deactivate TUI
@enduml
```

## 2. Dynamic Subagent Swarm Consensus Protocol Sequence

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

participant "LeadOrchestrator" as ORCH
participant "SwarmCoordinator" as COORD
participant "Worker1 (Proposer)" as W1
participant "Worker2 (Critic)" as W2
participant "ConsensusCoordinator" as CONS

ORCH -> COORD : delegateSubtask("Database Migration Strategy")
activate COORD
COORD -> W1 : sendEnvelope(PROPOSAL_REQUEST)
activate W1
W1 -> W1 : analyzesSchema()
W1 -> COORD : sendEnvelope(PROPOSAL, "Use Liquibase with idempotent changesets")
deactivate W1

COORD -> W2 : sendEnvelope(CRITIQUE_REQUEST, proposal)
activate W2
W2 -> W2 : analyzesRisks()
W2 -> COORD : sendEnvelope(CRITIQUE, "Approve with rollback guard")
deactivate W2

COORD -> CONS : openConsensusRound(topic, [W1, W2])
activate CONS
CONS -> W1 : requestVote()
W1 --> CONS : Vote(approve=true, confidence=0.95)
CONS -> W2 : requestVote()
W2 --> CONS : Vote(approve=true, confidence=0.90)

CONS -> CONS : evaluateQuorum(confidence >= 0.66)
CONS --> COORD : ConsensusResult(PASSED, resolution)
deactivate CONS

COORD --> ORCH : TaskResult(PASSED, resolution)
deactivate COORD
@enduml
```

## 3. Pluggable Protocol Bridge & Home Assistant Telemetry Sequence

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

actor "Home Assistant Hub" as HA
participant "HomeAssistantTool" as BRIDGE
participant "ReactorEventBus" as BUS
participant "WebSocketTelemetry" as WS
actor "Browser Web HUD" as WEB

HA -> BRIDGE : WebSocket Event ("state_changed", entity="light.lab_main", state="on")
activate BRIDGE
BRIDGE -> BRIDGE : parseEntityState()
BRIDGE -> BUS : publish(ProtocolMessage(topic="ha.entity.light.lab_main", payload="..."))
deactivate BRIDGE

activate BUS
BUS -> WS : onEvent(ProtocolMessage.class)
deactivate BUS
activate WS
WS -> WEB : STOMP frame (/topic/telemetry/ha)
deactivate WS
WEB --> WEB : Updates live dashboard toggle
@enduml
```

## 4. 5-Stage Verification Quality Gate Sequence

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

actor "Developer / CI" as CALLER
participant "OmniwrenchVerificationGate" as GATE
participant "MavenCompiler" as COMP
participant "CheckstylePMD" as LINT
participant "SurefireRunner" as TEST
participant "MkDocsKit" as DOC
participant "GitDiffAuditor" as AUDIT

CALLER -> GATE : verifyAll()
activate GATE

GATE -> COMP : Stage 1: compileJava()
activate COMP
COMP --> GATE : 0 compilation errors
deactivate COMP

GATE -> LINT : Stage 2: checkCodeQuality()
activate LINT
LINT --> GATE : 0 Checkstyle / PMD violations
deactivate LINT

GATE -> TEST : Stage 3: runAllModuleTests()
activate TEST
TEST --> GATE : 100% test pass rate
deactivate TEST

GATE -> DOC : Stage 4: buildDocumentation()
activate DOC
DOC --> GATE : HTML5 + PDF built with 0 errors
deactivate DOC

GATE -> AUDIT : Stage 5: auditDeletionImpact()
activate AUDIT
AUDIT --> GATE : No unintended critical file deletions
deactivate AUDIT

GATE --> CALLER : QualityGateResult(SUCCESS)
deactivate GATE
@enduml
```

