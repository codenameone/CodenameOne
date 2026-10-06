#!/bin/bash
# The archetype's default Maven layouts, end to end:
#
#  1. The minimal app: a root pom and common/, no platform modules. common does
#     each missing module's work (the cn1-host-* profiles and the hosted goals of
#     the codenameone-maven-plugin), so this checks every path a module used to
#     own:
#       * a native interface whose stubs land in <platform>/src/main/... with no
#         pom beside them;
#       * cn1:test (-Ptest) and JUnit (mvn test) run from common and reach the
#         JavaSE native code, which compile-javase-natives builds into
#         target/cn1-javase/classes;
#       * the simulator's argument file carries that directory;
#       * -Pexecutable-jar writes the desktop jar the javase module used to;
#       * every device target stages, from common, exactly the native files of
#         its own platform, and the Android upload carries no JavaSE classes;
#       * adding a platform module later moves that platform's build into it,
#         with nothing left in common.
#  2. The backend-only project: one module at the root that builds its router and
#     entry point, and packages into a native server that answers /healthz.
#
# Nothing is submitted and nothing opens a window: device builds stop at
# -Dcodename1.stageOnly, and the simulator is only prepared, never launched.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

generate() {
  local name=$1
  shift
  rm -rf "$name"
  mvn -B -ntp archetype:generate \
    -DarchetypeArtifactId=cn1app-archetype \
    -DarchetypeGroupId=com.codenameone \
    -DarchetypeVersion=$CN1_VERSION \
    -DartifactId=$name \
    -DgroupId=com.example \
    -Dpackage=com.example.$name \
    -Dversion=1.0-SNAPSHOT \
    -DmainName=MyApp \
    -DinteractiveMode=false "$@"
}

cd $SCRIPTPATH/build

echo "== The minimal app layout"
generate minimal
cd minimal
for module in javase android ios javascript win linux backend; do
  [ ! -e "$module" ] || fail "the minimal layout generated $module/"
done
[ -f common/pom.xml ] || fail "no common/pom.xml"
grep -q '<goal>compile-javase-natives</goal>' common/pom.xml || fail "common/pom.xml has no hosted profiles"
if grep -q '<activeByDefault>true</activeByDefault>' pom.xml; then
  fail "the root pom still activates the missing javase module by default"
fi

mkdir -p common/src/main/java/com/example/minimal
cat > common/src/main/java/com/example/minimal/Hello.java <<'EOF'
package com.example.minimal;

import com.codename1.system.NativeInterface;

public interface Hello extends NativeInterface {
    String hello();
}
EOF
mvn -B -ntp compile cn1:generate-native-interfaces
JAVASE_IMPL=javase/src/main/java/com/example/minimal/HelloImpl.java
ANDROID_IMPL=android/src/main/java/com/example/minimal/HelloImpl.java
[ -f "$JAVASE_IMPL" ] || fail "no JavaSE stub at $JAVASE_IMPL"
[ -f "$ANDROID_IMPL" ] || fail "no Android stub at $ANDROID_IMPL"
for module in javase android ios; do
  [ ! -e "$module/pom.xml" ] || fail "generate-native-interfaces created $module/pom.xml"
done
# A JavaSE implementation that only a JDK can compile, so a natives classpath that
# fell back to the bytecode-compliance-checked compile would fail here.
perl -0pi -e 's/return null;/return "hi from " + javax.swing.UIManager.class.getSimpleName();/' "$JAVASE_IMPL"

cat > common/src/test/java/com/example/minimal/NativeCallTest.java <<'EOF'
package com.example.minimal;

import com.codename1.system.NativeLookup;
import com.codename1.testing.AbstractTest;

public class NativeCallTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        Hello hello = NativeLookup.create(Hello.class);
        String said = hello == null ? null : hello.hello();
        System.out.println("NATIVE SAID: " + said);
        return "hi from UIManager".equals(said);
    }
}
EOF

echo "== cn1:test from common"
mvn -B -ntp verify -Ptest -Dcodename1.platform=javase | tee ../minimal-test.log
grep -q 'NATIVE SAID: hi from UIManager' ../minimal-test.log || fail "the JavaSE native was not reached from cn1:test"
grep -q 'NativeCallTest passed' ../minimal-test.log || fail "NativeCallTest did not pass"
[ -f common/target/cn1-javase/classes/com/example/minimal/HelloImpl.class ] \
  || fail "compile-javase-natives did not compile $JAVASE_IMPL"
