#!/usr/bin/env bash
export OMNIWRENCH_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export OMNIWRENCH_WORKSPACE="$OMNIWRENCH_HOME"
export PATH="$OMNIWRENCH_HOME:$PATH"

# Source GraalVM environment if present
if [[ -f "$OMNIWRENCH_HOME/graalvm-env.sh" ]]; then
    source "$OMNIWRENCH_HOME/graalvm-env.sh"
elif [[ -d "$HOME/.graalvm" ]]; then
    GRAALVM_CANDIDATE=$(find "$HOME/.graalvm" -maxdepth 2 -name "bin" -type d | grep -E "jdk|graalvm" | head -1)
    if [[ -n "$GRAALVM_CANDIDATE" ]]; then
        export GRAALVM_HOME="$(dirname "$GRAALVM_CANDIDATE")"
        export JAVA_HOME="$GRAALVM_HOME"
        export JDK_HOME="$GRAALVM_HOME"
        export PATH="$GRAALVM_HOME/bin:$PATH"
    fi
fi

echo "Omniwrench environment activated."
echo "  OMNIWRENCH_HOME: $OMNIWRENCH_HOME"
echo "  OMNIWRENCH_WORKSPACE: $OMNIWRENCH_WORKSPACE"
if [[ -n "${GRAALVM_HOME:-}" ]]; then
    echo "  GRAALVM_HOME:    $GRAALVM_HOME"
    echo "  JAVA_HOME:       $JAVA_HOME"
    echo "  JDK_HOME:        $JDK_HOME"
fi
