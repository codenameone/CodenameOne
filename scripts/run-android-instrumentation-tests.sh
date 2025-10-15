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
  local test="${2:-}"
  if [ -z "$CN1SS_TOOL" ] || [ ! -x "$CN1SS_TOOL" ]; then
    echo 0
    return
  fi
  if [ -z "$f" ] || [ ! -r "$f" ]; then
    echo 0
    return
  fi
  local args=("count" "$f")
  if [ -n "$test" ]; then
    args+=("--test" "$test")
  fi
  python3 "$CN1SS_TOOL" "${args[@]}" 2>/dev/null || echo 0
}

extract_cn1ss_base64() {
  local f="${1:-}"
  local test="${2:-}"
  if [ -z "$CN1SS_TOOL" ] || [ ! -x "$CN1SS_TOOL" ]; then
    return 1
  fi
  if [ -z "$f" ] || [ ! -r "$f" ]; then
    return 1
  fi
  local args=("extract" "$f")
  if [ -n "$test" ]; then
    args+=("--test" "$test")
  fi
  python3 "$CN1SS_TOOL" "${args[@]}"
}

decode_cn1ss_png() {
  local f="${1:-}"
  local test="${2:-}"
  if [ -z "$CN1SS_TOOL" ] || [ ! -x "$CN1SS_TOOL" ]; then
    return 1
  fi
  if [ -z "$f" ] || [ ! -r "$f" ]; then
    return 1
  fi
  local args=("extract" "$f" "--decode")
  if [ -n "$test" ]; then
    args+=("--test" "$test")
  fi
  python3 "$CN1SS_TOOL" "${args[@]}"
}

list_cn1ss_tests() {
  local f="${1:-}"
  if [ -z "$CN1SS_TOOL" ] || [ ! -x "$CN1SS_TOOL" ]; then
    return 1
  fi
  if [ -z "$f" ] || [ ! -r "$f" ]; then
    return 1
  fi
  python3 "$CN1SS_TOOL" tests "$f"
}

post_pr_comment() {
  local body_file="${1:-}"
  if [ -z "$body_file" ] || [ ! -s "$body_file" ]; then
    return 0
  fi
  if [ -z "${GITHUB_TOKEN:-}" ]; then
    ra_log "PR comment skipped (GITHUB_TOKEN not set)"
    return 0
  fi
  if [ -z "${GITHUB_EVENT_PATH:-}" ] || [ ! -f "$GITHUB_EVENT_PATH" ]; then
    ra_log "PR comment skipped (GITHUB_EVENT_PATH unavailable)"
    return 0
  fi
  python3 - "$body_file" <<'PY'
import json
import os
import pathlib
import sys
from urllib.error import HTTPError
from urllib.request import Request, urlopen

body_path = pathlib.Path(sys.argv[1])
body = body_path.read_text(encoding="utf-8").strip()
if not body:
    sys.exit(0)

event_path = os.environ.get("GITHUB_EVENT_PATH")
repo = os.environ.get("GITHUB_REPOSITORY")
token = os.environ.get("GITHUB_TOKEN")
if not event_path or not repo or not token:
    sys.exit(0)

with open(event_path, "r", encoding="utf-8") as fh:
    event = json.load(fh)

pr_number = None
if "pull_request" in event:
    pr_number = event["pull_request"].get("number")
elif event.get("issue") and event["issue"].get("pull_request"):
    pr_number = event["issue"].get("number")

if not pr_number:
    sys.exit(0)

payload = json.dumps({"body": body}).encode("utf-8")
req = Request(
    f"https://api.github.com/repos/{repo}/issues/{pr_number}/comments",
    data=payload,
    headers={
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github+json",
        "Content-Type": "application/json",
    },
    method="POST",
)

try:
    with urlopen(req) as resp:
        resp.read()
except HTTPError as exc:  # pragma: no cover - diagnostics only
    print(f"Failed to post PR comment: {exc}", file=sys.stderr)
    sys.exit(0)
PY
  if [ $? -eq 0 ]; then
    ra_log "Posted screenshot comparison comment to PR"
  fi
  return 0
}

