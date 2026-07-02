#!/usr/bin/env bash
# Run the native-fidelity suite on a booted iOS simulator (Metal pipeline).
# Installs + launches the prebuilt fidelity .app, collects the per-tile PNGs over
# the CN1SS WebSocket, then scores the CN1 renders against the SAME-RUN native
# UIKit references and applies the ratchet gate.
#
# Usage: run-ios-fidelity-tests.sh <app_bundle_path> [simulator_udid]
# <app_bundle_path> is the built *.app for the iphonesimulator SDK.
# Honors FIDELITY_UPDATE_GOLDENS=1 and FIDELITY_UPDATE_BASELINE=1.
set -euo pipefail

rf_log() { echo "[run-ios-fidelity-tests] $1"; }

if [ $# -lt 1 ]; then
  rf_log "Usage: $0 <app_bundle_path> [simulator_udid]" >&2
  exit 2
fi
APP_BUNDLE="$1"
SIM_UDID="${2:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

APP_DIR="${CN1_APP_DIR:-scripts/fidelity-app}"
GOLDENS_DIR="$APP_DIR/goldens/ios-metal"
BASELINE_FILE="$APP_DIR/baseline/ios-metal-fidelity-baseline.json"
mkdir -p "$GOLDENS_DIR" "$(dirname "$BASELINE_FILE")"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/common/java"
source "$SCRIPT_DIR/lib/cn1ss.sh"
cn1ss_log() { rf_log "$1"; }

ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts/ios-fidelity}"
mkdir -p "$ARTIFACTS_DIR"
TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
WORK_DIR="$(mktemp -d "${TMPDIR}/cn1ss-fid-ios-XXXXXX")"
WS_RAW_DIR="$WORK_DIR/ws"; mkdir -p "$WS_RAW_DIR"
PREVIEW_DIR="$WORK_DIR/previews"; mkdir -p "$PREVIEW_DIR"

[ -d "$APP_BUNDLE" ] || { rf_log "App bundle not found: $APP_BUNDLE"; exit 4; }
BUNDLE_ID="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$APP_BUNDLE/Info.plist" 2>/dev/null || echo "com.codenameone.fidelity")"
rf_log "app=$APP_BUNDLE bundle=$BUNDLE_ID"

# Host helpers run under a JDK (11-25). Prefer JAVA17_HOME, else JAVA_HOME, else java on PATH.
TARGET_JAVA_HOME="${JAVA17_HOME:-${JAVA_HOME:-}}"
if [ -n "$TARGET_JAVA_HOME" ] && [ -x "$TARGET_JAVA_HOME/bin/java" ]; then
  TARGET_JAVA_BIN="$TARGET_JAVA_HOME/bin/java"
else
  TARGET_JAVA_BIN="$(command -v java)"
fi
[ -x "$TARGET_JAVA_BIN" ] || { rf_log "java not found"; exit 3; }
cn1ss_setup "$TARGET_JAVA_BIN" "$CN1SS_HELPER_SOURCE_DIR"

# Pick a booted simulator if none was given.
if [ -z "$SIM_UDID" ]; then
  SIM_UDID="$(xcrun simctl list devices booted 2>/dev/null | grep -Eo '[0-9A-F-]{36}' | head -n1 || true)"
fi
if [ -z "$SIM_UDID" ]; then
  rf_log "No booted simulator and none specified; booting iPhone 16"
  SIM_UDID="$(xcrun simctl list devices available | grep -E 'iPhone 16 \(' | grep -Eo '[0-9A-F-]{36}' | head -n1)"
  xcrun simctl boot "$SIM_UDID"
  xcrun simctl bootstatus "$SIM_UDID" -b
fi
rf_log "Using simulator $SIM_UDID"

if ! cn1ss_start_ws_server "$WS_RAW_DIR"; then
  rf_log "FATAL: WebSocket screenshot server did not start"; exit 5
fi
LOG_PID=0
cleanup() { [ "$LOG_PID" -ne 0 ] && kill "$LOG_PID" >/dev/null 2>&1 || true; cn1ss_stop_ws_server || true; }
trap cleanup EXIT

TEST_LOG="$ARTIFACTS_DIR/simctl-log.txt"
xcrun simctl spawn "$SIM_UDID" log stream --level debug --predicate 'eventMessage CONTAINS "CN1SS"' > "$TEST_LOG" 2>&1 &
LOG_PID=$!
sleep 1

rf_log "Installing app"
xcrun simctl install "$SIM_UDID" "$APP_BUNDLE"
rf_log "Launching $BUNDLE_ID (Metal pipeline)"
xcrun simctl terminate "$SIM_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true
# Metal layer validation forwarded into the app process (mirrors the CN1SS iOS job).
SIMCTL_CHILD_MTL_DEBUG_LAYER=1 xcrun simctl launch "$SIM_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || \
  xcrun simctl launch "$SIM_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true

END_MARKER="CN1SS:SUITE:FINISHED"
TIMEOUT_SECONDS="${CN1SS_FIDELITY_TIMEOUT:-600}"
START_TIME="$(date +%s)"
rf_log "Waiting up to ${TIMEOUT_SECONDS}s for $END_MARKER"
while true; do
  if grep -q "$END_MARKER" "$TEST_LOG" 2>/dev/null; then rf_log "Suite finished"; break; fi
  NOW="$(date +%s)"
  if [ $(( NOW - START_TIME )) -ge "$TIMEOUT_SECONDS" ]; then rf_log "TIMEOUT"; break; fi
  sleep 3
