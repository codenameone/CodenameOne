#!/usr/bin/env bash
# Build a sample "Hello Codename One" Android application using the locally-built Codename One Android port
set -euo pipefail

ba_log() {
  echo "[build-android-app] $1"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"
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

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  ba_log "JAVA_HOME validation failed. Current value: ${JAVA_HOME:-<unset>}" >&2
  if [ -n "${JAVA_HOME:-}" ]; then
    ba_log "Contents of JAVA_HOME directory"
    if [ -d "$JAVA_HOME" ]; then
      ls -l "$JAVA_HOME" | while IFS= read -r line; do ba_log "$line"; done
    else
      ba_log "JAVA_HOME directory does not exist"
    fi
  fi
  ba_log "JAVA_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "$JAVA17_HOME/bin/java" ]; then
  ba_log "JAVA17_HOME validation failed. Current value: ${JAVA17_HOME:-<unset>}" >&2
  if [ -n "${JAVA17_HOME:-}" ]; then
    ba_log "Contents of JAVA17_HOME directory"
    if [ -d "$JAVA17_HOME" ]; then
      ls -l "$JAVA17_HOME" | while IFS= read -r line; do ba_log "$line"; done
    else
      ba_log "JAVA17_HOME directory does not exist"
    fi
  fi
  ba_log "JAVA17_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${MAVEN_HOME:-}" ] || [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  ba_log "MAVEN_HOME validation failed. Current value: ${MAVEN_HOME:-<unset>}" >&2
  if [ -n "${MAVEN_HOME:-}" ]; then
    ba_log "Contents of MAVEN_HOME directory"
    if [ -d "$MAVEN_HOME" ]; then
      ls -l "$MAVEN_HOME" | while IFS= read -r line; do ba_log "$line"; done
    else
      ba_log "MAVEN_HOME directory does not exist"
    fi
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
  if [ -d "/usr/local/lib/android/sdk" ]; then
    ANDROID_SDK_ROOT="/usr/local/lib/android/sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then
    ANDROID_SDK_ROOT="$HOME/Android/Sdk"
  fi
fi
if [ -z "$ANDROID_SDK_ROOT" ] || [ ! -d "$ANDROID_SDK_ROOT" ]; then
  ba_log "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME to a valid installation." >&2
  exit 1
fi
export ANDROID_SDK_ROOT
export ANDROID_HOME="$ANDROID_SDK_ROOT"
ba_log "Using Android SDK at $ANDROID_SDK_ROOT"

CN1_VERSION=$(awk -F'[<>]' '/<version>/{print $3; exit}' maven/pom.xml)
ba_log "Detected Codename One version $CN1_VERSION"

WORK_DIR="$TMPDIR/cn1-hello-android"
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"

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
  "$MAVEN_HOME/bin/mvn"
  -B
  -ntp
  -Dmaven.repo.local="$LOCAL_MAVEN_REPO"
  -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
)

ba_log "Generating Codename One application skeleton via codenameone-maven-plugin"
(
  cd "$WORK_DIR"
  xvfb-run -a "${MAVEN_CMD[@]}" -q \
    com.codenameone:codenameone-maven-plugin:"$CN1_VERSION":generate-app-project \
    -DgroupId="$GROUP_ID" \
    -DartifactId="$ARTIFACT_ID" \
    -Dversion=1.0-SNAPSHOT \
    -DsourceProject="$SOURCE_PROJECT" \
    -Dcn1Version="$CN1_VERSION" \
    "${EXTRA_MVN_ARGS[@]}"
)

APP_DIR="$WORK_DIR/$ARTIFACT_ID"
if [ ! -d "$APP_DIR" ]; then
  ba_log "Failed to create Codename One application project" >&2
  exit 1
fi

if [ -f "$APP_DIR/build.sh" ]; then
  chmod +x "$APP_DIR/build.sh"
fi

SETTINGS_FILE="$APP_DIR/common/codenameone_settings.properties"
if [ ! -f "$SETTINGS_FILE" ]; then
  ba_log "codenameone_settings.properties not found at $SETTINGS_FILE" >&2
  exit 1
fi

CN1_SETTINGS_TMP=$(mktemp)
trap 'rm -f "$CN1_SETTINGS_TMP"' EXIT

SETTINGS_FILE="$SETTINGS_FILE" python3 <<'PY' >"$CN1_SETTINGS_TMP"
import os
import pathlib
import shlex

