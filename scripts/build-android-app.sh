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

# --- Namespace-aware CN1 normalization (xmlstarlet) ---
ROOT_POM="$APP_DIR/pom.xml"
NS="mvn=http://maven.apache.org/POM/4.0.0"

if ! command -v xmlstarlet >/dev/null 2>&1; then
  sudo apt-get update -y && sudo apt-get install -y xmlstarlet
fi

# Helper to run xmlstarlet with Maven namespace
x() { xmlstarlet ed -L -N "$NS" "$@"; }
q() { xmlstarlet sel -N "$NS" "$@"; }

# 1) Ensure <properties><codenameone.version> exists/updated (root pom)
if [ "$(q -t -v 'count(/mvn:project/mvn:properties)' "$ROOT_POM" 2>/dev/null || echo 0)" = "0" ]; then
  x -s "/mvn:project" -t elem -n properties -v "" "$ROOT_POM"
fi
if [ "$(q -t -v 'count(/mvn:project/mvn:properties/mvn:codenameone.version)' "$ROOT_POM" 2>/dev/null || echo 0)" = "0" ]; then
  x -s "/mvn:project/mvn:properties" -t elem -n codenameone.version -v "$CN1_VERSION" "$ROOT_POM"
else
  x -u "/mvn:project/mvn:properties/mvn:codenameone.version" -v "$CN1_VERSION" "$ROOT_POM"
fi

# 2) Parent must be a LITERAL version (no property allowed)
while IFS= read -r -d '' P; do
  x -u "/mvn:project[mvn:parent/mvn:groupId='com.codenameone' and mvn:parent/mvn:artifactId='codenameone-maven-parent']/mvn:parent/mvn:version" -v "$CN1_VERSION" "$P" || true
done < <(find "$APP_DIR" -type f -name pom.xml -print0)

# 3) Point com.codenameone deps/plugins to ${codenameone.version}
while IFS= read -r -d '' P; do
  # Dependencies
  x -u "/mvn:project//mvn:dependencies/mvn:dependency[starts-with(mvn:groupId,'com.codenameone')]/mvn:version" -v '${codenameone.version}' "$P" 2>/dev/null || true
  # Plugins (regular)
  x -u "/mvn:project//mvn:build/mvn:plugins/mvn:plugin[starts-with(mvn:groupId,'com.codenameone')]/mvn:version" -v '${codenameone.version}' "$P" 2>/dev/null || true
  # Plugins (pluginManagement)
  x -u "/mvn:project//mvn:build/mvn:pluginManagement/mvn:plugins/mvn:plugin[starts-with(mvn:groupId,'com.codenameone')]/mvn:version" -v '${codenameone.version}' "$P" 2>/dev/null || true
done < <(find "$APP_DIR" -type f -name pom.xml -print0)

