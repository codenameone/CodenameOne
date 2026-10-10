#!/bin/bash
# End to end over a Gradle application, from a project that starts with no
# native code and no backend:
#
#  1. cn1:convert-to-gradle turns an archetype app into the Gradle layout, which
#     must carry none of the Maven modules or native trees.
#  2. classes + cn1Css compile the app and its theme, with the configuration
#     cache stored on the first run and reused on the second.
#  3. A NativeInterface added afterwards gets its stubs from
#     generateNativeInterfaces, which creates only the directories it writes;
#     a re-run keeps edited stubs; a missing implementation fails the build
#     before anything is sent.
#  4. Every cloud target stages its upload jar (codename1.stageOnly), holding
#     the app and that platform's native sources -- and no other platform's.
#  5. addBackend adds backend/, whose runBackend serves /healthz and whose
#     sample tests pass on the JVM and, with clang, compiled.
#  6. backendWebApp stages a browser build in backend/build/webapp, which
#     runBackend then serves at /.
#
# Needs the reactor installed (mvn install) and a JDK 17+ for Gradle; see
# inc/gradle.sh for MAVEN_REPO_LOCAL and GRADLE_JAVA_HOME.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"

WORKDIR="$SCRIPTPATH/build/gradle-app"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"
cd "$WORKDIR"

PKG=com.acme.gradleapp
PKG_PATH=com/acme/gradleapp
generate_gradle_app gradleapp "$PKG"
APP="$WORKDIR/gradleapp-gradle"

echo "== layout"
for f in settings.gradle.kts build.gradle.kts gradlew codenameone_settings.properties src/main/java src/main/css/theme.css; do
  [ -e "$APP/$f" ] || fail "converted project is missing $f"
done
for f in pom.xml common android ios javase javascript win linux backend src/android src/ios src/javase; do
  [ ! -e "$APP/$f" ] || fail "converted project should not have $f"
done
grep -q 'java.version=17' "$APP/codenameone_settings.properties" || fail "a Gradle project targets Java 17"

echo "== classes, theme, configuration cache"
run_gradle "$APP" classes cn1Css > "$WORKDIR/classes-1.log" 2>&1 || { cat "$WORKDIR/classes-1.log"; fail "classes"; }
THEME=$(find "$APP/build" -name theme.res | head -1)
[ -n "$THEME" ] && [ -s "$THEME" ] || fail "cn1Css produced no theme.res"
run_gradle "$APP" classes cn1Css > "$WORKDIR/classes-2.log" 2>&1 || { cat "$WORKDIR/classes-2.log"; fail "classes (2)"; }
grep -q "Configuration cache entry reused" "$WORKDIR/classes-2.log" \
  || { cat "$WORKDIR/classes-2.log"; fail "the second run did not reuse the configuration cache"; }

echo "== native interfaces"
cat > "$APP/src/main/java/$PKG_PATH/Greeter.java" <<EOF
package $PKG;

import com.codename1.system.NativeInterface;

public interface Greeter extends NativeInterface {
    String greet(String name);
}
EOF
run_gradle "$APP" generateNativeInterfaces > "$WORKDIR/ni.log" 2>&1 || { cat "$WORKDIR/ni.log"; fail "generateNativeInterfaces"; }
ANDROID_IMPL="$APP/src/android/java/$PKG_PATH/GreeterImpl.java"
IOS_IMPL="$APP/src/ios/objectivec/com_acme_gradleapp_GreeterImpl.m"
JAVASE_IMPL="$APP/src/javase/java/$PKG_PATH/GreeterImpl.java"
for f in "$ANDROID_IMPL" "$IOS_IMPL" "$JAVASE_IMPL" "$APP/src/javascript/javascript/com_acme_gradleapp_Greeter.js" \
    "$APP/src/win/c/$PKG_PATH/GreeterImplCodenameOne.c" "$APP/src/linux/c/$PKG_PATH/GreeterImplCodenameOne.c"; do
  [ -f "$f" ] || { find "$APP/src" -type f; fail "no stub at $f"; }
done

