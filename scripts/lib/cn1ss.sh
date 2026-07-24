#!/usr/bin/env bash
# Shared helpers for Codename One screenshot (CN1SS) chunk processing

# Default class names used by the Java source helpers
: "${CN1SS_PROCESS_CLASS:=ProcessScreenshots}"
: "${CN1SS_RENDER_CLASS:=RenderScreenshotReport}"
: "${CN1SS_POST_COMMENT_CLASS:=PostPrComment}"
: "${CN1SS_FIDELITY_RENDER_CLASS:=RenderFidelityReport}"
: "${CN1SS_FIDELITY_GATE_CLASS:=FidelityGate}"
: "${CN1SS_FIDELITY_COMPOSITE_CLASS:=FidelityComposite}"

CN1SS_INITIALIZED=0
CN1SS_JAVA_BIN=""
CN1SS_JAVAC_BIN=""
CN1SS_SOURCE_PATH=""
CN1SS_CACHE_ROOT=""
CN1SS_CLASS_DIR=""
CN1SS_STAMP_FILE=""
CN1SS_JAVA_CLASSPATH=""

if ! declare -p CN1SS_JAVA_OPTS >/dev/null 2>&1; then
  declare -a CN1SS_JAVA_OPTS=()
fi
if [ "${#CN1SS_JAVA_OPTS[@]}" -eq 0 ]; then
  CN1SS_JAVA_OPTS+=(-Djava.awt.headless=true)
fi

cn1ss_setup() {
  CN1SS_JAVA_BIN="$1"
  CN1SS_SOURCE_PATH="$2"
  local cache_override="${3:-}" tmp_root

  if [ -z "$CN1SS_SOURCE_PATH" ]; then
    # Default to common/java if not provided or empty
    local script_dir
    script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    CN1SS_SOURCE_PATH="$script_dir/common/java"
  fi

  if [ -z "$CN1SS_JAVA_BIN" ] || [ ! -x "$CN1SS_JAVA_BIN" ]; then
    cn1ss_log "CN1SS setup failed: java binary not executable ($CN1SS_JAVA_BIN)"
    return 1
  fi
  if [ ! -d "$CN1SS_SOURCE_PATH" ]; then
    cn1ss_log "CN1SS setup failed: source directory missing ($CN1SS_SOURCE_PATH)"
    return 1
  fi

  if [ -z "$CN1SS_JAVAC_BIN" ]; then
    local java_dir
    java_dir="$(dirname "$CN1SS_JAVA_BIN")"
    if [ -x "$java_dir/javac" ]; then
      CN1SS_JAVAC_BIN="$java_dir/javac"
    elif command -v javac >/dev/null 2>&1; then
      CN1SS_JAVAC_BIN="$(command -v javac)"
    else
      cn1ss_log "CN1SS setup failed: unable to locate javac"
      return 1
    fi
  fi

  tmp_root="${TMPDIR:-/tmp}"
  tmp_root="${tmp_root%/}"
  CN1SS_CACHE_ROOT="${cache_override:-${CN1SS_CACHE_DIR:-$tmp_root/cn1ss-java-cache}}"
  CN1SS_CLASS_DIR="$CN1SS_CACHE_ROOT/classes"
  CN1SS_STAMP_FILE="$CN1SS_CACHE_ROOT/.stamp"

  if [ "$CN1SS_INITIALIZED" -eq 1 ] && [ -n "$CN1SS_JAVA_CLASSPATH" ] && [ -d "$CN1SS_JAVA_CLASSPATH" ]; then
    return 0
  fi

  # Invalidate on CONTENT, not mtime. A git checkout / branch switch / cross-session
  # cache can leave source mtimes OLDER than a newer stamp, so an `-newer` check would
  # happily reuse stale classes -- e.g. a FidelityGate.java that gained
  # JsonUtil.stringifyPretty keeps serving a pre-method JsonUtil.class and the baseline
  # update dies with NoSuchMethodError. Digest every helper source (content + path) and
  # recompile whenever that digest changes.
  local src_hash
  src_hash="$(find "$CN1SS_SOURCE_PATH" -type f -name '*.java' -exec shasum {} + 2>/dev/null | awk '{print $1, $2}' | sort | shasum | awk '{print $1}')"
  local need_compile=1
  if [ -d "$CN1SS_CLASS_DIR" ] && [ -f "$CN1SS_STAMP_FILE" ]; then
    if [ "$(cat "$CN1SS_STAMP_FILE" 2>/dev/null)" = "$src_hash" ]; then
      need_compile=0
    fi
  fi

  if [ "$need_compile" -eq 1 ]; then
    mkdir -p "$CN1SS_CACHE_ROOT"
    rm -rf "$CN1SS_CLASS_DIR"
    mkdir -p "$CN1SS_CLASS_DIR"
    local -a sources=()
    while IFS= read -r -d '' src; do
      if grep -q '@PACKAGE@' "$src" 2>/dev/null; then
        cn1ss_log "Skipping template source $src"
        continue
      fi
      sources+=("$src")
    done < <(find "$CN1SS_SOURCE_PATH" -type f -name '*.java' -print0 | sort -z)
    if [ "${#sources[@]}" -eq 0 ]; then
      cn1ss_log "CN1SS setup failed: no Java sources found under $CN1SS_SOURCE_PATH"
      return 1
    fi
    cn1ss_log "Compiling CN1SS helpers -> $CN1SS_CLASS_DIR"
    local src display
    for src in "${sources[@]}"; do
      display="${src#$CN1SS_SOURCE_PATH/}"
      display="${display:-$(basename "$src")}"
      cn1ss_log "  javac $display"
      if ! "$CN1SS_JAVAC_BIN" -d "$CN1SS_CLASS_DIR" -cp "$CN1SS_CLASS_DIR" "$src"; then
        cn1ss_log "CN1SS setup failed: javac returned non-zero status ($display)"
        return 1
      fi
    done
    printf '%s\n' "$src_hash" > "$CN1SS_STAMP_FILE" 2>/dev/null || true
  else
    cn1ss_log "Reusing CN1SS helpers in $CN1SS_CLASS_DIR"
  fi

  CN1SS_JAVA_CLASSPATH="$CN1SS_CLASS_DIR"
  CN1SS_INITIALIZED=1
}

