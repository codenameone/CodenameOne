#!/bin/bash
# The desktop tools start the same way from both build tools. Launches
# Codename One Settings bound to an archetype app (mvn cn1:settings) and to its
# Gradle conversion (./gradlew settings), in the tool's screenshot mode, which
# renders the project, writes a PNG and exits.
#
# The Gradle half is what regressed: the tools' poms carry the JavaSE port in
# test scope only, which Maven's legacy resolver picks up anyway and Gradle
# rightly does not, so the tool died with NoClassDefFoundError: JavaSEPort and
# the task still reported success. A screenshot is proof the tool really ran.
#
# Needs com.codenameone:codenameone-settings in the local repository -- build it
# with `cd scripts/settings && mvn install -Pexecutable-jar -Dcodename1.platform=javase`
# -- and a display (xvfb on Linux). Skips without the tool unless
# CN1_TOOLS_REQUIRED=1.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"

if [ ! -d "$CN1_REPO/com/codenameone/codenameone-settings/$CN1_VERSION" ]; then
  if [ "${CN1_TOOLS_REQUIRED:-}" = "1" ]; then
    fail "codenameone-settings $CN1_VERSION is not in $CN1_REPO, and CN1_TOOLS_REQUIRED=1 forbids skipping"
  fi
  echo "gradle-tools-test: skipped, codenameone-settings $CN1_VERSION is not installed"
  exit 0
fi

WORKDIR="$SCRIPTPATH/build/gradle-tools"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"
cd "$WORKDIR"
generate_gradle_app toolsapp com.acme.toolsapp

check_screenshot() {
  local png="$1" log="$2" label="$3"
  # Detached launches return before the window exists, so both run attached;
  # the tool's own log is the one to read if it died.
  [ -s "$png" ] || { cat "$log"; tail -40 "$HOME/.codenameoneSettings/settings.log" 2>/dev/null; \
    fail "$label: Settings wrote no screenshot"; }
  [ "$(wc -c < "$png")" -gt 10000 ] || fail "$label: the screenshot is too small to be the rendered tool"
  echo "   $label: Settings rendered ($(wc -c < "$png" | tr -d ' ') bytes)"
}

echo "== Maven: cn1:settings"
(cd toolsapp && JAVA_HOME="$GRADLE_JDK" mvn_local cn1:settings -Dsettings.spawn=false \
    -Dsettings.screenshot="$WORKDIR/maven.png" < /dev/null) > "$WORKDIR/maven.log" 2>&1 \
  || { tail -40 "$WORKDIR/maven.log"; fail "mvn cn1:settings"; }
check_screenshot "$WORKDIR/maven.png" "$WORKDIR/maven.log" Maven

echo "== Gradle: settings"
run_gradle "$WORKDIR/toolsapp-gradle" settings -Pspawn=false -Dsettings.screenshot="$WORKDIR/gradle.png" \
  > "$WORKDIR/gradle.log" 2>&1 || { cat "$WORKDIR/gradle.log"; fail "./gradlew settings"; }
check_screenshot "$WORKDIR/gradle.png" "$WORKDIR/gradle.log" Gradle

echo "gradle-tools-test: OK"
