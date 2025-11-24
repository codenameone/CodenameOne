#!/usr/bin/env bash
# Build a sample "Hello Codename One" Android application using the locally-built Codename One Android port
set -euo pipefail

ba_log() { echo "[build-android-app] $1"; }

REPO_ROOT="$(pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR%/}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"

ENV_FILE="$ENV_DIR/env.sh"
ba_log "Loading workspace environment from $ENV_FILE"
if [ -f "$ENV_FILE" ]; then
  ba_log "Workspace environment file metadata"
  ls -l "$ENV_FILE" | while IFS= read -r line; do ba_log "$line"; done
  ba_log "Workspace environment file contents"
  sed 's/^/[build-android-app] ENV: /' "$ENV_FILE"
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  ba_log "Loaded environment: JAVA_HOME=${JAVA_HOME:-<unset>} JAVA17_HOME=${JAVA17_HOME:-<unset>} MAVEN_HOME=${MAVEN_HOME:-<unset>}"
else
  ba_log "Workspace tools not found. Run scripts/setup-workspace.sh before this script." >&2
  exit 1
fi

# --- Tool validations ---
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  ba_log "JAVA_HOME validation failed. Current value: ${JAVA_HOME:-<unset>}" >&2
  if [ -n "${JAVA_HOME:-}" ]; then
    ba_log "Contents of JAVA_HOME directory"
    if [ -d "$JAVA_HOME" ]; then ls -l "$JAVA_HOME" | while IFS= read -r line; do ba_log "$line"; done; else ba_log "JAVA_HOME directory does not exist"; fi
  fi
  ba_log "JAVA_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  ba_log "JAVA17_HOME validation failed. Current value: ${JAVA17_HOME:-<unset>}" >&2
  if [ -n "${JAVA17_HOME:-}" ]; then
    ba_log "Contents of JAVA17_HOME directory"
    if [ -d "$JAVA17_HOME" ]; then ls -l "$JAVA17_HOME" | while IFS= read -r line; do ba_log "$line"; done; else ba_log "JAVA17_HOME directory does not exist"; fi
  fi
  ba_log "JAVA17_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${MAVEN_HOME:-}" ] || [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  ba_log "MAVEN_HOME validation failed. Current value: ${MAVEN_HOME:-<unset>}" >&2
  if [ -n "${MAVEN_HOME:-}" ]; then
    ba_log "Contents of MAVEN_HOME directory"
    if [ -d "$MAVEN_HOME" ]; then ls -l "$MAVEN_HOME" | while IFS= read -r line; do ba_log "$line"; done; else ba_log "MAVEN_HOME directory does not exist"; fi
  fi
  ba_log "Maven is not available. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi

ba_log "Using JAVA_HOME at $JAVA_HOME"
ba_log "Using JAVA17_HOME at $JAVA17_HOME"
ba_log "Using Maven installation at $MAVEN_HOME"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$ANDROID_SDK_ROOT" ]; then
  if [ -d "/usr/local/lib/android/sdk" ]; then ANDROID_SDK_ROOT="/usr/local/lib/android/sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then ANDROID_SDK_ROOT="$HOME/Android/Sdk"; fi
fi
if [ -z "$ANDROID_SDK_ROOT" ] || [ ! -d "$ANDROID_SDK_ROOT" ]; then
  ba_log "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME to a valid installation." >&2
  exit 1
fi
export ANDROID_SDK_ROOT ANDROID_HOME="$ANDROID_SDK_ROOT"
ba_log "Using Android SDK at $ANDROID_SDK_ROOT"

APP_DIR="scripts/hellocodenameone"

[ -d "$APP_DIR" ] || { ba_log "Failed to create Codename One application project" >&2; exit 1; }
[ -f "$APP_DIR/build.sh" ] && chmod +x "$APP_DIR/build.sh"

# --- Build Android gradle project ---
ba_log "Building Android gradle project using Codename One port"
xvfb-run -a "$MAVEN_HOME/bin/mvn" -f "$APP_DIR/pom.xml" package \
  -DskipTests \
  -Dcodename1.platform=android \
  -Dcodename1.buildTarget=android-source \
  -Dopen=false \
  -U -e

GRADLE_PROJECT_DIR=$(find "$APP_DIR/android/target" -maxdepth 2 -type d -name "*-android-source" | head -n 1 || true)
if [ -z "$GRADLE_PROJECT_DIR" ]; then
  ba_log "Failed to locate generated Android project" >&2
  ba_log "Contents of $APP_DIR/android/target:" >&2
  ls -R "$APP_DIR/android/target" >&2 || ba_log "Unable to list $APP_DIR/android/target" >&2
  exit 1
fi

ba_log "Normalizing Android Gradle project in $GRADLE_PROJECT_DIR"

# --- Install Android instrumentation harness for coverage ---
ANDROID_TEST_SOURCE_DIR="$SCRIPT_DIR/device-runner-app/androidTest"
ANDROID_TEST_ROOT="$GRADLE_PROJECT_DIR/app/src/androidTest"
ANDROID_TEST_JAVA_DIR="$ANDROID_TEST_ROOT/java/${PACKAGE_PATH}"
if [ -d "$ANDROID_TEST_ROOT" ]; then
  ba_log "Removing template Android instrumentation tests from $ANDROID_TEST_ROOT"
  rm -rf "$ANDROID_TEST_ROOT"
fi
mkdir -p "$ANDROID_TEST_JAVA_DIR"
if [ ! -d "$ANDROID_TEST_SOURCE_DIR" ]; then
  ba_log "Android instrumentation test sources not found: $ANDROID_TEST_SOURCE_DIR" >&2
  exit 1
fi
cp "$ANDROID_TEST_SOURCE_DIR"/*.java "$ANDROID_TEST_JAVA_DIR"/
ba_log "Installed Android instrumentation tests in $ANDROID_TEST_JAVA_DIR"

# Ensure AndroidX flags in gradle.properties
# --- BEGIN: robust Gradle patch for AndroidX tests ---
GRADLE_PROPS="$GRADLE_PROJECT_DIR/gradle.properties"
grep -q '^android.useAndroidX=' "$GRADLE_PROPS" 2>/dev/null || echo 'android.useAndroidX=true' >> "$GRADLE_PROPS"
grep -q '^android.enableJetifier=' "$GRADLE_PROPS" 2>/dev/null || echo 'android.enableJetifier=true' >> "$GRADLE_PROPS"

APP_BUILD_GRADLE="$GRADLE_PROJECT_DIR/app/build.gradle"
ROOT_BUILD_GRADLE="$GRADLE_PROJECT_DIR/build.gradle"
PATCH_GRADLE_SOURCE_PATH="$SCRIPT_DIR/android/lib"
PATCH_GRADLE_MAIN_CLASS="PatchGradleFiles"

if [ ! -f "$PATCH_GRADLE_SOURCE_PATH/$PATCH_GRADLE_MAIN_CLASS.java" ]; then
  ba_log "Missing gradle patch helper: $PATCH_GRADLE_SOURCE_PATH/$PATCH_GRADLE_MAIN_CLASS.java" >&2
  exit 1
fi

PATCH_GRADLE_JAVA="${JAVA17_HOME}/bin/java"
if [ ! -x "$PATCH_GRADLE_JAVA" ]; then
  ba_log "JDK 17 java binary missing at $PATCH_GRADLE_JAVA" >&2
  exit 1
fi

"$PATCH_GRADLE_JAVA" "$PATCH_GRADLE_SOURCE_PATH/$PATCH_GRADLE_MAIN_CLASS.java" \
  --root "$ROOT_BUILD_GRADLE" \
  --app "$APP_BUILD_GRADLE" \
  --compile-sdk 33 \
  --target-sdk 33
# --- END: robust Gradle patch ---

echo "----- app/build.gradle tail -----"
tail -n 80 "$APP_BUILD_GRADLE" | sed 's/^/| /'
echo "---------------------------------"

ba_log "Invoking Gradle build in $GRADLE_PROJECT_DIR"
chmod +x "$GRADLE_PROJECT_DIR/gradlew"
ORIGINAL_JAVA_HOME="$JAVA_HOME"
export JAVA_HOME="$JAVA17_HOME"
(
  cd "$GRADLE_PROJECT_DIR"
  if command -v sdkmanager >/dev/null 2>&1; then
    yes | sdkmanager --licenses >/dev/null 2>&1 || true
  elif [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
  fi
  ./gradlew --no-daemon assembleDebug
)
export JAVA_HOME="$ORIGINAL_JAVA_HOME"

APK_PATH=$(find "$GRADLE_PROJECT_DIR" -path "*/outputs/apk/debug/*.apk" | head -n 1 || true)
[ -n "$APK_PATH" ] || { ba_log "Gradle build completed but no APK was found" >&2; exit 1; }
ba_log "Successfully built Android APK at $APK_PATH"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "gradle_project_dir=$GRADLE_PROJECT_DIR"
    echo "apk_path=$APK_PATH"
  } >> "$GITHUB_OUTPUT"
  ba_log "Published GitHub Actions outputs for downstream steps"
fi