cn1ss_log() {
  echo "[cn1ss] $*"
}

cn1ss_collect_suite_failures() {
  # Assertion-only tests do not emit screenshots, so screenshot count/mismatch
  # gates cannot catch them. Treat runner-level test failures as fatal in the
  # platform wrapper after logs have been collected.
  {
    grep -hE 'CN1SS:ERR:suite test=.*(failed=|failed:|failed due to timeout|forced timeout|finalize exception=)' "$@" 2>/dev/null || true
    grep -hE 'CN1SS:ERR:(exception caught in EDT|testExecutionFinished exception=)' "$@" 2>/dev/null || true
  } | awk '!seen[$0]++'
}

cn1ss_java_run() {
  local class_name="$1"; shift
  if [ -z "${CN1SS_JAVA_BIN:-}" ] || [ ! -x "$CN1SS_JAVA_BIN" ]; then
    cn1ss_log "CN1SS_JAVA_BIN is not configured"
    return 1
  fi
  if [ -z "${CN1SS_JAVA_CLASSPATH:-}" ] || [ ! -d "$CN1SS_JAVA_CLASSPATH" ]; then
    cn1ss_log "CN1SS Java helpers not initialized; call cn1ss_setup first"
    return 1
  fi
  "$CN1SS_JAVA_BIN" "${CN1SS_JAVA_OPTS[@]}" -cp "$CN1SS_JAVA_CLASSPATH" "$class_name" "$@"
}

# WebSocket screenshot server bootstrap. Starts Cn1ssScreenshotServer on
# an ephemeral port; captures the bound port from its first stdout line so
# the runner can hand it to the device via -Dcn1ss.websocket.url=...
# Sets CN1SS_WS_PORT and CN1SS_WS_PID on success.
cn1ss_start_ws_server() {
  local out_dir="$1"
  if [ -z "$out_dir" ]; then
    cn1ss_log "cn1ss_start_ws_server: missing output dir"
    return 1
  fi
  mkdir -p "$out_dir" 2>/dev/null || true
  if [ -z "${CN1SS_JAVA_BIN:-}" ] || [ -z "${CN1SS_JAVA_CLASSPATH:-}" ]; then
    cn1ss_log "cn1ss_start_ws_server: cn1ss_setup must be called first"
    return 1
  fi
  local port_file
  port_file="$(mktemp)"
  # Bind the fixed standard port (override with CN1SS_WS_BIND_PORT). The device
  # runner defaults to ws://HOST:8765 with no per-run injection, so this must
  # match CN1SS_WS_DEFAULT_PORT in Cn1ssDeviceRunnerHelper.java. CN1SS_WS_PORT
  # (set below from the server's reported port) stays the captured bound port.
  local bind_port="${CN1SS_WS_BIND_PORT:-8765}"
  "$CN1SS_JAVA_BIN" "${CN1SS_JAVA_OPTS[@]}" -cp "$CN1SS_JAVA_CLASSPATH" \
    Cn1ssScreenshotServer --port "$bind_port" --out "$out_dir" \
    >"$port_file" 2>&1 &
  CN1SS_WS_PID=$!
  # Wait for the server to print "CN1SS_SERVER_PORT=<n>" on the first line.
  local attempt
  for attempt in 1 2 3 4 5 6 7 8 9 10; do
    if grep -q "^CN1SS_SERVER_PORT=" "$port_file" 2>/dev/null; then
      CN1SS_WS_PORT="$(grep -m1 "^CN1SS_SERVER_PORT=" "$port_file" | cut -d'=' -f2)"
      CN1SS_WS_LOG="$port_file"
      cn1ss_log "Cn1ssScreenshotServer listening on port $CN1SS_WS_PORT (pid $CN1SS_WS_PID, log $port_file)"
      return 0
    fi
    if ! kill -0 "$CN1SS_WS_PID" 2>/dev/null; then
      cn1ss_log "Cn1ssScreenshotServer died before reporting a port:"
      cat "$port_file" >&2
      return 1
    fi
    sleep 0.2
  done
  cn1ss_log "Timed out waiting for Cn1ssScreenshotServer to bind a port"
  kill "$CN1SS_WS_PID" 2>/dev/null || true
  return 1
}

