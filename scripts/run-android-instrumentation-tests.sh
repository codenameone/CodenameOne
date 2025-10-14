#!/usr/bin/env bash
# Run instrumentation tests and reconstruct screenshot emitted as chunked Base64 (NO ADB)
set -euo pipefail

ra_log() { echo "[run-android-instrumentation-tests] $1"; }

# ---- Helpers ---------------------------------------------------------------

ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

# CN1SS helpers are implemented in Python for easier maintenance
CN1SS_TOOL=""

count_chunks() {
  local f="${1:-}"
  if [ -z "$CN1SS_TOOL" ] || [ ! -x "$CN1SS_TOOL" ]; then
    echo 0
    return
  fi
  if [ -z "$f" ] || [ ! -r "$f" ]; then
    echo 0
    return
  fi
  python3 "$CN1SS_TOOL" count "$f" 2>/dev/null || echo 0
}

extract_cn1ss_base64() {
  local f="${1:-}"
  if [ -z "$CN1SS_TOOL" ] || [ ! -x "$CN1SS_TOOL" ]; then
    return 1
  fi
  if [ -z "$f" ] || [ ! -r "$f" ]; then
    return 1
  fi
  python3 "$CN1SS_TOOL" extract "$f"
}

decode_cn1ss_png() {
  local f="${1:-}"
  if [ -z "$CN1SS_TOOL" ] || [ ! -x "$CN1SS_TOOL" ]; then
    return 1
  fi
  if [ -z "$f" ] || [ ! -r "$f" ]; then
    return 1
  fi
  python3 "$CN1SS_TOOL" extract "$f" --decode
}

# Verify PNG signature + non-zero size
verify_png() {
  local f="$1"
  [ -s "$f" ] || return 1
  head -c 8 "$f" | od -An -t x1 | tr -d ' \n' | grep -qi '^89504e470d0a1a0a$'
}

# ---- Args & environment ----------------------------------------------------

