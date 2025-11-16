#!/usr/bin/env bash
# Backwards-compatible entry point for Android UI testing.
# Delegates to run-android-instrumentation-tests.sh, which now handles
# screenshots and coverage.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/run-android-instrumentation-tests.sh" "$@"