cn1ss_stop_ws_server() {
  if [ -n "${CN1SS_WS_PID:-}" ]; then
    kill "$CN1SS_WS_PID" 2>/dev/null || true
    wait "$CN1SS_WS_PID" 2>/dev/null || true
    cn1ss_log "Cn1ssScreenshotServer (pid $CN1SS_WS_PID) stopped"
    unset CN1SS_WS_PID CN1SS_WS_PORT
  fi
  # Persist the server log so the WebSocket transport is debuggable from CI
  # artifacts. The server prints one CN1SS:INFO:test=... line per delivered
  # screenshot plus any "binary frame without META" / hash_mismatch warnings;
  # without this the only copy lived in a mktemp file that never reached the
  # uploaded artifacts (the WS pipeline was effectively a black box on
  # failure). Also surface a one-line summary + tail in the job log directly.
  if [ -n "${CN1SS_WS_LOG:-}" ] && [ -s "${CN1SS_WS_LOG:-}" ]; then
    local delivered dropped
    delivered="$(grep -c "^CN1SS:INFO:test=" "$CN1SS_WS_LOG" 2>/dev/null || echo 0)"
    dropped="$(grep -c "binary frame without META" "$CN1SS_WS_LOG" 2>/dev/null || echo 0)"
    cn1ss_log "WebSocket server summary: ${delivered} screenshot(s) written, ${dropped} unpaired binary frame(s) dropped"
    if [ -n "${ARTIFACTS_DIR:-}" ]; then
      mkdir -p "$ARTIFACTS_DIR" 2>/dev/null || true
      cp -f "$CN1SS_WS_LOG" "$ARTIFACTS_DIR/cn1ss-ws-server.log" 2>/dev/null \
        && cn1ss_log "WebSocket server log saved to $ARTIFACTS_DIR/cn1ss-ws-server.log"
    fi
    cn1ss_log "----- last 40 lines of Cn1ssScreenshotServer log -----"
    tail -n 40 "$CN1SS_WS_LOG" 2>/dev/null | sed 's/^/[cn1ss-ws-server] /'
    cn1ss_log "----- end of Cn1ssScreenshotServer log -----"
  fi
  unset CN1SS_WS_LOG
}

cn1ss_file_size() {
  local file="$1"
  if [ ! -f "$file" ]; then
    echo 0
    return
  fi
  if stat --version >/dev/null 2>&1; then
    stat --printf='%s' "$file"
  elif stat -f '%z' "$file" >/dev/null 2>&1; then
    stat -f '%z' "$file"
  else
    wc -c < "$file" 2>/dev/null | tr -d ' \n'
  fi
}

cn1ss_post_pr_comment() {
  local body_file="$1"
  local preview_dir="$2"
  if [ -z "$body_file" ] || [ ! -s "$body_file" ]; then
    cn1ss_log "Skipping PR comment post (no content)."
    return 0
  fi
  local comment_token="${GITHUB_TOKEN:-}"; local body_size
  if [ -z "$comment_token" ] && [ -n "${GH_TOKEN:-}" ]; then
    comment_token="${GH_TOKEN}"
    cn1ss_log "PR comment auth using GH_TOKEN fallback"
  fi
  if [ -z "$comment_token" ]; then
    cn1ss_log "PR comment skipped (no GitHub token available)"
    return 0
  fi
  if [ -z "${GITHUB_EVENT_PATH:-}" ] || [ ! -f "$GITHUB_EVENT_PATH" ]; then
    cn1ss_log "PR comment skipped (GITHUB_EVENT_PATH unavailable)"
    return 0
  fi
  body_size=$(wc -c < "$body_file" 2>/dev/null || echo 0)
  cn1ss_log "Attempting to post PR comment (payload bytes=${body_size})"
  local -a extra_args=()
  if [ -n "${CN1SS_COMMENT_MARKER:-}" ]; then
    extra_args+=(--marker "${CN1SS_COMMENT_MARKER}")
  fi
  if [ -n "${CN1SS_COMMENT_LOG_PREFIX:-}" ]; then
    extra_args+=(--log-prefix "${CN1SS_COMMENT_LOG_PREFIX}")
  fi
  if [ -n "${CN1SS_PREVIEW_SUBDIR:-}" ]; then
    extra_args+=(--preview-subdir "${CN1SS_PREVIEW_SUBDIR}")
  fi
  GITHUB_TOKEN="$comment_token" cn1ss_java_run "$CN1SS_POST_COMMENT_CLASS" \
    --body "$body_file" \
    --preview-dir "$preview_dir" \
    "${extra_args[@]}"
  local rc=$?
  if [ $rc -eq 0 ]; then
    cn1ss_log "Posted screenshot comparison comment to PR"
  else
    cn1ss_log "STAGE:COMMENT_POST_FAILED (see stderr for details)"
    if [ -n "${ARTIFACTS_DIR:-}" ]; then
      local failure_flag="$ARTIFACTS_DIR/pr-comment-failed.txt"
      printf 'Comment POST failed at %s\n' "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" > "$failure_flag" 2>/dev/null || true
    fi
  fi
  return $rc
}

# Count the "missing" screenshots in a comparison JSON, i.e. expected tests
# that produced no image (status == "missing_actual"). This is the signal for
# the screenshot-count regression this guard targets: when a test hangs or the
# rendering pipeline crashes partway (e.g. the Metal DialogTheme hang), every
# test from that point on is recorded as missing_actual instead of equal, so
# the suite silently drops from 122 captures to 107. Counting *entries* would
# miss this - the iOS harness still records all 122 names, just with 15 of them
# flagged missing_actual - which is why a len(results) guard never caught it.
# We count the missing ones directly instead.
cn1ss_count_missing() {
  local json="$1"
  if [ -z "$json" ] || [ ! -s "$json" ]; then
    # No comparison JSON at all means the suite produced nothing usable.
    # Report a sentinel large enough to trip any tolerance so the caller
    # fails loudly rather than treating "no data" as "nothing missing".
    echo 999999
    return
  fi
  python3 - "$json" <<'PY'
import json, sys
try:
    data = json.load(open(sys.argv[1]))
    results = data.get("results", []) if isinstance(data, dict) else []
except Exception:
    print(999999)
    sys.exit(0)
print(sum(1 for r in results if isinstance(r, dict) and r.get("status") == "missing_actual"))
PY
}

