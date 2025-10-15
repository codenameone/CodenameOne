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
  local channel="${3:-}"
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
  if [ -n "$channel" ]; then
    args+=("--channel" "$channel")
  fi
  python3 "$CN1SS_TOOL" "${args[@]}" 2>/dev/null || echo 0
}

extract_cn1ss_base64() {
  local f="${1:-}"
  local test="${2:-}"
  local channel="${3:-}"
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
  if [ -n "$channel" ]; then
    args+=("--channel" "$channel")
  fi
  python3 "$CN1SS_TOOL" "${args[@]}"
}

decode_cn1ss_binary() {
  local f="${1:-}"
  local test="${2:-}"
  local channel="${3:-}"
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
  if [ -n "$channel" ]; then
    args+=("--channel" "$channel")
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
  local preview_dir="${2:-}"
  if [ -z "$body_file" ] || [ ! -s "$body_file" ]; then
    ra_log "Skipping PR comment post (no content)."
    return 0
  fi
  local comment_token="${GITHUB_TOKEN:-}"
  if [ -z "$comment_token" ] && [ -n "${GH_TOKEN:-}" ]; then
    comment_token="${GH_TOKEN}"
    ra_log "PR comment auth using GH_TOKEN fallback"
  fi
  if [ -n "$comment_token" ]; then
    ra_log "PR comment authentication token detected"
  fi
  if [ -z "$comment_token" ]; then
    ra_log "PR comment skipped (no GitHub token available)"
    return 0
  fi
  if [ -z "${GITHUB_EVENT_PATH:-}" ] || [ ! -f "$GITHUB_EVENT_PATH" ]; then
    ra_log "PR comment skipped (GITHUB_EVENT_PATH unavailable)"
    return 0
  fi
  local body_size
  body_size=$(wc -c < "$body_file" 2>/dev/null || echo 0)
  ra_log "Attempting to post PR comment (payload bytes=${body_size})"
  GITHUB_TOKEN="$comment_token" python3 - "$body_file" "$preview_dir" <<'PY'
import json
import os
import pathlib
import re
import sys
import urllib.parse
import uuid
from typing import Dict, Optional, Tuple
from urllib.error import HTTPError
from urllib.request import Request, urlopen

MARKER = "<!-- CN1SS_SCREENSHOT_COMMENT -->"


def load_event(path: str) -> Dict[str, object]:
    with open(path, "r", encoding="utf-8") as fh:
        return json.load(fh)


def find_pr_number(event: Dict[str, object]) -> Optional[int]:
    if "pull_request" in event:
        return event["pull_request"].get("number")
    issue = event.get("issue")
    if isinstance(issue, dict) and issue.get("pull_request"):
        return issue.get("number")
    return None


def next_link(header: Optional[str]) -> Optional[str]:
    if not header:
        return None
    for part in header.split(","):
        segment = part.strip()
        if segment.endswith('rel="next"'):
            url_part = segment.split(";", 1)[0].strip()
            if url_part.startswith("<") and url_part.endswith(">"):
                return url_part[1:-1]
    return None


def guess_mime(name: str) -> str:
    lower = name.lower()
    if lower.endswith(".jpg") or lower.endswith(".jpeg"):
        return "image/jpeg"
    if lower.endswith(".png"):
        return "image/png"
    return "application/octet-stream"


def build_multipart_payload(name: str, mime: str, data: bytes) -> Tuple[bytes, str]:
    boundary = "cn1ss-" + uuid.uuid4().hex
    body_parts = [
        f"--{boundary}\r\n".encode("utf-8"),
        f"Content-Disposition: form-data; name=\"file\"; filename=\"{name}\"\r\n".encode("utf-8"),
        f"Content-Type: {mime}\r\n\r\n".encode("utf-8"),
        data,
        f"\r\n--{boundary}--\r\n".encode("utf-8"),
    ]
    return b"".join(body_parts), f"multipart/form-data; boundary={boundary}"


body_path = pathlib.Path(sys.argv[1])
preview_dir = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2] else None
raw_body = body_path.read_text(encoding="utf-8")
body = raw_body.strip()
if not body:
    sys.exit(0)

