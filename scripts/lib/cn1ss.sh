#!/usr/bin/env bash
# Shared helpers for Codename One screenshot (CN1SS) chunk processing

# Default class names used by the Java source helpers
: "${CN1SS_PROCESS_CLASS:=ProcessScreenshots}"
: "${CN1SS_RENDER_CLASS:=RenderScreenshotReport}"
: "${CN1SS_POST_COMMENT_CLASS:=PostPrComment}"

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

  local need_compile=1
  if [ -d "$CN1SS_CLASS_DIR" ] && [ -f "$CN1SS_STAMP_FILE" ]; then
    if ! find "$CN1SS_SOURCE_PATH" -type f -name '*.java' -newer "$CN1SS_STAMP_FILE" -print -quit | grep -q .; then
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
    touch "$CN1SS_STAMP_FILE" 2>/dev/null || true
  else
    cn1ss_log "Reusing CN1SS helpers in $CN1SS_CLASS_DIR"
  fi

  CN1SS_JAVA_CLASSPATH="$CN1SS_CLASS_DIR"
  CN1SS_INITIALIZED=1
}

cn1ss_log() {
  echo "[cn1ss] $*"
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
    for stats_file in "$artifacts_dir"/iphone-builder-stats.txt "$artifacts_dir"/ios-test-stats.txt "$artifacts_dir"/android-test-stats.txt "$artifacts_dir"/base64-performance-stats.txt; do
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

    # Missing-screenshot regression guard. Every expected test must produce its
    # screenshot; a test that runs but emits nothing is recorded as status
    # "missing_actual". When a test hangs or the rendering pipeline crashes
    # partway (the Metal DialogTheme hang), every test from that point on
    # becomes missing_actual and the suite silently drops from 122 captures to
    # 107 - all still listed, just unproduced. We fail when the number of
    # missing screenshots exceeds CN1SS_ALLOWED_MISSING (default 0: no missing
    # screenshots tolerated). A pipeline with a known, steady-state gap raises
    # its own tolerance (e.g. the iOS jobs set CN1SS_ALLOWED_MISSING=2 for
    # OrientationLock + MutableImageReadback, which do not render on either iOS
    # backend). Set CN1SS_SKIP_COUNT_CHECK=1 to bypass while intentionally
    # seeding a brand new reference set. Enforced on every pipeline that opts
    # into strict mode (CN1SS_FAIL_ON_MISMATCH=1).
    if [ "${CN1SS_SKIP_COUNT_CHECK:-0}" != "1" ]; then
      local missing_count allowed_missing
      missing_count=$(cn1ss_count_missing "$compare_json_out")
      missing_count="${missing_count//[^0-9]/}"; : "${missing_count:=999999}"
      allowed_missing="${CN1SS_ALLOWED_MISSING:-0}"
      allowed_missing="${allowed_missing//[^0-9]/}"; : "${allowed_missing:=0}"
      if [ "$missing_count" -gt "$allowed_missing" ]; then
        cn1ss_log "FATAL: $missing_count screenshot(s) missing (no image produced) but only $allowed_missing tolerated (CN1SS_ALLOWED_MISSING)."
        cn1ss_log "       A test failed to emit its screenshot - the suite likely hung or crashed before finishing. See the 'missing actual' entries above."
        return 17
      fi
      cn1ss_log "Missing-screenshot check passed: $missing_count missing <= $allowed_missing tolerated."
    fi
  fi

  return $comment_rc
}
