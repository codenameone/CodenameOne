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

CN1SS_MAIN_CLASS="Cn1ssChunkTools"
PROCESS_SCREENSHOTS_CLASS="ProcessScreenshots"
RENDER_SCREENSHOT_REPORT_CLASS="RenderScreenshotReport"
CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/android/tests"
if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" ]; then
  ri_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" >&2
  exit 3
fi

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

auto_select_destination() {
  if ! command -v python3 >/dev/null 2>&1; then
    return
  fi

  local show_dest selected
  if show_dest="$(xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$SCHEME" -showdestinations 2>/dev/null)"; then
    selected="$(
      printf '%s\n' "$show_dest" | python3 - <<'PY'
import sys

def version_key(value: str):
    value = (value or '').replace('latest', '').replace('(', '').replace(')', '')
    parts = []
    for part in value.split('.'):
        if part.isdigit():
            parts.append(int(part))
        elif part:
            parts.append(0)
    return tuple(parts)

candidates = []
for line in sys.stdin:
    text = line.strip()
    if not (text.startswith('{') and text.endswith('}')):
        continue
    fields = {}
    for segment in text.strip('{}').split(','):
        if ':' not in segment:
            continue
        key, value = segment.split(':', 1)
        fields[key.strip().lower()] = value.strip()
    if fields.get('platform') != 'iOS Simulator':
        continue
    ident = fields.get('id')
    if not ident:
        continue
    name = fields.get('name', '')
    os_version = fields.get('os', '')
    priority = 2 if 'iPhone' in name else (1 if 'iPad' in name else 0)
    candidates.append((priority, version_key(os_version), name, ident))

if candidates:
    priority, os_version, name, ident = sorted(candidates, reverse=True)[0]
    print(f"platform=iOS Simulator,id={ident}")
PY
    )"
  fi

  if [ -z "${selected:-}" ] && command -v xcrun >/dev/null 2>&1; then
    selected="$(
      xcrun simctl list devices --json 2>/dev/null | python3 - <<'PY'
import json
import sys

def version_key(value: str):
    value = (value or '').replace('latest', '')
    parts = []
    for part in value.replace('-', '.').split('.'):
        if part.isdigit():
            parts.append(int(part))
        elif part:
            parts.append(0)
    return tuple(parts)

try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)

candidates = []
for runtime, entries in (data.get('devices') or {}).items():
    if 'iOS' not in runtime:
        continue
    version = runtime.split('iOS-')[-1]
    vkey = version_key(version)
    for entry in entries or []:
        if not entry.get('isAvailable'):
            continue
        name = entry.get('name') or ''
        ident = entry.get('udid') or ''
        priority = 2 if 'iPhone' in name else (1 if 'iPad' in name else 0)
        candidates.append((priority, vkey, name, ident))

if candidates:
    priority, version_key_value, name, ident = sorted(candidates, reverse=True)[0]
    print(f"platform=iOS Simulator,id={ident}")
PY
    )"
  fi

  if [ -n "${selected:-}" ]; then
    printf '%s\n' "$selected"
  fi
  return 0
}

fallback_sim_destination() {
  if ! command -v xcrun >/dev/null 2>&1; then
    return
  fi

  local chosen=""
  if command -v python3 >/dev/null 2>&1; then
    chosen="$(xcrun simctl list devices --json 2>/dev/null | python3 - <<'PY'
import json
import re
import sys

def version_key(value: str):
    parts = []
    for chunk in re.split(r"[^0-9]+", value or ""):
        if chunk:
            try:
                parts.append(int(chunk))
            except ValueError:
                parts.append(0)
    return tuple(parts)

try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)

best = None
best_info = None
for runtime, devices in (data.get("devices") or {}).items():
    if "iOS" not in runtime:
        continue
    version = runtime.split("iOS-")[-1]
    vkey = version_key(version)
    for device in devices or []:
        if not device.get("isAvailable"):
            continue
        udid = (device.get("udid") or "").strip()
        name = (device.get("name") or "").strip()
        if not udid or not name:
            continue
        priority = 2 if "iPhone" in name else (1 if "iPad" in name else 0)
        booted = 1 if (device.get("state") or "").lower() == "booted" else 0
        candidate = (priority, vkey, booted, name.lower(), udid)
        if best is None or candidate > best:
            best = candidate
            best_info = (name, udid)

if best_info:
    name, udid = best_info
    print(f"platform=iOS Simulator,id={udid},name={name}")
