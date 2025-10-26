#!/usr/bin/env bash
# Run Codename One iOS UI tests on the simulator and compare screenshots
set -euo pipefail

ri_log() { echo "[run-ios-ui-tests] $1"; }

# --- begin: global cleanup/watchdog helpers ---
VIDEO_PID=""
SYSLOG_PID=""
SIM_UDID_CREATED=""

cleanup() {
  # Stop recorders
  [ -n "$VIDEO_PID" ] && kill "$VIDEO_PID" >/dev/null 2>&1 || true
  [ -n "$SYSLOG_PID" ] && kill "$SYSLOG_PID" >/dev/null 2>&1 || true
  # Shutdown and delete the temp simulator we created (if any)
  if [ -n "$SIM_UDID_CREATED" ]; then
    xcrun simctl shutdown "$SIM_UDID_CREATED" >/dev/null 2>&1 || true
    xcrun simctl delete "$SIM_UDID_CREATED"   >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

run_with_timeout() {
  # run_with_timeout <seconds> <cmd...>
  local t="$1"; shift
  local log="${ARTIFACTS_DIR:-.}/xcodebuild-live.log"
  ( "$@" 2>&1 | tee -a "$log" ) &  # background xcodebuild
  local child=$!
  local waited=0
  while kill -0 "$child" >/dev/null 2>&1; do
    sleep 5
    waited=$((waited+5))
    # heartbeat so CI doesn’t think we're idle
    if (( waited % 60 == 0 )); then echo "[run-ios-ui-tests] heartbeat: ${waited}s"; fi
    if (( waited >= t )); then
      echo "[run-ios-ui-tests] WATCHDOG: Killing long-running process (>${t}s)"
      kill -TERM "$child" >/dev/null 2>&1 || true
      sleep 2
      kill -KILL "$child" >/dev/null 2>&1 || true
      wait "$child" || true
      return 124
    fi
  done
  wait "$child"
}

wait_for_boot() {
  # wait_for_boot <udid> <timeout_seconds>
  local udid="$1" timeout="$2" waited=0
  xcrun simctl boot "$udid" >/dev/null 2>&1 || true
  while (( waited < timeout )); do
    if xcrun simctl bootstatus "$udid" -b >/dev/null 2>&1; then
      return 0
    fi
    if xcrun simctl list devices 2>/dev/null | grep -q "$udid" | grep -q 'Booted'; then
      return 0
    fi
    sleep 3
    waited=$((waited+3))
  done
  return 1
}
# --- end: global cleanup/watchdog helpers ---

ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }
require_cmd() { command -v "$1" >/dev/null 2>&1 || { ri_log "FATAL: '$1' not found"; exit 3; }; }

