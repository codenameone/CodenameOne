#!/usr/bin/env bash
# Run instrumentation tests and reconstruct screenshot emitted as chunked Base64 (NO ADB)
set -euo pipefail

ra_log() { echo "[run-android-instrumentation-tests] $1"; }

# ---- Helpers ---------------------------------------------------------------

ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

# CN1SS helpers are implemented in Java for easier maintenance
CN1SS_MAIN_CLASS="Cn1ssChunkTools"
POST_COMMENT_CLASS="PostPrComment"
PROCESS_SCREENSHOTS_CLASS="ProcessScreenshots"
RENDER_SCREENSHOT_REPORT_CLASS="RenderScreenshotReport"

# ---- Args & environment ----------------------------------------------------

if [ $# -lt 1 ]; then
  ra_log "Usage: $0 <gradle_project_dir>" >&2
  exit 2
fi
GRADLE_PROJECT_DIR="$1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/android/tests"
if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" ]; then
  ra_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" >&2
  exit 3
fi

source "$SCRIPT_DIR/lib/cn1ss.sh"
cn1ss_log() { ra_log "$1"; }

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
ensure_dir "$ARTIFACTS_DIR"
TEST_LOG="$ARTIFACTS_DIR/connectedAndroidTest.log"
SCREENSHOT_REF_DIR="$SCRIPT_DIR/android/screenshots"
SCREENSHOT_TMP_DIR="$(mktemp -d "${TMPDIR}/cn1ss-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1ss-tmp")"
ensure_dir "$SCREENSHOT_TMP_DIR"
SCREENSHOT_PREVIEW_DIR="$SCREENSHOT_TMP_DIR/previews"

ra_log "Loading workspace environment from $ENV_FILE"
[ -f "$ENV_FILE" ] || { ra_log "Missing env file: $ENV_FILE"; exit 3; }
# shellcheck disable=SC1090
source "$ENV_FILE"

if [ -z "${JAVA17_HOME:-}" ]; then
  ra_log "JAVA17_HOME not set in workspace environment" >&2
  exit 3
fi

JAVA17_BIN="$JAVA17_HOME/bin/java"
if [ ! -x "$JAVA17_BIN" ]; then
  ra_log "JDK 17 java binary missing at $JAVA17_BIN" >&2
  exit 3
fi

cn1ss_setup "$JAVA17_BIN" "$CN1SS_HELPER_SOURCE_DIR"

[ -d "$GRADLE_PROJECT_DIR" ] || { ra_log "Gradle project directory not found: $GRADLE_PROJECT_DIR"; exit 4; }
[ -x "$GRADLE_PROJECT_DIR/gradlew" ] || chmod +x "$GRADLE_PROJECT_DIR/gradlew"

# ---- Run tests -------------------------------------------------------------

set -o pipefail
ra_log "Running instrumentation tests (stdout -> $TEST_LOG; stderr -> terminal)"
(
  java scripts/java/BuildAndRun.java android | tee "$TEST_LOG"
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
mapfile -t LOGCAT_FILES < <(
  find "$RESULTS_ROOT" -type f -name 'logcat-*.txt' -print 2>/dev/null
) || LOGCAT_FILES=()

# execution log (use first if present)
TEST_EXEC_LOG="$(find "$RESULTS_ROOT" -type f -path '*/testlog/test-results.log' -print -quit 2>/dev/null || true)"
[ -n "${TEST_EXEC_LOG:-}" ] || TEST_EXEC_LOG=""

declare -a CN1SS_SOURCES=()
for x in "${XMLS[@]}"; do
  CN1SS_SOURCES+=("XML:$x")
done
for logcat in "${LOGCAT_FILES[@]}"; do
  CN1SS_SOURCES+=("LOGCAT:$logcat")
done
if [ -n "${TEST_EXEC_LOG:-}" ]; then
  CN1SS_SOURCES+=("EXEC:$TEST_EXEC_LOG")
fi

if [ "${#XMLS[@]}" -gt 0 ]; then
  ra_log "Found ${#XMLS[@]} test result file(s). First candidate: ${XMLS[0]}"
else
  ra_log "No test result XML files found under $RESULTS_ROOT"
fi

if [ "${#LOGCAT_FILES[@]}" -eq 0 ]; then
  ra_log "FATAL: No logcat-*.txt produced by connectedDebugAndroidTest (cannot extract CN1SS chunks)."
  exit 12
fi


# ---- Chunk accounting (diagnostics) ---------------------------------------

XML_CHUNKS_TOTAL=0
for x in "${XMLS[@]}"; do
  c="$(cn1ss_count_chunks "$x")"; c="${c//[^0-9]/}"; : "${c:=0}"
  XML_CHUNKS_TOTAL=$(( XML_CHUNKS_TOTAL + c ))
done
LOGCAT_CHUNKS=0
for logcat in "${LOGCAT_FILES[@]}"; do
  c="$(cn1ss_count_chunks "$logcat")"; c="${c//[^0-9]/}"; : "${c:=0}"
  LOGCAT_CHUNKS=$(( LOGCAT_CHUNKS + c ))
done
EXECLOG_CHUNKS="$(cn1ss_count_chunks "${TEST_EXEC_LOG:-}")"; EXECLOG_CHUNKS="${EXECLOG_CHUNKS//[^0-9]/}"; : "${EXECLOG_CHUNKS:=0}"

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

# ---- Identify CN1SS test streams -----------------------------------------

declare -A TEST_NAME_SET=()

if [ "${#XMLS[@]}" -gt 0 ]; then
  for x in "${XMLS[@]}"; do
    while IFS= read -r name; do
      [ -n "$name" ] || continue
      TEST_NAME_SET["$name"]=1
    done < <(cn1ss_list_tests "$x" 2>/dev/null || true)
  done
fi

for logcat in "${LOGCAT_FILES[@]}"; do
  [ -s "$logcat" ] || continue
  while IFS= read -r name; do
    [ -n "$name" ] || continue
    TEST_NAME_SET["$name"]=1
  done < <(cn1ss_list_tests "$logcat" 2>/dev/null || true)
done

if [ -n "${TEST_EXEC_LOG:-}" ] && [ -s "$TEST_EXEC_LOG" ]; then
  while IFS= read -r name; do
    [ -n "$name" ] || continue
    TEST_NAME_SET["$name"]=1
  done < <(cn1ss_list_tests "$TEST_EXEC_LOG" 2>/dev/null || true)
fi

if [ "${#TEST_NAME_SET[@]}" -eq 0 ] && { [ "${LOGCAT_CHUNKS:-0}" -gt 0 ] || [ "${XML_CHUNKS_TOTAL:-0}" -gt 0 ] || [ "${EXECLOG_CHUNKS:-0}" -gt 0 ]; }; then
  TEST_NAME_SET["default"]=1
fi

if [ "${#TEST_NAME_SET[@]}" -eq 0 ]; then
  ra_log "FATAL: Could not determine any CN1SS test streams"
  exit 12
fi

declare -a TEST_NAMES=()
for name in "${!TEST_NAME_SET[@]}"; do
  TEST_NAMES+=("$name")
done
IFS=$'\n' TEST_NAMES=($(printf '%s\n' "${TEST_NAMES[@]}" | sort))
unset IFS
ra_log "Detected CN1SS test streams: ${TEST_NAMES[*]}"

declare -A TEST_OUTPUTS=()
declare -A TEST_SOURCES=()
declare -A PREVIEW_OUTPUTS=()

ensure_dir "$SCREENSHOT_PREVIEW_DIR"

for test in "${TEST_NAMES[@]}"; do
  dest="$SCREENSHOT_TMP_DIR/${test}.png"
  if source_label="$(cn1ss_decode_test_png "$test" "$dest" "${CN1SS_SOURCES[@]}")"; then
    TEST_OUTPUTS["$test"]="$dest"
    TEST_SOURCES["$test"]="$source_label"
    ra_log "Decoded screenshot for '$test' (source=${source_label}, size: $(cn1ss_file_size "$dest") bytes)"
    preview_dest="$SCREENSHOT_PREVIEW_DIR/${test}.jpg"
    if preview_source="$(cn1ss_decode_test_preview "$test" "$preview_dest" "${CN1SS_SOURCES[@]}")"; then
      PREVIEW_OUTPUTS["$test"]="$preview_dest"
      ra_log "Decoded preview for '$test' (source=${preview_source}, size: $(cn1ss_file_size "$preview_dest") bytes)"
    else
      rm -f "$preview_dest" 2>/dev/null || true
    fi
  else
    ra_log "FATAL: Failed to extract/decode CN1SS payload for test '$test'"
    RAW_B64_OUT="$SCREENSHOT_TMP_DIR/${test}.raw.b64"
    {
      local count
      for logcat in "${LOGCAT_FILES[@]}"; do
        [ -s "$logcat" ] || continue
        count="$(cn1ss_count_chunks "$logcat" "$test")"; count="${count//[^0-9]/}"; : "${count:=0}"
        if [ "$count" -gt 0 ]; then cn1ss_extract_base64 "$logcat" "$test"; fi
      done
      if [ "${#XMLS[@]}" -gt 0 ]; then
        for x in "${XMLS[@]}"; do
          count="$(cn1ss_count_chunks "$x" "$test")"; count="${count//[^0-9]/}"; : "${count:=0}"
          if [ "$count" -gt 0 ]; then cn1ss_extract_base64 "$x" "$test"; fi
        done
      fi
      if [ -n "${TEST_EXEC_LOG:-}" ] && [ -s "$TEST_EXEC_LOG" ]; then
        count="$(cn1ss_count_chunks "$TEST_EXEC_LOG" "$test")"; count="${count//[^0-9]/}"; : "${count:=0}"
        if [ "$count" -gt 0 ]; then cn1ss_extract_base64 "$TEST_EXEC_LOG" "$test"; fi
      fi
    } > "$RAW_B64_OUT" 2>/dev/null || true
    if [ -s "$RAW_B64_OUT" ]; then
      head -c 64 "$RAW_B64_OUT" | sed 's/^/[CN1SS-B64-HEAD] /'
      ra_log "Partial base64 saved at: $RAW_B64_OUT"
    fi
    exit 12
  fi
done

# ---- Compare against stored references ------------------------------------

COMPARE_ARGS=()
for test in "${TEST_NAMES[@]}"; do
  dest="${TEST_OUTPUTS[$test]:-}"
  [ -n "$dest" ] || continue
  COMPARE_ARGS+=("--actual" "${test}=${dest}")
done

COMPARE_JSON="$SCREENSHOT_TMP_DIR/screenshot-compare.json"
export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"
ra_log "STAGE:COMPARE -> Evaluating screenshots against stored references"
if ! cn1ss_java_run "$PROCESS_SCREENSHOTS_CLASS" \
  --reference-dir "$SCREENSHOT_REF_DIR" \
  --emit-base64 \
  --preview-dir "$SCREENSHOT_PREVIEW_DIR" \
  "${COMPARE_ARGS[@]}" > "$COMPARE_JSON"; then
  ra_log "FATAL: Screenshot comparison helper failed"
  exit 13
fi

SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"

ra_log "STAGE:COMMENT_BUILD -> Rendering summary and PR comment markdown"
if ! cn1ss_java_run "$RENDER_SCREENSHOT_REPORT_CLASS" \
  --compare-json "$COMPARE_JSON" \
  --comment-out "$COMMENT_FILE" \
  --summary-out "$SUMMARY_FILE"; then
  ra_log "FATAL: Failed to render screenshot summary/comment"
  exit 14
fi

if [ -s "$SUMMARY_FILE" ]; then
  ra_log "  -> Wrote summary entries to $SUMMARY_FILE ($(wc -l < "$SUMMARY_FILE" 2>/dev/null || echo 0) line(s))"
else
  ra_log "  -> No summary entries generated (all screenshots matched stored baselines)"
fi

if [ -s "$COMMENT_FILE" ]; then
  ra_log "  -> Prepared PR comment payload at $COMMENT_FILE (bytes=$(wc -c < "$COMMENT_FILE" 2>/dev/null || echo 0))"
else
  ra_log "  -> No PR comment content produced"
fi

if [ -s "$SUMMARY_FILE" ]; then
  while IFS='|' read -r status test message copy_flag path preview_note; do
    [ -n "${test:-}" ] || continue
    ra_log "Test '${test}': ${message}"
    if [ "$copy_flag" = "1" ] && [ -n "${path:-}" ] && [ -f "$path" ]; then
      cp -f "$path" "$ARTIFACTS_DIR/${test}.png" 2>/dev/null || true
      ra_log "  -> Stored PNG artifact copy at $ARTIFACTS_DIR/${test}.png"
    fi
    if [ "$status" = "equal" ] && [ -n "${path:-}" ]; then
      rm -f "$path" 2>/dev/null || true
    fi
    if [ -n "${preview_note:-}" ]; then
      ra_log "  Preview note: ${preview_note}"
    fi
  done < "$SUMMARY_FILE"
fi

cp -f "$COMPARE_JSON" "$ARTIFACTS_DIR/screenshot-compare.json" 2>/dev/null || true
if [ -s "$COMMENT_FILE" ]; then
  cp -f "$COMMENT_FILE" "$ARTIFACTS_DIR/screenshot-comment.md" 2>/dev/null || true
fi

ra_log "STAGE:COMMENT_POST -> Submitting PR feedback"
comment_rc=0
if ! cn1ss_post_pr_comment "$COMMENT_FILE" "$SCREENSHOT_PREVIEW_DIR"; then
  comment_rc=$?
fi

# Copy useful artifacts for GH Actions
for logcat in "${LOGCAT_FILES[@]}"; do
  cp -f "$logcat" "$ARTIFACTS_DIR/$(basename "$logcat")" 2>/dev/null || true
done
for x in "${XMLS[@]}"; do
  cp -f "$x" "$ARTIFACTS_DIR/$(basename "$x")" 2>/dev/null || true
done
[ -n "${TEST_EXEC_LOG:-}" ] && cp -f "$TEST_EXEC_LOG" "$ARTIFACTS_DIR/test-results.log" 2>/dev/null || true

exit $comment_rc
