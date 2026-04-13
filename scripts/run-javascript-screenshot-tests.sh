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

if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" ]; then
  rj_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" >&2
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

LOG_CHUNKS="$(cn1ss_count_chunks "$LOG_FILE")"
LOG_CHUNKS="${LOG_CHUNKS//[^0-9]/}"
: "${LOG_CHUNKS:=0}"
rj_log "Chunk counts -> browser log: ${LOG_CHUNKS}"

if [ "${LOG_CHUNKS:-0}" = "0" ]; then
  rj_log "STAGE:MARKERS_NOT_FOUND -> browser log did not include CN1SS chunks"
  rj_log "---- CN1SS lines from log ----"
  (grep "CN1SS:" "$LOG_FILE" || true) | sed 's/^/[CN1SS] /'
  exit 12
fi

TEST_NAMES_RAW="$(cn1ss_list_tests "$LOG_FILE" 2>/dev/null | awk 'NF' | sort -u || true)"
declare -a TEST_NAMES=()
if [ -n "$TEST_NAMES_RAW" ]; then
  while IFS= read -r name; do
    [ -n "$name" ] || continue
    TEST_NAMES+=("$name")
  done <<< "$TEST_NAMES_RAW"
else
  TEST_NAMES+=("default")
fi
if [ "${#TEST_NAMES[@]}" -gt 1 ]; then
  FILTERED_TEST_NAMES=()
  for test in "${TEST_NAMES[@]}"; do
    if [ "$test" = "default" ]; then
      continue
    fi
    FILTERED_TEST_NAMES+=("$test")
  done
  if [ "${#FILTERED_TEST_NAMES[@]}" -gt 0 ]; then
    TEST_NAMES=("${FILTERED_TEST_NAMES[@]}")
  fi
fi
rj_log "Detected CN1SS test streams: ${TEST_NAMES[*]}"

declare -a FAILED_TESTS=()
declare -a COMPARE_ENTRIES=()

if ! cn1ss_print_log "$LOG_FILE"; then
  # Keep processing screenshots even when suite logs include CN1SS error markers.
  # The report/comment output is needed specifically for these failing runs.
  rj_log "CN1SS log check reported test errors; continuing with screenshot decode/report generation"
fi

for test in "${TEST_NAMES[@]}"; do
  dest="$SCREENSHOT_RAW_DIR/${test}.png"
  if source_label="$(cn1ss_decode_test_png "$test" "$dest" "${CN1SS_SOURCES[@]}")"; then
    rj_log "Decoded screenshot for '$test' (source=${source_label}, size: $(cn1ss_file_size "$dest") bytes)"
    COMPARE_ENTRIES+=("${test}=${dest}")
    preview_dest="$SCREENSHOT_PREVIEW_DIR/${test}.jpg"
    if preview_source="$(cn1ss_decode_test_preview "$test" "$preview_dest" "${CN1SS_SOURCES[@]}")"; then
      rj_log "Decoded preview for '$test' (source=${preview_source}, size: $(cn1ss_file_size "$preview_dest") bytes)"
    else
      rm -f "$preview_dest" 2>/dev/null || true
    fi
  else
    if { [ "$test" = "default" ] || [ "$test" = "bootstrap_placeholder" ]; } && [ "${#TEST_NAMES[@]}" -gt 1 ]; then
      rj_log "WARN: Skipping decode failure for synthetic '$test' stream because named test streams were detected"
      continue
    fi
    rj_log "ERROR: Failed to extract/decode CN1SS payload for test '$test'"
    FAILED_TESTS+=("$test")
    RAW_B64_OUT="$SCREENSHOT_TMP_DIR/${test}.raw.b64"
    if cn1ss_extract_base64 "$LOG_FILE" "$test" > "$RAW_B64_OUT" 2>/dev/null; then
      if [ -s "$RAW_B64_OUT" ]; then
        head -c 64 "$RAW_B64_OUT" | sed 's/^/[CN1SS-B64-HEAD] /'
        rj_log "Partial base64 saved at: $RAW_B64_OUT"
      fi
    fi
  fi
done

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

exit $comment_rc
