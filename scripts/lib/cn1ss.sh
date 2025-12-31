#!/usr/bin/env bash
# Shared helpers for Codename One screenshot (CN1SS) chunk processing

# Default class names used by the Java source helpers
: "${CN1SS_MAIN_CLASS:=Cn1ssChunkTools}"
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

cn1ss_count_chunks() {
  local file="$1"
  local test="${2:-}"
  local channel="${3:-}"
  if [ -z "$file" ] || [ ! -r "$file" ]; then
    echo 0
    return
  fi
  local args=("count" "$file")
  if [ -n "$test" ]; then
    args+=("--test" "$test")
  fi
  if [ -n "$channel" ]; then
    args+=("--channel" "$channel")
  fi
  cn1ss_java_run "$CN1SS_MAIN_CLASS" "${args[@]}"
}

cn1ss_extract_base64() {
  local file="$1"
  local test="${2:-}"
  local channel="${3:-}"
  if [ -z "$file" ] || [ ! -r "$file" ]; then
    return 1
  fi
  local args=("extract" "$file")
  if [ -n "$test" ]; then
    args+=("--test" "$test")
  fi
  if [ -n "$channel" ]; then
    args+=("--channel" "$channel")
  fi
  cn1ss_java_run "$CN1SS_MAIN_CLASS" "${args[@]}"
}

cn1ss_decode_binary() {
  local file="$1"
  local test="${2:-}"
  local channel="${3:-}"
  if [ -z "$file" ] || [ ! -r "$file" ]; then
    return 1
  fi
  local args=("extract" "$file" "--decode")
  if [ -n "$test" ]; then
    args+=("--test" "$test")
  fi
  if [ -n "$channel" ]; then
    args+=("--channel" "$channel")
  fi
  cn1ss_java_run "$CN1SS_MAIN_CLASS" "${args[@]}"
}

cn1ss_list_tests() {
  local file="$1"
  if [ -z "$file" ] || [ ! -r "$file" ]; then
    return 1
  fi
  cn1ss_java_run "$CN1SS_MAIN_CLASS" tests "$file"
}

cn1ss_print_log() {
  local file="$1"
  if [ -z "$file" ] || [ ! -r "$file" ]; then
    return 1
  fi
  cn1ss_java_run "$CN1SS_MAIN_CLASS" check "$file"
}

cn1ss_verify_png() {
  local file="$1"
  [ -s "$file" ] || return 1
  head -c 8 "$file" | od -An -t x1 | tr -d ' \n' | grep -qi '^89504e470d0a1a0a$'
}

cn1ss_verify_jpeg() {
  local file="$1"
  [ -s "$file" ] || return 1
  local header trailer
  header="$(head -c 2 "$file" | od -An -t x1 | tr -d ' \n' | tr '[:lower:]' '[:upper:]')"
  trailer="$(tail -c 2 "$file" | od -An -t x1 | tr -d ' \n' | tr '[:lower:]' '[:upper:]')"
  [ "$header" = "FFD8" ] && [ "$trailer" = "FFD9" ]
}

cn1ss_decode_test_asset() {
  local test="$1"; shift
  local dest="$1"; shift
  local channel="$1"; shift
  local verifier="$1"; shift
  local entry source_type source_path count

  rm -f "$dest" 2>/dev/null || true
  for entry in "$@"; do
    source_type="${entry%%:*}"
    source_path="${entry#*:}"
    [ -s "$source_path" ] || continue
    count="$(cn1ss_count_chunks "$source_path" "$test" "$channel")"
    count="${count//[^0-9]/}"; : "${count:=0}"
    [ "$count" -gt 0 ] || continue
    cn1ss_log "Reassembling test '$test' from ${source_type} source: $source_path (chunks=$count)"
    if cn1ss_decode_binary "$source_path" "$test" "$channel" > "$dest" 2>/dev/null; then
      if [ -z "$verifier" ] || "$verifier" "$dest"; then
        echo "${source_type}:$(basename "$source_path")"
        return 0
      fi
    fi
  done
  rm -f "$dest" 2>/dev/null || true
  return 1
}

cn1ss_decode_test_png() {
  local test="$1"; shift
  local dest="$1"; shift
  cn1ss_decode_test_asset "$test" "$dest" "" cn1ss_verify_png "$@"
}

cn1ss_decode_test_preview() {
  local test="$1"; shift
  local dest="$1"; shift
  cn1ss_decode_test_asset "$test" "$dest" "PREVIEW" cn1ss_verify_jpeg "$@"
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
    for stats_file in "$artifacts_dir"/iphone-builder-stats.txt "$artifacts_dir"/ios-test-stats.txt; do
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

  if [ -n "${CN1SS_EXTRA_MARKDOWN_FILE:-}" ] && [ -f "$CN1SS_EXTRA_MARKDOWN_FILE" ]; then
    cn1ss_log "Appending extra markdown from $CN1SS_EXTRA_MARKDOWN_FILE to comment"
    {
      echo ""
      cat "$CN1SS_EXTRA_MARKDOWN_FILE"
    } >> "$comment_out"
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
  fi

  return $comment_rc
}
