# Configuration & Environment Guide

Omniwrench supports hierarchical configuration via CLI flags, environment variables, and `application.yml`.

## Setting Environment Variables

```bash
# Set custom web server port
export PORT=9090

# Set execution mode (dual, tui, web)
export OMNIWRENCH_MODE=tui

# Target specific workspace directory
export OMNIWRENCH_WORKSPACE=/home/vortigern/git/nunki
```
