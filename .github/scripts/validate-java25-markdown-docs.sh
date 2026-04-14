#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT=""
HAS_GIT_REPO=0

if git rev-parse --show-toplevel >/dev/null 2>&1; then
  REPO_ROOT="$(git rev-parse --show-toplevel)"
elif git -C "${SCRIPT_DIR}" rev-parse --show-toplevel >/dev/null 2>&1; then
  REPO_ROOT="$(git -C "${SCRIPT_DIR}" rev-parse --show-toplevel)"
elif [ -n "${GITHUB_WORKSPACE:-}" ] && git -C "${GITHUB_WORKSPACE}" rev-parse --show-toplevel >/dev/null 2>&1; then
  REPO_ROOT="$(git -C "${GITHUB_WORKSPACE}" rev-parse --show-toplevel)"
else
  CANDIDATE_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
  if [ -d "${CANDIDATE_ROOT}/.git" ] || [ -f "${CANDIDATE_ROOT}/.git" ]; then
    REPO_ROOT="${CANDIDATE_ROOT}"
  fi
fi

if [ -n "${REPO_ROOT}" ] && git -C "${REPO_ROOT}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  HAS_GIT_REPO=1
fi

BASE_SHA="${DOCSTYLE_BASE_SHA:-}"
HEAD_SHA="${DOCSTYLE_HEAD_SHA:-HEAD}"

if [ -z "$BASE_SHA" ]; then
  if git -C "${REPO_ROOT}" rev-parse HEAD^ >/dev/null 2>&1; then
    BASE_SHA="HEAD^"
  else
    echo "Unable to determine base commit. Set DOCSTYLE_BASE_SHA." >&2
    exit 1
  fi
fi

TARGET_DIRS=("CodenameOne" "Ports/CLDC11")

if [ "${HAS_GIT_REPO}" -eq 1 ]; then
  if ! git -C "${REPO_ROOT}" cat-file -e "${BASE_SHA}^{commit}" >/dev/null 2>&1; then
    echo "Base commit ${BASE_SHA} is not available locally. Attempting to fetch it." >&2
    git -C "${REPO_ROOT}" fetch --no-tags --depth=200 origin "${BASE_SHA}" >/dev/null 2>&1 || true
  fi

  if ! git -C "${REPO_ROOT}" cat-file -e "${HEAD_SHA}^{commit}" >/dev/null 2>&1; then
    echo "Head commit ${HEAD_SHA} is not available locally. Attempting to fetch it." >&2
    git -C "${REPO_ROOT}" fetch --no-tags --depth=200 origin "${HEAD_SHA}" >/dev/null 2>&1 || true
  fi

  if ! git -C "${REPO_ROOT}" cat-file -e "${BASE_SHA}^{commit}" >/dev/null 2>&1 || \
     ! git -C "${REPO_ROOT}" cat-file -e "${HEAD_SHA}^{commit}" >/dev/null 2>&1; then
    echo "Unable to resolve commit range ${BASE_SHA}..${HEAD_SHA} in local git repo." >&2
    HAS_GIT_REPO=0
  fi
fi

echo "Validating Java 25 markdown docs style in ${TARGET_DIRS[*]} between ${BASE_SHA}..${HEAD_SHA}"

package_violations=""
comment_violations=""

if [ "${HAS_GIT_REPO}" -eq 1 ]; then
  package_violations=$(git -C "${REPO_ROOT}" diff --name-status --diff-filter=AR "$BASE_SHA" "$HEAD_SHA" -- "${TARGET_DIRS[@]}" | awk '$2 ~ /package\.html$/ {print $0}')

  comment_violations=$(git -C "${REPO_ROOT}" diff -U0 "$BASE_SHA" "$HEAD_SHA" -- "${TARGET_DIRS[@]}" -- '*.java' | awk '
    /^\+\+\+/ { next }
    /^\+/ && $0 ~ /^\+[[:space:]]*\/\*\*/ { print }
  ')
else
  if [ -z "${GITHUB_TOKEN:-}" ] || [ -z "${GITHUB_REPOSITORY:-}" ]; then
    echo "No local git repo available and missing GITHUB_TOKEN/GITHUB_REPOSITORY for API fallback." >&2
    exit 1
  fi

  compare_url="https://api.github.com/repos/${GITHUB_REPOSITORY}/compare/${BASE_SHA}...${HEAD_SHA}"
  compare_json="$(curl -fsSL -H "Authorization: Bearer ${GITHUB_TOKEN}" -H "Accept: application/vnd.github+json" "${compare_url}")"

  parsed_output="$(printf '%s' "${compare_json}" | python3 - <<'PY'
import json
import re
import sys

data = json.load(sys.stdin)
targets = ("CodenameOne/", "Ports/CLDC11/")
package_hits = []
comment_hits = []

for f in data.get("files", []):
    status = f.get("status", "")
    filename = f.get("filename", "")
    if filename.startswith(targets) and filename.endswith("package.html") and status in ("added", "renamed"):
        package_hits.append(f"{status}\t{filename}")
    if filename.startswith(targets) and filename.endswith(".java"):
        patch = f.get("patch") or ""
        for line in patch.splitlines():
            if line.startswith("+++") or not line.startswith("+"):
                continue
            if re.match(r"^\+\s*/\*\*", line):
                comment_hits.append(f"{filename}: {line}")

print("PACKAGE_START")
print("\n".join(package_hits))
print("PACKAGE_END")
print("COMMENT_START")
print("\n".join(comment_hits))
print("COMMENT_END")
PY
)"

  package_violations="$(printf '%s\n' "${parsed_output}" | sed -n '/^PACKAGE_START$/,/^PACKAGE_END$/p' | sed '1d;$d')"
  comment_violations="$(printf '%s\n' "${parsed_output}" | sed -n '/^COMMENT_START$/,/^COMMENT_END$/p' | sed '1d;$d')"
fi

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
