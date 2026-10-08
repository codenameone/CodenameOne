#!/bin/bash
# An unmodified Swing or JavaFX project (scripts/desktop-compat-samples/*) is
# imported into an archetype application and built for every target, by Maven
# and by Gradle. Checks, for each sample:
#
#   - cn1:import-desktop-project copies the sources and records the
#     application's entry point in src/main/desktop/cn1-desktop.properties;
#   - the build relocated every reference to the toolkits: no class the
#     application ships may name java/awt/, javax/swing/, java/beans/,
#     org/jdesktop/ or javafx/, because no device has those classes and the
#     first two packages cannot be defined by anything but a JDK;
#   - the runtime of the toolkit the sample uses is in the application's
#     classes, under its relocated name;
#   - every target's upload jar (staged, never sent) carries all of it, so the
#     iOS, Android, JavaScript and desktop builders all get the remapped code,
#     and no class in any of those jars names a toolkit type either;
#   - the Gradle build uploads the same entries as the Maven build.
#
# It also prints what the layer costs an application: the size of each staged
# jar, and how many of its classes and bytes are the layer's runtime rather
# than the application.
#
# Needs the reactor installed (mvn install) and a JDK 17+; see inc/gradle.sh.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
SAMPLES="$SCRIPTPATH/../../scripts/desktop-compat-samples"
if [ ! -d "$SAMPLES" ] || [ -z "$(find "$SAMPLES" -mindepth 1 -maxdepth 1 -type d 2>/dev/null)" ]; then
  echo "desktop-compat-test: SKIPPED, no samples under scripts/desktop-compat-samples"
  exit 0
fi
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"
source "$SCRIPTPATH/inc/compat.sh"

WORKDIR="${CN1_COMPAT_WORKDIR:-$SCRIPTPATH/build}/desktop-compat"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"

# The packages of both toolkits, as an application is compiled against them.
TOOLKITS='java/awt|javax/swing|java/beans|javax/accessibility|javax/imageio|org/jdesktop|javafx'
FAILED=0

# What every target's upload has to carry, whichever tool staged it. MAIN_ENTRY
# and RUNTIME are set per sample, below.
check_desktop_upload() {
  local jar="$1"
  assert_zip_has "$jar" "(^|/)$MAIN_ENTRY\\.class\$"
  assert_zip_has "$jar" "(^|/)$RUNTIME/.*\\.class\$"
  # At the root: the relocated runtimes legitimately live under
  # com/codename1/desktopcompat/ and com/codename1/fxcompat/.
  assert_zip_lacks "$jar" "^($TOOLKITS)/.*\\.class\$"
  # And inside the classes: the same question the classes directory was asked,
  # of what is actually uploaded.
  local bad
  bad=$(compat_unrelocated "$jar" "$TOOLKITS")
  if [ -n "$bad" ]; then
    echo "FAIL: $jar: classes still name a desktop toolkit:"
    echo "$bad" | head -20
    FAILED=1
  fi
}

# desktop_shipped_size <target> <jar>
#
# One line: the jar's size, and the share of it that is a compatibility
# runtime (the relocated toolkit and the shared JDK classes) as a class count,
# the bytes those classes take in the jar, and their size unpacked.
desktop_shipped_size() {
  local target="$1" jar="$2" total
  total=$(wc -c < "$jar" | tr -d ' ')
  unzip -v "$jar" | awk -v target="$target" -v total="$total" '
    $NF ~ /\.class$/ { all++ }
    $NF ~ /^com\/codename1\/(desktopcompat|fxcompat|compat\/jdk)\/.*\.class$/ { n++; raw += $1; packed += $3 }
    END { printf "   size %s: jar %d bytes, %d classes; compatibility runtime %d classes, %d bytes in the jar (%d unpacked)\n",
                 target, total, all, n, packed, raw }'
}

