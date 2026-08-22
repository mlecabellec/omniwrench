# Quality Assurance Index

Omniwrench enforces continuous quality gatekeeping through automated static analysis, architectural linting, and coverage verifiers.

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

start
:Code Written / Modified;
:Checkstyle Analysis (Traceability, Naming, No Magic Numbers);
if (Checkstyle Pass?) then (yes)
  :PMD Static Analysis (Parentheses, Modifiers, Best Practices);
  if (PMD Pass?) then (yes)
    :JUnit 5 & AssertJ Unit Tests;
    :Surefire Test Pass;
    :JaCoCo Code Coverage Verification;
    :All Quality Gates Satisfied;
    stop
  else (no)
    :Report PMD Violations & Halt;
    end
  endif
else (no)
  :Report Checkstyle Violations & Halt;
  end
endif
@enduml
```

## Quality Pillars
1. **Traceability Verification**: Every class and method contains traceability annotations (`CS-0010`).
2. **Defensive Preconditions**: Zero unvalidated parameters upon entry (`CS-0030.1`).
3. **No Local `var`**: Explicit typing required across all variables (`CS-0030.10`).
4. **Symbolic Limitations**: Zero magic numbers (`CS-0040.1`).
