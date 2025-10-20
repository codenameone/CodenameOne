#!/usr/bin/env bash
# Shared helpers for Codename One screenshot (CN1SS) chunk processing

# Default class names used by the Java source helpers
: "${CN1SS_MAIN_CLASS:=Cn1ssChunkTools}"
: "${CN1SS_PROCESS_CLASS:=ProcessScreenshots}"
: "${CN1SS_RENDER_CLASS:=RenderScreenshotReport}"
: "${CN1SS_POST_COMMENT_CLASS:=PostPrComment}"

cn1ss_setup() {
  CN1SS_JAVA_BIN="$1"
  CN1SS_SOURCE_PATH="$2"
}

cn1ss_log() {
  echo "[cn1ss] $*"
}

cn1ss_java_source() {
  local class_name="$1"; shift
  local source_file="${CN1SS_SOURCE_PATH%/}/$class_name.java"
  if [ -z "${CN1SS_JAVA_BIN:-}" ] || [ ! -x "$CN1SS_JAVA_BIN" ]; then
    cn1ss_log "CN1SS_JAVA_BIN is not configured"
    return 1
  fi
  if [ ! -f "$source_file" ]; then
    cn1ss_log "Missing CN1SS helper source: $source_file"
    return 1
  fi
  "$CN1SS_JAVA_BIN" "$source_file" "$@"
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
  cn1ss_java_source "$CN1SS_MAIN_CLASS" "${args[@]}" 2>/dev/null || echo 0
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
  cn1ss_java_source "$CN1SS_MAIN_CLASS" "${args[@]}"
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
  cn1ss_java_source "$CN1SS_MAIN_CLASS" "${args[@]}"
}

cn1ss_list_tests() {
  local file="$1"
  if [ -z "$file" ] || [ ! -r "$file" ]; then
    return 1
  fi
  cn1ss_java_source "$CN1SS_MAIN_CLASS" tests "$file"
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
  GITHUB_TOKEN="$comment_token" cn1ss_java_source "$CN1SS_POST_COMMENT_CLASS" \
    --body "$body_file" \
    --preview-dir "$preview_dir"
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
