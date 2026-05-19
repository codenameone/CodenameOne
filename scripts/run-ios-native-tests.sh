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
  ri_log "Xcode workspace/project not found at $WORKSPACE_PATH" >&2
  exit 3
fi

XCODE_CONTAINER_FLAG="-workspace"
if [[ "$WORKSPACE_PATH" == *.xcodeproj ]]; then
  XCODE_CONTAINER_FLAG="-project"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

if [ -z "$APP_SCHEME" ]; then
  if [[ "$WORKSPACE_PATH" == *.xcworkspace ]]; then
    APP_SCHEME="$(basename "$WORKSPACE_PATH" .xcworkspace)"
  else
    APP_SCHEME="$(basename "$WORKSPACE_PATH" .xcodeproj)"
  fi
fi
if [ -z "$TEST_SCHEME" ]; then
  TEST_SCHEME="${APP_SCHEME}Tests"
fi

PROJECT_DIR="$(cd "$(dirname "$WORKSPACE_PATH")" && pwd)"

ri_log "Injecting native notification tests into project at $PROJECT_DIR"
"$REPO_ROOT/scripts/ios/notification-tests/install-native-notification-tests.sh" "$PROJECT_DIR"
"$REPO_ROOT/scripts/ios/create-shared-scheme.py" "$PROJECT_DIR" "$APP_SCHEME"
"$REPO_ROOT/scripts/ios/create-shared-scheme.py" "$PROJECT_DIR" "$TEST_SCHEME"

ri_log "Discovering simulator destination for test scheme $TEST_SCHEME"
DESTINATION="$(xcodebuild "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH" -scheme "$TEST_SCHEME" -showdestinations 2>/dev/null \
  | sed -n 's/.*{ platform:iOS Simulator,.*id:\([^,}]*\).*/\1/p' \
  | grep -v "placeholder" \
  | head -n 1 \
  | sed 's#^#platform=iOS Simulator,id=#' || true)"

# `xcodebuild -showdestinations` on GitHub Actions macOS runners sometimes
# only lists the "Any iOS Simulator Device" placeholder when no simulator has
# been created yet for the current Xcode. Fall back to creating a runtime
# device from the latest available iOS runtime + an iPhone device type so
# we don't fail with "Unable to find a device" before tests can even run.
if [ -z "$DESTINATION" ]; then
  ri_log "No concrete iOS Simulator destination from -showdestinations; querying simctl"
  EXISTING_ID="$(xcrun simctl list -j devices available 2>/dev/null \
    | python3 -c 'import json,sys
data=json.load(sys.stdin)
for runtime, devs in data.get("devices", {}).items():
    if "iOS" not in runtime:
        continue
    for d in devs:
        if d.get("isAvailable") and "iPhone" in d.get("name",""):
            print(d["udid"]); sys.exit(0)' 2>/dev/null || true)"
  if [ -n "$EXISTING_ID" ]; then
    ri_log "Reusing existing iPhone simulator $EXISTING_ID"
    DESTINATION="platform=iOS Simulator,id=$EXISTING_ID"
  else
    LATEST_RUNTIME="$(xcrun simctl list -j runtimes available 2>/dev/null \
      | python3 -c 'import json,sys
runtimes=[r for r in json.load(sys.stdin).get("runtimes",[]) if r.get("isAvailable") and r.get("identifier","").startswith("com.apple.CoreSimulator.SimRuntime.iOS-")]
runtimes.sort(key=lambda r: r.get("version",""), reverse=True)
print(runtimes[0]["identifier"] if runtimes else "")' 2>/dev/null || true)"
    LATEST_DEVICE_TYPE="$(xcrun simctl list -j devicetypes 2>/dev/null \
      | python3 -c 'import json,sys
types=[t["identifier"] for t in json.load(sys.stdin).get("devicetypes",[]) if "iPhone" in t.get("name","")]
types.sort(reverse=True)
print(types[0] if types else "")' 2>/dev/null || true)"
    if [ -n "$LATEST_RUNTIME" ] && [ -n "$LATEST_DEVICE_TYPE" ]; then
      ri_log "Creating throwaway simulator (device=$LATEST_DEVICE_TYPE runtime=$LATEST_RUNTIME)"
      NEW_ID="$(xcrun simctl create "cn1-native-tests" "$LATEST_DEVICE_TYPE" "$LATEST_RUNTIME" 2>/dev/null || true)"
      if [ -n "$NEW_ID" ]; then
        DESTINATION="platform=iOS Simulator,id=$NEW_ID"
      fi
    fi
  fi
fi

if [ -z "$DESTINATION" ]; then
  ri_log "Falling back to name-based destination (will fail if no iPhone 16 is installed)"
  DESTINATION="platform=iOS Simulator,name=iPhone 16"
fi

SIMULATOR_ID="$(printf "%s" "$DESTINATION" | sed -n 's/.*id=\([^,]*\).*/\1/p')"
BUNDLE_ID="$(xcodebuild "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH" -scheme "$APP_SCHEME" -showBuildSettings 2>/dev/null \
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
  "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH" \
  -scheme "$TEST_SCHEME" \
  -destination "$DESTINATION" \
  test | tee "$TEST_LOG"
RC=${PIPESTATUS[0]}
set -e

ri_log "xcodebuild test exit code: $RC"
exit "$RC"
