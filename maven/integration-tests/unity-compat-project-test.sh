#!/bin/bash
# A Unity project built the way a developer builds one: imported into an
# archetype application with cn1:import-unity-project and compiled by the
# application's own Maven build (the compile-unity goal). unity-compat-test.sh
# covers the translator and the runtime, sample by sample and target by target;
# this covers the build integration around them. Checks:
#
#   - an application with no src/main/unity builds without the .NET SDK and
#     without the goal doing anything;
#   - the import copies Assets and ProjectSettings and nothing Unity generates
#     (Library, Temp, obj) or a version control system owns, adds the runtime
#     to the common module's pom and writes the entry point;
#   - the build translates the scripts, compiles the scenes and prefabs to
#     UnityAppImpl and puts both, with the images, in the common module's
#     classes and jar;
#   - the scene run from that jar, by the generic headless driver, prints the
#     trace committed beside the sample, line for line;
#   - a second build does not compile the C# again, and so needs no .NET SDK;
#   - every target's upload jar (staged, never sent) carries the translated
#     classes, the generated factory, the images and the runtime;
#   - converted with cn1:convert-to-gradle, the same project stages the same
#     entries for every target from Gradle, and Gradle's compileUnity task is
#     up to date for an unchanged project and needs the .NET SDK only for a
#     changed script.
#
# Needs the reactor installed with the unity-compat profile (mvn install
# -Plocal-dev-javase -Dunity-compat), the .NET SDK (DOTNET names the CLI or its
# directory, else DOTNET_ROOT, else the PATH) and a JDK 17+; see inc/gradle.sh.
#
# --classes-only stops at the common module's classes: no jar and no staging,
# which is as far as a machine with no network and no test provider in its
# local repository can go. CN1_MVN_ARGS adds arguments to every Maven run (-o),
# CN1_ARCHETYPE_GOAL replaces archetype:generate where the prefix cannot be
# resolved offline.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"
SAMPLES="$SCRIPTPATH/../../scripts/unity-compat-samples"
SAMPLE=arcade2d

CLASSES_ONLY=0
for arg in "$@"; do
  case "$arg" in
    --classes-only) CLASSES_ONLY=1 ;;
    *) echo "usage: $(basename "$0") [--classes-only]" >&2; exit 2 ;;
  esac
done

WORKDIR="$SCRIPTPATH/build/unity-compat-project"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"
W="$WORKDIR"

mvnw() {
  # shellcheck disable=SC2086
  JAVA_HOME="$GRADLE_JDK" mvn_local ${CN1_MVN_ARGS:-} "$@"
}

RUNTIME="$CN1_REPO/com/codenameone/codenameone-unity-compat/$CN1_VERSION/codenameone-unity-compat-$CN1_VERSION.jar"
CORE="$CN1_REPO/com/codenameone/codenameone-core/$CN1_VERSION/codenameone-core-$CN1_VERSION.jar"
for jar in "$RUNTIME" "${RUNTIME%.jar}-references.jar" "$CORE" \
    "$CN1_REPO/com/codenameone/codenameone-cil-translator/$CN1_VERSION/codenameone-cil-translator-$CN1_VERSION.jar"; do
  [ -f "$jar" ] || fail "$jar is missing; install the reactor with -Dunity-compat first"
done

# The SDK is named to the build only when DOTNET names it; otherwise the build
# has to find it the way a developer's does.
DOTNET_ARG=()
if [ -n "${DOTNET:-}" ]; then
  DOTNET_ARG=("-Dcn1.unity.dotnet=$DOTNET")
fi

# The PATH with dotnet taken out, for the builds that have to work without one.
# A directory that holds a dotnet cannot simply be dropped: on a Linux runner
# the SDK is linked into /usr/bin, and a PATH without /usr/bin has no env, no
# bash and no mvn either ("env: 'env': No such file or directory"). Such a
# directory is replaced by one of links to everything in it but dotnet.
NO_DOTNET_PATH=$(python3 - "$W/no-dotnet-path" <<'EOF'
import os, sys
shadow_root = sys.argv[1]
keep = []
seen = set()
for d in os.environ.get("PATH", "").split(os.pathsep):
    if not d or d in seen:
        continue
    seen.add(d)
    if not os.path.lexists(os.path.join(d, "dotnet")):
        keep.append(d)
        continue
    shadow = os.path.join(shadow_root, str(len(keep)))
    os.makedirs(shadow)
    for name in os.listdir(d):
        if name not in ("dotnet", "dotnet.exe"):
            os.symlink(os.path.join(os.path.abspath(d), name), os.path.join(shadow, name))
    keep.append(shadow)
print(os.pathsep.join(keep))
EOF
)
if PATH="$NO_DOTNET_PATH" command -v dotnet > /dev/null 2>&1; then
  fail "dotnet is still on the PATH the SDK-less builds run with: $NO_DOTNET_PATH"
