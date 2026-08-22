# Developer Standards & Workflow

Guidelines for contributing to the Omniwrench codebase.

## Workflow Rules
1. **Branch Naming**: Feature branches follow `feature/TSK-YYYYMMDD-XXX-description`.
2. **Pre-Commit Verification**: Run `mvn clean test` before requesting commit clearance.
3. **Commit Authorization**: AI agents must request unambiguous clearance from the human developer before executing `git commit` (`CS-0070.1`).
4. **PlantUML Standards**: Maintain syntax compatibility with PlantUML v1.2020.02.