PY
    )"
  fi

  if [ -z "${chosen:-}" ]; then
    chosen="$(xcrun simctl list devices 2>/dev/null | awk '
      /^-- iOS / { ios=1; next }
      /^-- / { ios=0 }
      ios && $0 ~ /\(.*\)/ {
        sub(/^\s+/, "")
        line=$0
        name=line
        sub(/\s*\(.*/, "", name)
        udid=line
        sub(/^[^(]*\(/, "", udid)
        sub(/\).*/, "", udid)
        state=line
        sub(/.*\(/, "", state)
        sub(/\)/, "", state)
        if (state ~ /Booted/) booted=1; else booted=0
        if (name ~ /iPhone/) pri=2
        else if (name ~ /iPad/) pri=1
        else pri=0
        printf "%d\t%d\t%s\t%s\n", pri, booted, name, udid
      }
    ' | sort -t $'\t' -k1,1nr -k2,2nr | head -n1 | awk -F '\t' '{ if (NF>=4) printf "platform=iOS Simulator,id=%s,name=%s", $4, $3 }')"
  fi

  if [ -n "${chosen:-}" ]; then
    printf '%s\n' "$chosen"
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
    SIM_DESTINATION="platform=iOS Simulator,name=iPhone 14"
    ri_log "Falling back to default simulator destination '$SIM_DESTINATION'"
  fi
fi

ri_log "Running DeviceRunner on destination '$SIM_DESTINATION'"

DERIVED_DATA_DIR="$SCREENSHOT_TMP_DIR/derived"
rm -rf "$DERIVED_DATA_DIR"
BUILD_LOG="$ARTIFACTS_DIR/xcodebuild-build.log"

ri_log "Building simulator app with xcodebuild"
if ! xcodebuild \
  -workspace "$WORKSPACE_PATH" \
  -scheme "$SCHEME" \
  -sdk iphonesimulator \
  -configuration Debug \
  -destination "$SIM_DESTINATION" \
  -derivedDataPath "$DERIVED_DATA_DIR" \
  build | tee "$BUILD_LOG"; then
  ri_log "STAGE:XCODE_BUILD_FAILED -> See $BUILD_LOG"
  exit 10
fi

BUILD_SETTINGS="$(xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$SCHEME" -sdk iphonesimulator -configuration Debug -showBuildSettings 2>/dev/null || true)"
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
  if printf '%s' "$SIM_DESTINATION" | grep -q 'id='; then
    SIM_DEVICE_ID="${SIM_DESTINATION##*id=}"
    SIM_DEVICE_ID="${SIM_DEVICE_ID%%,*}"
  fi
  if [ -z "$SIM_DEVICE_ID" ] || [ "$SIM_DEVICE_ID" = "$SIM_DESTINATION" ]; then
    SIM_DEVICE_NAME="$(echo "$SIM_DESTINATION" | sed -n 's/.*name=\([^,]*\).*/\1/p')"
    if [ -n "$SIM_DEVICE_NAME" ] && command -v python3 >/dev/null 2>&1; then
      SIM_DEVICE_ID="$(xcrun simctl list devices --json 2>/dev/null | python3 - "$SIM_DEVICE_NAME" <<'EOS'
import json, sys
name = sys.argv[1]
try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit()
for runtime, devices in (data.get("devices") or {}).items():
    for device in devices or []:
        if not device.get("isAvailable"):
            continue
        if device.get("name") == name:
            print(device.get("udid") or "")
            sys.exit()
EOS
)"
    fi
    if [ -z "$SIM_DEVICE_ID" ] && [ -n "$SIM_DEVICE_NAME" ]; then
      SIM_DEVICE_ID="$(xcrun simctl list devices 2>/dev/null | awk -v target="$SIM_DEVICE_NAME" '
        /^-- iOS / { ios=1; next }
        /^-- / { ios=0 }
        !ios { next }
        {
          line=$0
          gsub(/^\s+/, "", line)
          name=line
          sub(/\s*\(.*/, "", name)
          if (name != target) next
          udid=line
          sub(/^[^(]*\(/, "", udid)
          sub(/\).*/, "", udid)
          print udid
          exit
        }
      ')"
    fi
  fi

  if [ -n "$SIM_DEVICE_ID" ]; then
    ri_log "Booting simulator $SIM_DEVICE_ID"
    xcrun simctl boot "$SIM_DEVICE_ID" >/dev/null 2>&1 || true
    xcrun simctl bootstatus "$SIM_DEVICE_ID" -b
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
    xcrun simctl spawn "$SIM_DEVICE_ID" log stream --style compact --level debug --predicate 'composedMessage CONTAINS "CN1SS"' > "$TEST_LOG" 2>&1 &
  else
    xcrun simctl spawn booted log stream --style compact --level debug --predicate 'composedMessage CONTAINS "CN1SS"' > "$TEST_LOG" 2>&1 &
  fi
  LOG_STREAM_PID=$!
  sleep 2

  ri_log "Installing simulator app bundle"
  if [ -n "$SIM_DEVICE_ID" ]; then
    if ! xcrun simctl install "$SIM_DEVICE_ID" "$APP_BUNDLE_PATH"; then
      ri_log "FATAL: simctl install failed"
      exit 11
    fi
    if ! xcrun simctl launch "$SIM_DEVICE_ID" "$BUNDLE_IDENTIFIER" >/dev/null 2>&1; then
      ri_log "FATAL: simctl launch failed"
      exit 11
    fi
  else
    if ! xcrun simctl install booted "$APP_BUNDLE_PATH"; then
      ri_log "FATAL: simctl install failed"
      exit 11
    fi
    if ! xcrun simctl launch booted "$BUNDLE_IDENTIFIER" >/dev/null 2>&1; then
      ri_log "FATAL: simctl launch failed"
      exit 11
    fi
  fi

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

sleep 3

kill "$LOG_STREAM_PID" >/dev/null 2>&1 || true
wait "$LOG_STREAM_PID" 2>/dev/null || true
LOG_STREAM_PID=0

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
if [ -s "$COMMENT_FILE" ]; then
  cp -f "$COMMENT_FILE" "$ARTIFACTS_DIR/screenshot-comment.md" 2>/dev/null || true
fi
cp -f "$BUILD_LOG" "$ARTIFACTS_DIR/xcodebuild-build.log" 2>/dev/null || true
cp -f "$TEST_LOG" "$ARTIFACTS_DIR/device-runner.log" 2>/dev/null || true

ri_log "STAGE:COMMENT_POST -> Submitting PR feedback"
comment_rc=0
export CN1SS_COMMENT_MARKER="<!-- CN1SS_IOS_COMMENT -->"
export CN1SS_COMMENT_LOG_PREFIX="[run-ios-device-tests]"
if ! cn1ss_post_pr_comment "$COMMENT_FILE" "$SCREENSHOT_PREVIEW_DIR"; then
  comment_rc=$?
fi

exit $comment_rc

