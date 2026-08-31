#!/usr/bin/env bash
# Build a sample "Hello Codename One" native macOS application from the
# locally-built Codename One macOS port. This is a real AppKit app: its
# Xcode project targets the macOS SDK, links AppKit and Metal, and carries
# no UIKit at all. The legacy Mac Catalyst build is
# scripts/build-mac-catalyst-app.sh. Mirrors scripts/build-ios-app.sh.
set -euo pipefail

bma_log() { echo "[build-macos-app] $1"; }

# Pin Xcode 26 for CI validation: building against macOS SDK 26+ needs the
# Metal Toolchain component.
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

# shellcheck source=scripts/lib/inject-maps-key.sh
. "$SCRIPT_DIR/lib/inject-maps-key.sh"
inject_google_maps_key "$REPO_ROOT"

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

# No Ruby / xcodeproj gem preflight here, deliberately. The Catalyst path needs
# that gem because it injects its build settings into an already-generated iOS
# project after the fact; the AppKit builder writes a macOS project with every
# setting already in it, so there is nothing to inject and one less thing that
# can break a build host.

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

# Inject the macos.* build hints into the sample's
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

# Use placeholder team / distribution defaults so unsigned local + CI builds
# succeed. Real-app submissions override these via their own settings.
ensure_setting "codename1.arg.macos.teamId" \
    "${MACOS_TEAM_ID:-ABCDEF1234}"
ensure_setting "codename1.arg.macos.distribution" \
    "${MACOS_DISTRIBUTION:-both}"
ensure_setting "codename1.arg.macos.appCategory" \
    "${MACOS_APP_CATEGORY:-public.app-category.developer-tools}"
# Pin the window size deterministically so the screenshot
# CI's strict-pixel comparison stays stable across runs. Off by
# default for real apps -- only the screenshot sample sets this.
ensure_setting "codename1.arg.macos.fixedWindowSize" \
    "${MACOS_FIXED_WINDOW_SIZE:-1024x685}"

bma_log "macos.* hints in codenameone_settings.properties:"
grep -n 'codename1\.arg\.macos' "$CN1_SETTINGS_FILE" || true

xcodebuild -version

bma_log "Building native macOS Xcode project using the Codename One macOS port"
cd "$REPO_ROOT/$APP_DIR"
VM_START=$(date +%s)

ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts}"
mkdir -p "$ARTIFACTS_DIR"

export CN1_BUILD_STATS_FILE="$ARTIFACTS_DIR/macos-builder-stats.txt"

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
  MVN_LOG="$ARTIFACTS_DIR/cn1-macos-build.log"
  MVN_CMD=(
    ./mvnw package
    -DskipTests
    -Dcodename1.platform=mac
    -Dcodename1.buildTarget=mac-source
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
      rg -n "(macOS builder log:|Caused by:|BuildException|Cannot run program|UnsupportedClassVersionError|error:|\\[ERROR\\])" "$MVN_LOG" | tail -n 200 || true
    else
      grep -nE "(macOS builder log:|Caused by:|BuildException|Cannot run program|UnsupportedClassVersionError|error:|\\[ERROR\\])" "$MVN_LOG" | tail -n 200 || true
    fi
    exit $RC
  fi
)
VM_END=$(date +%s)
VM_TIME=$((VM_END - VM_START))
cd "$REPO_ROOT"

echo "$VM_TIME" > "$ARTIFACTS_DIR/vm_time.txt"
bma_log "VM translation time: ${VM_TIME}s (saved to $ARTIFACTS_DIR/vm_time.txt)"

if [ -f "$ARTIFACTS_DIR/macos-builder-stats.txt" ]; then
    TOTAL_BUILDER_TIME_MS=$(grep "Total Time" "$ARTIFACTS_DIR/macos-builder-stats.txt" | awk -F ':' '{print $2}' | tr -d ' ms')
    if [ -n "$TOTAL_BUILDER_TIME_MS" ]; then
        TOTAL_BUILDER_TIME_SEC=$((TOTAL_BUILDER_TIME_MS / 1000))
        MAVEN_OVERHEAD=$((VM_TIME - TOTAL_BUILDER_TIME_SEC))
        echo "Maven Overhead : ${MAVEN_OVERHEAD}000 ms" >> "$ARTIFACTS_DIR/macos-builder-stats.txt"
    fi
fi

# The AppKit port has its own maven module, unlike Mac Catalyst, which builds
# from ios/ because it is a variant of the iOS build.
MAC_TARGET_DIR="$APP_DIR/mac/target"
if [ ! -d "$MAC_TARGET_DIR" ]; then
  bma_log "macOS target directory not found at $MAC_TARGET_DIR" >&2
  exit 1
fi

# CN1BuildMojo routes the generated project to <finalName>-mac-source/ when
# the mac-source target is used (see getGeneratedMacProjectSourceDirectory).
PROJECT_DIR=""
for candidate in "$MAC_TARGET_DIR"/*-mac-source; do
  if [ -d "$candidate" ]; then
    PROJECT_DIR="$candidate"
    break
  fi
done
if [ -z "$PROJECT_DIR" ]; then
  bma_log "Failed to locate generated Mac native project under $MAC_TARGET_DIR (expected *-mac-source/)" >&2
  find "$MAC_TARGET_DIR" -maxdepth 2 -type d -print >&2 || true
  exit 1
fi
bma_log "Found generated Mac native project at $PROJECT_DIR"

# Surface the macOS artefacts (entitlements + ExportOptions plists +
# Mac.appiconset) so they're visible in the CI upload. Keep them in
# ARTIFACTS_DIR/macos-project/ to mirror the iOS pipeline's
# bytecode-translator-sources staging.
MACOS_ARTIFACTS_DIR="$ARTIFACTS_DIR/macos-project"
rm -rf "$MACOS_ARTIFACTS_DIR"
mkdir -p "$MACOS_ARTIFACTS_DIR"
for f in "$PROJECT_DIR"/ExportOptions-*-Mac.plist \
         "$PROJECT_DIR"/cn1-Bridging-Header.h \
         "$PROJECT_DIR/$APP_MAIN_NAME-src/$APP_MAIN_NAME.entitlements" \
         "$PROJECT_DIR/$APP_MAIN_NAME-src/$APP_MAIN_NAME-AppStore.entitlements" \
         "$PROJECT_DIR/$APP_MAIN_NAME-src/$APP_MAIN_NAME-DeveloperID.entitlements"; do
  [ -f "$f" ] && cp -p "$f" "$MACOS_ARTIFACTS_DIR/" || true
done
if [ -d "$PROJECT_DIR/$APP_MAIN_NAME-src/Images.xcassets/Mac.appiconset" ]; then
  cp -R "$PROJECT_DIR/$APP_MAIN_NAME-src/Images.xcassets/Mac.appiconset" \
        "$MACOS_ARTIFACTS_DIR/Mac.appiconset"
fi
bma_log "Staged macOS artefacts at $MACOS_ARTIFACTS_DIR"

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
  xcodebuild -workspace "$WORKSPACE" -list > "$ARTIFACTS_DIR/xcodebuild-list-macos.txt" 2>&1 || true
else
  xcodebuild -project "$WORKSPACE" -list > "$ARTIFACTS_DIR/xcodebuild-list-macos.txt" 2>&1 || true
fi

exit 0