fi
for tool in env bash mvn; do
  PATH="$NO_DOTNET_PATH" command -v $tool > /dev/null 2>&1 || fail "the SDK-less PATH lost $tool: $NO_DOTNET_PATH"
done
without_dotnet() {
  env -u DOTNET_ROOT -u DOTNET PATH="$NO_DOTNET_PATH" "$@"
}

PKG=com.acme.game
echo "== archetype application"
(cd "$W" && mvnw "${CN1_ARCHETYPE_GOAL:-archetype:generate}" -DarchetypeArtifactId=cn1app-archetype \
    -DarchetypeGroupId=com.codenameone -DarchetypeVersion="$CN1_VERSION" -DartifactId=app -DgroupId=$PKG \
    -Dpackage=$PKG -Dversion=1.0-SNAPSHOT -DmainName=MyGame -DjavaVersion=17 -DinteractiveMode=false) \
    > "$W/archetype.log" 2>&1 || { tail -40 "$W/archetype.log"; fail "archetype:generate"; }
APP="$W/app"
classes="$APP/common/target/classes"

echo "== without src/main/unity"
(cd "$APP" && without_dotnet env JAVA_HOME="$GRADLE_JDK" bash -c 'mvn -B -ntp "$@"' mvn \
    ${MAVEN_REPO_LOCAL:+"-Dmaven.repo.local=$MAVEN_REPO_LOCAL"} ${CN1_MVN_ARGS:-} process-classes -pl common -am) \
    > "$W/plain.log" 2>&1 || { tail -60 "$W/plain.log"; fail "an application with no Unity project did not build"; }
grep -q 'compile-unity' "$W/plain.log" || fail "the compile-unity goal is not bound in the archetype's common module"
[ ! -e "$APP/common/target/unity" ] || fail "compile-unity wrote target/unity for a project with no src/main/unity"
[ ! -e "$APP/common/target/generated-sources/unity" ] || fail "compile-unity generated sources with no src/main/unity"
[ -f "$classes/com/acme/game/MyGame.class" ] || fail "the plain application's main class was not compiled"
if grep -q 'codenameone-unity-compat' "$APP/common/target/classes/META-INF/codenameone/"* 2> /dev/null; then
  fail "the runtime reached a build with no Unity project"
fi

echo "== import"
# The sample as a Unity checkout really looks: with the directories the editor
# and the compiler generate, and a repository, none of which may be copied.
SRC="$W/unity-checkout"
cp -R "$SAMPLES/$SAMPLE" "$SRC"
mkdir -p "$SRC/Library/ScriptAssemblies" "$SRC/Temp" "$SRC/obj/Debug" "$SRC/.git" "$SRC/Assets/Scripts/obj"
echo binary > "$SRC/Library/ScriptAssemblies/Assembly-CSharp.dll"
echo lock > "$SRC/Temp/UnityLockfile"
echo cache > "$SRC/obj/Debug/project.assets.json"
echo "ref: refs/heads/main" > "$SRC/.git/HEAD"
echo "class Stale { NotAType x; }" > "$SRC/Assets/Scripts/obj/Stale.cs"
(cd "$APP" && mvnw "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:import-unity-project" "-Dsource=$SRC") \
    > "$W/import.log" 2>&1 || { tail -40 "$W/import.log"; fail "import-unity-project"; }
UNITY="$APP/common/src/main/unity"
[ -d "$UNITY/Assets" ] && [ -d "$UNITY/ProjectSettings" ] || fail "the import copied no Assets or ProjectSettings"
for unwanted in Library Temp obj .git Assets/Scripts/obj expected-trace.txt input.txt; do
  [ ! -e "$UNITY/$unwanted" ] || fail "the import copied $unwanted"
