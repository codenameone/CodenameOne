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
HUGO_PORT="${HUGO_PORT:-1313}"
HUGO_BIND="${HUGO_BIND:-127.0.0.1}"
HUGO_BASEURL="${HUGO_BASEURL:-http://${HUGO_BIND}:${HUGO_PORT}/}"
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

# Generate static output once so lunr index can be created before live preview.
"${HUGO_BIN}" --destination "${WEBSITE_DIR}/public" >/dev/null 2>&1 || true

if command -v "${PYTHON_BIN}" >/dev/null 2>&1; then
  "${PYTHON_BIN}" "${WEBSITE_DIR}/scripts/generate_lunr_index.py" >/dev/null 2>&1 || true
else
  echo "Warning: python3 not found; search index may be stale in preview." >&2
fi

"${HUGO_BIN}" server \
  --bind "${HUGO_BIND}" \
  --port "${HUGO_PORT}" \
  --baseURL "${HUGO_BASEURL}" \
  --buildDrafts \
  --buildFuture \
  --disableFastRender
