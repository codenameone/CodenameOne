#!/usr/bin/env bash
# Build a sample "Hello Codename One" iOS application using the locally-built Codename One iOS port
set -euo pipefail

bia_log() { echo "[build-ios-app] $1"; }

# Pin Xcode so CN1’s Java subprocess sees xcodebuild
export DEVELOPER_DIR="/Applications/Xcode_16.4.app/Contents/Developer"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
EXTRA_MVN_ARGS=("$@")

ENV_FILE="$ENV_DIR/env.sh"
bia_log "Loading workspace environment from $ENV_FILE"
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  bia_log "Loaded environment: JAVA_HOME=${JAVA_HOME:-<unset>} JAVA17_HOME=${JAVA17_HOME:-<unset>} MAVEN_HOME=${MAVEN_HOME:-<unset>}"
else
  bia_log "Workspace tools not found. Run scripts/setup-workspace.sh before this script." >&2
  exit 1
fi

# --- Tool validations ---
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  bia_log "JAVA_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  bia_log "JAVA17_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${MAVEN_HOME:-}" ] || [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  bia_log "Maven is not available. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if ! command -v xcodebuild >/dev/null 2>&1; then
  bia_log "xcodebuild not found. Install Xcode command-line tools." >&2
  exit 1
fi
if ! command -v pod >/dev/null 2>&1; then
  bia_log "CocoaPods (pod) command not found. Install cocoapods before running this script." >&2
  exit 1
fi

export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

bia_log "Using JAVA_HOME at $JAVA_HOME"
bia_log "Using JAVA17_HOME at $JAVA17_HOME"
bia_log "Using Maven installation at $MAVEN_HOME"
bia_log "Using CocoaPods version $(pod --version 2>/dev/null || echo '<unknown>')"

CN1_VERSION=$(awk -F'[<>]' '/<version>/{print $3; exit}' maven/pom.xml)
bia_log "Detected Codename One version $CN1_VERSION"

WORK_DIR="$TMPDIR/cn1-hello-ios"
rm -rf "$WORK_DIR"; mkdir -p "$WORK_DIR"

GROUP_ID="com.codenameone.examples"
ARTIFACT_ID="hello-codenameone-ios"
MAIN_NAME="HelloCodenameOne"
PACKAGE_NAME="com.codenameone.examples.hellocodenameone"

SOURCE_PROJECT="$REPO_ROOT/Samples/SampleProjectTemplate"
if [ ! -d "$SOURCE_PROJECT" ]; then
  bia_log "Source project template not found at $SOURCE_PROJECT" >&2
  exit 1
fi
bia_log "Using source project template at $SOURCE_PROJECT"

# Local Maven repo + command wrapper (define BEFORE using it)
LOCAL_MAVEN_REPO="${LOCAL_MAVEN_REPO:-$HOME/.m2/repository}"
bia_log "Using local Maven repository at $LOCAL_MAVEN_REPO"
mkdir -p "$LOCAL_MAVEN_REPO"

MAVEN_CMD=(
  "$MAVEN_HOME/bin/mvn" -B -ntp
  -Dmaven.repo.local="$LOCAL_MAVEN_REPO"
  -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
)

# --- Generate app skeleton ---
bia_log "Generating Codename One application skeleton via codenameone-maven-plugin"
(
  cd "$WORK_DIR"
  "${MAVEN_CMD[@]}" -q \
    com.codenameone:codenameone-maven-plugin:7.0.204:generate-app-project \
    -DgroupId="$GROUP_ID" \
    -DartifactId="$ARTIFACT_ID" \
    -Dversion=1.0-SNAPSHOT \
    -DsourceProject="$SOURCE_PROJECT" \
    -Dcn1Version="$CN1_VERSION" \
    "${EXTRA_MVN_ARGS[@]}"
)

APP_DIR="$WORK_DIR/$ARTIFACT_ID"
[ -d "$APP_DIR" ] || { bia_log "Failed to create Codename One application project" >&2; exit 1; }
[ -f "$APP_DIR/build.sh" ] && chmod +x "$APP_DIR/build.sh"

# --- Normalize Codename One versions in generated iOS project POMs ---
ROOT_POM="$APP_DIR/pom.xml"
NS="mvn=http://maven.apache.org/POM/4.0.0"

# Ensure xmlstarlet is available (macOS runners use Homebrew)
if ! command -v xmlstarlet >/dev/null 2>&1; then
  if command -v brew >/dev/null 2>&1; then
    brew install xmlstarlet
  elif command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -y && sudo apt-get install -y xmlstarlet
  else
    bia_log "xmlstarlet not found and no installer available"; exit 1
  fi
fi

# Helpers
x() { xmlstarlet ed -L -N "$NS" "$@"; }
q() { xmlstarlet sel -N "$NS" "$@"; }

