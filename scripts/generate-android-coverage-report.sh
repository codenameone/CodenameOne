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

if [ ! -d "$GRADLE_PROJECT_DIR" ]; then
  cov_log "Gradle project directory not found: $GRADLE_PROJECT_DIR" >&2
  exit 3
fi

if [ ! -x "$GRADLE_PROJECT_DIR/gradlew" ]; then
  cov_log "Gradle wrapper missing at $GRADLE_PROJECT_DIR/gradlew" >&2
  exit 3
fi

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

SUMMARY_OUT="$REPORT_DEST_DIR/coverage-summary.json"
ARTIFACT_NAME="android-coverage-report"
HTML_INDEX="jacocoAndroidReport/html/index.html"

python3 - "$REPORT_DEST_DIR/jacocoAndroidReport.xml" "$SUMMARY_OUT" "$ARTIFACT_NAME" "$HTML_INDEX" <<'PY'
import json
import sys
import os
from pathlib import Path
from xml.etree import ElementTree as ET

xml_path = Path(sys.argv[1])
summary_path = Path(sys.argv[2])
artifact_name = sys.argv[3]
html_index = sys.argv[4]

data = {
    "artifact": artifact_name,
    "html_index": html_index,
    "counters": {},
    "top_classes": [],
}

if not xml_path.is_file():
    json.dump(data, summary_path.open("w", encoding="utf-8"), indent=2)
    sys.exit(0)

def safe_int(value, default=0):
    try:
        return int(value)
    except Exception:
        return default


def format_class_name(package, class_name):
    pkg = package.replace("/", ".").strip(".")
    cls = class_name.replace("/", ".")
    if pkg:
        return f"{pkg}.{cls}"
    return cls


def parse_class_coverage(root):
    classes = []
    for package in root.findall("package"):
        pkg_name = package.get("name", "")
        for cls in package.findall("class"):
            class_name = cls.get("name", "")
            line_counter = None
            for counter in cls.findall("counter"):
                if counter.get("type") == "LINE":
                    line_counter = counter
                    break
            if line_counter is None:
                continue
            missed = safe_int(line_counter.get("missed", 0))
            covered = safe_int(line_counter.get("covered", 0))
            total = missed + covered
            if total <= 0:
                continue
            pct = covered / total * 100.0
            classes.append(
                {
                    "name": format_class_name(pkg_name, class_name),
                    "missed": missed,
                    "covered": covered,
                    "total": total,
                    "coverage": pct,
                }
            )
    classes.sort(key=lambda c: (c["coverage"], -c["total"]))
    return classes[:10]


try:
    tree = ET.parse(xml_path)
except ET.ParseError:
    json.dump(data, summary_path.open("w", encoding="utf-8"), indent=2)
    sys.exit(0)

root = tree.getroot()
for counter in root.findall("counter"):
    ctype = counter.get("type")
    missed = safe_int(counter.get("missed", 0))
    covered = safe_int(counter.get("covered", 0))
    total = missed + covered
    pct = (covered / total * 100.0) if total else 0.0
    data["counters"][ctype] = {
        "missed": missed,
        "covered": covered,
        "total": total,
        "coverage": pct,
    }

data["top_classes"] = parse_class_coverage(root)

html_url_env = os.environ.get("ANDROID_COVERAGE_HTML_URL") or os.environ.get("COVERAGE_HTML_URL")
if html_url_env:
    data["html_url"] = html_url_env

json.dump(data, summary_path.open("w", encoding="utf-8"), indent=2)
PY

cov_log "Copied Jacoco coverage report to $REPORT_DEST_DIR"
if [ -f "$SUMMARY_OUT" ]; then
  cov_log "Wrote coverage summary to $SUMMARY_OUT"
fi
