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

# Match build-ios-app.sh. The generated project may depend on the simulator
# platforms installed into Xcode 26; using the runner default Xcode here can
# make xcodebuild see only tvOS destinations for an iOS test scheme.
if [ -z "${XCODE_APP:-}" ]; then
  XCODE_APP="$(ls -d /Applications/Xcode_26*.app 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [ ! -x "${XCODE_APP:-}/Contents/Developer/usr/bin/xcodebuild" ]; then
  ri_log "Xcode 26 not found. Set XCODE_APP to an installed Xcode 26 app bundle path." >&2
  exit 1
fi
export DEVELOPER_DIR="$XCODE_APP/Contents/Developer"
export XCODEBUILD="$DEVELOPER_DIR/usr/bin/xcodebuild"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"
ri_log "Using DEVELOPER_DIR=$DEVELOPER_DIR"
ri_log "Using XCODEBUILD=$XCODEBUILD"

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

# Ensure the active Xcode actually has the iOS Simulator platform before we try
# to build/run. Some GitHub runners select an Xcode that only has the tvOS
# platform installed; -showdestinations then lists no iOS destination, and even
# a concrete simctl device (which may belong to a *different* Xcode's runtime)
# fails the build with "Unable to find a destination matching ...
# { platform:iOS Simulator }". Download the iOS platform for the active Xcode
# when it is absent. Mirrors run-ios-ui-tests.sh.
DOWNLOAD_PLATFORMS="${XCODE_DOWNLOAD_PLATFORMS:-}"
if [ -z "$DOWNLOAD_PLATFORMS" ] && [ "${GITHUB_ACTIONS:-false}" = "true" ]; then
  DOWNLOAD_PLATFORMS="true"
fi
DOWNLOAD_PLATFORMS="${DOWNLOAD_PLATFORMS:-false}"
if ! xcodebuild "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH" -scheme "$TEST_SCHEME" -showdestinations 2>/dev/null \
     | grep -q "platform:iOS Simulator"; then
  if [ "$DOWNLOAD_PLATFORMS" = "true" ]; then
    ri_log "No iOS simulator platform for the active Xcode; downloading via xcodebuild -downloadPlatform iOS"
    # On some runner instances the CoreSimulator service is wedged and the
    # download fails with "Unable to connect to simulator", leaving the job
    # to fail minutes later with the misleading "Unable to find a
    # destination". Detect that, restart the service and retry once.
    DOWNLOAD_OUT="$(xcodebuild -downloadPlatform iOS 2>&1)" || true
    printf '%s\n' "$DOWNLOAD_OUT"
    if printf '%s' "$DOWNLOAD_OUT" | grep -qi "Unable to connect to simulator"; then
      ri_log "CoreSimulator not responding; restarting the service and retrying the platform download"
      killall -9 com.apple.CoreSimulator.CoreSimulatorService 2>/dev/null || true
      sleep 5
      xcrun simctl list runtimes >/dev/null 2>&1 || true
      xcodebuild -downloadPlatform iOS || true
    fi
  else
    ri_log "No iOS simulator platform for the active Xcode. Set XCODE_DOWNLOAD_PLATFORMS=true to attempt auto-download."
  fi
fi

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
  # Prefer a device on the NEWEST iOS runtime: a simctl-available device from
  # an older Xcode's runtime is still rejected by xcodebuild destination
  # matching ("Unable to find a destination matching ...").
  EXISTING_ID="$(xcrun simctl list -j devices available 2>/dev/null \
    | python3 -c 'import json,sys,re
data=json.load(sys.stdin)
best=None
for runtime, devs in data.get("devices", {}).items():
    if "iOS" not in runtime:
        continue
    ver=[int(x) for x in re.findall(r"\d+", runtime)]
    for d in devs:
        if d.get("isAvailable") and "iPhone" in d.get("name",""):
            if best is None or ver > best[0]:
                best=(ver, d["udid"])
