#!/bin/bash
# An unmodified Android Studio project (scripts/android-compat-samples/*) is
# imported into an archetype application and built for every target, by Maven
# and by Gradle. Checks, for each sample:
#
#   - cn1:import-android-project copies the module and merges the Gradle
#     namespace and version into the manifest;
#   - the build compiled the resources (R, the generated factories and
#     dispatchers, the binary tables) and relocated every android.* reference:
#     no class the application ships may name android/, androidx/ or
#     com/google/android/material/, or the real framework would collide with it
#     on Android and the classes would not exist anywhere else;
#   - every target's upload jar (staged, never sent) carries all of it, so the
#     iOS, Android, JavaScript and desktop builders all get the remapped code;
#   - the Gradle build uploads the same entries as the Maven build.
#
# Needs the reactor installed (mvn install) and a JDK 17+; see inc/gradle.sh.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"
SAMPLES="$SCRIPTPATH/../../scripts/android-compat-samples"

WORKDIR="$SCRIPTPATH/build/android-compat"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"

# Class files that still name an unrelocated Android package. A relocated name
# is preceded by "/" (com/codename1/androidcompat/android/...), an unrelocated
# one by the constant pool's length bytes or by the "L" of a descriptor.
unrelocated() {
  local dir="$1"
  find "$dir" -name '*.class' -print0 | while IFS= read -r -d '' f; do
    if LC_ALL=C grep -aEq '(^|[^/a-zA-Z0-9_$])L?(android|androidx|com/google/android/material)/[A-Za-z]' "$f"; then
      echo "${f#$dir/}"
    fi
  done
}

staged_jar() {
  sed -n 's/.*codename1.stageOnly is set: staged \(.*\) ([0-9]* bytes) for .*/\1/p' "$1" | tail -1
}

# Bookkeeping, never read from the upload: directory entries and Maven's pom
# metadata and manifest (as gradle-maven-parity-test.sh); Kotlin's module file,
# which each tool names after its own project (app-common, DroidApp) and only
# Kotlin reflection reads; and the ORM enhancer's per-directory list, which
# Gradle keeps in each of its classes directories and the enhancer only ever
# reads from the directory.
IGNORED='/$|^META-INF/maven/|^META-INF/MANIFEST\.MF$|^META-INF/[^/]*\.kotlin_module$|^META-INF/cn1/orm-enhanced-dependencies\.list$'
FAILED=0