if [ $# -lt 1 ]; then
  ra_log "Usage: $0 <gradle_project_dir>" >&2
  exit 2
fi
GRADLE_PROJECT_DIR="$1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_TOOL="$SCRIPT_DIR/android/tests/cn1ss_chunk_tools.py"
if [ ! -x "$CN1SS_TOOL" ]; then
  ra_log "Missing CN1SS helper: $CN1SS_TOOL" >&2
  exit 3
fi

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
ensure_dir "$ARTIFACTS_DIR"
TEST_LOG="$ARTIFACTS_DIR/connectedAndroidTest.log"
SCREENSHOT_OUT="$ARTIFACTS_DIR/emulator-screenshot.png"

ra_log "Loading workspace environment from $ENV_FILE"
[ -f "$ENV_FILE" ] || { ra_log "Missing env file: $ENV_FILE"; exit 3; }
# shellcheck disable=SC1090
source "$ENV_FILE"

[ -d "$GRADLE_PROJECT_DIR" ] || { ra_log "Gradle project directory not found: $GRADLE_PROJECT_DIR"; exit 4; }
[ -x "$GRADLE_PROJECT_DIR/gradlew" ] || chmod +x "$GRADLE_PROJECT_DIR/gradlew"

# ---- Run tests -------------------------------------------------------------

set -o pipefail
ra_log "Running instrumentation tests (stdout -> $TEST_LOG; stderr -> terminal)"
(
  cd "$GRADLE_PROJECT_DIR"
  ORIG_JAVA_HOME="${JAVA_HOME:-}"
  export JAVA_HOME="${JAVA17_HOME:?JAVA17_HOME not set}"
  ./gradlew --no-daemon --console=plain connectedDebugAndroidTest | tee "$TEST_LOG"
  export JAVA_HOME="$ORIG_JAVA_HOME"
) || { ra_log "STAGE:GRADLE_TEST_FAILED (see $TEST_LOG)"; exit 10; }

echo
ra_log "==== Begin connectedAndroidTest.log (tail -n 200) ===="
tail -n 200 "$TEST_LOG" || true
ra_log "==== End connectedAndroidTest.log ===="
echo

# ---- Locate outputs (NO ADB) ----------------------------------------------

RESULTS_ROOT="$GRADLE_PROJECT_DIR/app/build/outputs/androidTest-results/connected"
ra_log "Listing connected test outputs under: $RESULTS_ROOT"
find "$RESULTS_ROOT" -maxdepth 4 -printf '%y %p\n' 2>/dev/null | sed 's/^/[run-android-instrumentation-tests]   /' || true

# Arrays must be declared for set -u safety
declare -a XMLS=()
declare -a LOGCATS=()
TEST_EXEC_LOG=""

# XML result candidates (new + old formats), mtime desc
mapfile -t XMLS < <(
  find "$RESULTS_ROOT" -type f \( -name 'test-result.xml' -o -name 'TEST-*.xml' \) \
  -printf '%T@ %p\n' 2>/dev/null | sort -nr | awk '{ $1=""; sub(/^ /,""); print }'
) || XMLS=()

# logcat files produced by AGP
mapfile -t LOGCATS < <(
  find "$RESULTS_ROOT" -type f -name 'logcat-*.txt' -print 2>/dev/null
) || LOGCATS=()

# execution log (use first if present)
TEST_EXEC_LOG="$(find "$RESULTS_ROOT" -type f -path '*/testlog/test-results.log' -print -quit 2>/dev/null || true)"
[ -n "${TEST_EXEC_LOG:-}" ] || TEST_EXEC_LOG=""

if [ "${#XMLS[@]}" -gt 0 ]; then
  ra_log "Found ${#XMLS[@]} test result file(s). First candidate: ${XMLS[0]}"
else
  ra_log "No test result XML files found under $RESULTS_ROOT"
fi

# Pick first logcat if any
LOGCAT_FILE="${LOGCATS[0]:-}"
if [ -z "${LOGCAT_FILE:-}" ] || [ ! -s "$LOGCAT_FILE" ]; then
  ra_log "FATAL: No logcat-*.txt produced by connectedDebugAndroidTest (cannot extract CN1SS chunks)."
  exit 12
fi

# ---- Chunk accounting (diagnostics) ---------------------------------------

XML_CHUNKS_TOTAL=0
for x in "${XMLS[@]}"; do
  c="$(count_chunks "$x")"; c="${c//[^0-9]/}"; : "${c:=0}"
  XML_CHUNKS_TOTAL=$(( XML_CHUNKS_TOTAL + c ))
done
LOGCAT_CHUNKS="$(count_chunks "$LOGCAT_FILE")"; LOGCAT_CHUNKS="${LOGCAT_CHUNKS//[^0-9]/}"; : "${LOGCAT_CHUNKS:=0}"
EXECLOG_CHUNKS="$(count_chunks "${TEST_EXEC_LOG:-}")"; EXECLOG_CHUNKS="${EXECLOG_CHUNKS//[^0-9]/}"; : "${EXECLOG_CHUNKS:=0}"

ra_log "Chunk counts -> XML: ${XML_CHUNKS_TOTAL} | logcat: ${LOGCAT_CHUNKS} | test-results.log: ${EXECLOG_CHUNKS}"

if [ "${LOGCAT_CHUNKS:-0}" = "0" ] && [ "${XML_CHUNKS_TOTAL:-0}" = "0" ] && [ "${EXECLOG_CHUNKS:-0}" = "0" ]; then
  ra_log "STAGE:MARKERS_NOT_FOUND -> The test did not emit CN1SS chunks"
  ra_log "Hints:"
  ra_log "  • Ensure the test actually ran (check FAILED vs SUCCESS in $TEST_LOG)"
  ra_log "  • Check for CN1SS:ERR or CN1SS:INFO lines below"
  ra_log "---- CN1SS lines from any result files ----"
  (grep -R "CN1SS:" "$RESULTS_ROOT" || true) | sed 's/^/[CN1SS] /'
  exit 12
fi

# ---- Reassemble (prefer XML → logcat → exec log) --------------------------

: > "$SCREENSHOT_OUT"
SOURCE=""

if [ "${#XMLS[@]}" -gt 0 ] && [ "${XML_CHUNKS_TOTAL:-0}" -gt 0 ]; then
  for x in "${XMLS[@]}"; do
    c="$(count_chunks "$x")"; c="${c//[^0-9]/}"; : "${c:=0}"
    [ "$c" -gt 0 ] || continue
    ra_log "Reassembling from XML: $x (chunks=$c)"
    if decode_cn1ss_png "$x" > "$SCREENSHOT_OUT" 2>/dev/null; then
      if verify_png "$SCREENSHOT_OUT"; then SOURCE="XML"; break; fi
    fi
  done
fi

if [ -z "$SOURCE" ] && [ "${LOGCAT_CHUNKS:-0}" -gt 0 ]; then
  ra_log "Reassembling from logcat: $LOGCAT_FILE (chunks=$LOGCAT_CHUNKS)"
  if decode_cn1ss_png "$LOGCAT_FILE" > "$SCREENSHOT_OUT" 2>/dev/null; then
    if verify_png "$SCREENSHOT_OUT"; then SOURCE="LOGCAT"; fi
  fi
fi

if [ -z "$SOURCE" ] && [ -n "${TEST_EXEC_LOG:-}" ] && [ "${EXECLOG_CHUNKS:-0}" -gt 0 ]; then
  ra_log "Reassembling from test-results.log: $TEST_EXEC_LOG (chunks=$EXECLOG_CHUNKS)"
  if decode_cn1ss_png "$TEST_EXEC_LOG" > "$SCREENSHOT_OUT" 2>/dev/null; then
    if verify_png "$SCREENSHOT_OUT"; then SOURCE="EXECLOG"; fi
  fi
fi

# ---- Final validation / failure paths -------------------------------------

if [ -z "$SOURCE" ]; then
  ra_log "FATAL: Failed to extract/decode CN1SS payload from any source"
  # Keep partial for debugging
  RAW_B64_OUT="${SCREENSHOT_OUT}.raw.b64"
  {
    # Try to emit concatenated base64 from whichever had chunks (priority logcat, then XML, then exec)
    if [ "${LOGCAT_CHUNKS:-0}" -gt 0 ]; then extract_cn1ss_base64 "$LOGCAT_FILE"; fi
    if [ "${XML_CHUNKS_TOTAL:-0}" -gt 0 ] && [ "${LOGCAT_CHUNKS:-0}" -eq 0 ]; then
      # concatenate all XMLs
      for x in "${XMLS[@]}"; do
        if [ "$(count_chunks "$x")" -gt 0 ]; then extract_cn1ss_base64 "$x"; fi
      done
    fi
    if [ -n "${TEST_EXEC_LOG:-}" ] && [ "${EXECLOG_CHUNKS:-0}" -gt 0 ] && [ "${LOGCAT_CHUNKS:-0}" -eq 0 ] && [ "${XML_CHUNKS_TOTAL:-0}" -eq 0 ]; then
      extract_cn1ss_base64 "$TEST_EXEC_LOG"
    fi
  } > "$RAW_B64_OUT" 2>/dev/null || true
  if [ -s "$RAW_B64_OUT" ]; then
    head -c 64 "$RAW_B64_OUT" | sed 's/^/[CN1SS-B64-HEAD] /'
    ra_log "Partial base64 saved at: $RAW_B64_OUT"
  fi
  # Emit contextual INFO lines
  grep -n 'CN1SS:INFO' "$LOGCAT_FILE" 2>/dev/null || true
  exit 12
fi

# Size & signature check (belt & suspenders)
if ! verify_png "$SCREENSHOT_OUT"; then
  ra_log "STAGE:BAD_PNG_SIGNATURE -> Not a PNG"
  file "$SCREENSHOT_OUT" || true
  exit 14
fi

ra_log "SUCCESS -> screenshot saved (${SOURCE}), size: $(stat -c '%s' "$SCREENSHOT_OUT") bytes at $SCREENSHOT_OUT"

# Copy useful artifacts for GH Actions
cp -f "$LOGCAT_FILE" "$ARTIFACTS_DIR/$(basename "$LOGCAT_FILE")" 2>/dev/null || true
for x in "${XMLS[@]}"; do
  cp -f "$x" "$ARTIFACTS_DIR/$(basename "$x")" 2>/dev/null || true
done
[ -n "${TEST_EXEC_LOG:-}" ] && cp -f "$TEST_EXEC_LOG" "$ARTIFACTS_DIR/test-results.log" 2>/dev/null || true

exit 0
