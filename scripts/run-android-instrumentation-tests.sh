#!/usr/bin/env bash
# Boot an Android emulator, execute instrumentation tests, and capture a screenshot artifact.
set -euo pipefail

log() { echo "[run-android-instrumentation-tests] $1"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"
TOOLS_ENV_DIR="$TMPDIR/codenameone-tools/tools"
ENV_FILE="$TOOLS_ENV_DIR/env.sh"
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
else
  log "Workspace tools not provisioned. Run scripts/setup-workspace.sh first." >&2
  exit 1
fi

BUILD_INFO_FILE="$SCRIPT_DIR/.android-build-info"
if [ ! -f "$BUILD_INFO_FILE" ]; then
  log "Android build metadata not found at $BUILD_INFO_FILE. Run scripts/build-android-app.sh first." >&2
  exit 1
fi

# shellcheck disable=SC1090
source "$BUILD_INFO_FILE"

required_vars=(APP_DIR GRADLE_PROJECT_DIR PACKAGE_NAME ARTIFACT_ID WORK_DIR APK_PATH)
for var in "${required_vars[@]}"; do
  if [ -z "${!var:-}" ]; then
    log "Required build metadata '$var' is missing. Regenerate the Android project." >&2
    exit 1
  fi
done

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$ANDROID_SDK_ROOT" ]; then
  if [ -d "/usr/local/lib/android/sdk" ]; then
    ANDROID_SDK_ROOT="/usr/local/lib/android/sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then
    ANDROID_SDK_ROOT="$HOME/Android/Sdk"
  fi
fi
if [ -z "$ANDROID_SDK_ROOT" ] || [ ! -d "$ANDROID_SDK_ROOT" ]; then
  log "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME to a valid installation." >&2
  exit 1
fi
export ANDROID_SDK_ROOT ANDROID_HOME="$ANDROID_SDK_ROOT"

find_tool() {
  local binary="$1"
  shift
  for dir in "$@"; do
    if [ -x "$dir/$binary" ]; then
      printf '%s' "$dir/$binary"
      return 0
    fi
  done
  return 1
}

SDKMANAGER=$(find_tool sdkmanager \
  "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin" \
  "$ANDROID_SDK_ROOT/cmdline-tools/bin" \
  "$ANDROID_SDK_ROOT/tools/bin" || true)
AVDMANAGER=$(find_tool avdmanager \
  "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin" \
  "$ANDROID_SDK_ROOT/cmdline-tools/bin" \
  "$ANDROID_SDK_ROOT/tools/bin" || true)
EMU_BIN="$ANDROID_SDK_ROOT/emulator/emulator"
ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb"

if [ -z "$SDKMANAGER" ] || [ -z "$AVDMANAGER" ] || [ ! -x "$EMU_BIN" ] || [ ! -x "$ADB_BIN" ]; then
  log "Required Android command-line tools were not found." >&2
  log "SDKMANAGER=$SDKMANAGER AVDMANAGER=$AVDMANAGER EMULATOR=$EMU_BIN ADB=$ADB_BIN" >&2
  exit 1
fi

log "Accepting Android SDK licenses"
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true

log "Installing Android 35 ARM system image"
yes | "$SDKMANAGER" --install \
  "platform-tools" \
  "platforms;android-35" \
  "emulator" \
  "system-images;android-35;google_apis;arm64-v8a" >/dev/null

AVD_NAME="cn1-api35-arm"
if ! "$AVDMANAGER" list avd | grep -q "Name: $AVD_NAME"; then
  log "Creating AVD $AVD_NAME"
  printf 'no\n' | "$AVDMANAGER" create avd -n "$AVD_NAME" -k "system-images;android-35;google_apis;arm64-v8a" -d pixel >/dev/null
fi

log "Starting AVD $AVD_NAME in headless mode"
"$EMU_BIN" -avd "$AVD_NAME" -no-boot-anim -no-audio -no-snapshot -no-window -gpu swiftshader_indirect -netfast >/tmp/$AVD_NAME.log 2>&1 &
EMU_PID=$!
cleanup() {
  log "Cleaning up emulator"
  "$ADB_BIN" emu kill >/dev/null 2>&1 || true
  kill "$EMU_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

log "Waiting for emulator to boot"
"$ADB_BIN" wait-for-device
BOOT_COMPLETED="0"
for _ in $(seq 1 120); do
  BOOT_COMPLETED=$("$ADB_BIN" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') || true
  if [ "$BOOT_COMPLETED" = "1" ]; then
    break
  fi
  sleep 5
done

if [ "$BOOT_COMPLETED" != "1" ]; then
  log "Emulator did not boot in the allotted time" >&2
  exit 1
fi

log "Executing connected Android tests"
chmod +x "$GRADLE_PROJECT_DIR/gradlew"
ORIGINAL_JAVA_HOME="$JAVA_HOME"
export JAVA_HOME="$JAVA17_HOME"
(
  cd "$GRADLE_PROJECT_DIR"
  ./gradlew --no-daemon connectedDebugAndroidTest
)
export JAVA_HOME="$ORIGINAL_JAVA_HOME"

log "Launching application to capture screenshot"
"$ADB_BIN" shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
sleep 10

ARTIFACTS_DIR="$WORK_DIR/artifacts"
mkdir -p "$ARTIFACTS_DIR"
SCREENSHOT_PATH="$ARTIFACTS_DIR/${ARTIFACT_ID}-emulator.png"
log "Capturing screenshot at $SCREENSHOT_PATH"
"$ADB_BIN" exec-out screencap -p > "$SCREENSHOT_PATH"
log "Screenshot captured"

persist_build_info() {
  {
    printf "APP_DIR=%q\n" "$APP_DIR"
    printf "GRADLE_PROJECT_DIR=%q\n" "$GRADLE_PROJECT_DIR"
    printf "PACKAGE_NAME=%q\n" "$PACKAGE_NAME"
    printf "ARTIFACT_ID=%q\n" "$ARTIFACT_ID"
    printf "WORK_DIR=%q\n" "$WORK_DIR"
    printf "APK_PATH=%q\n" "$APK_PATH"
    printf "SCREENSHOT_PATH=%q\n" "$SCREENSHOT_PATH"
  } > "$BUILD_INFO_FILE"
}

persist_build_info
log "Updated build metadata with screenshot information"

