#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BASELINE_DIR="${BASELINE_DIR:-${PROJECT_ROOT}/docs/developer-guide/img}"
STORAGE_DIR="${CN1_STORAGE_DIR:-${HOME}/.cn1}"
ARTIFACT_DIR="${1:-${PROJECT_ROOT}/docs/demos/animation-screenshot-artifacts}"

if ! command -v compare >/dev/null 2>&1; then
  echo "ImageMagick 'compare' command is required." >&2
  exit 2
fi

mkdir -p "${ARTIFACT_DIR}"
find "${ARTIFACT_DIR}" -mindepth 1 -delete

if [[ ! -d "${STORAGE_DIR}" ]]; then
  echo "Storage directory ${STORAGE_DIR} does not exist; nothing to compare."
  exit 0
fi

shopt -s nullglob
mapfile -d '' SCREENSHOTS < <(find "${STORAGE_DIR}" -type f -name '*.png' -print0)
shopt -u nullglob

if [[ ${#SCREENSHOTS[@]} -eq 0 ]]; then
  echo "No screenshots found under ${STORAGE_DIR}; nothing to compare."
  exit 0
fi

mismatch_count=0

for screenshot in "${SCREENSHOTS[@]}"; do
  filename="$(basename "${screenshot}")"
  base="${filename%.png}"

  baseline=""
  baseline_ext=""
  for ext in png PNG jpg JPG jpeg JPEG; do
    candidate="${BASELINE_DIR}/${base}.${ext}"
    if [[ -f "${candidate}" ]]; then
      baseline="${candidate}"
      baseline_ext="${ext}"
      break
    fi
  done

  if [[ -z "${baseline}" ]]; then
    echo "No baseline found for ${filename}; treating as mismatch." >&2
    mismatch_count=$((mismatch_count + 1))
    cp "${screenshot}" "${ARTIFACT_DIR}/${filename}"
    rm -f "${screenshot}"
    continue
  fi

  metric_file="$(mktemp)"
  diff_image="${ARTIFACT_DIR}/${base}.diff.png"

  set +e
  compare -metric AE "${screenshot}" "${baseline}" "${diff_image}" 2>"${metric_file}"
  status=$?
  set -e

  if [[ ${status} -eq 0 ]]; then
    rm -f "${screenshot}" "${diff_image}" "${metric_file}"
    echo "${filename} matches baseline; capture discarded."
    continue
  fi

  if [[ ${status} -ne 1 ]]; then
    cat "${metric_file}" >&2
    rm -f "${metric_file}" "${diff_image}"
    echo "ImageMagick comparison failed for ${filename}." >&2
    exit ${status}
  fi

  mismatch_count=$((mismatch_count + 1))
  metric_value="$(cat "${metric_file}")"
  rm -f "${metric_file}"

  cp "${screenshot}" "${ARTIFACT_DIR}/${filename}"
  cp "${baseline}" "${ARTIFACT_DIR}/${base}.baseline.${baseline_ext}"
  echo "${metric_value}" > "${ARTIFACT_DIR}/${base}.metric.txt"
  rm -f "${screenshot}"
  echo "${filename} differs from baseline; artifacts stored in ${ARTIFACT_DIR}." >&2
done

if [[ ${mismatch_count} -eq 0 ]]; then
  echo "All screenshots match their baselines."
  exit 0
fi

echo "${mismatch_count} screenshot(s) differ from the documented baseline." >&2
exit 1
