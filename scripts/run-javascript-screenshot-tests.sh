#!/usr/bin/env bash
# Decode and compare CN1SS screenshots for the JavaScript port.
set -euo pipefail

rj_log() { echo "[run-javascript-screenshot-tests] $1"; }
ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

usage() {
  cat <<'EOF' >&2
Usage: run-javascript-screenshot-tests.sh <device-runner.log> [reference_dir]

This script adapts the existing CN1SS screenshot decoding/comparison flow for
the JavaScript port. It expects a browser/device-runner log that already
contains CN1SS chunk output and reuses the same PNG/preview/report helpers used
by the iOS and Android screenshot pipelines.
EOF
}

if [ $# -lt 1 ]; then
  usage
  exit 2
fi

LOG_FILE="$1"
if [ ! -r "$LOG_FILE" ]; then
  rj_log "Log file not found or not readable: $LOG_FILE" >&2
  exit 3
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
REFERENCE_DIR="${2:-$SCRIPT_DIR/javascript/screenshots}"

cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/common/java"
source "$SCRIPT_DIR/lib/cn1ss.sh"

if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/Cn1ssScreenshotServer.java" ]; then
  rj_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/Cn1ssScreenshotServer.java" >&2
  exit 3
fi
cn1ss_log() { rj_log "$1"; }

JAVA_BIN=""
if [ -n "${JAVA17_HOME:-}" ] && [ -x "${JAVA17_HOME}/bin/java" ]; then
  JAVA_BIN="${JAVA17_HOME}/bin/java"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA_BIN="$(command -v java)"
fi

if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ]; then
  rj_log "Unable to locate a usable Java runtime" >&2
  exit 3
fi

cn1ss_setup "$JAVA_BIN" "$CN1SS_HELPER_SOURCE_DIR"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts/javascript-screenshot-report}"
ensure_dir "$ARTIFACTS_DIR"

TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"
SCREENSHOT_TMP_DIR="$(mktemp -d "${TMPDIR}/cn1ss-js-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1ss-js-tmp")"
SCREENSHOT_RAW_DIR="$SCREENSHOT_TMP_DIR/raw"
SCREENSHOT_PREVIEW_DIR="$SCREENSHOT_TMP_DIR/previews"
ensure_dir "$SCREENSHOT_RAW_DIR"
ensure_dir "$SCREENSHOT_PREVIEW_DIR"
ensure_dir "$REFERENCE_DIR"

declare -a CN1SS_SOURCES=("BROWSER_LOG:$LOG_FILE")

