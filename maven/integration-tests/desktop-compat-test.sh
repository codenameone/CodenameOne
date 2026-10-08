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
#     iOS, Android, JavaScript and desktop builders all get the remapped code;
#   - the Gradle build uploads the same entries as the Maven build.
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
TOOLKITS='java/awt|javax/swing|java/beans|org/jdesktop|javafx'
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
done

[ $FAILED -eq 0 ] || exit 1
echo "desktop-compat-test: OK"
