#!/usr/bin/env bash
# Build a sample "Hello Codename One" Android application using the locally-built Codename One Android port
set -euo pipefail

ba_log() { echo "[build-android-app] $1"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR%/}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
EXTRA_MVN_ARGS=("$@")

ENV_FILE="$ENV_DIR/env.sh"
ba_log "Loading workspace environment from $ENV_FILE"
if [ -f "$ENV_FILE" ]; then
  ba_log "Workspace environment file metadata"
  ls -l "$ENV_FILE" | while IFS= read -r line; do ba_log "$line"; done
  ba_log "Workspace environment file contents"
  sed 's/^/[build-android-app] ENV: /' "$ENV_FILE"
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  ba_log "Loaded environment: JAVA_HOME=${JAVA_HOME:-<unset>} JAVA17_HOME=${JAVA17_HOME:-<unset>} MAVEN_HOME=${MAVEN_HOME:-<unset>}"
else
  ba_log "Workspace tools not found. Run scripts/setup-workspace.sh before this script." >&2
  exit 1
fi

# --- Tool validations ---
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  ba_log "JAVA_HOME validation failed. Current value: ${JAVA_HOME:-<unset>}" >&2
  if [ -n "${JAVA_HOME:-}" ]; then
    ba_log "Contents of JAVA_HOME directory"
    if [ -d "$JAVA_HOME" ]; then ls -l "$JAVA_HOME" | while IFS= read -r line; do ba_log "$line"; done; else ba_log "JAVA_HOME directory does not exist"; fi
  fi
  ba_log "JAVA_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  ba_log "JAVA17_HOME validation failed. Current value: ${JAVA17_HOME:-<unset>}" >&2
  if [ -n "${JAVA17_HOME:-}" ]; then
    ba_log "Contents of JAVA17_HOME directory"
    if [ -d "$JAVA17_HOME" ]; then ls -l "$JAVA17_HOME" | while IFS= read -r line; do ba_log "$line"; done; else ba_log "JAVA17_HOME directory does not exist"; fi
  fi
  ba_log "JAVA17_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${MAVEN_HOME:-}" ] || [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  ba_log "MAVEN_HOME validation failed. Current value: ${MAVEN_HOME:-<unset>}" >&2
  if [ -n "${MAVEN_HOME:-}" ]; then
    ba_log "Contents of MAVEN_HOME directory"
    if [ -d "$MAVEN_HOME" ]; then ls -l "$MAVEN_HOME" | while IFS= read -r line; do ba_log "$line"; done; else ba_log "MAVEN_HOME directory does not exist"; fi
  fi
  ba_log "Maven is not available. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi

ba_log "Using JAVA_HOME at $JAVA_HOME"
ba_log "Using JAVA17_HOME at $JAVA17_HOME"
ba_log "Using Maven installation at $MAVEN_HOME"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$ANDROID_SDK_ROOT" ]; then
  if [ -d "/usr/local/lib/android/sdk" ]; then ANDROID_SDK_ROOT="/usr/local/lib/android/sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then ANDROID_SDK_ROOT="$HOME/Android/Sdk"; fi
fi
if [ -z "$ANDROID_SDK_ROOT" ] || [ ! -d "$ANDROID_SDK_ROOT" ]; then
  ba_log "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME to a valid installation." >&2
  exit 1
fi
export ANDROID_SDK_ROOT ANDROID_HOME="$ANDROID_SDK_ROOT"
ba_log "Using Android SDK at $ANDROID_SDK_ROOT"

CN1_VERSION=$(awk -F'[<>]' '/<version>/{print $3; exit}' maven/pom.xml)
ba_log "Detected Codename One version $CN1_VERSION"

WORK_DIR="$TMPDIR/cn1-hello-android"
rm -rf "$WORK_DIR"; mkdir -p "$WORK_DIR"

GROUP_ID="com.codenameone.examples"
ARTIFACT_ID="hello-codenameone"
MAIN_NAME="HelloCodenameOne"

SOURCE_PROJECT="$REPO_ROOT/Samples/SampleProjectTemplate"
if [ ! -d "$SOURCE_PROJECT" ]; then
  ba_log "Source project template not found at $SOURCE_PROJECT" >&2
  exit 1
fi
ba_log "Using source project template at $SOURCE_PROJECT"

LOCAL_MAVEN_REPO="${LOCAL_MAVEN_REPO:-$HOME/.m2/repository}"
ba_log "Using local Maven repository at $LOCAL_MAVEN_REPO"
mkdir -p "$LOCAL_MAVEN_REPO"
MAVEN_CMD=(
  "$MAVEN_HOME/bin/mvn" -B -ntp
  -Dmaven.repo.local="$LOCAL_MAVEN_REPO"
  -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
)

# --- Generate app skeleton ---
ba_log "Generating Codename One application skeleton via codenameone-maven-plugin"
(
  cd "$WORK_DIR"
  xvfb-run -a "${MAVEN_CMD[@]}" -q \
    com.codenameone:codenameone-maven-plugin:7.0.204:generate-app-project \
    -DgroupId="$GROUP_ID" \
    -DartifactId="$ARTIFACT_ID" \
    -Dversion=1.0-SNAPSHOT \
    -DsourceProject="$SOURCE_PROJECT" \
    -Dcn1Version="7.0.204" \
    "${EXTRA_MVN_ARGS[@]}"
)

APP_DIR="$WORK_DIR/$ARTIFACT_ID"

# --- Normalize Codename One versions post-generation (no versions-maven-plugin) ---

ROOT_POM="$APP_DIR/pom.xml"

# 1) Ensure a codenameone.version property exists in the root pom
ensure_property() {
  local pom="$1" name="$2" value="$3"

  if ! grep -q "<properties>" "$pom"; then
    # Insert a <properties> block near the top-level <project> block
    awk -v v="$value" -v n="$name" '
      BEGIN{inserted=0}
      /<project[^>]*>/ && inserted==0 {
        print;
        print "  <properties>";
        print "    <" n ">" v "</" n ">";
        print "  </properties>";
        inserted=1; next
      }
      {print}
    ' "$pom" > "$pom.tmp" && mv "$pom.tmp" "$pom"
  elif ! grep -q "<${name}>" "$pom"; then
    # Add the property inside existing <properties>
    awk -v v="$value" -v n="$name" '
      /<properties>/ && !done {
        print;
        print "    <" n ">" v "</" n ">";
        done=1; next
      }
      {print}
    ' "$pom" > "$pom.tmp" && mv "$pom.tmp" "$pom"
  else
    # Update existing property value
    perl -0777 -pe "s|(<${name}>)[^<]+(</${name}>)|\$1${value}\$2|s" -i "$pom"
  fi
}

ensure_property "$ROOT_POM" "codenameone.version" "$CN1_VERSION"

# 2) Rewrite com.codenameone dependency versions to ${codenameone.version}
rewrite_versions() {
  local pom="$1"
  # Dependencies
  perl -0777 -pe 's!(<dependency>\s*<groupId>com\.codenameone[^<]*</groupId>\s*<artifactId>[^<]+</artifactId>\s*<version>)[^<]+(</version>)!${1}${codenameone.version}${2}!sg' -i "$pom"
  # Plugins
  perl -0777 -pe 's!(<plugin>\s*<groupId>com\.codenameone[^<]*</groupId>\s*<artifactId>[^<]+</artifactId>\s*<version>)[^<]+(</version>)!${1}${codenameone.version}${2}!sg' -i "$pom"
}

export codenameone_version_placeholder='${codenameone.version}'

# Walk all poms under the generated app (skip non-poms like cn1libs/)
while IFS= read -r -d '' P; do
  rewrite_versions "$P"
done < <(find "$APP_DIR" -type f -name pom.xml -print0)

# 3) From now on, build with the property explicitly set too (helps any missed spots)
EXTRA_MVN_ARGS+=("-Dcodenameone.version=${CN1_VERSION}")

[ -d "$APP_DIR" ] || { ba_log "Failed to create Codename One application project" >&2; exit 1; }
[ -f "$APP_DIR/build.sh" ] && chmod +x "$APP_DIR/build.sh"

SETTINGS_FILE="$APP_DIR/common/codenameone_settings.properties"
[ -f "$SETTINGS_FILE" ] || { ba_log "codenameone_settings.properties not found at $SETTINGS_FILE" >&2; exit 1; }

# --- Read settings ---
read_prop() { grep -E "^$1=" "$SETTINGS_FILE" | head -n1 | cut -d'=' -f2- | sed 's/^[[:space:]]*//'; }

PACKAGE_NAME="$(read_prop 'codename1.packageName' || true)"
CURRENT_MAIN_NAME="$(read_prop 'codename1.mainName' || true)"

if [ -z "$PACKAGE_NAME" ]; then
  PACKAGE_NAME="$GROUP_ID"
  ba_log "Package name not found in settings. Falling back to groupId $PACKAGE_NAME"
fi
if [ -z "$CURRENT_MAIN_NAME" ]; then
  CURRENT_MAIN_NAME="$MAIN_NAME"
  ba_log "Main class name not found in settings. Falling back to target $CURRENT_MAIN_NAME"
fi

# --- Generate Java from external template ---
PACKAGE_PATH="${PACKAGE_NAME//.//}"
JAVA_DIR="$APP_DIR/common/src/main/java/${PACKAGE_PATH}"
mkdir -p "$JAVA_DIR"
MAIN_FILE="$JAVA_DIR/${MAIN_NAME}.java"

TEMPLATE="$SCRIPT_DIR/templates/HelloCodenameOne.java.tmpl"
if [ ! -f "$TEMPLATE" ]; then
  ba_log "Template not found: $TEMPLATE" >&2
  exit 1
fi

sed -e "s|@PACKAGE@|$PACKAGE_NAME|g" \
    -e "s|@MAIN_NAME@|$MAIN_NAME|g" \
    "$TEMPLATE" > "$MAIN_FILE"

# --- Ensure codename1.mainName is set ---
ba_log "Setting codename1.mainName to $MAIN_NAME"
if grep -q '^codename1.mainName=' "$SETTINGS_FILE"; then
  # GNU sed in CI: in-place edit without backup
  sed -E -i 's|^codename1\.mainName=.*$|codename1.mainName='"$MAIN_NAME"'|' "$SETTINGS_FILE"
else
  printf '\ncodename1.mainName=%s\n' "$MAIN_NAME" >> "$SETTINGS_FILE"
fi
# Ensure trailing newline
tail -c1 "$SETTINGS_FILE" | read -r _ || echo >> "$SETTINGS_FILE"

# --- Normalize Codename One versions (use Maven Versions Plugin) ---
ba_log "Normalizing Codename One Maven coordinates to $CN1_VERSION"

# --- Build Android gradle project ---
ba_log "Building Android gradle project using Codename One port"
xvfb-run -a "${MAVEN_CMD[@]}" -q -f "$APP_DIR/pom.xml" package \
  -DskipTests \
  -Dcodename1.platform=android \
  -Dcodename1.buildTarget=android-source \
  -Dopen=false \
  "${EXTRA_MVN_ARGS[@]}"

GRADLE_PROJECT_DIR=$(find "$APP_DIR/android/target" -maxdepth 2 -type d -name "*-android-source" | head -n 1 || true)
if [ -z "$GRADLE_PROJECT_DIR" ]; then
  ba_log "Failed to locate generated Android project" >&2
  ba_log "Contents of $APP_DIR/android/target:" >&2
  ls -R "$APP_DIR/android/target" >&2 || ba_log "Unable to list $APP_DIR/android/target" >&2
  exit 1
fi

ba_log "Invoking Gradle build in $GRADLE_PROJECT_DIR"
chmod +x "$GRADLE_PROJECT_DIR/gradlew"
ORIGINAL_JAVA_HOME="$JAVA_HOME"
export JAVA_HOME="$JAVA17_HOME"
(
  cd "$GRADLE_PROJECT_DIR"
  if command -v sdkmanager >/dev/null 2>&1; then
    yes | sdkmanager --licenses >/dev/null 2>&1 || true
  elif [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
  fi
  ./gradlew --no-daemon assembleDebug
)
export JAVA_HOME="$ORIGINAL_JAVA_HOME"

APK_PATH=$(find "$GRADLE_PROJECT_DIR" -path "*/outputs/apk/debug/*.apk" | head -n 1 || true)
[ -n "$APK_PATH" ] || { ba_log "Gradle build completed but no APK was found" >&2; exit 1; }
ba_log "Successfully built Android APK at $APK_PATH"