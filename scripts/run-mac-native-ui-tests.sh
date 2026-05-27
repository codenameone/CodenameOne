#!/usr/bin/env bash
# Run Codename One UI screenshot tests on a Mac native (Mac Catalyst) build
# and compare against goldens. Mirrors scripts/run-ios-ui-tests.sh, but the
# Mac slice runs as a native process on the host so there is no simulator
# to boot / install / launch -- xcodebuild produces a .app under
# Build/Products/Debug-maccatalyst and we exec the binary directly.
set -euo pipefail

rm_log() { echo "[run-mac-native-ui-tests] $1"; }

ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

extract_base64_stats() {
  local out_file="$1"
  shift

  local log_file lines found=0
  : > "$out_file"
  for log_file in "$@"; do
    [ -f "$log_file" ] || continue
    lines="$(grep 'CN1SS:STAT:' "$log_file" 2>/dev/null | sed -E 's/^.*CN1SS:STAT://')" || true
    if [ -z "${lines:-}" ]; then
      continue
    fi
    found=1
    while IFS= read -r line; do
      [ -n "$line" ] || continue
      echo "$line" >> "$out_file"
    done <<< "$lines"
  done

  if [ "$found" -eq 1 ] && [ -f "$out_file" ]; then
    awk '!seen[$0]++' "$out_file" > "$out_file.tmp" && mv "$out_file.tmp" "$out_file"
  else
    rm -f "$out_file"
  fi
}

