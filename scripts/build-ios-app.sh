#!/usr/bin/env bash
# Build a sample "Hello Codename One" iOS application using the locally-built Codename One iOS port
set -euo pipefail

bia_log() { echo "[build-ios-app] $1"; }

# Pin Xcode 26 for CI validation.
if [ -z "${XCODE_APP:-}" ]; then
  XCODE_APP="$(ls -d /Applications/Xcode_26*.app 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [ ! -x "$XCODE_APP/Contents/Developer/usr/bin/xcodebuild" ]; then
  bia_log "Xcode 26 not found. Set XCODE_APP to an installed Xcode 26 app bundle path." >&2
  exit 1
fi
export DEVELOPER_DIR="$XCODE_APP/Contents/Developer"
export XCODEBUILD="$DEVELOPER_DIR/usr/bin/xcodebuild"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"
bia_log "Using DEVELOPER_DIR=$DEVELOPER_DIR"
bia_log "Using XCODEBUILD=$XCODEBUILD"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"

ENV_FILE="$ENV_DIR/env.sh"
bia_log "Loading workspace environment from $ENV_FILE"
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  bia_log "Loaded environment: JAVA_HOME=${JAVA_HOME:-<unset>} JAVA17_HOME=${JAVA17_HOME:-<unset>} MAVEN_HOME=${MAVEN_HOME:-<unset>}"
else
  bia_log "Workspace tools not found. Run scripts/setup-workspace.sh before this script." >&2
  exit 1
fi

# --- Tool validations ---
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  bia_log "JAVA_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  bia_log "JAVA17_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${MAVEN_HOME:-}" ] || [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  bia_log "Maven is not available. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if ! command -v xcodebuild >/dev/null 2>&1; then
  bia_log "xcodebuild not found. Install Xcode command-line tools." >&2
  exit 1
fi
if ! command -v pod >/dev/null 2>&1; then
  bia_log "CocoaPods (pod) command not found. Install cocoapods before running this script." >&2
  exit 1
fi

export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
BASE_PATH="$PATH"

bia_log "Using JAVA_HOME at $JAVA_HOME"
bia_log "Using JAVA17_HOME at $JAVA17_HOME"
bia_log "Using Maven installation at $MAVEN_HOME"
bia_log "Using CocoaPods version $(pod --version 2>/dev/null || echo '<unknown>')"
bia_log "Java version for baseline toolchain:"
"$JAVA_HOME/bin/java" -version
bia_log "Using JAVAC from JAVA17_HOME for demo compilation:"
"$JAVA17_HOME/bin/javac" -version
IOS_UISCENE="${IOS_UISCENE:-false}"
bia_log "Building sample app with ios.uiscene=${IOS_UISCENE}"

APP_DIR="scripts/hellocodenameone"

xcodebuild -version

bia_log "Building iOS Xcode project using Codename One port"
cd $APP_DIR
VM_START=$(date +%s)

ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts}"
mkdir -p "$ARTIFACTS_DIR"

export CN1_BUILD_STATS_FILE="$ARTIFACTS_DIR/iphone-builder-stats.txt"

bia_log "Running HelloCodenameOne Maven build with JAVA_HOME=$JAVA17_HOME"
(
  export JAVA_HOME="$JAVA17_HOME"
  export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$BASE_PATH"
  MVN_IOS_LOG="$ARTIFACTS_DIR/hellocn1-ios-build.log"
  set +e
  ./mvnw package \
    -DskipTests \
    -Dcodename1.platform=ios \
    -Dcodename1.buildTarget=ios-source \
    -Dmaven.compiler.fork=true \
    -Dmaven.compiler.executable="$JAVA17_HOME/bin/javac" \
    -Dcodename1.arg.ios.uiscene="${IOS_UISCENE}" \
    -Dopen=false \
    -U -e -X > "$MVN_IOS_LOG" 2>&1
  RC=$?
  set -e
  if [ $RC -ne 0 ]; then
    bia_log "Maven iOS build failed (exit=$RC). Log: $MVN_IOS_LOG"
    bia_log "Key failure lines:"
    if command -v rg >/dev/null 2>&1; then
      rg -n "(iOS builder log:|Caused by:|BuildException|Cannot run program|UnsupportedClassVersionError|error:|\\[ERROR\\])" "$MVN_IOS_LOG" | tail -n 200 || true
    else
      grep -nE "(iOS builder log:|Caused by:|BuildException|Cannot run program|UnsupportedClassVersionError|error:|\\[ERROR\\])" "$MVN_IOS_LOG" | tail -n 200 || true
    fi
    exit $RC
  fi
)
VM_END=$(date +%s)
VM_TIME=$((VM_END - VM_START))
cd ../..

echo "$VM_TIME" > "$ARTIFACTS_DIR/vm_time.txt"
bia_log "VM translation time: ${VM_TIME}s (saved to $ARTIFACTS_DIR/vm_time.txt)"

# Calculate Maven overhead if stats file exists
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
  bia_log "iOS target directory not found at $IOS_TARGET_DIR" >&2
  exit 1
fi

PROJECT_DIR=""
for candidate in "$IOS_TARGET_DIR"/*-ios-source; do
  if [ -d "$candidate" ]; then
    PROJECT_DIR="$candidate"
    break
  fi
done
if [ -z "$PROJECT_DIR" ]; then
  bia_log "Failed to locate generated iOS project under $IOS_TARGET_DIR" >&2
  find "$IOS_TARGET_DIR" -type d -print >&2 || true
  exit 1
fi
bia_log "Found generated iOS project at $PROJECT_DIR"

# CocoaPods (project contains a Podfile but usually empty — fine)
if [ -f "$PROJECT_DIR/Podfile" ]; then
  bia_log "Installing CocoaPods dependencies"
  POD_START=$(date +%s)
  (
    cd "$PROJECT_DIR"
    if ! pod install --repo-update; then
      bia_log "pod install --repo-update failed; retrying without repo update"
      pod install
    fi
  )
  POD_END=$(date +%s)
  POD_TIME=$((POD_END - POD_START))
  echo "CocoaPods Install (Script) : ${POD_TIME}000 ms" >> "$ARTIFACTS_DIR/iphone-builder-stats.txt"
else
  bia_log "Podfile not found in generated project; skipping pod install"
fi

# Locate workspace for the next step
WORKSPACE=""
for candidate in "$PROJECT_DIR"/*.xcworkspace; do
  if [ -d "$candidate" ]; then
    WORKSPACE="$candidate"
    break
  fi
done
if [ -z "$WORKSPACE" ]; then
  bia_log "Failed to locate xcworkspace in $PROJECT_DIR" >&2
  ls "$PROJECT_DIR" >&2 || true
  exit 1
fi
bia_log "Found xcworkspace: $WORKSPACE"


# Make these visible to the next GH Actions step
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "workspace=$WORKSPACE"
    echo "scheme=HelloCodenameOne"
  } >> "$GITHUB_OUTPUT"
fi

bia_log "Emitted outputs -> workspace=$WORKSPACE, scheme=HelloCodenameOne"

# (Optional) dump xcodebuild -list for debugging
ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts}"
mkdir -p "$ARTIFACTS_DIR"
xcodebuild -workspace "$WORKSPACE" -list > "$ARTIFACTS_DIR/xcodebuild-list.txt" 2>&1 || true

exit 0
