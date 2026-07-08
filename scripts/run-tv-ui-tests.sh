#!/usr/bin/env bash
# Run the Codename One UI screenshot suite on the tvOS simulator and compare the
# captured frames to the Apple TV golden set (scripts/ios/screenshots-tv).
# The golden set includes the Material FontImage glyphs (tab/FAB/toolbar icons,
# checkbox/radio marks, ImageViewer navigation arrows) which load on tvOS via
# the runtime CTFontManagerRegisterFontsForURL registration in IOSNative.m.
#
# Apple TV reuses the iOS UIApplicationMain entry and the Metal renderer (tvOS
# has UIKit + Metal, just no OpenGL ES), so the <Main>TV target is built like a
# regular iOS app for the appletvsimulator SDK and launched via simctl. It
# streams each screenshot to the host-side Cn1ssScreenshotServer over
# ws://127.0.0.1:8765 -- the same transport the iOS / watch jobs use -- so the
# comparison/report tooling in scripts/lib/cn1ss.sh is reused verbatim.
#
# Usage: run-tv-ui-tests.sh <workspace_or_project> [scheme]
set -euo pipefail

rt_log() { printf '%s %s\n' "[run-tv-ui-tests]" "$*" >&2; }

WORKSPACE_PATH="${1:?Usage: $0 <workspace_or_project> [scheme]}"
SCHEME="${2:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/common/java"
source "$SCRIPT_DIR/lib/cn1ss.sh"

: "${ARTIFACTS_DIR:=$REPO_ROOT/artifacts/tv-ui-tests}"
mkdir -p "$ARTIFACTS_DIR"

# --- Xcode / project resolution -------------------------------------------
if [ -z "${XCODE_APP:-}" ]; then
  XCODE_APP="$(ls -d /Applications/Xcode_26*.app 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [ ! -x "${XCODE_APP:-}/Contents/Developer/usr/bin/xcodebuild" ]; then
  XCODE_APP="/Applications/Xcode.app"
fi
export DEVELOPER_DIR="${DEVELOPER_DIR:-$XCODE_APP/Contents/Developer}"
rt_log "Using DEVELOPER_DIR=$DEVELOPER_DIR"
if ! command -v xcodebuild >/dev/null 2>&1; then
  rt_log "xcodebuild not found (DEVELOPER_DIR=$DEVELOPER_DIR)"; exit 3
fi

# The tvOS target builds via -project (its target carries no CocoaPods deps).
if [[ "$WORKSPACE_PATH" == *.xcodeproj ]]; then
  PROJECT_PATH="$WORKSPACE_PATH"
else
  PROJECT_DIR="$(dirname "$WORKSPACE_PATH")"
  base="$(basename "$WORKSPACE_PATH" .xcworkspace)"
  PROJECT_PATH="$PROJECT_DIR/$base.xcodeproj"
fi
if [ ! -d "$PROJECT_PATH" ]; then
  rt_log "Could not resolve .xcodeproj from '$WORKSPACE_PATH' (looked for $PROJECT_PATH)"; exit 3
fi
[ -z "$SCHEME" ] && SCHEME="$(basename "$PROJECT_PATH" .xcodeproj)"
TV_TARGET="${SCHEME}TV"
rt_log "Project=$PROJECT_PATH tvTarget=$TV_TARGET"

# --- Pick an Apple TV simulator --------------------------------------------
# Screenshots are pixel-compared, so the device must match the one the goldens
# were captured on: Apple TV renders at 1920x1080. Prefer "Apple TV 4K"; fall
# back to any Apple TV (override with CN1SS_TV_UDID / CN1SS_TV_MODEL).
TV_MODEL_PREF="${CN1SS_TV_MODEL:-Apple TV 4K}"
TV_UDID="${CN1SS_TV_UDID:-}"
DEVLIST="$(xcrun simctl list devices available 2>/dev/null | grep -iE 'Apple TV')"
if [ -z "$TV_UDID" ]; then
  TV_UDID="$(printf '%s\n' "$DEVLIST" | grep -i "$TV_MODEL_PREF" | grep -oE '\([0-9A-F-]{36}\)' | head -1 | tr -d '()')"
fi
if [ -z "$TV_UDID" ]; then
  rt_log "No '$TV_MODEL_PREF' simulator found; falling back to any available Apple TV"
  TV_UDID="$(printf '%s\n' "$DEVLIST" | grep -oE '\([0-9A-F-]{36}\)' | head -1 | tr -d '()')"