if [ $# -lt 1 ]; then
  rm_log "Usage: $0 <workspace_path> [app_bundle] [scheme]" >&2
  exit 2
fi

WORKSPACE_PATH="$1"
APP_BUNDLE_PATH="${2:-}"
REQUESTED_SCHEME="${3:-}"

if [ -n "$APP_BUNDLE_PATH" ] && [ ! -d "$APP_BUNDLE_PATH" ] && [ -z "$REQUESTED_SCHEME" ]; then
  REQUESTED_SCHEME="$APP_BUNDLE_PATH"
  APP_BUNDLE_PATH=""
fi

if [ ! -d "$WORKSPACE_PATH" ]; then
  rm_log "Xcode workspace/project not found at $WORKSPACE_PATH" >&2
  exit 3
fi

XCODE_CONTAINER_FLAG="-workspace"
if [[ "$WORKSPACE_PATH" == *.xcodeproj ]]; then
  XCODE_CONTAINER_FLAG="-project"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/common/java"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/cn1ss.sh"

if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" ]; then
  rm_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" >&2
  exit 3
fi
cn1ss_log() { rm_log "$1"; }

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

rm_log "Loading workspace environment from $ENV_FILE"
[ -f "$ENV_FILE" ] || { rm_log "Missing env file: $ENV_FILE"; exit 3; }
# shellcheck disable=SC1090
source "$ENV_FILE"

# Pin Xcode 26 for CI validation. The Mac Catalyst slice needs the macOS
# SDK 26+ headers / Metal Toolchain to compile.
if [ -z "${XCODE_APP:-}" ]; then
  XCODE_APP="$(ls -d /Applications/Xcode_26*.app 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [ ! -x "$XCODE_APP/Contents/Developer/usr/bin/xcodebuild" ]; then
  rm_log "Xcode 26 not found. Set XCODE_APP to an installed Xcode 26 app bundle path." >&2
  exit 3
fi
export DEVELOPER_DIR="$XCODE_APP/Contents/Developer"
export XCODEBUILD="$DEVELOPER_DIR/usr/bin/xcodebuild"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"
rm_log "Using DEVELOPER_DIR=$DEVELOPER_DIR"
rm_log "Using XCODEBUILD=$XCODEBUILD"

if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  rm_log "JAVA17_HOME not set correctly" >&2
  exit 3
fi
if ! command -v xcodebuild >/dev/null 2>&1; then
  rm_log "xcodebuild not found" >&2
  exit 3
fi

JAVA17_BIN="$JAVA17_HOME/bin/java"
cn1ss_setup "$JAVA17_BIN" "$CN1SS_HELPER_SOURCE_DIR"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
mkdir -p "$ARTIFACTS_DIR"
TEST_LOG="$ARTIFACTS_DIR/device-runner.log"
FALLBACK_LOG="$ARTIFACTS_DIR/device-runner-fallback.log"

if [ -z "$REQUESTED_SCHEME" ]; then
  if [[ "$WORKSPACE_PATH" == *.xcworkspace ]]; then
    REQUESTED_SCHEME="$(basename "$WORKSPACE_PATH" .xcworkspace)"
  elif [[ "$WORKSPACE_PATH" == *.xcodeproj ]]; then
    REQUESTED_SCHEME="$(basename "$WORKSPACE_PATH" .xcodeproj)"
  else
    REQUESTED_SCHEME="$(basename "$WORKSPACE_PATH")"
  fi
fi
SCHEME="$REQUESTED_SCHEME"
rm_log "Using scheme $SCHEME"

# Golden-image directory defaults to scripts/mac-native/screenshots/.
# Override via SCREENSHOT_REF_DIR for parallel rendering backends or local
# experimentation. See scripts/mac-native/screenshots/README.md.
if [ -n "${SCREENSHOT_REF_DIR:-}" ]; then
  if [ ! -d "$SCREENSHOT_REF_DIR" ]; then
    rm_log "SCREENSHOT_REF_DIR override '$SCREENSHOT_REF_DIR' is not a directory" >&2
    exit 3
  fi
  SCREENSHOT_REF_DIR="$(cd "$SCREENSHOT_REF_DIR" && pwd)"
  rm_log "Using screenshot reference dir from SCREENSHOT_REF_DIR: $SCREENSHOT_REF_DIR"
else
  SCREENSHOT_REF_DIR="$REPO_ROOT/scripts/mac-native/screenshots"
fi
ensure_dir "$SCREENSHOT_REF_DIR"

SCREENSHOT_TMP_DIR="$(mktemp -d "${TMPDIR}/cn1-mac-native-tests-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1-mac-native-tests")"
SCREENSHOT_RAW_DIR="$SCREENSHOT_TMP_DIR/raw"
SCREENSHOT_PREVIEW_DIR="$SCREENSHOT_TMP_DIR/previews"
mkdir -p "$SCREENSHOT_RAW_DIR" "$SCREENSHOT_PREVIEW_DIR"

export CN1SS_OUTPUT_DIR="$SCREENSHOT_RAW_DIR"
export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"

# Patch CN1SS_* placeholders in the shared scheme (the iOS pipeline does
# this too -- the placeholders come from create-shared-scheme.py). Mac
# binaries are launched outside Xcode, so the env vars are also exported
# explicitly below; the scheme patch is kept for parity / debugging-from-IDE.
SCHEME_FILE="$WORKSPACE_PATH/xcshareddata/xcschemes/$SCHEME.xcscheme"
if [ ! -f "$SCHEME_FILE" ] && [[ "$WORKSPACE_PATH" == *.xcworkspace ]]; then
  PROJECT_DIR="$(cd "$(dirname "$WORKSPACE_PATH")" && pwd)"
  PROJECT_SCHEME_FILE="$PROJECT_DIR/$(basename "$WORKSPACE_PATH" .xcworkspace).xcodeproj/xcshareddata/xcschemes/$SCHEME.xcscheme"
  if [ -f "$PROJECT_SCHEME_FILE" ]; then
    SCHEME_FILE="$PROJECT_SCHEME_FILE"
  fi
fi
if [ -f "$SCHEME_FILE" ]; then
  if sed --version >/dev/null 2>&1; then
    sed -i -e "s|__CN1SS_OUTPUT_DIR__|$SCREENSHOT_RAW_DIR|g" \
           -e "s|__CN1SS_PREVIEW_DIR__|$SCREENSHOT_PREVIEW_DIR|g" "$SCHEME_FILE"
  else
    sed -i '' -e "s|__CN1SS_OUTPUT_DIR__|$SCREENSHOT_RAW_DIR|g" \
              -e "s|__CN1SS_PREVIEW_DIR__|$SCREENSHOT_PREVIEW_DIR|g" "$SCHEME_FILE"
  fi
  rm_log "Injected CN1SS_* envs into scheme: $SCHEME_FILE"
fi

HOST_ARCH="$(uname -m 2>/dev/null || echo arm64)"
case "$HOST_ARCH" in
  arm64|x86_64) BUILD_ARCH="$HOST_ARCH" ;;
  *) BUILD_ARCH="arm64" ;;
