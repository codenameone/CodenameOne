#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MANIFEST_FILE="${GUIDE_SCREENSHOT_MANIFEST:-${PROJECT_ROOT}/scripts/developer-guide/guide-screenshots.txt}"
GUIDE_DIR="${PROJECT_ROOT}/docs/developer-guide"

if [[ ! -f "${MANIFEST_FILE}" ]]; then
  echo "Guide screenshot manifest not found: ${MANIFEST_FILE}" >&2
  exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

manifest_sorted="${tmp_dir}/manifest.txt"
referenced_sorted="${tmp_dir}/referenced.txt"
generated_refs_sorted="${tmp_dir}/generated-refs.txt"

grep -v '^[[:space:]]*$' "${MANIFEST_FILE}" | sort -u > "${manifest_sorted}"

rg -o 'img/[A-Za-z0-9._/-]+\.(png|jpg|jpeg)' "${GUIDE_DIR}" \
  | sed -E 's#.*img/##' \
  | sort -u > "${referenced_sorted}"

grep -E '^(layout-animation-[0-9]+|transition-[A-Za-z0-9-]+|mighty-morphing-components-1)\.png$' \
  "${referenced_sorted}" > "${generated_refs_sorted}" || true

missing_from_manifest="$(comm -23 "${generated_refs_sorted}" "${manifest_sorted}")"
if [[ -n "${missing_from_manifest}" ]]; then
  echo "Generated guide screenshot reference(s) missing from manifest:" >&2
  echo "${missing_from_manifest}" >&2
  exit 1
fi

missing_from_guide="$(comm -23 "${manifest_sorted}" "${referenced_sorted}")"
if [[ -n "${missing_from_guide}" ]]; then
  echo "Guide screenshot manifest entry/entries not referenced by developer guide:" >&2
  echo "${missing_from_guide}" >&2
  exit 1
fi

echo "Guide screenshot manifest covers $(wc -l < "${manifest_sorted}" | tr -d '[:space:]') generated screenshot reference(s)."
