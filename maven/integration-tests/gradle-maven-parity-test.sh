#!/bin/bash
# The Gradle plugin reuses the Maven plugin's build engine rather than
# re-implementing it, so one application built both ways must upload the same
# thing. This stages every cloud target from an archetype app with a native
# interface, then from its Gradle conversion, and compares the upload jars
# entry by entry; then generates the Android Studio and (on macOS) Xcode
# projects both ways and compares those trees file by file.
#
# Entries that legitimately differ are the build tools' own bookkeeping and are
# listed in IGNORED below; anything else in one jar and not the other fails.
#
# Needs the reactor installed (mvn install) and a JDK 17+; see inc/gradle.sh.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"

WORKDIR="$SCRIPTPATH/build/gradle-parity"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"
cd "$WORKDIR"

PKG=com.acme.parity
PKG_PATH=com/acme/parity
mvn_local archetype:generate -DarchetypeArtifactId=cn1app-archetype -DarchetypeGroupId=com.codenameone \
  -DplatformModules=all -DprojectType=app-with-backend \
  -DarchetypeVersion="$CN1_VERSION" -DartifactId=parity -DgroupId=$PKG -Dpackage=$PKG \
  -Dversion=1.0-SNAPSHOT -DmainName=Parity -DjavaVersion=17 -DinteractiveMode=false > "$WORKDIR/archetype.log" 2>&1 \
  || { tail -40 "$WORKDIR/archetype.log"; fail "archetype:generate"; }
MAPP="$WORKDIR/parity"
cat > "$MAPP/common/src/main/java/$PKG_PATH/Greeter.java" <<EOF
package $PKG;

import com.codename1.system.NativeInterface;

public interface Greeter extends NativeInterface {
    String greet(String name);
}
EOF
# Platform resources, which a Maven platform module packages from
# src/main/resources and the conversion moves to src/<platform>/resources.
mkdir -p "$MAPP/android/src/main/resources" "$MAPP/ios/src/main/resources"
echo "android" > "$MAPP/android/src/main/resources/parity-android.txt"
echo "ios" > "$MAPP/ios/src/main/resources/parity-ios.txt"
(cd "$MAPP" && JAVA_HOME="$GRADLE_JDK" mvn_local install -DskipTests -pl common -am > "$WORKDIR/mvn-common.log" 2>&1 \
  && JAVA_HOME="$GRADLE_JDK" mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:generate-native-interfaces" \
     > "$WORKDIR/mvn-ni.log" 2>&1) || { tail -40 "$WORKDIR/mvn-ni.log" "$WORKDIR/mvn-common.log"; fail "Maven native interfaces"; }
(cd "$MAPP" && mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:convert-to-gradle" > "$WORKDIR/convert.log" 2>&1) \
  || { cat "$WORKDIR/convert.log"; fail "convert-to-gradle"; }
GAPP="$WORKDIR/parity-gradle"
use_local_plugin "$GAPP"

staged_jar() {
  sed -n 's/.*codename1.stageOnly is set: staged \(.*\) ([0-9]* bytes) for .*/\1/p' "$1" | tail -1
}

# Bookkeeping each tool adds for itself, never read by the build server:
# directory entries, which the two archivers write for different parents, and
# Maven's own pom metadata and manifest.
IGNORED='/$|^META-INF/maven/|^META-INF/MANIFEST\.MF$'

FAILED=0
for spec in "android android-device buildAndroid" "ios ios-device buildIos" "javascript javascript buildJavascript" \
            "javase windows-desktop buildWindowsDesktop" "javase mac-os-x-desktop buildMacDesktop" \
            "win windows-device buildWindowsDevice" "linux linux-device buildLinuxDevice"; do
  read -r platform target task <<< "$spec"
  mlog="$WORKDIR/maven-$target.log"
  glog="$WORKDIR/gradle-$target.log"
  (cd "$MAPP" && JAVA_HOME="$GRADLE_JDK" mvn_local package -DskipTests -Dopen=false -Dcodename1.platform=$platform \
      -Dcodename1.buildTarget=$target -Dcodename1.stageOnly=true < /dev/null) > "$mlog" 2>&1 \
    || { tail -40 "$mlog"; fail "Maven staging $target"; }
  run_gradle "$GAPP" "$task" -Pcodename1.stageOnly=true > "$glog" 2>&1 || { cat "$glog"; fail "Gradle staging $target"; }
  mjar=$(staged_jar "$mlog")
  gjar=$(staged_jar "$glog")
  [ -f "$mjar" ] && [ -f "$gjar" ] || fail "$target: a staged jar is missing (maven=$mjar gradle=$gjar)"
  unzip -Z1 "$mjar" | grep -Ev "$IGNORED" | sort > "$WORKDIR/maven-$target.txt"
  unzip -Z1 "$gjar" | grep -Ev "$IGNORED" | sort > "$WORKDIR/gradle-$target.txt"
  if diff "$WORKDIR/maven-$target.txt" "$WORKDIR/gradle-$target.txt" > "$WORKDIR/diff-$target.txt"; then
    echo "   $target: $(wc -l < "$WORKDIR/maven-$target.txt" | tr -d ' ') entries, identical"
  else
    echo "FAIL: $target uploads differ (< Maven only, > Gradle only):"
    head -40 "$WORKDIR/diff-$target.txt"
    FAILED=1
  fi
done
# The local builders: the Android Studio project and, where Xcode is installed,
# the Xcode project. Only generated, never compiled -- the question is whether
# both build tools hand the builder the same application, and the generated
# tree answers it file by file. See inc/compare_generated_projects.py for what
# may differ.
generated_dir() {
  sed -n "s/.*Copying $2 Project to \(.*\)$/\1/p" "$1" | tail -1
}
local_source() {
  local platform=$1 target=$2 task=$3 kind=$4
  local mlog="$WORKDIR/maven-$target.log" glog="$WORKDIR/gradle-$target.log"
  (cd "$MAPP" && JAVA_HOME="$GRADLE_JDK" mvn_local package -DskipTests -Dopen=false -Dcodename1.platform=$platform \
      -Dcodename1.buildTarget=$target < /dev/null) > "$mlog" 2>&1 || { tail -40 "$mlog"; fail "Maven $target"; }
  run_gradle "$GAPP" "$task" -Popen=false > "$glog" 2>&1 || { cat "$glog"; fail "Gradle $target"; }
  local mdir gdir
  mdir=$(generated_dir "$mlog" "$kind")
  gdir=$(generated_dir "$glog" "$kind")
  [ -d "$mdir" ] && [ -d "$gdir" ] || fail "$target: a generated project is missing (maven=$mdir gradle=$gdir)"
  if python3 "$SCRIPTPATH/inc/compare_generated_projects.py" "$mdir" "$gdir" > "$WORKDIR/compare-$target.txt"; then
    echo "   $target: $(cat "$WORKDIR/compare-$target.txt")"
  else
    echo "FAIL: $target projects differ:"
    cat "$WORKDIR/compare-$target.txt"
    FAILED=1
  fi
}

local_source android android-source buildAndroidGradleProject Gradle
if xcrun --find xcodebuild > /dev/null 2>&1; then
  local_source ios ios-source buildIosXcodeProject Xcode
elif [ "${CN1_IOS_SOURCE_REQUIRED:-}" = "1" ]; then
  fail "ios-source needs Xcode, and CN1_IOS_SOURCE_REQUIRED=1 forbids skipping it"
else
  echo "   ios-source: skipped, no Xcode on this machine"
fi

[ $FAILED -eq 0 ] || exit 1
echo "gradle-maven-parity-test: OK"
