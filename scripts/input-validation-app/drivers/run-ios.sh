#!/usr/bin/env bash
# Drive the CN1 input-validation app through tap / drag / long-press on an
# iOS simulator and assert the expected CN1IV:EVENT log lines appear.
#
# Usage:
#   run-ios.sh <path-to-cn1-built.app>
#
# The .app bundle is produced by `mvn -P ios package` against the parent POM
# in scripts/input-validation-app and the cn1-builder build server (or local
# iOS build chain). This script is intentionally lean -- no screenshot
# decoding, no chunked Base64, no comparison report. The only thing it cares
# about is whether the OS-level taps reached Component listeners.
set -euo pipefail

iv_log() { echo "[run-ios] $1"; }

if [ $# -lt 1 ]; then
  iv_log "Usage: $0 <path-to-cn1-built.app>" >&2
  exit 2
fi

APP_BUNDLE="$1"
if [ ! -d "$APP_BUNDLE" ]; then
  iv_log "App bundle not found: $APP_BUNDLE" >&2
  exit 3
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TESTS_DIR="$APP_DIR/ios-tests"
ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$APP_DIR}/artifacts/input-validation-ios}"
mkdir -p "$ARTIFACTS_DIR"
LOG_FILE="$ARTIFACTS_DIR/device.log"
XCODEBUILD_LOG="$ARTIFACTS_DIR/xcodebuild-test.log"

if ! command -v xcrun >/dev/null 2>&1; then iv_log "xcrun not on PATH" >&2; exit 3; fi
if ! command -v xcodebuild >/dev/null 2>&1; then iv_log "xcodebuild not on PATH" >&2; exit 3; fi
if ! command -v xcodegen >/dev/null 2>&1; then
  iv_log "xcodegen not on PATH. Install with: brew install xcodegen" >&2
  exit 3
fi

# Read the app's actual bundle identifier from its Info.plist so we don't
# guess wrong if the CN1 generator changes its default.
BUNDLE_ID="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$APP_BUNDLE/Info.plist" 2>/dev/null || true)"
if [ -z "$BUNDLE_ID" ]; then
  iv_log "Could not read CFBundleIdentifier from $APP_BUNDLE/Info.plist" >&2
  exit 3
fi
iv_log "Bundle id: $BUNDLE_ID"

DEVICE_NAME="${CN1IV_DEVICE_NAME:-}"
DEVICE_RUNTIME="${CN1IV_DEVICE_RUNTIME:-}"

# Build a sorted (name, runtime, udid) list of available simulators. Newer
# iOS runtimes sort last so we pick them by default.
read_devices() {
  xcrun simctl list devices available -j \
    | python3 -c '
import json, sys
data = json.load(sys.stdin)
rows = []
for runtime, devs in data.get("devices", {}).items():
    if "iOS-" not in runtime:
        continue
    for d in devs:
        if d.get("isAvailable"):
            rows.append((runtime, d["name"], d["udid"]))
rows.sort()
for r in rows:
    print("\t".join(r))
'
}

SIM_UDID=""
if [ -n "$DEVICE_NAME" ]; then
  iv_log "Locating simulator by name: $DEVICE_NAME"
  while IFS=$'\t' read -r runtime name udid; do
    if [ "$name" = "$DEVICE_NAME" ] && { [ -z "$DEVICE_RUNTIME" ] || [ "$runtime" = "$DEVICE_RUNTIME" ]; }; then
      SIM_UDID="$udid"
      break
    fi
  done < <(read_devices)
fi

if [ -z "$SIM_UDID" ]; then
  # Fall back to the newest available iPhone (any model). XCode 16.4 only has
  # iPhone 16; XCode 26 has iPhone 17. We'd rather adapt than fail-fast on a
  # CI runner that doesn't have the exact device name we'd prefer.
  iv_log "No exact device match -- picking the newest available iPhone"
  while IFS=$'\t' read -r runtime name udid; do
    case "$name" in
      iPhone*) SIM_UDID="$udid"; DEVICE_NAME="$name"; DEVICE_RUNTIME="$runtime" ;;
    esac
  done < <(read_devices)
fi

if [ -z "$SIM_UDID" ]; then
  iv_log "No iOS simulator available on this host" >&2
  xcrun simctl list devices available >&2 || true
  exit 3
fi
iv_log "Selected simulator: $DEVICE_NAME ($DEVICE_RUNTIME)"
iv_log "Using simulator $SIM_UDID"

# Boot the simulator if needed. `bootstatus -b` blocks until SpringBoard is up.
xcrun simctl boot "$SIM_UDID" >/dev/null 2>&1 || true
xcrun simctl bootstatus "$SIM_UDID" -b

