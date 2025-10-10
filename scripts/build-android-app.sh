#!/usr/bin/env bash
# Build a sample "Hello Codename One" Android application using the locally-built Codename One Android port
set -euo pipefail

ba_log() { echo "[build-android-app] $1"; }

run_with_timeout() {
  local duration="$1"
  shift
  if command -v timeout >/dev/null 2>&1; then
    timeout "$duration" "$@"
  else
    "$@"
  fi
}

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

FQCN="${PACKAGE_NAME}.${MAIN_NAME}Stub"
MANIFEST_FILE="$APP_MODULE_DIR/src/main/AndroidManifest.xml"

dump_manifest_merger_reports() {
  local blame_a="$APP_MODULE_DIR/build/intermediates/manifest_merge_blame_file/debug/manifest-merger-blame-debug-report.txt"
  local blame_b="$APP_MODULE_DIR/build/intermediates/incremental/processDebugMainManifest/manifest-merger-blame-report.txt"
  local merged="$APP_MODULE_DIR/build/intermediates/packaged_manifests/debug/AndroidManifest.xml"

  for report in "$blame_a" "$blame_b"; do
    if [ -f "$report" ]; then
      ba_log "manifest-merger blame report: $report"
      sed -n '1,200p' "$report" | sed 's/^/[build-android-app] manifest-blame: /'
    fi
  done

  if [ -f "$merged" ]; then
    ba_log "merged manifest (first 120 lines): $merged"
    sed -n '1,120p' "$merged" | sed 's/^/[build-android-app] merged-manifest: /'
  fi
}

ba_log "Normalizing Codename One stub activity manifest"
mkdir -p "$(dirname "$MANIFEST_FILE")"
if [ ! -f "$MANIFEST_FILE" ]; then
  cat >"$MANIFEST_FILE" <<'EOF_MANIFEST'
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application/>
</manifest>
EOF_MANIFEST
  ba_log "Created minimal Android manifest at $MANIFEST_FILE"
fi

grep -q '<application' "$MANIFEST_FILE" || sed -i 's#</manifest>#  <application/>\n</manifest>#' "$MANIFEST_FILE"

# Remove deprecated package attribute and inline <uses-sdk/> declarations
perl -0777 -pe 's/\s+package="[^"]*"//; s#<uses-sdk\b[^>]*/>\s*##g' -i "$MANIFEST_FILE"

# Ensure tools namespace for tools:node annotations
if ! grep -Fq 'xmlns:tools=' "$MANIFEST_FILE"; then
  perl -0777 -pe 's#<manifest\b([^>]*)>#<manifest\1 xmlns:tools="http://schemas.android.com/tools">#' -i "$MANIFEST_FILE"
fi

# Normalize existing stub declarations rather than inserting new ones
python3 - "$MANIFEST_FILE" "$FQCN" "$PACKAGE_NAME" "$MAIN_NAME" <<'PY'
import re
import sys
from pathlib import Path

manifest_path, fqcn, package_name, main_name = sys.argv[1:5]
manifest = Path(manifest_path)
text = manifest.read_text()

text = re.sub(r'<!--\s*CN1-STUB-BEGIN\s*-->.*?<!--\s*CN1-STUB-END\s*-->', '', text, flags=re.S)

name_pattern = re.compile(
    r'(android:name=")(?:(?:%s)|(?:\.?%sStub)|(?:%s\.%sStub))"' % (
        re.escape(fqcn), re.escape(main_name), re.escape(package_name), re.escape(main_name)
    )
)
text = name_pattern.sub(r'\1%s"' % fqcn, text)

activity_pattern = re.compile(
    r'<activity\b[^>]*android:name="%s"[^>]*>(?:.*?)</activity>|<activity\b[^>]*android:name="%s"[^>]*/>' % (
        re.escape(fqcn), re.escape(fqcn)
    ),
    flags=re.S,
)

seen = {"value": False}

def replace_activity(match):
    body = match.group(0)
    body = re.sub(r'\s+tools:node="[^"]*"', '', body)
    if 'android:exported=' in body:
        body = re.sub(r'android:exported="[^"]*"', 'android:exported="true"', body, count=1)
    else:
        close = body.find('>')
        if close != -1:
            body = body[:close] + ' android:exported="true"' + body[close:]
    if seen["value"]:
        return ''
    seen["value"] = True
    return body

text = activity_pattern.sub(replace_activity, text)

if not seen["value"]:
    raise SystemExit(f"Stub activity declaration not found in manifest: {manifest_path}")

manifest.write_text(text)
PY

