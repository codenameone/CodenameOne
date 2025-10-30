#!/usr/bin/env bash
# Run Codename One iOS UI tests on the simulator and export screenshot attachments
set -euo pipefail

ri_log() { echo "[run-ios-ui-tests] $1"; }

if [ $# -lt 1 ]; then
  ri_log "Usage: $0 <workspace_path> [app_bundle] [scheme]" >&2
  exit 2
fi

WORKSPACE_PATH="$1"
APP_BUNDLE_PATH="${2:-}"
REQUESTED_SCHEME="${3:-}"

# Backwards compatibility: if the optional app bundle argument is omitted but the
# second parameter was historically used for the scheme, treat it as such when it
# is not a directory path.
if [ -n "$APP_BUNDLE_PATH" ] && [ ! -d "$APP_BUNDLE_PATH" ] && [ -z "$REQUESTED_SCHEME" ]; then
  REQUESTED_SCHEME="$APP_BUNDLE_PATH"
  APP_BUNDLE_PATH=""
fi

if [ -n "$APP_BUNDLE_PATH" ]; then
  ri_log "Ignoring deprecated app bundle argument '$APP_BUNDLE_PATH'"
fi

if [ ! -d "$WORKSPACE_PATH" ]; then
  ri_log "Workspace not found at $WORKSPACE_PATH" >&2
  exit 3
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

if [ -f "$ENV_FILE" ]; then
  ri_log "Loading workspace environment from $ENV_FILE"
  # shellcheck disable=SC1090
  source "$ENV_FILE"
else
  ri_log "Workspace environment not found at $ENV_FILE (continuing with current shell env)"
fi

if ! command -v xcodebuild >/dev/null 2>&1; then
  ri_log "xcodebuild not found" >&2
  exit 3
fi
if ! command -v xcrun >/dev/null 2>&1; then
  ri_log "xcrun not found" >&2
  exit 3
fi

# Use the same Xcode as the build step when available
export DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode_16.4.app/Contents/Developer}"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
mkdir -p "$ARTIFACTS_DIR"
TEST_LOG="$ARTIFACTS_DIR/xcodebuild-test.log"

DEFAULT_SCHEME="HelloCodenameOne-CI"
if [ -z "$REQUESTED_SCHEME" ]; then
  SCHEME="$DEFAULT_SCHEME"
elif [ "$REQUESTED_SCHEME" != "$DEFAULT_SCHEME" ]; then
  ri_log "Ignoring requested scheme '$REQUESTED_SCHEME'; forcing $DEFAULT_SCHEME"
  SCHEME="$DEFAULT_SCHEME"
else
  SCHEME="$DEFAULT_SCHEME"
fi
ri_log "Using scheme $SCHEME"

detect_app_bundle_id() {
  local workspace="$1" scheme="$2"
  python3 - "$workspace" "$scheme" <<'PY'
import json
import subprocess
import sys

workspace, scheme = sys.argv[1:3]
cmd = [
    "xcodebuild",
    "-workspace",
    workspace,
    "-scheme",
    scheme,
    "-showBuildSettings",
    "-json",
]
proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
if proc.returncode != 0:
    sys.exit(0)
try:
    payload = json.loads(proc.stdout.decode("utf-8"))
except json.JSONDecodeError:
    sys.exit(0)
for entry in payload:
    target = entry.get("target") or ""
    if target.endswith("UITests"):
        continue
    bundle = (entry.get("buildSettings") or {}).get("PRODUCT_BUNDLE_IDENTIFIER")
    if bundle:
        print(bundle)
        break
PY
}

DETECTED_APP_BUNDLE_ID=""
if DETECTED_APP_BUNDLE_ID="$(detect_app_bundle_id "$WORKSPACE_PATH" "$SCHEME" 2>/dev/null)"; then
  DETECTED_APP_BUNDLE_ID="${DETECTED_APP_BUNDLE_ID//[$'\r\n']}"
fi
if [ -n "$DETECTED_APP_BUNDLE_ID" ]; then
  ri_log "Detected bundle identifier $DETECTED_APP_BUNDLE_ID from build settings"
fi

DEFAULT_APP_BUNDLE_ID="com.codenameone.examples.HelloCodenameOne"
APP_BUNDLE_ID="${CN1_APP_BUNDLE_ID:-${IOS_APP_BUNDLE_ID:-${DETECTED_APP_BUNDLE_ID:-$DEFAULT_APP_BUNDLE_ID}}}"
export CN1_APP_BUNDLE_ID="$APP_BUNDLE_ID"
ri_log "Targeting app bundle $APP_BUNDLE_ID"

