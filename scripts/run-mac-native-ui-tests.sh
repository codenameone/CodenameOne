#!/usr/bin/env bash
# Run Codename One UI screenshot tests on a Mac native (Mac Catalyst) build
# and compare against goldens. Mirrors scripts/run-ios-ui-tests.sh, but the
# Mac slice runs as a native process on the host so there is no simulator
# to boot / install / launch -- xcodebuild produces a .app under
# Build/Products/Debug-maccatalyst and we exec the binary directly.
set -euo pipefail

rm_log() { echo "[run-mac-native-ui-tests] $1"; }

ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

extract_base64_stats() {
  local out_file="$1"
  shift

  local log_file lines found=0
  : > "$out_file"
  for log_file in "$@"; do
    [ -f "$log_file" ] || continue
    lines="$(grep 'CN1SS:STAT:' "$log_file" 2>/dev/null | sed -E 's/^.*CN1SS:STAT://')" || true
    if [ -z "${lines:-}" ]; then
      continue
    fi
    found=1
    while IFS= read -r line; do
      [ -n "$line" ] || continue
      echo "$line" >> "$out_file"
    done <<< "$lines"
  done

  if [ "$found" -eq 1 ] && [ -f "$out_file" ]; then
    awk '!seen[$0]++' "$out_file" > "$out_file.tmp" && mv "$out_file.tmp" "$out_file"
  else
    rm -f "$out_file"
  fi
}