path = pathlib.Path(os.environ['SETTINGS_FILE'])
package = ""
main_name = ""
if path.exists():
    for line in path.read_text().splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith('#'):
            continue
        if stripped.startswith('codename1.packageName=') and not package:
            package = stripped.split('=', 1)[1].strip()
        elif stripped.startswith('codename1.mainName=') and not main_name:
            main_name = stripped.split('=', 1)[1].strip()

print(f"CN1_PACKAGE_NAME={shlex.quote(package)}")
print(f"CN1_CURRENT_MAIN_NAME={shlex.quote(main_name)}")
PY

# shellcheck disable=SC1090
source "$CN1_SETTINGS_TMP"
rm -f "$CN1_SETTINGS_TMP"
trap - EXIT

PACKAGE_NAME="${CN1_PACKAGE_NAME:-}"
CURRENT_MAIN_NAME="${CN1_CURRENT_MAIN_NAME:-}"

if [ -z "$PACKAGE_NAME" ]; then
  PACKAGE_NAME="$GROUP_ID"
  ba_log "Package name not found in settings. Falling back to groupId $PACKAGE_NAME"
fi

if [ -z "$CURRENT_MAIN_NAME" ]; then
  CURRENT_MAIN_NAME="$MAIN_NAME"
  ba_log "Main class name not found in settings. Falling back to target $CURRENT_MAIN_NAME"
fi

PACKAGE_PATH="${PACKAGE_NAME//.//}"
if [ -n "$PACKAGE_PATH" ]; then
  EXPECTED_MAIN_PATH="$APP_DIR/common/src/main/java/$PACKAGE_PATH/$CURRENT_MAIN_NAME.java"
else
  EXPECTED_MAIN_PATH="$APP_DIR/common/src/main/java/$CURRENT_MAIN_NAME.java"
fi

if [ -f "$EXPECTED_MAIN_PATH" ]; then
  MAIN_FILE="$EXPECTED_MAIN_PATH"
else
  ba_log "Expected main source $EXPECTED_MAIN_PATH not found. Scanning project tree for $CURRENT_MAIN_NAME.java"
  MAIN_FILE=$(find "$APP_DIR/common/src/main/java" -name "$CURRENT_MAIN_NAME.java" | head -n 1 || true)
fi

if [ -z "$MAIN_FILE" ]; then
  ba_log "Could not locate the generated application source file" >&2
  exit 1
fi

PACKAGE_NAME=$(sed -n 's/^package \(.*\);/\1/p' "$MAIN_FILE" | head -n 1)
if [ -z "$PACKAGE_NAME" ]; then
  ba_log "Unable to determine package name from $MAIN_FILE" >&2
  exit 1
fi

TARGET_MAIN_FILE_DIR="$(dirname "$MAIN_FILE")"
TARGET_MAIN_FILE="$TARGET_MAIN_FILE_DIR/${MAIN_NAME}.java"
if [ "$MAIN_FILE" != "$TARGET_MAIN_FILE" ]; then
  mv "$MAIN_FILE" "$TARGET_MAIN_FILE"
  MAIN_FILE="$TARGET_MAIN_FILE"
fi

cat > "$MAIN_FILE" <<HELLOEOF
package $PACKAGE_NAME;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

public class ${MAIN_NAME} {
    private Form current;

    public void init(Object context) {
        // No special initialization required for this sample
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form helloForm = new Form("Hello Codename One", new BorderLayout());
        helloForm.add(BorderLayout.CENTER, new Label("Hello Codename One"));
        helloForm.show();
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
        // Nothing to clean up for this sample
    }
}
HELLOEOF

ba_log "Setting codename1.mainName to $MAIN_NAME"
if grep -q '^codename1.mainName=' "$SETTINGS_FILE"; then
  python3 - "$SETTINGS_FILE" "$MAIN_NAME" <<'PY'
import pathlib
import re
import sys

path = pathlib.Path(sys.argv[1])
main_name = sys.argv[2]
text = path.read_text()
replacement = f'codename1.mainName={main_name}'
if re.search(r'^codename1\.mainName=', text, flags=re.MULTILINE):
    text = re.sub(r'^codename1\.mainName=.*$', replacement, text, flags=re.MULTILINE)
else:
    text = text + ('\n' if not text.endswith('\n') else '') + replacement + '\n'
path.write_text(text if text.endswith('\n') else text + '\n')
PY
else
  printf '\ncodename1.mainName=%s\n' "$MAIN_NAME" >> "$SETTINGS_FILE"
fi

ba_log "Normalizing Codename One Maven coordinates to $CN1_VERSION"
while IFS= read -r -d '' pom_file; do
  ba_log "Updating Codename One artifacts in $pom_file"
  python3 - "$pom_file" "$CN1_VERSION" <<'PY'