# Count the authoritative *expected* screenshot set: the number of golden PNGs
# stored in the reference directory. The reference dir is the manifest -- it is
# the single source of truth for how many screenshots a suite must produce. We
# deliberately do NOT derive the expected count from whatever the harness chose
# to deliver, because a harness that silently drops a test (a hang, a crash, a
# transport that never delivered the frame) simply omits it from its delivered
# set, leaving no "missing_actual" record behind. Counting goldens instead means
# a dropped test is always visible as an uncovered golden. Top-level *.png only;
# the reference dirs are flat <testName>.png sets.
cn1ss_count_reference() {
  local dir="$1"
  if [ -z "$dir" ] || [ ! -d "$dir" ]; then
    echo 0
    return
  fi
  local n
  n=$(find "$dir" -maxdepth 1 -name '*.png' -type f 2>/dev/null | wc -l)
  echo "${n//[^0-9]/}"
}

# Count the goldens that were actually rendered AND compared against their
# reference, i.e. results with status "equal" or "different". A "missing_actual"
# (listed but no image), a "missing_expected" (new image with no golden yet) and
# any test that never appeared at all are all NOT covered. expected - covered is
# therefore the number of expected screenshots that failed to materialise.
cn1ss_count_covered() {
  local json="$1"
  if [ -z "$json" ] || [ ! -s "$json" ]; then
    echo 0
    return
  fi
  python3 - "$json" <<'PY'
import json, sys
try:
    data = json.load(open(sys.argv[1]))
    results = data.get("results", []) if isinstance(data, dict) else []
except Exception:
    print(0)
    sys.exit(0)
print(sum(1 for r in results if isinstance(r, dict) and r.get("status") in ("equal", "different")))
PY
}

# Count "missing_expected" results: a screenshot the suite captured and delivered
# but which has NO committed golden under the reference directory. This is the
# signal that a test ran for real yet its reference was never integrated -- the
# golden set is incomplete. It is invisible to the count-regression guard (which
# counts goldens, and there is no golden here) and to the mismatch guard (status
# is not "different"/"error"), so without an explicit check a brand-new test that
# captures fine but whose golden was forgotten passes silently on every platform.
cn1ss_count_missing_expected() {
  local json="$1"
  if [ -z "$json" ] || [ ! -s "$json" ]; then
    echo 0
    return
  fi
  python3 - "$json" <<'PY'
import json, sys
try:
    data = json.load(open(sys.argv[1]))
    results = data.get("results", []) if isinstance(data, dict) else []
except Exception:
    print(0)
    sys.exit(0)
print(sum(1 for r in results if isinstance(r, dict) and r.get("status") == "missing_expected"))
PY
}

# Write the machine-readable report consumed by /port-status/. Callers opt in
# with CN1SS_PORT_ID and provide up to three suite logs. Screenshot comparison
# results come from the same JSON that drives the strict golden gate below.
cn1ss_generate_port_status() {
  local compare_json="$1"
  local artifacts_dir="$2"
  if [ -z "${CN1SS_PORT_ID:-}" ]; then
    return 0
  fi

  local script_dir repo_root status_script python_bin output run_url binary_size
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  repo_root="$(cd "$script_dir/../.." && pwd)"
  status_script="$repo_root/scripts/hellocodenameone/conformance/port_status.py"
  python_bin="${CN1SS_PYTHON_BIN:-python3}"
  output="$artifacts_dir/port-status-${CN1SS_PORT_ID}.json"
  run_url="${GITHUB_RUN_URL:-}"
  if [ -z "$run_url" ] && [ -n "${GITHUB_SERVER_URL:-}" ] && [ -n "${GITHUB_REPOSITORY:-}" ] && [ -n "${GITHUB_RUN_ID:-}" ]; then
    run_url="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
  fi

  if ! command -v "$python_bin" >/dev/null 2>&1; then
    cn1ss_log "FATAL: Cannot generate port status: $python_bin is unavailable"
    return 19
  fi
  if [ ! -f "$status_script" ]; then
    cn1ss_log "FATAL: Cannot generate port status: $status_script is missing"
    return 19
  fi

  local -a args=(
    "$status_script" normalize
    --port "$CN1SS_PORT_ID"
    --compare "$compare_json"
    --output "$output"
    --run-url "$run_url"
    --commit "${GITHUB_SHA:-}"
  )
  local log_var log_path
  for log_var in CN1SS_SUITE_LOG CN1SS_SUITE_LOG_2 CN1SS_SUITE_LOG_3; do
    log_path="${!log_var:-}"
    if [ -n "$log_path" ]; then
      args+=(--log "$log_path")
    fi
  done
  binary_size="${CN1SS_BINARY_SIZE_BYTES:-}"
  if [ -z "$binary_size" ] && [ -n "${CN1SS_BINARY_PATH:-}" ] && [ -e "$CN1SS_BINARY_PATH" ]; then
    binary_size="$($python_bin -c 'from pathlib import Path; import sys; p=Path(sys.argv[1]); print(p.stat().st_size if p.is_file() else sum(f.stat().st_size for f in p.rglob("*") if f.is_file()))' "$CN1SS_BINARY_PATH")"
  fi
  if [ -n "$binary_size" ]; then
    args+=(--binary-size "$binary_size")
  fi
  if [ "${CN1SS_FAIL_ON_TEST_FAILURE:-0}" = "1" ]; then
    args+=(--fail-on-test-failure)
  fi
  if ! "$python_bin" "${args[@]}"; then
    cn1ss_log "FATAL: Failed to generate normalized port status for $CN1SS_PORT_ID"
    return 19
  fi
  cn1ss_log "Wrote normalized port status to $output"
}