if best:
    print(best[1])' 2>/dev/null || true)"
  if [ -n "$EXISTING_ID" ]; then
    ri_log "Reusing existing iPhone simulator $EXISTING_ID"
    DESTINATION="platform=iOS Simulator,id=$EXISTING_ID"
  else
    # Pick the newest iOS runtime AND a device type it actually supports (from
    # supportedDeviceTypes). A naive alphabetical pick selects "iPhone Xs Max"
    # ("X" > "1"), too old for a current runtime, so `simctl create` fails with
    # "Unable to create a device ...". Emit "<runtime>|<deviceType>".
    RT_DT="$(xcrun simctl list -j runtimes available 2>/dev/null \
      | python3 -c '
import json,sys,re
data=json.load(sys.stdin)
rs=[r for r in data.get("runtimes",[]) if r.get("isAvailable") and r.get("identifier","").startswith("com.apple.CoreSimulator.SimRuntime.iOS-")]
rs.sort(key=lambda r: [int(x) for x in re.findall(r"\d+", r.get("version","0"))][:3], reverse=True)
if not rs:
    sys.exit(0)
rt=rs[0]
dts=[d for d in rt.get("supportedDeviceTypes",[]) if "iPhone" in d.get("name","")]
def num(d):
    m=re.search(r"iPhone (\d+)", d.get("name",""))
    return int(m.group(1)) if m else -1
dts.sort(key=num, reverse=True)
if dts:
    print(rt["identifier"]+"|"+dts[0]["identifier"])
' 2>/dev/null || true)"
    LATEST_RUNTIME="${RT_DT%%|*}"
    LATEST_DEVICE_TYPE="${RT_DT##*|}"
    if [ -n "$RT_DT" ] && [ -n "$LATEST_RUNTIME" ] && [ -n "$LATEST_DEVICE_TYPE" ]; then
      ri_log "Creating throwaway simulator (device=$LATEST_DEVICE_TYPE runtime=$LATEST_RUNTIME)"
      NEW_ID="$(xcrun simctl create "cn1-native-tests" "$LATEST_DEVICE_TYPE" "$LATEST_RUNTIME" 2>/dev/null || true)"
      # Only accept a real UDID (hex + dashes) so a failed create's error text
      # on stdout is never used as the device id.
      if [[ "$NEW_ID" =~ ^[0-9A-Fa-f-]+$ ]]; then
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

# Reuse a shared derived-data dir when one is supplied (CN1_IOS_DERIVED_DATA): the UI-smoke
# step in the same job already compiled the app + core into it, so this test build reuses those
# object files instead of recompiling the whole translated core a second time. Unset = default
# per-run derived data (previous behavior), so other pipelines are unaffected.
DERIVED_ARGS=()
if [ -n "${CN1_IOS_DERIVED_DATA:-}" ]; then
  mkdir -p "$CN1_IOS_DERIVED_DATA"
  DERIVED_ARGS=(-derivedDataPath "$CN1_IOS_DERIVED_DATA")
  ri_log "Reusing shared derived data at $CN1_IOS_DERIVED_DATA"
fi
# Match the smoke step's optimization level so the shared derived-data objects are reused rather
# than treated as stale (xcodebuild rebuilds a TU when its build settings change).
if [ -n "${CN1_TEST_OPT_LEVEL:-}" ]; then
  DERIVED_ARGS+=("GCC_OPTIMIZATION_LEVEL=$CN1_TEST_OPT_LEVEL")
fi
ri_log "Running xcodebuild test (scheme=$TEST_SCHEME, destination=$DESTINATION)"
set +e
xcodebuild \
  "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH" \
  -scheme "$TEST_SCHEME" \
  -destination "$DESTINATION" \
  ${DERIVED_ARGS[@]+"${DERIVED_ARGS[@]}"} \
  test | tee "$TEST_LOG"
RC=${PIPESTATUS[0]}
set -e

ri_log "xcodebuild test exit code: $RC"
exit "$RC"