# An edited stub survives a re-run, which only -Pcn1.overwrite=true replaces.
echo "// edited" >> "$JAVASE_IMPL"
run_gradle "$APP" generateNativeInterfaces > "$WORKDIR/ni-2.log" 2>&1 || { cat "$WORKDIR/ni-2.log"; fail "generateNativeInterfaces (2)"; }
grep -q "// edited" "$JAVASE_IMPL" || fail "a re-run overwrote an existing implementation"

# The javase implementation joins the simulator's classpath.
run_gradle "$APP" javaseClasses > "$WORKDIR/javase.log" 2>&1 || { cat "$WORKDIR/javase.log"; fail "javaseClasses"; }
find "$APP/build" -path "*javase*" -name GreeterImpl.class | grep -q . || fail "the javase implementation was not compiled"

echo "== a missing implementation fails before the build is sent"
mv "$IOS_IMPL" "$WORKDIR/GreeterImpl.m.bak"
if run_gradle "$APP" buildIos -Pcodename1.stageOnly=true > "$WORKDIR/verify.log" 2>&1; then
  cat "$WORKDIR/verify.log"
  fail "buildIos succeeded without an iOS implementation of Greeter"
fi
grep -q "GreeterImpl" "$WORKDIR/verify.log" || { cat "$WORKDIR/verify.log"; fail "the failure does not name the missing file"; }
# cn1Build with no platform fails for that, without first writing stubs into
# src/<platform> (an absent platform used to mean "generate" to its check).
if run_gradle "$APP" cn1Build -Pcodename1.stageOnly=true > "$WORKDIR/no-platform.log" 2>&1; then
  cat "$WORKDIR/no-platform.log"
  fail "cn1Build succeeded without a platform"
fi
[ ! -e "$IOS_IMPL" ] || fail "cn1Build without a platform generated native stubs"
mv "$WORKDIR/GreeterImpl.m.bak" "$IOS_IMPL"

# The simulator's code rides only a JVM target's upload, so a broken
# src/javase must not stop a device build.
BROKEN="$APP/src/javase/java/$PKG_PATH/Broken.java"
echo "this does not compile" > "$BROKEN"
run_gradle "$APP" buildIos -Pcodename1.stageOnly=true > "$WORKDIR/broken-javase.log" 2>&1 \
  || { cat "$WORKDIR/broken-javase.log"; fail "a broken src/javase stopped buildIos"; }
rm -f "$BROKEN"

echo "== staged upload jars"
staged_jar() {
  sed -n 's/.*codename1.stageOnly is set: staged \(.*\) ([0-9]* bytes) for .*/\1/p' "$1" | tail -1
}
# task | a native source that must be in the jar | one that must not
for spec in \
    "buildAndroid|GreeterImpl\.java$|_GreeterImpl\.m$" \
    "buildIos|com_acme_gradleapp_GreeterImpl\.m$|$PKG_PATH/GreeterImpl\.java$" \
    "buildIosRelease|com_acme_gradleapp_GreeterImpl\.m$|$PKG_PATH/GreeterImpl\.java$" \
    "buildJavascript|^com_acme_gradleapp_Greeter\.js$|_GreeterImpl\.m$" \
    "buildWindowsDesktop|$PKG_PATH/GreeterImpl\.class$|_GreeterImpl\.m$" \
    "buildMacDesktop|$PKG_PATH/GreeterImpl\.class$|_GreeterImpl\.m$" \
    "buildWindowsDevice|$PKG_PATH/GreeterImplCodenameOne\.c$|_GreeterImpl\.m$" \
    "buildLinuxDevice|$PKG_PATH/GreeterImplCodenameOne\.c$|$PKG_PATH/GreeterImpl\.java$"; do
  IFS='|' read -r task has lacks <<< "$spec"
  log="$WORKDIR/stage-$task.log"
  run_gradle "$APP" "$task" -Pcodename1.stageOnly=true > "$log" 2>&1 || { cat "$log"; fail "$task staging"; }
  jar=$(staged_jar "$log")
  [ -n "$jar" ] && [ -f "$jar" ] || { cat "$log"; fail "$task staged no jar"; }
  assert_zip_has "$jar" "^$PKG_PATH/[A-Za-z]+\.class$"
  assert_zip_has "$jar" "$has"
  assert_zip_lacks "$jar" "$lacks"
  # The framework is supplied by the build server, never uploaded.
  assert_zip_lacks "$jar" "^com/codename1/ui/Form\.class$"
  echo "   $task: $(unzip -Z1 "$jar" | wc -l | tr -d ' ') entries in $(basename "$jar")"
