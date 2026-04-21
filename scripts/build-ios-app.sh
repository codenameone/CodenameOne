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
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
BASE_PATH="$PATH"

bia_log "Using JAVA_HOME at $JAVA_HOME"
bia_log "Using JAVA17_HOME at $JAVA17_HOME"
bia_log "Using Maven installation at $MAVEN_HOME"
if command -v pod >/dev/null 2>&1; then
  bia_log "Using CocoaPods version $(pod --version 2>/dev/null || echo '<unknown>')"
else
  bia_log "CocoaPods command not found; pod install will be skipped unless the generated project requires it"
fi
bia_log "Java version for baseline toolchain:"
"$JAVA_HOME/bin/java" -version
bia_log "Using JAVAC from JAVA17_HOME for demo compilation:"
"$JAVA17_HOME/bin/javac" -version
IOS_UISCENE="${IOS_UISCENE:-false}"
bia_log "Building sample app with ios.uiscene=${IOS_UISCENE}"
EXTRA_IOS_ARGS=()
if [ -n "${IOS_DEPENDENCY_ARGS:-}" ]; then
  # shellcheck disable=SC2206
  EXTRA_IOS_ARGS=(${IOS_DEPENDENCY_ARGS})
  bia_log "Applying extra iOS build args: ${IOS_DEPENDENCY_ARGS}"
fi

APP_DIR="scripts/hellocodenameone"

xcodebuild -version

bia_log "Building iOS Xcode project using Codename One port"
cd $APP_DIR
VM_START=$(date +%s)

ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts}"
mkdir -p "$ARTIFACTS_DIR"

export CN1_BUILD_STATS_FILE="$ARTIFACTS_DIR/iphone-builder-stats.txt"

copy_tree_contents() {
  local src="$1"
  local dest="$2"
  mkdir -p "$dest"
  if command -v rsync >/dev/null 2>&1; then
    rsync -a "$src"/ "$dest"/
  else
    cp -R "$src"/. "$dest"/
  fi
}

find_bytecode_translator_sources() {
  local root="$1"
  local best=""
  local best_score=0
  local dir score m_count c_count h_count

  [ -d "$root" ] || return 1

  while IFS= read -r dir; do
    [ -d "$dir" ] || continue

    score=0
    [ -f "$dir/cn1_globals.m" ] && score=$((score + 100))
    [ -f "$dir/xmlvm.h" ] && score=$((score + 100))

    m_count="$(find "$dir" -maxdepth 1 -type f -name '*.m' 2>/dev/null | wc -l | tr -d ' ')"
    c_count="$(find "$dir" -maxdepth 1 -type f -name '*.c' 2>/dev/null | wc -l | tr -d ' ')"
    h_count="$(find "$dir" -maxdepth 1 -type f -name '*.h' 2>/dev/null | wc -l | tr -d ' ')"

    score=$((score + m_count + c_count + h_count))

    if [ "$score" -gt "$best_score" ]; then
      best="$dir"
      best_score="$score"
    fi
  done < <(
    find "$root" -type d \
      ! -path '*/Pods/*' \
      ! -path '*/build/*' \
      ! -path '*/Build/*' \
      ! -path '*/DerivedData/*' \
      ! -path '*/xcuserdata/*' \
      2>/dev/null
  )

  [ -n "$best" ] || return 1
  printf '%s\n' "$best"
}

stage_bytecode_translator_sources() {
  local project_dir="$1"
  local artifacts_dir="$2"

  local bt_dir=""
  local out_dir="$artifacts_dir/bytecode-translator-sources"
  local zip_file="$artifacts_dir/bytecode-translator-sources.zip"
  local listing_file="$artifacts_dir/bytecode-translator-files.txt"

  bt_dir="$(find_bytecode_translator_sources "$project_dir" || true)"
  if [ -z "$bt_dir" ]; then
    bia_log "ByteCodeTranslator source directory not found under $project_dir"
    return 0
  fi

  bia_log "Detected ByteCodeTranslator sources at $bt_dir"

  rm -rf "$out_dir" "$zip_file"
  mkdir -p "$out_dir"

  copy_tree_contents "$bt_dir" "$out_dir"

  find "$out_dir" -maxdepth 2 -type f \( -name '*.m' -o -name '*.c' -o -name '*.h' \) \
    | sort > "$listing_file" || true

  (
    cd "$artifacts_dir"
    zip -qry "$(basename "$zip_file")" "$(basename "$out_dir")"
  )

  bia_log "Staged ByteCodeTranslator sources in $out_dir"
  bia_log "Created archive $zip_file"
}

bia_log "Running HelloCodenameOne Maven build with JAVA_HOME=$JAVA17_HOME"
(
  export JAVA_HOME="$JAVA17_HOME"
  export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$BASE_PATH"
  MVN_IOS_LOG="$ARTIFACTS_DIR/hellocn1-ios-build.log"
  MVN_CMD=(
    ./mvnw package
    -DskipTests
    -Dcodename1.platform=ios
    -Dcodename1.buildTarget=ios-source
    -Dmaven.compiler.fork=true
    -Dmaven.compiler.executable="$JAVA17_HOME/bin/javac"
    -Dcodename1.arg.ios.uiscene="${IOS_UISCENE}"
    -Dopen=false
  )
  if [ ${#EXTRA_IOS_ARGS[@]} -gt 0 ]; then
    MVN_CMD+=("${EXTRA_IOS_ARGS[@]}")
  fi
  MVN_CMD+=(-U -e -X)
  set +e
  "${MVN_CMD[@]}" > "$MVN_IOS_LOG" 2>&1
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

stage_bytecode_translator_sources "$PROJECT_DIR" "$ARTIFACTS_DIR"

if [ -f "$PROJECT_DIR/Podfile" ]; then
  if ! command -v pod >/dev/null 2>&1; then
    bia_log "Generated project requires CocoaPods but the pod command is not installed." >&2
    exit 1
  fi
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

WORKSPACE_XML='<?xml version="1.0" encoding="UTF-8"?>
<Workspace
   version = "1.0">
   <FileRef
      location = "group:HelloCodenameOne.xcodeproj">
   </FileRef>
</Workspace>'
if [ ! -d "$PROJECT_DIR/HelloCodenameOne.xcworkspace" ] && [ -d "$PROJECT_DIR/HelloCodenameOne.xcodeproj" ]; then
  bia_log "Creating fallback xcworkspace for generated Xcode project"
  mkdir -p "$PROJECT_DIR/HelloCodenameOne.xcworkspace"
  printf '%s\n' "$WORKSPACE_XML" > "$PROJECT_DIR/HelloCodenameOne.xcworkspace/contents.xcworkspacedata"
fi

if [ -d "$PROJECT_DIR/HelloCodenameOne.xcodeproj" ]; then
  bia_log "Ensuring shared Xcode scheme exists"
  "$REPO_ROOT/scripts/ios/create-shared-scheme.py" "$PROJECT_DIR" HelloCodenameOne
fi

# Locate workspace or project for the next step
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
  bia_log "Failed to locate xcworkspace or xcodeproj in $PROJECT_DIR" >&2
  ls "$PROJECT_DIR" >&2 || true
  exit 1
fi
bia_log "Found Xcode entrypoint: $WORKSPACE"


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
if [[ "$WORKSPACE" == *.xcworkspace ]]; then
  xcodebuild -workspace "$WORKSPACE" -list > "$ARTIFACTS_DIR/xcodebuild-list.txt" 2>&1 || true
else
  xcodebuild -project "$WORKSPACE" -list > "$ARTIFACTS_DIR/xcodebuild-list.txt" 2>&1 || true
fi

exit 0
