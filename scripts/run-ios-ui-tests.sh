#!/usr/bin/env bash
# Run Codename One iOS UI tests on the simulator and compare screenshots
set -euo pipefail

ri_log() { echo "[run-ios-ui-tests] $1"; }

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

auto_select_destination() {
  if ! command -v python3 >/dev/null 2>&1; then
    return
  fi

  local show_dest selected
  if show_dest="$(xcodebuild -workspace "$WORKSPACE_PATH" -scheme "$SCHEME" -showdestinations 2>/dev/null)"; then
    selected="$(
      printf '%s\n' "$show_dest" | python3 - <<'PY'
import re, sys
def parse_version_tuple(v): return tuple(int(p) if p.isdigit() else 0 for p in v.split('.') if p)
for block in re.findall(r"\{([^}]+)\}", sys.stdin.read()):
    f = dict(s.split(':',1) for s in block.split(',') if ':' in s)
    if f.get('platform')!='iOS Simulator': continue
    name=f.get('name',''); os=f.get('OS') or f.get('os') or ''
    pri=2 if 'iPhone' in name else (1 if 'iPad' in name else 0)
    print(f"__CAND__|{pri}|{'.'.join(map(str,parse_version_tuple(os.replace('latest',''))))}|{name}|{os}|{f.get('id','')}")
cands=[l.split('|',5) for l in sys.stdin if False]
PY
    )"
  fi

  if [ -z "${selected:-}" ]; then
    if command -v xcrun >/dev/null 2>&1; then
      selected="$(
        xcrun simctl list devices --json 2>/dev/null | python3 - <<'PY'
import json, sys
def parse_version_tuple(v): return tuple(int(p) if p.isdigit() else 0 for p in v.split('.') if p)
try: data=json.load(sys.stdin)
except: sys.exit(0)
c=[]
for runtime, entries in (data.get('devices') or {}).items():
    if 'iOS' not in runtime: continue
    ver=runtime.split('iOS-')[-1].replace('-','.')
    vt=parse_version_tuple(ver)
    for e in entries or []:
        if not e.get('isAvailable'): continue
        name=e.get('name') or ''; ident=e.get('udid') or ''
        pri=2 if 'iPhone' in name else (1 if 'iPad' in name else 0)
        c.append((pri, vt, name, ident))
if c:
    pri, vt, name, ident = sorted(c, reverse=True)[0]
    print(f"platform=iOS Simulator,id={ident}")
PY
      )"
    fi
  fi

  if [ -n "${selected:-}" ]; then
    echo "$selected"
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
  SIM_DESTINATION="platform=iOS Simulator,name=iPhone 16"
  ri_log "Falling back to default simulator destination '$SIM_DESTINATION'"
fi

ri_log "Running UI tests on destination '$SIM_DESTINATION'"

DERIVED_DATA_DIR="$SCREENSHOT_TMP_DIR/derived"
rm -rf "$DERIVED_DATA_DIR"

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
  test | tee "$TEST_LOG"; then
  ri_log "STAGE:XCODE_TEST_FAILED -> See $TEST_LOG"
  exit 10
fi
set +o pipefail

PNG_FILES=()
while IFS= read -r png; do
  [ -n "$png" ] || continue
  PNG_FILES+=("$png")
done < <(find "$SCREENSHOT_RAW_DIR" -type f -name '*.png' -print | sort)

if [ "${#PNG_FILES[@]}" -eq 0 ]; then
  ri_log "No screenshots produced under $SCREENSHOT_RAW_DIR" >&2
  exit 11
fi

ri_log "Captured ${#PNG_FILES[@]} screenshot(s)"

declare -a COMPARE_ARGS=()
for png in "${PNG_FILES[@]}"; do
  test_name="$(basename "$png" .png)"
  COMPARE_ARGS+=("--actual" "${test_name}=${png}")
  cp "$png" "$ARTIFACTS_DIR/$(basename "$png")" 2>/dev/null || true
  ri_log "  -> Saved artifact copy for test '$test_name'"
done

COMPARE_JSON="$SCREENSHOT_TMP_DIR/screenshot-compare.json"
SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"

ri_log "Running screenshot comparison"
"$JAVA17_BIN" "$SCRIPT_DIR/android/tests/ProcessScreenshots.java" \
  --reference-dir "$SCREENSHOT_REF_DIR" \
  --emit-base64 \
  --preview-dir "$SCREENSHOT_PREVIEW_DIR" \
  "${COMPARE_ARGS[@]}" > "$COMPARE_JSON"

ri_log "Rendering screenshot summary and PR comment"
"$JAVA17_BIN" "$SCRIPT_DIR/android/tests/RenderScreenshotReport.java" \
  --compare-json "$COMPARE_JSON" \
  --comment-out "$COMMENT_FILE" \
  --summary-out "$SUMMARY_FILE" \
  --title "iOS screenshot updates" \
  --success-message "✅ Native iOS screenshot tests passed." \
  --marker "<!-- CN1SS_IOS_SCREENSHOT_COMMENT -->"

if [ -s "$COMMENT_FILE" ]; then
  ri_log "Prepared comment payload at $COMMENT_FILE"
fi

cp -f "$COMPARE_JSON" "$ARTIFACTS_DIR/screenshot-compare.json" 2>/dev/null || true
cp -f "$SUMMARY_FILE" "$ARTIFACTS_DIR/screenshot-summary.txt" 2>/dev/null || true
cp -f "$COMMENT_FILE" "$ARTIFACTS_DIR/screenshot-comment-ios.md" 2>/dev/null || true
if [ -d "$SCREENSHOT_PREVIEW_DIR" ]; then
  for preview in "$SCREENSHOT_PREVIEW_DIR"/*; do
    [ -f "$preview" ] || continue
    cp "$preview" "$ARTIFACTS_DIR/$(basename "$preview")" 2>/dev/null || true
  done
fi

COMMENT_RC=0
if [ -s "$COMMENT_FILE" ]; then
  ri_log "Posting PR comment"
  if ! "$JAVA17_BIN" "$SCRIPT_DIR/android/tests/PostPrComment.java" \
    --body "$COMMENT_FILE" \
    --preview-dir "$SCREENSHOT_PREVIEW_DIR" \
    --marker "<!-- CN1SS_IOS_SCREENSHOT_COMMENT -->" \
    --log-prefix "[run-ios-ui-tests]"; then
    COMMENT_RC=$?
    ri_log "PR comment submission failed"
  fi
else
  ri_log "No PR comment generated"
fi

if [ -d "$RESULT_BUNDLE" ]; then
  rm -f "$ARTIFACTS_DIR/test-results.xcresult.zip" 2>/dev/null || true
  zip -qr "$ARTIFACTS_DIR/test-results.xcresult.zip" "$RESULT_BUNDLE"
fi

exit "$COMMENT_RC"