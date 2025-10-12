#!/usr/bin/env bash
# Run instrumentation tests against the generated Codename One Android project
set -euo pipefail

ra_log() { echo "[run-android-instrumentation-tests] $1"; }

if [ $# -lt 1 ]; then
  ra_log "Usage: $0 <gradle_project_dir>" >&2
  exit 1
fi

GRADLE_PROJECT_DIR="$1"

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