if [ $# -lt 1 ]; then
  rm_log "Usage: $0 <workspace_path> [app_bundle] [scheme]" >&2
  exit 2
fi

WORKSPACE_PATH="$1"
APP_BUNDLE_PATH="${2:-}"
REQUESTED_SCHEME="${3:-}"

if [ -n "$APP_BUNDLE_PATH" ] && [ ! -d "$APP_BUNDLE_PATH" ] && [ -z "$REQUESTED_SCHEME" ]; then
  REQUESTED_SCHEME="$APP_BUNDLE_PATH"
  APP_BUNDLE_PATH=""
fi

if [ ! -d "$WORKSPACE_PATH" ]; then
  rm_log "Xcode workspace/project not found at $WORKSPACE_PATH" >&2
  exit 3
fi

XCODE_CONTAINER_FLAG="-workspace"
if [[ "$WORKSPACE_PATH" == *.xcodeproj ]]; then
  XCODE_CONTAINER_FLAG="-project"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/common/java"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/cn1ss.sh"

if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/Cn1ssScreenshotServer.java" ]; then
  rm_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/Cn1ssScreenshotServer.java" >&2
  exit 3
fi
cn1ss_log() { rm_log "$1"; }

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

rm_log "Loading workspace environment from $ENV_FILE"
[ -f "$ENV_FILE" ] || { rm_log "Missing env file: $ENV_FILE"; exit 3; }
# shellcheck disable=SC1090
source "$ENV_FILE"

# Pin Xcode 26 for CI validation. The Mac Catalyst slice needs the macOS
# SDK 26+ headers / Metal Toolchain to compile.
if [ -z "${XCODE_APP:-}" ]; then
  XCODE_APP="$(ls -d /Applications/Xcode_26*.app 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [ ! -x "$XCODE_APP/Contents/Developer/usr/bin/xcodebuild" ]; then
  rm_log "Xcode 26 not found. Set XCODE_APP to an installed Xcode 26 app bundle path." >&2
  exit 3
fi
export DEVELOPER_DIR="$XCODE_APP/Contents/Developer"
export XCODEBUILD="$DEVELOPER_DIR/usr/bin/xcodebuild"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"
rm_log "Using DEVELOPER_DIR=$DEVELOPER_DIR"
rm_log "Using XCODEBUILD=$XCODEBUILD"

if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  rm_log "JAVA17_HOME not set correctly" >&2
  exit 3
fi
if ! command -v xcodebuild >/dev/null 2>&1; then
  rm_log "xcodebuild not found" >&2
  exit 3
fi

JAVA17_BIN="$JAVA17_HOME/bin/java"
cn1ss_setup "$JAVA17_BIN" "$CN1SS_HELPER_SOURCE_DIR"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
mkdir -p "$ARTIFACTS_DIR"
TEST_LOG="$ARTIFACTS_DIR/device-runner.log"
FALLBACK_LOG="$ARTIFACTS_DIR/device-runner-fallback.log"

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
rm_log "Using scheme $SCHEME"

# Golden-image directory defaults to scripts/mac-native/screenshots/.
# Override via SCREENSHOT_REF_DIR for parallel rendering backends or local
# experimentation. See scripts/mac-native/screenshots/README.md.
if [ -n "${SCREENSHOT_REF_DIR:-}" ]; then
  if [ ! -d "$SCREENSHOT_REF_DIR" ]; then
    rm_log "SCREENSHOT_REF_DIR override '$SCREENSHOT_REF_DIR' is not a directory" >&2
    exit 3
  fi
  SCREENSHOT_REF_DIR="$(cd "$SCREENSHOT_REF_DIR" && pwd)"
  rm_log "Using screenshot reference dir from SCREENSHOT_REF_DIR: $SCREENSHOT_REF_DIR"
else
  SCREENSHOT_REF_DIR="$REPO_ROOT/scripts/mac-native/screenshots"
fi
ensure_dir "$SCREENSHOT_REF_DIR"

SCREENSHOT_TMP_DIR="$(mktemp -d "${TMPDIR}/cn1-mac-native-tests-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1-mac-native-tests")"
SCREENSHOT_RAW_DIR="$SCREENSHOT_TMP_DIR/raw"
SCREENSHOT_PREVIEW_DIR="$SCREENSHOT_TMP_DIR/previews"
mkdir -p "$SCREENSHOT_RAW_DIR" "$SCREENSHOT_PREVIEW_DIR"

export CN1SS_OUTPUT_DIR="$SCREENSHOT_RAW_DIR"
export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"

# Tight golden gate (see run-ios-ui-tests.sh): the stock 0.30% default lets
# widget-level regressions pass silently on full-screen captures. Mac Catalyst
# renders deterministically; noisy screens carry per-test .tolerance files.
export CN1SS_MAX_MISMATCH_PERCENT="${CN1SS_MAX_MISMATCH_PERCENT:-0.05}"

# Start the host-side WebSocket screenshot server on the fixed standard port.
# The Mac Catalyst app runs on this host, so the device-runner defaults to
# ws://127.0.0.1:8765 with no per-launch URL injection (see
# Cn1ssDeviceRunnerHelper.CN1SS_WS_DEFAULT_PORT). Screenshots the app sends
# land directly in $WS_RAW_DIR; if the WS transport delivers nothing (server
# failed to start, app could not connect) the legacy file/chunk decode below
# is used instead, so this is purely additive.
WS_RAW_DIR="$SCREENSHOT_TMP_DIR/ws"
mkdir -p "$WS_RAW_DIR"
if cn1ss_start_ws_server "$WS_RAW_DIR"; then
  rm_log "WebSocket screenshot server listening on port ${CN1SS_WS_PORT} (out=$WS_RAW_DIR)"
else
  rm_log "WebSocket screenshot server did not start; relying on file/chunk fallback"
fi

# Patch CN1SS_* placeholders in the shared scheme (the iOS pipeline does
# this too -- the placeholders come from create-shared-scheme.py). Mac
# binaries are launched outside Xcode, so the env vars are also exported
# explicitly below; the scheme patch is kept for parity / debugging-from-IDE.
SCHEME_FILE="$WORKSPACE_PATH/xcshareddata/xcschemes/$SCHEME.xcscheme"
if [ ! -f "$SCHEME_FILE" ] && [[ "$WORKSPACE_PATH" == *.xcworkspace ]]; then
  PROJECT_DIR="$(cd "$(dirname "$WORKSPACE_PATH")" && pwd)"
  PROJECT_SCHEME_FILE="$PROJECT_DIR/$(basename "$WORKSPACE_PATH" .xcworkspace).xcodeproj/xcshareddata/xcschemes/$SCHEME.xcscheme"
  if [ -f "$PROJECT_SCHEME_FILE" ]; then
    SCHEME_FILE="$PROJECT_SCHEME_FILE"
  fi
fi
if [ -f "$SCHEME_FILE" ]; then
  if sed --version >/dev/null 2>&1; then
    sed -i -e "s|__CN1SS_OUTPUT_DIR__|$SCREENSHOT_RAW_DIR|g" \
           -e "s|__CN1SS_PREVIEW_DIR__|$SCREENSHOT_PREVIEW_DIR|g" "$SCHEME_FILE"
  else
    sed -i '' -e "s|__CN1SS_OUTPUT_DIR__|$SCREENSHOT_RAW_DIR|g" \
              -e "s|__CN1SS_PREVIEW_DIR__|$SCREENSHOT_PREVIEW_DIR|g" "$SCHEME_FILE"
  fi
  rm_log "Injected CN1SS_* envs into scheme: $SCHEME_FILE"
fi

HOST_ARCH="$(uname -m 2>/dev/null || echo arm64)"
case "$HOST_ARCH" in
  arm64|x86_64) BUILD_ARCH="$HOST_ARCH" ;;
  *) BUILD_ARCH="arm64" ;;
esac

DERIVED_DATA_DIR="$SCREENSHOT_TMP_DIR/derived"
rm -rf "$DERIVED_DATA_DIR"
BUILD_LOG="$ARTIFACTS_DIR/xcodebuild-build.log"

