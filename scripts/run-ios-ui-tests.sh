#!/usr/bin/env bash
# Run Codename One iOS UI tests on the simulator and compare screenshots
set -euo pipefail

ri_log() { echo "[run-ios-ui-tests] $1"; }

ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

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

if [ -n "$APP_BUNDLE_PATH" ]; then
  ri_log "Using simulator app bundle at $APP_BUNDLE_PATH"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/common/java"
source "$SCRIPT_DIR/lib/cn1ss.sh"

if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" ]; then
  ri_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" >&2
  exit 3
fi
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
# Use default Xcode.app which points to latest stable GA version on the runner
XCODE_APP="${XCODE_APP:-/Applications/Xcode.app}"
if [ ! -d "$XCODE_APP" ]; then
  ri_log "Xcode not found at $XCODE_APP. Set XCODE_APP to the Xcode app bundle path." >&2
  exit 3
fi
export DEVELOPER_DIR="$XCODE_APP/Contents/Developer"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"

if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  ri_log "JAVA17_HOME not set correctly" >&2
  exit 3
fi
if ! command -v xcodebuild >/dev/null 2>&1; then
  ri_log "xcodebuild not found" >&2
  exit 3
fi
if ! command -v xcrun >/dev/null 2>&1; then
  ri_log "xcrun not found" >&2
  exit 3
fi

JAVA17_BIN="$JAVA17_HOME/bin/java"

cn1ss_setup "$JAVA17_BIN" "$CN1SS_HELPER_SOURCE_DIR"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
mkdir -p "$ARTIFACTS_DIR"
TEST_LOG="$ARTIFACTS_DIR/device-runner.log"

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
mkdir -p "$SCREENSHOT_RAW_DIR" "$SCREENSHOT_PREVIEW_DIR"

export CN1SS_OUTPUT_DIR="$SCREENSHOT_RAW_DIR"
export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"

# Patch scheme env vars to point to our runtime dirs
SCHEME_FILE="$WORKSPACE_PATH/xcshareddata/xcschemes/$SCHEME.xcscheme"
if [ -f "$SCHEME_FILE" ]; then
  if sed --version >/dev/null 2>&1; then
    # GNU sed
    sed -i -e "s|__CN1SS_OUTPUT_DIR__|$SCREENSHOT_RAW_DIR|g" \
           -e "s|__CN1SS_PREVIEW_DIR__|$SCREENSHOT_PREVIEW_DIR|g" "$SCHEME_FILE"
  else
    # BSD sed (macOS)
    sed -i '' -e "s|__CN1SS_OUTPUT_DIR__|$SCREENSHOT_RAW_DIR|g" \
              -e "s|__CN1SS_PREVIEW_DIR__|$SCREENSHOT_PREVIEW_DIR|g" "$SCHEME_FILE"
  fi
  ri_log "Injected CN1SS_* envs into scheme: $SCHEME_FILE"
else
  ri_log "Scheme file not found for env injection: $SCHEME_FILE"
fi

MAX_SIM_OS_MAJOR=20

trim_whitespace() {
  local value="$1"
  value="${value#${value%%[![:space:]]*}}"
  value="${value%${value##*[![:space:]]}}"
  printf '%s' "$value"
}