# 1) Ensure <properties><codenameone.version>${CN1_VERSION}</codenameone.version>
if [ "$(q -t -v 'count(/mvn:project/mvn:properties)' "$ROOT_POM" 2>/dev/null || echo 0)" = "0" ]; then
  x -s "/mvn:project" -t elem -n properties -v "" "$ROOT_POM"
fi
if [ "$(q -t -v 'count(/mvn:project/mvn:properties/mvn:codenameone.version)' "$ROOT_POM" 2>/dev/null || echo 0)" = "0" ]; then
  x -s "/mvn:project/mvn:properties" -t elem -n codenameone.version -v "$CN1_VERSION" "$ROOT_POM"
else
  x -u "/mvn:project/mvn:properties/mvn:codenameone.version" -v "$CN1_VERSION" "$ROOT_POM"
fi

# 2) Force the com.codenameone parent to a literal version (no property)
while IFS= read -r -d '' P; do
  x -u "/mvn:project[mvn:parent/mvn:groupId='com.codenameone' and mvn:parent/mvn:artifactId='codenameone-maven-parent']/mvn:parent/mvn:version" -v "$CN1_VERSION" "$P" || true
done < <(find "$APP_DIR" -type f -name pom.xml -print0)

# 3) Point all com.codenameone deps/plugins to ${codenameone.version}
while IFS= read -r -d '' P; do
  x -u "/mvn:project//mvn:dependencies/mvn:dependency[starts-with(mvn:groupId,'com.codenameone')]/mvn:version" -v '${codenameone.version}' "$P" 2>/dev/null || true
  x -u "/mvn:project//mvn:build/mvn:plugins/mvn:plugin[starts-with(mvn:groupId,'com.codenameone')]/mvn:version" -v '${codenameone.version}' "$P" 2>/dev/null || true
  x -u "/mvn:project//mvn:build/mvn:pluginManagement/mvn:plugins/mvn:plugin[starts-with(mvn:groupId,'com.codenameone')]/mvn:version" -v '${codenameone.version}' "$P" 2>/dev/null || true
done < <(find "$APP_DIR" -type f -name pom.xml -print0)