fi
if [ -z "$TV_UDID" ]; then
  # Xcode 26 on some macos-15 runner images ships without the tvOS Simulator
  # runtime (the same lean-image issue run-ios-ui-tests.sh handles for iOS).
  # Install it on demand and create an Apple TV 4K device (matches the 1920x1080
  # golden) so the run has a target. Gated on CI / XCODE_DOWNLOAD_PLATFORMS;
  # never runs when a device is already present, so healthy runners are untouched.
  TV_DL="${XCODE_DOWNLOAD_PLATFORMS:-}"
  if [ -z "$TV_DL" ] && [ "${GITHUB_ACTIONS:-false}" = "true" ]; then TV_DL="true"; fi
  if [ "${TV_DL:-false}" = "true" ]; then
    rt_log "No Apple TV simulator; installing tvOS platform via xcodebuild -downloadPlatform tvOS"
    xcodebuild -downloadPlatform tvOS || true
    TV_RT="$(xcrun simctl list -j runtimes available 2>/dev/null | python3 -c 'import json,sys
rs=[r for r in json.load(sys.stdin).get("runtimes",[]) if r.get("isAvailable") and r.get("identifier","").startswith("com.apple.CoreSimulator.SimRuntime.tvOS-")]
rs.sort(key=lambda r:r.get("version",""),reverse=True)
print(rs[0]["identifier"] if rs else "")' 2>/dev/null || true)"
    TV_DT="$(xcrun simctl list -j devicetypes 2>/dev/null | python3 -c 'import json,sys
ts=[t["identifier"] for t in json.load(sys.stdin).get("devicetypes",[]) if "Apple TV 4K" in t.get("name","")]
print(ts[0] if ts else "")' 2>/dev/null || true)"
    if [ -n "$TV_RT" ] && [ -n "$TV_DT" ]; then
      rt_log "Creating Apple TV simulator ($TV_DT on $TV_RT)"
      xcrun simctl create "cn1-tv-tests" "$TV_DT" "$TV_RT" >/dev/null 2>&1 || true
      DEVLIST="$(xcrun simctl list devices available 2>/dev/null | grep -iE 'Apple TV')"
      TV_UDID="$(printf '%s\n' "$DEVLIST" | grep -i "$TV_MODEL_PREF" | grep -oE '\([0-9A-F-]{36}\)' | head -1 | tr -d '()')"
      [ -z "$TV_UDID" ] && TV_UDID="$(printf '%s\n' "$DEVLIST" | grep -oE '\([0-9A-F-]{36}\)' | head -1 | tr -d '()')"
    fi
  fi
fi
if [ -z "$TV_UDID" ]; then
  rt_log "No Apple TV simulator available. Install a tvOS runtime in Xcode."; exit 4
fi
rt_log "Apple TV simulators available:"; printf '%s\n' "$DEVLIST" | sed 's/^/  /' >&2
rt_log "Using Apple TV simulator $TV_UDID (pref '$TV_MODEL_PREF')"
xcrun simctl boot "$TV_UDID" 2>/dev/null || true
xcrun simctl bootstatus "$TV_UDID" -b 2>/dev/null || true

# --- Build the tvOS target --------------------------------------------------
BUILD_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/cn1-tv-build-XXXXXX")"
rt_log "Building $TV_TARGET for appletvsimulator -> $BUILD_ROOT"
# Build for arm64 explicitly: the macos-15 runner is Apple Silicon and its
# tvOS simulator is arm64, so the app must be arm64 to launch. It also keeps
# the NEON-only IOSSimd.m off the x86_64 slice (a destination-less simulator
# `build` otherwise resolves the active arch to x86_64 and fails to compile).
xcodebuild -project "$PROJECT_PATH" -target "$TV_TARGET" \
  -sdk appletvsimulator -configuration Debug \
  ARCHS=arm64 ONLY_ACTIVE_ARCH=NO CODE_SIGNING_ALLOWED=NO SYMROOT="$BUILD_ROOT" build \
  > "$ARTIFACTS_DIR/tv-build.log" 2>&1 || {
    rt_log "tvOS build FAILED (see $ARTIFACTS_DIR/tv-build.log)"; tail -40 "$ARTIFACTS_DIR/tv-build.log" >&2; exit 5; }

