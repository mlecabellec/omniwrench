# Project Management & Governance (2026-2029)

This document establishes the strict governance, capacity model, and architectural milestones for Omniwrench in alignment with the **Industrial Infrastructure Management 2026-2029** charter.

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

gantt
title Omniwrench & Industrial Infrastructure Roadmap 2026-2029
dateFormat YYYY-MM-DD
section Core Engine
Omniwrench Initial Skeleton       :done,    2026-08-22, 2026-08-30
Cyberpunk TUI HUD & Stream Engine :active,  2026-08-25, 2026-09-30
Autonomous Subagent Dispatcher    :         2026-10-01, 2026-12-15
section Infrastructure Migrations
Debian 11 & SUSE 15 SP6 Migration :active,  2026-08-01, 2027-06-30
Hardware Refresh Cycle (€100k/yr) :         2026-01-01, 2029-12-31
NetApp Storage Replacement        :         2028-06-01, 2029-06-30
@enduml
```

---

## 🎯 Global Constraints & Resource Boundaries

* **Project Time Allocation**: Maximum **1,600 hours / year** allocated to infrastructure and framework engineering.
* **Hardware Capital Budget**: Total **€400,000** allocated over 2026-2029 (Target rate: **€100,000 / year**).
* **Team Allocation**: **3.0 FTE** (1.5 FTE Internal + 1.5 FTE External).
* **Critical Operational Tasks**:
  1. Debian 11 & SUSE 15 SP6 Operating System Migrations.
  2. NetApp Storage Infrastructure Replacement (2029).
* **Tooling Standard**: PlantUML (v1.2020.02 compatibility enforced), Python cost analysis modeling.

---

## 📋 Active Tasks Register

| Task Reference | Title | Owner | Status |
|---|---|---|---|
| [`TSK-20260822-001`](tasks/TSK-20260822-001.md) | Project Initialization & Maven Dual Skeleton Setup | AI Agent | Completed |
| [`TSK-20260822-002`](tasks/TSK-20260822-002.md) | Documentation Kit Setup & Quality Standards Incorporation | AI Agent | Completed |
| [`TSK-20260822-003`](tasks/TSK-20260822-003.md) | Modern Cyberpunk TUI Design & Integration | AI Agent | Completed |
| [`TSK-20260822-004`](tasks/TSK-20260822-004.md) | Spring Web & Reactive WebSocket Server Engine | AI Agent | Completed |
| [`TSK-20260822-005`](tasks/TSK-20260822-005.md) | Pluggable Tool Registry & Agent Execution Loop | AI Agent | Completed |
| [`TSK-20260822-006`](tasks/TSK-20260822-006.md) | Maven Multi-Module Modular Architecture Restructuring | AI Agent | Completed |