normalize_destination() {
  local raw="$1"
  IFS=',' read -r -a parts <<< "$raw"
  local platform="" id="" os="" name=""
  local extras=()
  for part in "${parts[@]}"; do
    part="$(trim_whitespace "$part")"
    case "$part" in
      platform=*) platform="${part#platform=}" ;;
      id=*) id="${part#id=}" ;;
      OS=*) os="${part#OS=}" ;;
      os=*) os="${part#os=}" ;;
      name=*) name="${part#name=}" ;;
      '') ;;
      *) extras+=("$part") ;;
    esac
  done
  [ -z "$platform" ] && platform="iOS Simulator"

  local components=("platform=$platform")
  [ -n "$id" ] && components+=("id=$id")
  [ -n "$os" ] && components+=("OS=$os")
  [ -n "$name" ] && components+=("name=$name")
  if [ ${#extras[@]} -gt 0 ]; then
    for extra in "${extras[@]}"; do
      [ -n "$extra" ] && components+=("$extra")
    done
  fi

  local joined="${components[0]}" part
  for part in "${components[@]:1}"; do
    joined+=",$part"
  done

  printf '%s\n' "$joined"
}

auto_select_destination() {
  local show_dest rc=0 best_line="" best_key="" line payload platform id name os priority key part value
  set +e
  show_dest="$(xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$SCHEME" -sdk iphonesimulator -showdestinations 2>/dev/null)"
  rc=$?
  set -e

  [ -z "${show_dest:-}" ] && return $rc

  local section=""
  while IFS= read -r line; do
    case "$line" in
      *"Available destinations"*) section="available"; continue ;;
      *"Ineligible destinations"*|*"Local destinations"*) section="other"; continue ;;
    esac
    [ "$section" != "available" ] && continue
    case "$line" in
      *{*}*)
        payload="$(printf '%s\n' "$line" | sed -n 's/.*{\(.*\)}/\1/p')"
        [ -z "$payload" ] && continue
        platform=""; id=""; name=""; os=""
        IFS=',' read -r -a parts <<< "$payload"
        for part in "${parts[@]}"; do
          part="$(trim_whitespace "$part")"
          key="${part%%:*}"
          value="${part#*:}"
          [ "$value" = "$part" ] && continue
          key="$(trim_whitespace "$key")"
          value="$(trim_whitespace "$value")"
          case "$key" in
            platform) platform="$value" ;;
            id) id="$value" ;;
            name) name="$value" ;;
            OS|os|"OS version") os="$value" ;;
          esac
        done
        [ "$platform" != "iOS Simulator" ] && continue
        [ -z "$id" ] && continue
        # Skip placeholder device IDs
        case "$id" in
          *Placeholder*|*placeholder*) continue ;;
        esac
        priority=0
        case "$(printf '%s' "$name" | tr 'A-Z' 'a-z')" in
          *iphone*) priority=2 ;;
          *ipad*) priority=1 ;;
        esac
        validity=1
        major="${os%%.*}"
        case "$major" in ''|*[!0-9]*) major=0 ;; esac
        if [ "$major" -gt "$MAX_SIM_OS_MAJOR" ] 2>/dev/null; then
          validity=0
        fi
        IFS='.' read -r v1 v2 v3 <<< "$os"
        v1=${v1:-0}; v2=${v2:-0}; v3=${v3:-0}
        key=$(printf '%d-%d-%03d-%03d-%03d' "$validity" "$priority" "$v1" "$v2" "$v3")
        if [ -z "$best_key" ] || [[ "$key" > "$best_key" ]]; then
          best_key="$key"
          best_line="platform=iOS Simulator,id=$id"
          [ -n "$os" ] && best_line="$best_line,OS=$os"
          [ -n "$name" ] && best_line="$best_line,name=$name"
        fi
        ;;
    esac
  done <<< "$show_dest"

  [ -n "$best_line" ] && printf '%s\n' "$best_line"
  [ -n "$best_line" ] && return 0

  return $rc
}

fallback_sim_destination() {
  if ! command -v xcrun >/dev/null 2>&1; then
    return
  fi

  local best_line="" best_key="" current_version="" lower_name="" lower_state=""

  while IFS= read -r raw_line; do
    case "$raw_line" in
      --\ iOS\ *--)
        current_version="$(printf '%s\n' "$raw_line" | sed -n 's/.*-- iOS \([0-9.]*\) --.*/\1/p')"
        continue
        ;;
      --\ *--)
        current_version=""
        continue
        ;;
    esac

    [ -z "$current_version" ] && continue
    line="${raw_line#${raw_line%%[![:space:]]*}}"
    [ -z "$line" ] && continue

    name="${line%% (*}"
    rest="${line#* (}"
    [ "$rest" = "$line" ] && continue
    id="${rest%%)*}"
    state="${line##*(}"
    state="${state%)}"
    name="$(trim_whitespace "$name")"
    id="$(trim_whitespace "$id")"
    state="$(trim_whitespace "$state")"
    [ -z "$name" ] && continue
    [ -z "$id" ] && continue
    # Skip placeholder device IDs
    case "$id" in
      *Placeholder*|*placeholder*) continue ;;
    esac

    lower_name="$(printf '%s' "$name" | tr 'A-Z' 'a-z')"
    case "$lower_name" in
      *iphone*) priority=2 ;;
      *ipad*) priority=1 ;;
      *) priority=0 ;;
    esac

    lower_state="$(printf '%s' "$state" | tr 'A-Z' 'a-z')"
    boot=0
    [ "$lower_state" = "booted" ] && boot=1

    validity=1
    major="${current_version%%.*}"
    case "$major" in ''|*[!0-9]*) major=0 ;; esac
    if [ "$major" -gt "$MAX_SIM_OS_MAJOR" ] 2>/dev/null; then
      validity=0
    fi

    IFS='.' read -r v1 v2 v3 <<< "$current_version"
    v1=${v1:-0}; v2=${v2:-0}; v3=${v3:-0}

    candidate_key=$(printf '%d-%d-%03d-%03d-%03d-%d' "$validity" "$priority" "$v1" "$v2" "$v3" "$boot")
    if [ -z "$best_key" ] || [[ "$candidate_key" > "$best_key" ]]; then
      best_key="$candidate_key"
      best_line="platform=iOS Simulator,id=$id"
      [ -n "$current_version" ] && best_line="$best_line,OS=$current_version"
      best_line="$best_line,name=$name"
    fi
  done < <(xcrun simctl list devices 2>/dev/null)

  if [ -n "$best_line" ]; then
    printf '%s\n' "$best_line"
  fi
}



