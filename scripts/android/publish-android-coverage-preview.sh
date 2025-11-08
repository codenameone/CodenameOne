#!/usr/bin/env bash
set -euo pipefail

HTML_DIR="artifacts/android-coverage/site/jacoco"
PREVIEW_DIR="artifacts/android-previews"

if [ ! -d "${HTML_DIR}" ] && { [ ! -d "${PREVIEW_DIR}" ] || ! find "${PREVIEW_DIR}" -type f \( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' \) -print -quit >/dev/null; }; then
  echo "No coverage HTML report or screenshot previews generated; skipping preview publication."
  exit 0
fi

tmp_dir=$(mktemp -d)
run_dir="android-runs/${RUN_ID:-manual}-${RUN_ATTEMPT:-0}"
dest_dir="${tmp_dir}/${run_dir}"
mkdir -p "${dest_dir}"

if [ -d "${HTML_DIR}" ]; then
  mkdir -p "${dest_dir}/coverage"
  cp -R "${HTML_DIR}/." "${dest_dir}/coverage/"
fi

if [ -d "${PREVIEW_DIR}" ]; then
  if find "${PREVIEW_DIR}" -type f \( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' \) -print -quit >/dev/null; then
    mkdir -p "${dest_dir}/previews"
    cp -R "${PREVIEW_DIR}/." "${dest_dir}/previews/"
  fi
fi

cat <<'README' > "${tmp_dir}/README.md"
# Android quality previews

This branch is automatically managed by the Android CI workflow and may be force-pushed.
README

git -C "${tmp_dir}" init -b previews >/dev/null
git -C "${tmp_dir}" config user.name "github-actions[bot]"
git -C "${tmp_dir}" config user.email "github-actions[bot]@users.noreply.github.com"
git -C "${tmp_dir}" add .
git -C "${tmp_dir}" commit -m "Publish Android quality previews for run ${RUN_ID} (attempt ${RUN_ATTEMPT})" >/dev/null

remote_url="${SERVER_URL}/${REPOSITORY}.git"
token_remote_url="${remote_url/https:\/\//https://x-access-token:${GITHUB_TOKEN}@}"
git -C "${tmp_dir}" push --force "${token_remote_url}" previews:quality-report-previews >/dev/null

commit_sha=$(git -C "${tmp_dir}" rev-parse HEAD)
raw_base="https://raw.githubusercontent.com/${REPOSITORY}/${commit_sha}/${run_dir}"
preview_base="https://htmlpreview.github.io/?${raw_base}"

if [ -d "${dest_dir}/coverage" ]; then
  echo "coverage_commit=${commit_sha}" >> "$GITHUB_OUTPUT"
  echo "coverage_url=${preview_base}/coverage/index.html" >> "$GITHUB_OUTPUT"
fi

if [ -d "${dest_dir}/previews" ]; then
  echo "preview_base=${raw_base}/previews" >> "$GITHUB_OUTPUT"
fi
