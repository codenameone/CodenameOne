#!/usr/bin/env bash
# Generate a Jacoco coverage report for the Android sample app
set -euo pipefail

cov_log() { echo "[generate-android-coverage] $1"; }

if [ $# -lt 1 ]; then
  cov_log "Usage: $0 <gradle_project_dir>" >&2
  exit 2
fi

GRADLE_PROJECT_DIR="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
mkdir -p "$ARTIFACTS_DIR"
REPORT_DEST_DIR="$ARTIFACTS_DIR/android-coverage-report"

cov_log "Loading workspace environment from $ENV_FILE"
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  cov_log "Loaded environment: JAVA_HOME=${JAVA_HOME:-<unset>} JAVA17_HOME=${JAVA17_HOME:-<unset>}"
else
  cov_log "Workspace tools not found. Run scripts/setup-workspace.sh before this script." >&2
  exit 1
fi

if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  cov_log "JAVA17_HOME validation failed. Current value: ${JAVA17_HOME:-<unset>}" >&2
  exit 1
fi

if [ ! -d "$GRADLE_PROJECT_DIR" ]; then
  cov_log "Gradle project directory not found: $GRADLE_PROJECT_DIR" >&2
  exit 3
fi

if [ ! -x "$GRADLE_PROJECT_DIR/gradlew" ]; then
  cov_log "Gradle wrapper missing at $GRADLE_PROJECT_DIR/gradlew" >&2
  exit 3
fi

ORIGINAL_JAVA_HOME="${JAVA_HOME:-}";
export JAVA_HOME="$JAVA17_HOME"

cov_log "Running jacocoAndroidReport in $GRADLE_PROJECT_DIR"
(
  cd "$GRADLE_PROJECT_DIR"
  ./gradlew --no-daemon jacocoAndroidReport
)

export JAVA_HOME="$ORIGINAL_JAVA_HOME"

COVERAGE_SCAN=$(find "$GRADLE_PROJECT_DIR/app/build" -type f \( -name "*.ec" -o -name "*.exec" \) 2>/dev/null | sed 's/^/[generate-android-coverage] coverage-file: /')
if [ -n "$COVERAGE_SCAN" ]; then
  echo "$COVERAGE_SCAN"
else
  cov_log "No coverage data files detected under $GRADLE_PROJECT_DIR/app/build"
fi

REPORT_SOURCE_DIR="$GRADLE_PROJECT_DIR/app/build/reports/jacoco/jacocoAndroidReport"
if [ ! -d "$REPORT_SOURCE_DIR" ]; then
  cov_log "Coverage report directory not found: $REPORT_SOURCE_DIR" >&2
  exit 4
fi

rm -rf "$REPORT_DEST_DIR"
mkdir -p "$REPORT_DEST_DIR"
cp -R "$REPORT_SOURCE_DIR"/ "${REPORT_DEST_DIR}"/

cov_log "Copied Jacoco coverage report to $REPORT_DEST_DIR"