if MARKER not in body:
    body = body.rstrip() + "\n\n" + MARKER

body_without_marker = body.replace(MARKER, "").strip()
if not body_without_marker:
    sys.exit(0)

event_path = os.environ.get("GITHUB_EVENT_PATH")
repo = os.environ.get("GITHUB_REPOSITORY")
token = os.environ.get("GITHUB_TOKEN")
actor = os.environ.get("GITHUB_ACTOR")
if not event_path or not repo or not token:
    sys.exit(0)

event = load_event(event_path)
pr_number = find_pr_number(event)
if not pr_number:
    sys.exit(0)

headers = {
    "Authorization": f"token {token}",
    "Accept": "application/vnd.github+json",
    "Content-Type": "application/json",
}

comments_url = f"https://api.github.com/repos/{repo}/issues/{pr_number}/comments?per_page=100"
existing_comment: Optional[Dict[str, object]] = None
preferred_comment: Optional[Dict[str, object]] = None
preferred_logins = set()
if actor:
    preferred_logins.add(actor)
preferred_logins.add("github-actions[bot]")

while comments_url:
    req = Request(comments_url, headers=headers)
    with urlopen(req) as resp:
        comments = json.load(resp)
        for comment in comments:
            body_text = comment.get("body") or ""
            if MARKER in body_text:
                existing_comment = comment
                login = comment.get("user", {}).get("login")
                if login in preferred_logins:
                    preferred_comment = comment
        comments_url = next_link(resp.headers.get("Link"))

comment_id: Optional[int] = None
created_placeholder = False

if preferred_comment is not None:
    existing_comment = preferred_comment

if existing_comment is not None:
    comment_id = existing_comment.get("id")
else:
    create_payload = json.dumps({"body": MARKER}).encode("utf-8")
    create_req = Request(
        f"https://api.github.com/repos/{repo}/issues/{pr_number}/comments",
        data=create_payload,
        headers=headers,
        method="POST",
    )
    with urlopen(create_req) as resp:
        created = json.load(resp)
        comment_id = created.get("id")
    created_placeholder = True
    print(
        f"[run-android-instrumentation-tests] Created new screenshot comment placeholder (id={comment_id})",
        file=sys.stdout,
    )

if comment_id is None:
    sys.exit(0)

attachment_pattern = re.compile(r"\(attachment:([^)]+)\)")
attachment_names = attachment_pattern.findall(body)

attachment_urls: Dict[str, str] = {}
failed_uploads = []

for name in attachment_names:
    if name in attachment_urls or name in failed_uploads:
        continue
    if not preview_dir:
        failed_uploads.append(name)
        continue
    file_path = preview_dir / name
    if not file_path.exists():
        failed_uploads.append(name)
        continue
    data = file_path.read_bytes()
    upload_url = (
        "https://uploads.github.com/repos/"
        f"{repo}/issues/comments/{comment_id}/attachments?name="
        + urllib.parse.quote(name, safe="")
    )
    mime = guess_mime(name)
    payload, content_type = build_multipart_payload(name, mime, data)
    upload_headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github+json",
        "Content-Type": content_type,
        "Content-Length": str(len(payload)),
        "X-GitHub-Api-Version": "2022-11-28",
    }
    try:
        upload_req = Request(upload_url, data=payload, headers=upload_headers, method="POST")
        with urlopen(upload_req) as resp:
            upload_info = json.load(resp)
    except HTTPError as exc:
        failed_uploads.append(name)
        error_body = exc.read().decode("utf-8", "replace") if hasattr(exc, "read") else ""
        print(
            f"[run-android-instrumentation-tests] Attachment upload failed for {name}: {exc} (status={exc.code})",
            file=sys.stderr,
        )
        if error_body:
            print(error_body, file=sys.stderr)
        continue
    url = (
        upload_info.get("url")
        or upload_info.get("download_url")
        or upload_info.get("browser_download_url")
        or upload_info.get("html_url")
    )
    if url:
        attachment_urls[name] = url
        print(
            f"[run-android-instrumentation-tests] Uploaded preview attachment '{name}'",
            file=sys.stdout,
        )
    else:
        failed_uploads.append(name)


