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
echo "codename1.arg.android.useAndroidX=true" >> "$SETTINGS_FILE"
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

ba_log "Configuring instrumentation test sources in $GRADLE_PROJECT_DIR"

# Ensure AndroidX flags in gradle.properties
# --- BEGIN: robust Gradle patch for AndroidX tests ---
GRADLE_PROPS="$GRADLE_PROJECT_DIR/gradle.properties"
grep -q '^android.useAndroidX=' "$GRADLE_PROPS" 2>/dev/null || echo 'android.useAndroidX=true' >> "$GRADLE_PROPS"
grep -q '^android.enableJetifier=' "$GRADLE_PROPS" 2>/dev/null || echo 'android.enableJetifier=true' >> "$GRADLE_PROPS"

APP_BUILD_GRADLE="$GRADLE_PROJECT_DIR/app/build.gradle"
ROOT_BUILD_GRADLE="$GRADLE_PROJECT_DIR/build.gradle"

# Ensure repos in both root and app
for F in "$ROOT_BUILD_GRADLE" "$APP_BUILD_GRADLE"; do
  if [ -f "$F" ]; then
    if ! grep -qE '^\s*repositories\s*{' "$F"; then
      cat >> "$F" <<'EOS'

repositories {
    google()
    mavenCentral()
}
EOS
    else
      grep -q 'google()' "$F" || sed -E -i '0,/repositories[[:space:]]*\{/s//repositories {\n    google()\n    mavenCentral()/' "$F"
      grep -q 'mavenCentral()' "$F" || sed -E -i '0,/repositories[[:space:]]*\{/s//repositories {\n    google()\n    mavenCentral()/' "$F"
    fi
  fi
done

# Edit app/build.gradle
python3 - "$APP_BUILD_GRADLE" <<'PY'
import sys, re, pathlib
p = pathlib.Path(sys.argv[1]); txt = p.read_text(); orig = txt; changed = False

def strip_block(name, s):
    return re.sub(rf'(?ms)^\s*{name}\s*\{{.*?\}}\s*', '', s)

module_view = strip_block('buildscript', strip_block('pluginManagement', txt))

# 1) android { compileSdkVersion/targetSdkVersion }
def ensure_sdk(body):
    # If android { ... } exists, update/insert inside defaultConfig and the android block
    if re.search(r'(?m)^\s*android\s*\{', body):
        # compileSdkVersion
        if re.search(r'(?m)^\s*compileSdkVersion\s+\d+', body) is None:
            body = re.sub(r'(?m)(^\s*android\s*\{)', r'\1\n    compileSdkVersion 33', body, count=1)
        else:
            body = re.sub(r'(?m)^\s*compileSdkVersion\s+\d+', '    compileSdkVersion 33', body)
        # targetSdkVersion
        if re.search(r'(?ms)^\s*defaultConfig\s*\{.*?^\s*\}', body):
            dc = re.search(r'(?ms)^\s*defaultConfig\s*\{.*?^\s*\}', body)
            block = dc.group(0)
            if re.search(r'(?m)^\s*targetSdkVersion\s+\d+', block):
                block2 = re.sub(r'(?m)^\s*targetSdkVersion\s+\d+', '        targetSdkVersion 33', block)
            else:
                block2 = re.sub(r'(\{\s*)', r'\1\n        targetSdkVersion 33', block, count=1)
            body = body[:dc.start()] + block2 + body[dc.end():]
        else:
            body = re.sub(r'(?m)(^\s*android\s*\{)', r'\1\n    defaultConfig {\n        targetSdkVersion 33\n    }', body, count=1)
    else:
        # No android block at all: add minimal
        body += '\n\nandroid {\n    compileSdkVersion 33\n    defaultConfig { targetSdkVersion 33 }\n}\n'
    return body

txt2 = ensure_sdk(txt)
if txt2 != txt: txt = txt2; module_view = strip_block('buildscript', strip_block('pluginManagement', txt)); changed = True

# 2) testInstrumentationRunner -> AndroidX
if "androidx.test.runner.AndroidJUnitRunner" not in module_view:
    t2, n = re.subn(r'(?m)^\s*testInstrumentationRunner\s*".*?"\s*$', '        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"', txt)
    if n == 0:
        t2, n = re.subn(r'(?m)(^\s*defaultConfig\s*\{)', r'\1\n        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"', txt, count=1)
    if n == 0:
        t2, n = re.subn(r'(?ms)(^\s*android\s*\{)', r'\1\n    defaultConfig {\n        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"\n    }', txt, count=1)
    if n: txt = t2; module_view = strip_block('buildscript', strip_block('pluginManagement', txt)); changed = True

# 3) remove legacy useLibrary lines
t2, n = re.subn(r'(?m)^\s*useLibrary\s+\'android\.test\.(base|mock|runner)\'\s*$', '', txt)
if n: txt = t2; module_view = strip_block('buildscript', strip_block('pluginManagement', txt)); changed = True

# 4) deps: choose androidTestImplementation vs androidTestCompile
uses_modern = re.search(r'(?m)^\s*(implementation|api|testImplementation|androidTestImplementation)\b', module_view) is not None
conf = "androidTestImplementation" if uses_modern else "androidTestCompile"
need = [
    ("androidx.test.ext:junit:1.1.5", conf),   # AndroidJUnit4
    ("androidx.test:runner:1.5.2", conf),
    ("androidx.test:core:1.5.0", conf),
    ("androidx.test.services:storage:1.4.2", conf),
]
to_add = [(c, k) for (c, k) in need if c not in module_view]

