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

# Derive desired simulator OS from SDKROOT (e.g., iphonesimulator18.5 -> 18 / 5)
SDKROOT_OS="${SDKROOT#iphonesimulator}"
DESIRED_OS_MAJOR="${SDKROOT_OS%%.*}"
DESIRED_OS_MINOR="${SDKROOT_OS#*.}"; [ "$DESIRED_OS_MINOR" = "$SDKROOT_OS" ] && DESIRED_OS_MINOR=""

auto_select_destination() {
  # 1) Try xcodebuild -showdestinations, but skip placeholder ids
  if command -v xcodebuild >/dev/null 2>&1; then
    sel="$(
      xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$SCHEME" -showdestinations 2>/dev/null |
      awk -v wantMajor="${DESIRED_OS_MAJOR:-}" -v wantMinor="${DESIRED_OS_MINOR:-}" '
        function is_uuid(s) { return match(s, /^[0-9A-Fa-f-]{8}-[0-9A-Fa-f-]{4}-[0-9A-Fa-f-]{4}-[0-9A-Fa-f-]{4}-[0-9A-Fa-f-]{12}$/) }
        /platform:iOS Simulator/ && /name:/ && /id:/ {
          os=""; name=""; id="";
          for (i=1;i<=NF;i++) {
            if ($i ~ /^OS:/)   { sub(/^OS:/,"",$i); os=$i }
            if ($i ~ /^name:/) { sub(/^name:/,"",$i); name=$i }
            if ($i ~ /^id:/)   { sub(/^id:/,"",$i); id=$i }
          }
          if (!is_uuid(id)) next;                    # skip placeholders
          gsub(/[^0-9.]/,"",os)                      # keep 18.5 form if present
          pri=(name ~ /iPhone/)?2:((name ~ /iPad/)?1:0)
          # preference score: major-match first, then minor proximity if same major
          split(os, p, "."); major=p[1]; minor=p[2]
          major_ok = (wantMajor=="" || major==wantMajor) ? 1 : 0
          minor_pen = (wantMinor=="" || major!=wantMajor) ? 999 : (minor=="" ? 500 : (minor<wantMinor ? wantMinor-minor : minor-wantMinor))
          printf("%d|%d|%03d|%s|%s\n", pri, major_ok, minor_pen, name, id)
        }
      ' | sort -t'|' -k2,2nr -k1,1nr -k3,3n | head -n1 | awk -F'|' '{print "platform=iOS Simulator,id="$5}'
    )"
    [ -n "$sel" ] && { echo "$sel"; return; }
  fi

  # 2) Fallback: simctl list devices available (plain text)
  if command -v xcrun >/dev/null 2>&1; then
    sel="$(
      xcrun simctl list devices available 2>/dev/null |
      awk -v wantMajor="${DESIRED_OS_MAJOR:-}" -v wantMinor="${DESIRED_OS_MINOR:-}" '
        # Example: "iPhone 16e (18.5) [UDID] (Available)"
        /\[/ && /\)/ {
          line=$0
          name=line; sub(/ *\(.*/,"",name); sub(/^ +/,"",name)
          os=""; if (match(line, /\(([0-9.]+)\)/, a)) os=a[1]
          udid=""; if (match(line, /\[([0-9A-Fa-f-]+)\]/, b)) udid=b[1]
          if (udid=="") next
          pri=(name ~ /iPhone/)?2:((name ~ /iPad/)?1:0)
          split(os, p, "."); major=p[1]; minor=p[2]
          major_ok = (wantMajor=="" || major==wantMajor) ? 1 : 0
          minor_pen = (wantMinor=="" || major!=wantMajor) ? 999 : (minor=="" ? 500 : (minor<wantMinor ? wantMinor-minor : minor-wantMinor))
          printf("%d|%d|%03d|%s|%s\n", pri, major_ok, minor_pen, name, udid)
        }
      ' | sort -t'|' -k2,2nr -k1,1nr -k3,3n | head -n1 | awk -F'|' '{print "platform=iOS Simulator,id="$5}'
    )"
    [ -n "$sel" ] && { echo "$sel"; return; }
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
  SIM_DESTINATION="platform=iOS Simulator,name=iPhone 16,OS=latest"
  ri_log "Falling back to default simulator destination '$SIM_DESTINATION'"
fi

