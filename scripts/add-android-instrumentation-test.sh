#!/usr/bin/env bash
# Inject an instrumentation test into the generated Codename One Android Gradle project.
set -euo pipefail

log() { echo "[add-android-instrumentation-test] $1"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"

TOOLS_ENV_DIR="$TMPDIR/codenameone-tools/tools"
ENV_FILE="$TOOLS_ENV_DIR/env.sh"
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
else
  log "Workspace tools not provisioned. Run scripts/setup-workspace.sh first." >&2
  exit 1
fi

BUILD_INFO_FILE="$SCRIPT_DIR/.android-build-info"
if [ ! -f "$BUILD_INFO_FILE" ]; then
  log "Android build metadata not found at $BUILD_INFO_FILE. Run scripts/build-android-app.sh first." >&2
  exit 1
fi

# shellcheck disable=SC1090
source "$BUILD_INFO_FILE"

required_vars=(GRADLE_PROJECT_DIR PACKAGE_NAME ARTIFACT_ID WORK_DIR)
for var in "${required_vars[@]}"; do
  if [ -z "${!var:-}" ]; then
    log "Required build metadata '$var' is missing. Regenerate the Android project." >&2
    exit 1
  fi
done

ANDROID_APP_MODULE_DIR="$GRADLE_PROJECT_DIR/app"
if [ ! -d "$ANDROID_APP_MODULE_DIR" ]; then
  log "Android app module directory not found at $ANDROID_APP_MODULE_DIR" >&2
  exit 1
fi

PACKAGE_PATH="${PACKAGE_NAME//.//}"
ANDROID_TEST_DIR="$ANDROID_APP_MODULE_DIR/src/androidTest/java/$PACKAGE_PATH"
mkdir -p "$ANDROID_TEST_DIR"

TEMPLATE_FILE="$SCRIPT_DIR/templates/AndroidPackageInstrumentationTest.java.tmpl"
if [ ! -f "$TEMPLATE_FILE" ]; then
  log "Instrumentation test template not found at $TEMPLATE_FILE" >&2
  exit 1
fi

TEST_FILE="$ANDROID_TEST_DIR/PackageNameInstrumentationTest.java"
sed -e "s|@PACKAGE@|$PACKAGE_NAME|g" "$TEMPLATE_FILE" > "$TEST_FILE"
log "Wrote instrumentation test to $TEST_FILE"

GRADLE_APP_BUILD_FILE="$ANDROID_APP_MODULE_DIR/build.gradle"
if [ ! -f "$GRADLE_APP_BUILD_FILE" ]; then
  log "Gradle build file not found at $GRADLE_APP_BUILD_FILE" >&2
  exit 1
fi

python3 - "$GRADLE_APP_BUILD_FILE" <<'PY'
import re
import sys
from pathlib import Path

build_file = Path(sys.argv[1])
contents = build_file.read_text()

missing_lines = []
if "androidx.test.ext:junit" not in contents:
    missing_lines.append("androidTestImplementation 'androidx.test.ext:junit:1.1.5'")
if "androidx.test.espresso:espresso-core" not in contents:
    missing_lines.append("androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'")

if missing_lines:
    match = re.search(r'dependencies\s*\{', contents)
    if not match:
        raise SystemExit("dependencies block not found in app build.gradle")
    insertion = ''.join(f"\n    {line}" for line in missing_lines)
    idx = match.end()
    contents = contents[:idx] + insertion + contents[idx:]
    build_file.write_text(contents)
PY
log "Ensured Android test dependencies are declared in $GRADLE_APP_BUILD_FILE"

