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
source "$SCRIPTPATH/inc/compat.sh"
SAMPLES="$SCRIPTPATH/../../scripts/android-compat-samples"

WORKDIR="$SCRIPTPATH/build/android-compat"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"

FAILED=0

# What every target's upload has to carry, whichever tool staged it.
check_android_upload() {
  local jar="$1"
  assert_zip_has "$jar" '(^|/)com/codename1/generated/android/AndroidAppImpl\.class$'
  assert_zip_has "$jar" '(^|/)com/codename1/androidcompat/android/app/Activity\.class$'
  assert_zip_has "$jar" '(^|/)com/example/droid/R\.class$'
  assert_zip_has "$jar" '(^|/)cn1_android_res\.bin$'
  # At the root: the relocated runtime legitimately lives under
  # com/codename1/androidcompat/android/.
  assert_zip_lacks "$jar" '^(android|androidx)/.*\.class$'
}

for sample_dir in "$SAMPLES"/*/; do
  sample=$(basename "$sample_dir")
  echo "== $sample"
  W="$WORKDIR/$sample"
  mkdir -p "$W"
  PKG=com.acme.droid
  compat_generate_app "$sample" "$W" $PKG DroidApp
  MAPP="$W/app"
  (cd "$MAPP" && mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:import-android-project" \
      "-Dcn1.android.import=$sample_dir") > "$W/import.log" 2>&1 \
    || { tail -40 "$W/import.log"; fail "$sample: import-android-project"; }
  manifest="$MAPP/common/src/main/android/AndroidManifest.xml"
  [ -f "$manifest" ] || fail "$sample: the import copied no manifest"
  grep -q 'package="' "$manifest" || fail "$sample: the Gradle namespace was not merged into the manifest"
  grep -q 'extends com.codename1.androidcompat.runtime.AndroidLifecycle' "$MAPP/common/src/main/java/com/acme/droid/DroidApp.java" \
    || fail "$sample: the entry point does not start the Android application"

  compat_build_common "$sample" "$W" "$MAPP"
  classes="$MAPP/common/target/classes"
  for expected in com/codename1/generated/android/AndroidAppImpl.class cn1_android_res.bin cn1_android_framework.bin \
                  com/codename1/androidcompat/rt/OnClickDispatch.class; do
    [ -e "$classes/$expected" ] || fail "$sample: the build produced no $expected"
  done
  ls "$classes"/com/codename1/androidcompat/android/app/Activity.class > /dev/null 2>&1 \
    || fail "$sample: the relocated runtime is not in the application's classes"
  bad=$(compat_unrelocated "$classes" 'android|androidx|com/google/android/material')
  if [ -n "$bad" ]; then
    echo "FAIL: $sample: classes still name android.* after the remap:"
    echo "$bad" | head -20
    FAILED=1
  fi

  compat_convert_to_gradle "$sample" "$W" "$MAPP"
  GAPP="$W/app-gradle"
  [ -f "$GAPP/src/main/android/AndroidManifest.xml" ] || fail "$sample: the Gradle conversion dropped src/main/android"

  compat_stage_all "$sample" "$W" "$MAPP" "$GAPP" check_android_upload
  bash "$SCRIPTPATH/android-gradle-incremental-test.sh" "$GAPP"
done

[ $FAILED -eq 0 ] || exit 1
echo "android-compat-test: OK"