done
(cd "$SRC" && find Assets ProjectSettings -type f -not -path '*/obj/*' | sort) > "$W/source-files.txt"
(cd "$UNITY" && find Assets ProjectSettings -type f | sort) > "$W/imported-files.txt"
diff "$W/source-files.txt" "$W/imported-files.txt" > "$W/import.diff" \
  || { cat "$W/import.diff"; fail "the imported files are not the project's (< project, > imported)"; }
grep -q '<artifactId>codenameone-unity-compat</artifactId>' "$APP/common/pom.xml" \
  || fail "the common module does not depend on the runtime"
grep -q 'extends com.codename1.unitycompat.app.UnityApplication' "$APP/common/src/main/java/com/acme/game/MyGame.java" \
  || fail "the entry point does not start the Unity project"

echo "== build"
GOAL="install"
[ $CLASSES_ONLY -eq 1 ] && GOAL="process-classes"
(cd "$APP" && mvnw $GOAL -DskipTests -pl common -am "${DOTNET_ARG[@]}") > "$W/build.log" 2>&1 \
  || { tail -80 "$W/build.log"; fail "Maven build of the imported project"; }
grep -q 'Compiling the C# scripts' "$W/build.log" || fail "the build did not compile the Unity project"
if grep -q 'WARNING.*src/main/unity:' "$W/build.log"; then
  grep 'src/main/unity:' "$W/build.log"
  fail "the scene compiler warned about the sample"
fi
EXPECTED_ENTRIES=(global/Player.class global/Shot.class global/Target.class global/Faller.class
  com/codename1/generated/unity/UnityAppImpl.class com/acme/game/MyGame.class sheet.png)
for expected in "${EXPECTED_ENTRIES[@]}"; do
  [ -f "$classes/$expected" ] || fail "the build produced no $expected"
done
# The runtime jar already has the value types the scripts were compiled
# against; a second copy in the application would shadow it.
[ ! -e "$classes/UnityEngine" ] || fail "the application's classes hold a copy of the runtime's UnityEngine classes"

APPCP="$classes"
if [ $CLASSES_ONLY -eq 0 ]; then
  JAR="$APP/common/target/app-common-1.0-SNAPSHOT.jar"
  [ -f "$JAR" ] || fail "the build produced no $JAR"
  for expected in "${EXPECTED_ENTRIES[@]}"; do
    assert_zip_has "$JAR" "^$expected\$"
  done
  assert_zip_lacks "$JAR" '^UnityEngine/'
  assert_zip_lacks "$JAR" '\.(cs|dll|unity|prefab|meta)$'
  APPCP="$JAR"
fi

echo "== headless run"
H="$W/headless"
mkdir -p "$H"
"$GRADLE_JDK/bin/javac" -nowarn -d "$H" -cp "$APPCP:$RUNTIME:$CORE" $(find "$SAMPLES/headless/java" -name '*.java') \
    > "$W/headless-javac.log" 2>&1 || { cat "$W/headless-javac.log"; fail "the headless driver did not compile"; }
"$GRADLE_JDK/bin/java" -Xverify:all -Djava.awt.headless=true -cp "$H:$APPCP:$RUNTIME:$CORE" headless.HeadlessMain \
    --seed 3 --frames 300 --size 400x300 --input "$SAMPLES/$SAMPLE/input.txt" --dump 1,40,100,200 --count Shot \
    > "$W/trace.txt" 2> "$W/trace.err" || { cat "$W/trace.err"; fail "the built project failed on the JVM"; }
diff "$SAMPLES/$SAMPLE/expected-trace.txt" "$W/trace.txt" > "$W/trace.diff" \
  || { head -40 "$W/trace.diff"; fail "the built project's trace is not the sample's (< expected, > actual)"; }
echo "   $(wc -l < "$W/trace.txt" | tr -d ' ') trace lines, identical to expected-trace.txt"

echo "== incremental"
(cd "$APP" && without_dotnet env JAVA_HOME="$GRADLE_JDK" bash -c 'mvn -B -ntp "$@"' mvn \
    ${MAVEN_REPO_LOCAL:+"-Dmaven.repo.local=$MAVEN_REPO_LOCAL"} ${CN1_MVN_ARGS:-} process-classes -pl common -am) \
    > "$W/again.log" 2>&1 || { tail -60 "$W/again.log"; fail "an unchanged project needed the .NET SDK to build again"; }
