#!/usr/bin/env bash
#
# Validate GPLv2 + Classpath Exception headers on added or modified source
# files.  Existing source may retain the historical Oracle header; newly
# added source must use the Codename One header.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
EXCLUSIONS_FILE="$SCRIPT_DIR/copyright-header-exclusions.txt"

usage() {
  cat <<'EOF'
Usage:
  scripts/check-copyright-headers.sh
  scripts/check-copyright-headers.sh --base REV [--head REV]
  scripts/check-copyright-headers.sh --all [PATH ...]
  scripts/check-copyright-headers.sh PATH [PATH ...]

With no arguments, checks added and modified working-tree files.  --base is
intended for CI.  --all checks tracked source files, optionally below the
given paths.
EOF
}

is_source_file() {
  case "$1" in
    *.java|*.js|*.jsx|*.ts|*.tsx|*.css|*.c|*.cc|*.cpp|*.cxx|*.h|*.hh|*.hpp|*.hxx|*.m|*.mm|*.metal|*.kt|*.kts|*.swift|*.cs) return 0 ;;
    *) return 1 ;;
  esac
}

is_excluded() {
  local candidate="$1"
  awk -F '\\|' -v candidate="$candidate" '
    /^[[:space:]]*(#|$)/ { next }
    {
      path = $1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", path)
      if (path == candidate) found = 1
    }
    END { exit(found ? 0 : 1) }
  ' "$EXCLUSIONS_FILE"
}

validate_exclusions() {
  local failed=0
  local seen_file="$1"
  : > "$seen_file"

  while IFS='|' read -r raw_path raw_reason; do
    local path reason
    path="$(printf '%s' "$raw_path" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    reason="$(printf '%s' "${raw_reason:-}" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    case "$path" in
      ''|'#'*) continue ;;
    esac

    if [[ -z "$reason" ]]; then
      echo "check-copyright-headers: exclusion lacks a rationale: $path" >&2
      failed=1
    fi
    if [[ "$path" = /* || "$path" == *'*'* || "$path" == *'?'* || "$path" == */ ]]; then
      echo "check-copyright-headers: exclusions must be exact repository-relative files: $path" >&2
      failed=1
    fi
    if grep -Fqx "$path" "$seen_file"; then
      echo "check-copyright-headers: duplicate exclusion: $path" >&2
      failed=1
    else
      printf '%s\n' "$path" >> "$seen_file"
    fi
    if [[ ! -f "$REPO_ROOT/$path" ]]; then
      echo "check-copyright-headers: excluded file does not exist: $path" >&2
      failed=1
    elif ! is_source_file "$path"; then
      echo "check-copyright-headers: exclusion is not a checked source type: $path" >&2
      failed=1
    fi
  done < "$EXCLUSIONS_FILE"

  return "$failed"
}

header_kind() {
  local file="$1"
  local header_file="$2"
  sed -n '1,40p' "$file" > "$header_file"

  if [[ "$(awk 'NF { print; exit }' "$header_file")" != '/*' ]]; then
    return 1
  fi

  grep -Fq 'DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.' "$header_file" || return 1
  grep -Fq 'This code is free software; you can redistribute it and/or modify it' "$header_file" || return 1
  grep -Fq 'GNU General Public License version 2 only' "$header_file" || return 1
  grep -Fq 'particular file as subject to the "Classpath" exception' "$header_file" || return 1
  grep -Fq 'but WITHOUT' "$header_file" || return 1
  grep -Fq 'ANY WARRANTY' "$header_file" || return 1

  if grep -Eq '^ \* Copyright \(c\) [0-9]{4}([,-][[:space:]]*[0-9]{4})*, Codename One and/or its affiliates\. All rights reserved\.$' "$header_file" \
      && grep -Fq 'Codename One designates this' "$header_file" \
      && grep -Fq 'Please contact Codename One ' "$header_file"; then
    printf '%s\n' codename-one
    return 0
  fi

  if grep -Eq '^ \* Copyright \(c\) [0-9]{4}([,-][[:space:]]*[0-9]{4})*, Oracle and/or its affiliates\. All rights reserved\.$' "$header_file" \
      && grep -Fq 'Oracle designates this' "$header_file" \
      && grep -Fq 'Please contact Oracle' "$header_file"; then
    printf '%s\n' oracle
    return 0
  fi

  return 1
}

MODE=working
BASE=''
HEAD=HEAD
PATHS_FILE="$(mktemp -t cn1-copyright-paths.XXXXXX)"
NEW_FILES="$(mktemp -t cn1-copyright-new.XXXXXX)"
SEEN_EXCLUSIONS="$(mktemp -t cn1-copyright-exclusions.XXXXXX)"
HEADER_FILE="$(mktemp -t cn1-copyright-header.XXXXXX)"
BASE_FILE="$(mktemp -t cn1-copyright-base.XXXXXX)"
BASE_HEADER_FILE="$(mktemp -t cn1-copyright-base-header.XXXXXX)"
trap 'rm -f "$PATHS_FILE" "$NEW_FILES" "$SEEN_EXCLUSIONS" "$HEADER_FILE" "$BASE_FILE" "$BASE_HEADER_FILE"' EXIT
: > "$PATHS_FILE"
: > "$NEW_FILES"
REFERENCE=''

POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      [[ $# -ge 2 ]] || { usage >&2; exit 2; }
      MODE=base
      BASE="$2"
      shift 2
      ;;
    --head)
      [[ $# -ge 2 ]] || { usage >&2; exit 2; }
      HEAD="$2"
      shift 2
      ;;
    --all)
      MODE=all
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    --)
      shift
      while [[ $# -gt 0 ]]; do POSITIONAL+=("$1"); shift; done
      ;;
    -*)
      echo "check-copyright-headers: unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
    *)
      POSITIONAL+=("$1")
      shift
      ;;
  esac
done

cd "$REPO_ROOT"

if [[ ! -f "$EXCLUSIONS_FILE" ]]; then
  echo "check-copyright-headers: exclusions file not found: $EXCLUSIONS_FILE" >&2
  exit 2
fi
if ! validate_exclusions "$SEEN_EXCLUSIONS"; then
  exit 2
fi

case "$MODE" in
  base)
    if [[ -z "$BASE" ]] || ! git cat-file -e "$BASE^{commit}" 2>/dev/null; then
      echo "check-copyright-headers: base revision not found: $BASE" >&2
      exit 2
    fi
    if ! git cat-file -e "$HEAD^{commit}" 2>/dev/null; then
      echo "check-copyright-headers: head revision not found: $HEAD" >&2
      exit 2
    fi
    git diff --name-only --diff-filter=AMR "$BASE" "$HEAD" -- > "$PATHS_FILE"
    git diff --name-only --diff-filter=A "$BASE" "$HEAD" -- > "$NEW_FILES"
    REFERENCE="$BASE"
    ;;
  all)
    if [[ ${#POSITIONAL[@]} -eq 0 ]]; then
      git ls-files > "$PATHS_FILE"
    else
      for path in "${POSITIONAL[@]}"; do
        if [[ -f "$path" ]]; then
          printf '%s\n' "${path#./}" >> "$PATHS_FILE"
        else
          git ls-files -- "${path#./}" >> "$PATHS_FILE"
        fi
      done
    fi
    ;;
  working)
    if [[ ${#POSITIONAL[@]} -gt 0 ]]; then
      for path in "${POSITIONAL[@]}"; do
        if [[ -f "$path" ]]; then
          printf '%s\n' "${path#./}" >> "$PATHS_FILE"
        else
          git ls-files -- "${path#./}" >> "$PATHS_FILE"
        fi
      done
    else
      git diff --name-only --diff-filter=AMR HEAD -- > "$PATHS_FILE"
      git ls-files --others --exclude-standard >> "$PATHS_FILE"
      git ls-files --others --exclude-standard > "$NEW_FILES"
      REFERENCE=HEAD
    fi
    ;;
esac

sort -u "$PATHS_FILE" -o "$PATHS_FILE"
sort -u "$NEW_FILES" -o "$NEW_FILES"

checked=0
excluded=0
failed=0
while IFS= read -r path; do
  path="${path#./}"
  [[ -n "$path" && -f "$path" ]] || continue
  is_source_file "$path" || continue

  if is_excluded "$path"; then
    excluded=$((excluded + 1))
    continue
  fi

  checked=$((checked + 1))
  kind=''
  if kind="$(header_kind "$path" "$HEADER_FILE")"; then
    if grep -Fqx "$path" "$NEW_FILES" && [[ "$kind" != codename-one ]]; then
      echo "$path: newly added source files must use the Codename One GPLv2 + Classpath Exception header" >&2
      failed=$((failed + 1))
    elif [[ "$kind" == oracle && -n "$REFERENCE" ]]; then
      base_kind=''
      if ! git show "$REFERENCE:$path" > "$BASE_FILE" 2>/dev/null \
          || ! base_kind="$(header_kind "$BASE_FILE" "$BASE_HEADER_FILE")" \
          || [[ "$base_kind" != oracle ]]; then
        echo "$path: the Oracle header is only allowed when the base version already used it" >&2
        echo "  Use the complete Codename One GPLv2 + Classpath Exception header for other source." >&2
        failed=$((failed + 1))
      fi
    fi
  else
    echo "$path: missing or unsupported copyright header" >&2
    echo "  Use the complete Codename One GPLv2 + Classpath Exception header." >&2
    echo "  Existing older files may retain the complete Oracle GPLv2 + Classpath Exception header." >&2
    failed=$((failed + 1))
  fi
done < "$PATHS_FILE"

if [[ "$failed" -ne 0 ]]; then
  echo "check-copyright-headers: $failed file(s) failed; $checked checked, $excluded explicitly excluded." >&2
  exit 1
fi

echo "check-copyright-headers: $checked file(s) passed; $excluded explicitly excluded."
