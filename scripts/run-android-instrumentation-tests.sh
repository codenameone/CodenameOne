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

if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" ]; then
  ra_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" >&2
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
TIMEOUT_SECONDS=60
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

cn1ss_print_log "$TEST_LOG"

declare -a FAILED_TESTS=()
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
    ra_log "ERROR: Failed to extract/decode CN1SS payload for test '$test'"
    FAILED_TESTS+=("$test")
    RAW_B64_OUT="$SCREENSHOT_TMP_DIR/${test}.raw.b64"
    if cn1ss_extract_base64 "$TEST_LOG" "$test" > "$RAW_B64_OUT" 2>/dev/null; then
      if [ -s "$RAW_B64_OUT" ]; then
        head -c 64 "$RAW_B64_OUT" | sed 's/^/[CN1SS-B64-HEAD] /'
        ra_log "Partial base64 saved at: $RAW_B64_OUT"
      fi
    fi
    continue
  fi
done

# ---- Retry decode-only failures via adb relaunch -------------------------
# Logcat occasionally drops a single chunk line on the JDK 8 Android image
# (typically once per run, on a different test each time). When that happens
# the test's PNG can't be reassembled even though the CN1SS_SUITE actually
# completed successfully on-device. Rather than fail the build for what is a
# transport-layer flake, restart the already-installed APK via `adb shell am
# start` (no Gradle, no recompile, no reinstall) and re-decode just the
# tests that previously failed. The on-device suite re-emits every test;
# we only redo the work for the ones that need recovery.
#
# Why launching the main activity is enough to rerun the suite:
#   HelloCodenameOne.runApp() (the Lifecycle entry point packaged in the
#   main APK) spawns a worker thread that calls Cn1ssDeviceRunner.runSuite(),
#   which iterates every BaseTest and emits CN1SS:* markers via System.out.
#   The instrumentation runner is just a thin wrapper around the same
#   launch — we can drive it directly from adb.
#
# RIGHT FIX, KEEP IT THAT WAY:
#   * Re-install the main APK before relaunching. The Gradle
#     `connectedAndroidTest` task uninstalls BOTH the .test APK and the
#     app-under-test APK at teardown (look for two "DeviceConnector ...
#     uninstalling <pkg>" lines in the gradle log to confirm). So by the
#     time this retry block runs, the package is gone — `am start` then
#     fails with "Activity not started, unable to resolve Intent ... pkg=
#     <package>" no matter how cleanly we launch it. We sidestep that by
#     running `pm list packages` to check installation state and `adb
#     install -r $MAIN_APK` to put the APK back when missing. We don't
#     need the .test APK for the retry — the main APK contains
#     Cn1ssDeviceRunner and re-runs the entire suite when launched.
#   * Use `am start -W -a MAIN -c LAUNCHER -p $PACKAGE_NAME` to launch by
#     intent filter rather than resolving the launcher activity component
#     name. Older (`cmd package resolve-activity --brief`) returned just the
#     `pkg/.Activity` component; newer Android prepends a "priority=…" line
#     and may add leading whitespace, which broke our pattern match and
#     silently fell back to `monkey`. Filter-based launch sidesteps the
#     parsing entirely.
#   * `-W` (wait + summary) returns a `Status=ok` line we can inspect.
#   * Capture and log the output. DO NOT redirect to /dev/null and DO NOT
#     use `|| true` to silence non-zero exits — the previous version did
#     both, so when relaunch failed we got 600s of silence and zero signal
#     to debug from. If this code path goes quiet on you in the future,
#     check that nobody re-added an output redirect.
#   * Verify the app process is actually running before committing to a
#     10-minute wait. If it isn't, fail fast with a clear log line so the
#     CI surfaces the problem in seconds, not minutes.
if [ "${#FAILED_TESTS[@]}" -gt 0 ] && [ -n "${PACKAGE_NAME:-}" ]; then
  ra_log "STAGE:RETRY -> Attempting decode recovery for: ${FAILED_TESTS[*]}"

  # Stop the original logcat tail so the new capture starts cleanly. The
  # cleanup trap watches LOGCAT_PID, so reassign it to the retry tail
  # below and the trap will still kill the right process on exit.
  if [ "${LOGCAT_PID:-0}" -ne 0 ]; then
    kill "$LOGCAT_PID" >/dev/null 2>&1 || true
    wait "$LOGCAT_PID" 2>/dev/null || true
    LOGCAT_PID=0
  fi
  "$ADB_BIN" shell am force-stop "$PACKAGE_NAME" >/dev/null 2>&1 || true
  sleep 1
  "$ADB_BIN" logcat -c >/dev/null 2>&1 || true

  RETRY_LOG="$ARTIFACTS_DIR/connectedAndroidTest-retry.log"
  ra_log "Capturing retry logcat to $RETRY_LOG"
  "$ADB_BIN" logcat -v threadtime > "$RETRY_LOG" 2>&1 &
  RETRY_LOGCAT_PID=$!
  LOGCAT_PID="$RETRY_LOGCAT_PID"
  sleep 2

  # Re-install the main APK if Gradle has already uninstalled it (it
  # uninstalls both APKs at teardown — see the comment block above for the
  # diagnostic that surfaced this). Match the package name with `grep -x`
  # rather than relying on `pm list packages`'s substring filter so we
  # don't get a false positive from `<pkg>.test` or any other prefixed
  # package that happens to be present.
  if "$ADB_BIN" shell pm list packages "$PACKAGE_NAME" 2>/dev/null \
      | tr -d '\r' | grep -qx "package:$PACKAGE_NAME"; then
    ra_log "$PACKAGE_NAME already installed; skipping reinstall"
  else
    MAIN_APK="$GRADLE_PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$MAIN_APK" ]; then
      ra_log "$PACKAGE_NAME not installed; reinstalling from $MAIN_APK"
      if INSTALL_OUT="$("$ADB_BIN" install -r "$MAIN_APK" 2>&1 | tr -d '\r')"; then
        INSTALL_RC=0
      else
        INSTALL_RC=$?
      fi
      ra_log "adb install exit=${INSTALL_RC}, output:"
      printf '%s\n' "$INSTALL_OUT" | sed 's/^/[run-android-instrumentation-tests]   /'
      if [ "$INSTALL_RC" -ne 0 ]; then
        ra_log "WARN: adb install reported failure; the am start below will likely fail too"
      fi
    else
      ra_log "ERROR: cannot reinstall — APK not found at $MAIN_APK"
    fi
  fi

  # Launch by intent filter (action=MAIN, category=LAUNCHER, filtered to
  # our package). This avoids the launcher-component-resolution dance and
  # works across Android versions. `-W` blocks until the activity is up
  # and prints a Status= line we can parse.
  # `set -euo pipefail` is on for this script, so capture the exit status
  # via if/then/else; a bare `RC=$?` after the assignment would never run
  # (the script would exit at the failing pipeline). Logging both the exit
  # code and the raw output is the whole point of this block — see the
  # "RIGHT FIX" comment above.
  ra_log "Relaunching $PACKAGE_NAME via 'am start -W -a MAIN -c LAUNCHER -p $PACKAGE_NAME'"
  if AM_START_OUT="$("$ADB_BIN" shell am start -W \
      -a android.intent.action.MAIN \
      -c android.intent.category.LAUNCHER \
      -p "$PACKAGE_NAME" 2>&1 | tr -d '\r')"; then
    AM_START_RC=0
  else
    AM_START_RC=$?
  fi
  ra_log "am start exit=${AM_START_RC}, output:"
  printf '%s\n' "$AM_START_OUT" | sed 's/^/[run-android-instrumentation-tests]   /'

  AM_STATUS="$(printf '%s\n' "$AM_START_OUT" \
      | awk -F= '/^[[:space:]]*Status[[:space:]]*=/ {print $2; exit}' \
      | tr -d '[:space:]')"
  if [ "${AM_STATUS:-}" != "ok" ]; then
    ra_log "WARN: am start did not report Status=ok (got '${AM_STATUS:-<empty>}'); the app may not be running"
  fi

  # Verify the process is actually up before committing to a 10-minute
  # wait. `pidof` exits non-zero with empty output when nothing matches.
  # Try a few times because activity launch can race the first poll on
  # slower emulators.
  RETRY_PID=""
  for _attempt in 1 2 3 4 5; do
    # `pidof` exits non-zero when no match. Under pipefail+errexit that
    # would kill the script, so explicitly tolerate the empty result.
    RETRY_PID="$("$ADB_BIN" shell pidof "$PACKAGE_NAME" 2>/dev/null | tr -d '[:space:]\r')" || RETRY_PID=""
    if [ -n "$RETRY_PID" ]; then break; fi
    sleep 1
  done
  if [ -z "$RETRY_PID" ]; then
    # No process => app didn't launch. Skip the long wait so the CI
    # surfaces the failure quickly; the connectedAndroidTest-retry.log
    # artifact will contain whatever logcat captured.
    ra_log "ERROR: $PACKAGE_NAME process not detected after relaunch; skipping retry wait"
    RETRY_TIMEOUT=0
  else
    ra_log "Retry app PID: $RETRY_PID"
    RETRY_TIMEOUT=600
  fi

  RETRY_START="$(date +%s)"
  if [ "$RETRY_TIMEOUT" -gt 0 ]; then
    ra_log "Waiting up to ${RETRY_TIMEOUT}s for retry — will short-circuit as soon as every previously-failed test re-emits its CN1SS:END marker"
  fi
  while [ "$RETRY_TIMEOUT" -gt 0 ]; do
    have_all_ends=1
    for failed_test in "${FAILED_TESTS[@]}"; do
      if ! grep -q "CN1SS:END:${failed_test}" "$RETRY_LOG" 2>/dev/null; then
        have_all_ends=0
        break
      fi
    done
    if [ "$have_all_ends" = "1" ]; then
      ra_log "All previously-failed tests re-emitted CN1SS:END on retry; stopping early"
      break
    fi
    if grep -q "$END_MARKER" "$RETRY_LOG" 2>/dev/null; then
      ra_log "Detected DeviceRunner completion marker on retry"
      break
    fi
    NOW="$(date +%s)"
    if [ $(( NOW - RETRY_START )) -ge $RETRY_TIMEOUT ]; then
      ra_log "STAGE:RETRY_TIMEOUT -> retry did not emit completion marker within ${RETRY_TIMEOUT}s"
      break
    fi
    sleep 3
  done

  sleep 3
  if [ "${RETRY_LOGCAT_PID:-0}" -ne 0 ]; then
    kill "$RETRY_LOGCAT_PID" >/dev/null 2>&1 || true
    wait "$RETRY_LOGCAT_PID" 2>/dev/null || true
    RETRY_LOGCAT_PID=0
  fi

  declare -a STILL_FAILED=()
  for test in "${FAILED_TESTS[@]}"; do
    dest="$SCREENSHOT_TMP_DIR/${test}.png"
    if source_label="$(cn1ss_decode_test_png "$test" "$dest" "LOGCAT:$RETRY_LOG")"; then
      TEST_OUTPUTS["$test"]="$dest"
      TEST_SOURCES["$test"]="$source_label"
      ra_log "Retry decoded screenshot for '$test' (source=${source_label}, size: $(cn1ss_file_size "$dest") bytes)"
      preview_dest="$SCREENSHOT_PREVIEW_DIR/${test}.jpg"
      if preview_source="$(cn1ss_decode_test_preview "$test" "$preview_dest" "LOGCAT:$RETRY_LOG")"; then
        PREVIEW_OUTPUTS["$test"]="$preview_dest"
        ra_log "Retry decoded preview for '$test' (size: $(cn1ss_file_size "$preview_dest") bytes)"
      else
        rm -f "$preview_dest" 2>/dev/null || true
      fi
    else
      ra_log "Retry still failed to decode '$test'"
      STILL_FAILED+=("$test")
    fi
  done
  FAILED_TESTS=("${STILL_FAILED[@]}")
  if [ "${#FAILED_TESTS[@]}" -eq 0 ]; then
    ra_log "All decode-only failures recovered on retry"
  else
    ra_log "Tests still failing after retry: ${FAILED_TESTS[*]}"
  fi
fi

# ---- Compare against stored references ------------------------------------

COMPARE_ENTRIES=()
for test in "${TEST_NAMES[@]}"; do
  dest="${TEST_OUTPUTS[$test]:-}"
  [ -n "$dest" ] || continue
  COMPARE_ENTRIES+=("${test}=${dest}")
done

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
