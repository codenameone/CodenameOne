#!/usr/bin/env bash
set -euo pipefail

ARTIFACT_ROOT="android-quality-artifacts/coverage"
SOURCE_ROOT="artifacts/android-coverage"

mkdir -p "${ARTIFACT_ROOT}"

if [ -d "${SOURCE_ROOT}/site/jacoco" ]; then
  mkdir -p "${ARTIFACT_ROOT}/html"
  cp -R "${SOURCE_ROOT}/site/jacoco/." "${ARTIFACT_ROOT}/html/"
fi

if [ -d "artifacts/android-previews" ]; then
  mkdir -p "${ARTIFACT_ROOT}/previews"
  cp -R "artifacts/android-previews/." "${ARTIFACT_ROOT}/previews/"
fi

for file in coverage.ec coverage.json jacoco-report.log; do
  if [ -f "${SOURCE_ROOT}/${file}" ]; then
    cp "${SOURCE_ROOT}/${file}" "${ARTIFACT_ROOT}/"
  fi
done
