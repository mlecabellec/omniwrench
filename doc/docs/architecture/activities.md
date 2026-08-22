# Reasoning & Execution Activities

Comprehensive activity diagrams modeling the internal workflows of Omniwrench's reasoning loop, command safety evaluations, and generational context dreaming.

## 1. Hybrid Reasoning Cycle (Single-Step vs Plan-and-Execute DAG)

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

start
:Receive User Prompt;
:Validate Input Preconditions (CS-0030.1);
:Persist Prompt to Session History;

if (Is Prompt Complex Multi-Step Goal?) then (yes)
  :Decompose into Plan-and-Execute DAG;
  :Assign TSK-* Identifiers;
  :Persist DAG to .omniwrench/tasks/{id}.json;
  while (Unexecuted Steps in DAG?) is (yes)
    :Select Next Ready Task Node;
    :Evaluate Security Level (CS-0070);
    if (Requires Human Clearance?) then (yes)
      :Display Confirmation Modal in TUI / Web;
      if (User Approved?) then (yes)
        :Execute Step via ToolRegistry;
      else (rejected)
        :Mark Step REJECTED and Halt Plan;
        break
      endif
    else (auto-cleared)
      :Execute Step via ToolRegistry;
    endif
    :Record Step Output in Checkpoint;
  endwhile (all completed)
  :Synthesize Final Goal Report;
else (single-step)
  :Evaluate Smart Model Router;
  :Stream Reasoning & Tool Calls;
  :Generate Synthesized Response;
endif

:Construct Immutable AgentMessage;
:Validate Postconditions (CS-0050.1);
:Emit Event to ReactorEventBus;
:Render Output to TUI / Web Stream;
stop
@enduml
```

## 2. Command Safety Clearance Protocol (CS-0070)

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

start
:Intercept Tool Invocation (Command / File / Git);
:Classify Operation Safety Level (1 to 9);

if (Safety Level >= 6?) then (destructive)
  :Format Destructive Warning (Impacted files, commands);
  :Prompt User for Interactive Clearance;
  if (Clearance Granted?) then (yes)
    :Log Audit Trail to Tracing Spans;
    :Execute Destructive Operation;
  else (no)
    :Abort Operation;
    :Return HumanRejectionException;
  endif
else (read-only / safe mutation)
  :Validate Workspace Path Containment;
  if (Path Inside Workspace?) then (yes)
    :Execute Operation Automatically;
  else (out of bounds)
    :Reject Access Violation (SecurityException);
  endif
endif
stop
@enduml
```

## 3. Generational Context Dreaming (Compaction) Workflow

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

start
:Track Session Token Usage;
if (Tokens > 75% Context Budget?) then (threshold exceeded)
  :Spawn Background Compaction Worker;
  :Extract Full Session Turn History;
  :Invoke Lightweight Distillation Model ("Dreaming");
  :Generate Structured Summary Block (Decisions, Files, Tasks);
  :Rotate Generation Epoch (new epochId);
  :Archive Old Raw Turns to .omniwrench/sessions/{id}/archive/;
  :Compress Archive Files (gzip / zstd);
  :Prepend Distilled Summary to Active Context Window;
  :Emit EpochRotatedEvent to ReactorEventBus;
else (within budget)
  :Retain Active Context Intact;
endif
stop
@enduml
```

