#!/usr/bin/env bash
# Run instrumentation tests against the generated Codename One Android project
set -euo pipefail

ra_log() { echo "[run-android-instrumentation-tests] $1"; }

if [ $# -lt 1 ]; then
  ra_log "Usage: $0 <gradle_project_dir> [package_name]" >&2
  exit 1
fi

GRADLE_PROJECT_DIR="$1"
PACKAGE_NAME="${2:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR%/}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

ra_log "Loading workspace environment from $ENV_FILE"
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
else
  ra_log "Workspace environment file not found. Run scripts/setup-workspace.sh before this script." >&2
  exit 1
fi

if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  ra_log "JAVA17_HOME validation failed. Current value: ${JAVA17_HOME:-<unset>}" >&2
  exit 1
fi

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$ANDROID_SDK_ROOT" ]; then
  if [ -d "/usr/local/lib/android/sdk" ]; then ANDROID_SDK_ROOT="/usr/local/lib/android/sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then ANDROID_SDK_ROOT="$HOME/Android/Sdk"; fi
fi
if [ -z "$ANDROID_SDK_ROOT" ] || [ ! -d "$ANDROID_SDK_ROOT" ]; then
  ra_log "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME to a valid installation." >&2
  exit 1
fi
export ANDROID_SDK_ROOT ANDROID_HOME="$ANDROID_SDK_ROOT"

ADB_BIN="$(command -v adb || true)"
if [ -z "$ADB_BIN" ] && [ -x "$ANDROID_SDK_ROOT/platform-tools/adb" ]; then
  ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb"
fi
if [ -z "$ADB_BIN" ]; then
  ra_log "adb executable not found in PATH or Android SDK" >&2
  exit 1
fi
"$ADB_BIN" start-server >/dev/null 2>&1 || true
ra_log "Connected Android devices before selecting target:"
"$ADB_BIN" devices -l || true

DEVICE_SERIAL="${ANDROID_SERIAL:-}"
if [ -z "$DEVICE_SERIAL" ]; then
  DEVICE_SERIAL=$("$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1; exit}')
fi
if [ -z "$DEVICE_SERIAL" ]; then
  ra_log "No booted Android emulator/device detected" >&2
  exit 1
fi

ADB_TARGET=("$ADB_BIN" -s "$DEVICE_SERIAL")
adb_target() { "${ADB_TARGET[@]}" "$@"; }

ra_log "Using Android device serial $DEVICE_SERIAL"
adb_target wait-for-device

wait_for_property() {
  local property="$1" expected="$2" attempts="${3:-60}" delay="${4:-5}" value=""
  for attempt in $(seq 1 "$attempts"); do
    value="$(adb_target shell getprop "$property" 2>/dev/null | tr -d '\r')"
    if [ "$value" = "$expected" ]; then
      return 0
    fi
    sleep "$delay"
  done
  ra_log "Timed out waiting for $property to become $expected (last value: ${value:-<unset>})" >&2
  return 1
}

ra_log "Waiting for emulator to finish booting"
wait_for_property sys.boot_completed 1 120 5
wait_for_property dev.bootcomplete 1 120 5 || true
adb_target shell input keyevent 82 >/dev/null 2>&1 || true
ra_log "Device build fingerprint: $(adb_target shell getprop ro.build.fingerprint | tr -d '\r')"
ra_log "Installed instrumentation targets:"
adb_target shell pm list instrumentation || true

if [ -z "$PACKAGE_NAME" ]; then
  PACKAGE_NAME="$(adb_target shell pm list instrumentation 2>/dev/null | sed -n 's/.*target=\([^)]*\)).*/\1/p' | tr -d '\r' | head -n 1 || true)"
  if [ -n "$PACKAGE_NAME" ]; then
    ra_log "Detected application package from instrumentation list: $PACKAGE_NAME"
  fi
fi

if [ ! -d "$GRADLE_PROJECT_DIR" ]; then
  ra_log "Gradle project directory not found: $GRADLE_PROJECT_DIR" >&2
  exit 1
fi

if [ ! -x "$GRADLE_PROJECT_DIR/gradlew" ]; then
  chmod +x "$GRADLE_PROJECT_DIR/gradlew"
fi

ra_log "Running instrumentation tests from $GRADLE_PROJECT_DIR"
ORIGINAL_JAVA_HOME="${JAVA_HOME:-}"; export JAVA_HOME="$JAVA17_HOME"
(
  cd "$GRADLE_PROJECT_DIR"
  ./gradlew --no-daemon connectedDebugAndroidTest
)
export JAVA_HOME="$ORIGINAL_JAVA_HOME"
ra_log "Instrumentation tests completed successfully"

if [ -z "$PACKAGE_NAME" ]; then
  ra_log "Application package name not available; skipping screenshot capture" >&2
  exit 1
fi

ra_log "Launching $PACKAGE_NAME before capturing screenshot"
if adb_target shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; then
  ra_log "Application launch via monkey succeeded"
else
  ra_log "Failed to launch $PACKAGE_NAME via monkey; attempting explicit resolve" >&2
  MAIN_ACTIVITY="$(adb_target shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER "$PACKAGE_NAME" 2>/dev/null | tr -d '\r' | tail -n 1)"
  MAIN_ACTIVITY="${MAIN_ACTIVITY##* }"
  if [[ -z "$MAIN_ACTIVITY" || "$MAIN_ACTIVITY" != */* ]]; then
    ra_log "Unable to resolve launchable activity for $PACKAGE_NAME (cmd package output: ${MAIN_ACTIVITY:-<empty>})" >&2
    exit 1
  fi
  ra_log "Resolved main activity $MAIN_ACTIVITY; starting via am"
  if ! adb_target shell am start -n "$MAIN_ACTIVITY" >/dev/null 2>&1; then
    ra_log "Failed to start $MAIN_ACTIVITY via am start" >&2
    exit 1
  fi
fi

sleep 5

SCREENSHOT_DEVICE_PATH="/sdcard/Download/cn1-instrumentation-screenshot.png"
SCREENSHOT_DIR="$REPO_ROOT/out/android-emulator"
SCREENSHOT_PATH="$SCREENSHOT_DIR/hello-codenameone.png"
mkdir -p "$SCREENSHOT_DIR"
rm -f "$SCREENSHOT_PATH"

ra_log "Capturing emulator screenshot to $SCREENSHOT_DEVICE_PATH"
adb_target shell rm "$SCREENSHOT_DEVICE_PATH" >/dev/null 2>&1 || true
adb_target shell screencap -p "$SCREENSHOT_DEVICE_PATH" >/dev/null || {
  ra_log "Failed to capture screenshot on device" >&2
  exit 1
}

ra_log "Pulling screenshot to $SCREENSHOT_PATH"
adb_target pull "$SCREENSHOT_DEVICE_PATH" "$SCREENSHOT_PATH" >/dev/null || {
  ra_log "Failed to pull screenshot from device" >&2
  exit 1
}
adb_target shell rm "$SCREENSHOT_DEVICE_PATH" >/dev/null 2>&1 || true
ra_log "Screenshot available at $SCREENSHOT_PATH"