STUB_DECL_COUNT=$(grep -c "android:name=\"$FQCN\"" "$MANIFEST_FILE" || true)
ba_log "Stub activity declarations present after normalization: $STUB_DECL_COUNT"
if [ "$STUB_DECL_COUNT" -ne 1 ]; then
  ba_log "Expected exactly one stub activity declaration after normalization" >&2
  sed -n '1,160p' "$MANIFEST_FILE" | sed 's/^/[build-android-app] manifest: /'
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

ensure_gradle_package_config() {
  python3 - "$APP_BUILD_GRADLE" "$PACKAGE_NAME" <<'PY'
import sys
import re
from pathlib import Path

path = Path(sys.argv[1])
package_name = sys.argv[2]
text = path.read_text()
original = text
messages = []

def ensure_application_plugin(source: str) -> str:
    plugin_id = "com.android.application"
    if plugin_id in source:
        return source
    if "plugins" in source:
        updated = re.sub(r"(plugins\s*\{)", r"\1\n    id \"%s\"" % plugin_id, source, count=1)
        if updated != source:
            messages.append(f"Applied {plugin_id} via plugins block")
            return updated
    messages.append(f"Applied {plugin_id} via legacy apply plugin syntax")
    return f"apply plugin: \"{plugin_id}\"\n" + source

def ensure_android_block(source: str) -> str:
    if re.search(r"android\s*\{", source):
        return source
    messages.append("Inserted android block")
    return source + "\nandroid {\n}\n"

def ensure_namespace(source: str) -> str:
    pattern = re.compile(r"\bnamespace\s+[\"']([^\"']+)[\"']")
    match = pattern.search(source)
    if match:
        if match.group(1) != package_name:
            start, end = match.span()
            source = source[:start] + f"namespace \"{package_name}\"" + source[end:]
            messages.append(f"Updated namespace to {package_name}")
        return source
    android_match = re.search(r"android\s*\{", source)
    if not android_match:
        raise SystemExit("Unable to locate android block when inserting namespace")
    insert = android_match.end()
    source = source[:insert] + f"\n    namespace \"{package_name}\"" + source[insert:]
    messages.append(f"Inserted namespace {package_name}")
    return source

def ensure_default_config(source: str) -> str:
    if re.search(r"defaultConfig\s*\{", source):
        return source
    android_match = re.search(r"android\s*\{", source)
    if not android_match:
        raise SystemExit("Unable to locate android block when creating defaultConfig")
    insert = android_match.end()
    snippet = "\n    defaultConfig {\n    }"
    messages.append("Inserted defaultConfig block")
    return source[:insert] + snippet + source[insert:]

def ensure_application_id(source: str) -> str:
    pattern = re.compile(r"\bapplicationId\s+[\"']([^\"']+)[\"']")
    match = pattern.search(source)
    if match:
        if match.group(1) != package_name:
            start, end = match.span()
            indent = source[:start].split("\n")[-1].split("applicationId")[0]
            source = source[:start] + f"{indent}applicationId \"{package_name}\"" + source[end:]
            messages.append(f"Updated applicationId to {package_name}")
        return source
    default_match = re.search(r"defaultConfig\s*\{", source)
    if not default_match:
        raise SystemExit("Unable to locate defaultConfig when inserting applicationId")
    insert = default_match.end()
    source = source[:insert] + f"\n        applicationId \"{package_name}\"" + source[insert:]
    messages.append(f"Inserted applicationId {package_name}")
    return source

def ensure_compile_sdk(source: str) -> str:
    pattern = re.compile(r"\bcompileSdk(?:Version)?\s+(\d+)")
    match = pattern.search(source)
    desired = "compileSdk 35"
    if match:
        start, end = match.span()
        if match.group(0) != desired:
            source = source[:start] + desired + source[end:]
            messages.append("Updated compileSdk to 35")
        return source
    android_match = re.search(r"android\s*\{", source)
    if not android_match:
        raise SystemExit("Unable to locate android block when inserting compileSdk")
    insert = android_match.end()
    source = source[:insert] + f"\n    {desired}" + source[insert:]
    messages.append("Inserted compileSdk 35")
    return source

def ensure_default_config_value(source: str, key: str, value: str, *, quoted: bool = False) -> str:
    pattern = re.compile(rf"{key}(?:Version)?\s+[\"']?([^\"'\s]+)[\"']?")
    default_match = re.search(r"defaultConfig\s*\{", source)
    if not default_match:
        raise SystemExit(f"Unable to locate defaultConfig when inserting {key}")
    block_start = default_match.end()
    block_end = _find_matching_brace(source, default_match.start())
    block = source[block_start:block_end]
    match = pattern.search(block)
    replacement_value = f'"{value}"' if quoted else value
    replacement = f"        {key} {replacement_value}"
    if match:
        if match.group(1) != value or "Version" in match.group(0):
            start = block_start + match.start()
            end = block_start + match.end()
            source = source[:start] + replacement + source[end:]
            messages.append(f"Updated {key} to {value}")
        return source
    insert = block_start
    source = source[:insert] + "\n" + replacement + source[insert:]
    messages.append(f"Inserted {key} {value}")
    return source

def _find_matching_brace(source: str, start: int) -> int:
    depth = 0
    for index in range(start, len(source)):
        char = source[index]
        if char == '{':
            depth += 1
        elif char == '}':
            depth -= 1
            if depth == 0:
                return index
    raise SystemExit("Failed to locate matching brace for defaultConfig block")

def ensure_test_instrumentation_runner(source: str) -> str:
    default_match = re.search(r"defaultConfig\s*\{", source)
    if not default_match:
        raise SystemExit("Unable to locate defaultConfig when inserting testInstrumentationRunner")
    block_start = default_match.end()
    block_end = _find_matching_brace(source, default_match.start())
    block = source[block_start:block_end]
    pattern = re.compile(r"testInstrumentationRunner\s+[\"']([^\"']+)[\"']")
    desired = "androidx.test.runner.AndroidJUnitRunner"
    match = pattern.search(block)
    replacement = f"        testInstrumentationRunner \"{desired}\""
    if match:
        if match.group(1) != desired:
            start = block_start + match.start()
            end = block_start + match.end()
            source = source[:start] + replacement + source[end:]
            messages.append("Updated testInstrumentationRunner to AndroidJUnitRunner")
        return source
    insert = block_start
    source = source[:insert] + "\n" + replacement + source[insert:]
    messages.append("Inserted testInstrumentationRunner AndroidJUnitRunner")
    return source

text = ensure_application_plugin(text)
text = ensure_android_block(text)
text = ensure_namespace(text)
text = ensure_default_config(text)
text = ensure_application_id(text)
text = ensure_compile_sdk(text)
text = ensure_default_config_value(text, "minSdk", "19")
text = ensure_default_config_value(text, "targetSdk", "35")
text = ensure_default_config_value(text, "versionCode", "100")
text = ensure_default_config_value(text, "versionName", "1.0", quoted=True)
text = ensure_test_instrumentation_runner(text)

if text != original:
    path.write_text(text)

for message in messages:
    print(message)
PY
}