esac

DERIVED_DATA_DIR="$SCREENSHOT_TMP_DIR/derived"
rm -rf "$DERIVED_DATA_DIR"
BUILD_LOG="$ARTIFACTS_DIR/xcodebuild-build.log"

# Mac Catalyst destination + configuration. CODE_SIGN_* are disabled so
# unsigned local / CI runs don't require provisioning. The macNative
# entitlements file is still set via CODE_SIGN_ENTITLEMENTS on the project
# (IPhoneBuilder injects it), but with signing disabled it is a no-op.
rm_log "Building Mac Catalyst app with xcodebuild"
COMPILE_START=$(date +%s)
XCODE_BUILD_CMD=(
  xcodebuild
  "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH"
  -scheme "$SCHEME"
  -configuration Debug
  -destination 'platform=macOS,variant=Mac Catalyst'
  -destination-timeout 120
  -derivedDataPath "$DERIVED_DATA_DIR"
  "ARCHS=$BUILD_ARCH"
  "ONLY_ACTIVE_ARCH=YES"
  "CODE_SIGN_IDENTITY="
  "CODE_SIGNING_REQUIRED=NO"
  "CODE_SIGNING_ALLOWED=NO"
  build
)
if ! "${XCODE_BUILD_CMD[@]}" | tee "$BUILD_LOG"; then
  rm_log "STAGE:XCODE_BUILD_FAILED -> See $BUILD_LOG"
  exit 10
fi
COMPILE_END=$(date +%s)
COMPILATION_TIME=$((COMPILE_END - COMPILE_START))
rm_log "Compilation time: ${COMPILATION_TIME}s"

# Locate the produced .app under DerivedData. Mac Catalyst products land
# under Debug-maccatalyst/ (vs Debug-iphonesimulator/ on iOS).
BUILD_SETTINGS="$(xcodebuild "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH" -scheme "$SCHEME" -configuration Debug -destination 'platform=macOS,variant=Mac Catalyst' -showBuildSettings 2>/dev/null || true)"
WRAPPER_NAME="$(printf '%s\n' "$BUILD_SETTINGS" | awk -F' = ' '/ WRAPPER_NAME /{print $2; exit}' | tr -d ' ')"
if [ -z "$WRAPPER_NAME" ]; then
  WRAPPER_NAME="${SCHEME}.app"
fi
if [ -z "$APP_BUNDLE_PATH" ]; then
  CANDIDATE_BUNDLE="$DERIVED_DATA_DIR/Build/Products/Debug-maccatalyst/$WRAPPER_NAME"
  if [ -d "$CANDIDATE_BUNDLE" ]; then
    APP_BUNDLE_PATH="$CANDIDATE_BUNDLE"
  fi
fi
if [ -z "$APP_BUNDLE_PATH" ]; then
  CANDIDATE_BUNDLE="$(find "$DERIVED_DATA_DIR" -path "*/Debug-maccatalyst/$WRAPPER_NAME" -type d -print -quit 2>/dev/null || true)"
  if [ -d "$CANDIDATE_BUNDLE" ]; then
    APP_BUNDLE_PATH="$CANDIDATE_BUNDLE"
  fi
