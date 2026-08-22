# Configuration Specifications

Omniwrench parameters configured via `application.yml` or environment variables:

| Property Path | Environment Variable | Default Value | Description |
|---|---|---|---|
| `server.port` | `PORT` | `8080` | Spring Boot HTTP/WebSocket listening port |
| `omniwrench.mode` | `OMNIWRENCH_MODE` | `dual` | Execution mode: `dual`, `tui`, `web` |
| `omniwrench.workspace-path` | `OMNIWRENCH_WORKSPACE` | `.` | Root path of targeted workspace |
| `omniwrench.tui.theme` | `OMNIWRENCH_TUI_THEME` | `cyberpunk` | ANSI color palette theme |
| `omniwrench.tui.fps-target` | `OMNIWRENCH_TUI_FPS` | `30` | Target render frames per second |
| `omniwrench.engine.max-reasoning-steps` | `OMNIWRENCH_MAX_STEPS` | `50` | Maximum reasoning iterations per user turn |
| `omniwrench.engine.max-threads` | `OMNIWRENCH_MAX_THREADS` | `8` | Thread pool worker thread allocation limit |