if [ $# -lt 1 ]; then
  ri_log "Usage: $0 <workspace_path> [app_bundle] [scheme]" >&2
  exit 2
fi

WORKSPACE_PATH="$1"
APP_BUNDLE_PATH="${2:-}"
REQUESTED_SCHEME="${3:-}"

# If $2 isn’t a dir and $3 is empty, treat $2 as the scheme.
if [ -n "$APP_BUNDLE_PATH" ] && [ ! -d "$APP_BUNDLE_PATH" ] && [ -z "$REQUESTED_SCHEME" ]; then
  REQUESTED_SCHEME="$APP_BUNDLE_PATH"
  APP_BUNDLE_PATH=""
fi

if [ ! -d "$WORKSPACE_PATH" ]; then
  ri_log "Workspace not found at $WORKSPACE_PATH" >&2
  exit 3
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_MAIN_CLASS="Cn1ssChunkTools"
PROCESS_SCREENSHOTS_CLASS="ProcessScreenshots"
RENDER_SCREENSHOT_REPORT_CLASS="RenderScreenshotReport"
CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/android/tests"
[ -f "$CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" ] || { ri_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java"; exit 3; }

source "$SCRIPT_DIR/lib/cn1ss.sh"
cn1ss_log() { ri_log "$1"; }

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

ri_log "Loading workspace environment from $ENV_FILE"
[ -f "$ENV_FILE" ] || { ri_log "Missing env file: $ENV_FILE"; exit 3; }
# shellcheck disable=SC1090
source "$ENV_FILE"

# Use the same Xcode as the build step
export DEVELOPER_DIR="/Applications/Xcode_16.4.app/Contents/Developer"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"

require_cmd xcodebuild
require_cmd xcrun

if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  ri_log "JAVA17_HOME not set correctly" >&2
  exit 3
fi
JAVA17_BIN="$JAVA17_HOME/bin/java"
cn1ss_setup "$JAVA17_BIN" "$CN1SS_HELPER_SOURCE_DIR"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
mkdir -p "$ARTIFACTS_DIR"
TEST_LOG="$ARTIFACTS_DIR/xcodebuild-test.log"

if [ -z "$REQUESTED_SCHEME" ]; then
  if [[ "$WORKSPACE_PATH" == *.xcworkspace ]]; then
    REQUESTED_SCHEME="$(basename "$WORKSPACE_PATH" .xcworkspace)"
  else
    REQUESTED_SCHEME="$(basename "$WORKSPACE_PATH")"
  fi
fi
SCHEME="$REQUESTED_SCHEME"
ri_log "Using scheme $SCHEME"

SCREENSHOT_REF_DIR="$SCRIPT_DIR/ios/screenshots"
SCREENSHOT_TMP_DIR="$(mktemp -d "${TMPDIR}/cn1-ios-tests-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1-ios-tests")"
SCREENSHOT_RAW_DIR="$SCREENSHOT_TMP_DIR/raw"
SCREENSHOT_PREVIEW_DIR="$SCREENSHOT_TMP_DIR/previews"
RESULT_BUNDLE="$SCREENSHOT_TMP_DIR/test-results.xcresult"
mkdir -p "$SCREENSHOT_RAW_DIR" "$SCREENSHOT_PREVIEW_DIR"

export CN1SS_OUTPUT_DIR="$SCREENSHOT_RAW_DIR"
export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"

# Patch scheme env vars to point to our runtime dirs
SCHEME_FILE="$WORKSPACE_PATH/xcshareddata/xcschemes/$SCHEME.xcscheme"
if [ -f "$SCHEME_FILE" ]; then
  if sed --version >/dev/null 2>&1; then
    sed -i -e "s|__CN1SS_OUTPUT_DIR__|$SCREENSHOT_RAW_DIR|g" \
           -e "s|__CN1SS_PREVIEW_DIR__|$SCREENSHOT_PREVIEW_DIR|g" "$SCHEME_FILE"
  else
    sed -i '' -e "s|__CN1SS_OUTPUT_DIR__|$SCREENSHOT_RAW_DIR|g" \
              -e "s|__CN1SS_PREVIEW_DIR__|$SCREENSHOT_PREVIEW_DIR|g" "$SCHEME_FILE"
  fi
  ri_log "Injected CN1SS_* envs into scheme: $SCHEME_FILE"
else
  ri_log "Scheme file not found for env injection: $SCHEME_FILE"
fi

# --- begin: robust destination selection (text-only, no Python) ---
dump_sim_info() {
  xcrun simctl list             > "$ARTIFACTS_DIR/simctl-list.txt"           2>&1 || true
  xcrun simctl list runtimes    > "$ARTIFACTS_DIR/sim-runtimes.txt"          2>&1 || true
  xcrun simctl list devicetypes > "$ARTIFACTS_DIR/sim-devicetypes.txt"       2>&1 || true
  xcodebuild -showsdks          > "$ARTIFACTS_DIR/xcodebuild-showsdks.txt"   2>&1 || true
  xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$SCHEME" -showdestinations > "$ARTIFACTS_DIR/xcodebuild-showdestinations.txt" 2>&1 || true
}

pick_destination_from_showdestinations() {
  xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$SCHEME" -showdestinations 2>/dev/null | \
  awk '
    BEGIN{ FS="[, ]+"; best=""; }
    /platform:iOS Simulator/ && /id:/ {
      name=""; id="";
      for (i=1;i<=NF;i++) {
        if ($i ~ /^name:/) name=substr($i,6);
        if ($i ~ /^id:/)   id=substr($i,4);
      }
      if (id ~ /^[0-9A-Fa-f-]{36}$/) {
        score = (name ~ /iPhone/) ? 2 : ((name ~ /iPad/) ? 1 : 0);
        printf("%d|%s\n", score, id);
      }
    }
  ' | sort -t'|' -k1,1nr | head -n1 | cut -d'|' -f2
}

pick_available_device_udid() {
  xcrun simctl list devices available 2>/dev/null | \
  awk '/\[/ && /Available/ && /iPhone/ { sub(/^.*\[/,""); sub(/\].*$/,""); print; exit }'
}

create_temp_device_on_latest_runtime() {
  local rt dt name udid
  rt="$(xcrun simctl list runtimes 2>/dev/null | \
    awk '
      /iOS/ && /(Available|installed)/ {
        id=$NF; gsub(/[()]/,"",id);
        if (match($0,/iOS[[:space:]]+([0-9]+)\.([0-9]+)/,m)) {
          printf("%03d.%03d|%s\n", m[1], m[2], id);
        } else if (match($0,/iOS[[:space:]]+([0-9]+)/,m2)) {
          printf("%03d.%03d|%s\n", m2[1], 0, id);
        }
      }
    ' | sort | tail -n1 | cut -d"|" -f2)"
  [ -n "$rt" ] || { echo ""; return; }
  dt="$(xcrun simctl list devicetypes 2>/dev/null | \
    awk -F '[()]' '
      /iPhone 17 Pro Max/ {print $2; exit}
      /iPhone 17 Pro/     {print $2; exit}
      /iPhone 17/         {print $2; exit}
      /iPhone 16 Pro Max/ {print $2; exit}
      /iPhone 16 Pro/     {print $2; exit}
      /iPhone 16/         {print $2; exit}
      /iPhone 15 Pro Max/ {print $2; exit}
      /iPhone 15 Pro/     {print $2; exit}
      /iPhone 15/         {print $2; exit}
      /iPhone/            {print $2; exit}
    ' )"
  [ -n "$dt" ] || dt="com.apple.CoreSimulator.SimDeviceType.iPhone-16"
  name="CN1 UI Test iPhone"
  udid="$(xcrun simctl create "$name" "$dt" "$rt" 2>/dev/null || true)"
  if [ -n "$udid" ]; then SIM_UDID_CREATED="$udid"; echo "$udid"; else echo ""; fi
}

dump_sim_info

SIM_UDID=""
SIM_UDID="$(pick_destination_from_showdestinations || true)"
[ -n "$SIM_UDID" ] && ri_log "Chose simulator from xcodebuild -showdestinations: $SIM_UDID"
[ -n "$SIM_UDID" ] || SIM_UDID="$(pick_available_device_udid || true)"
[ -n "$SIM_UDID" ] && ri_log "Chose available simulator from simctl list: $SIM_UDID"
[ -n "$SIM_UDID" ] || SIM_UDID="$(create_temp_device_on_latest_runtime || true)"
[ -n "$SIM_UDID" ] && ri_log "Created simulator for tests: $SIM_UDID"

if [ -z "$SIM_UDID" ]; then
  ri_log "FATAL: No *available* iOS simulator runtime or device found on this runner"
  exit 3
fi

# Clean, boot, wait
xcrun simctl erase "$SIM_UDID" >/dev/null 2>&1 || true
if ! wait_for_boot "$SIM_UDID" 180; then
  ri_log "FATAL: Simulator never reached booted state"
  echo "Usage: simctl bootstatus <device> [-bcd]"
  exit 4
fi
ri_log "Simulator booted: $SIM_UDID"
SIM_DESTINATION="id=$SIM_UDID"
# --- end: robust destination selection ---

ri_log "Running UI tests on destination '$SIM_DESTINATION'"

DERIVED_DATA_DIR="$SCREENSHOT_TMP_DIR/derived"
rm -rf "$DERIVED_DATA_DIR"

ri_log "Xcode version: $(xcodebuild -version | tr '\n' ' ')"
ri_log "Destinations for scheme:"
xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$SCHEME" -showdestinations || true

ri_log "STAGE:BUILD_FOR_TESTING -> xcodebuild build-for-testing"
set -o pipefail
if ! xcodebuild \
  -workspace "$WORKSPACE_PATH" \
  -scheme "$SCHEME" \
  -sdk iphonesimulator \
  -configuration Debug \
  -destination "$SIM_DESTINATION" \
  -derivedDataPath "$DERIVED_DATA_DIR" \
  ONLY_ACTIVE_ARCH=YES \
  EXCLUDED_ARCHS_i386="i386" EXCLUDED_ARCHS_x86_64="x86_64" \
  build-for-testing | tee "$ARTIFACTS_DIR/xcodebuild-build.log"; then
  ri_log "STAGE:BUILD_FAILED -> See $ARTIFACTS_DIR/xcodebuild-build.log"
  exit 1
fi

# Locate products we need
AUT_APP="$(/bin/ls -1d "$DERIVED_DATA_DIR"/Build/Products/Debug-iphonesimulator/*.app 2>/dev/null | grep -v '\-Runner\.app$' | head -n1 || true)"
RUNNER_APP="$(/bin/ls -1d "$DERIVED_DATA_DIR"/Build/Products/Debug-iphonesimulator/*-Runner.app 2>/dev/null | head -n1 || true)"

# Fallback to optional arg2 if AUT not found
if [ -z "$AUT_APP" ] && [ -n "$APP_BUNDLE_PATH" ] && [ -d "$APP_BUNDLE_PATH" ]; then
  AUT_APP="$APP_BUNDLE_PATH"
fi

# Install AUT + Runner explicitly (prevents "unknown to FrontBoard")
if [ -n "$AUT_APP" ] && [ -d "$AUT_APP" ]; then
  ri_log "Installing AUT: $AUT_APP"
  xcrun simctl install "$SIM_UDID" "$AUT_APP" || true
  AUT_BUNDLE_ID=$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$AUT_APP/Info.plist" 2>/dev/null || true)
  [ -n "$AUT_BUNDLE_ID" ] && ri_log "AUT bundle id: $AUT_BUNDLE_ID"
fi
if [ -n "$RUNNER_APP" ] && [ -d "$RUNNER_APP" ]; then
  ri_log "Installing Test Runner: $RUNNER_APP"
  xcrun simctl install "$SIM_UDID" "$RUNNER_APP" || true
else
  ri_log "WARN: Test Runner app not found under derived data"
fi

# Begin syslog capture (after install)
SIM_SYSLOG="$ARTIFACTS_DIR/simulator-syslog.txt"
ri_log "Capturing simulator syslog at $SIM_SYSLOG"
( xcrun simctl spawn "$SIM_UDID" log stream --style syslog --level debug \
  || xcrun simctl spawn "$SIM_UDID" log stream --style compact ) > "$SIM_SYSLOG" 2>&1 &
SYSLOG_PID=$!

# Optional: record video of the run
RUN_VIDEO="$ARTIFACTS_DIR/run.mp4"
ri_log "Recording simulator video to $RUN_VIDEO"
( xcrun simctl io "$SIM_UDID" recordVideo "$RUN_VIDEO" & echo $! > "$SCREENSHOT_TMP_DIR/video.pid" ) || true
VIDEO_PID="$(cat "$SCREENSHOT_TMP_DIR/video.pid" 2>/dev/null || true)"

# Run only the UI test bundle
UI_TEST_TARGET="${UI_TEST_TARGET:-HelloCodenameOneUITests}"
XCODE_TEST_FILTERS=(
  -only-testing:"${UI_TEST_TARGET}"
  -skip-testing:HelloCodenameOneTests
)

ri_log "STAGE:TEST -> xcodebuild test-without-building (destination=$SIM_DESTINATION)"
if ! run_with_timeout 1500 xcodebuild \
  -workspace "$WORKSPACE_PATH" \
  -scheme "$SCHEME" \
  -sdk iphonesimulator \
  -configuration Debug \
  -destination "$SIM_DESTINATION" \
  -derivedDataPath "$DERIVED_DATA_DIR" \
  -resultBundlePath "$RESULT_BUNDLE" \
  "${XCODE_TEST_FILTERS[@]}" \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO \
  GENERATE_INFOPLIST_FILE=YES \
  -parallel-testing-enabled NO \
  test-without-building | tee "$TEST_LOG"; then
  rc=$?
  if [ "$rc" = "124" ]; then
    ri_log "STAGE:WATCHDOG_TRIGGERED -> Killed stalled xcodebuild"
  else
    ri_log "STAGE:XCODE_TEST_FAILED -> See $TEST_LOG"
  fi
  exit 10
fi
set +o pipefail

# --- Begin: Stop video + final screenshots ---
if [ -f "$SCREENSHOT_TMP_DIR/video.pid" ]; then
  rec_pid="$(cat "$SCREENSHOT_TMP_DIR/video.pid" 2>/dev/null || true)"
  if [ -n "$rec_pid" ]; then
    ri_log "Stopping simulator video recording (pid=$rec_pid)"
    kill "$rec_pid" >/dev/null 2>&1 || true
    sleep 1
  fi
fi

# Export xcresult JSON (best effort)
if [ -d "$RESULT_BUNDLE" ]; then
  ri_log "Exporting xcresult JSON"
  /usr/bin/xcrun xcresulttool get --format json --path "$RESULT_BUNDLE" > "$ARTIFACTS_DIR/xcresult.json" 2>/dev/null || true
else
  ri_log "xcresult bundle not found at $RESULT_BUNDLE"
fi

ri_log "Final simulator screenshot"
xcrun simctl io "$SIM_UDID" screenshot "$ARTIFACTS_DIR/final.png" || true
# --- End: Stop video + final screenshots ---

# --- CN1SS extraction & reporting (unchanged) ---
declare -a CN1SS_SOURCES=()
if [ -s "$TEST_LOG" ]; then
  CN1SS_SOURCES+=("XCODELOG:$TEST_LOG")
else
  ri_log "FATAL: Test log missing or empty at $TEST_LOG"
  exit 11
fi

LOG_CHUNKS="$(cn1ss_count_chunks "$TEST_LOG")"; LOG_CHUNKS="${LOG_CHUNKS//[^0-9]/}"; : "${LOG_CHUNKS:=0}"
ri_log "Chunk counts -> xcodebuild log: ${LOG_CHUNKS}"

if [ "${LOG_CHUNKS:-0}" = "0" ]; then
  ri_log "STAGE:MARKERS_NOT_FOUND -> xcodebuild output did not include CN1SS chunks"
  ri_log "---- CN1SS lines (if any) ----"
  (grep "CN1SS:" "$TEST_LOG" || true) | sed 's/^/[CN1SS] /'
  exit 12
fi

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
ri_log "Detected CN1SS test streams: ${TEST_NAMES[*]}"

PAIR_SEP=$'\037'
declare -a TEST_OUTPUT_ENTRIES=()

ensure_dir "$SCREENSHOT_PREVIEW_DIR"

for test in "${TEST_NAMES[@]}"; do
  dest="$SCREENSHOT_TMP_DIR/${test}.png"
  if source_label="$(cn1ss_decode_test_png "$test" "$dest" "${CN1SS_SOURCES[@]}")"; then
    TEST_OUTPUT_ENTRIES+=("${test}${PAIR_SEP}${dest}")
    ri_log "Decoded screenshot for '$test' (source=${source_label}, size: $(cn1ss_file_size "$dest") bytes)"
    preview_dest="$SCREENSHOT_PREVIEW_DIR/${test}.jpg"
    if preview_source="$(cn1ss_decode_test_preview "$test" "$preview_dest" "${CN1SS_SOURCES[@]}")"; then
      ri_log "Decoded preview for '$test' (source=${preview_source}, size: $(cn1ss_file_size "$preview_dest") bytes)"
    else
      rm -f "$preview_dest" 2>/dev/null || true
    fi
  else
    ri_log "FATAL: Failed to extract/decode CN1SS payload for test '$test'"
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
      ri_log "Partial base64 saved at: $RAW_B64_OUT"
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

COMPARE_ARGS=()
for test in "${TEST_NAMES[@]}"; do
  if dest="$(lookup_test_output "$test")"; then
    [ -n "$dest" ] || continue
    COMPARE_ARGS+=("--actual" "${test}=${dest}")
  fi
done

COMPARE_JSON="$SCREENSHOT_TMP_DIR/screenshot-compare.json"
export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"
ri_log "STAGE:COMPARE -> Evaluating screenshots against stored references"
if ! cn1ss_java_run "$PROCESS_SCREENSHOTS_CLASS" \
  --reference-dir "$SCREENSHOT_REF_DIR" \
  --emit-base64 \
  --preview-dir "$SCREENSHOT_PREVIEW_DIR" \
  "${COMPARE_ARGS[@]}" > "$COMPARE_JSON"; then
  ri_log "FATAL: Screenshot comparison helper failed"
  exit 13
fi

SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"

ri_log "STAGE:COMMENT_BUILD -> Rendering summary and PR comment markdown"
if ! cn1ss_java_run "$RENDER_SCREENSHOT_REPORT_CLASS" \
  --compare-json "$COMPARE_JSON" \
  --comment-out "$COMMENT_FILE" \
  --summary-out "$SUMMARY_FILE"; then
  ri_log "FATAL: Failed to render screenshot summary/comment"
  exit 14
fi

if [ -s "$SUMMARY_FILE" ]; then
  ri_log "  -> Wrote summary entries to $SUMMARY_FILE ($(wc -l < "$SUMMARY_FILE" 2>/dev/null || echo 0) line(s))"
else
  ri_log "  -> No summary entries generated (all screenshots matched stored baselines)"
fi

if [ -s "$COMMENT_FILE" ]; then
  ri_log "  -> Prepared PR comment payload at $COMMENT_FILE (bytes=$(wc -c < "$COMMENT_FILE" 2>/dev/null || echo 0))"
else
  ri_log "  -> No PR comment content produced"
fi

if [ -s "$SUMMARY_FILE" ]; then
  while IFS='|' read -r status test message copy_flag path preview_note; do
    [ -n "${test:-}" ] || continue
    ri_log "Test '${test}': ${message}"
    if [ "$copy_flag" = "1" ] && [ -n "${path:-}" ] && [ -f "$path" ]; then
      cp -f "$path" "$ARTIFACTS_DIR/${test}.png" 2>/dev/null || true
      ri_log "  -> Stored PNG artifact copy at $ARTIFACTS_DIR/${test}.png"
    fi
    if [ "$status" = "equal" ] && [ -n "${path:-}" ]; then
      rm -f "$path" 2>/dev/null || true
    fi
    if [ -n "${preview_note:-}" ]; then
      ri_log "  Preview note: ${preview_note}"
    fi
  done < "$SUMMARY_FILE"
fi

cp -f "$COMPARE_JSON" "$ARTIFACTS_DIR/screenshot-compare.json" 2>/dev/null || true
[ -s "$COMMENT_FILE" ] && cp -f "$COMMENT_FILE" "$ARTIFACTS_DIR/screenshot-comment.md" 2>/dev/null || true

# --- Begin: stop syslog capture ---
if [ -n "${SYSLOG_PID:-}" ]; then
  ri_log "Stopping simulator log capture (pid=$SYSLOG_PID)"
  kill "$SYSLOG_PID" >/dev/null 2>&1 || true
fi
# --- End: stop syslog capture ---

ri_log "STAGE:COMMENT_POST -> Submitting PR feedback"
comment_rc=0
if ! cn1ss_post_pr_comment "$COMMENT_FILE" "$SCREENSHOT_PREVIEW_DIR"; then
  comment_rc=$?
fi

exit $comment_rc