for sample_dir in "$SAMPLES"/*/; do
  sample=$(basename "$sample_dir")
  echo "== $sample"
  W="$WORKDIR/$sample"
  mkdir -p "$W"
  PKG=com.acme.desktop
  compat_generate_app "$sample" "$W" $PKG DesktopApp
  MAPP="$W/app"
  (cd "$MAPP" && mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:import-desktop-project" \
      "-Dcn1.desktop.import=$sample_dir") > "$W/import.log" 2>&1 \
    || { tail -40 "$W/import.log"; fail "$sample: import-desktop-project"; }
  desktop="$MAPP/common/src/main/desktop"
  record="$desktop/cn1-desktop.properties"
  [ -f "$record" ] || fail "$sample: the import recorded no entry point"
  main_class=$(sed -n 's/^mainClass=//p' "$record" | tr -d '\r' | tail -1)
  kind=$(sed -n 's/^kind=//p' "$record" | tr -d '\r' | tail -1)
  [ -n "$main_class" ] || fail "$sample: the entry record names no mainClass"
  case "$kind" in
    swing)  RUNTIME=com/codename1/desktopcompat ;;
    javafx) RUNTIME=com/codename1/fxcompat ;;
    *)      fail "$sample: the entry record's kind is '$kind', neither swing nor javafx" ;;
  esac
  MAIN_ENTRY=$(echo "$main_class" | tr . /)
  [ -d "$desktop/java" ] || [ -d "$desktop/kotlin" ] || fail "$sample: the import copied no sources"
  [ -z "$(find "$desktop" -name module-info.java)" ] || fail "$sample: the import kept a module descriptor"
  echo "   $kind application, started by $main_class"

  compat_build_common "$sample" "$W" "$MAPP"
  classes="$MAPP/common/target/classes"
  [ -f "$classes/$MAIN_ENTRY.class" ] || fail "$sample: the build produced no $MAIN_ENTRY.class"
  [ -n "$(find "$classes/$RUNTIME" -name '*.class' 2>/dev/null | head -1)" ] \
    || fail "$sample: the relocated $kind runtime is not in the application's classes"
  bad=$(compat_unrelocated "$classes" "$TOOLKITS")
  if [ -n "$bad" ]; then
    echo "FAIL: $sample: classes still name a desktop toolkit after the remap:"
    echo "$bad" | head -20
    FAILED=1
  fi

  compat_convert_to_gradle "$sample" "$W" "$MAPP"
  GAPP="$W/app-gradle"
  [ -f "$GAPP/src/main/desktop/cn1-desktop.properties" ] || fail "$sample: the Gradle conversion dropped src/main/desktop"

  compat_stage_all "$sample" "$W" "$MAPP" "$GAPP" check_desktop_upload
  for target in android-device ios-device javascript mac-os-x-desktop; do
    staged=$(compat_staged_jar "$W/maven-$target.log")
    [ -f "$staged" ] && desktop_shipped_size "$target" "$staged"
  done
done

# An Android application and a desktop one in the SAME project. A project has
# one main class and each layer's build generates it, so the two cannot both
# start: with the entry record in place the build has to stop and say so --
# never pick one silently -- and without the record the Android application
# starts while the desktop classes ship beside it, relocated like any library
# the Android code could call. Staged for one target; the relocation is the
# same for all of them and the loop above covers the rest.
ANDROID_SAMPLE="$SCRIPTPATH/../../scripts/android-compat-samples/gallery"
SWING_SAMPLE="$SAMPLES/swing-gallery"
if [ -d "$ANDROID_SAMPLE" ] && [ -d "$SWING_SAMPLE" ]; then
  echo "== both layers (android gallery + swing-gallery)"
  W="$WORKDIR/both-layers"
  mkdir -p "$W"
  compat_generate_app both "$W" com.acme.both BothApp
  MAPP="$W/app"
  (cd "$MAPP" && mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:import-android-project" \
      "-Dcn1.android.import=$ANDROID_SAMPLE") > "$W/import-android.log" 2>&1 \
    || { tail -40 "$W/import-android.log"; fail "both: import-android-project"; }
  (cd "$MAPP" && mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:import-desktop-project" \
      "-Dcn1.desktop.import=$SWING_SAMPLE") > "$W/import-desktop.log" 2>&1 \
    || { tail -40 "$W/import-desktop.log"; fail "both: import-desktop-project"; }
  if (cd "$MAPP" && JAVA_HOME="$GRADLE_JDK" mvn_local install -DskipTests -pl common -am) > "$W/mvn-conflict.log" 2>&1; then
    fail "both: two applications built as if one of them had been chosen"
  fi
  grep -q 'This project holds an Android application (src/main/android) and a desktop application' "$W/mvn-conflict.log" \
    || { tail -40 "$W/mvn-conflict.log"; fail "both: the build failed without naming the two applications"; }
  echo "   with the entry record: the build stops and names both applications"

  rm -f "$MAPP/common/src/main/desktop/cn1-desktop.properties"
  compat_build_common both "$W" "$MAPP"
  (cd "$MAPP" && JAVA_HOME="$GRADLE_JDK" mvn_local package -DskipTests -Dopen=false -Dcodename1.platform=ios \
      -Dcodename1.buildTarget=ios-device -Dcodename1.stageOnly=true < /dev/null) > "$W/maven-ios-device.log" 2>&1 \
    || { tail -40 "$W/maven-ios-device.log"; fail "both: Maven staging ios-device"; }
  staged=$(compat_staged_jar "$W/maven-ios-device.log")
  [ -f "$staged" ] || fail "both: no staged jar"
  assert_zip_has "$staged" '^com/acme/both/BothApp\.class$'
  assert_zip_has "$staged" '^com/codename1/generated/android/AndroidAppImpl\.class$'
  assert_zip_has "$staged" '^com/codename1/androidcompat/android/app/Activity\.class$'
  assert_zip_has "$staged" '^com/codename1/desktopcompat/javax/swing/JFrame\.class$'
  assert_zip_has "$staged" '^com/example/gallery/GalleryApp\.class$'
  bad=$(compat_unrelocated "$staged" "$TOOLKITS|android|androidx|com/google/android/material")
  if [ -n "$bad" ]; then
    echo "FAIL: both: classes still name a toolkit of either layer:"
    echo "$bad" | head -20
    FAILED=1
  fi
  echo "   without it: the Android application starts, both runtimes ship relocated"
  desktop_shipped_size "ios-device (both layers)" "$staged"
fi

[ $FAILED -eq 0 ] || exit 1
echo "desktop-compat-test: OK"