[ ! -f common/target/classes/com/example/minimal/HelloImpl.class ] \
  || fail "the JavaSE native was compiled into common's own classes, which every device build uploads"

echo "== JUnit from common"
cat > common/src/test/java/com/example/minimal/NativesJUnitTest.java <<'EOF'
package com.example.minimal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NativesJUnitTest {
    @Test
    public void theJavaseNativeIsOnTheTestClasspath() throws Exception {
        Object impl = Class.forName("com.example.minimal.HelloImpl").getDeclaredConstructor().newInstance();
        assertEquals("hi from UIManager", impl.getClass().getMethod("hello").invoke(impl));
    }
}
EOF
mvn -B -ntp test
REPORT=common/target/surefire-reports/TEST-com.example.minimal.NativesJUnitTest.xml
[ -f "$REPORT" ] || fail "Surefire did not run the JUnit test"
grep -q 'tests="1"' "$REPORT" || fail "the JUnit test did not run exactly once"
grep -q 'failures="0"' "$REPORT" || fail "the JUnit test failed"

echo "== The simulator classpath"
# The simulator needs JDK 11 or newer. A leg that names a newer JDK for the native
# backend step prepares it on that one; on an older JDK the check is that preparing
# it refuses, with the reason, instead of launching something that cannot start.
SIMULATOR_JDK="${CN1_BACKEND_PACKAGE_JDK:-$JAVA_HOME}"
SIMULATOR_JAVA_MAJOR="$("$SIMULATOR_JDK/bin/java" -version 2>&1 | sed -n 's/.*version "\(1\.\)\{0,1\}\([0-9]*\).*/\2/p' | head -1)"
ARGS=common/target/codenameone/simulator-classpath.args
if [ "${SIMULATOR_JAVA_MAJOR:-0}" -lt 11 ]; then
  if JAVA_HOME="$SIMULATOR_JDK" mvn -B -ntp initialize -Psimulator -Dcodename1.platform=javase \
      > ../simulator-old-jdk.log 2>&1; then
    fail "preparing the simulator on Java $SIMULATOR_JAVA_MAJOR should refuse"
  fi
  grep -q 'supports JDK 11' ../simulator-old-jdk.log \
    || { tail -30 ../simulator-old-jdk.log; fail "the simulator refused Java $SIMULATOR_JAVA_MAJOR without saying why"; }
  echo "NOTE the simulator needs JDK 11+; Java $SIMULATOR_JAVA_MAJOR was refused with the reason"
else
  JAVA_HOME="$SIMULATOR_JDK" mvn -B -ntp initialize -Psimulator -Dcodename1.platform=javase
  [ -f "$ARGS" ] || fail "prepare-simulator-classpath wrote no $ARGS"
  grep -q 'cn1-javase' "$ARGS" || fail "the simulator classpath has no JavaSE natives"
fi

echo "== The desktop jar"
mvn -B -ntp package -Pexecutable-jar -DskipTests -Dcodename1.platform=javase
JAR=common/target/minimal-javase-1.0-SNAPSHOT.jar
[ -f "$JAR" ] || fail "no desktop jar at $JAR"
[ -f common/target/minimal-javase-1.0-SNAPSHOT.zip ] || fail "no desktop zip"
unzip -p "$JAR" META-INF/MANIFEST.MF | grep -q 'Main-Class: com.example.minimal.MyAppStub' \
  || fail "the desktop jar has the wrong Main-Class"
for entry in com/example/minimal/MyAppStub.class com/example/minimal/HelloImpl.class \
    com/example/minimal/MyApp.class NativeTheme.res codenameone-desktop.properties applicationIconImage_64x64.png; do
  unzip -l "$JAR" | grep -q " $entry\$" || fail "the desktop jar has no $entry"
done
[ -f common/target/libs/codenameone-javase-$CN1_VERSION.jar ] || fail "the desktop jar's libs/ has no JavaSE port"
# The bundled FFmpeg media implementation, which the javase module's desktop app ships.
ls common/target/libs/ffmpeg-*.jar >/dev/null 2>&1 || fail "the desktop jar's libs/ has no FFmpeg binaries"