if grep -q 'Compiling the C# scripts' "$W/again.log"; then
  fail "an unchanged project was compiled again"
fi
[ -f "$classes/global/Player.class" ] || fail "the second build lost the translated classes"
# A changed script is compiled again -- which, with no SDK to be found, has to
# fail on the SDK and say how to supply one.
echo "// changed" >> "$UNITY/Assets/Scripts/Shot.cs"
if (cd "$APP" && without_dotnet env JAVA_HOME="$GRADLE_JDK" bash -c 'mvn -B -ntp "$@"' mvn \
    ${MAVEN_REPO_LOCAL:+"-Dmaven.repo.local=$MAVEN_REPO_LOCAL"} ${CN1_MVN_ARGS:-} process-classes -pl common -am) \
    > "$W/no-sdk.log" 2>&1; then
  fail "a changed script was not compiled again"
fi
grep -q 'cn1.unity.dotnet' "$W/no-sdk.log" || { tail -30 "$W/no-sdk.log"; fail "the missing SDK was not reported"; }
(cd "$APP" && mvnw process-classes -pl common -am "${DOTNET_ARG[@]}") > "$W/rebuild.log" 2>&1 \
  || { tail -60 "$W/rebuild.log"; fail "the build after a changed script"; }
grep -q 'Compiling the C# scripts' "$W/rebuild.log" || fail "the changed script was not compiled once the SDK was back"

if [ $CLASSES_ONLY -eq 1 ]; then
  echo "unity-compat-project-test: OK (classes only; no jar, no staging)"
  exit 0
fi

# Bookkeeping, never read from the upload (as android-compat-test.sh): directory
# entries, Maven's pom metadata and manifest, and the ORM enhancer's
# per-directory list.
IGNORED='/$|^META-INF/maven/|^META-INF/MANIFEST\.MF$|^META-INF/[^/]*\.kotlin_module$|^META-INF/cn1/orm-enhanced-dependencies\.list$'

echo "== gradle conversion"
(cd "$APP" && mvnw "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:convert-to-gradle") > "$W/convert.log" 2>&1 \
  || { cat "$W/convert.log"; fail "convert-to-gradle"; }
GAPP="$W/app-gradle"
use_local_plugin "$GAPP"
[ -d "$GAPP/src/main/unity/Assets" ] && [ -d "$GAPP/src/main/unity/ProjectSettings" ] \
  || fail "the Gradle conversion dropped src/main/unity"
# The plugin adds the runtime, at its own version, with the translator of that
# version; a second declaration carried over from the pom could name another.
if grep -q 'codenameone-unity-compat' "$GAPP/build.gradle.kts"; then
  fail "the Gradle conversion kept the pom's declaration of the runtime"
fi
GRADLE_DOTNET_ARG=()
if [ -n "${DOTNET:-}" ]; then
  GRADLE_DOTNET_ARG=("-Pcn1.unity.dotnet=$DOTNET")
fi
# Gradle with every dotnet taken out of its reach; run_gradle is a function,
# which env cannot run.
gradle_without_dotnet() {
  (cd "$GAPP" && without_dotnet env JAVA_HOME="$GRADLE_JDK" ./gradlew --no-daemon --no-build-cache --stacktrace "$@")
}