# Mac Catalyst destination + configuration. CODE_SIGN_* are disabled so
# unsigned local / CI runs don't require provisioning. The macNative
# entitlements file is still set via CODE_SIGN_ENTITLEMENTS on the project
# (IPhoneBuilder injects it), but with signing disabled it is a no-op.
rm_log "Building Mac Catalyst app with xcodebuild"
COMPILE_START=$(date +%s)
XCODE_BUILD_CMD=(
  xcodebuild
  "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH"
  -scheme "$SCHEME"
  -configuration Debug
  -destination 'platform=macOS,variant=Mac Catalyst'
  -destination-timeout 120
  -derivedDataPath "$DERIVED_DATA_DIR"
  "ARCHS=$BUILD_ARCH"
  "ONLY_ACTIVE_ARCH=YES"
  "CODE_SIGN_IDENTITY="
  "CODE_SIGNING_REQUIRED=NO"
  "CODE_SIGNING_ALLOWED=NO"
  # Optimize the translated C (Xcode Debug defaults to -O0) so the SIMD benchmark
  # compares against auto-vectorized scalar -- honest and matching Windows /O2.
  # Override with CN1_TEST_OPT_LEVEL (0/1/2/3/s).
  "GCC_OPTIMIZATION_LEVEL=${CN1_TEST_OPT_LEVEL:-2}"
  build
)