ri_log "Running UI tests on destination '$SIM_DESTINATION'"

DERIVED_DATA_DIR="$SCREENSHOT_TMP_DIR/derived"
rm -rf "$DERIVED_DATA_DIR"

ri_log "STAGE:BUILD_FOR_TESTING -> xcodebuild build-for-testing"
set -o pipefail
if ! xcodebuild \
  -workspace "$WORKSPACE_PATH" \
  -scheme "$SCHEME" \
  -sdk iphonesimulator \
  -configuration Debug \
  -destination "$SIM_DESTINATION" \
  -derivedDataPath "$DERIVED_DATA_DIR" \
  build-for-testing | tee "$ARTIFACTS_DIR/xcodebuild-build.log"; then
  ri_log "STAGE:BUILD_FAILED -> See $ARTIFACTS_DIR/xcodebuild-build.log"
  exit 1
fi

# Prefer the product we just built; fall back to the optional arg2 if provided
AUT_APP="$(/bin/ls -1d "$DERIVED_DATA_DIR"/Build/Products/Debug-iphonesimulator/*.app 2>/dev/null | head -n1 || true)"
if [ -z "$AUT_APP" ] && [ -n "$APP_BUNDLE_PATH" ] && [ -d "$APP_BUNDLE_PATH" ]; then
  AUT_APP="$APP_BUNDLE_PATH"
fi
if [ -n "$AUT_APP" ] && [ -d "$AUT_APP" ]; then
  ri_log "Using simulator app bundle at $AUT_APP"
  AUT_BUNDLE_ID=$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$AUT_APP/Info.plist" 2>/dev/null || true)
  if [ -n "$AUT_BUNDLE_ID" ]; then
    export CN1_AUT_BUNDLE_ID="$AUT_BUNDLE_ID"
    ri_log "Exported CN1_AUT_BUNDLE_ID=$AUT_BUNDLE_ID"
  fi

  # Resolve a UDID for the chosen destination name (no regex groups to keep BSD awk happy)
  SIM_NAME="$(printf '%s\n' "$SIM_DESTINATION" | sed -n 's/.*name=\([^,]*\).*/\1/p')"
  SIM_UDID="$(
    xcrun simctl list devices available 2>/dev/null | \
    awk -v name="$SIM_NAME" '
      # Match a line that contains the selected device name followed by " ("
      index($0, name" (") && index($0, "[") {
        ud=$0
        sub(/^.*\[/,"",ud)     # drop everything up to and including the first '['
        sub(/\].*$/,"",ud)     # drop everything after the closing ']'
        if (length(ud)>0) { print ud; exit }
      }'
  )"

  if [ -n "$SIM_UDID" ]; then
    xcrun simctl bootstatus "$SIM_UDID" -b || xcrun simctl boot "$SIM_UDID"
    xcrun simctl install "$SIM_UDID" "$AUT_APP" || true
    if [ -n "$AUT_BUNDLE_ID" ]; then
      # Warm launch so the GL surface is alive before XCTest attaches
      xcrun simctl launch "$SIM_UDID" "$AUT_BUNDLE_ID" --args -AppleLocale en_US -AppleLanguages "(en)" || true
      sleep 1
    fi
  else
    ri_log "WARN: Could not resolve simulator UDID for '$SIM_NAME'; skipping warm launch"
  fi
fi

# Run only the UI test bundle
UI_TEST_TARGET="${UI_TEST_TARGET:-HelloCodenameOneUITests}"
XCODE_TEST_FILTERS=(
  -only-testing:"${UI_TEST_TARGET}"
  -skip-testing:HelloCodenameOneTests
)

set -o pipefail
if ! xcodebuild \
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
  test-without-building | tee "$TEST_LOG"; then
  ri_log "STAGE:XCODE_TEST_FAILED -> See $TEST_LOG"
  exit 10
fi
set +o pipefail
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
if [ -s "$COMMENT_FILE" ]; then
  cp -f "$COMMENT_FILE" "$ARTIFACTS_DIR/screenshot-comment.md" 2>/dev/null || true
fi

ri_log "STAGE:COMMENT_POST -> Submitting PR feedback"
comment_rc=0
if ! cn1ss_post_pr_comment "$COMMENT_FILE" "$SCREENSHOT_PREVIEW_DIR"; then
  comment_rc=$?
fi

exit $comment_rc

