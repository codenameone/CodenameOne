#!/usr/bin/env bash
# Run the Codename One UI screenshot suite on the watchOS simulator and compare
# the captured frames to the watch golden set (scripts/ios/screenshots-watch).
#
# The watch app is SwiftUI-rooted and renders Codename One through the Core
# Graphics backend (no GL/Metal on watchOS). It streams each screenshot to the
# host-side Cn1ssScreenshotServer over ws://127.0.0.1:8765 -- the same transport
# the iOS jobs use -- so the comparison/report tooling in scripts/lib/cn1ss.sh
# is reused verbatim; only the build (watch target) and the simulator (a watch
# device booted directly via simctl, since watchOS apps aren't launched through
# an xcodebuild test action) differ.
#
# Usage: run-watch-ui-tests.sh <workspace_or_project> [scheme]
set -euo pipefail

rw_log() { printf '%s %s\n' "[run-watch-ui-tests]" "$*" >&2; }

WORKSPACE_PATH="${1:?Usage: $0 <workspace_or_project> [scheme]}"
SCHEME="${2:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/common/java"
source "$SCRIPT_DIR/lib/cn1ss.sh"

: "${ARTIFACTS_DIR:=$REPO_ROOT/artifacts/watch-ui-tests}"
mkdir -p "$ARTIFACTS_DIR"

# --- Xcode / project resolution -------------------------------------------
# Prefer Xcode 26 (watchOS 26 SDK + arm64 watch simulator), matching
# build-ios-app.sh. The runner's default xcodebuild is often an older Xcode
# (16.x) whose watchOS SDK can't build the slice.
if [ -z "${XCODE_APP:-}" ]; then
  XCODE_APP="$(ls -d /Applications/Xcode_26*.app 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [ ! -x "${XCODE_APP:-}/Contents/Developer/usr/bin/xcodebuild" ]; then
  XCODE_APP="/Applications/Xcode.app"
fi
export DEVELOPER_DIR="${DEVELOPER_DIR:-$XCODE_APP/Contents/Developer}"
rw_log "Using DEVELOPER_DIR=$DEVELOPER_DIR"
if ! command -v xcodebuild >/dev/null 2>&1; then
  rw_log "xcodebuild not found (DEVELOPER_DIR=$DEVELOPER_DIR)"; exit 3
fi

# The watch target builds via -project (its target carries no CocoaPods deps).
# Resolve the .xcodeproj whether we were handed a workspace or a project.
if [[ "$WORKSPACE_PATH" == *.xcodeproj ]]; then
  PROJECT_PATH="$WORKSPACE_PATH"
else
  PROJECT_DIR="$(dirname "$WORKSPACE_PATH")"
  base="$(basename "$WORKSPACE_PATH" .xcworkspace)"
  PROJECT_PATH="$PROJECT_DIR/$base.xcodeproj"
fi
if [ ! -d "$PROJECT_PATH" ]; then
  rw_log "Could not resolve .xcodeproj from '$WORKSPACE_PATH' (looked for $PROJECT_PATH)"; exit 3
fi
[ -z "$SCHEME" ] && SCHEME="$(basename "$PROJECT_PATH" .xcodeproj)"
WATCH_TARGET="${SCHEME}Watch"
rw_log "Project=$PROJECT_PATH watchTarget=$WATCH_TARGET"

# --- Pick a watch simulator -------------------------------------------------
# Screenshots are pixel-compared, so the device screen size must match the one
# the goldens were captured on: a 46mm Apple Watch (416x496 px). Prefer a 46mm
# model for determinism; fall back to any Apple Watch (override with
# CN1SS_WATCH_UDID / CN1SS_WATCH_MODEL). If only a non-46mm device exists the
# comparison will flag size mismatches rather than silently drifting.
WATCH_MODEL_PREF="${CN1SS_WATCH_MODEL:-46mm}"
WATCH_UDID="${CN1SS_WATCH_UDID:-}"
DEVLIST="$(xcrun simctl list devices available 2>/dev/null | grep -iE 'Apple Watch')"
if [ -z "$WATCH_UDID" ]; then
  WATCH_UDID="$(printf '%s\n' "$DEVLIST" | grep -i "$WATCH_MODEL_PREF" | grep -oE '\([0-9A-F-]{36}\)' | head -1 | tr -d '()')"