if ! GRADLE_PACKAGE_LOG=$(ensure_gradle_package_config); then
  ba_log "Failed to align namespace/applicationId with Codename One package" >&2
  exit 1
fi
if [ -n "$GRADLE_PACKAGE_LOG" ]; then
  while IFS= read -r line; do
    [ -n "$line" ] && ba_log "$line"
  done <<<"$GRADLE_PACKAGE_LOG"
fi

ba_log "app/build.gradle head after package alignment:"
sed -n '1,80p' "$APP_BUILD_GRADLE" | sed 's/^/[build-android-app] app.gradle: /'

chmod +x "$GRADLE_PROJECT_DIR/gradlew"

GRADLE_UPDATE_OUTPUT="$("$SCRIPT_DIR/update_android_ui_test_gradle.py" --package-name "$PACKAGE_NAME" "$APP_BUILD_GRADLE")"
if [ -n "$GRADLE_UPDATE_OUTPUT" ]; then
  while IFS= read -r line; do
    [ -n "$line" ] && ba_log "$line"
  done <<<"$GRADLE_UPDATE_OUTPUT"
fi

ba_log "Dependencies block after instrumentation update:"
awk '/^\s*dependencies\s*\{/{flag=1} flag{print} /^\s*\}/{if(flag){exit}}' "$APP_BUILD_GRADLE" \
  | sed 's/^/[build-android-app] | /'

# Final manifest sanity before Gradle preflight
if [ -f "$MANIFEST_FILE" ]; then
  tmp_manifest_pruned="$(mktemp)"
  FQCN="$FQCN" perl -0777 -pe '
    my $fq = quotemeta($ENV{FQCN});
    my $seen = 0;
    s{
       (<activity\b[^>]*android:name="$fq"[^>]*/>\s*)
      |
       (<activity\b[^>]*android:name="$fq"[^>]*>.*?</activity>\s*)
     }{
       $seen++ ? "" : $&
     }gsxe;
  ' "$MANIFEST_FILE" >"$tmp_manifest_pruned"
  mv "$tmp_manifest_pruned" "$MANIFEST_FILE"
  STUB_COUNT=$(grep -c "android:name=\"$FQCN\"" "$MANIFEST_FILE" || true)
  ba_log "Stub declarations in manifest after pruning: $STUB_COUNT"
  ba_log "Dumping manifest contents prior to preflight"
  nl -ba "$MANIFEST_FILE" | sed 's/^/[build-android-app] manifest: /'
  grep -n "android:name=\"$FQCN\"" "$MANIFEST_FILE" | sed 's/^/[build-android-app] manifest-match: /' || true
