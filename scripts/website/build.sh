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
HUGO_BASEURL="${HUGO_BASEURL:-https://www.codenameone.com/}"
PYTHON_BIN="${PYTHON_BIN:-python3}"
WEBSITE_INCLUDE_JAVADOCS="${WEBSITE_INCLUDE_JAVADOCS:-false}"
WEBSITE_INCLUDE_DEVGUIDE="${WEBSITE_INCLUDE_DEVGUIDE:-false}"

build_javadocs_for_site() {
  if [ "${WEBSITE_INCLUDE_JAVADOCS}" != "true" ]; then
    return
  fi

  echo "Building fresh JavaDocs for website..." >&2
  (
    cd "${REPO_ROOT}"
    ./.github/scripts/build_javadocs.sh
  )

  rm -rf "${WEBSITE_DIR}/static/javadoc"
  mkdir -p "${WEBSITE_DIR}/static/javadoc" "${WEBSITE_DIR}/static/files"
  cp -a "${REPO_ROOT}/CodenameOne/dist/javadoc/." "${WEBSITE_DIR}/static/javadoc/"
  cp "${REPO_ROOT}/CodenameOne/javadocs.zip" "${WEBSITE_DIR}/static/files/javadocs.zip"
}

build_developer_guide_for_site() {
  if [ "${WEBSITE_INCLUDE_DEVGUIDE}" != "true" ]; then
    return
  fi

  if ! command -v asciidoctor >/dev/null 2>&1; then
    echo "Asciidoctor tooling is required when WEBSITE_INCLUDE_DEVGUIDE=true." >&2
    exit 1
  fi

  echo "Building fresh Developer Guide for website..." >&2
  local output_root="${REPO_ROOT}/build/website-developer-guide"
  local html_out="${output_root}/html"
  local manual_dir="${WEBSITE_DIR}/static/manual"
  local source_dir="${REPO_ROOT}/docs/developer-guide"

  rm -rf "${output_root}" "${manual_dir}"
  mkdir -p "${html_out}" "${manual_dir}"

  (
    cd "${REPO_ROOT}"
    asciidoctor \
      -D "${html_out}" \
      -o developer-guide.html \
      docs/developer-guide/developer-guide.asciidoc

  )

  cp "${html_out}/developer-guide.html" "${manual_dir}/index.html"
  # Keep assets next to /manual/index.html exactly where generated HTML resolves them.
  rsync -a \
    --exclude 'sketch/' \
    --exclude '*.asciidoc' \
    --exclude '*.adoc' \
    "${source_dir}/" "${manual_dir}/"
}

if ! command -v "${HUGO_BIN}" >/dev/null 2>&1; then
  echo "Hugo binary not found. Install Hugo (extended) and retry." >&2
  exit 1
fi

build_javadocs_for_site
build_developer_guide_for_site

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
  --baseURL "${HUGO_BASEURL}" \
  ${MINIFY_FLAG}

if command -v "${PYTHON_BIN}" >/dev/null 2>&1; then
  "${PYTHON_BIN}" "${WEBSITE_DIR}/scripts/generate_lunr_index.py"
else
  echo "Warning: python3 not found; skipping lunr index generation." >&2
fi