done

echo "== a custom icon reaches the cloud build"
# build.xml hands every target icon="${codename1.icon}", resolved against the
# staged project, so an icon at another path has to be staged there.
mkdir -p "$APP/branding"
cp "$APP/icon.png" "$APP/branding/app.png" 2>/dev/null || printf 'png' > "$APP/branding/app.png"
cp "$APP/codenameone_settings.properties" "$WORKDIR/settings.bak"
printf '\ncodename1.icon=branding/app.png\n' >> "$APP/codenameone_settings.properties"
run_gradle "$APP" buildAndroid -Pcodename1.stageOnly=true > "$WORKDIR/icon-stage.log" 2>&1 \
  || { cat "$WORKDIR/icon-stage.log"; fail "staging with a custom icon"; }
find "$APP/build" -path "*antProject/branding/app.png" | grep -q . \
  || { find "$APP/build" -name app.png; fail "the custom icon was not staged for the cloud build"; }
cp "$WORKDIR/settings.bak" "$APP/codenameone_settings.properties"
rm -rf "$APP/branding"

echo "== unit tests in the simulator's runner"
run_gradle "$APP" cn1Test > "$WORKDIR/test.log" 2>&1 || { cat "$WORKDIR/test.log"; fail "cn1Test"; }

echo "== a relocated build directory takes the Codename One outputs with it"
cp "$APP/build.gradle.kts" "$WORKDIR/build.gradle.kts.bak"
printf '\nlayout.buildDirectory.set(file("out"))\n' >> "$APP/build.gradle.kts"
rm -rf "$APP/build" "$APP/out"
run_gradle "$APP" classes cn1Css > "$WORKDIR/relocated.log" 2>&1 || { cat "$WORKDIR/relocated.log"; fail "building into out/"; }
[ -d "$APP/out/generated/sources/cn1-svg" ] || { find "$APP/out" -maxdepth 3; fail "transcodeSvg did not follow the build directory"; }
[ -d "$APP/out/classes/java/main" ] || fail "nothing was compiled into out/"
[ ! -e "$APP/build" ] || { find "$APP/build" -maxdepth 3; fail "Codename One outputs were left in build/"; }
cp "$WORKDIR/build.gradle.kts.bak" "$APP/build.gradle.kts"
rm -rf "$APP/out"

echo "== backend added to an existing app"
run_gradle "$APP" addBackend > "$WORKDIR/add-backend.log" 2>&1 || { cat "$WORKDIR/add-backend.log"; fail "addBackend"; }
[ -f "$APP/backend/application.properties" ] || fail "addBackend wrote no backend/application.properties"
[ ! -e "$APP/backend/pom.xml" ] || fail "a Gradle backend has no pom"
check_backend_healthz "$APP" ":backend:runBackend" "$WORKDIR/backend-run.log"

echo "== the backend hosts the app's browser build"
# With no bundle given the task builds one: it depends on the root project's
# browser build, the one made without a proxy servlet. Checked on the task graph;
# the translation itself is the JavaScript port's to test.
run_gradle "$APP" :backend:backendWebApp --dry-run > "$WORKDIR/webapp-graph.log" 2>&1 \
  || { cat "$WORKDIR/webapp-graph.log"; fail ":backend:backendWebApp --dry-run"; }
grep -q "^:buildJavascriptWebApp SKIPPED" "$WORKDIR/webapp-graph.log" \
  || { cat "$WORKDIR/webapp-graph.log"; fail "backendWebApp does not build the app for the browser"; }
