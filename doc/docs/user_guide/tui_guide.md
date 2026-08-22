# Modern Cyberpunk TUI Guide

The Omniwrench interactive terminal dashboard provides an ultra-responsive, glowing HUD interface.

```
╔══════════════════════════════════════════════════════════════════════════════╗
║  🛠️  O M N I W R E N C H  ::  AUTONOMOUS DUAL AGENT WORKBENCH               ║
║  Inspired by OpenCode & OpenClaw | Java 17+ Spring Boot                     ║
╚══════════════════════════════════════════════════════════════════════════════╝
 [MODE: DUAL] [SESSIONS: 1] [TOOLS: 2 READY] [WEB PORT: 8080] [JVM: 21]

omniwrench> /run ls -la
┌── [USER]
│ /run ls -la
└──
┌── [AGENT / OMNIWRENCH]
│ Command executed:
│ Exit Code: 0
│ Output:
│ pom.xml
│ src
│ doc
└──
```

## Built-in Slash Commands
- `/help`: Displays help message and available commands.
- `/run <command>`: Executes a shell command inside the current workspace.
- `/cat <path>`: Reads and displays file contents.
- `/tools`: Lists all registered tools and their input parameters.
- `exit` or `quit`: Gracefully exits the TUI session.