APP_PATH="$(/usr/bin/find "$BUILD_ROOT" -name "${TV_TARGET}.app" -maxdepth 3 | head -1)"
[ -z "$APP_PATH" ] && { rt_log "Built tvOS .app not found under $BUILD_ROOT"; exit 5; }
BUNDLE_ID="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$APP_PATH/Info.plist" 2>/dev/null)"
[ -z "$BUNDLE_ID" ] && { rt_log "Could not read CFBundleIdentifier from $APP_PATH"; exit 5; }
rt_log "Built $APP_PATH (bundle $BUNDLE_ID)"

# --- Screenshot capture: host WS sink + the streaming tvOS app --------------
JAVA_BIN="${JAVA17_BIN:-$(command -v java)}"
cn1ss_setup "$JAVA_BIN" "$CN1SS_HELPER_SOURCE_DIR"

SS_TMP="$(mktemp -d "${TMPDIR:-/tmp}/cn1-tv-ss-XXXXXX")"
WS_RAW_DIR="$SS_TMP/ws"; PREVIEW_DIR="$SS_TMP/previews"
mkdir -p "$WS_RAW_DIR" "$PREVIEW_DIR"

cleanup() { cn1ss_stop_ws_server 2>/dev/null || true; xcrun simctl terminate "$TV_UDID" "$BUNDLE_ID" 2>/dev/null || true; }
trap cleanup EXIT

cn1ss_start_ws_server "$WS_RAW_DIR" || { rt_log "Failed to start Cn1ssScreenshotServer"; exit 6; }
rt_log "WS sink on port ${CN1SS_WS_PORT:-8765} -> $WS_RAW_DIR"

xcrun simctl terminate "$TV_UDID" "$BUNDLE_ID" 2>/dev/null || true
xcrun simctl install "$TV_UDID" "$APP_PATH"
# Capture the app's stdout/stderr (the CN1SS:* suite markers and any native
# exception / crash) so a no-screenshot run is diagnosable instead of silent.
APP_CONSOLE="$ARTIFACTS_DIR/tv-app-console.log"
: > "$APP_CONSOLE"
( xcrun simctl launch --console-pty "$TV_UDID" "$BUNDLE_ID" >>"$APP_CONSOLE" 2>&1 ) &
APP_CONSOLE_PID=$!
rt_log "Launched tvOS app (console -> $APP_CONSOLE); waiting for the suite to stream screenshots..."

MAX_WAIT="${CN1SS_TV_TIMEOUT:-1200}"
TV_REF_DIR="${SCREENSHOT_REF_DIR:-$SCRIPT_DIR/ios/screenshots-tv}"
EXPECTED="$(/usr/bin/find "$TV_REF_DIR" -name '*.png' 2>/dev/null | wc -l | tr -d ' ')"
rt_log "Expecting $EXPECTED screenshots (golden set)"
prev=-1; stable=0; waited=0
while [ "$waited" -lt "$MAX_WAIT" ]; do
  sleep 8; waited=$((waited+8))
  cur="$(/usr/bin/find "$WS_RAW_DIR" -name '*.png' 2>/dev/null | wc -l | tr -d ' ')"
  if [ "$EXPECTED" -gt 0 ] && [ "$cur" -ge "$EXPECTED" ]; then
    stable=$((stable+1)); [ "$stable" -ge 2 ] && break
    continue
  fi
  if [ "$cur" = "$prev" ] && [ "$cur" -gt 0 ]; then
    stable=$((stable+1)); [ "$stable" -ge 10 ] && break
  else stable=0; fi
  prev="$cur"
  # The suite emits CN1SS:SUITE:FINISHED when done; bail early on that (covers
  # the seed run where EXPECTED=0) or on an obvious native crash, instead of
  # blocking the full MAX_WAIT.
  if grep -qa "CN1SS:SUITE:FINISHED" "$APP_CONSOLE" 2>/dev/null; then
    rt_log "Suite reported FINISHED after ${waited}s"; break
  fi
  if grep -qaE "Fatal|Terminating app due to uncaught exception|EXC_BAD|did crash|libsystem_kernel" "$APP_CONSOLE" 2>/dev/null; then
    rt_log "Detected app crash/fatal in console after ${waited}s"; break
  fi
done
rt_log "Capture settled: $(/usr/bin/find "$WS_RAW_DIR" -name '*.png' 2>/dev/null | wc -l | tr -d ' ') of $EXPECTED screenshots after ${waited}s"

