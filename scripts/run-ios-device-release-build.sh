#!/usr/bin/env bash
# Build a generated Codename One iOS project for a Release iPhone device target
# without code signing. This catches arm64/iphoneos linker issues that simulator
# UI tests cannot see.
set -euo pipefail

rd_log() { echo "[run-ios-device-release-build] $1"; }

if [ $# -lt 1 ]; then
  rd_log "Usage: $0 <workspace_or_project_path> [scheme]" >&2
  exit 2
fi

WORKSPACE_PATH="$1"
REQUESTED_SCHEME="${2:-}"

if [ ! -d "$WORKSPACE_PATH" ]; then
  rd_log "Xcode workspace/project not found at $WORKSPACE_PATH" >&2
  exit 3
fi

XCODE_CONTAINER_FLAG="-workspace"
if [[ "$WORKSPACE_PATH" == *.xcodeproj ]]; then
  XCODE_CONTAINER_FLAG="-project"
fi

PROJECT_DIR="$(cd "$(dirname "$WORKSPACE_PATH")" && pwd)"
WORKSPACE_BASENAME="$(basename "$WORKSPACE_PATH")"
WORKSPACE_PATH="$PROJECT_DIR/$WORKSPACE_BASENAME"

if [[ "$WORKSPACE_PATH" == *.xcworkspace ]] && [ ! -f "$PROJECT_DIR/Podfile" ]; then
  SIBLING_PROJECT="$PROJECT_DIR/$(basename "$WORKSPACE_PATH" .xcworkspace).xcodeproj"
  if [ -d "$SIBLING_PROJECT" ]; then
    rd_log "No Podfile found; using sibling Xcode project for device build: $SIBLING_PROJECT"
    WORKSPACE_PATH="$SIBLING_PROJECT"
    XCODE_CONTAINER_FLAG="-project"
  fi
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
mkdir -p "$ARTIFACTS_DIR"

# Match the other iOS CI scripts: prefer Xcode 26, while still allowing callers
# to pin a specific installation via XCODE_APP.
if [ -z "${XCODE_APP:-}" ]; then
  XCODE_APP="$(ls -d /Applications/Xcode_26*.app 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [ ! -x "$XCODE_APP/Contents/Developer/usr/bin/xcodebuild" ]; then
  rd_log "Xcode 26 not found. Set XCODE_APP to an installed Xcode 26 app bundle path." >&2
  exit 3
fi
export DEVELOPER_DIR="$XCODE_APP/Contents/Developer"
export XCODEBUILD="$DEVELOPER_DIR/usr/bin/xcodebuild"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"
rd_log "Using DEVELOPER_DIR=$DEVELOPER_DIR"
rd_log "Using XCODEBUILD=$XCODEBUILD"

if ! command -v xcodebuild >/dev/null 2>&1; then
  rd_log "xcodebuild not found" >&2
  exit 3
fi

DOWNLOAD_PLATFORMS="${XCODE_DOWNLOAD_PLATFORMS:-}"
if [ -z "$DOWNLOAD_PLATFORMS" ] && [ "${GITHUB_ACTIONS:-false}" = "true" ]; then
  DOWNLOAD_PLATFORMS="true"
fi
DOWNLOAD_PLATFORMS="${DOWNLOAD_PLATFORMS:-false}"
rd_log "XCODE_DOWNLOAD_PLATFORMS=${DOWNLOAD_PLATFORMS}"

SDK_LIST="$(xcodebuild -showsdks 2>/dev/null || true)"
if ! printf '%s\n' "$SDK_LIST" | grep -q "iphoneos"; then
  if [ "$DOWNLOAD_PLATFORMS" = "true" ]; then
    rd_log "Attempting to download missing iOS platform via xcodebuild -downloadPlatform iOS"
    xcodebuild -downloadPlatform iOS || true
    SDK_LIST="$(xcodebuild -showsdks 2>/dev/null || true)"
  else
    rd_log "Missing iphoneos SDK detected. Set XCODE_DOWNLOAD_PLATFORMS=true to attempt auto-download."
  fi
fi

if ! printf '%s\n' "$SDK_LIST" | grep -q "iphoneos"; then
  rd_log "No iPhoneOS SDK detected in Xcode. Install the iOS platform in Xcode > Settings > Components." >&2
  printf '%s\n' "$SDK_LIST" > "$ARTIFACTS_DIR/xcodebuild-showsdks.log"
  exit 3
fi

if [ -z "$REQUESTED_SCHEME" ]; then
  if [[ "$WORKSPACE_PATH" == *.xcworkspace ]]; then
    REQUESTED_SCHEME="$(basename "$WORKSPACE_PATH" .xcworkspace)"
  elif [[ "$WORKSPACE_PATH" == *.xcodeproj ]]; then
    REQUESTED_SCHEME="$(basename "$WORKSPACE_PATH" .xcodeproj)"
  else
    REQUESTED_SCHEME="$(basename "$WORKSPACE_PATH")"
  fi
fi
SCHEME="$REQUESTED_SCHEME"
rd_log "Using scheme $SCHEME"

DERIVED_DATA_DIR="${CN1_IOS_DEVICE_DERIVED_DATA:-${TMPDIR}/cn1-ios-device-release-derived}"
mkdir -p "$DERIVED_DATA_DIR/ModuleCache.noindex" "$DERIVED_DATA_DIR/PrecompiledHeaders"
BUILD_LOG="$ARTIFACTS_DIR/xcodebuild-device-release.log"

rd_log "Building Release iphoneos app without code signing"
COMPILE_START=$(date +%s)
XCODE_BUILD_CMD=(
  xcodebuild
  "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH"
  -scheme "$SCHEME"
  -sdk iphoneos
  -configuration Release
  -destination 'generic/platform=iOS'
  -destination-timeout 120
  -derivedDataPath "$DERIVED_DATA_DIR"
  "ARCHS=arm64"
  "ONLY_ACTIVE_ARCH=NO"
  "EXCLUDED_ARCHS=armv7 armv7s"
  "CODE_SIGN_IDENTITY="
  "CODE_SIGNING_REQUIRED=NO"
  "CODE_SIGNING_ALLOWED=NO"
  "DEVELOPMENT_TEAM="
  "PROVISIONING_PROFILE_SPECIFIER="
  "MODULE_CACHE_DIR=$DERIVED_DATA_DIR/ModuleCache.noindex"
  "CLANG_MODULE_CACHE_PATH=$DERIVED_DATA_DIR/ModuleCache.noindex"
  "SHARED_PRECOMPS_DIR=$DERIVED_DATA_DIR/PrecompiledHeaders"
  "COMPILER_INDEX_STORE_ENABLE=NO"
  build
)

if ! "${XCODE_BUILD_CMD[@]}" | tee "$BUILD_LOG"; then
  # tvOS-heavy runners hit the same destination-enumeration bug documented in
  # run-ios-ui-tests.sh: the multi-platform scheme lists ONLY Apple TV
  # destinations, so { generic:1, platform:iOS } cannot match even though the
  # iphoneos SDK builds fine. -sdk iphoneos alone does not go through
  # destination matching, so retry once without the -destination pair.
  if grep -q "Unable to find a destination matching the provided destination" "$BUILD_LOG"; then
    rd_log "Generic iOS destination did not enumerate (tvOS-heavy scheme); retrying with -sdk iphoneos only"
    RETRY_CMD=()
    skip_next=0
    for arg in "${XCODE_BUILD_CMD[@]}"; do
      if [ "$skip_next" = "1" ]; then skip_next=0; continue; fi
      case "$arg" in
        -destination|-destination-timeout) skip_next=1; continue ;;
      esac
      RETRY_CMD+=("$arg")
    done
    if "${RETRY_CMD[@]}" | tee "$BUILD_LOG"; then
      rd_log "Retry without destination succeeded"
    else
      rd_log "STAGE:IOS_DEVICE_RELEASE_BUILD_FAILED -> See $BUILD_LOG (after destination retry)"
      rd_log "Key failure lines:"
      grep -nE "(Undefined symbols|ld: symbol|framework not found|library not found|clang: error|ld:|error:)" "$BUILD_LOG" | tail -n 200 || true
      exit 10
    fi
  else
    rd_log "STAGE:IOS_DEVICE_RELEASE_BUILD_FAILED -> See $BUILD_LOG"
    rd_log "Key failure lines:"
    grep -nE "(Undefined symbols|ld: symbol|framework not found|library not found|clang: error|ld:|error:)" "$BUILD_LOG" | tail -n 200 || true
    exit 10
  fi
fi
COMPILE_END=$(date +%s)
COMPILATION_TIME=$((COMPILE_END - COMPILE_START))
rd_log "Release iphoneos build completed in ${COMPILATION_TIME}s"