echo "== staging"
staged_jar() {
  sed -n 's/.*codename1.stageOnly is set: staged \(.*\) ([0-9]* bytes) for .*/\1/p' "$1" | tail -1
}
FAILED=0
for spec in "android android-device buildAndroid" "ios ios-device buildIos" "javascript javascript buildJavascript" \
            "javase mac-os-x-desktop buildMacDesktop"; do
  read -r platform target task <<< "$spec"
  log="$W/stage-$target.log"
  glog="$W/gradle-$target.log"
  (cd "$APP" && mvnw package -DskipTests -Dopen=false -Dcodename1.platform=$platform \
      -Dcodename1.buildTarget=$target -Dcodename1.stageOnly=true "${DOTNET_ARG[@]}" < /dev/null) > "$log" 2>&1 \
    || { tail -40 "$log"; fail "staging $target"; }
  run_gradle "$GAPP" "$task" -Pcodename1.stageOnly=true "${GRADLE_DOTNET_ARG[@]}" > "$glog" 2>&1 \
    || { tail -60 "$glog"; fail "Gradle staging $target"; }
  jar=$(staged_jar "$log")
  gjar=$(staged_jar "$glog")
  [ -f "$jar" ] && [ -f "$gjar" ] || fail "$target: a staged jar is missing (maven=$jar gradle=$gjar)"
  for staged in "$jar" "$gjar"; do
    for expected in "${EXPECTED_ENTRIES[@]}"; do
      assert_zip_has "$staged" "(^|/)$expected\$"
    done
    # The runtime is an ordinary dependency: without it in the upload the
    # translated classes have nothing to run against on the device.
    assert_zip_has "$staged" '(^|/)com/codename1/unitycompat/app/UnityApplication\.class$'
    assert_zip_has "$staged" '(^|/)UnityEngine/Vector3\.class$'
    assert_zip_lacks "$staged" '\.(cs|dll)$'
  done
  unzip -Z1 "$jar" | grep -Ev "$IGNORED" | sort > "$W/maven-$target.txt"
  unzip -Z1 "$gjar" | grep -Ev "$IGNORED" | sort > "$W/gradle-$target.txt"
  if diff "$W/maven-$target.txt" "$W/gradle-$target.txt" > "$W/diff-$target.txt"; then
    echo "   $target: $(wc -l < "$W/maven-$target.txt" | tr -d ' ') entries, Maven and Gradle identical"
  else
    echo "FAIL: $target uploads differ (< Maven only, > Gradle only):"
    head -40 "$W/diff-$target.txt"
    FAILED=1
  fi
done
[ $FAILED -eq 0 ] || exit 1

echo "== gradle incremental"
# The translated classes are an output directory of the main source set, not
# files in javac's own: they have to be in the jar all the same.
run_gradle "$GAPP" jar > "$W/gradle-jar.log" 2>&1 || { tail -60 "$W/gradle-jar.log"; fail "Gradle jar"; }
GJAR=$(ls "$GAPP"/build/libs/*.jar | head -1)
for expected in "${EXPECTED_ENTRIES[@]}"; do
  assert_zip_has "$GJAR" "^$expected\$"
done
assert_zip_lacks "$GJAR" '^UnityEngine/'
gradle_without_dotnet classes > "$W/gradle-again.log" 2>&1 \
  || { tail -60 "$W/gradle-again.log"; fail "an unchanged Gradle project needed the .NET SDK to build again"; }
grep -q ':compileUnity UP-TO-DATE' "$W/gradle-again.log" || fail "compileUnity ran again with nothing changed"
grep -q ':compileJava UP-TO-DATE' "$W/gradle-again.log" || fail "compileJava ran again with nothing changed"
# A change to the application's Java runs the task again, because a main class
# written since the last build has to be noticed; the C# is not compiled again
# for it, so that still needs no SDK.
echo "// changed" >> "$GAPP/src/main/java/com/acme/game/MyGame.java"
gradle_without_dotnet classes > "$W/gradle-java-edit.log" 2>&1 \
  || { tail -60 "$W/gradle-java-edit.log"; fail "a Java edit needed the .NET SDK"; }
if grep -q 'Compiling the C# scripts' "$W/gradle-java-edit.log"; then
  fail "a Java edit compiled the C# again"
fi
[ -f "$GAPP/build/cn1-unity/classes/global/Player.class" ] || fail "the Java edit lost the translated classes"
echo "// changed again" >> "$GAPP/src/main/unity/Assets/Scripts/Shot.cs"
if gradle_without_dotnet classes > "$W/gradle-no-sdk.log" 2>&1; then
  fail "Gradle did not compile a changed script again"
fi
grep -q 'cn1.unity.dotnet' "$W/gradle-no-sdk.log" || { tail -30 "$W/gradle-no-sdk.log"; fail "Gradle did not report the missing SDK"; }
run_gradle "$GAPP" classes "${GRADLE_DOTNET_ARG[@]}" > "$W/gradle-rebuild.log" 2>&1 \
  || { tail -60 "$W/gradle-rebuild.log"; fail "the Gradle build after a changed script"; }
grep -q 'Compiling the C# scripts' "$W/gradle-rebuild.log" || fail "Gradle did not compile the changed script once the SDK was back"

echo "unity-compat-project-test: OK"