def replace_attachment(match: re.Match[str]) -> str:
    name = match.group(1)
    url = attachment_urls.get(name)
    if url:
        return f"({url})"
    failed_uploads.append(name)
    return "(#)"


final_body = attachment_pattern.sub(replace_attachment, body)

if failed_uploads:
    unique_failures = sorted(set(failed_uploads))
    warning_line = "⚠️ _Preview upload failed for: " + ", ".join(unique_failures) + "._"
    final_body = final_body.replace(MARKER, warning_line + "\n" + MARKER, 1)

update_payload = json.dumps({"body": final_body}).encode("utf-8")
update_req = Request(
    f"https://api.github.com/repos/{repo}/issues/comments/{comment_id}",
    data=update_payload,
    headers=headers,
    method="PATCH",
)

with urlopen(update_req) as resp:
    resp.read()
    action = "updated" if not created_placeholder else "posted"
    print(
        f"[run-android-instrumentation-tests] PR comment {action} (status={resp.status}, bytes={len(update_payload)})",
        file=sys.stdout,
    )
PY
  local rc=$?
  if [ $rc -eq 0 ]; then
    ra_log "Posted screenshot comparison comment to PR"
  else
    ra_log "STAGE:COMMENT_POST_FAILED (see stderr for details)"
    if [ -n "${ARTIFACTS_DIR:-}" ]; then
      local failure_flag="$ARTIFACTS_DIR/pr-comment-failed.txt"
      printf 'Comment POST failed at %s\n' "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" > "$failure_flag" 2>/dev/null || true
    fi
  fi
  return 0
}

decode_test_asset() {
  local test_name="${1:-}"
  local dest="${2:-}"
  local channel="${3:-}"
  local verifier="${4:-}"
  local source=""
  local count="0"

  if [ "${#XMLS[@]}" -gt 0 ]; then
    for x in "${XMLS[@]}"; do
      count="$(count_chunks "$x" "$test_name" "$channel")"; count="${count//[^0-9]/}"; : "${count:=0}"
      [ "$count" -gt 0 ] || continue
      ra_log "Reassembling test '$test_name' from XML: $x (chunks=$count)"
      if decode_cn1ss_binary "$x" "$test_name" "$channel" > "$dest" 2>/dev/null; then
        if [ -z "$verifier" ] || "$verifier" "$dest"; then source="XML:$(basename "$x")"; break; fi
      fi
    done
  fi

  if [ -z "$source" ] && [ "${#LOGCAT_FILES[@]}" -gt 0 ]; then
    for logcat in "${LOGCAT_FILES[@]}"; do
      [ -s "$logcat" ] || continue
      count="$(count_chunks "$logcat" "$test_name" "$channel")"; count="${count//[^0-9]/}"; : "${count:=0}"
      [ "$count" -gt 0 ] || continue
      ra_log "Reassembling test '$test_name' from logcat: $logcat (chunks=$count)"
      if decode_cn1ss_binary "$logcat" "$test_name" "$channel" > "$dest" 2>/dev/null; then
        if [ -z "$verifier" ] || "$verifier" "$dest"; then source="LOGCAT:$(basename "$logcat")"; break; fi
      fi
    done
  fi

  if [ -z "$source" ] && [ -n "${TEST_EXEC_LOG:-}" ] && [ -s "$TEST_EXEC_LOG" ]; then
    count="$(count_chunks "$TEST_EXEC_LOG" "$test_name" "$channel")"; count="${count//[^0-9]/}"; : "${count:=0}"
    if [ "$count" -gt 0 ]; then
      ra_log "Reassembling test '$test_name' from test-results.log: $TEST_EXEC_LOG (chunks=$count)"
      if decode_cn1ss_binary "$TEST_EXEC_LOG" "$test_name" "$channel" > "$dest" 2>/dev/null; then
        if [ -z "$verifier" ] || "$verifier" "$dest"; then source="EXECLOG:$(basename "$TEST_EXEC_LOG")"; fi
      fi
    fi
  fi

  if [ -n "$source" ]; then
    printf '%s' "$source"
    return 0
  fi

  rm -f "$dest" 2>/dev/null || true
  return 1
}

