# Omniwrench User Manual: Overview & Quick Start

Omniwrench is an autonomous engineering assistant and developer workbench for mission-critical software engineering.

```plantuml
@startuml
skinparam backgroundColor #2e303f
skinparam defaultFontColor #ffffff

start
:Clone or install Omniwrench;
:Execute './omniwrench-helper.sh check';
:Choose execution mode;
fork
  :Launch TUI ('./omniwrench-helper.sh tui');
fork again
  :Launch Web Server ('./omniwrench-helper.sh web');
fork again
  :Launch Dual Mode ('./omniwrench-helper.sh');
end fork
:Issue prompts and commands;
stop
@enduml
```

## System Requirements
- **Java**: OpenJDK 17 or higher (tested with Eclipse Temurin 17 and 21).
- **Maven**: Apache Maven 3.8+ (or bundled wrapper).
- **Operating System**: Linux (Debian, SUSE, RHEL, Arch, Ubuntu), macOS, or Windows (WSL2).
- **Terminal**: Modern ANSI/VT100 terminal supporting 256 colors or UTF-8 box characters.