done
sleep 2
cn1ss_stop_ws_server || true
[ "$LOG_PID" -ne 0 ] && kill "$LOG_PID" >/dev/null 2>&1 || true

rf_log "CN1SS log tail:"; (grep "CN1SS:" "$TEST_LOG" || true) | sed 's/^/  /' | tail -40

# The iOS native references are generated OFFLINE by the standalone native-ref
# app (scripts/build-ios-native-ref.sh -> committed goldens), NOT same-run: a real
# UIWindow renders the UIKit widgets correctly, unlike the off-screen factory
# render that produced blank nav/tab bars and point-sized (tiny) widgets. So the
# CN1 suite here only renders the CN1 side and diffs it against the committed
# goldens. The CN1 app may still deliver factory native renders; they are ignored.
CN1_COUNT=0
FRAME_COUNT=0
FRAMES_WORK_DIR="$WORK_DIR/frames"; mkdir -p "$FRAMES_WORK_DIR"
shopt -s nullglob
declare -a COMPARE_ENTRIES=()
for png in "$WS_RAW_DIR"/*_cn1.png; do
  base="$(basename "$png" .png)"; name="${base%_cn1}"
  # Animation-frame captures ("<id>_tNNN_<appearance>") are validated by
  # MorphFrameValidator against committed CN1 frame goldens + motion properties;
  # they have no native golden, so they are kept OUT of the fidelity comparison.
  if [[ "$name" =~ _t[0-9]{3}_ ]]; then
    cp -f "$png" "$FRAMES_WORK_DIR/${name}_cn1.png"
    FRAME_COUNT=$(( FRAME_COUNT + 1 ))
    continue
  fi
  dest="$WORK_DIR/${name}_cn1.png"; cp -f "$png" "$dest"
  COMPARE_ENTRIES+=("${name}=${dest}")
  CN1_COUNT=$(( CN1_COUNT + 1 ))
done
GOLDEN_COUNT=$(ls "$GOLDENS_DIR"/*.png 2>/dev/null | wc -l | tr -d ' ')
shopt -u nullglob
rf_log "Delivered: ${CN1_COUNT} cn1 + ${FRAME_COUNT} animation frame(s); committed native goldens: ${GOLDEN_COUNT}"
[ "$CN1_COUNT" -gt 0 ] || { rf_log "FATAL: no CN1 renders delivered"; exit 12; }
[ "$GOLDEN_COUNT" -gt 0 ] || { rf_log "FATAL: no committed iOS goldens (run scripts/build-ios-native-ref.sh)"; exit 12; }

export CN1SS_COMMENT_MARKER="<!-- CN1SS_FIDELITY_IOS_COMMENT -->"
export CN1SS_PREVIEW_SUBDIR="ios-fidelity"
export CN1SS_FIDELITY_SPEC="${CN1SS_FIDELITY_SPEC:-$APP_DIR/common/src/main/resources/fidelity-tests.yaml}"
export CN1SS_FIDELITY_PLATFORM="${CN1SS_FIDELITY_PLATFORM:-ios}"
cn1ss_process_fidelity \
  "Native fidelity (iOS Modern, Metal)" \
  "$WORK_DIR/fidelity-compare.json" "$WORK_DIR/fidelity-summary.txt" "$WORK_DIR/fidelity-comment.md" \
  "$GOLDENS_DIR" "$PREVIEW_DIR" "$ARTIFACTS_DIR" "$BASELINE_FILE" \
  "${COMPARE_ENTRIES[@]}"
rc=$?
cp -f "$WORK_DIR/fidelity-comment.md" "$ARTIFACTS_DIR/fidelity-comment.md" 2>/dev/null || true

# ---- deterministic animation-frame validation ----
# Frames are self-goldens (CN1 vs committed CN1): golden drift, stuck frames,
# non-monotonic travel and broken overshoot all fail here. Missing goldens are
# seeded from the run (and must be committed); the strips land in artifacts so
# reviewers can see the whole motion at a glance.
if [ "$FRAME_COUNT" -gt 0 ]; then
  FRAME_GOLDENS_DIR="$APP_DIR/goldens/ios-metal-frames"
  rf_log "STAGE:MORPH_FRAMES -> validating ${FRAME_COUNT} animation frame(s)"
  frame_rc=0
  cn1ss_java_run MorphFrameValidator \
    --frames-dir "$FRAMES_WORK_DIR" \
    --goldens-dir "$FRAME_GOLDENS_DIR" \
    --seed-missing \
    --out-json "$ARTIFACTS_DIR/morph-frames.json" \
    --strip-dir "$ARTIFACTS_DIR" || frame_rc=$?
  cp -f "$FRAMES_WORK_DIR"/*.png "$ARTIFACTS_DIR/" 2>/dev/null || true
  if [ "$frame_rc" -ne 0 ]; then
    rf_log "Animation-frame validation FAILED (rc=$frame_rc)"
    if [ "${CN1SS_FAIL_ON_MISMATCH:-0}" = "1" ]; then
      [ "$rc" -eq 0 ] && rc=$frame_rc
    else
      rf_log "WARNING: not failing the run (CN1SS_FAIL_ON_MISMATCH unset)"
    fi
  fi
fi

rf_log "Done (rc=$rc). Artifacts in $ARTIFACTS_DIR"
exit $rc
