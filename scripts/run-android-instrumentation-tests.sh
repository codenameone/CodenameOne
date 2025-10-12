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

if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  log "JAVA17_HOME is not configured. Run scripts/setup-workspace.sh to provision Java 17." >&2
  exit 1
fi

export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA17_HOME/bin:$PATH"

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
  elif [ -d "/usr/lib/android-sdk" ]; then
    ANDROID_SDK_ROOT="/usr/lib/android-sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then
    ANDROID_SDK_ROOT="$HOME/Android/Sdk"
  fi
fi
if [ -z "$ANDROID_SDK_ROOT" ] || [ ! -d "$ANDROID_SDK_ROOT" ]; then
  log "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME to a valid installation." >&2
  exit 1
fi
export ANDROID_SDK_ROOT ANDROID_HOME="$ANDROID_SDK_ROOT"

ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb"

if [ ! -x "$ADB_BIN" ]; then
  log "adb binary not found. Ensure Android platform-tools are installed." >&2
  log "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
  exit 1
fi

ensure_emulator_ready() {
  log "Waiting for emulator to register with adb"
  "$ADB_BIN" start-server >/dev/null 2>&1 || true

  local adb_device_timeout=180
  for _ in $(seq 1 "$adb_device_timeout"); do
    EMULATOR_SERIAL=$("$ADB_BIN" devices | awk 'NR>1 && /^emulator-/{print $1; exit}') || true
    if [ -n "${EMULATOR_SERIAL:-}" ]; then
      break
    fi
    sleep 1
  done

  if [ -z "${EMULATOR_SERIAL:-}" ]; then
    log "No emulator appeared in adb devices within ${adb_device_timeout}s" >&2
    "$ADB_BIN" devices >&2 || true
    cat <<'EOF' >&2
Ensure an Android emulator is booted before invoking this script. In CI, run it
inside the reactivecircus/android-emulator-runner action (see scripts-android.yml).
EOF
    exit 1
  fi

  log "Waiting for emulator to boot"
  local boot_completed="0"
  for _ in $(seq 1 120); do
    boot_completed=$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') || true
    if [ "$boot_completed" = "1" ]; then
      break
    fi
    sleep 5
  done

  if [ "$boot_completed" != "1" ]; then
    log "Emulator did not boot in the allotted time" >&2
    exit 1
  fi
}

ensure_emulator_ready

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
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
sleep 10

ARTIFACTS_DIR="$WORK_DIR/artifacts"
mkdir -p "$ARTIFACTS_DIR"
SCREENSHOT_PATH="$ARTIFACTS_DIR/${ARTIFACT_ID}-emulator.png"
log "Capturing screenshot at $SCREENSHOT_PATH"
"$ADB_BIN" -s "$EMULATOR_SERIAL" exec-out screencap -p > "$SCREENSHOT_PATH"
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