fi
if [ -z "$WATCH_UDID" ]; then
  rw_log "No '$WATCH_MODEL_PREF' Apple Watch found; falling back to any available Apple Watch"
  WATCH_UDID="$(printf '%s\n' "$DEVLIST" | grep -oE '\([0-9A-F-]{36}\)' | head -1 | tr -d '()')"
fi
if [ -z "$WATCH_UDID" ]; then
  # Xcode 26 on some macos-15 runner images ships without the watchOS Simulator
  # runtime (the same lean-image issue run-ios-ui-tests.sh handles for iOS).
  # Install it on demand and create a matching-model Apple Watch device (so the
  # screen size matches the golden) so the run has a target. Gated on CI /
  # XCODE_DOWNLOAD_PLATFORMS; never runs when a device is already present.
  WATCH_DL="${XCODE_DOWNLOAD_PLATFORMS:-}"
  if [ -z "$WATCH_DL" ] && [ "${GITHUB_ACTIONS:-false}" = "true" ]; then WATCH_DL="true"; fi
  if [ "${WATCH_DL:-false}" = "true" ]; then
    rw_log "No Apple Watch simulator; installing watchOS platform via xcodebuild -downloadPlatform watchOS"
    xcodebuild -downloadPlatform watchOS || true
    WATCH_RT="$(xcrun simctl list -j runtimes available 2>/dev/null | python3 -c 'import json,sys
rs=[r for r in json.load(sys.stdin).get("runtimes",[]) if r.get("isAvailable") and r.get("identifier","").startswith("com.apple.CoreSimulator.SimRuntime.watchOS-")]
rs.sort(key=lambda r:r.get("version",""),reverse=True)
print(rs[0]["identifier"] if rs else "")' 2>/dev/null || true)"
    WATCH_DT="$(xcrun simctl list -j devicetypes 2>/dev/null | WMP="$WATCH_MODEL_PREF" python3 -c 'import json,sys,os
pref=os.environ.get("WMP","")
ts=[t["identifier"] for t in json.load(sys.stdin).get("devicetypes",[]) if "Apple Watch" in t.get("name","") and pref in t.get("name","")]
print(ts[0] if ts else "")' 2>/dev/null || true)"
    if [ -n "$WATCH_RT" ] && [ -n "$WATCH_DT" ]; then
      rw_log "Creating Apple Watch simulator ($WATCH_DT on $WATCH_RT)"
      xcrun simctl create "cn1-watch-tests" "$WATCH_DT" "$WATCH_RT" >/dev/null 2>&1 || true
      DEVLIST="$(xcrun simctl list devices available 2>/dev/null | grep -iE 'Apple Watch')"
      WATCH_UDID="$(printf '%s\n' "$DEVLIST" | grep -i "$WATCH_MODEL_PREF" | grep -oE '\([0-9A-F-]{36}\)' | head -1 | tr -d '()')"
      [ -z "$WATCH_UDID" ] && WATCH_UDID="$(printf '%s\n' "$DEVLIST" | grep -oE '\([0-9A-F-]{36}\)' | head -1 | tr -d '()')"
    fi
  fi
fi
if [ -z "$WATCH_UDID" ]; then
  rw_log "No Apple Watch simulator available. Install a watchOS runtime in Xcode."; exit 4
fi
rw_log "Watch simulators available:"; printf '%s\n' "$DEVLIST" | sed 's/^/  /' >&2
rw_log "Using watch simulator $WATCH_UDID (pref '$WATCH_MODEL_PREF')"
xcrun simctl boot "$WATCH_UDID" 2>/dev/null || true
xcrun simctl bootstatus "$WATCH_UDID" -b 2>/dev/null || true

