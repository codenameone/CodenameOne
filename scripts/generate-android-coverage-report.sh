#!/usr/bin/env bash
# Generate a Jacoco coverage report for the Android sample app
set -euo pipefail

cov_log() { echo "[generate-android-coverage] $1"; }

publish_coverage_preview() {
  local source_dir="$1" html_index="$2"
  local server_url="${GITHUB_SERVER_URL:-}" repository="${GITHUB_REPOSITORY:-}" token="${GITHUB_TOKEN:-${GH_TOKEN:-}}"
  local run_id="${GITHUB_RUN_ID:-local}" run_attempt="${GITHUB_RUN_ATTEMPT:-1}" actor="${GITHUB_ACTOR:-github-actions[bot]}"

  if [ "$server_url" != "https://github.com" ]; then
    cov_log "Skipping coverage preview publish (unsupported server: ${server_url:-<unset>})"
    return 1
  fi
  if [ -z "$repository" ] || [ -z "$token" ]; then
    cov_log "Skipping coverage preview publish (missing repository or token)"
    return 1
  fi
  if [ ! -d "$source_dir" ]; then
    cov_log "Skipping coverage preview publish (source directory missing: $source_dir)"
    return 1
  fi
  if [ -z "$html_index" ] || [ ! -f "$source_dir/$html_index" ]; then
    cov_log "Skipping coverage preview publish (HTML index not found: $source_dir/$html_index)"
    return 1
  fi

  local tmp_dir run_dir dest_dir remote_url commit_sha raw_base preview_base
  tmp_dir="$(mktemp -d)"
  run_dir="runs/${run_id}-${run_attempt}/android-coverage"
  dest_dir="${tmp_dir}/${run_dir}"
  mkdir -p "$dest_dir"

  cp -R "$source_dir"/. "$dest_dir"/

  if ! git -C "$tmp_dir" init -b previews >/dev/null 2>&1; then
    cov_log "Failed to initialize preview git repository"
    rm -rf "$tmp_dir"
    return 1
  fi
  git -C "$tmp_dir" config user.name "$actor" >/dev/null
  git -C "$tmp_dir" config user.email "github-actions@users.noreply.github.com" >/dev/null
  git -C "$tmp_dir" add . >/dev/null
  if ! git -C "$tmp_dir" commit -m "Publish Android coverage preview for run ${run_id} (attempt ${run_attempt})" >/dev/null 2>&1; then
    cov_log "No changes to commit for coverage preview"
    rm -rf "$tmp_dir"
    return 1
  fi

  remote_url="${server_url}/${repository}.git"
  remote_url="${remote_url/https:\/\//https://x-access-token:${token}@}"
  if ! git -C "$tmp_dir" push --force "$remote_url" previews:quality-report-previews >/dev/null 2>&1; then
    cov_log "Failed to push coverage preview to quality-report-previews"
    rm -rf "$tmp_dir"
    return 1
  fi

  commit_sha="$(git -C "$tmp_dir" rev-parse HEAD)"
  raw_base="https://raw.githubusercontent.com/${repository}/${commit_sha}/${run_dir}"
  preview_base="https://htmlpreview.github.io/?${raw_base}"
  echo "${preview_base}/${html_index}"

  rm -rf "$tmp_dir"
  return 0
}

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
REPORT_XML_PATH="$REPORT_DEST_DIR/jacocoAndroidReport.xml"

if [ ! -f "$REPORT_XML_PATH" ]; then
  alt_xml="$(find "$REPORT_DEST_DIR" -maxdepth 3 -type f -name '*.xml' | head -n1)"
  if [ -n "$alt_xml" ]; then
    cov_log "Using fallback coverage XML: $alt_xml"
    REPORT_XML_PATH="$alt_xml"
  fi
fi

if preview_url=$(publish_coverage_preview "$REPORT_DEST_DIR" "$HTML_INDEX"); then
  export ANDROID_COVERAGE_HTML_URL="$preview_url"
  cov_log "Published coverage HTML preview: $ANDROID_COVERAGE_HTML_URL"
fi

python3 - "$REPORT_XML_PATH" "$SUMMARY_OUT" "$ARTIFACT_NAME" "$HTML_INDEX" <<'PY'
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