# Shared function to generate report, compare screenshots, and post PR comment
cn1ss_process_and_report() {
  local platform_title="$1"
  local compare_json_out="$2"
  local summary_out="$3"
  local comment_out="$4"
  local ref_dir="$5"
  local preview_dir="$6"
  local artifacts_dir="$7"
  # Optional: array of actual entries in format "testName=path"
  shift 7
  local actual_entries=("$@")

  local rc=0

  # Run ProcessScreenshots
  local -a compare_args=("--reference-dir" "$ref_dir" "--emit-base64" "--preview-dir" "$preview_dir")
  # Optional per-pipeline gate override. The ProcessScreenshots defaults
  # (channelDelta=4, mismatch=0.30%) allow ~9k px of drift on a 1179x2556
  # phone capture -- enough for a widget-level regression (e.g. a button's
  # square-vs-round corners, ~2-3k px) to pass silently and leave a stale
  # golden in the tree. Pipelines with deterministic renders should set a
  # tighter CN1SS_MAX_MISMATCH_PERCENT; per-test .tolerance files still
  # override for legitimately noisy screens (GPU 3D, maps, video, toasts).
  if [ -n "${CN1SS_MAX_CHANNEL_DELTA:-}" ]; then
    compare_args+=("--max-channel-delta" "${CN1SS_MAX_CHANNEL_DELTA}")
  fi
  if [ -n "${CN1SS_MAX_MISMATCH_PERCENT:-}" ]; then
    compare_args+=("--max-mismatch-percent" "${CN1SS_MAX_MISMATCH_PERCENT}")
  fi
  for entry in "${actual_entries[@]}"; do
    compare_args+=("--actual" "$entry")
  done

  cn1ss_log "STAGE:COMPARE -> Evaluating screenshots against stored references"
  if ! cn1ss_java_run "$CN1SS_PROCESS_CLASS" "${compare_args[@]}" > "$compare_json_out"; then
    cn1ss_log "FATAL: Screenshot comparison helper failed"
    return 13
  fi

  # Run RenderScreenshotReport
  cn1ss_log "STAGE:COMMENT_BUILD -> Rendering summary and PR comment markdown"
  local -a render_args=(
    --title "$platform_title"
    --compare-json "$compare_json_out"
    --comment-out "$comment_out"
    --summary-out "$summary_out"
  )
  if [ -n "${CN1SS_SUCCESS_MESSAGE:-}" ]; then
    render_args+=(--success-message "$CN1SS_SUCCESS_MESSAGE")
  fi
  if [ -n "${CN1SS_COVERAGE_SUMMARY:-}" ]; then
    render_args+=(--coverage-summary "$CN1SS_COVERAGE_SUMMARY")
  fi
  if [ -n "${CN1SS_COVERAGE_HTML_URL:-}" ]; then
    render_args+=(--coverage-html-url "$CN1SS_COVERAGE_HTML_URL")
  fi
  if [ -n "${CN1SS_VM_TIME:-}" ]; then
    render_args+=(--vm-time "$CN1SS_VM_TIME")
  fi
  if [ -n "${CN1SS_COMPILATION_TIME:-}" ]; then
    render_args+=(--compilation-time "$CN1SS_COMPILATION_TIME")
  fi

  # Pass any stats files found in artifacts
  if [ -n "$artifacts_dir" ] && [ -d "$artifacts_dir" ]; then
    for stats_file in "$artifacts_dir"/iphone-builder-stats.txt "$artifacts_dir"/ios-test-stats.txt "$artifacts_dir"/android-test-stats.txt "$artifacts_dir"/base64-performance-stats.txt "$artifacts_dir"/windows-benchmark-stats.txt; do
      if [ -f "$stats_file" ]; then
        render_args+=(--extra-stats "$stats_file")
      fi
    done
  fi

  if ! cn1ss_java_run "$CN1SS_RENDER_CLASS" "${render_args[@]}"; then
    cn1ss_log "FATAL: Failed to render screenshot summary/comment"
    return 14
  fi

  if [ -s "$summary_out" ]; then
    cn1ss_log "  -> Wrote summary entries to $summary_out ($(wc -l < "$summary_out" 2>/dev/null || echo 0) line(s))"
  else
    cn1ss_log "  -> No summary entries generated (all screenshots matched stored baselines)"
  fi

  if [ -s "$comment_out" ]; then
    cn1ss_log "  -> Prepared PR comment payload at $comment_out (bytes=$(wc -c < "$comment_out" 2>/dev/null || echo 0))"
  else
    cn1ss_log "  -> No PR comment content produced"
  fi

  # Process summary entries (copy artifacts, clean up)
  if [ -s "$summary_out" ]; then
    while IFS='|' read -r status test message copy_flag path preview_note; do
      [ -n "${test:-}" ] || continue
      cn1ss_log "Test '${test}': ${message}"
      if [ "$copy_flag" = "1" ] && [ -n "${path:-}" ] && [ -f "$path" ]; then
        cp -f "$path" "$artifacts_dir/${test}.png" 2>/dev/null || true
        cn1ss_log "  -> Stored PNG artifact copy at $artifacts_dir/${test}.png"
      fi
      if [ "$status" = "equal" ] && [ -n "${path:-}" ]; then
        rm -f "$path" 2>/dev/null || true
      fi
      if [ -n "${preview_note:-}" ]; then
        cn1ss_log "  Preview note: ${preview_note}"
      fi
    done < "$summary_out"
  fi

  cp -f "$compare_json_out" "$artifacts_dir/screenshot-compare.json" 2>/dev/null || true
  if [ -s "$comment_out" ]; then
    cp -f "$comment_out" "$artifacts_dir/screenshot-comment.md" 2>/dev/null || true
  fi

  cn1ss_generate_port_status "$compare_json_out" "$artifacts_dir" || {
    local port_status_rc=$?
    return "$port_status_rc"
  }

  cn1ss_log "STAGE:COMMENT_POST -> Submitting PR feedback"
  local comment_rc=0
  if [ "${CN1SS_SKIP_COMMENT:-0}" = "1" ]; then
    cn1ss_log "Skipping PR comment as requested (CN1SS_SKIP_COMMENT=1)"
  elif ! cn1ss_post_pr_comment "$comment_out" "$preview_dir"; then
    comment_rc=$?
  fi

  if [ "${CN1SS_FAIL_ON_MISMATCH:-0}" = "1" ]; then
    if [ -f "$summary_out" ] && (grep -q "^different|" "$summary_out" || grep -q "^error|" "$summary_out"); then
      cn1ss_log "FATAL: Screenshot mismatches or errors detected (CN1SS_FAIL_ON_MISMATCH=1)"
      return 15
    fi

    # ------------------------------------------------------------------------
    # Screenshot count-regression guard. DO NOT WEAKEN OR REMOVE.
    #
    # Every golden in the reference directory must be re-produced and compared
    # on every run. The reference set is the manifest: expected == number of
    # golden PNGs (cn1ss_count_reference), covered == goldens that were rendered
    # and compared (cn1ss_count_covered, i.e. status equal|different). When a
    # test hangs, crashes or its frame never gets delivered, it drops out of the
    # comparison entirely and `covered` falls below `expected` -- which is the
    # ONLY reliable signal, because a dropped test leaves no per-test record
    # behind to count (the older missing_actual-only check was blind to this and
    # let suites silently shrink from 124 captures to 58 while still going green).
    #
    # We fail when expected - covered exceeds CN1SS_ALLOWED_MISSING (default 0:
    # no uncovered goldens tolerated). A pipeline with a known, steady-state
    # screenshot-only gap may set its own tolerance and document why. Assertion
    # test failures are never covered by this tolerance; platform wrappers scan
    # CN1SS:ERR:suite test=... failure lines separately and fail the run.
    # CN1SS_MIN_SCREENSHOTS can raise the floor above the on-disk golden count
    # (useful before the reference set is fully seeded). The only bypass is
    # CN1SS_SKIP_COUNT_CHECK=1, reserved for the deliberate, manual act of
    # seeding a brand new reference set; it is loud in the log so it can never
    # be mistaken for normal operation.
    # ------------------------------------------------------------------------
    if [ "${CN1SS_SKIP_COUNT_CHECK:-0}" = "1" ]; then
      cn1ss_log "WARNING: CN1SS_SKIP_COUNT_CHECK=1 -- screenshot count-regression guard BYPASSED. This must only be used while intentionally seeding a new reference set."
    else
      local expected_count covered_count uncovered_count allowed_missing min_floor
      expected_count=$(cn1ss_count_reference "$ref_dir")
      expected_count="${expected_count//[^0-9]/}"; : "${expected_count:=0}"
      min_floor="${CN1SS_MIN_SCREENSHOTS:-0}"
      min_floor="${min_floor//[^0-9]/}"; : "${min_floor:=0}"
      if [ "$min_floor" -gt "$expected_count" ]; then
        expected_count="$min_floor"
      fi
      covered_count=$(cn1ss_count_covered "$compare_json_out")
      covered_count="${covered_count//[^0-9]/}"; : "${covered_count:=0}"
      allowed_missing="${CN1SS_ALLOWED_MISSING:-0}"
      allowed_missing="${allowed_missing//[^0-9]/}"; : "${allowed_missing:=0}"
      uncovered_count=$(( expected_count - covered_count ))
      [ "$uncovered_count" -lt 0 ] && uncovered_count=0
      if [ "$uncovered_count" -gt "$allowed_missing" ]; then
        cn1ss_log "FATAL: $uncovered_count of $expected_count expected screenshot(s) were not produced and compared (only $covered_count covered); $allowed_missing tolerated (CN1SS_ALLOWED_MISSING)."
        cn1ss_log "       A test failed to emit its screenshot, or the suite hung/crashed before finishing. The golden set under the comparison directory is the source of truth for how many screenshots must be produced."
        return 17
      fi
      cn1ss_log "Screenshot count check passed: $covered_count of $expected_count goldens covered ($uncovered_count uncovered <= $allowed_missing tolerated)."

      # Missing-reference guard: a test that captured and delivered a screenshot
      # but has no committed golden (status "missing_expected") means the
      # reference set is incomplete -- the golden was never integrated. Neither
      # the mismatch guard (status != different/error) nor the count guard (no
      # golden to count) catches it, so it would otherwise pass silently. Fail
      # here so a new/ported test's golden cannot be left unintegrated on any
      # strict pipeline. CN1SS_ALLOWED_MISSING_EXPECTED can set a tolerance; the
      # deliberate seeding bypass (CN1SS_SKIP_COUNT_CHECK=1) skips this too.
      local missing_expected_count allowed_missing_expected
      missing_expected_count=$(cn1ss_count_missing_expected "$compare_json_out")
      missing_expected_count="${missing_expected_count//[^0-9]/}"; : "${missing_expected_count:=0}"
      allowed_missing_expected="${CN1SS_ALLOWED_MISSING_EXPECTED:-0}"
      allowed_missing_expected="${allowed_missing_expected//[^0-9]/}"; : "${allowed_missing_expected:=0}"
      if [ "$missing_expected_count" -gt "$allowed_missing_expected" ]; then
        cn1ss_log "FATAL: $missing_expected_count captured screenshot(s) have no committed reference (missing_expected); $allowed_missing_expected tolerated (CN1SS_ALLOWED_MISSING_EXPECTED)."
        cn1ss_log "       A test ran and produced a screenshot but its golden was never integrated under the reference directory. Seed the golden (see the reference dir README) and commit it."
        return 18
      fi
      cn1ss_log "Missing-reference check passed: $missing_expected_count captured screenshots without a golden <= $allowed_missing_expected tolerated."
    fi
  fi

  return $comment_rc
}