SIM_DESTINATION="${IOS_SIM_DESTINATION:-}"
if [ -z "$SIM_DESTINATION" ]; then
  SELECTED_DESTINATION="$(auto_select_destination || true)"
  if [ -n "${SELECTED_DESTINATION:-}" ]; then
    SIM_DESTINATION="$SELECTED_DESTINATION"
    ri_log "Auto-selected simulator destination '$SIM_DESTINATION'"
  else
    ri_log "Simulator auto-selection did not return a destination"
  fi
fi
if [ -z "$SIM_DESTINATION" ]; then
  FALLBACK_DESTINATION="$(fallback_sim_destination || true)"
  if [ -n "${FALLBACK_DESTINATION:-}" ]; then
    SIM_DESTINATION="$FALLBACK_DESTINATION"
    ri_log "Using fallback simulator destination '$SIM_DESTINATION'"
  else
    SIM_DESTINATION="platform=iOS Simulator,name=iPhone 16"
    ri_log "Falling back to default simulator destination '$SIM_DESTINATION'"
  fi
fi

SIM_DESTINATION="$(normalize_destination "$SIM_DESTINATION")"

# Extract UDID and prefer id-only destination to avoid OS/SDK mismatches
SIM_UDID="$(printf '%s\n' "$SIM_DESTINATION" | sed -n 's/.*id=\([^,]*\).*/\1/p' | tr -d '\r[:space:]')"
if [ -n "$SIM_UDID" ]; then
  ri_log "Booting simulator $SIM_UDID"
  BOOT_START=$(date +%s)
  xcrun simctl boot "$SIM_UDID" >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$SIM_UDID" -b
  BOOT_END=$(date +%s)
  echo "Simulator Boot : $(( (BOOT_END - BOOT_START) * 1000 )) ms" >> "$ARTIFACTS_DIR/ios-test-stats.txt"
  
  # Wait for the simulator to stabilize after boot
  # This is particularly important with Xcode 26 / iOS 26 where there can be a delay
  # between simctl reporting the simulator as booted and xcodebuild being able to use it
  # Using a fixed delay is more reliable than polling xcodebuild -showdestinations which
  # is very slow (~10-15s per call) and can hang on macOS runners with iOS 26
  XCODEBUILD_STABILIZATION_DELAY=15  # seconds to wait after boot completes
  ri_log "Waiting ${XCODEBUILD_STABILIZATION_DELAY}s for simulator to stabilize..."
  sleep "$XCODEBUILD_STABILIZATION_DELAY"
  
  # Verify simulator is still in Booted state
  SIM_STATE="$(xcrun simctl list devices 2>/dev/null | grep "$SIM_UDID" | grep -o '(.*' | tr -d '()' || true)"
  if [ -n "$SIM_STATE" ]; then
    ri_log "Simulator state after stabilization: $SIM_STATE"
  fi
  
  SIM_DESTINATION="id=$SIM_UDID"
fi
ri_log "Running DeviceRunner on destination '$SIM_DESTINATION'"

DERIVED_DATA_DIR="$SCREENSHOT_TMP_DIR/derived"
rm -rf "$DERIVED_DATA_DIR"
BUILD_LOG="$ARTIFACTS_DIR/xcodebuild-build.log"