echo "== Every device target, staged from common"
stage() {
  local platform=$1 target=$2
  mvn -B -ntp package -DskipTests -Dcodename1.platform=$platform -Dcodename1.buildTarget=$target \
    -Dcodename1.stageOnly=true > ../stage-$target.log 2>&1 || { tail -40 ../stage-$target.log; fail "staging $target"; }
  STAGED=common/target/minimal-common-1.0-SNAPSHOT-$target-jar-with-dependencies.jar
  [ -f "$STAGED" ] || fail "nothing staged for $target at $STAGED"
  unzip -l "$STAGED" | grep -q ' com/example/minimal/MyApp.class$' || fail "$target: the app is missing"
}
stage android android-device
unzip -l "$STAGED" | grep -q ' com/example/minimal/HelloImpl.java$' || fail "android: no native source"
if unzip -l "$STAGED" | grep -q 'HelloImpl.class$'; then
  fail "android: the JavaSE native leaked into the upload"
fi
stage ios ios-device
unzip -l "$STAGED" | grep -q ' com_example_minimal_HelloImpl.m$' || fail "ios: no native source"
stage javase mac-os-x-desktop
unzip -l "$STAGED" | grep -q ' com/example/minimal/HelloImpl.class$' || fail "desktop: no compiled JavaSE native"
stage win windows-device
stage linux linux-device
stage javascript javascript

echo "== A platform module added later takes over"
( cd "$SCRIPTPATH/build" && generate minimal-full -DplatformModules=android )
sed 's/minimal-full/minimal/g' "$SCRIPTPATH/build/minimal-full/android/pom.xml" > android/pom.xml
rm -f common/target/*android-device-jar-with-dependencies.jar
mvn -B -ntp package -DskipTests -Dcodename1.platform=android -Dcodename1.buildTarget=android-device \
  -Dcodename1.stageOnly=true
[ -f android/target/minimal-android-1.0-SNAPSHOT-android-device-jar-with-dependencies.jar ] \
  || fail "the added android module did not stage the build"
if ls common/target/*android-device-jar-with-dependencies.jar >/dev/null 2>&1; then
  fail "common built android too, beside the module"
fi
cd ..

echo "== The backend-only layout"
generate server -DprojectType=backend-only
cd server
for entry in common javase android backend run.sh build.sh; do
  [ ! -e "$entry" ] || fail "the backend-only project has $entry"
done
[ -f application.properties ] || fail "no application.properties at the root"
if grep -rqi gradle pom.xml application.properties src; then
  fail "the backend-only project mentions Gradle"
fi
mvn -B -ntp process-classes
MAIN_CLASS_FILE=target/classes/META-INF/cn1-backend-main
[ -f "$MAIN_CLASS_FILE" ] || fail "no generated entry point"
GENERATED_MAIN="$(cat "$MAIN_CLASS_FILE")"
[ -f "target/classes/$(echo "$GENERATED_MAIN" | tr '.' '/').class" ] || fail "$GENERATED_MAIN was not compiled"
[ -f target/classes/com/example/server/ApiRouter.class ] || fail "no router for the @RestController"
# The native binary, from the root with no module or profile -- what the project's
# README tells its developer to run.
if ! command -v clang >/dev/null 2>&1; then
  [ "${CN1_BACKEND_PACKAGE_REQUIRED:-0}" != "1" ] || fail "clang is required to package a backend"
  echo "NOTE skipping cn1:backend-package: no clang on PATH"
else
  mvn -B -ntp cn1:backend-package
  [ -x target/server ] || fail "cn1:backend-package produced no executable at target/server"
  SERVER_PORT="${CN1_BACKEND_PACKAGE_PORT:-18081}"
  CN1_PROFILE=dev PORT="$SERVER_PORT" ./target/server > target/server-run.log 2>&1 &
  SERVER_PID=$!
  trap 'kill -9 $SERVER_PID 2>/dev/null || true' EXIT
  HEALTH=""
  for attempt in $(seq 1 60); do
    HEALTH="$(curl -s -m 1 "http://127.0.0.1:$SERVER_PORT/healthz" || true)"
    [ "$HEALTH" != "ok" ] || break
    kill -0 $SERVER_PID 2>/dev/null || { cat target/server-run.log >&2; fail "the packaged server exited"; }
    sleep 1
  done
  kill -9 $SERVER_PID 2>/dev/null || true
  trap - EXIT
  [ "$HEALTH" = "ok" ] || { cat target/server-run.log >&2; fail "the packaged server did not answer /healthz"; }
fi
cd ..

echo "PASS: the minimal and backend-only Maven layouts"