fi
if [ -z "$APP_BUNDLE_PATH" ] || [ ! -d "$APP_BUNDLE_PATH" ]; then
  rm_log "FATAL: Mac Catalyst app bundle not found under $DERIVED_DATA_DIR (looking for $WRAPPER_NAME)"
  find "$DERIVED_DATA_DIR/Build/Products" -maxdepth 2 -type d -print >&2 2>/dev/null || true
  exit 11
fi
rm_log "Found Mac Catalyst app bundle at $APP_BUNDLE_PATH"

APP_PROCESS_NAME="${WRAPPER_NAME%.app}"
APP_EXECUTABLE="$APP_BUNDLE_PATH/Contents/MacOS/$APP_PROCESS_NAME"
if [ ! -x "$APP_EXECUTABLE" ]; then
  rm_log "FATAL: Mac Catalyst executable not found at $APP_EXECUTABLE"
  ls -l "$APP_BUNDLE_PATH/Contents/MacOS/" >&2 2>/dev/null || true
  exit 11
fi
BUNDLE_IDENTIFIER="$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$APP_BUNDLE_PATH/Contents/Info.plist" 2>/dev/null || true)"
if [ -z "$BUNDLE_IDENTIFIER" ]; then
  rm_log "Warning: could not read CFBundleIdentifier from $APP_BUNDLE_PATH"
fi
rm_log "App bundle id: ${BUNDLE_IDENTIFIER:-<unknown>}"
rm_log "App executable: $APP_EXECUTABLE"