# --- Build the watch target -------------------------------------------------
BUILD_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/cn1-watch-build-XXXXXX")"
rw_log "Building $WATCH_TARGET for watchsimulator -> $BUILD_ROOT"
xcodebuild -project "$PROJECT_PATH" -target "$WATCH_TARGET" \
  -sdk watchsimulator -configuration Debug -arch arm64 \
  ONLY_ACTIVE_ARCH=YES CODE_SIGNING_ALLOWED=NO SYMROOT="$BUILD_ROOT" build \
  > "$ARTIFACTS_DIR/watch-build.log" 2>&1 || {
    rw_log "Watch build FAILED (see $ARTIFACTS_DIR/watch-build.log)"; tail -40 "$ARTIFACTS_DIR/watch-build.log" >&2; exit 5; }

APP_PATH="$(/usr/bin/find "$BUILD_ROOT" -name "${WATCH_TARGET}.app" -maxdepth 3 | head -1)"
[ -z "$APP_PATH" ] && { rw_log "Built watch .app not found under $BUILD_ROOT"; exit 5; }
BUNDLE_ID="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$APP_PATH/Info.plist" 2>/dev/null)"
[ -z "$BUNDLE_ID" ] && { rw_log "Could not read CFBundleIdentifier from $APP_PATH"; exit 5; }
rw_log "Built $APP_PATH (bundle $BUNDLE_ID)"

# --- Screenshot capture: host WS sink + the streaming watch app -------------
JAVA_BIN="${JAVA17_BIN:-$(command -v java)}"
cn1ss_setup "$JAVA_BIN" "$CN1SS_HELPER_SOURCE_DIR"

SS_TMP="$(mktemp -d "${TMPDIR:-/tmp}/cn1-watch-ss-XXXXXX")"
WS_RAW_DIR="$SS_TMP/ws"; PREVIEW_DIR="$SS_TMP/previews"
mkdir -p "$WS_RAW_DIR" "$PREVIEW_DIR"

APP_CONSOLE_PID=""
cleanup() { cn1ss_stop_ws_server 2>/dev/null || true; [ -n "$APP_CONSOLE_PID" ] && kill "$APP_CONSOLE_PID" 2>/dev/null || true; xcrun simctl terminate "$WATCH_UDID" "$BUNDLE_ID" 2>/dev/null || true; }
trap cleanup EXIT

cn1ss_start_ws_server "$WS_RAW_DIR" || { rw_log "Failed to start Cn1ssScreenshotServer"; exit 6; }
rw_log "WS sink on port ${CN1SS_WS_PORT:-8765} -> $WS_RAW_DIR"

xcrun simctl terminate "$WATCH_UDID" "$BUNDLE_ID" 2>/dev/null || true
WATCH_LOG_START="$(/bin/date -u '+%Y-%m-%d %H:%M:%S %z')"
xcrun simctl install "$WATCH_UDID" "$APP_PATH"
# Stream the app's os_log/NSLog output into the artifacts so suite-side failures
# (exceptions, stalled animations, font issues) are diagnosable from CI runs.
xcrun simctl spawn "$WATCH_UDID" log stream --style compact \
  --predicate 'processImagePath CONTAINS "'"$WATCH_TARGET"'"' \
  > "$ARTIFACTS_DIR/app-console.log" 2>&1 &
APP_CONSOLE_PID=$!
xcrun simctl launch --stdout="$ARTIFACTS_DIR/app-stdout.log" --stderr="$ARTIFACTS_DIR/app-stderr.log" \
  "$WATCH_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || { rw_log "launch failed"; exit 6; }
rw_log "Launched watch app; waiting for the suite to stream screenshots..."