fi

ba_log "Validating manifest merge before assemble"
if ! (
  cd "$GRADLE_PROJECT_DIR" &&
  JAVA_HOME="$JAVA17_HOME" PATH="$JAVA17_HOME/bin:$PATH" ./gradlew --no-daemon :app:processDebugMainManifest
); then
  ba_log ":app:processDebugMainManifest failed during preflight" >&2
  dump_manifest_merger_reports
  exit 1
fi

FINAL_ARTIFACT_DIR="${CN1_TEST_SCREENSHOT_EXPORT_DIR:-$REPO_ROOT/build-artifacts}"
mkdir -p "$FINAL_ARTIFACT_DIR"
if [ -n "${GITHUB_ENV:-}" ]; then
  printf 'CN1_UI_TEST_ARTIFACT_DIR=%s\n' "$FINAL_ARTIFACT_DIR" >> "$GITHUB_ENV"
fi

ba_log "Invoking Gradle build in $GRADLE_PROJECT_DIR"
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
    "system-images;android-35;google_apis;x86_64" >/dev/null 2>&1 || true
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
    ["hw.ramSize"]=4096
    ["disk.dataPartition.size"]=8192M
    ["fastboot.forceColdBoot"]=yes
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

adb_framework_ready_once() {
  local serial="$1"
  local per_try="$2"
  local phase_timeout="$3"
  local log_interval="${FRAMEWORK_READY_STATUS_LOG_INTERVAL_SECONDS:-10}"

  if ! [[ "$phase_timeout" =~ ^[0-9]+$ ]] || [ "$phase_timeout" -le 0 ]; then
    phase_timeout=180
  fi
  if ! [[ "$per_try" =~ ^[0-9]+$ ]] || [ "$per_try" -le 0 ]; then
    per_try=5
  fi
  if ! [[ "$log_interval" =~ ^[0-9]+$ ]] || [ "$log_interval" -le 0 ]; then
    log_interval=10
  fi

  local deadline=$((SECONDS + phase_timeout))
  local last_log=$SECONDS

  while [ $SECONDS -lt $deadline ]; do
    local boot_ok dev_boot system_pid pm_ok activity_ok service_ok user_ready service_status
    boot_ok="$($ADB_BIN -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    dev_boot="$($ADB_BIN -s "$serial" shell getprop dev.bootcomplete 2>/dev/null | tr -d '\r')"
    system_pid="$(run_with_timeout "$per_try" "$ADB_BIN" -s "$serial" shell pidof system_server 2>/dev/null | tr -d '\r' || true)"
    pm_ok=0
    activity_ok=0
    service_ok=0
    user_ready=0
    if run_with_timeout "$per_try" "$ADB_BIN" -s "$serial" shell pm path android >/dev/null 2>&1; then
      pm_ok=1
    fi
    if run_with_timeout "$per_try" "$ADB_BIN" -s "$serial" shell cmd activity get-standby-bucket >/dev/null 2>&1; then
      activity_ok=1
    fi
    if run_with_timeout "$per_try" "$ADB_BIN" -s "$serial" shell am get-current-user >/dev/null 2>&1; then
      user_ready=1
    fi
    service_status="$(run_with_timeout "$per_try" "$ADB_BIN" -s "$serial" shell service check package 2>/dev/null | tr -d '\r' || true)"
    if [ -n "$service_status" ] && printf '%s' "$service_status" | grep -q "found"; then
      service_ok=1
    fi

    if [ "$boot_ok" = "1" ] && [ "$dev_boot" = "1" ] && [ -n "$system_pid" ] \
       && [ $pm_ok -eq 1 ] && [ $activity_ok -eq 1 ] && [ $service_ok -eq 1 ] && [ $user_ready -eq 1 ]; then
      ba_log "Android framework ready on $serial (system_server=$system_pid)"
      return 0
    fi

    if [ $((SECONDS - last_log)) -ge $log_interval ]; then
      ba_log "Waiting for Android framework on $serial (system_server=${system_pid:-down} boot_ok=${boot_ok:-?}/${dev_boot:-?} pm_ready=$pm_ok activity_ready=$activity_ok package_service_ready=$service_ok user_ready=$user_ready)"
      last_log=$SECONDS
    fi
    sleep 2
  done

  return 1
}

