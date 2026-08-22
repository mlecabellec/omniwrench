# Quality Assurance & 5-Stage Verification Gate

Omniwrench enforces continuous quality gatekeeping through a strict **5-Stage Verification Protocol** (ADR-0028) integrating compilation, static analysis, test execution, documentation build, and deletion impact verification.

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff
skinparam ActivityBackgroundColor #3d405b
skinparam ActivityBorderColor #00ffcc

start
:Stage 1: Clean Compilation (`mvn compile`);
if (Compilation Success?) then (yes)
  :Stage 2: Static Analysis (Checkstyle & PMD);
  if (Checkstyle & PMD Violations == 0?) then (yes)
    :Stage 3: Automated Test Pass (JUnit 5 & AssertJ);
    if (100% Tests Pass?) then (yes)
      :Stage 4: Documentation Build (`mkdocs-kit build`);
      if (Doc Build & Diagram Render OK?) then (yes)
        :Stage 5: Deletion & Diff Impact Analysis (CS-0070);
        if (Human Clearance Granted?) then (yes)
          :Quality Gate Verified - Ready to Commit;
          stop
        else (no)
          :Clearance Denied by Developer;
          end
        endif
      else (no)
        :Doc Build or Diagram Error;
        end
      endif
    else (no)
      :Unit / Integration Test Failure;
      end
    endif
  else (no)
    :Checkstyle or PMD Violation;
    end
  endif
else (no)
  :Compilation Error;
  end
endif
@enduml
```

## Quality Pillars & Enforced Standards
1. **Stage 1: Clean Compilation**: Java 21 compilation with `-parameters` and zero warnings.
2. **Stage 2: Static Analysis**: Checkstyle (`checkstyle.xml`) and PMD (`pmd-ruleset.xml`) enforcement.
3. **Stage 3: Automated Test Pass**: 100% test execution pass rate across all modules (`mvn test`).
4. **Stage 4: Documentation Verification**: Full `mkdocs-kit` HTML/PDF compilation with zero broken links.
5. **Stage 5: Deletion & Impact Analysis**: Strict human clearance guardrails per `CS-0070`.

