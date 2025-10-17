#!/usr/bin/env bash
# Run Codename One iOS automation tests in the simulator and compare screenshots
set -euo pipefail

ris_log() { echo "[run-ios-simulator-tests] $1"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CN1SS_BUILD_SCRIPT="$SCRIPT_DIR/tools/cn1ss-java/build-cn1ss-tools.sh"
CN1SS_JAR="$SCRIPT_DIR/tools/cn1ss-java/cn1ss-tools.jar"
SCREENSHOT_REF_DIR="$SCRIPT_DIR/ios/screenshots"

if [ ! -x "$CN1SS_BUILD_SCRIPT" ]; then
  ris_log "CN1SS build helper not found at $CN1SS_BUILD_SCRIPT" >&2
  exit 1
fi

"$CN1SS_BUILD_SCRIPT"

if [ ! -f "$CN1SS_JAR" ]; then
  ris_log "Failed to build CN1SS helper jar at $CN1SS_JAR" >&2
  exit 1
fi

CN1SS_TOOL_CMD=(java -cp "$CN1SS_JAR" com.codename1.tools.cn1ss.CN1SSTool)

mkdir -p "$SCREENSHOT_REF_DIR" 2>/dev/null || true

APP_BUNDLE_PATH="${1:-}"
if [ -z "$APP_BUNDLE_PATH" ]; then
  ris_log "Usage: $0 <path-to-ios-app-bundle>" >&2
  exit 2
fi
if [ ! -d "$APP_BUNDLE_PATH" ]; then
  ris_log "App bundle not found: $APP_BUNDLE_PATH" >&2
  exit 2
fi

INFO_PLIST="$APP_BUNDLE_PATH/Info.plist"
if [ ! -f "$INFO_PLIST" ]; then
  ris_log "Info.plist not found inside app bundle: $INFO_PLIST" >&2
  exit 2
fi

if ! command -v /usr/libexec/PlistBuddy >/dev/null 2>&1; then
  ris_log "PlistBuddy command not available" >&2
  exit 2
fi

BUNDLE_ID=$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$INFO_PLIST" 2>/dev/null || true)
if [ -z "$BUNDLE_ID" ]; then
  ris_log "Unable to determine bundle identifier from $INFO_PLIST" >&2
  exit 2
fi
ris_log "Detected bundle identifier $BUNDLE_ID"

DEVICE_NAME="${IOS_SIMULATOR_DEVICE:-iPhone 15}"

if ! command -v xcrun >/dev/null 2>&1; then
  ris_log "xcrun command not available" >&2
  exit 2
fi

TMP_ROOT="${TMPDIR:-/tmp}"
WORK_DIR="$(mktemp -d "$TMP_ROOT/cn1-ios-tests.XXXXXX")"
DEVICE_UDID=""
DELETE_DEVICE=0
cleanup() {
  rm -rf "$WORK_DIR" >/dev/null 2>&1 || true
  if [ "$DELETE_DEVICE" -eq 1 ] && [ -n "$DEVICE_UDID" ]; then
    xcrun simctl delete "$DEVICE_UDID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts}"
mkdir -p "$ARTIFACTS_DIR"

LOG_FILE="$WORK_DIR/simulator.log"
SCREENSHOT_TMP_DIR="$WORK_DIR/screenshots"
SCREENSHOT_PREVIEW_DIR="$WORK_DIR/preview"
mkdir -p "$SCREENSHOT_TMP_DIR" "$SCREENSHOT_PREVIEW_DIR"

DEVICE_RUNTIME=$(xcrun simctl list runtimes --json | "${CN1SS_TOOL_CMD[@]}" simctl best-runtime)
if [ -z "$DEVICE_RUNTIME" ]; then
  ris_log "Failed to determine available iOS runtime" >&2
  exit 3
fi
ris_log "Using simulator runtime $DEVICE_RUNTIME"

DEVICE_TYPE=$(xcrun simctl list devicetypes --json | "${CN1SS_TOOL_CMD[@]}" simctl device-type --device-name "$DEVICE_NAME")
if [ -z "$DEVICE_TYPE" ]; then
  ris_log "Simulator device type '$DEVICE_NAME' not available" >&2
  exit 3
fi
ris_log "Using simulator device type $DEVICE_TYPE"

DEVICE_INFO=$(xcrun simctl list devices --json | "${CN1SS_TOOL_CMD[@]}" simctl device-info --runtime "$DEVICE_RUNTIME" --device-name "$DEVICE_NAME")

DEVICE_STATE=""
DEVICE_UDID=""
if [ -n "$DEVICE_INFO" ]; then
  DEVICE_UDID="${DEVICE_INFO%%|*}"
  DEVICE_STATE="${DEVICE_INFO##*|}"
fi

if [ -z "$DEVICE_UDID" ]; then
  DEVICE_LABEL="CN1-Automation-$(date +%s)"
  DEVICE_UDID=$(xcrun simctl create "$DEVICE_LABEL" "$DEVICE_TYPE" "$DEVICE_RUNTIME")
  DEVICE_STATE="Shutdown"
  DELETE_DEVICE=1
  ris_log "Created temporary simulator $DEVICE_LABEL ($DEVICE_UDID)"
else
  ris_log "Reusing existing simulator $DEVICE_NAME ($DEVICE_UDID)"
fi

RIS_SHUTDOWN=0
if [ "$DEVICE_STATE" = "Booted" ]; then
  ris_log "Simulator already booted"
else
  xcrun simctl boot "$DEVICE_UDID" >/dev/null
  RIS_SHUTDOWN=1
fi
xcrun simctl bootstatus "$DEVICE_UDID" -b >/dev/null

ris_log "Installing app bundle"
xcrun simctl install "$DEVICE_UDID" "$APP_BUNDLE_PATH"

ris_log "Launching $BUNDLE_ID in simulator"
set +e
xcrun simctl launch "$DEVICE_UDID" --console "$BUNDLE_ID" >"$LOG_FILE" 2>&1
LAUNCH_RC=$?
set -e
ris_log "Simulator launch completed with status $LAUNCH_RC"

cp -f "$LOG_FILE" "$ARTIFACTS_DIR/simulator.log" 2>/dev/null || true

if [ "$RIS_SHUTDOWN" -eq 1 ]; then
  xcrun simctl shutdown "$DEVICE_UDID" >/dev/null || true
fi

TEST_NAMES=()
if [ -s "$LOG_FILE" ]; then
  while IFS= read -r line; do
    [ -n "$line" ] || continue
    TEST_NAMES+=("$line")
  done < <("${CN1SS_TOOL_CMD[@]}" chunks tests "$LOG_FILE" 2>/dev/null || true)
fi

if [ "${#TEST_NAMES[@]}" -eq 0 ]; then
  ris_log "No CN1SS screenshot streams detected in simulator output" >&2
  exit 4
fi

ris_log "Detected CN1SS test streams: ${TEST_NAMES[*]}"

COMPARE_ARGS=()
for test in "${TEST_NAMES[@]}"; do
  sanitized="$test"
  output="$SCREENSHOT_TMP_DIR/${sanitized}.png"
  if "${CN1SS_TOOL_CMD[@]}" chunks extract --decode --test "$test" "$LOG_FILE" >"$output" 2>/dev/null; then
    if [ -s "$output" ]; then
      COMPARE_ARGS+=("--actual" "${sanitized}=${output}")
      cp -f "$output" "$ARTIFACTS_DIR/${sanitized}.png" 2>/dev/null || true
      continue
    fi
  fi
  ris_log "Failed to decode screenshot payload for test '$test'" >&2
  rm -f "$output" 2>/dev/null || true
done

if [ "${#COMPARE_ARGS[@]}" -eq 0 ]; then
  ris_log "No screenshots decoded from simulator output" >&2
  exit 4
fi

COMPARE_JSON="$WORK_DIR/screenshot-compare.json"
COMMENT_FILE="$WORK_DIR/screenshot-comment.md"
SUMMARY_FILE="$WORK_DIR/screenshot-summary.txt"

export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"
"${CN1SS_TOOL_CMD[@]}" compare \
  --reference-dir "$SCREENSHOT_REF_DIR" \
  --emit-base64 \
  --preview-dir "$SCREENSHOT_PREVIEW_DIR" \
  --preview-source-dir "$SCREENSHOT_PREVIEW_DIR" \
  --json-out "$COMPARE_JSON" \
  --summary-out "$SUMMARY_FILE" \
  --comment-out "$COMMENT_FILE" \
  --platform "iOS" \
  "${COMPARE_ARGS[@]}"

cp -f "$COMPARE_JSON" "$ARTIFACTS_DIR/screenshot-compare.json" 2>/dev/null || true
if [ -s "$COMMENT_FILE" ]; then
  cp -f "$COMMENT_FILE" "$ARTIFACTS_DIR/screenshot-comment.md" 2>/dev/null || true
fi
if [ -d "$SCREENSHOT_PREVIEW_DIR" ]; then
  tar -C "$SCREENSHOT_PREVIEW_DIR" -czf "$ARTIFACTS_DIR/preview-images.tgz" . 2>/dev/null || true
fi

if [ -s "$SUMMARY_FILE" ]; then
  while IFS='|' read -r status test message copy_flag path preview_note; do
    [ -n "${test:-}" ] || continue
    ris_log "Test '${test}': ${message}"
    if [ "$status" = "equal" ] && [ -n "${path:-}" ]; then
      rm -f "$path" 2>/dev/null || true
    fi
    if [ -n "${preview_note:-}" ]; then
      ris_log "  Preview note: ${preview_note}"
    fi
  done <"$SUMMARY_FILE"
else
  ris_log "All simulator screenshots matched stored references"
fi

comment_rc=0
if [ -s "$COMMENT_FILE" ]; then
  ris_log "Posting PR comment for screenshot differences"
  if ! "${CN1SS_TOOL_CMD[@]}" comment --body "$COMMENT_FILE" --preview-dir "$SCREENSHOT_PREVIEW_DIR"; then
    ris_log "PR comment post failed"
    comment_rc=1
  else
    ris_log "Posted screenshot comparison comment"
  fi
else
  ris_log "No screenshot differences detected; skipping PR comment"
fi

exit $comment_rc