# Install the app fresh -- uninstall first so a stale bundle doesn't shadow the
# new one when the bundle identifier collides.
xcrun simctl uninstall "$SIM_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true
iv_log "Installing $APP_BUNDLE"
xcrun simctl install "$SIM_UDID" "$APP_BUNDLE"

# Start streaming os_log lines that came from the CN1 process (printf -> NSLog
# on the iOS port routes through unified logging). Capture in the background;
# we'll wait on the file after the XCUITest run.
iv_log "Starting log stream -> $LOG_FILE"
: > "$LOG_FILE"
xcrun simctl spawn "$SIM_UDID" log stream \
    --style compact --level debug \
    --predicate '(processImagePath CONTAINS[c] "'"$BUNDLE_ID"'") OR (eventMessage CONTAINS "CN1IV:")' \
    > "$LOG_FILE" 2>&1 &
LOG_PID=$!
cleanup() {
  kill "$LOG_PID" 2>/dev/null || true
  wait "$LOG_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

# Generate the XCUITest Xcode project on demand. We don't check in pbxproj.
iv_log "Generating XCUITest project via xcodegen"
( cd "$TESTS_DIR" && xcodegen generate >> "$XCODEBUILD_LOG" 2>&1 )

# Run the XCUITest suite. CN1IV_BUNDLE_ID tells the Swift code which app to
# attach to; CN1IV_STEP_DELAY_SEC lets us slow the inter-gesture wait on
# heavily loaded CI runners. `-resultBundlePath` captures the .xcresult so
# we can extract the actual test failure reason post-hoc (without it,
# xcodebuild just prints `** TEST FAILED **`).
XCRESULT_BUNDLE="$ARTIFACTS_DIR/test.xcresult"
rm -rf "$XCRESULT_BUNDLE"
iv_log "Running XCUITest"
set +e
xcodebuild test \
  -project "$TESTS_DIR/CN1InputValidationUITests.xcodeproj" \
  -scheme CN1InputValidationUITests \
  -destination "platform=iOS Simulator,id=$SIM_UDID" \
  -resultBundlePath "$XCRESULT_BUNDLE" \
  CN1IV_BUNDLE_ID="$BUNDLE_ID" \
  CODE_SIGNING_ALLOWED=NO \
  | tee -a "$XCODEBUILD_LOG"
XCB_RC=${PIPESTATUS[0]}
set -e
iv_log "xcodebuild test exit=$XCB_RC"

# Extract the human-readable failure summary if the result bundle is present
# so the artifact upload has something searchable beyond the opaque
# "** TEST FAILED **" line in xcodebuild-test.log.
if [ -d "$XCRESULT_BUNDLE" ]; then
  iv_log "Extracting xcresult diagnostics"
  xcrun xcresulttool get test-results summary --path "$XCRESULT_BUNDLE" --format json \
    > "$ARTIFACTS_DIR/xcresult-summary.json" 2>/dev/null || true
  xcrun xcresulttool get log --type action --path "$XCRESULT_BUNDLE" \
    > "$ARTIFACTS_DIR/xcresult-action.log" 2>/dev/null || true
fi

# Give the log stream a beat to flush the final CN1IV:SUITE:FINISHED line.
sleep 2
cleanup
trap - EXIT INT TERM

# Assertion: each expected event must appear at least once in the log.
REQUIRED_EVENTS=(
  "CN1IV:READY:tap"
  "CN1IV:EVENT:tap"
  "CN1IV:READY:drag"
  "CN1IV:EVENT:drag"
  "CN1IV:READY:longpress"
  "CN1IV:EVENT:longpress"
  "CN1IV:SUITE:FINISHED"
)
FAILED=0
for needle in "${REQUIRED_EVENTS[@]}"; do
  if grep -q "$needle" "$LOG_FILE"; then
    iv_log "OK  $needle"
  else
    iv_log "MISS $needle"
    FAILED=1
  fi
done

if grep -qE 'CN1IV:TIMEOUT:' "$LOG_FILE"; then
  iv_log "Gesture timeouts detected in device log:"
  grep -E 'CN1IV:TIMEOUT:' "$LOG_FILE" | sed 's/^/  /'
  FAILED=1
fi

if [ "$XCB_RC" -ne 0 ]; then
  iv_log "xcodebuild test failed (rc=$XCB_RC) -- see $XCODEBUILD_LOG"
  FAILED=1
fi

if [ "$FAILED" -ne 0 ]; then
  iv_log "Input-validation suite FAILED -- see $LOG_FILE"
  exit 1
fi

iv_log "Input-validation suite PASSED"