decode_test_png() {
  local test_name="${1:-}"
  local dest="${2:-}"
  local source=""
  local count="0"

  if [ "${#XMLS[@]}" -gt 0 ]; then
    for x in "${XMLS[@]}"; do
      count="$(count_chunks "$x" "$test_name")"; count="${count//[^0-9]/}"; : "${count:=0}"
      [ "$count" -gt 0 ] || continue
      ra_log "Reassembling test '$test_name' from XML: $x (chunks=$count)"
      if decode_cn1ss_png "$x" "$test_name" > "$dest" 2>/dev/null; then
        if verify_png "$dest"; then source="XML"; break; fi
      fi
    done
  fi

  if [ -z "$source" ] && [ -s "${LOGCAT_FILE:-}" ]; then
    count="$(count_chunks "$LOGCAT_FILE" "$test_name")"; count="${count//[^0-9]/}"; : "${count:=0}"
    if [ "$count" -gt 0 ]; then
      ra_log "Reassembling test '$test_name' from logcat: $LOGCAT_FILE (chunks=$count)"
      if decode_cn1ss_png "$LOGCAT_FILE" "$test_name" > "$dest" 2>/dev/null; then
        if verify_png "$dest"; then source="LOGCAT"; fi
      fi
    fi
  fi

  if [ -z "$source" ] && [ -n "${TEST_EXEC_LOG:-}" ] && [ -s "$TEST_EXEC_LOG" ]; then
    count="$(count_chunks "$TEST_EXEC_LOG" "$test_name")"; count="${count//[^0-9]/}"; : "${count:=0}"
    if [ "$count" -gt 0 ]; then
      ra_log "Reassembling test '$test_name' from test-results.log: $TEST_EXEC_LOG (chunks=$count)"
      if decode_cn1ss_png "$TEST_EXEC_LOG" "$test_name" > "$dest" 2>/dev/null; then
        if verify_png "$dest"; then source="EXECLOG"; fi
      fi
    fi
  fi

  if [ -n "$source" ]; then
    printf '%s' "$source"
    return 0
  fi

  return 1
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
SCREENSHOT_REF_DIR="$SCRIPT_DIR/android/screenshots"
SCREENSHOT_TMP_DIR="$(mktemp -d "${TMPDIR}/cn1ss-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1ss-tmp")"
ensure_dir "$SCREENSHOT_TMP_DIR"

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

# ---- Identify CN1SS test streams -----------------------------------------

declare -A TEST_NAME_SET=()

if [ "${#XMLS[@]}" -gt 0 ]; then
  for x in "${XMLS[@]}"; do
    while IFS= read -r name; do
      [ -n "$name" ] || continue
      TEST_NAME_SET["$name"]=1
    done < <(list_cn1ss_tests "$x" 2>/dev/null || true)
  done
fi

if [ -s "${LOGCAT_FILE:-}" ]; then
  while IFS= read -r name; do
    [ -n "$name" ] || continue
    TEST_NAME_SET["$name"]=1
  done < <(list_cn1ss_tests "$LOGCAT_FILE" 2>/dev/null || true)
fi

if [ -n "${TEST_EXEC_LOG:-}" ] && [ -s "$TEST_EXEC_LOG" ]; then
  while IFS= read -r name; do
    [ -n "$name" ] || continue
    TEST_NAME_SET["$name"]=1
  done < <(list_cn1ss_tests "$TEST_EXEC_LOG" 2>/dev/null || true)
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

for test in "${TEST_NAMES[@]}"; do
  dest="$SCREENSHOT_TMP_DIR/${test}.png"
  if source_label="$(decode_test_png "$test" "$dest")"; then
    TEST_OUTPUTS["$test"]="$dest"
    TEST_SOURCES["$test"]="$source_label"
    ra_log "Decoded screenshot for '$test' (source=${source_label}, size: $(stat -c '%s' "$dest") bytes)"
  else
    ra_log "FATAL: Failed to extract/decode CN1SS payload for test '$test'"
    RAW_B64_OUT="$SCREENSHOT_TMP_DIR/${test}.raw.b64"
    {
      local count
      count="$(count_chunks "$LOGCAT_FILE" "$test")"; count="${count//[^0-9]/}"; : "${count:=0}"
      if [ "$count" -gt 0 ]; then extract_cn1ss_base64 "$LOGCAT_FILE" "$test"; fi
      if [ "${#XMLS[@]}" -gt 0 ]; then
        for x in "${XMLS[@]}"; do
          count="$(count_chunks "$x" "$test")"; count="${count//[^0-9]/}"; : "${count:=0}"
          if [ "$count" -gt 0 ]; then extract_cn1ss_base64 "$x" "$test"; fi
        done
      fi
      if [ -n "${TEST_EXEC_LOG:-}" ] && [ -s "$TEST_EXEC_LOG" ]; then
        count="$(count_chunks "$TEST_EXEC_LOG" "$test")"; count="${count//[^0-9]/}"; : "${count:=0}"
        if [ "$count" -gt 0 ]; then extract_cn1ss_base64 "$TEST_EXEC_LOG" "$test"; fi
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
python3 "$SCRIPT_DIR/android/tests/process_screenshots.py" \
  --reference-dir "$SCREENSHOT_REF_DIR" \
  --emit-base64 \
  "${COMPARE_ARGS[@]}" > "$COMPARE_JSON"

SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"

python3 <<'PY' "$COMPARE_JSON" "$COMMENT_FILE" "$SUMMARY_FILE"
import json
import pathlib
import sys

compare_path = pathlib.Path(sys.argv[1])
comment_path = pathlib.Path(sys.argv[2])
summary_path = pathlib.Path(sys.argv[3])

data = json.loads(compare_path.read_text(encoding="utf-8"))
summary_lines = []
comment_entries = []

for result in data.get("results", []):
    test = result.get("test", "unknown")
    status = result.get("status", "unknown")
    expected_path = result.get("expected_path")
    actual_path = result.get("actual_path", "")
    details = result.get("details") or {}
    base64_data = result.get("base64")
    base64_omitted = result.get("base64_omitted")
    base64_length = result.get("base64_length")
    message = ""
    copy_flag = "0"

    if status == "equal":
        message = "Matches stored reference."
    elif status == "missing_expected":
        message = f"Reference screenshot missing at {expected_path}."
        copy_flag = "1"
        comment_entries.append({
            "test": test,
            "status": "missing reference",
            "message": message,
            "base64": base64_data,
            "base64_omitted": base64_omitted,
            "base64_length": base64_length,
            "artifact_name": f"{test}.png",
        })
    elif status == "different":
        dims = ""
        if details:
            dims = f" ({details.get('width')}x{details.get('height')} px, bit depth {details.get('bit_depth')})"
        message = f"Screenshot differs{dims}."
        copy_flag = "1"
        comment_entries.append({
            "test": test,
            "status": "updated screenshot",
            "message": message,
            "base64": base64_data,
            "base64_omitted": base64_omitted,
            "base64_length": base64_length,
            "artifact_name": f"{test}.png",
        })
    elif status == "error":
        message = f"Comparison error: {result.get('message', 'unknown error')}"
        copy_flag = "1"
        comment_entries.append({
            "test": test,
            "status": "comparison error",
            "message": message,
            "base64": None,
            "base64_omitted": base64_omitted,
            "base64_length": base64_length,
            "artifact_name": f"{test}.png",
        })
    elif status == "missing_actual":
        message = "Actual screenshot missing (test did not produce output)."
        copy_flag = "1"
        comment_entries.append({
            "test": test,
            "status": "missing actual screenshot",
            "message": message,
            "base64": None,
            "base64_omitted": base64_omitted,
            "base64_length": base64_length,
            "artifact_name": None,
        })
    else:
        message = f"Status: {status}."

    summary_lines.append("|".join([status, test, message, copy_flag, actual_path]))

summary_path.write_text("\n".join(summary_lines) + ("\n" if summary_lines else ""), encoding="utf-8")

if comment_entries:
    lines = ["### Android screenshot updates", ""]
    for entry in comment_entries:
        lines.append(f"- **{entry['test']}** — {entry['status']}. {entry['message']}")
        if entry.get("base64"):
            lines.append("")
            lines.append(f"  ![{entry['test']}](data:image/png;base64,{entry['base64']})")
            artifact_name = entry.get("artifact_name")
            if artifact_name:
                lines.append(f"  _Full-resolution PNG saved as `{artifact_name}` in workflow artifacts._")
                lines.append("")
        elif entry.get("base64_omitted") == "too_large":
            artifact_name = entry.get("artifact_name")
            size_note = ""
            if entry.get("base64_length"):
                size_note = f" (base64 length ≈ {entry['base64_length']:,} chars)"
            lines.append("")
            lines.append("  _Screenshot omitted from comment because the encoded payload exceeded GitHub's size limits" + size_note + "._")
            if artifact_name:
                lines.append(f"  _Full-resolution PNG saved as `{artifact_name}` in workflow artifacts._")
            lines.append("")
    comment_path.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
else:
    comment_path.write_text("", encoding="utf-8")
PY

if [ -s "$SUMMARY_FILE" ]; then
  while IFS='|' read -r status test message copy_flag path; do
    [ -n "${test:-}" ] || continue
    ra_log "Test '${test}': ${message}"
    if [ "$copy_flag" = "1" ] && [ -n "${path:-}" ] && [ -f "$path" ]; then
      cp -f "$path" "$ARTIFACTS_DIR/${test}.png" 2>/dev/null || true
    fi
    if [ "$status" = "equal" ] && [ -n "${path:-}" ]; then
      rm -f "$path" 2>/dev/null || true
    fi
  done < "$SUMMARY_FILE"
fi

cp -f "$COMPARE_JSON" "$ARTIFACTS_DIR/screenshot-compare.json" 2>/dev/null || true
if [ -s "$COMMENT_FILE" ]; then
  cp -f "$COMMENT_FILE" "$ARTIFACTS_DIR/screenshot-comment.md" 2>/dev/null || true
fi

post_pr_comment "$COMMENT_FILE"

# Copy useful artifacts for GH Actions
cp -f "$LOGCAT_FILE" "$ARTIFACTS_DIR/$(basename "$LOGCAT_FILE")" 2>/dev/null || true
for x in "${XMLS[@]}"; do
  cp -f "$x" "$ARTIFACTS_DIR/$(basename "$x")" 2>/dev/null || true
done
[ -n "${TEST_EXEC_LOG:-}" ] && cp -f "$TEST_EXEC_LOG" "$ARTIFACTS_DIR/test-results.log" 2>/dev/null || true

exit 0
