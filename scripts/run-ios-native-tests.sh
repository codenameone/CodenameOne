#!/usr/bin/env bash
# Run native XCTest unit tests for generated Codename One iOS project.
# This script:
# 1) installs in-repo native test sources into the generated Xcode project,
# 2) auto-selects an available simulator destination,
# 3) executes `xcodebuild test` using the standard Xcode test runner.

set -euo pipefail

ri_log() { echo "[run-ios-native-tests] $1"; }

if [ $# -lt 1 ]; then
  ri_log "Usage: $0 <workspace_path> [app_scheme] [test_scheme]" >&2
  exit 2
fi

WORKSPACE_PATH="$1"
APP_SCHEME="${2:-}"
TEST_SCHEME="${3:-}"

if [ ! -d "$WORKSPACE_PATH" ]; then
  ri_log "Workspace not found at $WORKSPACE_PATH" >&2
  exit 3
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

if [ -z "$APP_SCHEME" ]; then
  APP_SCHEME="$(basename "$WORKSPACE_PATH" .xcworkspace)"
fi
if [ -z "$TEST_SCHEME" ]; then
  TEST_SCHEME="${APP_SCHEME}Tests"
fi

PROJECT_DIR="$(cd "$(dirname "$WORKSPACE_PATH")" && pwd)"

ri_log "Injecting native notification tests into project at $PROJECT_DIR"
"$REPO_ROOT/scripts/ios/notification-tests/install-native-notification-tests.sh" "$PROJECT_DIR"

ri_log "Discovering simulator destination for test scheme $TEST_SCHEME"
DESTINATION="$(xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$TEST_SCHEME" -showdestinations 2>/dev/null \
  | sed -n 's/.*{ platform:iOS Simulator,.*id:\([^,}]*\).*/\1/p' \
  | rg -v "placeholder" \
  | head -n 1 \
  | sed 's#^#platform=iOS Simulator,id=#' || true)"
if [ -z "$DESTINATION" ]; then
  DESTINATION="platform=iOS Simulator,name=iPhone 16"
fi

SIMULATOR_ID="$(printf "%s" "$DESTINATION" | sed -n 's/.*id=\([^,]*\).*/\1/p')"
BUNDLE_ID="$(xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$APP_SCHEME" -showBuildSettings 2>/dev/null \
  | sed -n 's/^[[:space:]]*PRODUCT_BUNDLE_IDENTIFIER = //p' \
  | head -n 1 || true)"

if [ -n "$SIMULATOR_ID" ]; then
  ri_log "Booting simulator $SIMULATOR_ID"
  xcrun simctl boot "$SIMULATOR_ID" >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$SIMULATOR_ID" -b >/dev/null 2>&1 || true
  if [ -n "$BUNDLE_ID" ]; then
    ri_log "Granting notifications permission to $BUNDLE_ID on simulator"
    xcrun simctl privacy "$SIMULATOR_ID" grant notifications "$BUNDLE_ID" >/dev/null 2>&1 || true
  fi
fi

ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts}"
mkdir -p "$ARTIFACTS_DIR"
TEST_LOG="$ARTIFACTS_DIR/xcode-native-tests.log"

ri_log "Running xcodebuild test (scheme=$TEST_SCHEME, destination=$DESTINATION)"
set +e
xcodebuild \
  -workspace "$WORKSPACE_PATH" \
  -scheme "$TEST_SCHEME" \
  -destination "$DESTINATION" \
  test | tee "$TEST_LOG"
RC=${PIPESTATUS[0]}
set -e

ri_log "xcodebuild test exit code: $RC"
exit "$RC"