SCREENSHOT_TMP_DIR="$(mktemp -d "${TMPDIR}/cn1-ios-tests-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1-ios-tests")"
RESULT_BUNDLE="$SCREENSHOT_TMP_DIR/test-results.xcresult"
DERIVED_DATA_DIR="$SCREENSHOT_TMP_DIR/derived"
rm -rf "$DERIVED_DATA_DIR"

find_sim_udid() {
  local desired="${1:-iPhone 16}" json
  if ! json="$(xcrun simctl list devices --json 2>/dev/null)"; then
    return 1
  fi
  SIMCTL_JSON="$json" python3 - "$desired" <<'PY2'
import json, os, sys


def normalize(name: str) -> str:
    return name.strip().lower()


target = normalize(sys.argv[1])
try:
    data = json.loads(os.environ.get("SIMCTL_JSON", "{}"))
except json.JSONDecodeError:
    sys.exit(1)

for runtime, devices in (data.get("devices") or {}).items():
    if "iOS" not in runtime:
        continue
    for device in devices or []:
        if not device.get("isAvailable"):
            continue
        if normalize(device.get("name", "")) == target:
            udid = device.get("udid", "")
            if udid:
                print(udid)
            sys.exit(0)

print("")
PY2
}

ensure_booted_device() {
  local name="$1" udid
  if ! udid="$(find_sim_udid "$name")"; then
    ri_log "Failed to query simulator udid for '$name'"
    return 1
  fi
  if [ -z "$udid" ]; then
    ri_log "Simulator '$name' not found"
    return 1
  fi
  ri_log "Using simulator '$name' (udid=$udid)"
  if ! xcrun simctl bootstatus "$udid" -b >/dev/null 2>&1; then
    ri_log "Booting simulator '$name'"
    xcrun simctl boot "$udid" >/dev/null
    xcrun simctl bootstatus "$udid" -b
  fi
  SIM_UDID="$udid"
}

SIM_DEVICE_NAME="${IOS_SIM_DEVICE_NAME:-iPhone 16}"
SIM_UDID=""
ensure_booted_device "$SIM_DEVICE_NAME" || true
if [ -z "$SIM_UDID" ]; then
  ri_log "Falling back to 'booted' simulator for destination"
  SIM_DESTINATION="platform=iOS Simulator,name=$SIM_DEVICE_NAME"
else
  SIM_DESTINATION="id=$SIM_UDID"
fi

ri_log "Running UI tests on destination '$SIM_DESTINATION'"

UI_TEST_TARGET="HelloCodenameOneUITests"
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
  ri_log "xcodebuild test failed – see $TEST_LOG"
  exit 10
fi
set +o pipefail

EXPORT_DIR="$SCREENSHOT_TMP_DIR/xcresult-export"
rm -rf "$EXPORT_DIR"
mkdir -p "$EXPORT_DIR"

ri_log "Exporting screenshot attachments from $RESULT_BUNDLE"
EXPORT_HELPER="$SCRIPT_DIR/ios/export_xcresult_attachments.py"
if ! python3 "$EXPORT_HELPER" "$RESULT_BUNDLE" "$EXPORT_DIR"; then
  ri_log "xcresulttool export failed"
  exit 11
fi

copy_exported_screenshots() {
  local src_dir="$1" found=0 safe_name dest
  while IFS= read -r -d '' shot; do
    found=1
    safe_name="$(basename "$shot")"
    safe_name="${safe_name//[^A-Za-z0-9_.-]/_}"
    dest="$ARTIFACTS_DIR/$safe_name"
    cp -f "$shot" "$dest"
    ri_log "Saved screenshot attachment to $dest"
  done < <(find "$src_dir" -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' \) -print0)

  if [ "$found" -eq 0 ]; then
    ri_log "No PNG/JPEG attachments found in exported results"
  fi
}

copy_exported_screenshots "$EXPORT_DIR"

SUMMARY_FILE="$ARTIFACTS_DIR/screenshot-summary.txt"
{
  echo "iOS UI test screenshots exported on $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  echo "Workspace: $WORKSPACE_PATH"
  echo "Scheme: $SCHEME"
  echo "Simulator: ${SIM_UDID:-booted}"
  find "$ARTIFACTS_DIR" -maxdepth 1 -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' \) -print 2>/dev/null \
    | sed 's#.*/# - #' || true
} > "$SUMMARY_FILE"
ri_log "Wrote summary to $SUMMARY_FILE"

exit 0