# Native-fidelity counterpart to cn1ss_process_and_report. Instead of asserting
# pixel-equality against a stored CN1 golden, it measures how close each CN1
# component render is to the committed NATIVE widget golden of the same name and
# applies the one-way ratchet gate (FidelityGate): a change may only keep or
# improve fidelity, never regress it below the recorded baseline (minus epsilon).
#
#   cn1ss_process_fidelity TITLE COMPARE_JSON SUMMARY COMMENT GOLDENS_DIR \
#       PREVIEW_DIR ARTIFACTS_DIR BASELINE_JSON [name=path ...]
#
# Goldens live under GOLDENS_DIR as "<name>.png" (the native widget); each actual
# entry is the CN1 render "<name>=<path-to-_cn1.png>". Behaviour switches on env:
#   FIDELITY_UPDATE_BASELINE=1 -> rewrite BASELINE_JSON from the current scores
#                                 and SKIP gating (loud, must be reviewed in PR).
#   CN1SS_FIDELITY_EPSILON      -> allowed fidelity drop before failing (def 0.5).
#   CN1SS_FAIL_ON_MISMATCH=1    -> let the gate's exit code fail the run.
# The native goldens themselves are (re)generated by the runner script from the
# device-delivered "_native.png" frames, not here.
cn1ss_process_fidelity() {
  local platform_title="$1"
  local compare_json_out="$2"
  local summary_out="$3"
  local comment_out="$4"
  local goldens_dir="$5"
  local preview_dir="$6"
  local artifacts_dir="$7"
  local baseline_file="$8"
  shift 8
  local actual_entries=("$@")

  # 1) Compare CN1 renders against native goldens (fidelity scoring).
  local -a compare_args=("--mode" "fidelity" "--reference-dir" "$goldens_dir" "--emit-base64" "--preview-dir" "$preview_dir")
  # Glass tiles are composited over a shared gradient backdrop; pass it so the
  # comparator can mask the backdrop out and score only the widget. When unset the
  # comparator falls back to the canonical path relative to the goldens dir.
  if [ -n "${CN1SS_FIDELITY_BACKDROP:-}" ] && [ -f "${CN1SS_FIDELITY_BACKDROP}" ]; then
    compare_args+=("--backdrop" "${CN1SS_FIDELITY_BACKDROP}")
  fi
  # The comparison mode (normal / glass-masked / lens) is declared per test in
  # fidelity-tests.yaml; pass the spec + platform so the comparator scores from
  # test intent instead of the legacy corner heuristic. When unset the comparator
  # looks for the canonical spec relative to the goldens dir.
  if [ -n "${CN1SS_FIDELITY_SPEC:-}" ] && [ -f "${CN1SS_FIDELITY_SPEC}" ]; then
    compare_args+=("--spec" "${CN1SS_FIDELITY_SPEC}")
  fi
  if [ -n "${CN1SS_FIDELITY_PLATFORM:-}" ]; then
    compare_args+=("--spec-platform" "${CN1SS_FIDELITY_PLATFORM}")
  fi
  local entry
  for entry in "${actual_entries[@]}"; do
    compare_args+=("--actual" "$entry")
  done
  cn1ss_log "STAGE:FIDELITY_COMPARE -> Scoring CN1 renders against native widget goldens"
  if ! cn1ss_java_run "$CN1SS_PROCESS_CLASS" "${compare_args[@]}" > "$compare_json_out"; then
    cn1ss_log "FATAL: Fidelity comparison helper failed"
    return 13
  fi

  # 2) Render the fidelity report (summary + PR comment markdown).
  cn1ss_log "STAGE:FIDELITY_REPORT -> Rendering fidelity summary and PR comment"
  local -a render_args=(
    --title "$platform_title"
    --compare-json "$compare_json_out"
    --comment-out "$comment_out"
    --summary-out "$summary_out"
  )
  if [ -n "${baseline_file:-}" ] && [ -f "$baseline_file" ]; then
    render_args+=(--baseline "$baseline_file")
  fi
  if [ -n "${CN1SS_FIDELITY_ASPIRATIONAL:-}" ]; then
    render_args+=(--aspirational "$CN1SS_FIDELITY_ASPIRATIONAL")
  fi
  if ! cn1ss_java_run "$CN1SS_FIDELITY_RENDER_CLASS" "${render_args[@]}"; then
    cn1ss_log "FATAL: Failed to render fidelity summary/comment"
    return 14
  fi

  # Persist artifacts: the comparison JSON, the rendered comment, and a copy of
  # every CN1 render the summary flagged (copyFlag is always 1 in fidelity mode).
  cp -f "$compare_json_out" "$artifacts_dir/fidelity-compare.json" 2>/dev/null || true
  if [ -s "$comment_out" ]; then
    cp -f "$comment_out" "$artifacts_dir/fidelity-comment.md" 2>/dev/null || true
  fi
  local cn1_dir=""
  if [ -s "$summary_out" ]; then
    while IFS='|' read -r status test message copy_flag path fidelity; do
      [ -n "${test:-}" ] || continue
      cn1ss_log "Fidelity '${test}': ${message}"
      if [ "$copy_flag" = "1" ] && [ -n "${path:-}" ] && [ -f "$path" ]; then
        cp -f "$path" "$artifacts_dir/${test}_cn1.png" 2>/dev/null || true
        [ -z "$cn1_dir" ] && cn1_dir="$(dirname "$path")"
      fi
    done < "$summary_out"
  fi

  # Render the visual fidelity guide: one "card" per component+state showing the
  # native widget (left) next to the CN1 render (right) for each appearance, with
  # the fidelity percentage beside each pair, plus a single overview contact
  # sheet. ref_dir holds the same-run native references ("<name>.png"); cn1_dir
  # holds the CN1 renders ("<name>_cn1.png").
  if [ -n "$cn1_dir" ]; then
    cn1ss_log "STAGE:FIDELITY_CARDS -> Rendering visual native-vs-CN1 comparison cards"
    if cn1ss_java_run "$CN1SS_FIDELITY_COMPOSITE_CLASS" \
        --native-dir "$goldens_dir" \
        --cn1-dir "$cn1_dir" \
        --compare-json "$compare_json_out" \
        --title "${platform_title}" \
        --out "$artifacts_dir/cards"; then
      cn1ss_log "  -> Wrote comparison cards to $artifacts_dir/cards (overview: fidelity-overview.png)"
    else
      cn1ss_log "  -> WARNING: failed to render comparison cards (non-fatal)"
    fi
  fi

  # 3) Post the PR comment (best-effort; never the gating signal).
  cn1ss_log "STAGE:FIDELITY_COMMENT_POST -> Submitting fidelity feedback"
  local comment_rc=0
  if [ "${CN1SS_SKIP_COMMENT:-0}" = "1" ]; then
    cn1ss_log "Skipping PR comment as requested (CN1SS_SKIP_COMMENT=1)"
  elif ! cn1ss_post_pr_comment "$comment_out" "$preview_dir"; then
    comment_rc=$?
  fi

  # 4) Baseline update OR ratchet gate.
  local -a gate_args=(--compare-json "$compare_json_out")
  if [ -n "${baseline_file:-}" ] && [ -f "$baseline_file" ]; then
    gate_args+=(--baseline "$baseline_file")
  fi
  if [ -n "${CN1SS_FIDELITY_EPSILON:-}" ]; then
    gate_args+=(--epsilon "$CN1SS_FIDELITY_EPSILON")
  fi
  if [ -n "${CN1SS_FIDELITY_GEOMETRY_EPSILON_PX:-}" ]; then
    gate_args+=(--geometry-epsilon-px "$CN1SS_FIDELITY_GEOMETRY_EPSILON_PX")
  fi
  if [ -n "${CN1SS_FIDELITY_GEOMETRY_EPSILON_RATIO:-}" ]; then
    gate_args+=(--geometry-epsilon-ratio "$CN1SS_FIDELITY_GEOMETRY_EPSILON_RATIO")
  fi
  if [ "${FIDELITY_UPDATE_BASELINE:-0}" = "1" ]; then
    cn1ss_log "WARNING: FIDELITY_UPDATE_BASELINE=1 -- recording current fidelity as the new baseline (gate BYPASSED)."
    if ! cn1ss_java_run "$CN1SS_FIDELITY_GATE_CLASS" "${gate_args[@]}" --update-baseline "$baseline_file"; then
      cn1ss_log "FATAL: Failed to update fidelity baseline"
      return 14
    fi
    return $comment_rc
  fi

  cn1ss_log "STAGE:FIDELITY_GATE -> Enforcing the fidelity ratchet against the baseline"
  if cn1ss_java_run "$CN1SS_FIDELITY_GATE_CLASS" "${gate_args[@]}"; then
    cn1ss_log "Fidelity gate passed."
  else
    local gate_rc=$?
    if [ "${CN1SS_FAIL_ON_MISMATCH:-0}" = "1" ]; then
      cn1ss_log "FATAL: Fidelity gate failed (rc=$gate_rc, CN1SS_FAIL_ON_MISMATCH=1)"
      return 15
    fi
    cn1ss_log "WARNING: Fidelity gate reported regressions (rc=$gate_rc) but CN1SS_FAIL_ON_MISMATCH is not set; not failing."
  fi

  return $comment_rc
}
