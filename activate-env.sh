#!/usr/bin/env bash
export OMNIWRENCH_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export OMNIWRENCH_WORKSPACE="$OMNIWRENCH_HOME"
export PATH="$OMNIWRENCH_HOME:$PATH"

echo "Omniwrench environment activated."
echo "  OMNIWRENCH_HOME: $OMNIWRENCH_HOME"
echo "  OMNIWRENCH_WORKSPACE: $OMNIWRENCH_WORKSPACE"