# On fresh GitHub macOS runners the very first xcodebuild against a
# freshly-generated project sometimes runs before Xcode has enumerated its
# build destinations, so the build aborts in ~2s with an *empty* "Available
# destinations" list and "Unable to find a destination matching ... Mac
# Catalyst". Mac Catalyst is not a downloadable runtime -- it is the host
# macOS SDK already shipped inside Xcode, so there is nothing to fetch; the
# destination cache just needs to warm. Poll -showdestinations until the
# Catalyst destination is listed before building (mirrors the iOS scripts).
warm_catalyst_destination() {
  local deadline=$(( $(date +%s) + 90 ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    if xcodebuild "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH" -scheme "$SCHEME" \
         -showdestinations 2>/dev/null | grep -q "Mac Catalyst"; then
      return 0
    fi
    sleep 5
  done
  return 1
}

# The Catalyst destination of an SDKROOT=iphoneos target also needs the iOS
# platform content installed in the active Xcode. Some runner instances ship
# Xcode without it (the same provisioning gap run-ios-native-tests.sh handles),
# and no amount of destination warming helps -- the scheme lists only
# watchOS/tvOS destinations. Detect the missing iphoneos SDK and download the
# platform, restarting a wedged CoreSimulator service when the download fails
# with "Unable to connect to simulator".
ensure_ios_platform() {
  if xcodebuild -showsdks 2>/dev/null | grep -q "iphoneos"; then
    return 0
  fi
  rm_log "The active Xcode has no iOS platform (required for the Mac Catalyst destination); downloading via xcodebuild -downloadPlatform iOS"
  local out
  out="$(xcodebuild -downloadPlatform iOS 2>&1)" || true
  printf '%s\n' "$out"
  if printf '%s' "$out" | grep -qi "Unable to connect to simulator"; then
    rm_log "CoreSimulator not responding; restarting the service and retrying the platform download"
    killall -9 com.apple.CoreSimulator.CoreSimulatorService 2>/dev/null || true
    sleep 5
    xcrun simctl list runtimes >/dev/null 2>&1 || true
    xcodebuild -downloadPlatform iOS || true
  fi
}
ensure_ios_platform

BUILD_OK=0
for attempt in 1 2 3; do
  if [ "$attempt" -gt 1 ]; then
    rm_log "xcodebuild could not resolve the Mac Catalyst destination (attempt $((attempt - 1))); warming destinations and retrying"
  fi
  if warm_catalyst_destination; then
    rm_log "Mac Catalyst destination available"
  else
    rm_log "Mac Catalyst destination still not listed after 90s warm-up; attempting build anyway"
  fi
  if "${XCODE_BUILD_CMD[@]}" | tee "$BUILD_LOG"; then
    BUILD_OK=1
    break
  fi
  # Only the destination-resolution race is transient -- a genuine clang/link
  # error must fail fast rather than burn three full builds.
  if ! grep -q "Unable to find a destination matching" "$BUILD_LOG"; then
    break
  fi
done
if [ "$BUILD_OK" -ne 1 ]; then
  rm_log "STAGE:XCODE_BUILD_FAILED -> See $BUILD_LOG"
  exit 10
fi
COMPILE_END=$(date +%s)
COMPILATION_TIME=$((COMPILE_END - COMPILE_START))
rm_log "Compilation time: ${COMPILATION_TIME}s"

# Locate the produced .app under DerivedData. Mac Catalyst products land
# under Debug-maccatalyst/ (vs Debug-iphonesimulator/ on iOS).
BUILD_SETTINGS="$(xcodebuild "$XCODE_CONTAINER_FLAG" "$WORKSPACE_PATH" -scheme "$SCHEME" -configuration Debug -destination 'platform=macOS,variant=Mac Catalyst' -showBuildSettings 2>/dev/null || true)"
WRAPPER_NAME="$(printf '%s\n' "$BUILD_SETTINGS" | awk -F' = ' '/ WRAPPER_NAME /{print $2; exit}' | tr -d ' ')"
if [ -z "$WRAPPER_NAME" ]; then
  WRAPPER_NAME="${SCHEME}.app"
fi
if [ -z "$APP_BUNDLE_PATH" ]; then
  CANDIDATE_BUNDLE="$DERIVED_DATA_DIR/Build/Products/Debug-maccatalyst/$WRAPPER_NAME"
  if [ -d "$CANDIDATE_BUNDLE" ]; then
    APP_BUNDLE_PATH="$CANDIDATE_BUNDLE"
  fi
fi
if [ -z "$APP_BUNDLE_PATH" ]; then
  CANDIDATE_BUNDLE="$(find "$DERIVED_DATA_DIR" -path "*/Debug-maccatalyst/$WRAPPER_NAME" -type d -print -quit 2>/dev/null || true)"
  if [ -d "$CANDIDATE_BUNDLE" ]; then
    APP_BUNDLE_PATH="$CANDIDATE_BUNDLE"
  fi
fi
if [ -z "$APP_BUNDLE_PATH" ] || [ ! -d "$APP_BUNDLE_PATH" ]; then
  rm_log "FATAL: Mac Catalyst app bundle not found under $DERIVED_DATA_DIR (looking for $WRAPPER_NAME)"
  find "$DERIVED_DATA_DIR/Build/Products" -maxdepth 2 -type d -print >&2 2>/dev/null || true
  exit 11
fi
rm_log "Found Mac Catalyst app bundle at $APP_BUNDLE_PATH"

APP_PROCESS_NAME="${WRAPPER_NAME%.app}"
APP_EXECUTABLE="$APP_BUNDLE_PATH/Contents/MacOS/$APP_PROCESS_NAME"
if [ ! -x "$APP_EXECUTABLE" ]; then
  rm_log "FATAL: Mac Catalyst executable not found at $APP_EXECUTABLE"
  ls -l "$APP_BUNDLE_PATH/Contents/MacOS/" >&2 2>/dev/null || true
  exit 11
fi
BUNDLE_IDENTIFIER="$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$APP_BUNDLE_PATH/Contents/Info.plist" 2>/dev/null || true)"
if [ -z "$BUNDLE_IDENTIFIER" ]; then
  rm_log "Warning: could not read CFBundleIdentifier from $APP_BUNDLE_PATH"
fi
rm_log "App bundle id: ${BUNDLE_IDENTIFIER:-<unknown>}"
rm_log "App executable: $APP_EXECUTABLE"

# Mac Catalyst apps need a proper window-server session to keep their
# UIScene alive. Running the binary directly from a non-interactive shell
# (which is what the CI runner provides) launches the app outside of any
# Aqua session; the app gets a few seconds before the OS tears it down,
# which on CI manifests as an exit before any CN1SS:CHUNK is emitted.
# Launching via `open -W -n` routes through LaunchServices so the app
# gets the same session a normal user-launched Mac app would. Stdout is
# no longer connected to our pipe in that flow, so the unified log
# becomes the primary capture channel; stdout is best-effort.
APP_PID=0
LOG_STREAM_PID=0
APP_PROCESS_NAME_PATTERN="^${APP_PROCESS_NAME}$"
cleanup() {
  if [ "$LOG_STREAM_PID" -ne 0 ]; then
    kill "$LOG_STREAM_PID" >/dev/null 2>&1 || true
    wait "$LOG_STREAM_PID" 2>/dev/null || true
  fi
  # Terminate the launched Catalyst app by process name (open -W detaches
  # the child PID so a direct kill of $APP_PID only ends the `open`
  # wrapper, not the actual app). pkill -x matches exact name so we
  # don't accidentally hit a host process.
  pkill -x "$APP_PROCESS_NAME" >/dev/null 2>&1 || true
  for _ in 1 2 3 4 5; do
    pkill -0 -x "$APP_PROCESS_NAME" >/dev/null 2>&1 || break
    sleep 1
  done
  pkill -9 -x "$APP_PROCESS_NAME" >/dev/null 2>&1 || true
  if [ "$APP_PID" -ne 0 ]; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

rm_log "Starting unified log stream (primary capture)"
if [ -n "$BUNDLE_IDENTIFIER" ]; then
  log stream --style compact --level debug \
    --predicate "(subsystem == \"$BUNDLE_IDENTIFIER\") OR (composedMessage CONTAINS \"CN1SS\") OR (eventMessage CONTAINS \"CN1SS\")" \
    > "$FALLBACK_LOG" 2>&1 &
else
  log stream --style compact --level debug \
    --predicate "(composedMessage CONTAINS \"CN1SS\") OR (eventMessage CONTAINS \"CN1SS\")" \
    > "$FALLBACK_LOG" 2>&1 &
fi
LOG_STREAM_PID=$!
sleep 1

# Make sure no leftover instance is around from a previous attempt.
pkill -x "$APP_PROCESS_NAME" >/dev/null 2>&1 || true
sleep 1

rm_log "Launching Mac Catalyst app via LaunchServices: $APP_BUNDLE_PATH"
# Timestamp marker so crash reports written during this run can be picked
# out of ~/Library/Logs/DiagnosticReports afterwards (find -newer).
LAUNCH_MARKER="$SCREENSHOT_TMP_DIR/.launch-marker"
touch "$LAUNCH_MARKER"
LAUNCH_START=$(date +%s)
# `open -W -n -F` waits for the app to terminate, forces a fresh
# instance, and skips state restoration. `--stdout / --stderr` pipe the
# bundled binary's I/O straight to a file (avoids the `log stream` drop
# problem that occurs once base64 PNG chunks land in os_log). `--env`
# forwards CN1SS_OUTPUT_DIR / CN1SS_PREVIEW_DIR into the launched
# process even though LaunchServices doesn't inherit the parent shell
# environment.
(
  open -W -n -F \
       --stdout "$TEST_LOG" \
       --stderr "$TEST_LOG" \
       --env "CN1SS_OUTPUT_DIR=$CN1SS_OUTPUT_DIR" \
       --env "CN1SS_PREVIEW_DIR=$CN1SS_PREVIEW_DIR" \
       -a "$APP_BUNDLE_PATH"
) &
APP_PID=$!
LAUNCH_END=$(date +%s)
echo "App Launch : $(( (LAUNCH_END - LAUNCH_START) * 1000 )) ms" >> "$ARTIFACTS_DIR/mac-test-stats.txt"

# Resolve the actual app PID once LaunchServices has started it. Used
# only for diagnostics; the wait loop polls log content, not the PID.
sleep 2
RESOLVED_APP_PID="$(pgrep -x "$APP_PROCESS_NAME" 2>/dev/null | head -n 1 || true)"
if [ -n "$RESOLVED_APP_PID" ]; then
  rm_log "Mac Catalyst app pid=$RESOLVED_APP_PID"
else
  rm_log "Warning: could not resolve pid for $APP_PROCESS_NAME"
fi

# When the suite times out the app is usually not idle: ParparVM's
# SignalHandler (CodenameOne_GLAppDelegate.m) converts SIGSEGV into a Java
# NPE and returns, so a thread that faulted outside a Java try frame
# re-executes the faulting instruction forever -- that is the "We had a
# signal 11" spam seen in device-runner.log when the suite "hangs". A
# process sample taken at timeout therefore contains the exact faulting
# stack (and, for genuine deadlocks, every thread's wait state).
capture_hang_diagnostics() {
  local pid spam
  report_inflight_test
  pid="$(pgrep -x "$APP_PROCESS_NAME" 2>/dev/null | head -n 1 || true)"
  if [ -n "$pid" ]; then
    rm_log "Sampling hung app (pid=$pid) -> app-hang-sample.txt"
    sample "$pid" 5 -file "$ARTIFACTS_DIR/app-hang-sample.txt" >/dev/null 2>&1 || true
  else
    rm_log "No live $APP_PROCESS_NAME process found to sample"
  fi
  spam="$(grep -c 'We had a signal' "$TEST_LOG" 2>/dev/null || echo 0)"
  if [ "${spam:-0}" -gt 0 ]; then
    rm_log "Signal-handler loop detected: ${spam} 'We had a signal' lines in app stdout (a crashed thread is spinning in ParparVM's SignalHandler; see app-hang-sample.txt for the faulting stack)"
  fi
}

# Report the screenshot test that was in flight when the app died. Every test
# logs "CN1SS:INFO:suite starting test=<name>" before it runs and
# "CN1SS:INFO:suite finished test=<name>" after; the test whose start has no
# matching finish is the one that crashed. This single line turns an opaque
# "delivered N of 128" into "it died in <test>", which is the first thing you
# need to know to fix a flaky mid-suite crash.
report_inflight_test() {
  local started finished
  started="$(grep -hoE 'suite starting test=[^ ]+' "$TEST_LOG" "$FALLBACK_LOG" 2>/dev/null \
              | sed -E 's/.*test=//' | tail -n 1)"
  finished="$(grep -hoE 'suite finished test=[^ ]+' "$TEST_LOG" "$FALLBACK_LOG" 2>/dev/null \
               | sed -E 's/.*test=//' | tail -n 1)"
  if [ -n "$started" ] && [ "$started" != "$finished" ]; then
    rm_log "STAGE:APP_CRASHED -> in-flight test when the app died: '${started}' (last completed: '${finished:-<none>}')"
  elif [ -n "$finished" ]; then
    rm_log "STAGE:APP_CRASHED -> app exited after completing '${finished}' but before the suite-finished marker"
  fi
}

# Diagnostics for the crash / early-exit path (app exited before emitting the
# completion marker). Unlike a timeout there is usually no live process left to
# `sample`, so the authoritative faulting stack comes from the OS crash report
# (harvested separately) and from the tail of the app's own stdout, which
# carries ParparVM's "Codename One revisions:" crash dump. That dump is often
# truncated in the captured file because the process dies mid-write with
# stdout block-buffered, so we surface whatever made it to disk directly in the
# job log -- a partial native dump still names the faulting area.
capture_crash_diagnostics() {
  local pid
  report_inflight_test
  pid="$(pgrep -x "$APP_PROCESS_NAME" 2>/dev/null | head -n 1 || true)"
  if [ -n "$pid" ]; then
    rm_log "A $APP_PROCESS_NAME process is still alive (pid=$pid); sampling -> app-hang-sample.txt"
    sample "$pid" 3 -file "$ARTIFACTS_DIR/app-hang-sample.txt" >/dev/null 2>&1 || true
  fi
  if [ -s "$TEST_LOG" ]; then
    rm_log "---- last 25 lines of app stdout ($TEST_LOG) ----"
    tail -n 25 "$TEST_LOG" 2>/dev/null | sed 's/^/[app-stdout] /'
    rm_log "---- end of app stdout tail ----"
  fi
}

# Harvest OS crash reports for the app. macOS writes a .ips (the authoritative
# native faulting stack + termination reason) for a real SIGSEGV/SIGBUS, but
# ReportCrash runs asynchronously, so on a fast CI runner the report may not be
# on disk yet when the app's `open` wrapper returns. Poll for up to ~45s when a
# crash is suspected. Search both the per-user and system DiagnosticReports
# dirs, and match the process name as well as common Catalyst report suffixes
# (.ips / .crash / .diag). Echo the first report's header (Exception Type /
# Termination / Crashed Thread) into the job log so the cause is visible
# without unzipping the artifact bundle.
harvest_crash_reports() {
  local wait_for_report="$1" deadline=0 found=0 dir crash_file
  local -a report_dirs=("$HOME/Library/Logs/DiagnosticReports" "/Library/Logs/DiagnosticReports")
  if [ "$wait_for_report" = "1" ]; then
    deadline=$(( $(date +%s) + 45 ))
  fi
  while true; do
    found=0
    for dir in "${report_dirs[@]}"; do
      [ -d "$dir" ] || continue
      while IFS= read -r crash_file; do
        [ -n "$crash_file" ] || continue
        found=1
        local base; base="$(basename "$crash_file")"
        if [ ! -f "$ARTIFACTS_DIR/$base" ]; then
          rm_log "Collected crash report: $base (from $dir)"
          cp -f "$crash_file" "$ARTIFACTS_DIR/" 2>/dev/null || true
          # Surface the key fields. .ips reports are JSON-ish; grep the human
          # header lines that exist in both the legacy and IPS formats.
          rm_log "---- crash report header: $base ----"
          grep -aE 'Exception Type|Exception Codes|Termination|Crashed Thread|"signal"|"exceptionType"|faulting' "$crash_file" 2>/dev/null \
            | head -n 8 | sed 's/^/[crash] /'
          rm_log "---- end crash report header ----"
        fi
      done < <(find "$dir" -maxdepth 1 -name "${APP_PROCESS_NAME}*" \
                 -newer "$LAUNCH_MARKER" 2>/dev/null)
    done
    if [ "$found" -eq 1 ] || [ "$wait_for_report" != "1" ] || [ "$(date +%s)" -ge "$deadline" ]; then
      break
    fi
    sleep 3
  done
  if [ "$found" -eq 0 ] && [ "$wait_for_report" = "1" ]; then
    rm_log "No OS crash report (.ips) found for $APP_PROCESS_NAME within the wait window; the native faulting stack may be unavailable (ReportCrash disabled or the process was SIGKILLed). The app-stdout tail above is the best remaining signal."
  fi
}

END_MARKER="CN1SS:SUITE:FINISHED"
TIMEOUT_SECONDS="${CN1SS_SUITE_TIMEOUT_SECONDS:-1500}"
APP_CRASHED=0
START_TIME="$(date +%s)"
rm_log "Waiting for DeviceRunner completion marker ($END_MARKER) -- timeout ${TIMEOUT_SECONDS}s"
while true; do
  if [ -s "$FALLBACK_LOG" ] && grep -q "$END_MARKER" "$FALLBACK_LOG"; then
    rm_log "Detected completion marker in unified log"
    break
  fi
  if [ -s "$TEST_LOG" ] && grep -q "$END_MARKER" "$TEST_LOG"; then
    rm_log "Detected completion marker in `open` stdout fallback"
    break
  fi
  # Bail out early if the app process is gone (open -W has returned).
  if [ "$APP_PID" -ne 0 ] && ! kill -0 "$APP_PID" >/dev/null 2>&1; then
    if ! pgrep -x "$APP_PROCESS_NAME" >/dev/null 2>&1; then
      rm_log "App process exited before completion marker -- check $FALLBACK_LOG"
      APP_CRASHED=1
      capture_crash_diagnostics
      break
    fi
  fi
  NOW="$(date +%s)"
  if [ $(( NOW - START_TIME )) -ge $TIMEOUT_SECONDS ]; then
    rm_log "STAGE:TIMEOUT -> DeviceRunner did not emit completion marker within ${TIMEOUT_SECONDS}s"
    capture_hang_diagnostics
    break
  fi
  sleep 5
done
END_TIME=$(date +%s)
echo "Test Execution : $(( (END_TIME - START_TIME) * 1000 )) ms" >> "$ARTIFACTS_DIR/mac-test-stats.txt"

sleep 2

# Drain the unified-log fallback before tearing it down.
kill "$LOG_STREAM_PID" >/dev/null 2>&1 || true
wait "$LOG_STREAM_PID" 2>/dev/null || true
LOG_STREAM_PID=0

# Belt-and-suspenders: run `log show` for the last 30 minutes filtered to
# CN1SS so any late messages that the stream didn't get are captured.
LATE_FALLBACK_LOG="$ARTIFACTS_DIR/device-runner-late-fallback.log"
log show --style syslog --last 30m \
  --predicate "(composedMessage CONTAINS \"CN1SS\") OR (eventMessage CONTAINS \"CN1SS\")" \
  > "$LATE_FALLBACK_LOG" 2>/dev/null || true

BASE64_STATS_FILE="$ARTIFACTS_DIR/base64-performance-stats.txt"
extract_base64_stats "$BASE64_STATS_FILE" "$TEST_LOG" "$FALLBACK_LOG" "$LATE_FALLBACK_LOG"
if [ -s "$BASE64_STATS_FILE" ]; then
  rm_log "Base64 benchmark stats captured at $BASE64_STATS_FILE"
fi

SUITE_FAILURE_LINES="$(cn1ss_collect_suite_failures "$TEST_LOG" "$FALLBACK_LOG" "$LATE_FALLBACK_LOG")"
if [ -n "$SUITE_FAILURE_LINES" ]; then
  rm_log "Detected DeviceRunner assertion/test failure(s); artifacts and screenshot report will still be collected before failing."
fi

# Tear down the app process if it's still running (it sometimes is,
# especially when the test suite finishes but the NSApplication run loop
# keeps the process alive until SIGTERM).
if kill -0 "$APP_PID" >/dev/null 2>&1; then
  kill "$APP_PID" >/dev/null 2>&1 || true
fi
wait "$APP_PID" 2>/dev/null || true
APP_PID=0

# Collect any crash reports the OS wrote for the app during this run (covers
# the case where the process died outright instead of spinning in the signal
# handler -- LaunchServices apps report to DiagnosticReports, not to our stdout
# pipe). When we already know the app crashed (it exited before the completion
# marker) wait for ReportCrash to flush the .ips, since it is the only place the
# native faulting stack survives; otherwise do a single non-blocking sweep so a
# clean run is not slowed down.
harvest_crash_reports "$APP_CRASHED"

# The app has exited; stop the WebSocket server and adopt whatever it
# received. The server wrote one <test>.png per delivered screenshot into
# $WS_RAW_DIR. When WS delivered at least one image we use that set directly
# and skip the legacy file/chunk decode entirely.
cn1ss_stop_ws_server
declare -a COMPARE_ENTRIES=()
WS_DELIVERED=0
if [ -d "${WS_RAW_DIR:-}" ]; then
  for ws_png in "$WS_RAW_DIR"/*.png; do
    [ -s "$ws_png" ] || continue
    ws_test="$(basename "$ws_png" .png)"
    ws_dest="$SCREENSHOT_TMP_DIR/${ws_test}.png"
    cp -f "$ws_png" "$ws_dest" 2>/dev/null || continue
    COMPARE_ENTRIES+=("${ws_test}=${ws_dest}")
    WS_DELIVERED=$(( WS_DELIVERED + 1 ))
  done
fi
if [ "$WS_DELIVERED" -gt 0 ]; then
  rm_log "WebSocket transport delivered ${WS_DELIVERED} screenshot(s); using WS path (legacy file/chunk decode skipped)"
fi

# WebSocket is the only transport now. If it delivered nothing the on-device
# suite either never ran or produced no screenshots -- fail loudly; there is
# no syslog/base64/file fallback any more.
if [ "$WS_DELIVERED" -eq 0 ]; then
  rm_log "STAGE:MARKERS_NOT_FOUND -> no screenshots delivered over WebSocket"
  rm_log "---- CN1SS lines from log ----"
  (grep "CN1SS:" "$TEST_LOG" 2>/dev/null || true) | sed 's/^/[CN1SS] /'
  exit 12
fi

COMPARE_JSON="$SCREENSHOT_TMP_DIR/screenshot-compare.json"
SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"
export CN1SS_PORT_ID="${CN1SS_PORT_ID:-mac-native}"
export CN1SS_SUITE_LOG="$TEST_LOG"
export CN1SS_SUITE_LOG_2="$FALLBACK_LOG"
export CN1SS_SUITE_LOG_3="$LATE_FALLBACK_LOG"

export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"
# Distinct PR-comment marker / preview path / title so this job posts its
# own comment instead of overwriting the iOS / iOS Metal job's comment.
export CN1SS_COMMENT_MARKER="${CN1SS_COMMENT_MARKER:-<!-- CN1SS_MAC_NATIVE_COMMENT -->}"
export CN1SS_COMMENT_LOG_PREFIX="${CN1SS_COMMENT_LOG_PREFIX:-[run-mac-native-ui-tests]}"
export CN1SS_PREVIEW_SUBDIR="${CN1SS_PREVIEW_SUBDIR:-mac-native}"
export CN1SS_SUCCESS_MESSAGE="${CN1SS_SUCCESS_MESSAGE:-✅ Native Mac screenshot tests passed.}"
REPORT_TITLE="${CN1SS_REPORT_TITLE:-Mac native screenshot updates}"

CN1SS_VM_TIME=0
if [ -f "$ARTIFACTS_DIR/vm_time.txt" ]; then
  CN1SS_VM_TIME=$(cat "$ARTIFACTS_DIR/vm_time.txt")
  rm_log "Loaded VM translation time: ${CN1SS_VM_TIME}s"
fi
export CN1SS_VM_TIME
export CN1SS_COMPILATION_TIME="$COMPILATION_TIME"

cn1ss_process_and_report \
  "$REPORT_TITLE" \
  "$COMPARE_JSON" \
  "$SUMMARY_FILE" \
  "$COMMENT_FILE" \
  "$SCREENSHOT_REF_DIR" \
  "$SCREENSHOT_PREVIEW_DIR" \
  "$ARTIFACTS_DIR" \
  "${COMPARE_ENTRIES[@]}"
comment_rc=$?

cp -f "$BUILD_LOG" "$ARTIFACTS_DIR/xcodebuild-build.log" 2>/dev/null || true
cp -f "$TEST_LOG" "$ARTIFACTS_DIR/device-runner.log" 2>/dev/null || true

if [ -n "$SUITE_FAILURE_LINES" ]; then
  rm_log "STAGE:DEVICE_RUNNER_TEST_FAILED -> assertion/test failure(s) are not allowed:"
  printf '%s\n' "$SUITE_FAILURE_LINES" | sed 's/^/[CN1SS-FAIL] /'
  exit 19
fi

# Screenshot mismatch / count-regression guards are centralised in
# cn1ss_process_and_report (scripts/lib/cn1ss.sh), which returns these
# codes only when CN1SS_FAIL_ON_MISMATCH=1:
#   15 - a screenshot differs from / errored against its stored baseline
#   17 - fewer screenshots were produced than there are stored references
#        (a test failed to emit; the suite most likely hung or crashed
#        partway). The count floor is the size of $SCREENSHOT_REF_DIR
#        (optionally raised via CN1SS_MIN_SCREENSHOTS). While the Mac port
#        is still seeding its baseline the reference dir is small, so the
#        floor is naturally low and seeding runs are not failed by it.
# comment_rc already carries those codes; surface it as the exit status.
if [ "${comment_rc:-0}" -eq 15 ] || [ "${comment_rc:-0}" -eq 17 ]; then
  rm_log "STAGE:SCREENSHOT_REGRESSION -> failing with exit ${comment_rc} (see cn1ss FATAL message above)."
fi

exit $comment_rc
