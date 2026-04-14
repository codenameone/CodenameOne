#!/usr/bin/env bash
set -euo pipefail

BASE_SHA="${DOCSTYLE_BASE_SHA:-}"
HEAD_SHA="${DOCSTYLE_HEAD_SHA:-HEAD}"

if [ -z "$BASE_SHA" ]; then
  if git rev-parse HEAD^ >/dev/null 2>&1; then
    BASE_SHA="HEAD^"
  else
    echo "Unable to determine base commit. Set DOCSTYLE_BASE_SHA." >&2
    exit 1
  fi
fi

TARGET_DIRS=("CodenameOne" "Ports/CLDC11")

echo "Validating Java 25 markdown docs style in ${TARGET_DIRS[*]} between ${BASE_SHA}..${HEAD_SHA}"

package_violations=$(git diff --name-status --diff-filter=AR "$BASE_SHA" "$HEAD_SHA" -- "${TARGET_DIRS[@]}" | awk '$2 ~ /package\.html$/ {print $0}')

comment_violations=$(git diff -U0 "$BASE_SHA" "$HEAD_SHA" -- "${TARGET_DIRS[@]}" -- '*.java' | awk '
  /^\+\+\+/ { next }
  /^\+/ && $0 ~ /^\+[[:space:]]*\/\*\*/ { print }
')

failed=0

if [ -n "$package_violations" ]; then
  failed=1
  echo "ERROR: package.html files were added or renamed into scope. Use package-info.java with markdown doc comments instead:" >&2
  echo "$package_violations" >&2
fi

if [ -n "$comment_violations" ]; then
  failed=1
  echo "ERROR: Classic Javadoc block comments (/**) were added in Java files. Use Java 25 markdown docs with /// instead:" >&2
  echo "$comment_violations" >&2
fi

if [ "$failed" -ne 0 ]; then
  exit 1
fi

echo "Validation passed: no new /** JavaDoc blocks or package.html files were introduced."
