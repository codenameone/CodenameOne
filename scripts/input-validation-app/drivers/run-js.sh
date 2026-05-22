#!/usr/bin/env bash
# Drive the CN1 input-validation app in headless Chromium against a URL
# serving the built JavaScript port. Thin wrapper around playwright-driver.mjs.
#
# Usage:
#   run-js.sh <url-of-deployed-js-build>
#
# Assumes Playwright (with Chromium) is installed -- the parent Maven build
# leaves a node_modules/ alongside the JS port output that has it; CI installs
# globally.
set -euo pipefail

iv_log() { echo "[run-js] $1"; }

if [ $# -lt 1 ]; then
  iv_log "Usage: $0 <url-of-deployed-js-build>" >&2
  exit 2
fi

URL="$1"

if ! command -v node >/dev/null 2>&1; then
  iv_log "node not on PATH" >&2
  exit 3
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$APP_DIR}/artifacts/input-validation-js}"
mkdir -p "$ARTIFACTS_DIR"

export CN1IV_URL="$URL"
export CN1IV_ARTIFACTS_DIR="$ARTIFACTS_DIR"

iv_log "Driving $URL"
node "$SCRIPT_DIR/playwright-driver.mjs"