# WebSocket-first: run-javascript-browser-tests.sh starts a host-side WS server
# and the browser app sends each screenshot as <test>.png into $CN1SS_WS_DIR.
# The WebSocket transport frames every screenshot with an explicit per-test
# META header, so it does not suffer the base64-over-console demux problems
# (interleaved/misassigned chunks) the JS port hits under load. When WS
# delivered images we use them directly and skip the log decode below.
declare -a COMPARE_ENTRIES=()
declare -a FAILED_TESTS=()
declare -a TEST_NAMES=()
WS_DELIVERED=0
if [ -n "${CN1SS_WS_DIR:-}" ] && [ -d "$CN1SS_WS_DIR" ]; then
  for ws_png in "$CN1SS_WS_DIR"/*.png; do
    [ -s "$ws_png" ] || continue
    ws_test="$(basename "$ws_png" .png)"
    ws_dest="$SCREENSHOT_RAW_DIR/${ws_test}.png"
    cp -f "$ws_png" "$ws_dest" 2>/dev/null || continue
    COMPARE_ENTRIES+=("${ws_test}=${ws_dest}")
    TEST_NAMES+=("$ws_test")
    WS_DELIVERED=$(( WS_DELIVERED + 1 ))
  done
fi

if [ "$WS_DELIVERED" -gt 0 ]; then
  rj_log "WebSocket transport delivered ${WS_DELIVERED} screenshot(s)"
else
  # WebSocket is the only transport. Zero delivered screenshots means either
  # the suite finished with nothing to capture (a clean no-screenshot run) or
  # the harness never ran end-to-end (markers absent). Distinguish the two via
  # the SUITE:FINISHED marker; there is no base64-over-console decode any more.
  if grep -q "CN1SS:SUITE:FINISHED" "$LOG_FILE"; then
    cp -f "$LOG_FILE" "$ARTIFACTS_DIR/javascript-device-runner.log" 2>/dev/null || true
    # "Zero delivered" is only a legitimate no-screenshot run when nothing is
    # expected. If the reference set holds goldens, reaching SUITE:FINISHED with
    # an empty delivery is the worst kind of count regression -- the whole suite
    # dropped -- and must fail, never exit 0. This mirrors the reference-anchored
    # floor in cn1ss_process_and_report (cn1ss.sh) for the path that never
    # reaches it. CN1SS_SKIP_COUNT_CHECK=1 bypasses (reserved for seeding).
    expected_goldens=$(cn1ss_count_reference "$REFERENCE_DIR")
    expected_goldens="${expected_goldens//[^0-9]/}"; : "${expected_goldens:=0}"
    allowed_missing="${CN1SS_ALLOWED_MISSING:-0}"
    allowed_missing="${allowed_missing//[^0-9]/}"; : "${allowed_missing:=0}"
    min_floor="${CN1SS_MIN_SCREENSHOTS:-0}"
    min_floor="${min_floor//[^0-9]/}"; : "${min_floor:=0}"
    if [ "$min_floor" -gt "$expected_goldens" ]; then expected_goldens="$min_floor"; fi
    if [ "${CN1SS_SKIP_COUNT_CHECK:-0}" = "1" ]; then
      rj_log "WARNING: CN1SS_SKIP_COUNT_CHECK=1 -- accepting zero-screenshot run despite $expected_goldens expected golden(s)."
      exit 0
    fi
    if [ "$expected_goldens" -le "$allowed_missing" ]; then
      rj_log "No screenshots delivered over WebSocket but reached SUITE:FINISHED; $expected_goldens expected (<= $allowed_missing tolerated) -- treating as a no-screenshot run"
      exit 0
    fi
    rj_log "FATAL: reached SUITE:FINISHED but delivered 0 of $expected_goldens expected screenshot(s) ($allowed_missing tolerated) -- the suite dropped every screenshot (hang/crash)."
    exit 17
  fi
  rj_log "STAGE:MARKERS_NOT_FOUND -> no WebSocket screenshots and no SUITE:FINISHED in browser log"
  rj_log "---- CN1SS lines from log ----"
  (grep "CN1SS:" "$LOG_FILE" || true) | sed 's/^/[CN1SS] /'
  exit 12
fi

decoded_count="${#COMPARE_ENTRIES[@]}"
meaningful_decoded_count=0
for entry in "${COMPARE_ENTRIES[@]}"; do
  test_name="${entry%%=*}"
  if [ "$test_name" = "bootstrap_placeholder" ] || [ "$test_name" = "default" ]; then
    continue
  fi
  meaningful_decoded_count=$((meaningful_decoded_count + 1))
done

COMPARE_JSON="$SCREENSHOT_TMP_DIR/screenshot-compare.json"
SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"
export CN1SS_PORT_ID="${CN1SS_PORT_ID:-javascript}"
export CN1SS_SUITE_LOG="$LOG_FILE"

export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"
export CN1SS_COMMENT_MARKER="<!-- CN1SS_JAVASCRIPT_COMMENT -->"
export CN1SS_COMMENT_LOG_PREFIX="[run-javascript-screenshot-tests]"
export CN1SS_PREVIEW_SUBDIR="javascript"
export CN1SS_SUCCESS_MESSAGE="✅ JavaScript-port screenshot tests passed."

cn1ss_process_and_report \
  "JavaScript port screenshot updates" \
  "$COMPARE_JSON" \
  "$SUMMARY_FILE" \
  "$COMMENT_FILE" \
  "$REFERENCE_DIR" \
  "$SCREENSHOT_PREVIEW_DIR" \
  "$ARTIFACTS_DIR" \
  "${COMPARE_ENTRIES[@]}"
comment_rc=$?

cp -f "$LOG_FILE" "$ARTIFACTS_DIR/javascript-device-runner.log" 2>/dev/null || true

if [ "${#FAILED_TESTS[@]}" -gt 0 ]; then
  if [ "$meaningful_decoded_count" -gt 0 ] && [ "${#FAILED_TESTS[@]}" -lt "$meaningful_decoded_count" ]; then
    rj_log "WARN: CN1SS decode failures for tests: ${FAILED_TESTS[*]} (non-fatal: $meaningful_decoded_count tests succeeded)"
  else
    rj_log "ERROR: CN1SS decode failures for tests: ${FAILED_TESTS[*]}"
    comment_rc=12
  fi
fi

if [ "$meaningful_decoded_count" -eq 0 ]; then
  rj_log "ERROR: No meaningful screenshots decoded (only default/bootstrap streams were present)"
  comment_rc=12
fi

# Sanity gate. A capture bug (frozen / wrong surface) makes many DIFFERENT tests
# deliver the IDENTICAL image -- that is never a legitimate golden change, so fail
# the job on a duplicate-image cluster. Heavy per-test mismatch is reported (so it
# is visible in the log) but NOT failed here, because a real rendering change is
# reviewed and reseeded via the screenshot comment. This guards against reading a
# capture bug as "green" (e.g. mistaking the constant tolerance threshold in
# screenshot-compare.json for the measured mismatch_percent).
SANITY_SCRIPT="$SCRIPT_DIR/lint/jsport-screenshot-sanity.py"
if [ -f "$SANITY_SCRIPT" ] && [ -f "$COMPARE_JSON" ] && command -v python3 >/dev/null 2>&1; then
  if ! python3 "$SANITY_SCRIPT" "$COMPARE_JSON" "$SCREENSHOT_RAW_DIR" --max-wrong 9999 --dup-cluster 5; then
    rj_log "ERROR: screenshot sanity check failed -- a duplicate-image cluster means the capture grabbed the wrong/stale surface, not a per-test render."
    comment_rc=12
  fi
fi

exit $comment_rc
