#!/usr/bin/env bash
# Run the native-fidelity suite on a booted Android emulator: launch the
# fidelity app (it auto-runs the suite), collect the per-tile PNGs over the
# CN1SS WebSocket, (re)generate native goldens from the "_native" frames, then
# score the "_cn1" renders against the goldens and apply the ratchet gate.
#
# Usage: run-android-fidelity-tests.sh <gradle_project_dir>
# Assumes an emulator is already booted (adb device online) and the app APK is
# built. Honors FIDELITY_UPDATE_GOLDENS=1 (refresh committed goldens from this
# run) and FIDELITY_UPDATE_BASELINE=1 (record current fidelity as baseline).
set -euo pipefail

rf_log() { echo "[run-android-fidelity-tests] $1"; }

if [ $# -lt 1 ]; then
  rf_log "Usage: $0 <gradle_project_dir>" >&2
  exit 2
fi
GRADLE_PROJECT_DIR="$1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

APP_DIR="${CN1_APP_DIR:-scripts/fidelity-app}"
GOLDENS_DIR="$APP_DIR/goldens/android"
BASELINE_FILE="$APP_DIR/baseline/android-fidelity-baseline.json"
mkdir -p "$GOLDENS_DIR" "$(dirname "$BASELINE_FILE")"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/common/java"
source "$SCRIPT_DIR/lib/cn1ss.sh"
cn1ss_log() { rf_log "$1"; }

ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts/android-fidelity}"
mkdir -p "$ARTIFACTS_DIR"
TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
WORK_DIR="$(mktemp -d "${TMPDIR}/cn1ss-fid-XXXXXX")"
WS_RAW_DIR="$WORK_DIR/ws"; mkdir -p "$WS_RAW_DIR"
PREVIEW_DIR="$WORK_DIR/previews"; mkdir -p "$PREVIEW_DIR"

# Toolchain (JAVA17 runs the host helpers).
TARGET_JAVA_HOME="${JDK_HOME:-${JAVA17_HOME:-$JAVA_HOME}}"
TARGET_JAVA_BIN="$TARGET_JAVA_HOME/bin/java"
[ -x "$TARGET_JAVA_BIN" ] || { rf_log "java not found at $TARGET_JAVA_BIN"; exit 3; }
cn1ss_setup "$TARGET_JAVA_BIN" "$CN1SS_HELPER_SOURCE_DIR"

command -v adb >/dev/null 2>&1 || { rf_log "adb not on PATH"; exit 3; }
adb wait-for-device

APK_PATH="$(find "$GRADLE_PROJECT_DIR" -path "*/outputs/apk/debug/*.apk" | head -n1 || true)"
[ -n "$APK_PATH" ] || { rf_log "APK not found under $GRADLE_PROJECT_DIR"; exit 4; }
PACKAGE_NAME="$(sed -n 's/.*package="\([^"]*\)".*/\1/p' "$GRADLE_PROJECT_DIR/app/src/main/AndroidManifest.xml" | head -n1)"
MAIN_NAME="$(sed -n 's/^codename1.mainName=//p' "$APP_DIR/common/codenameone_settings.properties" | head -n1)"
rf_log "package=$PACKAGE_NAME launcher=${MAIN_NAME}Stub apk=$APK_PATH"

# Start the host WS server (emulator reaches it via 10.0.2.2:8765).
if ! cn1ss_start_ws_server "$WS_RAW_DIR"; then
  rf_log "FATAL: WebSocket screenshot server did not start"; exit 5
fi
trap 'cn1ss_stop_ws_server || true' EXIT

rf_log "Installing APK"
adb install -r -g "$APK_PATH" >/dev/null 2>&1 || adb install -r "$APK_PATH"
adb logcat -G 16M >/dev/null 2>&1 || true
adb logcat -c || true
TEST_LOG="$ARTIFACTS_DIR/logcat.txt"
adb logcat -v threadtime > "$TEST_LOG" 2>&1 &
LOGCAT_PID=$!
trap 'kill "$LOGCAT_PID" >/dev/null 2>&1 || true; cn1ss_stop_ws_server || true' EXIT
sleep 1

rf_log "Launching $PACKAGE_NAME/.${MAIN_NAME}Stub"
adb shell am force-stop "$PACKAGE_NAME" >/dev/null 2>&1 || true
adb shell am start -n "$PACKAGE_NAME/.${MAIN_NAME}Stub" >/dev/null 2>&1 || \
  adb shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true