import sys
import xml.etree.ElementTree as ET

pom_path, cn1_version = sys.argv[1:3]
tree = ET.parse(pom_path)
root = tree.getroot()

def local_name(tag):
    return tag.split('}', 1)[-1] if '}' in tag else tag

ns_uri = ''
if root.tag.startswith('{') and '}' in root.tag:
    ns_uri = root.tag[1:root.tag.index('}')]

def qname(name):
    return f'{{{ns_uri}}}{name}' if ns_uri else name

def find_child(parent, name):
    for child in list(parent):
        if local_name(child.tag) == name:
            return child
    return None

changed = False

properties = root.find(qname('properties'))
if properties is None:
    properties = ET.Element(qname('properties'))
    # Insert properties after version if possible, otherwise append to root
    inserted = False
    for idx, child in enumerate(list(root)):
        lname = local_name(child.tag)
        if lname in {'modelVersion', 'groupId', 'artifactId', 'version'}:
            continue
        root.insert(idx, properties)
        inserted = True
        break
    if not inserted:
        root.append(properties)
    changed = True

def ensure_property(name, value):
    global changed
    prop = find_child(properties, name)
    if prop is None:
        prop = ET.SubElement(properties, qname(name))
        changed = True
    if (prop.text or '').strip() != value:
        prop.text = value
        changed = True

ensure_property('codenameone.version', cn1_version)

def ensure_version(element):
    global changed
    version = element.find(qname('version'))
    if version is None:
        version = ET.SubElement(element, qname('version'))
        version.text = cn1_version
        changed = True
        return
    text = (version.text or '').strip()
    if text.startswith('${'):
        return
    if text != cn1_version:
        version.text = cn1_version
        changed = True

def update_artifacts(parent):
    if parent is None:
        return
    plugins = parent.find(qname('plugins'))
    if plugins is None:
        return
    for plugin in plugins.findall(qname('plugin')):
        group = plugin.find(qname('groupId'))
        if group is not None and (group.text or '').strip() == 'com.codenameone':
            ensure_version(plugin)

build = root.find(qname('build'))
if build is not None:
    update_artifacts(build)
    plugin_mgmt = build.find(qname('pluginManagement'))
    if plugin_mgmt is not None:
        update_artifacts(plugin_mgmt)

for dependency in root.findall('.//' + qname('dependency')):
    group = dependency.find(qname('groupId'))
    if group is not None and (group.text or '').strip() == 'com.codenameone':
        ensure_version(dependency)

if ns_uri:
    ET.register_namespace('', ns_uri)

if changed:
    try:
        ET.indent(tree, space='  ')
    except AttributeError:
        pass
    tree.write(pom_path, encoding='UTF-8', xml_declaration=True)
PY
done < <(find "$APP_DIR" -name pom.xml -print0)

ba_log "Disabling Codename One CSS compilation to avoid headless failures"
if grep -q '^codename1.cssTheme=' "$SETTINGS_FILE"; then
  python3 - "$SETTINGS_FILE" <<'PY'
import pathlib
import re
import sys

path = pathlib.Path(sys.argv[1])
text = path.read_text()
replacement = 'codename1.cssTheme=false'
if re.search(r'^codename1\.cssTheme=', text, flags=re.MULTILINE):
    text = re.sub(r'^codename1\.cssTheme=.*$', replacement, text, flags=re.MULTILINE)
else:
    text = text + ('\n' if not text.endswith('\n') else '') + replacement + '\n'
path.write_text(text if text.endswith('\n') else text + '\n')
PY
else
  printf '\ncodename1.cssTheme=false\n' >> "$SETTINGS_FILE"
fi

ba_log "Building Android gradle project using Codename One port"
xvfb-run -a "${MAVEN_CMD[@]}" -q -f "$APP_DIR/pom.xml" package -DskipTests -Dcodename1.platform=android -Dcodename1.buildTarget=android-source -Dopen=false "${EXTRA_MVN_ARGS[@]}"

GRADLE_PROJECT_DIR=$(find "$APP_DIR/target" -maxdepth 2 -type d -name "*-android-source" | head -n 1 || true)

if [ -z "$GRADLE_PROJECT_DIR" ]; then
  ba_log "Failed to locate generated Android project" >&2
  ba_log "Contents of $APP_DIR/target:" >&2
  ls -R "$APP_DIR/target" >&2 || ba_log "Unable to list $APP_DIR/target" >&2
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
if [ -z "$APK_PATH" ]; then
  ba_log "Gradle build completed but no APK was found" >&2
  exit 1
fi

ba_log "Successfully built Android APK at $APK_PATH"