if to_add:
    block = "\n\ndependencies {\n" + "".join([f"    {k} \"{c}\"\n" for c, k in to_add]) + "}\n"
    txt = txt.rstrip() + block
    changed = True

if changed and txt != orig:
    if not txt.endswith("\n"): txt += "\n"
    p.write_text(txt)
    print(f"Patched app/build.gradle (SDK=33; deps via {conf})")
else:
    print("No changes needed in app/build.gradle")
PY
# --- END: robust Gradle patch ---

echo "----- app/build.gradle tail -----"
tail -n 80 "$APP_BUILD_GRADLE" | sed 's/^/| /'
echo "---------------------------------"

TEST_SRC_DIR="$GRADLE_PROJECT_DIR/app/src/androidTest/java/${PACKAGE_PATH}"
mkdir -p "$TEST_SRC_DIR"
TEST_CLASS="$TEST_SRC_DIR/HelloCodenameOneInstrumentedTest.java"
cat >"$TEST_CLASS" <<'EOF'
package @PACKAGE@;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;

@RunWith(AndroidJUnit4.class)
public class HelloCodenameOneInstrumentedTest {

    private static void println(String s) { System.out.println(s); }

    @Test
    public void testUseAppContext_andEmitScreenshot() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        String pkg = "@PACKAGE@";
        Assert.assertEquals("Package mismatch", pkg, ctx.getPackageName());

        // Resolve real launcher intent (don’t hard-code activity)
        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) {
            // Fallback MAIN/LAUNCHER inside this package
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(Intent.CATEGORY_LAUNCHER);
            q.setPackage(pkg);
            launch = q;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        println("CN1SS:INFO: about to launch Activity");
        byte[] pngBytes = null;

        try (ActivityScenario<Activity> scenario = ActivityScenario.launch(launch)) {
            // give the activity a tiny moment to layout
            Thread.sleep(750);

            println("CN1SS:INFO: activity launched");

            final byte[][] holder = new byte[1][];
            scenario.onActivity(activity -> {
                try {
                    View root = activity.getWindow().getDecorView().getRootView();
                    int w = root.getWidth();
                    int h = root.getHeight();
                    if (w <= 0 || h <= 0) {
                        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                        w = Math.max(1, dm.widthPixels);
                        h = Math.max(1, dm.heightPixels);
                        int sw = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY);
                        int sh = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY);
                        root.measure(sw, sh);
                        root.layout(0, 0, w, h);
                        println("CN1SS:INFO: forced layout to " + w + "x" + h);
                    } else {
                        println("CN1SS:INFO: natural layout " + w + "x" + h);
                    }

                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmp);
                    root.draw(c);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(1024, w * h / 2));
                    boolean ok = bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    if (!ok) throw new RuntimeException("Bitmap.compress returned false");
                    holder[0] = baos.toByteArray();
                    println("CN1SS:INFO: png_bytes=" + holder[0].length);
                } catch (Throwable t) {
                    println("CN1SS:ERR: onActivity " + t);
                    t.printStackTrace(System.out);
                }
            });

            pngBytes = holder[0];
        } catch (Throwable t) {
            println("CN1SS:ERR: launch " + t);
            t.printStackTrace(System.out);
        }

        if (pngBytes == null || pngBytes.length == 0) {
            println("CN1SS:END");  // terminator for the runner parser
            Assert.fail("Screenshot capture produced 0 bytes");
            return;
        }

        // Chunk & emit (safe for Gradle/logcat capture)
        String b64 = Base64.encodeToString(pngBytes, Base64.NO_WRAP);
        final int CHUNK = 2000;
        int count = 0;
        for (int pos = 0; pos < b64.length(); pos += CHUNK) {
            int end = Math.min(pos + CHUNK, b64.length());
            System.out.println("CN1SS:" + String.format("%06d", pos) + ":" + b64.substring(pos, end));
            count++;
        }
        println("CN1SS:INFO: chunks=" + count + " total_b64_len=" + b64.length());
        System.out.println("CN1SS:END");
        System.out.flush();
    }
}
EOF
sed -i "s|@PACKAGE@|$PACKAGE_NAME|g" "$TEST_CLASS"
ba_log "Created instrumentation test at $TEST_CLASS"

DEFAULT_ANDROID_TEST="$GRADLE_PROJECT_DIR/app/src/androidTest/java/com/example/myapplication2/ExampleInstrumentedTest.java"
if [ -f "$DEFAULT_ANDROID_TEST" ]; then
  rm -f "$DEFAULT_ANDROID_TEST"
  ba_log "Removed default instrumentation stub at $DEFAULT_ANDROID_TEST"
  DEFAULT_ANDROID_TEST_DIR="$(dirname "$DEFAULT_ANDROID_TEST")"
  DEFAULT_ANDROID_TEST_PARENT="$(dirname "$DEFAULT_ANDROID_TEST_DIR")"
  rmdir "$DEFAULT_ANDROID_TEST_DIR" 2>/dev/null || true
  rmdir "$DEFAULT_ANDROID_TEST_PARENT" 2>/dev/null || true
  rmdir "$(dirname "$DEFAULT_ANDROID_TEST_PARENT")" 2>/dev/null || true
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

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "gradle_project_dir=$GRADLE_PROJECT_DIR"
    echo "apk_path=$APK_PATH"
    echo "instrumentation_test_class=$PACKAGE_NAME.HelloCodenameOneInstrumentedTest"
  } >> "$GITHUB_OUTPUT"
  ba_log "Published GitHub Actions outputs for downstream steps"
fi