END_MARKER="CN1SS:SUITE:FINISHED"
TIMEOUT_SECONDS="${CN1SS_FIDELITY_TIMEOUT:-300}"
START_TIME="$(date +%s)"
rf_log "Waiting up to ${TIMEOUT_SECONDS}s for $END_MARKER"
while true; do
  if grep -q "$END_MARKER" "$TEST_LOG" 2>/dev/null; then rf_log "Suite finished"; break; fi
  NOW="$(date +%s)"
  if [ $(( NOW - START_TIME )) -ge "$TIMEOUT_SECONDS" ]; then
    rf_log "TIMEOUT waiting for suite completion"; break
  fi
  sleep 3
done
sleep 2
cn1ss_stop_ws_server || true
kill "$LOGCAT_PID" >/dev/null 2>&1 || true

rf_log "CN1SS log lines:"; (grep "CN1SS:" "$TEST_LOG" || true) | sed 's/^/  /' | tail -60

# Split delivered PNGs. The fidelity comparison uses the SAME-RUN native render
# as the reference, not the committed golden: both the CN1 and native tiles are
# produced in the *same* environment this run, so the score is robust to the
# subtle rendering differences between environments (emulator GPU, OS version,
# font hinting) that would otherwise make a golden captured elsewhere mismatch.
# The committed goldens are a re-seedable drift artifact for human review.
NATIVE_REF_DIR="$WORK_DIR/native-ref"; mkdir -p "$NATIVE_REF_DIR"
NATIVE_COUNT=0; CN1_COUNT=0
shopt -s nullglob
for png in "$WS_RAW_DIR"/*_native.png; do
  base="$(basename "$png" .png)"; name="${base%_native}"
  cp -f "$png" "$NATIVE_REF_DIR/$name.png"          # same-run reference (comparison)
  if [ "${FIDELITY_UPDATE_GOLDENS:-0}" = "1" ] || [ ! -f "$GOLDENS_DIR/$name.png" ]; then
    cp -f "$png" "$GOLDENS_DIR/$name.png"           # committed drift artifact (re-seedable)
  fi
  NATIVE_COUNT=$(( NATIVE_COUNT + 1 ))
done
declare -a COMPARE_ENTRIES=()
for png in "$WS_RAW_DIR"/*_cn1.png; do
  base="$(basename "$png" .png)"; name="${base%_cn1}"
  dest="$WORK_DIR/${name}_cn1.png"; cp -f "$png" "$dest"
  COMPARE_ENTRIES+=("${name}=${dest}")
  CN1_COUNT=$(( CN1_COUNT + 1 ))
done
shopt -u nullglob
rf_log "Delivered: ${NATIVE_COUNT} native, ${CN1_COUNT} cn1. Same-run native ref: $NATIVE_REF_DIR; committed goldens: $GOLDENS_DIR"

if [ "$CN1_COUNT" -eq 0 ]; then
  rf_log "FATAL: no CN1 renders delivered over WebSocket"
  exit 12
fi
if [ "$NATIVE_COUNT" -eq 0 ]; then
  rf_log "FATAL: no native references delivered (NativeWidgetFactory unavailable?)"
  exit 12
fi

export CN1SS_COMMENT_MARKER="<!-- CN1SS_FIDELITY_ANDROID_COMMENT -->"
export CN1SS_FIDELITY_SPEC="${CN1SS_FIDELITY_SPEC:-$APP_DIR/common/src/main/resources/fidelity-tests.yaml}"
export CN1SS_FIDELITY_PLATFORM="${CN1SS_FIDELITY_PLATFORM:-android}"
export CN1SS_PREVIEW_SUBDIR="android-fidelity"
COMPARE_JSON="$WORK_DIR/fidelity-compare.json"
SUMMARY_FILE="$WORK_DIR/fidelity-summary.txt"
COMMENT_FILE="$WORK_DIR/fidelity-comment.md"

cn1ss_process_fidelity \
  "Native fidelity (Android, Material 3)" \
  "$COMPARE_JSON" "$SUMMARY_FILE" "$COMMENT_FILE" \
  "$NATIVE_REF_DIR" "$PREVIEW_DIR" "$ARTIFACTS_DIR" "$BASELINE_FILE" \
  "${COMPARE_ENTRIES[@]}"
rc=$?
cp -f "$COMMENT_FILE" "$ARTIFACTS_DIR/fidelity-comment.md" 2>/dev/null || true
rf_log "Done (rc=$rc). Artifacts in $ARTIFACTS_DIR"
exit $rc
