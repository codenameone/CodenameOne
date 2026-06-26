#!/usr/bin/env bash
# Build a sample "Hello Codename One" Mac native application using the
# locally-built Codename One iOS port. The Mac slice is produced by the
# same IPhoneBuilder pipeline as iOS, via the macNative.enabled=true build
# hint (Mac Catalyst under the hood -- implementation detail not surfaced
# in user-facing names). Mirrors scripts/build-ios-app.sh.
set -euo pipefail

bma_log() { echo "[build-mac-native-app] $1"; }

# Pin Xcode 26 for CI validation (Mac Catalyst archive on macOS SDK 26+
# requires the Metal Toolchain component; older Xcode doesn't include the
# Catalyst build settings the macNative path injects).
if [ -z "${XCODE_APP:-}" ]; then
  XCODE_APP="$(ls -d /Applications/Xcode_26*.app 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [ ! -x "$XCODE_APP/Contents/Developer/usr/bin/xcodebuild" ]; then
  bma_log "Xcode 26 not found. Set XCODE_APP to an installed Xcode 26 app bundle path." >&2
  exit 1
fi
export DEVELOPER_DIR="$XCODE_APP/Contents/Developer"
export XCODEBUILD="$DEVELOPER_DIR/usr/bin/xcodebuild"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"
bma_log "Using DEVELOPER_DIR=$DEVELOPER_DIR"
bma_log "Using XCODEBUILD=$XCODEBUILD"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"
bma_log "Loading workspace environment from $ENV_FILE"
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  bma_log "Loaded environment: JAVA_HOME=${JAVA_HOME:-<unset>} JAVA17_HOME=${JAVA17_HOME:-<unset>} MAVEN_HOME=${MAVEN_HOME:-<unset>}"
else
  bma_log "Workspace tools not found. Run scripts/setup-workspace.sh before this script." >&2
  exit 1
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  bma_log "JAVA_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  bma_log "JAVA17_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${MAVEN_HOME:-}" ] || [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  bma_log "Maven is not available. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if ! command -v xcodebuild >/dev/null 2>&1; then
  bma_log "xcodebuild not found. Install Xcode command-line tools." >&2
  exit 1
fi

# The macNative path uses Ruby's xcodeproj gem unconditionally to inject
# the Mac Catalyst build settings (SUPPORTS_MACCATALYST, deployment
# targets, signing) post-generation. Fail early if it's missing -- the
# Maven build would otherwise get most of the way through ParparVM before
# the hook script crashes.
if ! command -v ruby >/dev/null 2>&1; then
  bma_log "ruby not found. Install Ruby and the xcodeproj gem." >&2
  exit 1
fi
if ! ruby -e "require 'xcodeproj'" >/dev/null 2>&1; then
  bma_log "The xcodeproj Ruby gem is required for macNative builds. Install with: gem install xcodeproj" >&2
  exit 1
fi

export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
BASE_PATH="$PATH"

bma_log "Using JAVA_HOME at $JAVA_HOME"
bma_log "Using JAVA17_HOME at $JAVA17_HOME"
bma_log "Using Maven installation at $MAVEN_HOME"
bma_log "Java version for baseline toolchain:"
"$JAVA_HOME/bin/java" -version
bma_log "Using JAVAC from JAVA17_HOME for demo compilation:"
"$JAVA17_HOME/bin/javac" -version

APP_DIR="${CN1_APP_DIR:-scripts/hellocodenameone}"
CN1_SETTINGS_FILE="$REPO_ROOT/$APP_DIR/common/codenameone_settings.properties"
if [ -f "$CN1_SETTINGS_FILE" ]; then
  MAIN_NAME_FROM_SETTINGS="$(awk -F= '/^codename1.mainName=/{print $2; exit}' "$CN1_SETTINGS_FILE" | tr -d '\r')"
fi
APP_MAIN_NAME="${CN1_APP_MAIN_NAME:-${MAIN_NAME_FROM_SETTINGS:-HelloCodenameOne}}"
bma_log "Using APP_DIR=$APP_DIR APP_MAIN_NAME=$APP_MAIN_NAME"

# Inject the macNative.* build hints into the sample's
# codenameone_settings.properties. -D arguments on the Maven CLI don't flow
# into the Codename One Maven plugin's BuildRequest (the plugin reads
# build args from the settings file on disk); follow the same pattern the
# iOS Metal CI uses for codename1.arg.ios.metal=true.
#
# The original file is restored on exit so subsequent iOS-only invocations
# of build-ios-app.sh against the same sample aren't poisoned.
SETTINGS_BACKUP="$(mktemp "${TMPDIR}/cn1-settings-backup.XXXXXX")"
cp -p "$CN1_SETTINGS_FILE" "$SETTINGS_BACKUP"
restore_settings() {
  if [ -f "$SETTINGS_BACKUP" ]; then
    cp -p "$SETTINGS_BACKUP" "$CN1_SETTINGS_FILE"
    rm -f "$SETTINGS_BACKUP"
    bma_log "Restored original codenameone_settings.properties"
  fi
}
trap restore_settings EXIT

