#!/bin/bash
# Measure successful fresh-process jobs and verify every output before reporting.
set -euo pipefail
exec python3 "$(dirname "$0")/bench-selfhost.py" "$@"