ri_log "Building simulator app with xcodebuild"
COMPILE_START=$(date +%s)
# Note: Omitting -sdk flag to let xcodebuild infer the correct SDK from the destination
# This prevents SDK version mismatches when the simulator OS differs from Xcode's default SDK
if ! xcodebuild \
  -workspace "$WORKSPACE_PATH" \
  -scheme "$SCHEME" \
  -configuration Debug \
  -destination "$SIM_DESTINATION" \
  -destination-timeout 120 \
  -derivedDataPath "$DERIVED_DATA_DIR" \
  build | tee "$BUILD_LOG"; then
  ri_log "STAGE:XCODE_BUILD_FAILED -> See $BUILD_LOG"
  exit 10
fi
COMPILE_END=$(date +%s)
COMPILATION_TIME=$((COMPILE_END - COMPILE_START))
ri_log "Compilation time: ${COMPILATION_TIME}s"

BUILD_SETTINGS="$(xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$SCHEME" -configuration Debug -showBuildSettings 2>/dev/null || true)"
TARGET_BUILD_DIR="$(printf '%s\n' "$BUILD_SETTINGS" | awk -F' = ' '/ TARGET_BUILD_DIR /{print $2; exit}')"
WRAPPER_NAME="$(printf '%s\n' "$BUILD_SETTINGS" | awk -F' = ' '/ WRAPPER_NAME /{print $2; exit}')"
if [ -z "$WRAPPER_NAME" ]; then
  ri_log "FATAL: Unable to determine build wrapper name"
  exit 11
fi
if [ -z "$APP_BUNDLE_PATH" ]; then
  CANDIDATE_BUNDLE="$DERIVED_DATA_DIR/Build/Products/Debug-iphonesimulator/$WRAPPER_NAME"
  if [ -d "$CANDIDATE_BUNDLE" ]; then
    APP_BUNDLE_PATH="$CANDIDATE_BUNDLE"
  fi
fi
if [ -z "$APP_BUNDLE_PATH" ] && [ -n "$TARGET_BUILD_DIR" ]; then
  CANDIDATE_BUNDLE="$TARGET_BUILD_DIR/$WRAPPER_NAME"
  if [ -d "$CANDIDATE_BUNDLE" ]; then
    APP_BUNDLE_PATH="$CANDIDATE_BUNDLE"
  fi
fi
if [ -z "$APP_BUNDLE_PATH" ]; then
  CANDIDATE_BUNDLE="$(find "$DERIVED_DATA_DIR" -path "*/Debug-iphonesimulator/$WRAPPER_NAME" -type d -print -quit 2>/dev/null || true)"
  if [ -d "$CANDIDATE_BUNDLE" ]; then
    APP_BUNDLE_PATH="$CANDIDATE_BUNDLE"
  fi
fi
if [ -z "$APP_BUNDLE_PATH" ]; then
  ri_log "FATAL: Simulator app bundle missing for wrapper $WRAPPER_NAME"
  exit 11
fi
if [ ! -d "$APP_BUNDLE_PATH" ]; then
  ri_log "FATAL: Simulator app bundle missing at $APP_BUNDLE_PATH"
  exit 11
fi
BUNDLE_IDENTIFIER="$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$APP_BUNDLE_PATH/Info.plist" 2>/dev/null || true)"
if [ -z "$BUNDLE_IDENTIFIER" ]; then
  ri_log "FATAL: Unable to determine CFBundleIdentifier"
  exit 11