ensure_setting() {
  local key="$1" value="$2"
  if grep -q "^${key}=" "$CN1_SETTINGS_FILE"; then
    if sed --version >/dev/null 2>&1; then
      sed -i -e "s|^${key}=.*|${key}=${value}|" "$CN1_SETTINGS_FILE"
    else
      sed -i '' -e "s|^${key}=.*|${key}=${value}|" "$CN1_SETTINGS_FILE"
    fi
  else
    printf '%s=%s\n' "$key" "$value" >> "$CN1_SETTINGS_FILE"
  fi
}

ensure_setting "codename1.arg.macNative.enabled" "true"
# Use placeholder team / distribution defaults so unsigned local + CI builds
# succeed. Real-app submissions override these via their own settings.
ensure_setting "codename1.arg.macNative.teamId" \
    "${MAC_NATIVE_TEAM_ID:-ABCDEF1234}"
ensure_setting "codename1.arg.macNative.distribution" \
    "${MAC_NATIVE_DISTRIBUTION:-both}"
ensure_setting "codename1.arg.macNative.appCategory" \
    "${MAC_NATIVE_APP_CATEGORY:-public.app-category.developer-tools}"
# Pin the Catalyst window size deterministically so the screenshot
# CI's strict-pixel comparison stays stable across runs. Off by
# default for real apps -- only the screenshot sample sets this.
# Set MAC_NATIVE_NO_FIXED_WINDOW=1 to leave the window freely
# resizable (the simulator relay wants this).
if [ -z "${MAC_NATIVE_NO_FIXED_WINDOW:-}" ]; then
  ensure_setting "codename1.arg.macNative.fixedWindowSize" \
      "${MAC_NATIVE_FIXED_WINDOW_SIZE:-1024x685}"
fi

bma_log "macNative.* hints in codenameone_settings.properties:"
grep -n 'codename1\.arg\.macNative' "$CN1_SETTINGS_FILE" || true

xcodebuild -version

bma_log "Building Mac native Xcode project using Codename One iOS port (macNative.enabled=true)"
cd "$REPO_ROOT/$APP_DIR"
VM_START=$(date +%s)

ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts}"
mkdir -p "$ARTIFACTS_DIR"

export CN1_BUILD_STATS_FILE="$ARTIFACTS_DIR/iphone-builder-stats.txt"

EXTRA_IOS_ARGS=()
if [ -n "${IOS_DEPENDENCY_ARGS:-}" ]; then
  # shellcheck disable=SC2206
  EXTRA_IOS_ARGS=(${IOS_DEPENDENCY_ARGS})
  bma_log "Applying extra iOS build args: ${IOS_DEPENDENCY_ARGS}"
fi