for sample_dir in "$SAMPLES"/*/; do
  sample=$(basename "$sample_dir")
  echo "== $sample"
  W="$WORKDIR/$sample"
  mkdir -p "$W"
  PKG=com.acme.droid
  (cd "$W" && mvn_local archetype:generate -DarchetypeArtifactId=cn1app-archetype -DarchetypeGroupId=com.codenameone \
    -DarchetypeVersion="$CN1_VERSION" -DartifactId=app -DgroupId=$PKG -Dpackage=$PKG \
    -Dversion=1.0-SNAPSHOT -DmainName=DroidApp -DjavaVersion=17 -DinteractiveMode=false) > "$W/archetype.log" 2>&1 \
    || { tail -40 "$W/archetype.log"; fail "$sample: archetype:generate"; }
  MAPP="$W/app"
  (cd "$MAPP" && mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:import-android-project" \
      "-Dcn1.android.import=$sample_dir") > "$W/import.log" 2>&1 \
    || { tail -40 "$W/import.log"; fail "$sample: import-android-project"; }
  manifest="$MAPP/common/src/main/android/AndroidManifest.xml"
  [ -f "$manifest" ] || fail "$sample: the import copied no manifest"
  grep -q 'package="' "$manifest" || fail "$sample: the Gradle namespace was not merged into the manifest"
  grep -q 'extends com.codename1.androidcompat.runtime.AndroidLifecycle' "$MAPP/common/src/main/java/com/acme/droid/DroidApp.java" \
    || fail "$sample: the entry point does not start the Android application"

  (cd "$MAPP" && JAVA_HOME="$GRADLE_JDK" mvn_local install -DskipTests -pl common -am) > "$W/mvn-common.log" 2>&1 \
    || { tail -60 "$W/mvn-common.log"; fail "$sample: Maven build"; }
  classes="$MAPP/common/target/classes"
  for expected in com/codename1/generated/android/AndroidAppImpl.class cn1_android_res.bin cn1_android_framework.bin \
                  com/codename1/androidcompat/rt/OnClickDispatch.class; do
    [ -e "$classes/$expected" ] || fail "$sample: the build produced no $expected"
  done
  ls "$classes"/com/codename1/androidcompat/android/app/Activity.class > /dev/null 2>&1 \
    || fail "$sample: the relocated runtime is not in the application's classes"
  bad=$(unrelocated "$classes")
  if [ -n "$bad" ]; then
    echo "FAIL: $sample: classes still name android.* after the remap:"
    echo "$bad" | head -20
    FAILED=1
  fi

  (cd "$MAPP" && mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:convert-to-gradle") > "$W/convert.log" 2>&1 \
    || { cat "$W/convert.log"; fail "$sample: convert-to-gradle"; }
  GAPP="$W/app-gradle"
  use_local_plugin "$GAPP"
  [ -f "$GAPP/src/main/android/AndroidManifest.xml" ] || fail "$sample: the Gradle conversion dropped src/main/android"

  for spec in "android android-device buildAndroid" "ios ios-device buildIos" "javascript javascript buildJavascript" \
              "javase mac-os-x-desktop buildMacDesktop"; do
    read -r platform target task <<< "$spec"
    mlog="$W/maven-$target.log"
    glog="$W/gradle-$target.log"
    (cd "$MAPP" && JAVA_HOME="$GRADLE_JDK" mvn_local package -DskipTests -Dopen=false -Dcodename1.platform=$platform \
        -Dcodename1.buildTarget=$target -Dcodename1.stageOnly=true < /dev/null) > "$mlog" 2>&1 \
      || { tail -40 "$mlog"; fail "$sample: Maven staging $target"; }
    run_gradle "$GAPP" "$task" -Pcodename1.stageOnly=true > "$glog" 2>&1 || { tail -60 "$glog"; fail "$sample: Gradle staging $target"; }
    mjar=$(staged_jar "$mlog")
    gjar=$(staged_jar "$glog")
    [ -f "$mjar" ] && [ -f "$gjar" ] || fail "$sample: $target: a staged jar is missing (maven=$mjar gradle=$gjar)"
    for jar in "$mjar" "$gjar"; do
      assert_zip_has "$jar" '(^|/)com/codename1/generated/android/AndroidAppImpl\.class$'
      assert_zip_has "$jar" '(^|/)com/codename1/androidcompat/android/app/Activity\.class$'
      assert_zip_has "$jar" '(^|/)com/example/droid/R\.class$'
      assert_zip_has "$jar" '(^|/)cn1_android_res\.bin$'
      # At the root: the relocated runtime legitimately lives under
      # com/codename1/androidcompat/android/.
      assert_zip_lacks "$jar" '^(android|androidx)/.*\.class$'
    done
    unzip -Z1 "$mjar" | grep -Ev "$IGNORED" | sort > "$W/maven-$target.txt"
    unzip -Z1 "$gjar" | grep -Ev "$IGNORED" | sort > "$W/gradle-$target.txt"
    if diff "$W/maven-$target.txt" "$W/gradle-$target.txt" > "$W/diff-$target.txt"; then
      echo "   $target: $(wc -l < "$W/maven-$target.txt" | tr -d ' ') entries, Maven and Gradle identical"
    else
      echo "FAIL: $sample: $target uploads differ (< Maven only, > Gradle only):"
      head -40 "$W/diff-$target.txt"
      FAILED=1
    fi
  done
  bash "$SCRIPTPATH/android-gradle-incremental-test.sh" "$GAPP"
done

[ $FAILED -eq 0 ] || exit 1
echo "android-compat-test: OK"