# 4) Ensure common Maven plugins have a version (Maven requires it even if parent not yet resolved)
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
  # build/plugins
  if [ "$(q -t -v "count(/mvn:project/mvn:build/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a']/mvn:version)" "$pom" 2>/dev/null || echo 0)" = "0" ] &&
     [ "$(q -t -v "count(/mvn:project/mvn:build/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a'])" "$pom" 2>/dev/null || echo 0)" != "0" ]; then
    x -s "/mvn:project/mvn:build/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a']" -t elem -n version -v "$v" "$pom" || true
  fi
  # pluginManagement/plugins
  if [ "$(q -t -v "count(/mvn:project/mvn:build/mvn:pluginManagement/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a']/mvn:version)" "$pom" 2>/dev/null || echo 0)" = "0" ] &&
     [ "$(q -t -v "count(/mvn:project/mvn:build/mvn:pluginManagement/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a'])" "$pom" 2>/dev/null || echo 0)" != "0" ]; then
    x -s "/mvn:project/mvn:build/mvn:pluginManagement/mvn:plugins/mvn:plugin[mvn:groupId='$g' and mvn:artifactId='$a']" -t elem -n version -v "$v" "$pom" || true
  fi
}

while IFS= read -r -d '' P; do
  for ga in "${!PIN[@]}"; do
    add_version_if_missing "$P" "${ga%%:*}" "${ga##*:}" "${PIN[$ga]}"
  done
done < <(find "$APP_DIR" -type f -name pom.xml -print0)

# 5) Build with the property set so any lingering refs resolve to the local snapshot
EXTRA_MVN_ARGS+=("-Dcodenameone.version=${CN1_VERSION}")

# (Optional) quick non-fatal checks
xmlstarlet sel -N "$NS" -t -v "/mvn:project/mvn:properties/mvn:codenameone.version" -n "$ROOT_POM" || true
xmlstarlet sel -N "$NS" -t -c "/mvn:project/mvn:build/mvn:plugins" -n "$ROOT_POM" | head -n 60 || true



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

# --- Inject instrumentation UI test into Gradle project ---
APP_MODULE_DIR=$(find "$GRADLE_PROJECT_DIR" -maxdepth 1 -type d -name "app" | head -n 1 || true)
if [ -z "$APP_MODULE_DIR" ]; then
  ba_log "Unable to locate Gradle app module inside $GRADLE_PROJECT_DIR" >&2
  exit 1
fi

UI_TEST_TEMPLATE="$SCRIPT_DIR/templates/HelloCodenameOneUiTest.java.tmpl"
if [ ! -f "$UI_TEST_TEMPLATE" ]; then
  ba_log "UI test template not found: $UI_TEST_TEMPLATE" >&2
  exit 1
fi

UI_TEST_DIR="$APP_MODULE_DIR/src/androidTest/java/${PACKAGE_PATH}"
mkdir -p "$UI_TEST_DIR"
UI_TEST_FILE="$UI_TEST_DIR/${MAIN_NAME}UiTest.java"

sed -e "s|@PACKAGE@|$PACKAGE_NAME|g" \
    -e "s|@MAIN_NAME@|$MAIN_NAME|g" \
    "$UI_TEST_TEMPLATE" > "$UI_TEST_FILE"
ba_log "Created instrumentation UI test at $UI_TEST_FILE"

STUB_SRC_DIR="$APP_MODULE_DIR/src/main/java/${PACKAGE_PATH}"
mkdir -p "$STUB_SRC_DIR"
STUB_SRC_FILE="$STUB_SRC_DIR/${MAIN_NAME}Stub.java"
if [ ! -f "$STUB_SRC_FILE" ]; then
  cat >"$STUB_SRC_FILE" <<EOF
package ${PACKAGE_NAME};

import android.os.Bundle;

import com.codename1.impl.android.CodenameOneActivity;

public class ${MAIN_NAME}Stub extends CodenameOneActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
EOF
  ba_log "Created Codename One stub activity at $STUB_SRC_FILE"
else
  ba_log "Codename One stub activity already present at $STUB_SRC_FILE"
fi

MANIFEST_FILE="$APP_MODULE_DIR/src/main/AndroidManifest.xml"
if [ ! -f "$MANIFEST_FILE" ]; then
  ba_log "AndroidManifest.xml not found at $MANIFEST_FILE" >&2
  exit 1
fi

ensure_manifest_stub() {
  python3 - "$MANIFEST_FILE" "$MAIN_NAME" <<'PY'
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

manifest_path = Path(sys.argv[1])
main_name = sys.argv[2]
ns_android = "http://schemas.android.com/apk/res/android"

ET.register_namespace('android', ns_android)
tree = ET.parse(manifest_path)
root = tree.getroot()
app = root.find('application')
if app is None:
    sys.exit("Application element missing in AndroidManifest.xml")

target = f".{main_name}Stub"
android_name = f"{{{ns_android}}}name"
android_exported = f"{{{ns_android}}}exported"

for activity in app.findall('activity'):
    name = activity.get(android_name)
    if name in (target, target.lstrip('.')):
        break
else:
    activity = ET.Element('activity')
    activity.set(android_name, target)
    activity.set(android_exported, 'false')
    app.append(activity)
    tree.write(manifest_path, encoding='utf-8', xml_declaration=True)
PY
}

if ! ensure_manifest_stub; then
  ba_log "Failed to ensure stub activity declaration in $MANIFEST_FILE" >&2
  exit 1
fi

if ! grep -Eq "android:name=\"(\.${MAIN_NAME}Stub|${PACKAGE_NAME//./\\.}.${MAIN_NAME}Stub)\"" "$MANIFEST_FILE"; then
  ba_log "Manifest does not declare ${MAIN_NAME}Stub activity" >&2
  exit 1
fi

if [ ! -f "$STUB_SRC_FILE" ]; then
  ba_log "Missing stub activity source at $STUB_SRC_FILE" >&2
  exit 1
fi

APP_BUILD_GRADLE="$APP_MODULE_DIR/build.gradle"
if [ ! -f "$APP_BUILD_GRADLE" ]; then
  ba_log "Expected Gradle build file not found at $APP_BUILD_GRADLE" >&2
  exit 1
fi

if ! grep -q "android[[:space:]]*{" "$APP_BUILD_GRADLE"; then
  ba_log "Gradle build file at $APP_BUILD_GRADLE is missing an android { } block" >&2
  exit 1
fi

GRADLE_UPDATE_OUTPUT="$("$SCRIPT_DIR/update_android_ui_test_gradle.py" "$APP_BUILD_GRADLE")"
if [ -n "$GRADLE_UPDATE_OUTPUT" ]; then
  while IFS= read -r line; do
    [ -n "$line" ] && ba_log "$line"
  done <<<"$GRADLE_UPDATE_OUTPUT"
fi

ba_log "Dependencies block after instrumentation update:"
awk '/^\s*dependencies\s*\{/{flag=1} flag{print} /^\s*\}/{if(flag){exit}}' "$APP_BUILD_GRADLE" \
  | sed 's/^/[build-android-app] | /'

FINAL_ARTIFACT_DIR="${CN1_TEST_SCREENSHOT_EXPORT_DIR:-$REPO_ROOT/build-artifacts}"
mkdir -p "$FINAL_ARTIFACT_DIR"
if [ -n "${GITHUB_ENV:-}" ]; then
  printf 'CN1_UI_TEST_ARTIFACT_DIR=%s\n' "$FINAL_ARTIFACT_DIR" >> "$GITHUB_ENV"
fi

ba_log "Invoking Gradle build in $GRADLE_PROJECT_DIR"
chmod +x "$GRADLE_PROJECT_DIR/gradlew"
ORIGINAL_JAVA_HOME="$JAVA_HOME"
export JAVA_HOME="$JAVA17_HOME"
export PATH="$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"

SDKMANAGER_BIN=""
if [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
  SDKMANAGER_BIN="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
elif [ -x "$ANDROID_SDK_ROOT/cmdline-tools/bin/sdkmanager" ]; then
  SDKMANAGER_BIN="$ANDROID_SDK_ROOT/cmdline-tools/bin/sdkmanager"
elif command -v sdkmanager >/dev/null 2>&1; then
  SDKMANAGER_BIN="$(command -v sdkmanager)"
fi

AVDMANAGER_BIN=""
if [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager" ]; then
  AVDMANAGER_BIN="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager"
elif [ -x "$ANDROID_SDK_ROOT/cmdline-tools/bin/avdmanager" ]; then
  AVDMANAGER_BIN="$ANDROID_SDK_ROOT/cmdline-tools/bin/avdmanager"
elif command -v avdmanager >/dev/null 2>&1; then
  AVDMANAGER_BIN="$(command -v avdmanager)"
fi

install_android_packages() {
  local manager="$1"
  if [ -z "$manager" ]; then
    ba_log "sdkmanager not available; cannot install system images" >&2
    exit 1
  fi
  yes | "$manager" --licenses >/dev/null 2>&1 || true
  "$manager" --install \
    "platform-tools" \
    "emulator" \
    "platforms;android-35" \
    "system-images;android-35;default;x86_64" >/dev/null 2>&1 || true
}

create_avd() {
  local manager="$1"
  local name="$2"
  local image="$3"
  local avd_dir="$4"
  if [ -z "$manager" ]; then
    ba_log "avdmanager not available; cannot create emulator" >&2
    exit 1
  fi
  mkdir -p "$avd_dir"
  local ini_file="$avd_dir/$name.ini"
  local image_dir="$avd_dir/$name.avd"
  if [ -f "$ini_file" ] && [ -d "$image_dir" ]; then
    if grep -F -q "$image" "$ini_file" 2>/dev/null; then
      ba_log "Reusing existing Android Virtual Device $name"
      configure_avd "$avd_dir" "$name"
      return
    fi
    ba_log "Existing Android Virtual Device $name uses a different system image; recreating"
    rm -f "$ini_file"
    rm -rf "$image_dir"
  fi
  if ! ANDROID_AVD_HOME="$avd_dir" "$manager" create avd -n "$name" -k "$image" --device "2.7in QVGA" --force >/dev/null <<<'no'
  then
    ba_log "Failed to create Android Virtual Device $name using image $image" >&2
    find "$avd_dir" -maxdepth 2 -mindepth 1 -print | sed 's/^/[build-android-app] AVD: /' >&2 || true
    exit 1
  fi
  if [ ! -f "$ini_file" ]; then
    ba_log "AVD $name was created but configuration file $ini_file is missing" >&2
    find "$avd_dir" -maxdepth 1 -mindepth 1 -print | sed 's/^/[build-android-app] AVD: /' >&2 || true
    exit 1
  fi
  configure_avd "$avd_dir" "$name"
}

configure_avd() {
  local avd_dir="$1"
  local name="$2"
  local cfg="$avd_dir/$name.avd/config.ini"
  if [ ! -f "$cfg" ]; then
    return
  fi
  declare -A settings=(
    ["hw.ramSize"]=3072
    ["disk.dataPartition.size"]=4096M
    ["hw.bluetooth"]=no
    ["hw.camera.back"]=none
    ["hw.camera.front"]=none
    ["hw.audioInput"]=no
    ["hw.audioOutput"]=no
  )
  local key value
  for key in "${!settings[@]}"; do
    value="${settings[$key]}"
    if grep -q "^${key}=" "$cfg" 2>/dev/null; then
      sed -i "s/^${key}=.*/${key}=${value}/" "$cfg"
    else
      echo "${key}=${value}" >>"$cfg"
    fi
  done
}

wait_for_emulator() {
  local serial="$1"
  "$ADB_BIN" start-server >/dev/null
  "$ADB_BIN" -s "$serial" wait-for-device

  local boot_timeout="${EMULATOR_BOOT_TIMEOUT_SECONDS:-900}"
  if ! [[ "$boot_timeout" =~ ^[0-9]+$ ]] || [ "$boot_timeout" -le 0 ]; then
    ba_log "Invalid EMULATOR_BOOT_TIMEOUT_SECONDS=$boot_timeout provided; falling back to 900"
    boot_timeout=900
  fi
  local poll_interval="${EMULATOR_BOOT_POLL_INTERVAL_SECONDS:-5}"
  if ! [[ "$poll_interval" =~ ^[0-9]+$ ]] || [ "$poll_interval" -le 0 ]; then
    poll_interval=5
  fi
  local status_log_interval="${EMULATOR_BOOT_STATUS_LOG_INTERVAL_SECONDS:-30}"
  if ! [[ "$status_log_interval" =~ ^[0-9]+$ ]] || [ "$status_log_interval" -le 0 ]; then
    status_log_interval=30
  fi

  local deadline=$((SECONDS + boot_timeout))
  local last_log=$SECONDS
  local boot_completed="0"
  local dev_boot_completed="0"
  local bootanim=""
  local bootanim_exit=""
  local device_state=""
  local boot_ready=0

  while [ $SECONDS -lt $deadline ]; do
    device_state="$($ADB_BIN -s "$serial" get-state 2>/dev/null | tr -d '\r')"
    if [ "$device_state" != "device" ]; then
      if [ $((SECONDS - last_log)) -ge $status_log_interval ]; then
        ba_log "Waiting for emulator $serial to become ready (state=$device_state)"
        last_log=$SECONDS
      fi
      sleep "$poll_interval"
      continue
    fi

    boot_completed="$($ADB_BIN -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    dev_boot_completed="$($ADB_BIN -s "$serial" shell getprop dev.bootcomplete 2>/dev/null | tr -d '\r')"
    bootanim="$($ADB_BIN -s "$serial" shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r')"
    bootanim_exit="$($ADB_BIN -s "$serial" shell getprop service.bootanim.exit 2>/dev/null | tr -d '\r')"

    if { [ "$boot_completed" = "1" ] || [ "$boot_completed" = "true" ]; } \
      && { [ -z "$dev_boot_completed" ] || [ "$dev_boot_completed" = "1" ] || [ "$dev_boot_completed" = "true" ]; }; then
      boot_ready=1
      break
    fi

    if [ "$bootanim" = "stopped" ] || [ "$bootanim_exit" = "1" ]; then
      boot_ready=2
      break
    fi

    if [ $((SECONDS - last_log)) -ge $status_log_interval ]; then
      ba_log "Waiting for emulator $serial to boot (sys.boot_completed=${boot_completed:-<unset>} dev.bootcomplete=${dev_boot_completed:-<unset>} bootanim=${bootanim:-<unset>} bootanim_exit=${bootanim_exit:-<unset>})"
      last_log=$SECONDS
    fi
    sleep "$poll_interval"
  done

  if [ $boot_ready -eq 0 ]; then
    ba_log "Emulator $serial failed to boot within ${boot_timeout}s (sys.boot_completed=${boot_completed:-<unset>} dev.bootcomplete=${dev_boot_completed:-<unset>} bootanim=${bootanim:-<unset>} bootanim_exit=${bootanim_exit:-<unset>} state=${device_state:-<unset>})" >&2
    return 1
  elif [ $boot_ready -eq 2 ]; then
    ba_log "Emulator $serial reported boot animation stopped; proceeding without bootcomplete properties"
  fi

  "$ADB_BIN" -s "$serial" shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
  "$ADB_BIN" -s "$serial" shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
  "$ADB_BIN" -s "$serial" shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
  "$ADB_BIN" -s "$serial" shell input keyevent 82 >/dev/null 2>&1 || true
  "$ADB_BIN" -s "$serial" shell wm dismiss-keyguard >/dev/null 2>&1 || true
  return 0
}

wait_for_package_service() {
  local serial="$1"
  local timeout="${PACKAGE_SERVICE_TIMEOUT_SECONDS:-${PACKAGE_SERVICE_TIMEOUT:-600}}"
  local per_try="${PACKAGE_SERVICE_PER_TRY_TIMEOUT_SECONDS:-${PACKAGE_SERVICE_PER_TRY_TIMEOUT:-5}}"
  if ! [[ "$timeout" =~ ^[0-9]+$ ]] || [ "$timeout" -le 0 ]; then
    timeout=600
  fi
  if ! [[ "$per_try" =~ ^[0-9]+$ ]] || [ "$per_try" -le 0 ]; then
    per_try=5
  fi

  local deadline=$((SECONDS + timeout))
  local last_log=$SECONDS

  while [ $SECONDS -lt $deadline ]; do
    local boot_ok ce_ok
    boot_ok="$($ADB_BIN -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    ce_ok="$($ADB_BIN -s "$serial" shell getprop sys.user.0.ce_available 2>/dev/null | tr -d '\r')"

    if timeout "$per_try" "$ADB_BIN" -s "$serial" shell cmd package path android >/dev/null 2>&1 \
      || timeout "$per_try" "$ADB_BIN" -s "$serial" shell pm path android >/dev/null 2>&1 \
      || timeout "$per_try" "$ADB_BIN" -s "$serial" shell cmd package list packages >/dev/null 2>&1 \
      || timeout "$per_try" "$ADB_BIN" -s "$serial" shell pm list packages >/dev/null 2>&1 \
      || timeout "$per_try" "$ADB_BIN" -s "$serial" shell dumpsys package >/dev/null 2>&1; then
      return 0
    fi

    if [ $((SECONDS - last_log)) -ge 10 ]; then
      ba_log "Waiting for package manager service on $serial (boot_ok=${boot_ok:-?} ce_ok=${ce_ok:-?})"
      last_log=$SECONDS
    fi
    sleep 2
  done

  ba_log "Package manager service not ready on $serial after ${timeout}s" >&2
  return 1
}

wait_for_api_level() {
  local serial="$1"
  local timeout="${API_LEVEL_TIMEOUT_SECONDS:-600}"
  local per_try="${API_LEVEL_PER_TRY_TIMEOUT_SECONDS:-5}"
  if ! [[ "$timeout" =~ ^[0-9]+$ ]] || [ "$timeout" -le 0 ]; then
    timeout=600
  fi
  if ! [[ "$per_try" =~ ^[0-9]+$ ]] || [ "$per_try" -le 0 ]; then
    per_try=5
  fi

  local deadline=$((SECONDS + timeout))
  local last_log=$SECONDS
  local sdk=""

  while [ $SECONDS -lt $deadline ]; do
    if sdk="$(timeout "$per_try" "$ADB_BIN" -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' | tr -d '\n')"; then
      if [[ "$sdk" =~ ^[0-9]+$ ]]; then
        ba_log "Device API level is $sdk"
        return 0
      fi
    fi
    if [ $((SECONDS - last_log)) -ge 10 ]; then
      ba_log "Waiting for ro.build.version.sdk on $serial"
      last_log=$SECONDS
    fi
    sleep 2
  done

  ba_log "ro.build.version.sdk not available after ${timeout}s" >&2
  return 1
}

dump_emulator_diagnostics() {
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell getprop | sed 's/^/[build-android-app] getprop: /' || true
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell logcat -d -t 2000 \
    | grep -v -E 'com\\.android\\.bluetooth|BtGd|bluetooth' \
    | tail -n 200 | sed 's/^/[build-android-app] logcat: /' || true
}

log_instrumentation_state() {
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path android | sed 's/^/[build-android-app] pm path android: /' || true

  local instrumentation_list
  instrumentation_list="$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list instrumentation 2>/dev/null || true)"
  if [ -n "$instrumentation_list" ]; then
    printf '%s\n' "$instrumentation_list" | sed 's/^/[build-android-app] instrumentation: /'
  else
    ba_log "No instrumentation targets reported on $EMULATOR_SERIAL before installation"
  fi

  local have_test_apk=0
  if "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path "${PACKAGE_NAME}.test" >/dev/null 2>&1; then
    have_test_apk=1
    "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path "${PACKAGE_NAME}.test" \
      | sed 's/^/[build-android-app] test-apk: /'
  else
    ba_log "Test APK for ${PACKAGE_NAME}.test not yet installed on $EMULATOR_SERIAL"
  fi

  local package_regex package_list package_matches
  package_regex="${PACKAGE_NAME//./\.}"
  package_list="$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list packages 2>/dev/null || true)"
  if [ -n "$package_list" ]; then
    package_matches="$(printf '%s\n' "$package_list" | grep -E "${package_regex}|${package_regex}\.test" || true)"
    if [ -n "$package_matches" ]; then
      printf '%s\n' "$package_matches" | sed 's/^/[build-android-app] package: /'
    else
      ba_log "Packages matching $PACKAGE_NAME not yet installed on $EMULATOR_SERIAL"
    fi
  else
    ba_log "Package manager returned no packages on $EMULATOR_SERIAL"
  fi
}

collect_instrumentation_crash() {
  local attempt="$1"
  local crash_log="$FINAL_ARTIFACT_DIR/ui-test-crash-attempt-$attempt.log"
  local latest_log="$FINAL_ARTIFACT_DIR/ui-test-crash.log"
  local log_tmp filtered_tmp tomb_tmp tombstones

  log_tmp="$(mktemp "${TMPDIR:-/tmp}/ui-test-logcat.XXXXXX")"
  filtered_tmp="$(mktemp "${TMPDIR:-/tmp}/ui-test-logcat-filtered.XXXXXX")"
  tomb_tmp="$(mktemp "${TMPDIR:-/tmp}/ui-test-tombstones.XXXXXX")"

  : >"$crash_log"

  if "$ADB_BIN" -s "$EMULATOR_SERIAL" shell logcat -d -t 2000 >"$log_tmp" 2>/dev/null; then
    if grep -E "FATAL EXCEPTION|AndroidRuntime|Process: ${PACKAGE_NAME}(\\.test)?|Abort message:" "$log_tmp" >"$filtered_tmp" 2>/dev/null; then
      if [ -s "$filtered_tmp" ]; then
        while IFS= read -r line; do
          echo "[build-android-app] crash: $line"
        done <"$filtered_tmp"
        cat "$filtered_tmp" >>"$crash_log"
      fi
    fi
  fi

  tombstones="$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell ls /data/tombstones 2>/dev/null | tail -n 3 || true)"
  if [ -n "$tombstones" ]; then
    printf '%s\n' "$tombstones" | sed 's/^/[build-android-app] tombstone: /'
    printf '%s\n' "$tombstones" >"$tomb_tmp"
    cat "$tomb_tmp" >>"$crash_log"
  fi

  if [ -s "$crash_log" ]; then
    cp "$crash_log" "$latest_log" 2>/dev/null || true
  else
    rm -f "$crash_log"
  fi

  rm -f "$log_tmp" "$filtered_tmp" "$tomb_tmp"
}

stop_emulator() {
  if [ -n "${EMULATOR_SERIAL:-}" ]; then
    "$ADB_BIN" -s "$EMULATOR_SERIAL" emu kill >/dev/null 2>&1 || true
  fi
  if [ -n "${EMULATOR_PID:-}" ]; then
    kill "$EMULATOR_PID" >/dev/null 2>&1 || true
    wait "$EMULATOR_PID" 2>/dev/null || true
  fi
}

install_android_packages "$SDKMANAGER_BIN"

ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb"
if [ ! -x "$ADB_BIN" ]; then
  if command -v adb >/dev/null 2>&1; then
    ADB_BIN="$(command -v adb)"
  else
    ba_log "adb not found in Android SDK. Ensure platform-tools are installed." >&2
    exit 1
  fi
fi

EMULATOR_BIN="$ANDROID_SDK_ROOT/emulator/emulator"
if [ ! -x "$EMULATOR_BIN" ]; then
  if command -v emulator >/dev/null 2>&1; then
    EMULATOR_BIN="$(command -v emulator)"
  else
    ba_log "Android emulator binary not found" >&2
    exit 1
  fi
fi

AVD_NAME="cn1UiTestAvd"
SYSTEM_IMAGE="system-images;android-35;default;x86_64"
AVD_CACHE_ROOT="${AVD_CACHE_ROOT:-${RUNNER_TEMP:-$HOME}/cn1-android-avd}"
mkdir -p "$AVD_CACHE_ROOT"
AVD_HOME="$AVD_CACHE_ROOT"
ba_log "Using AVD home at $AVD_HOME"
create_avd "$AVDMANAGER_BIN" "$AVD_NAME" "$SYSTEM_IMAGE" "$AVD_HOME"

ANDROID_AVD_HOME="$AVD_HOME" "$ADB_BIN" start-server >/dev/null

mapfile -t EXISTING_EMULATORS < <("$ADB_BIN" devices | awk '/^emulator-/{print $1}')

EMULATOR_PORT="${EMULATOR_PORT:-5560}"
if ! [[ "$EMULATOR_PORT" =~ ^[0-9]+$ ]]; then
  EMULATOR_PORT=5560
elif [ $((EMULATOR_PORT % 2)) -ne 0 ] || [ $EMULATOR_PORT -lt 5554 ] || [ $EMULATOR_PORT -gt 5584 ]; then
  # emulator requires an even console port between 5554-5584; fall back if invalid
  EMULATOR_PORT=5560
fi
EMULATOR_SERIAL="emulator-$EMULATOR_PORT"

EMULATOR_LOG="$GRADLE_PROJECT_DIR/emulator.log"
ba_log "Starting headless Android emulator $AVD_NAME on port $EMULATOR_PORT"
ANDROID_AVD_HOME="$AVD_HOME" "$EMULATOR_BIN" -avd "$AVD_NAME" -port "$EMULATOR_PORT" \
  -no-window -no-snapshot -gpu swiftshader_indirect -no-audio -no-boot-anim \
  -accel off -no-accel -camera-back none -camera-front none -skip-adb-auth \
  -feature -Vulkan -netfast -memory 3072 >"$EMULATOR_LOG" 2>&1 &
EMULATOR_PID=$!
trap stop_emulator EXIT

sleep 5

detect_emulator_serial() {
  local deadline current_devices serial existing
  deadline=$((SECONDS + 180))
  while [ $SECONDS -lt $deadline ]; do
    mapfile -t current_devices < <("$ADB_BIN" devices | awk '/^emulator-/{print $1}')
    for serial in "${current_devices[@]}"; do
      for existing in "${EXISTING_EMULATORS[@]}"; do
        if [ "$serial" = "$existing" ]; then
          # already present before launch; ignore unless it matches requested serial
          if [ "$serial" = "$EMULATOR_SERIAL" ]; then
            EMULATOR_SERIAL="$serial"
            return 0
          fi
          serial=""
          break
        fi
      done
      if [ -n "$serial" ]; then
        EMULATOR_SERIAL="$serial"
        return 0
      fi
    done
    sleep 2
  done
  return 1
}

if ! detect_emulator_serial; then
  mapfile -t CURRENT_EMULATORS < <("$ADB_BIN" devices | awk '/^emulator-/{print $1}')
  if [ -z "${EMULATOR_SERIAL:-}" ] && [ ${#CURRENT_EMULATORS[@]} -gt 0 ]; then
    EMULATOR_SERIAL="${CURRENT_EMULATORS[0]}"
  fi
  if [ -z "${EMULATOR_SERIAL:-}" ] || ! printf '%s\n' "${CURRENT_EMULATORS[@]}" | grep -Fxq "$EMULATOR_SERIAL"; then
    ba_log "Failed to detect emulator serial after launch" >&2
    if [ -f "$EMULATOR_LOG" ]; then
      ba_log "Emulator log tail:" >&2
      tail -n 40 "$EMULATOR_LOG" | sed 's/^/[build-android-app] | /' >&2
    fi
    stop_emulator
    exit 1
  fi
fi
ba_log "Using emulator serial $EMULATOR_SERIAL"

if ! wait_for_emulator "$EMULATOR_SERIAL"; then
  stop_emulator
  exit 1
fi

POST_BOOT_GRACE="${EMULATOR_POST_BOOT_GRACE_SECONDS:-20}"
if ! [[ "$POST_BOOT_GRACE" =~ ^[0-9]+$ ]] || [ "$POST_BOOT_GRACE" -lt 0 ]; then
  POST_BOOT_GRACE=20
fi
if [ "$POST_BOOT_GRACE" -gt 0 ]; then
  ba_log "Waiting ${POST_BOOT_GRACE}s for emulator system services to stabilize"
  sleep "$POST_BOOT_GRACE"
fi

if ! wait_for_package_service "$EMULATOR_SERIAL"; then
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi

"$ADB_BIN" -s "$EMULATOR_SERIAL" shell settings put global device_provisioned 1 >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell settings put secure user_setup_complete 1 >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell input keyevent 82 >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell wm dismiss-keyguard >/dev/null 2>&1 || true

if ! wait_for_api_level "$EMULATOR_SERIAL"; then
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi

"$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path android | sed 's/^/[build-android-app] pm path android: /' || true

"$ADB_BIN" kill-server >/dev/null 2>&1 || true
"$ADB_BIN" start-server >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" wait-for-device
export ANDROID_SERIAL="$EMULATOR_SERIAL"

ASSEMBLE_TIMEOUT_SECONDS="${ASSEMBLE_TIMEOUT_SECONDS:-900}"
if ! [[ "$ASSEMBLE_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] || [ "$ASSEMBLE_TIMEOUT_SECONDS" -le 0 ]; then
  ASSEMBLE_TIMEOUT_SECONDS=900
fi

GRADLE_ASSEMBLE_CMD=(
  "./gradlew"
  "--no-daemon"
  ":app:assembleDebug"
  ":app:assembleDebugAndroidTest"
  "-x"
  "lint"
  "-x"
  "test"
)
if command -v timeout >/dev/null 2>&1; then
  ba_log "Building app and androidTest APKs with external timeout of ${ASSEMBLE_TIMEOUT_SECONDS}s"
  GRADLE_ASSEMBLE_CMD=("timeout" "$ASSEMBLE_TIMEOUT_SECONDS" "${GRADLE_ASSEMBLE_CMD[@]}")
else
  ba_log "timeout command not found; running Gradle assemble tasks without external watchdog"
fi

GRADLE_ASSEMBLE_LOG="$GRADLE_PROJECT_DIR/gradle-ui-assemble.log"
set +e
(
  cd "$GRADLE_PROJECT_DIR"
  "${GRADLE_ASSEMBLE_CMD[@]}" | tee "$GRADLE_ASSEMBLE_LOG"
  exit "${PIPESTATUS[0]}"
)
ASSEMBLE_EXIT_CODE=$?
set -e

if [ -f "$GRADLE_ASSEMBLE_LOG" ]; then
  cp "$GRADLE_ASSEMBLE_LOG" "$FINAL_ARTIFACT_DIR/ui-test-assemble.log"
  ba_log "Gradle assemble log saved to $FINAL_ARTIFACT_DIR/ui-test-assemble.log"
fi

if [ "$ASSEMBLE_EXIT_CODE" -ne 0 ]; then
  ba_log "Gradle assemble tasks exited with status $ASSEMBLE_EXIT_CODE"
  stop_emulator
  exit 1
fi

APP_APK="$(find "$GRADLE_PROJECT_DIR/app/build/outputs/apk/debug" -maxdepth 1 -name '*-debug.apk' | head -n1 || true)"
TEST_APK="$(find "$GRADLE_PROJECT_DIR/app/build/outputs/apk/androidTest/debug" -maxdepth 1 -name '*-debug-androidTest.apk' | head -n1 || true)"

if [ -z "$APP_APK" ] || [ ! -f "$APP_APK" ]; then
  ba_log "App APK not found after assemble tasks" >&2
  stop_emulator
  exit 1
fi
if [ -z "$TEST_APK" ] || [ ! -f "$TEST_APK" ]; then
  ba_log "androidTest APK not found after assemble tasks" >&2
  stop_emulator
  exit 1
fi

MERGED_MANIFEST="$APP_MODULE_DIR/build/intermediates/packaged_manifests/debug/AndroidManifest.xml"
if [ -f "$MERGED_MANIFEST" ]; then
  grep -n "${MAIN_NAME}Stub" "$MERGED_MANIFEST" | sed 's/^/[build-android-app] merged-manifest: /' || true
else
  ba_log "Merged manifest not found at $MERGED_MANIFEST"
fi

adb_install_retry() {
  local serial="$1" apk="$2" tries=5
  local attempt
  for attempt in $(seq 1 "$tries"); do
    if command -v timeout >/dev/null 2>&1; then
      timeout 120 "$ADB_BIN" -s "$serial" install -r -t "$apk" && return 0
    else
      "$ADB_BIN" -s "$serial" install -r -t "$apk" && return 0
    fi
    if [ "$attempt" -lt "$tries" ]; then
      ba_log "install retry $attempt/$tries for $(basename "$apk")"
      "$ADB_BIN" -s "$serial" wait-for-device
      sleep 3
    fi
  done
  return 1
}

ba_log "Installing app APK: $APP_APK"
if ! adb_install_retry "$EMULATOR_SERIAL" "$APP_APK"; then
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi

ba_log "Installing androidTest APK: $TEST_APK"
if ! adb_install_retry "$EMULATOR_SERIAL" "$TEST_APK"; then
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi

APP_PACKAGE_PATH="$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path "$PACKAGE_NAME" 2>/dev/null | tr -d '\r' || true)"
if [ -n "$APP_PACKAGE_PATH" ]; then
  printf '%s\n' "$APP_PACKAGE_PATH" | sed 's/^/[build-android-app] app-apk: /'
else
  ba_log "App package $PACKAGE_NAME not yet reported by pm path on $EMULATOR_SERIAL"
fi

TEST_PACKAGE_PATH="$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path "${PACKAGE_NAME}.test" 2>/dev/null | tr -d '\r' || true)"
if [ -n "$TEST_PACKAGE_PATH" ]; then
  printf '%s\n' "$TEST_PACKAGE_PATH" | sed 's/^/[build-android-app] test-apk: /'
else
  ba_log "Test package ${PACKAGE_NAME}.test not yet reported by pm path on $EMULATOR_SERIAL"
fi

log_instrumentation_state

RUNNER="$(
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list instrumentation \
    | tr -d '\r' \
    | grep -F "(target=$PACKAGE_NAME)" \
    | head -n1 \
    | sed -E 's/^instrumentation:([^ ]+).*/\1/'
)"
if [ -z "$RUNNER" ]; then
  ba_log "No instrumentation runner found for $PACKAGE_NAME"
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list instrumentation | sed 's/^/[build-android-app] instrumentation: /'
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi
ba_log "Using instrumentation runner: $RUNNER"

ba_log "Inspecting launcher activities for $PACKAGE_NAME"
LAUNCH_RESOLVE_OUTPUT="$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell cmd package resolve-activity --brief "$PACKAGE_NAME" 2>&1 || true)"
if [ -n "$LAUNCH_RESOLVE_OUTPUT" ]; then
  printf '%s\n' "$LAUNCH_RESOLVE_OUTPUT" | sed 's/^/[build-android-app] resolve-launch: /'
fi
STUB_ACTIVITY_FQCN="$PACKAGE_NAME/.${MAIN_NAME}Stub"
STUB_RESOLVE_OUTPUT="$(
    "$ADB_BIN" -s "$EMULATOR_SERIAL" shell cmd package resolve-activity --brief "$STUB_ACTIVITY_FQCN" 2>&1 || true
  )"
if [ -n "$STUB_RESOLVE_OUTPUT" ]; then
  printf '%s\n' "$STUB_RESOLVE_OUTPUT" | sed 's/^/[build-android-app] resolve-stub: /'
else
  ba_log "Unable to resolve stub activity $STUB_ACTIVITY_FQCN on device"
fi

"$ADB_BIN" -s "$EMULATOR_SERIAL" shell am force-stop "$PACKAGE_NAME" >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell am force-stop "${PACKAGE_NAME}.test" >/dev/null 2>&1 || true

UI_TEST_TIMEOUT_SECONDS="${UI_TEST_TIMEOUT_SECONDS:-900}"
if ! [[ "$UI_TEST_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] || [ "$UI_TEST_TIMEOUT_SECONDS" -le 0 ]; then
  ba_log "Invalid UI_TEST_TIMEOUT_SECONDS=$UI_TEST_TIMEOUT_SECONDS provided; falling back to 900"
  UI_TEST_TIMEOUT_SECONDS=900
fi

INSTRUMENT_EXIT_CODE=1

run_instrumentation() {
  local tries=3 delay=15 attempt exit_code=1
  local args=(-w -r -e clearPackageData true -e log true -e class "${PACKAGE_NAME}.${MAIN_NAME}UiTest")
  for attempt in $(seq 1 "$tries"); do
    local cmd=("$ADB_BIN" "-s" "$EMULATOR_SERIAL" "shell" "am" "instrument" "${args[@]}" "$RUNNER")
    if command -v timeout >/dev/null 2>&1; then
      cmd=("timeout" "$UI_TEST_TIMEOUT_SECONDS" "${cmd[@]}")
      ba_log "Instrumentation attempt $attempt/$tries with external timeout of ${UI_TEST_TIMEOUT_SECONDS}s"
    else
      ba_log "Instrumentation attempt $attempt/$tries without external watchdog"
    fi
    local attempt_log="$FINAL_ARTIFACT_DIR/ui-test-instrumentation-attempt-$attempt.log"
    "$ADB_BIN" -s "$EMULATOR_SERIAL" logcat -c >/dev/null 2>&1 || true
    set +e
    "${cmd[@]}" | tee "$attempt_log"
    exit_code=${PIPESTATUS[0]}
    set -e
    cp "$attempt_log" "$FINAL_ARTIFACT_DIR/ui-test-instrumentation.log" 2>/dev/null || true
    if [ "$exit_code" -ne 0 ]; then
      collect_instrumentation_crash "$attempt"
    fi
    if [ "$exit_code" -eq 0 ]; then
      INSTRUMENT_EXIT_CODE=0
      return 0
    fi
    if grep -q "INSTRUMENTATION_ABORTED: System has crashed." "$attempt_log" 2>/dev/null && [ "$attempt" -lt "$tries" ]; then
      ba_log "System crashed during instrumentation attempt $attempt; retrying after ${delay}s"
      sleep "$delay"
      continue
    fi
    INSTRUMENT_EXIT_CODE=$exit_code
    return $exit_code
  done
  INSTRUMENT_EXIT_CODE=$exit_code
  return $exit_code
}

if ! run_instrumentation; then
  ba_log "Instrumentation command exited with status $INSTRUMENT_EXIT_CODE"
  dump_emulator_diagnostics
fi

copy_device_file() {
  local src="$1"
  local dest="$2"
  if ! "$ADB_BIN" -s "$EMULATOR_SERIAL" shell run-as "$PACKAGE_NAME" ls "$src" >/dev/null 2>&1; then
    return 1
  fi
  if "$ADB_BIN" -s "$EMULATOR_SERIAL" exec-out run-as "$PACKAGE_NAME" cat "$src" >"$dest"; then
    return 0
  fi
  rm -f "$dest"
  return 1
}

SCREENSHOT_STATUS=0
ANDROID_SCREENSHOT=""
CODENAMEONE_SCREENSHOT=""
DEFAULT_SCREENSHOT=""

SCREENSHOT_DIR_ON_DEVICE="files/ui-test-screenshots"
ANDROID_SCREENSHOT_NAME="${MAIN_NAME}-android-ui.png"
CODENAMEONE_SCREENSHOT_NAME="${MAIN_NAME}-codenameone-ui.png"

ANDROID_SCREENSHOT_PATH_DEVICE="$SCREENSHOT_DIR_ON_DEVICE/$ANDROID_SCREENSHOT_NAME"
CODENAMEONE_SCREENSHOT_PATH_DEVICE="$SCREENSHOT_DIR_ON_DEVICE/$CODENAMEONE_SCREENSHOT_NAME"

ANDROID_SCREENSHOT_DEST="$FINAL_ARTIFACT_DIR/$ANDROID_SCREENSHOT_NAME"
CODENAMEONE_SCREENSHOT_DEST="$FINAL_ARTIFACT_DIR/$CODENAMEONE_SCREENSHOT_NAME"

if copy_device_file "$ANDROID_SCREENSHOT_PATH_DEVICE" "$ANDROID_SCREENSHOT_DEST"; then
  ba_log "Android screenshot copied to $ANDROID_SCREENSHOT_DEST"
  ANDROID_SCREENSHOT="$ANDROID_SCREENSHOT_DEST"
  DEFAULT_SCREENSHOT="$ANDROID_SCREENSHOT_DEST"
else
  ba_log "Android screenshot not found at $ANDROID_SCREENSHOT_PATH_DEVICE" >&2
  SCREENSHOT_STATUS=1
fi

if copy_device_file "$CODENAMEONE_SCREENSHOT_PATH_DEVICE" "$CODENAMEONE_SCREENSHOT_DEST"; then
  ba_log "Codename One screenshot copied to $CODENAMEONE_SCREENSHOT_DEST"
  CODENAMEONE_SCREENSHOT="$CODENAMEONE_SCREENSHOT_DEST"
  if [ -z "$DEFAULT_SCREENSHOT" ]; then
    DEFAULT_SCREENSHOT="$CODENAMEONE_SCREENSHOT_DEST"
  fi
else
  ba_log "Codename One screenshot not found at $CODENAMEONE_SCREENSHOT_PATH_DEVICE" >&2
  SCREENSHOT_STATUS=1
fi

if [ -f "$EMULATOR_LOG" ]; then
  cp "$EMULATOR_LOG" "$FINAL_ARTIFACT_DIR/emulator.log" || true
fi

if [ -n "${GITHUB_ENV:-}" ]; then
  if [ -n "$DEFAULT_SCREENSHOT" ]; then
    printf 'CN1_UI_TEST_SCREENSHOT=%s\n' "$DEFAULT_SCREENSHOT" >> "$GITHUB_ENV"
  fi
  if [ -n "$ANDROID_SCREENSHOT" ]; then
    printf 'CN1_UI_TEST_ANDROID_SCREENSHOT=%s\n' "$ANDROID_SCREENSHOT" >> "$GITHUB_ENV"
  fi
  if [ -n "$CODENAMEONE_SCREENSHOT" ]; then
    printf 'CN1_UI_TEST_CODENAMEONE_SCREENSHOT=%s\n' "$CODENAMEONE_SCREENSHOT" >> "$GITHUB_ENV"
  fi
fi

export JAVA_HOME="$ORIGINAL_JAVA_HOME"

stop_emulator
trap - EXIT

if [ "$INSTRUMENT_EXIT_CODE" -ne 0 ]; then
  exit "$INSTRUMENT_EXIT_CODE"
fi

if [ "$SCREENSHOT_STATUS" -ne 0 ]; then
  exit 1
fi

APK_PATH=$(find "$GRADLE_PROJECT_DIR" -path "*/outputs/apk/debug/*.apk" | head -n 1 || true)
[ -n "$APK_PATH" ] || { ba_log "Gradle build completed but no APK was found" >&2; exit 1; }
ba_log "Successfully built Android APK at $APK_PATH"
