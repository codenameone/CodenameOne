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

cleanup() { cn1ss_stop_ws_server 2>/dev/null || true; xcrun simctl terminate "$WATCH_UDID" "$BUNDLE_ID" 2>/dev/null || true; }
trap cleanup EXIT

cn1ss_start_ws_server "$WS_RAW_DIR" || { rw_log "Failed to start Cn1ssScreenshotServer"; exit 6; }
rw_log "WS sink on port ${CN1SS_WS_PORT:-8765} -> $WS_RAW_DIR"

xcrun simctl terminate "$WATCH_UDID" "$BUNDLE_ID" 2>/dev/null || true
xcrun simctl install "$WATCH_UDID" "$APP_PATH"
xcrun simctl launch "$WATCH_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || { rw_log "launch failed"; exit 6; }
rw_log "Launched watch app; waiting for the suite to stream screenshots..."

# Poll until the streamed count is stable (suite idle) or we hit the cap.
MAX_WAIT="${CN1SS_WATCH_TIMEOUT:-1200}"
prev=-1; stable=0; waited=0
while [ "$waited" -lt "$MAX_WAIT" ]; do
  sleep 8; waited=$((waited+8))
  cur="$(/usr/bin/find "$WS_RAW_DIR" -name '*.png' 2>/dev/null | wc -l | tr -d ' ')"
  if [ "$cur" = "$prev" ] && [ "$cur" -gt 0 ]; then
    stable=$((stable+1)); [ "$stable" -ge 3 ] && break
  else stable=0; fi
  prev="$cur"
done
rw_log "Capture settled: $prev screenshots after ${waited}s"

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
CN1SS_SUCCESS_MESSAGE="${CN1SS_SUCCESS_MESSAGE:-Apple Watch (watchOS, Core Graphics) screenshots match the goldens.}" \
cn1ss_process_and_report \
  "Apple Watch (watchOS / Core Graphics)" \
  "$COMPARE_JSON" "$SUMMARY_OUT" "$COMMENT_OUT" \
  "$REF_DIR" "$PREVIEW_DIR" "$ARTIFACTS_DIR" \
  "${ACTUAL[@]}" || true

cp -f "$COMPARE_JSON" "$SUMMARY_OUT" "$COMMENT_OUT" "$ARTIFACTS_DIR/" 2>/dev/null || true
cn1ss_post_pr_comment "$COMMENT_OUT" "$PREVIEW_DIR" || true

# --- Gate (mirrors the iOS jobs) -------------------------------------------
MISSING="$(cn1ss_count_missing "$COMPARE_JSON" 2>/dev/null || echo 0)"
MISMATCH="$(grep -c '"status"[[:space:]]*:[[:space:]]*"mismatch"' "$COMPARE_JSON" 2>/dev/null || echo 0)"
rw_log "missing=$MISSING mismatch=$MISMATCH (allowed_missing=${CN1SS_ALLOWED_MISSING:-0})"
rc=0
if [ "${CN1SS_FAIL_ON_MISMATCH:-1}" = "1" ] && [ "$MISMATCH" -gt 0 ]; then rc=1; fi
if [ "$MISSING" -gt "${CN1SS_ALLOWED_MISSING:-0}" ]; then rc=1; fi
[ "$prev" -gt 0 ] || rc=1
rw_log "exit rc=$rc"
exit "$rc"