adb_wait_framework_ready() {
  local serial="$1"
  "$ADB_BIN" -s "$serial" wait-for-device >/dev/null 2>&1 || return 1

  local per_try="${FRAMEWORK_READY_PER_TRY_TIMEOUT_SECONDS:-5}"

  if adb_framework_ready_once "$serial" "$per_try" "${FRAMEWORK_READY_PRIMARY_TIMEOUT_SECONDS:-180}"; then
    return 0
  fi

  ba_log "Framework not ready on $serial; restarting system services"
  "$ADB_BIN" -s "$serial" shell stop >/dev/null 2>&1 || true
  sleep 2
  "$ADB_BIN" -s "$serial" shell start >/dev/null 2>&1 || true

  if adb_framework_ready_once "$serial" "$per_try" "${FRAMEWORK_READY_RESTART_TIMEOUT_SECONDS:-120}"; then
    return 0
  fi

  ba_log "Framework still unavailable on $serial; rebooting device"
  "$ADB_BIN" -s "$serial" reboot >/dev/null 2>&1 || return 1
  "$ADB_BIN" -s "$serial" wait-for-device >/dev/null 2>&1 || return 1

  if adb_framework_ready_once "$serial" "$per_try" "${FRAMEWORK_READY_REBOOT_TIMEOUT_SECONDS:-180}"; then
    return 0
  fi

  ba_log "ERROR: Android framework/package manager not available on $serial" >&2
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

  local runtime_pkg="${RUNTIME_PACKAGE:-$PACKAGE_NAME}"
  local test_pkg="${TEST_RUNTIME_PACKAGE:-${runtime_pkg}.test}"

  local instrumentation_list
  instrumentation_list="$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list instrumentation 2>/dev/null || true)"
  if [ -n "$instrumentation_list" ]; then
    printf '%s\n' "$instrumentation_list" | sed 's/^/[build-android-app] instrumentation: /'
  else
    ba_log "No instrumentation targets reported on $EMULATOR_SERIAL before installation"
  fi

  local have_test_apk=0
  if [ -n "$test_pkg" ] && "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path "$test_pkg" >/dev/null 2>&1; then
    have_test_apk=1
    "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path "$test_pkg" \
      | sed 's/^/[build-android-app] test-apk: /'
  else
    ba_log "Test APK for $test_pkg not yet installed on $EMULATOR_SERIAL"
  fi

  local package_regex package_list package_matches
  package_regex="${runtime_pkg//./\.}"
  package_list="$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list packages 2>/dev/null || true)"
  if [ -n "$package_list" ]; then
    package_matches="$(printf '%s\n' "$package_list" | grep -E "${package_regex}|${package_regex}\.test" || true)"
    if [ -n "$package_matches" ]; then
      printf '%s\n' "$package_matches" | sed 's/^/[build-android-app] package: /'
    else
      ba_log "Packages matching $runtime_pkg not yet installed on $EMULATOR_SERIAL"
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
    if grep -E "FATAL EXCEPTION|AndroidRuntime|Process: ${RUNTIME_PACKAGE}(\\.test)?|Abort message:" "$log_tmp" >"$filtered_tmp" 2>/dev/null; then
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
SYSTEM_IMAGE="system-images;android-35;google_apis;x86_64"
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
  -no-window -no-snapshot -no-snapshot-load -no-snapshot-save -wipe-data \
  -gpu swiftshader_indirect -no-audio -no-boot-anim \
  -accel off -no-accel -camera-back none -camera-front none -skip-adb-auth \
  -feature -Vulkan -netfast -memory 4096 >"$EMULATOR_LOG" 2>&1 &
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

if ! "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pidof system_server >/dev/null 2>&1; then
  ba_log "system_server not running after boot; restarting framework"
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell stop >/dev/null 2>&1 || true
  sleep 2
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell start >/dev/null 2>&1 || true
fi

if ! adb_wait_framework_ready "$EMULATOR_SERIAL"; then
  dump_emulator_diagnostics
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

"$ADB_BIN" -s "$EMULATOR_SERIAL" shell locksettings set-disabled true >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell settings put global device_provisioned 1 >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell settings put secure user_setup_complete 1 >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell svc power stayon true >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell input keyevent 82 >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell wm dismiss-keyguard >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell am get-current-user >/dev/null 2>&1 || true

if ! wait_for_api_level "$EMULATOR_SERIAL"; then
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi

"$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path android | sed 's/^/[build-android-app] pm path android: /' || true

"$ADB_BIN" start-server >/dev/null 2>&1 || true
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
  dump_manifest_merger_reports
  stop_emulator
  exit 1
fi

adb_install_file_path() {
  local serial="$1" apk="$2"
  local remote_tmp="/data/local/tmp/$(basename "$apk")"
  local attempts="${ADB_INSTALL_ATTEMPTS:-3}"
  local sleep_between="${ADB_INSTALL_RETRY_DELAY_SECONDS:-5}"
  local attempt install_status apk_size

  if ! [[ "$attempts" =~ ^[0-9]+$ ]] || [ "$attempts" -le 0 ]; then
    attempts=3
  fi
  if ! [[ "$sleep_between" =~ ^[0-9]+$ ]]; then
    sleep_between=5
  fi

  install_status=1

  for attempt in $(seq 1 "$attempts"); do
    if ! adb_wait_framework_ready "$serial"; then
      ba_log "Android framework not ready before install attempt $attempt for $(basename "$apk")"
      sleep "$sleep_between"
      continue
    fi

    "$ADB_BIN" -s "$serial" shell rm -f "$remote_tmp" >/dev/null 2>&1 || true
    if ! "$ADB_BIN" -s "$serial" push "$apk" "$remote_tmp" >/dev/null 2>&1; then
      ba_log "Failed to push $(basename "$apk") to $remote_tmp on attempt $attempt" >&2
      sleep "$sleep_between"
      continue
    fi

    if "$ADB_BIN" -s "$serial" shell pm install -r -t -g "$remote_tmp"; then
      install_status=0
    else
      apk_size=$(stat -c%s "$apk" 2>/dev/null || wc -c <"$apk")
      if [ -n "$apk_size" ] && "$ADB_BIN" -s "$serial" shell "cat '$remote_tmp' | pm install -r -t -g -S $apk_size"; then
        install_status=0
      fi
    fi

    "$ADB_BIN" -s "$serial" shell rm -f "$remote_tmp" >/dev/null 2>&1 || true

    if [ "$install_status" -eq 0 ]; then
      ba_log "Install of $(basename "$apk") succeeded on attempt $attempt"
      break
    fi

    ba_log "Install attempt $attempt for $(basename "$apk") failed; retrying after ${sleep_between}s"
    sleep "$sleep_between"
  done

  return $install_status
}

ba_log "Inspecting Gradle application identifiers"

APP_PROPERTIES_RAW=$(cd "$GRADLE_PROJECT_DIR" && ./gradlew -q :app:properties 2>/dev/null || true)
if [ -n "$APP_PROPERTIES_RAW" ]; then
  set +o pipefail
  MATCHED_PROPS=$(printf '%s\n' "$APP_PROPERTIES_RAW" | grep -E '^(applicationId|testApplicationId|namespace):' || true)
  set -o pipefail
  if [ -n "$MATCHED_PROPS" ]; then
    printf '%s\n' "$MATCHED_PROPS" | sed 's/^/[build-android-app] props: /'
  fi
fi

APP_ID="$(printf '%s\n' "$APP_PROPERTIES_RAW" | awk -F': ' '/^applicationId:/{print $2; found=1} END{if(!found) print ""}' || true)"
NS_VALUE="$(printf '%s\n' "$APP_PROPERTIES_RAW" | awk -F': ' '/^namespace:/{print $2; found=1} END{if(!found) print ""}' || true)"

if [ -z "$APP_ID" ]; then
  ba_log "Gradle did not report applicationId; relying on APK metadata"
elif [ "$APP_ID" != "$PACKAGE_NAME" ]; then
  ba_log "WARNING: Gradle applicationId '$APP_ID' differs from Codename One package '$PACKAGE_NAME'" >&2
fi
if [ -z "$NS_VALUE" ]; then
  ba_log "Gradle did not report namespace; relying on APK metadata"
elif [ "$NS_VALUE" != "$PACKAGE_NAME" ]; then
  ba_log "WARNING: Gradle namespace '$NS_VALUE' differs from Codename One package '$PACKAGE_NAME'" >&2
fi

MERGED_MANIFEST="$APP_MODULE_DIR/build/intermediates/packaged_manifests/debug/AndroidManifest.xml"
if [ -f "$MERGED_MANIFEST" ]; then
  if grep -Fq "android:name=\"$FQCN\"" "$MERGED_MANIFEST"; then
    grep -Fn "android:name=\"$FQCN\"" "$MERGED_MANIFEST" | sed 's/^/[build-android-app] merged-manifest: /'
  else
    ba_log "ERROR: merged manifest missing $FQCN"
    sed -n '1,160p' "$MERGED_MANIFEST" | sed 's/^/[build-android-app] merged: /'
    stop_emulator
    exit 1
  fi
else
  ba_log "WARN: merged manifest not found at $MERGED_MANIFEST"
fi

if [ -z "$APP_PROPERTIES_RAW" ]; then
  ba_log "Warning: unable to query :app:properties" >&2
fi

APP_APK="$(find "$GRADLE_PROJECT_DIR/app/build/outputs/apk/debug" -maxdepth 1 -name '*-debug.apk' | head -n1 || true)"
TEST_APK="$(find "$GRADLE_PROJECT_DIR/app/build/outputs/apk/androidTest/debug" -maxdepth 1 -name '*-debug-androidTest.apk' | head -n1 || true)"

if [ -z "$APP_APK" ] || [ ! -f "$APP_APK" ]; then
  ba_log "App APK not found after identifier patch assemble" >&2
  stop_emulator
  exit 1
fi
if [ -z "$TEST_APK" ] || [ ! -f "$TEST_APK" ]; then
  ba_log "androidTest APK not found after identifier patch assemble" >&2
  stop_emulator
  exit 1
fi

AAPT_BIN=""
if [ -d "$ANDROID_SDK_ROOT/build-tools" ]; then
  while IFS= read -r dir; do
    if [ -x "$dir/aapt" ]; then
      AAPT_BIN="$dir/aapt"
      break
    fi
  done < <(find "$ANDROID_SDK_ROOT/build-tools" -maxdepth 1 -mindepth 1 -type d | sort -Vr)
fi

RUNTIME_PACKAGE="$PACKAGE_NAME"
if [ -n "$AAPT_BIN" ] && [ -x "$AAPT_BIN" ]; then
  APK_PACKAGE="$($AAPT_BIN dump badging "$APP_APK" 2>/dev/null | awk -F"'" '/^package: name=/{print $2; exit}')"
  if [ -n "$APK_PACKAGE" ]; then
    ba_log "aapt reported application package: $APK_PACKAGE"
    if [ "$APK_PACKAGE" != "$PACKAGE_NAME" ]; then
      ba_log "WARNING: APK package ($APK_PACKAGE) differs from Codename One package ($PACKAGE_NAME)" >&2
    fi
    RUNTIME_PACKAGE="$APK_PACKAGE"
  else
    ba_log "WARN: Unable to extract application package using $AAPT_BIN" >&2
  fi
else
  ba_log "WARN: aapt binary not found under $ANDROID_SDK_ROOT/build-tools; skipping APK package verification" >&2
fi

TEST_RUNTIME_PACKAGE="${RUNTIME_PACKAGE}.test"
RUNTIME_STUB_FQCN="${RUNTIME_PACKAGE}.${MAIN_NAME}Stub"

ba_log "Preparing device for APK installation"
if ! adb_wait_framework_ready "$EMULATOR_SERIAL"; then
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi

if "$ADB_BIN" -s "$EMULATOR_SERIAL" shell cmd package list sessions >/dev/null 2>&1; then
  SESSION_IDS="$($ADB_BIN -s "$EMULATOR_SERIAL" shell cmd package list sessions 2>/dev/null | awk '{print $1}' | sed 's/sessionId=//g' || true)"
  if [ -n "$SESSION_IDS" ]; then
    while IFS= read -r sid; do
      [ -z "$sid" ] && continue
      "$ADB_BIN" -s "$EMULATOR_SERIAL" shell cmd package abort-session "$sid" >/dev/null 2>&1 || true
    done <<<"$SESSION_IDS"
  fi
fi

"$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm uninstall "$RUNTIME_PACKAGE" >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm uninstall "$TEST_RUNTIME_PACKAGE" >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell cmd package bg-dexopt-job >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell rm -f /data/local/tmp/*.apk >/dev/null 2>&1 || true

ba_log "Installing app APK: $APP_APK"
if ! adb_install_file_path "$EMULATOR_SERIAL" "$APP_APK"; then
  ba_log "App APK install failed; restarting framework services and retrying"
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell stop >/dev/null 2>&1 || true
  sleep 2
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell start >/dev/null 2>&1 || true
  if ! adb_install_file_path "$EMULATOR_SERIAL" "$APP_APK"; then
    dump_emulator_diagnostics
    stop_emulator
    exit 1
  fi
fi

ba_log "Installing androidTest APK: $TEST_APK"
if ! adb_install_file_path "$EMULATOR_SERIAL" "$TEST_APK"; then
  ba_log "androidTest APK install failed; restarting framework services and retrying"
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell stop >/dev/null 2>&1 || true
  sleep 2
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell start >/dev/null 2>&1 || true
  if ! adb_install_file_path "$EMULATOR_SERIAL" "$TEST_APK"; then
    dump_emulator_diagnostics
    stop_emulator
    exit 1
  fi
fi

if ! adb_wait_framework_ready "$EMULATOR_SERIAL"; then
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi

if ! "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list packages | grep -q "^package:${RUNTIME_PACKAGE//./\.}$"; then
  ba_log "ERROR: Installed package $RUNTIME_PACKAGE not visible on $EMULATOR_SERIAL"
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi

if ! "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list packages | grep -q "^package:${TEST_RUNTIME_PACKAGE//./\.}$"; then
  ba_log "ERROR: Installed test package $TEST_RUNTIME_PACKAGE not visible on $EMULATOR_SERIAL"
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi

"$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list instrumentation | sed "s/^/[build-android-app] instrumentation: /" || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list packages | grep -E "${RUNTIME_PACKAGE//./\.}|${RUNTIME_PACKAGE//./\.}\\.test" | sed "s/^/[build-android-app] package: /" || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell cmd package resolve-activity --brief "$RUNTIME_PACKAGE/$RUNTIME_STUB_FQCN" | sed "s/^/[build-android-app] resolve-stub (pre-test): /" || true

APP_PACKAGE_PATH="$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path "$RUNTIME_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [ -n "$APP_PACKAGE_PATH" ]; then
  printf '%s\n' "$APP_PACKAGE_PATH" | sed 's/^/[build-android-app] app-apk: /'
else
  ba_log "App package $RUNTIME_PACKAGE not yet reported by pm path on $EMULATOR_SERIAL"
fi

TEST_PACKAGE_PATH="$("$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm path "$TEST_RUNTIME_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [ -n "$TEST_PACKAGE_PATH" ]; then
  printf '%s\n' "$TEST_PACKAGE_PATH" | sed 's/^/[build-android-app] test-apk: /'
else
  ba_log "Test package $TEST_RUNTIME_PACKAGE not yet reported by pm path on $EMULATOR_SERIAL"
fi

log_instrumentation_state

RUNNER="$(
  "$ADB_BIN" -s "$EMULATOR_SERIAL" shell pm list instrumentation \
    | tr -d '\r' \
    | grep -F "(target=$RUNTIME_PACKAGE)" \
    | head -n1 \
    | sed -E 's/^instrumentation:([^ ]+).*/\1/'
)"
if [ -z "$RUNNER" ]; then
  ba_log "No instrumentation runner found for $RUNTIME_PACKAGE"
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
STUB_ACTIVITY_FQCN="$RUNTIME_PACKAGE/$RUNTIME_STUB_FQCN"
STUB_RESOLVE_OUTPUT="$(
    "$ADB_BIN" -s "$EMULATOR_SERIAL" shell cmd package resolve-activity --brief "$STUB_ACTIVITY_FQCN" 2>&1 || true
  )"
if [ -n "$STUB_RESOLVE_OUTPUT" ]; then
  printf '%s\n' "$STUB_RESOLVE_OUTPUT" | sed 's/^/[build-android-app] resolve-stub: /'
else
  ba_log "Unable to resolve stub activity $STUB_ACTIVITY_FQCN on device"
fi
if [[ "$STUB_RESOLVE_OUTPUT" == *"No activity found"* ]] || [ -z "$STUB_RESOLVE_OUTPUT" ]; then
  ba_log "Stub activity $STUB_ACTIVITY_FQCN is not resolvable on the device"
  dump_emulator_diagnostics
  stop_emulator
  exit 1
fi

"$ADB_BIN" -s "$EMULATOR_SERIAL" shell am force-stop "$RUNTIME_PACKAGE" >/dev/null 2>&1 || true
"$ADB_BIN" -s "$EMULATOR_SERIAL" shell am force-stop "$TEST_RUNTIME_PACKAGE" >/dev/null 2>&1 || true

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
  if ! "$ADB_BIN" -s "$EMULATOR_SERIAL" shell run-as "$RUNTIME_PACKAGE" ls "$src" >/dev/null 2>&1; then
    return 1
  fi
  if "$ADB_BIN" -s "$EMULATOR_SERIAL" exec-out run-as "$RUNTIME_PACKAGE" cat "$src" >"$dest"; then
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