# Mac NSLog output goes to the unified log when running as a foreground
# process from a terminal, but the Codename One screenshot helper (CN1SS)
# emits the chunked PNG payloads via printf / fprintf to stdout. We
# capture both stdout and stderr to be safe, and also start a `log
# stream` predicate filter as a fallback in case the runtime decides to
# route some lines to os_log only (Catalyst behaviour drifts between SDKs).
APP_PID=0
LOG_STREAM_PID=0
cleanup() {
  if [ "$LOG_STREAM_PID" -ne 0 ]; then
    kill "$LOG_STREAM_PID" >/dev/null 2>&1 || true
    wait "$LOG_STREAM_PID" 2>/dev/null || true
  fi
  if [ "$APP_PID" -ne 0 ]; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    # SIGTERM doesn't always reach a Catalyst NSApplication-bridged main
    # loop quickly; give it 3s then KILL.
    for _ in 1 2 3; do
      if ! kill -0 "$APP_PID" >/dev/null 2>&1; then break; fi
      sleep 1
    done
    kill -KILL "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

rm_log "Starting unified log stream (fallback capture)"
if [ -n "$BUNDLE_IDENTIFIER" ]; then
  log stream --style compact --level debug \
    --predicate "(subsystem == \"$BUNDLE_IDENTIFIER\") OR (composedMessage CONTAINS \"CN1SS\") OR (eventMessage CONTAINS \"CN1SS\")" \
    > "$FALLBACK_LOG" 2>&1 &
else
  log stream --style compact --level debug \
    --predicate "(composedMessage CONTAINS \"CN1SS\") OR (eventMessage CONTAINS \"CN1SS\")" \
    > "$FALLBACK_LOG" 2>&1 &
fi
LOG_STREAM_PID=$!
sleep 1

rm_log "Launching Mac Catalyst app: $APP_EXECUTABLE"
LAUNCH_START=$(date +%s)
(
  # Per-process environment for the launched binary. CN1SS_OUTPUT_DIR /
  # CN1SS_PREVIEW_DIR are read by the screenshot helper. Disable the Mac
  # window restoration so each run starts clean.
  export CN1SS_OUTPUT_DIR
  export CN1SS_PREVIEW_DIR
  export NSWindowRestoresAlerts=0
  export NSDisableAppNapAssertions=1
  "$APP_EXECUTABLE" > "$TEST_LOG" 2>&1
) &
APP_PID=$!
LAUNCH_END=$(date +%s)
echo "App Launch : $(( (LAUNCH_END - LAUNCH_START) * 1000 )) ms" >> "$ARTIFACTS_DIR/mac-test-stats.txt"

END_MARKER="CN1SS:SUITE:FINISHED"
TIMEOUT_SECONDS="${CN1SS_SUITE_TIMEOUT_SECONDS:-1500}"
START_TIME="$(date +%s)"
rm_log "Waiting for DeviceRunner completion marker ($END_MARKER) -- timeout ${TIMEOUT_SECONDS}s"
while true; do
  if [ -s "$TEST_LOG" ] && grep -q "$END_MARKER" "$TEST_LOG"; then
    rm_log "Detected completion marker in stdout log"
    break
  fi
  if [ -s "$FALLBACK_LOG" ] && grep -q "$END_MARKER" "$FALLBACK_LOG"; then
    rm_log "Detected completion marker in unified log fallback"
    break
  fi
  # Bail out early if the app crashed (PID gone before marker arrived).
  if ! kill -0 "$APP_PID" >/dev/null 2>&1; then
    rm_log "App process exited before completion marker -- check $TEST_LOG"
    break
  fi
  NOW="$(date +%s)"
  if [ $(( NOW - START_TIME )) -ge $TIMEOUT_SECONDS ]; then
    rm_log "STAGE:TIMEOUT -> DeviceRunner did not emit completion marker within ${TIMEOUT_SECONDS}s"
    break
  fi
  sleep 5
done
END_TIME=$(date +%s)
echo "Test Execution : $(( (END_TIME - START_TIME) * 1000 )) ms" >> "$ARTIFACTS_DIR/mac-test-stats.txt"

sleep 2

# Drain the unified-log fallback before tearing it down.
kill "$LOG_STREAM_PID" >/dev/null 2>&1 || true
wait "$LOG_STREAM_PID" 2>/dev/null || true
LOG_STREAM_PID=0

# Belt-and-suspenders: run `log show` for the last 30 minutes filtered to
# CN1SS so any late messages that the stream didn't get are captured.
LATE_FALLBACK_LOG="$ARTIFACTS_DIR/device-runner-late-fallback.log"
log show --style syslog --last 30m \
  --predicate "(composedMessage CONTAINS \"CN1SS\") OR (eventMessage CONTAINS \"CN1SS\")" \
  > "$LATE_FALLBACK_LOG" 2>/dev/null || true

BASE64_STATS_FILE="$ARTIFACTS_DIR/base64-performance-stats.txt"
extract_base64_stats "$BASE64_STATS_FILE" "$TEST_LOG" "$FALLBACK_LOG" "$LATE_FALLBACK_LOG"
if [ -s "$BASE64_STATS_FILE" ]; then
  rm_log "Base64 benchmark stats captured at $BASE64_STATS_FILE"
fi

# Tear down the app process if it's still running (it sometimes is,
# especially when the test suite finishes but the NSApplication run loop
# keeps the process alive until SIGTERM).
if kill -0 "$APP_PID" >/dev/null 2>&1; then
  kill "$APP_PID" >/dev/null 2>&1 || true
fi
wait "$APP_PID" 2>/dev/null || true
APP_PID=0

# Aggregate CN1SS sources in priority order: stdout (richest -- chunked
# PNGs are emitted there) first, then the unified-log streams.
declare -a CN1SS_SOURCES=("SIMLOG:$TEST_LOG" "SIMLOG:$FALLBACK_LOG" "SIMLOG:$LATE_FALLBACK_LOG")

# Find the source with the most chunks for diagnostic logging.
LOG_CHUNKS="$(cn1ss_count_chunks "$TEST_LOG")"; LOG_CHUNKS="${LOG_CHUNKS//[^0-9]/}"; : "${LOG_CHUNKS:=0}"
FALLBACK_CHUNKS="$(cn1ss_count_chunks "$FALLBACK_LOG")"; FALLBACK_CHUNKS="${FALLBACK_CHUNKS//[^0-9]/}"; : "${FALLBACK_CHUNKS:=0}"
LATE_CHUNKS="$(cn1ss_count_chunks "$LATE_FALLBACK_LOG")"; LATE_CHUNKS="${LATE_CHUNKS//[^0-9]/}"; : "${LATE_CHUNKS:=0}"
rm_log "Chunk counts -> stdout: ${LOG_CHUNKS}, log-stream: ${FALLBACK_CHUNKS}, log-show: ${LATE_CHUNKS}"

if [ "${LOG_CHUNKS:-0}" = "0" ] && [ "${FALLBACK_CHUNKS:-0}" = "0" ] && [ "${LATE_CHUNKS:-0}" = "0" ]; then
  rm_log "STAGE:MARKERS_NOT_FOUND -> no CN1SS chunks captured"
  rm_log "---- last 50 lines of stdout log ----"
  tail -n 50 "$TEST_LOG" 2>/dev/null | sed 's/^/[STDOUT] /' || true
  rm_log "---- last 50 lines of unified log fallback ----"
  tail -n 50 "$FALLBACK_LOG" 2>/dev/null | sed 's/^/[OSLOG] /' || true
  exit 12
fi

# Pick whichever source has chunks for the test enumeration. The decoder
# iterates over all sources for each test, so this is just to seed
# TEST_NAMES.
PRIMARY_LOG="$TEST_LOG"
if [ "${LOG_CHUNKS:-0}" = "0" ]; then
  if [ "${FALLBACK_CHUNKS:-0}" != "0" ]; then PRIMARY_LOG="$FALLBACK_LOG"; else PRIMARY_LOG="$LATE_FALLBACK_LOG"; fi
fi

TEST_NAMES_RAW="$(cn1ss_list_tests "$PRIMARY_LOG" 2>/dev/null | awk 'NF' | sort -u || true)"
declare -a TEST_NAMES=()
if [ -n "$TEST_NAMES_RAW" ]; then
  while IFS= read -r name; do
    [ -n "$name" ] || continue
    TEST_NAMES+=("$name")
  done <<< "$TEST_NAMES_RAW"
else
  TEST_NAMES+=("default")
fi
rm_log "Detected CN1SS test streams: ${TEST_NAMES[*]}"

PAIR_SEP=$'\037'
declare -a TEST_OUTPUT_ENTRIES=()

ensure_dir "$SCREENSHOT_PREVIEW_DIR"

for test in "${TEST_NAMES[@]}"; do
  dest="$SCREENSHOT_TMP_DIR/${test}.png"
  if source_label="$(cn1ss_decode_test_png "$test" "$dest" "${CN1SS_SOURCES[@]}")"; then
    TEST_OUTPUT_ENTRIES+=("${test}${PAIR_SEP}${dest}")
    rm_log "Decoded screenshot for '$test' (source=${source_label}, size: $(cn1ss_file_size "$dest") bytes)"
    preview_dest="$SCREENSHOT_PREVIEW_DIR/${test}.jpg"
    if preview_source="$(cn1ss_decode_test_preview "$test" "$preview_dest" "${CN1SS_SOURCES[@]}")"; then
      rm_log "Decoded preview for '$test' (source=${preview_source}, size: $(cn1ss_file_size "$preview_dest") bytes)"
    else
      rm -f "$preview_dest" 2>/dev/null || true
    fi
  else
    rm_log "FATAL: Failed to extract/decode CN1SS payload for test '$test'"
    RAW_B64_OUT="$SCREENSHOT_TMP_DIR/${test}.raw.b64"
    {
      for entry in "${CN1SS_SOURCES[@]}"; do
        path="${entry#*:}"
        [ -s "$path" ] || continue
        count="$(cn1ss_count_chunks "$path" "$test")"; count="${count//[^0-9]/}"; : "${count:=0}"
        if [ "$count" -gt 0 ]; then cn1ss_extract_base64 "$path" "$test"; fi
      done
    } > "$RAW_B64_OUT" 2>/dev/null || true
    if [ -s "$RAW_B64_OUT" ]; then
      head -c 64 "$RAW_B64_OUT" | sed 's/^/[CN1SS-B64-HEAD] /'
      rm_log "Partial base64 saved at: $RAW_B64_OUT"
    fi
    exit 12
  fi
done

lookup_test_output() {
  local key="$1" entry prefix
  for entry in "${TEST_OUTPUT_ENTRIES[@]}"; do
    prefix="${entry%%$PAIR_SEP*}"
    if [ "$prefix" = "$key" ]; then
      echo "${entry#*$PAIR_SEP}"
      return 0
    fi
  done
  return 1
}

COMPARE_ENTRIES=()
for test in "${TEST_NAMES[@]}"; do
  if dest="$(lookup_test_output "$test")"; then
    [ -n "$dest" ] || continue
    COMPARE_ENTRIES+=("${test}=${dest}")
  fi
done

COMPARE_JSON="$SCREENSHOT_TMP_DIR/screenshot-compare.json"
SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"

export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"
# Distinct PR-comment marker / preview path / title so this job posts its
# own comment instead of overwriting the iOS / iOS Metal job's comment.
export CN1SS_COMMENT_MARKER="${CN1SS_COMMENT_MARKER:-<!-- CN1SS_MAC_NATIVE_COMMENT -->}"
export CN1SS_COMMENT_LOG_PREFIX="${CN1SS_COMMENT_LOG_PREFIX:-[run-mac-native-ui-tests]}"
export CN1SS_PREVIEW_SUBDIR="${CN1SS_PREVIEW_SUBDIR:-mac-native}"
export CN1SS_SUCCESS_MESSAGE="${CN1SS_SUCCESS_MESSAGE:-✅ Native Mac screenshot tests passed.}"
REPORT_TITLE="${CN1SS_REPORT_TITLE:-Mac native screenshot updates}"

CN1SS_VM_TIME=0
if [ -f "$ARTIFACTS_DIR/vm_time.txt" ]; then
  CN1SS_VM_TIME=$(cat "$ARTIFACTS_DIR/vm_time.txt")
  rm_log "Loaded VM translation time: ${CN1SS_VM_TIME}s"
fi
export CN1SS_VM_TIME
export CN1SS_COMPILATION_TIME="$COMPILATION_TIME"

cn1ss_process_and_report \
  "$REPORT_TITLE" \
  "$COMPARE_JSON" \
  "$SUMMARY_FILE" \
  "$COMMENT_FILE" \
  "$SCREENSHOT_REF_DIR" \
  "$SCREENSHOT_PREVIEW_DIR" \
  "$ARTIFACTS_DIR" \
  "${COMPARE_ENTRIES[@]}"
comment_rc=$?

cp -f "$BUILD_LOG" "$ARTIFACTS_DIR/xcodebuild-build.log" 2>/dev/null || true
cp -f "$TEST_LOG" "$ARTIFACTS_DIR/device-runner.log" 2>/dev/null || true

# Guard: the suite must produce at least this many screenshots. Matches
# the iOS pipeline's CN1SS_MIN_SCREENSHOTS so a regression that crashes
# the app early surfaces here too. Defaults to 0 on the first run so the
# goldens dir can be seeded without immediately failing CI.
MIN_SCREENSHOTS="${CN1SS_MIN_SCREENSHOTS:-0}"
if [ -s "$COMPARE_JSON" ]; then
  ACTUAL_COUNT="$(python3 -c "import json,sys
try:
    with open(sys.argv[1]) as f:
        d = json.load(f)
    print(len(d.get('results', [])))
except Exception as e:
    print(0)" "$COMPARE_JSON" 2>/dev/null || echo 0)"
else
  ACTUAL_COUNT=0
fi
if [ "$ACTUAL_COUNT" -lt "$MIN_SCREENSHOTS" ]; then
  rm_log "STAGE:SCREENSHOT_COUNT_REGRESSION -> got $ACTUAL_COUNT, expected >= $MIN_SCREENSHOTS"
  exit 17
fi
rm_log "Screenshot count check passed: $ACTUAL_COUNT >= $MIN_SCREENSHOTS"

exit $comment_rc