# Wait until every expected screenshot has streamed (preferred), or the suite
# explicitly reports completion, capped by MAX_WAIT. Do not infer completion
# from screenshot inactivity: assertion and benchmark tests near the end of the
# suite can legitimately run for minutes without producing a PNG. An inactivity
# heuristic used to compare the partial set while the app was still running,
# producing false "missing" failures even though the final screenshot reached
# the WebSocket sink seconds later.
MAX_WAIT="${CN1SS_WATCH_TIMEOUT:-1200}"
WATCH_REF_DIR="${SCREENSHOT_REF_DIR:-$SCRIPT_DIR/ios/screenshots-watch}"
EXPECTED="$(/usr/bin/find "$WATCH_REF_DIR" -name '*.png' 2>/dev/null | wc -l | tr -d ' ')"
rw_log "Expecting $EXPECTED screenshots (golden set)"
stable=0; suite_finished_stable=0; waited=0
while [ "$waited" -lt "$MAX_WAIT" ]; do
  sleep 8; waited=$((waited+8))
  cur="$(/usr/bin/find "$WS_RAW_DIR" -name '*.png' 2>/dev/null | wc -l | tr -d ' ')"
  # Preferred exit: the full golden set has arrived. Confirm once more so the
  # final PNG writes flush to disk before we snapshot.
  if [ "$EXPECTED" -gt 0 ] && [ "$cur" -ge "$EXPECTED" ]; then
    stable=$((stable+1)); [ "$stable" -ge 2 ] && break
    continue
  fi
  stable=0

  # stdout/stderr are attached directly by simctl launch, so the DeviceRunner
  # completion marker is available here without waiting for unified-log
  # collection. Confirm it twice to give the ACK-paced WebSocket sink a final
  # drain window before snapshotting the directory. A genuinely missing
  # screenshot will then fail comparison; a slow screenshot cannot race it.
  if grep -qa "CN1SS:SUITE:FINISHED" \
      "$ARTIFACTS_DIR/app-stderr.log" "$ARTIFACTS_DIR/app-stdout.log" \
      "$ARTIFACTS_DIR/app-console.log" 2>/dev/null; then
    suite_finished_stable=$((suite_finished_stable+1))
    if [ "$suite_finished_stable" -ge 2 ]; then
      rw_log "Suite reported FINISHED; WebSocket drain confirmed after ${waited}s"
      break
    fi
  else
    suite_finished_stable=0
  fi

  if grep -qaE "Fatal|Terminating app due to uncaught exception|EXC_BAD|did crash|libsystem_kernel" \
      "$ARTIFACTS_DIR/app-stderr.log" "$ARTIFACTS_DIR/app-stdout.log" \
      "$ARTIFACTS_DIR/app-console.log" 2>/dev/null; then
    rw_log "Detected app crash/fatal after ${waited}s"
    break
  fi
done
rw_log "Capture settled: $(/usr/bin/find "$WS_RAW_DIR" -name '*.png' 2>/dev/null | wc -l | tr -d ' ') of $EXPECTED screenshots after ${waited}s"

WATCH_APP_LOG="$ARTIFACTS_DIR/watch-app-cn1ss.log"
xcrun simctl spawn "$WATCH_UDID" \
  log show --style syslog --start "$WATCH_LOG_START" \
  --predicate '(composedMessage CONTAINS "CN1SS") OR (eventMessage CONTAINS "CN1SS")' \
  > "$WATCH_APP_LOG" 2>/dev/null || true
SUITE_FAILURE_LINES="$(cn1ss_collect_suite_failures "$WATCH_APP_LOG")"
if [ -n "$SUITE_FAILURE_LINES" ]; then
  rw_log "Detected DeviceRunner assertion/test failure(s); artifacts and screenshot report will still be collected before failing."
fi

# --- Compare against the watch golden set + emit report ---------------------
REF_DIR="${SCREENSHOT_REF_DIR:-$SCRIPT_DIR/ios/screenshots-watch}"
REF_DIR="$(cd "$REF_DIR" && pwd)"
declare -a ACTUAL=()
while IFS= read -r png; do
  name="$(basename "$png" .png)"
  ACTUAL+=("$name=$png")
