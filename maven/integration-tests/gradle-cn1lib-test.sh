#!/bin/bash
# A cn1lib built with Gradle, consumed by a Gradle app AND a Maven app.
#
#  1. The library project is the whole Gradle cn1lib bootstrap: the settings
#     file, codenameone_library_appended.properties, and sources under src/.
#  2. `./gradlew publish` writes the Maven shape cn1lib-archetype publishes:
#     N-common (with its properties under META-INF/codenameone), the cn1css
#     zip, one jar per platform, and the N-lib pom with codename1.platform
#     profiles.
#  3. A Gradle app declares cn1lib("G:N-lib:V"): the library's classes and
#     hints reach every upload, its Android sources reach only Android's, and
#     its CSS is merged into the theme.
#  4. A Maven app depends on the same N-lib pom and stages the same content,
#     so a Gradle-built library is indistinguishable from an archetype-built one.
#
# Needs the reactor installed (mvn install) and a JDK 17+; see inc/gradle.sh.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"

WORKDIR="$SCRIPTPATH/build/gradle-cn1lib"
LIBREPO="$WORKDIR/librepo"
GROUP=com.acme.cn1libtest
GROUP_PATH=com/acme/cn1libtest
LIBVER=1.0
rm -rf "$WORKDIR"
# Maven copies the library into its local repository; a copy left by an earlier
# run would be used instead of this run's.
rm -rf "$CN1_REPO/$GROUP_PATH"
mkdir -p "$WORKDIR" "$LIBREPO"
cd "$WORKDIR"

echo "== library"
LIB="$WORKDIR/greeter"
TEMPLATES="$SCRIPTPATH/../build-engine/src/main/resources/com/codename1/project/templates/gradle"
mkdir -p "$LIB/gradle/wrapper" "$LIB/src/main/java/$GROUP_PATH" "$LIB/src/main/css" \
  "$LIB/src/android/java/$GROUP_PATH" "$LIB/src/ios/objectivec"
cp "$TEMPLATES/gradlew" "$TEMPLATES/gradlew.bat" "$LIB/"
cp "$TEMPLATES"/gradle/wrapper/* "$LIB/gradle/wrapper/"
chmod +x "$LIB/gradlew"
sed -e "s/__CN1_VERSION__/$CN1_VERSION/" -e "s/__PROJECT_NAME__/greeter/" \
  "$TEMPLATES/settings.gradle.kts.txt" > "$LIB/settings.gradle.kts"
cp "$TEMPLATES/gradle.properties.txt" "$LIB/gradle.properties"
use_local_plugin "$LIB"
cat > "$LIB/build.gradle.kts" <<EOF
group = "$GROUP"
version = "$LIBVER"

publishing {
    repositories { maven(url = uri("$LIBREPO")) }
}
EOF
echo 'codename1.arg.android.xpermissions=<uses-permission android:name="android.permission.VIBRATE"/>' \
  > "$LIB/codenameone_library_appended.properties"
cat > "$LIB/src/main/java/$GROUP_PATH/Greeter.java" <<EOF
package $GROUP;

public class Greeter {
    public static String greet(String name) {
        return "Hello " + name;
    }
}
EOF
cat > "$LIB/src/main/css/theme.css" <<'EOF'
GreeterLabel {
    color: #ff0000;
}
EOF
cat > "$LIB/src/android/java/$GROUP_PATH/GreeterAndroid.java" <<EOF
package $GROUP;

public class GreeterAndroid {
}
EOF
echo "// ios" > "$LIB/src/ios/objectivec/greeter_ios.m"

run_gradle "$LIB" publish > "$WORKDIR/publish.log" 2>&1 || { cat "$WORKDIR/publish.log"; fail "publishing the library"; }

P="$LIBREPO/$GROUP_PATH"
for f in greeter-common/$LIBVER/greeter-common-$LIBVER.jar \
         greeter-common/$LIBVER/greeter-common-$LIBVER-cn1css.zip \
         greeter-android/$LIBVER/greeter-android-$LIBVER.jar \
         greeter-ios/$LIBVER/greeter-ios-$LIBVER.jar \
         greeter-javase/$LIBVER/greeter-javase-$LIBVER.jar \
         greeter-lib/$LIBVER/greeter-lib-$LIBVER.pom; do
  [ -f "$P/$f" ] || { find "$LIBREPO" -type f; fail "the library did not publish $f"; }
done
assert_zip_has "$P/greeter-common/$LIBVER/greeter-common-$LIBVER.jar" "^$GROUP_PATH/Greeter\.class$"
assert_zip_has "$P/greeter-common/$LIBVER/greeter-common-$LIBVER.jar" \
  "^META-INF/codenameone/$GROUP/greeter-common/codenameone_library_appended\.properties$"
assert_zip_has "$P/greeter-common/$LIBVER/greeter-common-$LIBVER-cn1css.zip" "css/theme\.css$"
assert_zip_has "$P/greeter-android/$LIBVER/greeter-android-$LIBVER.jar" "^$GROUP_PATH/GreeterAndroid\.java$"
assert_zip_has "$P/greeter-ios/$LIBVER/greeter-ios-$LIBVER.jar" "^greeter_ios\.m$"
grep -q "<name>codename1.platform</name>" "$P/greeter-lib/$LIBVER/greeter-lib-$LIBVER.pom" \
  || fail "the -lib pom has no codename1.platform profiles"

# app-dir -- stages android and ios and checks what the library contributed
check_consumer() {
  local label="$1" android_jar="$2" ios_jar="$3"
  assert_zip_has "$android_jar" "^$GROUP_PATH/Greeter\.class$"
  assert_zip_has "$android_jar" "^$GROUP_PATH/GreeterAndroid\.java$"
  assert_zip_lacks "$android_jar" "greeter_ios\.m$"
  assert_zip_has "$android_jar" "codenameone_library_appended\.properties$"
  assert_zip_has "$ios_jar" "^$GROUP_PATH/Greeter\.class$"
  assert_zip_has "$ios_jar" "greeter_ios\.m$"
  assert_zip_lacks "$ios_jar" "GreeterAndroid\.java$"
  echo "   $label: library classes, hints and per-platform sources staged"
}
staged_jar() {
  sed -n 's/.*codename1.stageOnly is set: staged \(.*\) ([0-9]* bytes) for .*/\1/p' "$1" | tail -1
}

