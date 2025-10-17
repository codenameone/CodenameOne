#!/usr/bin/env bash
# Build a sample "Hello Codename One" iOS application using the locally-built Codename One iOS port
set -euo pipefail

bia_log() { echo "[build-ios-app] $1"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR%/}/codenameone-tools"
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
PACKAGE_NAME="$GROUP_ID"

SOURCE_PROJECT="$REPO_ROOT/Samples/SampleProjectTemplate"
if [ ! -d "$SOURCE_PROJECT" ]; then
  bia_log "Source project template not found at $SOURCE_PROJECT" >&2
  exit 1
fi
bia_log "Using source project template at $SOURCE_PROJECT"

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
MAIN_FILE="$JAVA_DIR/${MAIN_NAME}.java"
TEMPLATE="$SCRIPT_DIR/templates/HelloCodenameOne.java.tmpl"
if [ ! -f "$TEMPLATE" ]; then
  bia_log "Template not found: $TEMPLATE" >&2
  exit 1
fi

sed -e "s|@PACKAGE@|$PACKAGE_NAME|g" \
    -e "s|@MAIN_NAME@|$MAIN_NAME|g" \
    "$TEMPLATE" > "$MAIN_FILE"

bia_log "Wrote main application class to $MAIN_FILE"

# --- Build iOS project ---
DERIVED_DATA_DIR="${TMPDIR}/codenameone-ios-derived"
rm -rf "$DERIVED_DATA_DIR"
mkdir -p "$DERIVED_DATA_DIR"

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

UITEST_TEMPLATE="$SCRIPT_DIR/ios/tests/HelloCodenameOneUITests.swift.tmpl"
if [ -f "$UITEST_TEMPLATE" ]; then
  IOS_UITEST_DIR="$(find "$PROJECT_DIR" -maxdepth 1 -type d -name '*UITests' -print -quit 2>/dev/null || true)"
  if [ -n "$IOS_UITEST_DIR" ]; then
    UI_TEST_DEST="$IOS_UITEST_DIR/templateUITests.swift"
    bia_log "Installing UI test template at $UI_TEST_DEST"
    cp "$UITEST_TEMPLATE" "$UI_TEST_DEST"
  else
    bia_log "Warning: Could not locate a *UITests target directory under $PROJECT_DIR; UI tests will be skipped"
  fi
fi

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

SCHEME_HELPER="$SCRIPT_DIR/ios/create-shared-scheme.py"
if [ -f "$SCHEME_HELPER" ]; then
  bia_log "Ensuring shared Xcode scheme exposes UI tests"
  if command -v python3 >/dev/null 2>&1; then
    if ! python3 "$SCHEME_HELPER" "$PROJECT_DIR" "$MAIN_NAME"; then
      bia_log "Warning: Failed to configure shared Xcode scheme" >&2
    fi
  else
    bia_log "Warning: python3 is not available; skipping shared scheme configuration" >&2
  fi
else
  bia_log "Warning: Missing scheme helper script at $SCHEME_HELPER" >&2
fi

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

bia_log "Building workspace $WORKSPACE with scheme $MAIN_NAME"
(
  cd "$PROJECT_DIR"
  xcodebuild \
    -workspace "$WORKSPACE" \
    -scheme "$MAIN_NAME" \
    -sdk iphonesimulator \
    -configuration Debug \
    -destination 'generic/platform=iOS Simulator' \
    -derivedDataPath "$DERIVED_DATA_DIR" \
    CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO \
    build
)

PRODUCT_APP=""
while IFS= read -r app_path; do
  PRODUCT_APP="$app_path"
  break
done < <(find "$DERIVED_DATA_DIR" -type d -name '*.app' -print 2>/dev/null)
if [ -n "$PRODUCT_APP" ]; then
  bia_log "Successfully built iOS simulator app at $PRODUCT_APP"
fi

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "workspace=$WORKSPACE"
    [ -n "$PRODUCT_APP" ] && echo "app_bundle=$PRODUCT_APP"
  } >> "$GITHUB_OUTPUT"
fi

bia_log "iOS workspace build completed successfully"