decode_test_png() {
  decode_test_asset "$1" "$2" "" verify_png
}

decode_test_preview() {
  decode_test_asset "$1" "$2" "PREVIEW" verify_jpeg
}

# Verify PNG signature + non-zero size
verify_png() {
  local f="$1"
  [ -s "$f" ] || return 1
  head -c 8 "$f" | od -An -t x1 | tr -d ' \n' | grep -qi '^89504e470d0a1a0a$'
}

verify_jpeg() {
  local f="$1"
  [ -s "$f" ] || return 1
  local header
  header="$(head -c 2 "$f" | od -An -t x1 | tr -d ' \n' | tr '[:lower:]' '[:upper:]')"
  local trailer
  trailer="$(tail -c 2 "$f" | od -An -t x1 | tr -d ' \n' | tr '[:lower:]' '[:upper:]')"
  [ "$header" = "FFD8" ] && [ "$trailer" = "FFD9" ]
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
SCREENSHOT_PREVIEW_DIR="$SCREENSHOT_TMP_DIR/previews"

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
mapfile -t LOGCAT_FILES < <(
  find "$RESULTS_ROOT" -type f -name 'logcat-*.txt' -print 2>/dev/null
) || LOGCAT_FILES=()

# execution log (use first if present)
TEST_EXEC_LOG="$(find "$RESULTS_ROOT" -type f -path '*/testlog/test-results.log' -print -quit 2>/dev/null || true)"
[ -n "${TEST_EXEC_LOG:-}" ] || TEST_EXEC_LOG=""

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
  c="$(count_chunks "$x")"; c="${c//[^0-9]/}"; : "${c:=0}"
  XML_CHUNKS_TOTAL=$(( XML_CHUNKS_TOTAL + c ))
done
LOGCAT_CHUNKS=0
for logcat in "${LOGCAT_FILES[@]}"; do
  c="$(count_chunks "$logcat")"; c="${c//[^0-9]/}"; : "${c:=0}"
  LOGCAT_CHUNKS=$(( LOGCAT_CHUNKS + c ))
done
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

for logcat in "${LOGCAT_FILES[@]}"; do
  [ -s "$logcat" ] || continue
  while IFS= read -r name; do
    [ -n "$name" ] || continue
    TEST_NAME_SET["$name"]=1
  done < <(list_cn1ss_tests "$logcat" 2>/dev/null || true)
done

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
declare -A PREVIEW_OUTPUTS=()

ensure_dir "$SCREENSHOT_PREVIEW_DIR"

for test in "${TEST_NAMES[@]}"; do
  dest="$SCREENSHOT_TMP_DIR/${test}.png"
  if source_label="$(decode_test_png "$test" "$dest")"; then
    TEST_OUTPUTS["$test"]="$dest"
    TEST_SOURCES["$test"]="$source_label"
    ra_log "Decoded screenshot for '$test' (source=${source_label}, size: $(stat -c '%s' "$dest") bytes)"
    preview_dest="$SCREENSHOT_PREVIEW_DIR/${test}.jpg"
    if preview_source="$(decode_test_preview "$test" "$preview_dest")"; then
      PREVIEW_OUTPUTS["$test"]="$preview_dest"
      ra_log "Decoded preview for '$test' (source=${preview_source}, size: $(stat -c '%s' "$preview_dest") bytes)"
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
        count="$(count_chunks "$logcat" "$test")"; count="${count//[^0-9]/}"; : "${count:=0}"
        if [ "$count" -gt 0 ]; then extract_cn1ss_base64 "$logcat" "$test"; fi
      done
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
export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"
ra_log "STAGE:COMPARE -> Evaluating screenshots against stored references"
python3 "$SCRIPT_DIR/android/tests/process_screenshots.py" \
  --reference-dir "$SCREENSHOT_REF_DIR" \
  --emit-base64 \
  --preview-dir "$SCREENSHOT_PREVIEW_DIR" \
  "${COMPARE_ARGS[@]}" > "$COMPARE_JSON"

SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"

ra_log "STAGE:COMMENT_BUILD -> Rendering summary and PR comment markdown"
python3 - "$COMPARE_JSON" "$COMMENT_FILE" "$SUMMARY_FILE" <<'PY'
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
    base64_mime = result.get("base64_mime") or "image/png"
    base64_codec = result.get("base64_codec")
    base64_quality = result.get("base64_quality")
    base64_note = result.get("base64_note")
    message = ""
    copy_flag = "0"

    preview = result.get("preview") or {}
    preview_name = preview.get("name")
    preview_path = preview.get("path")
    preview_mime = preview.get("mime")
    preview_note = preview.get("note")
    preview_quality = preview.get("quality")
    if status == "equal":
        message = "Matches stored reference."
    elif status == "missing_expected":
        message = f"Reference screenshot missing at {expected_path}."
        copy_flag = "1"
        comment_entries.append({
            "test": test,
            "status": "missing reference",
            "message": message,
            "artifact_name": f"{test}.png",
            "preview_name": preview_name,
            "preview_path": preview_path,
            "preview_mime": preview_mime,
            "preview_note": preview_note,
            "preview_quality": preview_quality,
            "base64": base64_data,
            "base64_omitted": base64_omitted,
            "base64_length": base64_length,
            "base64_mime": base64_mime,
            "base64_codec": base64_codec,
            "base64_quality": base64_quality,
            "base64_note": base64_note,
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
            "artifact_name": f"{test}.png",
            "preview_name": preview_name,
            "preview_path": preview_path,
            "preview_mime": preview_mime,
            "preview_note": preview_note,
            "preview_quality": preview_quality,
            "base64": base64_data,
            "base64_omitted": base64_omitted,
            "base64_length": base64_length,
            "base64_mime": base64_mime,
            "base64_codec": base64_codec,
            "base64_quality": base64_quality,
            "base64_note": base64_note,
        })
    elif status == "error":
        message = f"Comparison error: {result.get('message', 'unknown error')}"
        copy_flag = "1"
        comment_entries.append({
            "test": test,
            "status": "comparison error",
            "message": message,
            "artifact_name": f"{test}.png",
            "preview_name": preview_name,
            "preview_path": preview_path,
            "preview_mime": preview_mime,
            "preview_note": preview_note,
            "preview_quality": preview_quality,
            "base64": None,
            "base64_omitted": base64_omitted,
            "base64_length": base64_length,
            "base64_mime": base64_mime,
            "base64_codec": base64_codec,
            "base64_quality": base64_quality,
            "base64_note": base64_note,
        })
    elif status == "missing_actual":
        message = "Actual screenshot missing (test did not produce output)."
        copy_flag = "1"
        comment_entries.append({
            "test": test,
            "status": "missing actual screenshot",
            "message": message,
            "artifact_name": None,
            "preview_name": preview_name,
            "preview_path": preview_path,
            "preview_mime": preview_mime,
            "preview_note": preview_note,
            "preview_quality": preview_quality,
            "base64": None,
            "base64_omitted": base64_omitted,
            "base64_length": base64_length,
            "base64_mime": base64_mime,
            "base64_codec": base64_codec,
            "base64_quality": base64_quality,
            "base64_note": base64_note,
        })
    else:
        message = f"Status: {status}."

    note_column = preview_note or base64_note or ""
    summary_lines.append("|".join([status, test, message, copy_flag, actual_path, note_column]))

summary_path.write_text("\n".join(summary_lines) + ("\n" if summary_lines else ""), encoding="utf-8")

if comment_entries:
    lines = ["### Android screenshot updates", ""]
    for entry in comment_entries:
        lines.append(f"- **{entry['test']}** — {entry['status']}. {entry['message']}")
        preview_name = entry.get("preview_name")
        if preview_name:
            lines.append("")
            lines.append(f"  ![{entry['test']}](attachment:{preview_name})")
            preview_notes = []
            preview_quality = entry.get("preview_quality")
            preview_note = entry.get("preview_note")
            if entry.get("preview_mime") == "image/jpeg" and preview_quality:
                preview_notes.append(f"JPEG preview quality {preview_quality}")
            if preview_note:
                preview_notes.append(preview_note)
            if entry.get("base64_note") and entry.get("base64_note") != preview_note:
                preview_notes.append(entry["base64_note"])
            if preview_notes:
                lines.append(f"  _Preview info: {'; '.join(preview_notes)}._")
        elif entry.get("base64"):
            lines.append("")
            mime = entry.get("base64_mime") or "image/png"
            lines.append(f"  ![{entry['test']}](data:{mime};base64,{entry['base64']})")
            preview_notes = []
            codec = entry.get("base64_codec")
            quality = entry.get("base64_quality")
            note = entry.get("base64_note")
            if codec == "jpeg" and quality:
                preview_notes.append(f"JPEG preview quality {quality}")
            if note:
                preview_notes.append(note)
            if preview_notes:
                lines.append(f"  _Preview info: {'; '.join(preview_notes)}._")
        elif entry.get("base64_omitted") == "too_large":
            size_note = ""
            if entry.get("base64_length"):
                size_note = f" (base64 length ≈ {entry['base64_length']:,} chars)"
            lines.append("")
            codec = entry.get("base64_codec")
            quality = entry.get("base64_quality")
            note = entry.get("base64_note")
            extra_bits = []
            if codec == "jpeg" and quality:
                extra_bits.append(f"attempted JPEG quality {quality}")
            if note:
                extra_bits.append(note)
            tail = ""
            if extra_bits:
                tail = " (" + "; ".join(extra_bits) + ")"
            lines.append(
                "  _Screenshot omitted from comment because the encoded payload exceeded GitHub's size limits"
                + size_note
                + "." + tail + "_"
            )
        artifact_name = entry.get("artifact_name")
        if artifact_name:
            lines.append(f"  _Full-resolution PNG saved as `{artifact_name}` in workflow artifacts._")
        lines.append("")
    MARKER = "<!-- CN1SS_SCREENSHOT_COMMENT -->"
    if lines[-1] != "":
        lines.append("")
    lines.append(MARKER)
    comment_path.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
else:
    comment_path.write_text("", encoding="utf-8")
PY

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
post_pr_comment "$COMMENT_FILE" "$SCREENSHOT_PREVIEW_DIR"

# Copy useful artifacts for GH Actions
for logcat in "${LOGCAT_FILES[@]}"; do
  cp -f "$logcat" "$ARTIFACTS_DIR/$(basename "$logcat")" 2>/dev/null || true
done
for x in "${XMLS[@]}"; do
  cp -f "$x" "$ARTIFACTS_DIR/$(basename "$x")" 2>/dev/null || true
done
[ -n "${TEST_EXEC_LOG:-}" ] && cp -f "$TEST_EXEC_LOG" "$ARTIFACTS_DIR/test-results.log" 2>/dev/null || true

exit 0