fi
APP_PROCESS_NAME="${WRAPPER_NAME%.app}"

  SIM_DEVICE_ID=""
  SIM_DEVICE_ID="$(printf '%s\n' "$SIM_DESTINATION" | sed -n 's/.*id=\([^,]*\).*/\1/p' | tr -d '\r' | sed 's/[[:space:]]//g')"
  if [ -z "$SIM_DEVICE_ID" ] || [ "$SIM_DEVICE_ID" = "$SIM_DESTINATION" ]; then
    SIM_DEVICE_NAME="$(printf '%s\n' "$SIM_DESTINATION" | sed -n 's/.*name=\([^,]*\).*/\1/p')"
    SIM_DEVICE_NAME="$(trim_whitespace "${SIM_DEVICE_NAME:-}")"
    if [ -n "$SIM_DEVICE_NAME" ]; then
      resolved_id=""
      in_ios=0
      while IFS= read -r raw_line; do
        case "$raw_line" in
          --\ iOS\ *--) in_ios=1; continue ;;
          --\ *--) in_ios=0; continue ;;
        esac
        [ "$in_ios" -eq 0 ] && continue
        line="${raw_line#${raw_line%%[![:space:]]*}}"
        [ -z "$line" ] && continue
        name_part="${line%% (*}"
        rest="${line#* (}"
        [ "$rest" = "$line" ] && continue
        id_part="${rest%%)*}"
        name_candidate="$(trim_whitespace "$name_part")"
        if [ "$name_candidate" = "$SIM_DEVICE_NAME" ]; then
          resolved_id="$(trim_whitespace "$id_part")"
          break
        fi
      done < <(xcrun simctl list devices 2>/dev/null)
      SIM_DEVICE_ID="$resolved_id"
    fi
  fi

  if [ -n "$SIM_DEVICE_ID" ]; then
    ri_log "Booting simulator $SIM_DEVICE_ID"
    BOOT_START=$(date +%s)
    xcrun simctl boot "$SIM_DEVICE_ID" >/dev/null 2>&1 || true
    xcrun simctl bootstatus "$SIM_DEVICE_ID" -b
    BOOT_END=$(date +%s)
    echo "Simulator Boot (Run) : $(( (BOOT_END - BOOT_START) * 1000 )) ms" >> "$ARTIFACTS_DIR/ios-test-stats.txt"
  else
    ri_log "Warning: simulator UDID not resolved; relying on default booted device"
    xcrun simctl bootstatus booted -b || true
  fi

  LOG_STREAM_PID=0
  cleanup() {
    if [ "$LOG_STREAM_PID" -ne 0 ]; then
      kill "$LOG_STREAM_PID" >/dev/null 2>&1 || true
      wait "$LOG_STREAM_PID" 2>/dev/null || true
    fi
    if [ -n "$SIM_DEVICE_ID" ] && [ -n "$BUNDLE_IDENTIFIER" ]; then
      xcrun simctl terminate "$SIM_DEVICE_ID" "$BUNDLE_IDENTIFIER" >/dev/null 2>&1 || true
    fi
  }
  trap cleanup EXIT

  ri_log "Streaming simulator logs to $TEST_LOG"
  if [ -n "$SIM_DEVICE_ID" ]; then
    xcrun simctl terminate "$SIM_DEVICE_ID" "$BUNDLE_IDENTIFIER" >/dev/null 2>&1 || true
    xcrun simctl uninstall "$SIM_DEVICE_ID" "$BUNDLE_IDENTIFIER" >/dev/null 2>&1 || true

    xcrun simctl spawn "$SIM_DEVICE_ID" \
      log stream --style json --level debug \
      --predicate 'eventMessage CONTAINS "CN1SS"' \
      > "$TEST_LOG" 2>&1 &
  else
    xcrun simctl spawn booted log stream --style compact --level debug --predicate 'composedMessage CONTAINS "CN1SS"' > "$TEST_LOG" 2>&1 &
  fi
  LOG_STREAM_PID=$!
  sleep 2

  LAUNCH_LOG="$ARTIFACTS_DIR/simctl-launch.log"

  launch_simulator_app() {
    local target="$1"
    local attempt=1
    while true; do
      local output
      if output="$(xcrun simctl launch "$target" "$BUNDLE_IDENTIFIER" 2>&1)"; then
        printf '%s\n' "$output" >> "$LAUNCH_LOG"
        return 0
      fi
      printf '%s\n' "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] simctl launch failed (attempt $attempt): $output" >> "$LAUNCH_LOG"
      if [ "$attempt" -ge 2 ]; then
        return 1
      fi
      ri_log "simctl launch failed (attempt $attempt), retrying"
      xcrun simctl bootstatus "$target" -b >/dev/null 2>&1 || true
      sleep 5
      attempt=$((attempt + 1))
    done
  }

  ri_log "Installing simulator app bundle"
  INSTALL_START=$(date +%s)
  if [ -n "$SIM_DEVICE_ID" ]; then
    if ! xcrun simctl install "$SIM_DEVICE_ID" "$APP_BUNDLE_PATH"; then
      ri_log "FATAL: simctl install failed"
      exit 11
    fi
    INSTALL_END=$(date +%s)

    LAUNCH_START=$(date +%s)
    if ! launch_simulator_app "$SIM_DEVICE_ID"; then
      ri_log "FATAL: simctl launch failed (see $LAUNCH_LOG)"
      exit 11
    fi
    LAUNCH_END=$(date +%s)
  else
    if ! xcrun simctl install booted "$APP_BUNDLE_PATH"; then
      ri_log "FATAL: simctl install failed"
      exit 11
    fi
    INSTALL_END=$(date +%s)

    LAUNCH_START=$(date +%s)
    if ! launch_simulator_app booted; then
      ri_log "FATAL: simctl launch failed (see $LAUNCH_LOG)"
      exit 11
    fi
    LAUNCH_END=$(date +%s)
  fi
  echo "App Install : $(( (INSTALL_END - INSTALL_START) * 1000 )) ms" >> "$ARTIFACTS_DIR/ios-test-stats.txt"
  echo "App Launch : $(( (LAUNCH_END - LAUNCH_START) * 1000 )) ms" >> "$ARTIFACTS_DIR/ios-test-stats.txt"