done < <(/usr/bin/find "$WS_RAW_DIR" -name '*.png' | sort)

cp -f "$WS_RAW_DIR"/*.png "$ARTIFACTS_DIR/" 2>/dev/null || true

COMPARE_JSON="$SS_TMP/compare.json"; SUMMARY_OUT="$SS_TMP/summary.txt"; COMMENT_OUT="$SS_TMP/comment.md"
export CN1SS_PORT_ID="${CN1SS_PORT_ID:-watchos}"
export CN1SS_SUITE_LOG="$ARTIFACTS_DIR/app-stderr.log"
export CN1SS_SUITE_LOG_2="$ARTIFACTS_DIR/app-stdout.log"
export CN1SS_SUITE_LOG_3="$WATCH_APP_LOG"
export CN1SS_BINARY_PATH="$APP_PATH"

# --- Gate (mirrors the iOS jobs, enforced centrally in scripts/lib/cn1ss.sh) -
# The watch form factor is intentionally very different from the phone and some
# tests are not meaningful on a watch, so the goldens need not be pretty -- but
# every test must RUN and the goldens must stay in SYNC, so any drift fails CI.
# cn1ss_process_and_report returns (only when CN1SS_FAIL_ON_MISMATCH=1):
#   15 - a screenshot differs from / errored against its golden
#   17 - fewer screenshots were produced than there are goldens (a test failed
#        to emit; the suite most likely hung or crashed partway).
# We additionally fail on "missing_expected" (a screenshot streamed that has no
# golden yet) so a new test cannot land without its golden.
export CN1SS_FAIL_ON_MISMATCH="${CN1SS_FAIL_ON_MISMATCH:-1}"
export CN1SS_ALLOWED_MISSING="${CN1SS_ALLOWED_MISSING:-0}"
set +e
CN1SS_SUCCESS_MESSAGE="${CN1SS_SUCCESS_MESSAGE:-Apple Watch (watchOS, Core Graphics) screenshots match the goldens.}" \
cn1ss_process_and_report \
  "Apple Watch (watchOS / Core Graphics)" \
  "$COMPARE_JSON" "$SUMMARY_OUT" "$COMMENT_OUT" \
  "$REF_DIR" "$PREVIEW_DIR" "$ARTIFACTS_DIR" \
  "${ACTUAL[@]}"
gate_rc=$?
set -e

cp -f "$COMPARE_JSON" "$SUMMARY_OUT" "$COMMENT_OUT" "$ARTIFACTS_DIR/" 2>/dev/null || true
cn1ss_post_pr_comment "$COMMENT_OUT" "$PREVIEW_DIR" || true

rc="$gate_rc"
# A streamed screenshot with no golden (missing_expected) keeps the goldens out
# of sync; the central gate treats it as "extra" and ignores it, so fail here.
if [ -f "$SUMMARY_OUT" ] && grep -q "^missing_expected|" "$SUMMARY_OUT"; then
  me="$(grep -c "^missing_expected|" "$SUMMARY_OUT" 2>/dev/null || echo 0)"
  rw_log "FATAL: $me screenshot(s) streamed with no stored golden (missing_expected) -- add them to scripts/ios/screenshots-watch."
  [ "$rc" -eq 0 ] && rc=17
fi
if [ -n "$SUITE_FAILURE_LINES" ]; then
  rw_log "STAGE:DEVICE_RUNNER_TEST_FAILED -> assertion/test failure(s) are not allowed:"
  printf '%s\n' "$SUITE_FAILURE_LINES" | sed 's/^/[CN1SS-FAIL] /'
  [ "$rc" -eq 0 ] && rc=19
fi
# The suite must have produced something at all.
[ "${#ACTUAL[@]}" -gt 0 ] || rc=1
rw_log "exit rc=$rc (gate_rc=$gate_rc, mismatch_fail=${CN1SS_FAIL_ON_MISMATCH}, allowed_missing=${CN1SS_ALLOWED_MISSING})"
exit "$rc"
