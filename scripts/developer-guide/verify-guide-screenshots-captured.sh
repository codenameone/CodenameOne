#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
STORAGE_DIR="${CN1_STORAGE_DIR:-${HOME}/.cn1}"
MANIFEST_FILE="${GUIDE_SCREENSHOT_MANIFEST:-${PROJECT_ROOT}/scripts/developer-guide/guide-screenshots.txt}"

if [[ ! -f "${MANIFEST_FILE}" ]]; then
  echo "Guide screenshot manifest not found: ${MANIFEST_FILE}" >&2
  exit 2
fi

if [[ ! -d "${STORAGE_DIR}" ]]; then
  echo "Storage directory ${STORAGE_DIR} does not exist; CN1 screenshot tests did not capture images." >&2
  exit 1
fi

missing_count=0
captured_count=0

while IFS= read -r expected_screenshot; do
  screenshot="${STORAGE_DIR}/${expected_screenshot}"
  if [[ -f "${screenshot}" ]]; then
    size="$(wc -c < "${screenshot}" | tr -d '[:space:]')"
    if [[ "${size}" -eq 0 ]]; then
      echo "Captured guide screenshot is empty: ${screenshot}" >&2
      missing_count=$((missing_count + 1))
      continue
    fi
    echo "Captured guide screenshot: ${screenshot} (${size} bytes)"
    captured_count=$((captured_count + 1))
  else
    echo "Expected guide screenshot was not captured: ${expected_screenshot}" >&2
    missing_count=$((missing_count + 1))
  fi
done < <(sed -e 's/#.*//' -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' "${MANIFEST_FILE}" | awk 'length($0) > 0')

if [[ ${captured_count} -eq 0 ]]; then
  echo "No guide screenshots were captured in ${STORAGE_DIR}." >&2
  exit 1
fi

if [[ ${missing_count} -ne 0 ]]; then
  echo "${missing_count} expected guide screenshot(s) were not captured." >&2
  exit 1
fi

echo "Verified ${captured_count} guide screenshot(s) in ${STORAGE_DIR}."