echo "== Gradle consumer"
generate_gradle_app libconsumer com.acme.libconsumer
GAPP="$WORKDIR/libconsumer-gradle"
python3 - "$GAPP/build.gradle.kts" "$LIBREPO" "$GROUP:greeter-lib:$LIBVER" <<'EOF'
import sys
path, repo, coord = sys.argv[1:]
text = open(path).read()
marker = "dependencies {"
assert marker in text, text
text = text.replace(marker, marker + '\n    cn1lib("%s")' % coord, 1)
text = 'repositories { maven(url = uri("%s")) }\n\n' % repo + text
open(path, "w").write(text)
EOF
run_gradle "$GAPP" classes cn1Css > "$WORKDIR/gapp-classes.log" 2>&1 || { cat "$WORKDIR/gapp-classes.log"; fail "Gradle consumer classes"; }
THEME=$(find "$GAPP/build" -name theme.res | head -1)
grep -aq "GreeterLabel" "$THEME" || fail "the library's CSS was not merged into $THEME"
run_gradle "$GAPP" buildAndroid -Pcodename1.stageOnly=true > "$WORKDIR/gapp-android.log" 2>&1 \
  || { cat "$WORKDIR/gapp-android.log"; fail "Gradle consumer buildAndroid"; }
run_gradle "$GAPP" buildIos -Pcodename1.stageOnly=true > "$WORKDIR/gapp-ios.log" 2>&1 \
  || { cat "$WORKDIR/gapp-ios.log"; fail "Gradle consumer buildIos"; }
check_consumer "Gradle app" "$(staged_jar "$WORKDIR/gapp-android.log")" "$(staged_jar "$WORKDIR/gapp-ios.log")"

echo "== Maven consumer"
MAPP="$WORKDIR/libconsumer"
python3 - "$MAPP" "$LIBREPO" "$GROUP" "$LIBVER" <<'EOF'
import sys
app, repo, group, version = sys.argv[1:]
root = app + "/pom.xml"
text = open(root).read()
entry = "<repository><id>gradle-cn1lib</id><url>file://%s</url></repository>" % repo
if "<repositories>" in text:
    text = text.replace("<repositories>", "<repositories>" + entry, 1)
else:
    text = text.replace("</project>", "<repositories>" + entry + "</repositories>\n</project>")
open(root, "w").write(text)
common = app + "/common/pom.xml"
text = open(common).read()
dep = ("<dependency><groupId>%s</groupId><artifactId>greeter-lib</artifactId><version>%s</version>"
       "<type>pom</type></dependency>") % (group, version)
assert "<dependencies>" in text
text = text.replace("<dependencies>", "<dependencies>" + dep, 1)
open(common, "w").write(text)
EOF
# The app was generated for Java 17, so Maven runs on the same JDK as Gradle.
for spec in "android android-device" "ios ios-device"; do
  read -r platform target <<< "$spec"
  (cd "$MAPP" && JAVA_HOME="$GRADLE_JDK" mvn_local package -DskipTests -Dopen=false -Dcodename1.platform=$platform \
      -Dcodename1.buildTarget=$target -Dcodename1.stageOnly=true < /dev/null) > "$WORKDIR/mapp-$platform.log" 2>&1 \
    || { tail -60 "$WORKDIR/mapp-$platform.log"; fail "Maven consumer $target"; }
done
check_consumer "Maven app" "$(staged_jar "$WORKDIR/mapp-android.log")" "$(staged_jar "$WORKDIR/mapp-ios.log")"

echo "gradle-cn1lib-test: OK"
