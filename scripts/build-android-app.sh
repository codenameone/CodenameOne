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
  # shellcheck disable=SC1090
  source "$ENV_FILE"
else
  ba_log "Workspace tools not found. Run scripts/setup-workspace.sh before this script." >&2
  exit 1
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  ba_log "JAVA_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${JAVA_HOME_17:-}" ] || [ ! -x "$JAVA_HOME_17/bin/java" ]; then
  ba_log "JAVA_HOME_17 is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi
if [ -z "${MAVEN_HOME:-}" ] || [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  ba_log "Maven is not available. Please run scripts/setup-workspace.sh first." >&2
  exit 1
fi

ba_log "Using JAVA_HOME at $JAVA_HOME"
ba_log "Using JAVA_HOME_17 at $JAVA_HOME_17"
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

LOCAL_MAVEN_REPO="${LOCAL_MAVEN_REPO:-$HOME/.m2/repository}"
ba_log "Using local Maven repository at $LOCAL_MAVEN_REPO"
MAVEN_CMD=("$MAVEN_HOME/bin/mvn" -Dmaven.repo.local="$LOCAL_MAVEN_REPO")

ba_log "Generating Codename One application skeleton"
xvfb-run -a "${MAVEN_CMD[@]}" -q --offline archetype:generate \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion="$CN1_VERSION" \
  -DarchetypeCatalog=local \
  -DgroupId="$GROUP_ID" \
  -DartifactId="$ARTIFACT_ID" \
  -Dversion=1.0-SNAPSHOT \
  -DmainName="$MAIN_NAME" \
  -DinteractiveMode=false \
  -Dpackage="$GROUP_ID.$MAIN_NAME" \
  -DoutputDirectory="$WORK_DIR" \
  "${EXTRA_MVN_ARGS[@]}"

APP_DIR="$WORK_DIR/$ARTIFACT_ID"
if [ ! -d "$APP_DIR" ]; then
  ba_log "Failed to create Codename One application project" >&2
  exit 1
fi

if [ -f "$APP_DIR/build.sh" ]; then
  chmod +x "$APP_DIR/build.sh"
fi

MAIN_FILE=$(find "$APP_DIR" -path "*/common/src/main/java/*Application.java" | head -n 1 || true)
if [ -z "$MAIN_FILE" ]; then
  ba_log "Could not locate the generated application source file" >&2
  exit 1
fi

PACKAGE_NAME=$(sed -n 's/^package \(.*\);/\1/p' "$MAIN_FILE" | head -n 1)
if [ -z "$PACKAGE_NAME" ]; then
  ba_log "Unable to determine package name from $MAIN_FILE" >&2
  exit 1
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

SETTINGS_FILE="$APP_DIR/common/codenameone_settings.properties"
ba_log "Ensuring Codename One CSS compilation is enabled"
if [ -f "$SETTINGS_FILE" ]; then
  if grep -q '^codename1.cssTheme=' "$SETTINGS_FILE"; then
    python3 - "$SETTINGS_FILE" <<'PY'
import pathlib
import re
import sys

path = pathlib.Path(sys.argv[1])
text = path.read_text()
replacement = 'codename1.cssTheme=true'
if re.search(r'^codename1\.cssTheme=', text, flags=re.MULTILINE):
    text = re.sub(r'^codename1\.cssTheme=.*$', replacement, text, flags=re.MULTILINE)
else:
    text = text + ('\n' if not text.endswith('\n') else '') + replacement + '\n'
path.write_text(text if text.endswith('\n') else text + '\n')
PY
  else
    printf '\ncodename1.cssTheme=true\n' >> "$SETTINGS_FILE"
  fi
else
  printf 'codename1.cssTheme=true\n' > "$SETTINGS_FILE"
fi

CSS_DIR="$APP_DIR/common/src/main/css"
mkdir -p "$CSS_DIR"
THEME_CSS="$CSS_DIR/theme.css"
if [ ! -f "$THEME_CSS" ]; then
  cat > "$THEME_CSS" <<'CSSEOF'
/* Minimal CSS to ensure the theme compiles during automated builds */
Form {
    background-color: #ffffff;
}

Label {
    font-size: 2mm;
}
CSSEOF
fi

ba_log "Compiling Codename One CSS theme"
xvfb-run -a "${MAVEN_CMD[@]}" -q --offline -f "$APP_DIR/pom.xml" codenameone:css "${EXTRA_MVN_ARGS[@]}"

ba_log "Building Android gradle project using Codename One port"
xvfb-run -a "${MAVEN_CMD[@]}" -q --offline -f "$APP_DIR/pom.xml" package -DskipTests -Dcodename1.platform=android -Dcodename1.buildTarget=android-source -Dopen=false "${EXTRA_MVN_ARGS[@]}"

GRADLE_PROJECT_DIR=$(find "$APP_DIR/target" -maxdepth 1 -type d -name "*-android-source" | head -n 1 || true)
if [ -z "$GRADLE_PROJECT_DIR" ]; then
  ba_log "Failed to locate generated Android project" >&2
  exit 1
fi

ba_log "Invoking Gradle build in $GRADLE_PROJECT_DIR"
chmod +x "$GRADLE_PROJECT_DIR/gradlew"
ORIGINAL_JAVA_HOME="$JAVA_HOME"
export JAVA_HOME="$JAVA_HOME_17"
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