END_MARKER="CN1SS:SUITE:FINISHED"
TIMEOUT_SECONDS=300
START_TIME="$(date +%s)"
ri_log "Waiting for DeviceRunner completion marker ($END_MARKER)"
while true; do
  if grep -q "$END_MARKER" "$TEST_LOG"; then
    ri_log "Detected DeviceRunner completion marker"
    break
  fi
  NOW="$(date +%s)"
  if [ $(( NOW - START_TIME )) -ge $TIMEOUT_SECONDS ]; then
    ri_log "STAGE:TIMEOUT -> DeviceRunner did not emit completion marker within ${TIMEOUT_SECONDS}s"
    break
  fi
  sleep 5
done
END_TIME=$(date +%s)
echo "Test Execution : $(( (END_TIME - START_TIME) * 1000 )) ms" >> "$ARTIFACTS_DIR/ios-test-stats.txt"

sleep 3

kill "$LOG_STREAM_PID" >/dev/null 2>&1 || true
wait "$LOG_STREAM_PID" 2>/dev/null || true
LOG_STREAM_PID=0

FALLBACK_LOG="$ARTIFACTS_DIR/device-runner-fallback.log"
xcrun simctl spawn "$SIM_DEVICE_ID" \
  log show --style syslog --last 30m \
  --predicate 'eventMessage CONTAINS "CN1SS"' \
  > "$FALLBACK_LOG" 2>/dev/null || true

if [ -n "$SIM_DEVICE_ID" ]; then
  xcrun simctl terminate "$SIM_DEVICE_ID" "$BUNDLE_IDENTIFIER" >/dev/null 2>&1 || true
fi

declare -a CN1SS_SOURCES=("SIMLOG:$TEST_LOG")

LOG_CHUNKS="$(cn1ss_count_chunks "$TEST_LOG")"; LOG_CHUNKS="${LOG_CHUNKS//[^0-9]/}"; : "${LOG_CHUNKS:=0}"
ri_log "Chunk counts -> simulator log: ${LOG_CHUNKS}"

if [ "${LOG_CHUNKS:-0}" = "0" ]; then
  ri_log "STAGE:MARKERS_NOT_FOUND -> simulator output did not include CN1SS chunks"
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
    ri_log "Primary decode failed for '$test'; trying fallback log"
    if [ -s "$FALLBACK_LOG" ] && source_label="$(cn1ss_decode_test_png "$test" "$dest" "SIMLOG:$FALLBACK_LOG")"; then
      ri_log "Decoded screenshot for '$test' from fallback (size: $(cn1ss_file_size "$dest") bytes)"
    else
      ri_log "FATAL: Failed to extract/decode CN1SS payload for test '$test'"
      RAW_B64_OUT="$SCREENSHOT_TMP_DIR/${test}.raw.b64"
      {
        for entry in "${CN1SS_SOURCES[@]}" "SIMLOG:$FALLBACK_LOG"; do
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
export CN1SS_COMMENT_MARKER="<!-- CN1SS_IOS_COMMENT -->"
export CN1SS_COMMENT_LOG_PREFIX="[run-ios-device-tests]"
export CN1SS_PREVIEW_SUBDIR="ios"

# Load VM translation time if available
CN1SS_VM_TIME=0
if [ -f "$ARTIFACTS_DIR/vm_time.txt" ]; then
  CN1SS_VM_TIME=$(cat "$ARTIFACTS_DIR/vm_time.txt")
  ri_log "Loaded VM translation time: ${CN1SS_VM_TIME}s"
fi
export CN1SS_VM_TIME
export CN1SS_COMPILATION_TIME="$COMPILATION_TIME"

cn1ss_process_and_report \
  "iOS screenshot updates" \
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

exit $comment_rc
