#!/bin/bash
set -e

echo \"--- Starting Robust Documentation Build ---\"

# 1. Pre-flight Validation
echo \"[Step 1] Validating environment...\"
if ! command -v mkdocs > /dev/null; then
    echo \"Error: 'mkdocs' not found in path.\"
    exit 1
fi

# 2. Execution with Output Capture
# We use a dedicated build directory to ensure isolation from the root context during construction.
BUILD_DIR=\"./doc\\\"
echo \"[Step 2] Executing documentation generation via mkdocs-kit...\"
python3 -m mkdocs_kit build --output-dir \\$BUILD_DIR

# 3. Verification Loop
echo \"[Step 3] Verifying built artifacts...\"
REQUIRED_PATHS=(
    \"doc/site/index.html\"
    \"doc/site/use_quide/overview.html\"
)

for path in \"\${REQUIRED_PATHS[@]}\"; do
    if [ ! -f \"\$$path\" ]; then
        echo \"Critical Error: Missing required asset $path\"
        exit 1
    fi
done

echo \"Success: All assets verified.\"
exit 0
