#!/usr/bin/env bash
# Run instrumentation tests and reconstruct screenshot emitted as chunked Base64 (NO ADB)
set -euo pipefail

ra_log() { echo "[run-android-instrumentation-tests] $1"; }

# ---- Helpers ---------------------------------------------------------------

ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

# CN1SS helpers are implemented in Java for easier maintenance
CN1SS_MAIN_CLASS="Cn1ssChunkTools"
POST_COMMENT_CLASS="PostPrComment"
PROCESS_SCREENSHOTS_CLASS="ProcessScreenshots"
RENDER_SCREENSHOT_REPORT_CLASS="RenderScreenshotReport"

# ---- Args & environment ----------------------------------------------------

if [ $# -lt 1 ]; then
  ra_log "Usage: $0 <gradle_project_dir>" >&2
  exit 2
fi
GRADLE_PROJECT_DIR="$1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/android/tests"
if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" ]; then
  ra_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" >&2
  exit 3
fi

source "$SCRIPT_DIR/lib/cn1ss.sh"
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

ra_log "Loading workspace environment from $ENV_FILE"
[ -f "$ENV_FILE" ] || { ra_log "Missing env file: $ENV_FILE"; exit 3; }
# shellcheck disable=SC1090
source "$ENV_FILE"

if [ -z "${JAVA17_HOME:-}" ]; then
  ra_log "JAVA17_HOME not set in workspace environment" >&2
  exit 3
fi

JAVA17_BIN="$JAVA17_HOME/bin/java"
if [ ! -x "$JAVA17_BIN" ]; then
  ra_log "JDK 17 java binary missing at $JAVA17_BIN" >&2
  exit 3
fi

cn1ss_setup "$JAVA17_BIN" "$CN1SS_HELPER_SOURCE_DIR"

[ -d "$GRADLE_PROJECT_DIR" ] || { ra_log "Gradle project directory not found: $GRADLE_PROJECT_DIR"; exit 4; }
[ -x "$GRADLE_PROJECT_DIR/gradlew" ] || chmod +x "$GRADLE_PROJECT_DIR/gradlew"

# ---- Prepare app + emulator state -----------------------------------------

APK_PATH="${2:-}"
if [ -z "$APK_PATH" ]; then
  APK_PATH="$(find "$GRADLE_PROJECT_DIR" -type f -path '*/outputs/apk/debug/*.apk' | head -n 1 || true)"
fi
if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
  ra_log "FATAL: Unable to locate debug APK under $GRADLE_PROJECT_DIR" >&2
  exit 10
fi
ra_log "Using APK: $APK_PATH"

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

ra_log "Installing APK onto device"
"$ADB_BIN" shell am force-stop "$PACKAGE_NAME" >/dev/null 2>&1 || true
"$ADB_BIN" uninstall "$PACKAGE_NAME" >/dev/null 2>&1 || true
if ! "$ADB_BIN" install -r "$APK_PATH"; then
  ra_log "adb install failed; retrying after explicit uninstall"
  "$ADB_BIN" uninstall "$PACKAGE_NAME" >/dev/null 2>&1 || true
  if ! "$ADB_BIN" install "$APK_PATH"; then
    ra_log "FATAL: adb install failed after retry"
    exit 10
  fi
fi

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

ra_log "Launching Codename One DeviceRunner"
"$ADB_BIN" shell pm clear "$PACKAGE_NAME" >/dev/null 2>&1 || true
if ! "$ADB_BIN" shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; then
  ra_log "monkey launch failed; attempting am start fallback"
  MAIN_ACTIVITY="$("$ADB_BIN" shell cmd package resolve-activity --brief "$PACKAGE_NAME" 2>/dev/null | head -n 1 | tr -d '\r' | sed 's/ .*//')"
  if [[ "$MAIN_ACTIVITY" == */* ]]; then
    if ! "$ADB_BIN" shell am start -n "$MAIN_ACTIVITY" >/dev/null 2>&1; then
      ra_log "FATAL: Failed to start application via am start"
      exit 10
    fi
  else
    ra_log "FATAL: Unable to determine launchable activity"
    exit 10
  fi
fi

END_MARKER="CN1SS:SUITE:FINISHED"
TIMEOUT_SECONDS=300
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

declare -a CN1SS_SOURCES=("LOGCAT:$TEST_LOG")


# ---- Chunk accounting (diagnostics) ---------------------------------------

LOGCAT_CHUNKS="$(cn1ss_count_chunks "$TEST_LOG")"
LOGCAT_CHUNKS="${LOGCAT_CHUNKS//[^0-9]/}"; : "${LOGCAT_CHUNKS:=0}"

ra_log "Chunk counts -> logcat: ${LOGCAT_CHUNKS}"

if [ "${LOGCAT_CHUNKS:-0}" = "0" ]; then
  ra_log "STAGE:MARKERS_NOT_FOUND -> DeviceRunner output did not include CN1SS chunks"
  ra_log "---- CN1SS lines from logcat ----"
  (grep "CN1SS:" "$TEST_LOG" || true) | sed 's/^/[CN1SS] /'
  exit 12
fi

# ---- Identify CN1SS test streams -----------------------------------------

TEST_NAMES_RAW="$(cn1ss_list_tests "$TEST_LOG" 2>/dev/null | awk 'NF' | sort -u || true)"
declare -a TEST_NAMES=()
if [ -n "$TEST_NAMES_RAW" ]; then
  while IFS= read -r name; do
    [ -n "$name" ] || continue
    TEST_NAMES+=("$name")
  done <<< "$TEST_NAMES_RAW"
else
  TEST_NAMES+=("default")
fi
ra_log "Detected CN1SS test streams: ${TEST_NAMES[*]}"

declare -A TEST_OUTPUTS=()
declare -A TEST_SOURCES=()
declare -A PREVIEW_OUTPUTS=()

ensure_dir "$SCREENSHOT_PREVIEW_DIR"

for test in "${TEST_NAMES[@]}"; do
  dest="$SCREENSHOT_TMP_DIR/${test}.png"
  if source_label="$(cn1ss_decode_test_png "$test" "$dest" "${CN1SS_SOURCES[@]}")"; then
    TEST_OUTPUTS["$test"]="$dest"
    TEST_SOURCES["$test"]="$source_label"
    ra_log "Decoded screenshot for '$test' (source=${source_label}, size: $(cn1ss_file_size "$dest") bytes)"
    preview_dest="$SCREENSHOT_PREVIEW_DIR/${test}.jpg"
    if preview_source="$(cn1ss_decode_test_preview "$test" "$preview_dest" "${CN1SS_SOURCES[@]}")"; then
      PREVIEW_OUTPUTS["$test"]="$preview_dest"
      ra_log "Decoded preview for '$test' (source=${preview_source}, size: $(cn1ss_file_size "$preview_dest") bytes)"
    else
      rm -f "$preview_dest" 2>/dev/null || true
    fi
  else
    ra_log "FATAL: Failed to extract/decode CN1SS payload for test '$test'"
    RAW_B64_OUT="$SCREENSHOT_TMP_DIR/${test}.raw.b64"
    if cn1ss_extract_base64 "$TEST_LOG" "$test" > "$RAW_B64_OUT" 2>/dev/null; then
      if [ -s "$RAW_B64_OUT" ]; then
        head -c 64 "$RAW_B64_OUT" | sed 's/^/[CN1SS-B64-HEAD] /'
        ra_log "Partial base64 saved at: $RAW_B64_OUT"
      fi
    fi
    exit 12
  fi
done

# ---- Compare against stored references ------------------------------------

COMPARE_ARGS=()
for test in "${TEST_NAMES[@]}"; do
  dest="${TEST_OUTPUTS[$test]:-}"
  [ -n "$dest" ] || continue
  COMPARE_ARGS+=("--actual" "${test}=${dest}")
done

COMPARE_JSON="$SCREENSHOT_TMP_DIR/screenshot-compare.json"
export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"
ra_log "STAGE:COMPARE -> Evaluating screenshots against stored references"
if ! cn1ss_java_run "$PROCESS_SCREENSHOTS_CLASS" \
  --reference-dir "$SCREENSHOT_REF_DIR" \
  --emit-base64 \
  --preview-dir "$SCREENSHOT_PREVIEW_DIR" \
  "${COMPARE_ARGS[@]}" > "$COMPARE_JSON"; then
  ra_log "FATAL: Screenshot comparison helper failed"
  exit 13
fi

SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"

ra_log "STAGE:COMMENT_BUILD -> Rendering summary and PR comment markdown"
if ! cn1ss_java_run "$RENDER_SCREENSHOT_REPORT_CLASS" \
  --compare-json "$COMPARE_JSON" \
  --comment-out "$COMMENT_FILE" \
  --summary-out "$SUMMARY_FILE"; then
  ra_log "FATAL: Failed to render screenshot summary/comment"
  exit 14
fi

if [ -s "$SUMMARY_FILE" ]; then
  ra_log "  -> Wrote summary entries to $SUMMARY_FILE ($(wc -l < "$SUMMARY_FILE" 2>/dev/null || echo 0) line(s))"
else
  ra_log "  -> No summary entries generated (all screenshots matched stored baselines)"
fi

if [ -s "$COMMENT_FILE" ]; then
  ra_log "  -> Prepared PR comment payload at $COMMENT_FILE (bytes=$(wc -c < "$COMMENT_FILE" 2>/dev/null || echo 0))"
else
  ra_log "  -> No PR comment content produced"
fi

if [ -s "$SUMMARY_FILE" ]; then
  while IFS='|' read -r status test message copy_flag path preview_note; do
    [ -n "${test:-}" ] || continue
    ra_log "Test '${test}': ${message}"
    if [ "$copy_flag" = "1" ] && [ -n "${path:-}" ] && [ -f "$path" ]; then
      cp -f "$path" "$ARTIFACTS_DIR/${test}.png" 2>/dev/null || true
      ra_log "  -> Stored PNG artifact copy at $ARTIFACTS_DIR/${test}.png"
    fi
    if [ "$status" = "equal" ] && [ -n "${path:-}" ]; then
      rm -f "$path" 2>/dev/null || true
    fi
    if [ -n "${preview_note:-}" ]; then
      ra_log "  Preview note: ${preview_note}"
    fi
  done < "$SUMMARY_FILE"
fi

cp -f "$COMPARE_JSON" "$ARTIFACTS_DIR/screenshot-compare.json" 2>/dev/null || true
if [ -s "$COMMENT_FILE" ]; then
  cp -f "$COMMENT_FILE" "$ARTIFACTS_DIR/screenshot-comment.md" 2>/dev/null || true
fi

ra_log "STAGE:COMMENT_POST -> Submitting PR feedback"
comment_rc=0
export CN1SS_COMMENT_MARKER="<!-- CN1SS_ANDROID_COMMENT -->"
export CN1SS_COMMENT_LOG_PREFIX="[run-android-device-tests]"
export CN1SS_PREVIEW_SUBDIR="android"
if ! cn1ss_post_pr_comment "$COMMENT_FILE" "$SCREENSHOT_PREVIEW_DIR"; then
  comment_rc=$?
fi

# Copy useful artifacts for GH Actions
cp -f "$TEST_LOG" "$ARTIFACTS_DIR/device-runner-logcat.txt" 2>/dev/null || true
[ -n "${TEST_EXEC_LOG:-}" ] && cp -f "$TEST_EXEC_LOG" "$ARTIFACTS_DIR/test-results.log" 2>/dev/null || true

exit $comment_rc