# A bundle that is already built is staged as it is, and nothing is built for it.
python3 - "$WORKDIR/bundle.zip" <<'EOF'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1], "w") as bundle:
    bundle.writestr("index.html", "<!doctype html><title>hosted-by-gradle</title>")
    bundle.writestr("js/app.js", "// app\n" + "function f(){return 1;}\n" * 200)
EOF
run_gradle "$APP" :backend:backendWebApp "-Pcn1.backend.webapp.bundle=$WORKDIR/bundle.zip" > "$WORKDIR/webapp.log" 2>&1 \
  || { cat "$WORKDIR/webapp.log"; fail ":backend:backendWebApp"; }
if grep -q "buildJavascriptWebApp" "$WORKDIR/webapp.log"; then
  cat "$WORKDIR/webapp.log"
  fail "staging a given bundle ran the browser build"
fi
[ -f "$APP/backend/build/webapp/index.html" ] || fail "backendWebApp staged no index.html in backend/build/webapp"
[ -f "$APP/backend/build/webapp/js/app.js.gz" ] || fail "backendWebApp wrote no compressed copy of the app's script"
check_backend_webapp "$APP" ":backend:runBackend" "$WORKDIR/backend-webapp-run.log" "hosted-by-gradle"

echo "== backend tests"
# The sample tests addBackend writes, on the JVM and (with clang) compiled. Counted
# from the reports: a test task that found no engine runs nothing and succeeds.
count_tests() {
  local total=0 count report
  for report in "$@"; do
    [ -f "$report" ] || continue
    count="$(sed -n 's/.*<testsuite[^>]* tests="\([0-9]*\)".*/\1/p' "$report" | head -1)"
    total=$((total + ${count:-0}))
  done
  echo "$total"
}
run_gradle "$APP" :backend:test > "$WORKDIR/backend-test.log" 2>&1 || { cat "$WORKDIR/backend-test.log"; fail ":backend:test"; }
JVM_TESTS="$(count_tests "$APP"/backend/build/test-results/test/TEST-*.xml)"
[ "$JVM_TESTS" -ge 3 ] || { cat "$WORKDIR/backend-test.log"; fail "the backend's tests did not run (counted $JVM_TESTS)"; }
if command -v clang > /dev/null 2>&1; then
  run_gradle "$APP" :backend:backendTest > "$WORKDIR/backend-compiled.log" 2>&1 \
    || { cat "$WORKDIR/backend-compiled.log"; fail ":backend:backendTest"; }
  COMPILED="$(count_tests "$APP"/backend/build/surefire-reports/TEST-*-compiled.xml)"
  [ "$COMPILED" -ge 3 ] || { cat "$WORKDIR/backend-compiled.log"; fail "the backend's tests did not run compiled (counted $COMPILED)"; }
fi

# A Gradle older than the plugin supports is refused up front with the fix,
# rather than failing later on an API it lacks. Opt-in: it downloads one more
# Gradle distribution.
if [ -n "${CN1_TEST_REFUSED_GRADLE_VERSION:-}" ]; then
  echo "== Gradle $CN1_TEST_REFUSED_GRADLE_VERSION is refused"
  sed -i.bak "s#gradle-[0-9.]*-bin.zip#gradle-$CN1_TEST_REFUSED_GRADLE_VERSION-bin.zip#" "$APP/gradle/wrapper/gradle-wrapper.properties"
  rm -f "$APP/gradle/wrapper/gradle-wrapper.properties.bak"
  if run_gradle "$APP" help > "$WORKDIR/old-gradle.log" 2>&1; then
    cat "$WORKDIR/old-gradle.log"
    fail "Gradle $CN1_TEST_REFUSED_GRADLE_VERSION was accepted"
  fi
  grep -q "needs Gradle [0-9.]* or newer; this build runs $CN1_TEST_REFUSED_GRADLE_VERSION" "$WORKDIR/old-gradle.log" \
    || { cat "$WORKDIR/old-gradle.log"; fail "the refusal does not say which Gradle is needed"; }
fi

echo "gradle-app-test: OK"