bma_log "Running $APP_MAIN_NAME Maven build with JAVA_HOME=$JAVA17_HOME"
(
  export JAVA_HOME="$JAVA17_HOME"
  export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$BASE_PATH"
  MVN_LOG="$ARTIFACTS_DIR/cn1-mac-native-build.log"
  MVN_CMD=(
    ./mvnw package
    -DskipTests
    -Dcodename1.platform=ios
    -Dcodename1.buildTarget=ios-source
    -Dmaven.compiler.fork=true
    -Dmaven.compiler.executable="$JAVA17_HOME/bin/javac"
    -Dopen=false
  )
  if [ ${#EXTRA_IOS_ARGS[@]} -gt 0 ]; then
    MVN_CMD+=("${EXTRA_IOS_ARGS[@]}")
  fi
  MVN_CMD+=(-U -e -X)
  set +e
  "${MVN_CMD[@]}" > "$MVN_LOG" 2>&1
  RC=$?
  set -e
  if [ $RC -ne 0 ]; then
    bma_log "Maven build failed (exit=$RC). Log: $MVN_LOG"
    bma_log "Key failure lines:"
    if command -v rg >/dev/null 2>&1; then
      rg -n "(iOS builder log:|Caused by:|BuildException|Cannot run program|UnsupportedClassVersionError|error:|\\[ERROR\\])" "$MVN_LOG" | tail -n 200 || true
    else
      grep -nE "(iOS builder log:|Caused by:|BuildException|Cannot run program|UnsupportedClassVersionError|error:|\\[ERROR\\])" "$MVN_LOG" | tail -n 200 || true
    fi
    exit $RC
  fi
)
VM_END=$(date +%s)
VM_TIME=$((VM_END - VM_START))
cd "$REPO_ROOT"

echo "$VM_TIME" > "$ARTIFACTS_DIR/vm_time.txt"
bma_log "VM translation time: ${VM_TIME}s (saved to $ARTIFACTS_DIR/vm_time.txt)"

if [ -f "$ARTIFACTS_DIR/iphone-builder-stats.txt" ]; then
    TOTAL_BUILDER_TIME_MS=$(grep "Total Time" "$ARTIFACTS_DIR/iphone-builder-stats.txt" | awk -F ':' '{print $2}' | tr -d ' ms')
    if [ -n "$TOTAL_BUILDER_TIME_MS" ]; then
        TOTAL_BUILDER_TIME_SEC=$((TOTAL_BUILDER_TIME_MS / 1000))
        MAVEN_OVERHEAD=$((VM_TIME - TOTAL_BUILDER_TIME_SEC))
        echo "Maven Overhead : ${MAVEN_OVERHEAD}000 ms" >> "$ARTIFACTS_DIR/iphone-builder-stats.txt"
    fi
fi

IOS_TARGET_DIR="$APP_DIR/ios/target"
if [ ! -d "$IOS_TARGET_DIR" ]; then
  bma_log "iOS target directory not found at $IOS_TARGET_DIR" >&2
  exit 1
fi

# CN1BuildMojo routes the generated project to <finalName>-mac-source/ when
# the macNative.enabled hint is on (see getGeneratedMacProjectSourceDirectory).
PROJECT_DIR=""
for candidate in "$IOS_TARGET_DIR"/*-mac-source; do
  if [ -d "$candidate" ]; then
    PROJECT_DIR="$candidate"
    break
  fi
done
if [ -z "$PROJECT_DIR" ]; then
  bma_log "Failed to locate generated Mac native project under $IOS_TARGET_DIR (expected *-mac-source/)" >&2
  find "$IOS_TARGET_DIR" -maxdepth 2 -type d -print >&2 || true
  exit 1
fi
bma_log "Found generated Mac native project at $PROJECT_DIR"

# Surface the macNative artefacts (entitlements + ExportOptions plists +
# Mac.appiconset) so they're visible in the CI upload. Keep them in
# ARTIFACTS_DIR/mac-native-project/ to mirror the iOS pipeline's
# bytecode-translator-sources staging.
MAC_NATIVE_ARTIFACTS_DIR="$ARTIFACTS_DIR/mac-native-project"
rm -rf "$MAC_NATIVE_ARTIFACTS_DIR"
mkdir -p "$MAC_NATIVE_ARTIFACTS_DIR"
for f in "$PROJECT_DIR"/ExportOptions-*-Mac.plist \
         "$PROJECT_DIR"/cn1-Bridging-Header.h \
         "$PROJECT_DIR/$APP_MAIN_NAME-src/$APP_MAIN_NAME.entitlements" \
         "$PROJECT_DIR/$APP_MAIN_NAME-src/$APP_MAIN_NAME-AppStore.entitlements" \
         "$PROJECT_DIR/$APP_MAIN_NAME-src/$APP_MAIN_NAME-DeveloperID.entitlements"; do
  [ -f "$f" ] && cp -p "$f" "$MAC_NATIVE_ARTIFACTS_DIR/" || true
done
if [ -d "$PROJECT_DIR/$APP_MAIN_NAME-src/Images.xcassets/Mac.appiconset" ]; then
  cp -R "$PROJECT_DIR/$APP_MAIN_NAME-src/Images.xcassets/Mac.appiconset" \
        "$MAC_NATIVE_ARTIFACTS_DIR/Mac.appiconset"
fi
bma_log "Staged Mac native artefacts at $MAC_NATIVE_ARTIFACTS_DIR"

if [ -d "$PROJECT_DIR/${APP_MAIN_NAME}.xcodeproj" ]; then
  bma_log "Ensuring shared Xcode scheme exists"
  "$REPO_ROOT/scripts/ios/create-shared-scheme.py" "$PROJECT_DIR" "$APP_MAIN_NAME"
fi

# Locate workspace or project entrypoint
WORKSPACE=""
for candidate in "$PROJECT_DIR"/*.xcworkspace; do
  if [ -d "$candidate" ]; then
    WORKSPACE="$candidate"
    break
  fi
done
if [ -z "$WORKSPACE" ]; then
  for candidate in "$PROJECT_DIR"/*.xcodeproj; do
    if [ -d "$candidate" ]; then
      WORKSPACE="$candidate"
      break
    fi
  done
fi
if [ -z "$WORKSPACE" ]; then
  bma_log "Failed to locate xcworkspace or xcodeproj in $PROJECT_DIR" >&2
  ls "$PROJECT_DIR" >&2 || true
  exit 1
fi
bma_log "Found Xcode entrypoint: $WORKSPACE"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "workspace=$WORKSPACE"
    echo "scheme=$APP_MAIN_NAME"
    echo "project_dir=$PROJECT_DIR"
  } >> "$GITHUB_OUTPUT"
fi

bma_log "Emitted outputs -> workspace=$WORKSPACE, scheme=$APP_MAIN_NAME"

if [[ "$WORKSPACE" == *.xcworkspace ]]; then
  xcodebuild -workspace "$WORKSPACE" -list > "$ARTIFACTS_DIR/xcodebuild-list-mac.txt" 2>&1 || true
else
  xcodebuild -project "$WORKSPACE" -list > "$ARTIFACTS_DIR/xcodebuild-list-mac.txt" 2>&1 || true
fi

exit 0