# 4) Ensure common Maven plugins have versions (helps before parent resolves)
declare -A PIN=(
  [org.apache.maven.plugins:maven-compiler-plugin]=3.11.0
  [org.apache.maven.plugins:maven-resources-plugin]=3.3.1
  [org.apache.maven.plugins:maven-surefire-plugin]=3.2.5
  [org.apache.maven.plugins:maven-failsafe-plugin]=3.2.5
  [org.apache.maven.plugins:maven-jar-plugin]=3.3.0
  [org.apache.maven.plugins:maven-clean-plugin]=3.3.2
  [org.apache.maven.plugins:maven-deploy-plugin]=3.1.2
  [org.apache.maven.plugins:maven-install-plugin]=3.1.2
  [org.apache.maven.plugins:maven-assembly-plugin]=3.6.0
  [org.apache.maven.plugins:maven-site-plugin]=4.0.0-M15
  [com.codenameone:codenameone-maven-plugin]='${codenameone.version}'
)
add_version_if_missing() {
  local pom="$1" g="$2" a="$3" v="$4"
  if [ "$(q -t -v "count(/mvn:project/mvn:build/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a']/mvn:version)" "$pom" 2>/dev/null || echo 0)" = "0" ] &&
     [ "$(q -t -v "count(/mvn:project/mvn:build/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a'])" "$pom" 2>/dev/null || echo 0)" != "0" ]; then
    x -s "/mvn:project/mvn:build/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a']" -t elem -n version -v "$v" "$pom" || true
  fi
  if [ "$(q -t -v "count(/mvn:project/mvn:build/mvn:pluginManagement/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a']/mvn:version)" "$pom" 2>/dev/null || echo 0)" = "0" ] &&
     [ "$(q -t -v "count(/mvn:project/mvn:build/mvn:pluginManagement/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a'])" "$pom" 2>/dev/null || echo 0)" != "0" ]; then
    x -s "/mvn:project/mvn:build/mvn:pluginManagement/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a']" -t elem -n version -v "$v" "$pom" || true
  fi
}
while IFS= read -r -d '' P; do
  for ga in "${!PIN[@]}"; do add_version_if_missing "$P" "${ga%%:*}" "${ga##*:}" "${PIN[$ga]}"; done
done < <(find "$APP_DIR" -type f -name pom.xml -print0)

# 5) Also pass the property when building (already present below)
EXTRA_MVN_ARGS+=("-Dcodenameone.version=${CN1_VERSION}")

SETTINGS_FILE="$APP_DIR/common/codenameone_settings.properties"
if [ ! -f "$SETTINGS_FILE" ]; then
  bia_log "codenameone_settings.properties not found at $SETTINGS_FILE" >&2
  exit 1
fi

set_property() {
  local key="$1" value="$2"
  if grep -q "^${key}=" "$SETTINGS_FILE"; then
    if sed --version >/dev/null 2>&1; then
      sed -i -E "s|^${key}=.*$|${key}=${value}|" "$SETTINGS_FILE"
    else
      sed -i '' -E "s|^${key}=.*$|${key}=${value}|" "$SETTINGS_FILE"
    fi
  else
    printf '\n%s=%s\n' "$key" "$value" >> "$SETTINGS_FILE"
  fi
}

set_property "codename1.packageName" "$PACKAGE_NAME"
set_property "codename1.mainName" "$MAIN_NAME"

# Ensure trailing newline
tail -c1 "$SETTINGS_FILE" | read -r _ || echo >> "$SETTINGS_FILE"

PACKAGE_PATH="${PACKAGE_NAME//.//}"
JAVA_DIR="$APP_DIR/common/src/main/java/${PACKAGE_PATH}"
mkdir -p "$JAVA_DIR"
MAIN_FILE_SOURCE="$SCRIPT_DIR/device-runner-app/main/${MAIN_NAME}.java"
if [ ! -f "$MAIN_FILE_SOURCE" ]; then
  bia_log "Sample application source not found: $MAIN_FILE_SOURCE" >&2
  exit 1
fi
cp "$MAIN_FILE_SOURCE" "$JAVA_DIR/${MAIN_NAME}.java"
bia_log "Wrote main application class to $JAVA_DIR/${MAIN_NAME}.java"

TEST_SOURCE_DIR="$SCRIPT_DIR/device-runner-app/tests"
TEST_JAVA_DIR="$APP_DIR/common/src/main/java/${PACKAGE_PATH}/tests"
mkdir -p "$TEST_JAVA_DIR"
if [ ! -d "$TEST_SOURCE_DIR" ]; then
  bia_log "DeviceRunner test sources not found: $TEST_SOURCE_DIR" >&2
  exit 1
fi
cp "$TEST_SOURCE_DIR"/*.java "$TEST_JAVA_DIR"/
bia_log "Installed DeviceRunner UI tests in $TEST_JAVA_DIR"

# --- Build iOS project (ios-source) ---
DERIVED_DATA_DIR="${TMPDIR}/codenameone-ios-derived"
rm -rf "$DERIVED_DATA_DIR"; mkdir -p "$DERIVED_DATA_DIR"

xcodebuild -version

bia_log "Building iOS Xcode project using Codename One port"
"${MAVEN_CMD[@]}" -q -f "$APP_DIR/pom.xml" package \
  -DskipTests \
  -Dcodename1.platform=ios \
  -Dcodename1.buildTarget=ios-source \
  -Dopen=false \
  -Dcodenameone.version="$CN1_VERSION" \
  "${EXTRA_MVN_ARGS[@]}"

IOS_TARGET_DIR="$APP_DIR/ios/target"
if [ ! -d "$IOS_TARGET_DIR" ]; then
  bia_log "iOS target directory not found at $IOS_TARGET_DIR" >&2
  exit 1
fi

PROJECT_DIR=""
for candidate in "$IOS_TARGET_DIR"/*-ios-source; do
  if [ -d "$candidate" ]; then
    PROJECT_DIR="$candidate"
    break
  fi
done
if [ -z "$PROJECT_DIR" ]; then
  bia_log "Failed to locate generated iOS project under $IOS_TARGET_DIR" >&2
  find "$IOS_TARGET_DIR" -type d -print >&2 || true
  exit 1
fi
bia_log "Found generated iOS project at $PROJECT_DIR"

# CocoaPods (project contains a Podfile but usually empty — fine)
if [ -f "$PROJECT_DIR/Podfile" ]; then
  bia_log "Installing CocoaPods dependencies"
  (
    cd "$PROJECT_DIR"
    if ! pod install --repo-update; then
      bia_log "pod install --repo-update failed; retrying without repo update"
      pod install
    fi
  )
else
  bia_log "Podfile not found in generated project; skipping pod install"
fi

# Locate workspace for the next step
WORKSPACE=""
for candidate in "$PROJECT_DIR"/*.xcworkspace; do
  if [ -d "$candidate" ]; then
    WORKSPACE="$candidate"
    break
  fi
done
if [ -z "$WORKSPACE" ]; then
  bia_log "Failed to locate xcworkspace in $PROJECT_DIR" >&2
  ls "$PROJECT_DIR" >&2 || true
  exit 1
fi
bia_log "Found xcworkspace: $WORKSPACE"

SCHEME="$MAIN_NAME"

# Make these visible to the next GH Actions step
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "workspace=$WORKSPACE"
    echo "scheme=$SCHEME"
  } >> "$GITHUB_OUTPUT"
fi

bia_log "Emitted outputs -> workspace=$WORKSPACE, scheme=$SCHEME"

# (Optional) dump xcodebuild -list for debugging
ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts}"
mkdir -p "$ARTIFACTS_DIR"
xcodebuild -workspace "$WORKSPACE" -list > "$ARTIFACTS_DIR/xcodebuild-list.txt" 2>&1 || true

exit 0