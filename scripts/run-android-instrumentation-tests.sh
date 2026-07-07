#!/usr/bin/env bash
# Run instrumentation tests and reconstruct screenshot emitted as chunked Base64 (NO ADB)
set -euo pipefail

ra_log() { echo "[run-android-instrumentation-tests] $1"; }

# ---- Helpers ---------------------------------------------------------------

ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

extract_base64_stats() {
  local log_file="$1"
  local out_file="$2"
  [ -f "$log_file" ] || return 0

  local lines
  lines="$(grep 'CN1SS:STAT:' "$log_file" 2>/dev/null | sed -E 's/^.*CN1SS:STAT://')" || true
  if [ -z "${lines:-}" ]; then
    return 0
  fi

  : > "$out_file"
  while IFS= read -r line; do
    [ -n "$line" ] || continue
    echo "$line" >> "$out_file"
  done <<< "$lines"
}

# CN1SS helpers are implemented in Java for easier maintenance
# (Defaults for class names are provided by cn1ss.sh)

# ---- Args & environment ----------------------------------------------------

if [ $# -lt 1 ]; then
  ra_log "Usage: $0 <gradle_project_dir>" >&2
  exit 2
fi
GRADLE_PROJECT_DIR="$1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/common/java"
source "$SCRIPT_DIR/lib/cn1ss.sh"

if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/Cn1ssScreenshotServer.java" ]; then
  ra_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/Cn1ssScreenshotServer.java" >&2
  exit 3
fi
cn1ss_log() { ra_log "$1"; }

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
ensure_dir "$ARTIFACTS_DIR"
TEST_LOG="$ARTIFACTS_DIR/connectedAndroidTest.log"
SCREENSHOT_REF_DIR="$SCRIPT_DIR/android/screenshots"
SCREENSHOT_TMP_DIR="$(mktemp -d "${TMPDIR}/cn1ss-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1ss-tmp")"
ensure_dir "$SCREENSHOT_TMP_DIR"
SCREENSHOT_PREVIEW_DIR="$SCREENSHOT_TMP_DIR/previews"
COVERAGE_SUMMARY="$ARTIFACTS_DIR/android-coverage-report/coverage-summary.json"

ra_log "Loading workspace environment from $ENV_FILE"
[ -f "$ENV_FILE" ] || { ra_log "Missing env file: $ENV_FILE"; exit 3; }
# shellcheck disable=SC1090
source "$ENV_FILE"

if [ -z "${JAVA17_HOME:-}" ]; then
  ra_log "JAVA17_HOME not set in workspace environment" >&2
  exit 3
fi

TARGET_JAVA_HOME="${JDK_HOME:-$JAVA17_HOME}"
TARGET_JAVA_BIN="$TARGET_JAVA_HOME/bin/java"

if [ ! -x "$TARGET_JAVA_BIN" ]; then
  ra_log "Target java binary missing at $TARGET_JAVA_BIN" >&2
  exit 3
fi

cn1ss_setup "$TARGET_JAVA_BIN" "$CN1SS_HELPER_SOURCE_DIR"

# Start the host-side WebSocket screenshot server on the fixed standard port.
# The Android emulator reaches the host loopback via 10.0.2.2, so the device-
# runner defaults to ws://10.0.2.2:8765 with no per-launch injection. PNGs the
# app sends land directly in $WS_RAW_DIR; if WS delivers nothing the legacy
# logcat base64 decode below is used instead, so this is purely additive.
WS_RAW_DIR="$SCREENSHOT_TMP_DIR/ws"
ensure_dir "$WS_RAW_DIR"
if cn1ss_start_ws_server "$WS_RAW_DIR"; then
  ra_log "WebSocket screenshot server listening on port ${CN1SS_WS_PORT} (out=$WS_RAW_DIR)"
else
  ra_log "WebSocket screenshot server did not start; relying on logcat base64 fallback"
fi

[ -d "$GRADLE_PROJECT_DIR" ] || { ra_log "Gradle project directory not found: $GRADLE_PROJECT_DIR"; exit 4; }
[ -x "$GRADLE_PROJECT_DIR/gradlew" ] || chmod +x "$GRADLE_PROJECT_DIR/gradlew"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$ANDROID_SDK_ROOT" ]; then
  if [ -d "/usr/local/lib/android/sdk" ]; then ANDROID_SDK_ROOT="/usr/local/lib/android/sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then ANDROID_SDK_ROOT="$HOME/Android/Sdk"; fi
fi
if [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
  export ANDROID_SDK_ROOT ANDROID_HOME="$ANDROID_SDK_ROOT"
  SDKMANAGER_BIN=""
  if [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    SDKMANAGER_BIN="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
  elif command -v sdkmanager >/dev/null 2>&1; then
    SDKMANAGER_BIN="$(command -v sdkmanager)"
  fi
  if [ -n "$SDKMANAGER_BIN" ]; then
    ra_log "Ensuring Android SDK platform/build-tools 36 are installed"
    SDK_INSTALL_LOG="$ARTIFACTS_DIR/sdkmanager-android-36.log"
    if yes | "$SDKMANAGER_BIN" "platforms;android-36" "build-tools;36.0.0" >"$SDK_INSTALL_LOG" 2>&1; then
      ra_log "Android SDK 36 components installed"
    else
      ra_log "Warning: unable to install Android SDK 36 components (see $SDK_INSTALL_LOG)"
    fi
  else
    ra_log "Warning: sdkmanager not found; cannot install API 36 components"
  fi
else
  ra_log "Warning: Android SDK root not found; cannot install API 36 components"
fi

# ---- Prepare app + emulator state -----------------------------------------
MANIFEST="$GRADLE_PROJECT_DIR/app/src/main/AndroidManifest.xml"
if [ ! -f "$MANIFEST" ]; then
  ra_log "FATAL: AndroidManifest.xml not found at $MANIFEST" >&2
  exit 10
fi
PACKAGE_NAME="$(sed -n 's/.*package="\([^"]*\)".*/\1/p' "$MANIFEST" | head -n1)"
if [ -z "$PACKAGE_NAME" ]; then
  ra_log "FATAL: Unable to determine package name from AndroidManifest.xml" >&2
  exit 10
fi
ra_log "Detected application package: $PACKAGE_NAME"

if ! command -v adb >/dev/null 2>&1; then
  ra_log "FATAL: adb not found on PATH" >&2
  exit 10
fi

ADB_BIN="$(command -v adb)"
"$ADB_BIN" start-server >/dev/null 2>&1 || true
"$ADB_BIN" wait-for-device
ra_log "ADB connected devices:"
"$ADB_BIN" devices -l | sed 's/^/[run-android-instrumentation-tests]   /'

# Bump the device-side logcat ring buffer before clearing it. The default on
# Android emulators is 256K-1M per buffer, which is too small for our test
# suite: a single screenshot can emit ~70 base64 chunk lines (~500 bytes
# each), and across 90+ tests the main buffer has been observed to wrap
# mid-suite, dropping a chunk line and causing `Cn1ssChunkTools` to fail
# reassembly with a gap error. 16 MiB is plenty for a single suite run and
# matches what `adb logcat -G` accepts on every supported emulator image.
# Failing to set the buffer is non-fatal — older platforms silently ignore -G.
ra_log "Bumping device logcat ring buffer to 16M (mitigates chunk-line drops)"
"$ADB_BIN" logcat -G 16M >/dev/null 2>&1 || true
ra_log "Clearing logcat buffer"
"$ADB_BIN" logcat -c || true

LOGCAT_PID=0
cleanup() {
  if [ "$LOGCAT_PID" -ne 0 ]; then
    kill "$LOGCAT_PID" >/dev/null 2>&1 || true
    wait "$LOGCAT_PID" 2>/dev/null || true
  fi
  "$ADB_BIN" shell am force-stop "$PACKAGE_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

ra_log "Capturing device logcat to $TEST_LOG"
"$ADB_BIN" logcat -v threadtime > "$TEST_LOG" 2>&1 &
LOGCAT_PID=$!
sleep 2

GRADLEW="./gradlew"
GRADLE_CMD=("$GRADLEW" --stacktrace --info --warning-mode all --no-daemon connectedDebugAndroidTest)
GRADLE_LOG="$ARTIFACTS_DIR/connectedDebugAndroidTest-gradle.log"
ANDROID_TEST_REPORT_DIR="$GRADLE_PROJECT_DIR/app/build/reports/androidTests/connected"
ANDROID_TEST_REPORT_DEST="$ARTIFACTS_DIR/android-test-report"

ra_log "Executing connectedDebugAndroidTest via Gradle"
if ! (
  cd "scripts/hellocodenameone/android/target/hellocodenameone-android-1.0-SNAPSHOT-android-source"
  JAVA_HOME="${JDK_HOME:-$JAVA17_HOME}" "${GRADLE_CMD[@]}" 2>&1 | tee "$GRADLE_LOG"
); then
  if [ -d "$ANDROID_TEST_REPORT_DIR" ]; then
    rm -rf "$ANDROID_TEST_REPORT_DEST"
    cp -R "$ANDROID_TEST_REPORT_DIR" "$ANDROID_TEST_REPORT_DEST"
    ra_log "Saved Android test report to $ANDROID_TEST_REPORT_DEST"
  else
    ra_log "Android test report directory not found at $ANDROID_TEST_REPORT_DIR"
  fi
  ra_log "FATAL: connectedDebugAndroidTest failed (see $GRADLE_LOG)"
  exit 10
fi

END_MARKER="CN1SS:SUITE:FINISHED"
# The instrumentation @Test is a trivial launcher; the CN1SS suite runs
# asynchronously in the app process, so this wait is effectively the suite's
# completion budget (the DeviceRunner emits END_MARKER when the last test
# finishes and its screenshot has been delivered). 60s left almost no margin:
# adding a couple of GPU screenshot tests (each ~several seconds of readback on
# the emulator's software rasterizer) pushed completion just past it, so the
# marker never arrived in time and the final tests' screenshots were reported
# missing. Wait long enough that suite growth and slow runners keep margin; a
# suite that finishes sooner still breaks out immediately on marker detection,
# and a genuine hang is still caught by the missing-screenshot count guard.
TIMEOUT_SECONDS=240
START_TIME="$(date +%s)"
ra_log "Waiting for DeviceRunner completion marker ($END_MARKER)"
while true; do
  if grep -q "$END_MARKER" "$TEST_LOG"; then
    ra_log "Detected DeviceRunner completion marker"
    break
  fi
  NOW="$(date +%s)"
  if [ $(( NOW - START_TIME )) -ge $TIMEOUT_SECONDS ]; then
    ra_log "STAGE:TIMEOUT -> DeviceRunner did not emit completion marker within ${TIMEOUT_SECONDS}s"
    break
  fi
  sleep 5
done

sleep 3

if [ "${CN1SS_SKIP_COVERAGE:-0}" = "1" ]; then
  ra_log "Skipping coverage report generation (CN1SS_SKIP_COVERAGE=1)"
else
  ra_log "STAGE:COVERAGE -> Collecting Jacoco coverage report"
  if ARTIFACTS_DIR="$ARTIFACTS_DIR" "$SCRIPT_DIR/generate-android-coverage-report.sh" "$GRADLE_PROJECT_DIR"; then
    if [ -f "$COVERAGE_SUMMARY" ]; then
      ra_log "  -> Coverage summary detected at $COVERAGE_SUMMARY"
    else
      ra_log "  -> Coverage summary not found after report generation"
    fi
  else
    ra_log "WARNING: Coverage report generation failed; continuing without coverage details"
  fi
fi

# The instrumentation run has finished; stop the WS server and adopt whatever
# it received. One <test>.png per delivered screenshot is in $WS_RAW_DIR. When
# WS delivered at least one image we use that set and skip the legacy logcat
# base64 decode entirely.
cn1ss_stop_ws_server
declare -a COMPARE_ENTRIES=()
# Declared here (not only inside the legacy branch below) so the post-report
# guards that reference them still work on the WS path, which skips that branch.
declare -a FAILED_TESTS=()
declare -a TEST_NAMES=()
WS_DELIVERED=0
if [ -d "${WS_RAW_DIR:-}" ]; then
  for ws_png in "$WS_RAW_DIR"/*.png; do
    [ -s "$ws_png" ] || continue
    ws_test="$(basename "$ws_png" .png)"
    ws_dest="$SCREENSHOT_TMP_DIR/${ws_test}.png"
    cp -f "$ws_png" "$ws_dest" 2>/dev/null || continue
    COMPARE_ENTRIES+=("${ws_test}=${ws_dest}")
    WS_DELIVERED=$(( WS_DELIVERED + 1 ))
  done
fi
if [ "$WS_DELIVERED" -gt 0 ]; then
  ra_log "WebSocket transport delivered ${WS_DELIVERED} screenshot(s); using WS path (logcat decode skipped)"
fi

# WebSocket is the only transport now. If it delivered nothing the on-device
# suite either never ran or produced no screenshots -- fail loudly; there is
# no logcat base64 fallback any more.
if [ "$WS_DELIVERED" -eq 0 ]; then
  ra_log "STAGE:MARKERS_NOT_FOUND -> no screenshots delivered over WebSocket"
  ra_log "---- CN1SS lines from logcat ----"
  (grep "CN1SS:" "$TEST_LOG" || true) | sed 's/^/[CN1SS] /'
  exit 12
fi

COMPARE_JSON="$SCREENSHOT_TMP_DIR/screenshot-compare.json"
SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"

BASE64_STATS_FILE="$ARTIFACTS_DIR/base64-performance-stats.txt"
extract_base64_stats "$TEST_LOG" "$BASE64_STATS_FILE"
if [ -s "$BASE64_STATS_FILE" ]; then
  ra_log "Base64 benchmark stats captured at $BASE64_STATS_FILE"
fi

export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"
export CN1SS_COMMENT_MARKER="<!-- CN1SS_ANDROID_COMMENT -->"
export CN1SS_COMMENT_LOG_PREFIX="[run-android-device-tests]"
export CN1SS_PREVIEW_SUBDIR="android"
export CN1SS_COVERAGE_SUMMARY="$COVERAGE_SUMMARY"
if [ -n "${ANDROID_COVERAGE_HTML_URL:-}" ]; then
    export CN1SS_COVERAGE_HTML_URL="${ANDROID_COVERAGE_HTML_URL}"
fi

cn1ss_process_and_report \
  "Android screenshot updates" \
  "$COMPARE_JSON" \
  "$SUMMARY_FILE" \
  "$COMMENT_FILE" \
  "$SCREENSHOT_REF_DIR" \
  "$SCREENSHOT_PREVIEW_DIR" \
  "$ARTIFACTS_DIR" \
  "${COMPARE_ENTRIES[@]}"
comment_rc=$?

# Surface any decode failures after report generation.
if [ "${#FAILED_TESTS[@]}" -gt 0 ]; then
  ra_log "ERROR: CN1SS decode failures for tests: ${FAILED_TESTS[*]}"
  comment_rc=12
fi

# Copy useful artifacts for GH Actions
cp -f "$TEST_LOG" "$ARTIFACTS_DIR/device-runner-logcat.txt" 2>/dev/null || true
[ -n "${TEST_EXEC_LOG:-}" ] && cp -f "$TEST_EXEC_LOG" "$ARTIFACTS_DIR/test-results.log" 2>/dev/null || true

exit $comment_rc
