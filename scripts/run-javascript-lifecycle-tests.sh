#!/usr/bin/env bash
#
# Convenience wrapper for run-javascript-lifecycle-tests.mjs. Builds
# the HelloCodenameOne and Initializr JavaScript-port bundles if
# they're missing, then drives them through the playwright-based
# lifecycle test.
#
# Usage:
#   scripts/run-javascript-lifecycle-tests.sh [extra-bundle.zip ...]
#
# Environment:
#   CN1_LIFECYCLE_TIMEOUT_SECONDS  per-bundle timeout (default 90)
#   CN1_LIFECYCLE_REPORT_DIR       artifacts directory
#   CN1_LIFECYCLE_SKIP_BUILD       skip the mvn build step (1=skip)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

HELLO_BUNDLE="$REPO_ROOT/scripts/hellocodenameone/parparvm/target/hellocodenameone-javascript-port.zip"
INIT_BUNDLE="$REPO_ROOT/scripts/initializr/javascript/target/initializr-javascript-port.zip"

build_if_missing() {
  local bundle="$1"
  local module_dir="$2"
  if [ -f "$bundle" ]; then
    return 0
  fi
  if [ "${CN1_LIFECYCLE_SKIP_BUILD:-0}" = "1" ]; then
    echo "[lifecycle] $bundle missing and CN1_LIFECYCLE_SKIP_BUILD=1; skipping build" >&2
    return 1
  fi
  echo "[lifecycle] building bundle in $module_dir" >&2
  ( cd "$module_dir" && mvn -B -DskipTests package -Pjavascript-build ) >&2
}

bundles=()
if build_if_missing "$HELLO_BUNDLE" "$REPO_ROOT/scripts/hellocodenameone/parparvm"; then
  bundles+=( "$HELLO_BUNDLE" )
fi
if build_if_missing "$INIT_BUNDLE" "$REPO_ROOT/scripts/initializr/javascript"; then
  bundles+=( "$INIT_BUNDLE" )
fi
# Allow callers to add ad-hoc bundles after the defaults.
bundles+=( "$@" )

if [ ${#bundles[@]} -eq 0 ]; then
  echo "[lifecycle] no bundles available — set CN1_LIFECYCLE_SKIP_BUILD=0 or pass paths explicitly" >&2
  exit 2
fi

exec node "$SCRIPT_DIR/run-javascript-lifecycle-tests.mjs" "${bundles[@]}"
