#!/usr/bin/env bash
# Build a sample "Hello Codename One" Android application using the locally-built Codename One Android port
set -euo pipefail

log() {
  echo "[build-android-app] $1"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR%/}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"

ensure_workspace() {
  local attempt
  for attempt in 1 2; do
    if [ -f "$ENV_DIR/env.sh" ]; then
      # shellcheck disable=SC1090
      source "$ENV_DIR/env.sh"
    fi

    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ] && \
       [ -n "${JAVA_HOME_17:-}" ] && [ -x "$JAVA_HOME_17/bin/java" ] && \
       [ -n "${MAVEN_HOME:-}" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
      return 0
    fi

    if [ "$attempt" -eq 1 ]; then
      log "Workspace tools not provisioned; running setup-workspace.sh"
      ./scripts/setup-workspace.sh -q -DskipTests
    fi
  done

  if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    log "JAVA_HOME is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  elif [ -z "${JAVA_HOME_17:-}" ] || [ ! -x "$JAVA_HOME_17/bin/java" ]; then
    log "JAVA_HOME_17 is not set correctly. Please run scripts/setup-workspace.sh first." >&2
  else
    log "Maven is not available. Please run scripts/setup-workspace.sh first." >&2
  fi
  exit 1
}

ensure_workspace

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
  log "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME to a valid installation." >&2
  exit 1
fi
export ANDROID_SDK_ROOT
export ANDROID_HOME="$ANDROID_SDK_ROOT"
log "Using Android SDK at $ANDROID_SDK_ROOT"

CN1_VERSION=$(awk -F'[<>]' '/<version>/{print $3; exit}' maven/pom.xml)
log "Detected Codename One version $CN1_VERSION"

WORK_DIR="$TMPDIR/cn1-hello-android"
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"

GROUP_ID="com.codenameone.examples"
ARTIFACT_ID="hello-codenameone"
MAIN_NAME="HelloCodenameOne"

log "Generating Codename One application skeleton"
"$MAVEN_HOME/bin/mvn" -q archetype:generate \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion="$CN1_VERSION" \
  -DgroupId="$GROUP_ID" \
  -DartifactId="$ARTIFACT_ID" \
  -Dversion=1.0-SNAPSHOT \
  -DmainName="$MAIN_NAME" \
  -DinteractiveMode=false \
  -Dpackage="$GROUP_ID.$MAIN_NAME" \
  -DoutputDirectory="$WORK_DIR"

APP_DIR="$WORK_DIR/$ARTIFACT_ID"
if [ ! -d "$APP_DIR" ]; then
  log "Failed to create Codename One application project" >&2
  exit 1
fi

if [ -f "$APP_DIR/build.sh" ]; then
  chmod +x "$APP_DIR/build.sh"
fi

MAIN_FILE=$(find "$APP_DIR" -path "*/common/src/main/java/*Application.java" | head -n 1 || true)
if [ -z "$MAIN_FILE" ]; then
  log "Could not locate the generated application source file" >&2
  exit 1
fi

PACKAGE_NAME=$(sed -n 's/^package \(.*\);/\1/p' "$MAIN_FILE" | head -n 1)
if [ -z "$PACKAGE_NAME" ]; then
  log "Unable to determine package name from $MAIN_FILE" >&2
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

log "Building Android gradle project using Codename One port"
"$MAVEN_HOME/bin/mvn" -q -f "$APP_DIR/pom.xml" package -DskipTests -Dcodename1.platform=android -Dcodename1.buildTarget=android-source -Dopen=false "$@"

GRADLE_PROJECT_DIR=$(find "$APP_DIR/target" -maxdepth 1 -type d -name "*-android-source" | head -n 1 || true)
if [ -z "$GRADLE_PROJECT_DIR" ]; then
  log "Failed to locate generated Android project" >&2
  exit 1
fi

log "Invoking Gradle build in $GRADLE_PROJECT_DIR"
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
  log "Gradle build completed but no APK was found" >&2
  exit 1
fi

log "Successfully built Android APK at $APK_PATH"
