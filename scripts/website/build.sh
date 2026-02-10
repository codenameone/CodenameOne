#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
WEBSITE_DIR="${REPO_ROOT}/docs/website"

if [ ! -d "${WEBSITE_DIR}" ]; then
  echo "Website directory not found: ${WEBSITE_DIR}" >&2
  exit 1
fi

HUGO_BIN="${HUGO_BIN:-hugo}"
HUGO_ENVIRONMENT="${HUGO_ENVIRONMENT:-production}"
HUGO_MINIFY="${HUGO_MINIFY:-true}"
PYTHON_BIN="${PYTHON_BIN:-python3}"

if ! command -v "${HUGO_BIN}" >/dev/null 2>&1; then
  echo "Hugo binary not found. Install Hugo (extended) and retry." >&2
  exit 1
fi

cd "${WEBSITE_DIR}"

if command -v "${PYTHON_BIN}" >/dev/null 2>&1; then
  "${PYTHON_BIN}" "${WEBSITE_DIR}/scripts/generate_cn1libs.py"
else
  echo "Warning: python3 not found; skipping cn1libs refresh." >&2
fi

MINIFY_FLAG=""
if [ "${HUGO_MINIFY}" = "true" ]; then
  MINIFY_FLAG="--minify"
fi

HUGO_ENV="${HUGO_ENVIRONMENT}" "${HUGO_BIN}" \
  --cleanDestinationDir \
  --gc \
  ${MINIFY_FLAG}