SUITE_FAILURE_LINES="$(cn1ss_collect_suite_failures "$APP_CONSOLE")"
if [ -n "$SUITE_FAILURE_LINES" ]; then
  rt_log "Detected DeviceRunner assertion/test failure(s); artifacts and screenshot report will still be collected before failing."
fi

# Always surface the app console + any crash report so a zero-screenshot run is
# diagnosable from the uploaded artifacts.
rt_log "----- tvOS app console (tail) -----"
tail -60 "$APP_CONSOLE" 2>/dev/null | sed 's/^/[tv-app] /' || true
rt_log "----- end app console -----"
CRASH_DIR="$HOME/Library/Logs/DiagnosticReports"
SIM_CRASH_DIR="$HOME/Library/Developer/CoreSimulator/Devices/$TV_UDID/data/Library/Logs/DiagnosticReports"
for d in "$SIM_CRASH_DIR" "$CRASH_DIR"; do
  [ -d "$d" ] || continue
  /usr/bin/find "$d" -name 'HelloCodenameOneTV*' -newermt '-10 minutes' 2>/dev/null | while IFS= read -r cr; do
    rt_log "----- crash report: $cr -----"; sed -n '1,40p' "$cr" | sed 's/^/[crash] /'
    cp "$cr" "$ARTIFACTS_DIR/" 2>/dev/null || true
  done
done

# --- Compare against the tvOS golden set + emit report ----------------------
REF_DIR="${SCREENSHOT_REF_DIR:-$SCRIPT_DIR/ios/screenshots-tv}"
REF_DIR="$(cd "$REF_DIR" && pwd)"
declare -a ACTUAL=()
while IFS= read -r png; do
  name="$(basename "$png" .png)"
  ACTUAL+=("$name=$png")
done < <(/usr/bin/find "$WS_RAW_DIR" -name '*.png' | sort)

cp -f "$WS_RAW_DIR"/*.png "$ARTIFACTS_DIR/" 2>/dev/null || true

COMPARE_JSON="$SS_TMP/compare.json"; SUMMARY_OUT="$SS_TMP/summary.txt"; COMMENT_OUT="$SS_TMP/comment.md"

export CN1SS_FAIL_ON_MISMATCH="${CN1SS_FAIL_ON_MISMATCH:-1}"
export CN1SS_ALLOWED_MISSING="${CN1SS_ALLOWED_MISSING:-0}"
set +e
CN1SS_SUCCESS_MESSAGE="${CN1SS_SUCCESS_MESSAGE:-Apple TV (tvOS, Metal) screenshots match the goldens.}" \
cn1ss_process_and_report \
  "Apple TV (tvOS / Metal)" \
  "$COMPARE_JSON" "$SUMMARY_OUT" "$COMMENT_OUT" \
  "$REF_DIR" "$PREVIEW_DIR" "$ARTIFACTS_DIR" \
  "${ACTUAL[@]}"
gate_rc=$?
set -e

cp -f "$COMPARE_JSON" "$SUMMARY_OUT" "$COMMENT_OUT" "$ARTIFACTS_DIR/" 2>/dev/null || true
cn1ss_post_pr_comment "$COMMENT_OUT" "$PREVIEW_DIR" || true

rc="$gate_rc"
if [ -f "$SUMMARY_OUT" ] && grep -q "^missing_expected|" "$SUMMARY_OUT"; then
  me="$(grep -c "^missing_expected|" "$SUMMARY_OUT" 2>/dev/null || echo 0)"
  rt_log "FATAL: $me screenshot(s) streamed with no stored golden (missing_expected) -- add them to scripts/ios/screenshots-tv."
  [ "$rc" -eq 0 ] && rc=17
fi
if [ -n "$SUITE_FAILURE_LINES" ]; then
  rt_log "STAGE:DEVICE_RUNNER_TEST_FAILED -> assertion/test failure(s) are not allowed:"
  printf '%s\n' "$SUITE_FAILURE_LINES" | sed 's/^/[CN1SS-FAIL] /'
  [ "$rc" -eq 0 ] && rc=19
fi
[ "${#ACTUAL[@]}" -gt 0 ] || rc=1
rt_log "exit rc=$rc (gate_rc=$gate_rc, mismatch_fail=${CN1SS_FAIL_ON_MISMATCH}, allowed_missing=${CN1SS_ALLOWED_MISSING})"
exit "$rc"
