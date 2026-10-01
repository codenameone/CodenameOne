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

TEMPLATES="$SCRIPTPATH/../build-engine/src/main/resources/com/codename1/project/templates/gradle"

# scaffold_lib <dir> <name> -- the whole Gradle cn1lib bootstrap
scaffold_lib() {
  local dir="$1" name="$2"
  mkdir -p "$dir/gradle/wrapper" "$dir/src/main/java/$GROUP_PATH" "$dir/src/android/java/$GROUP_PATH"
  cp "$TEMPLATES/gradlew" "$TEMPLATES/gradlew.bat" "$dir/"
  cp "$TEMPLATES"/gradle/wrapper/* "$dir/gradle/wrapper/"
  chmod +x "$dir/gradlew"
  sed -e "s/__CN1_VERSION__/$CN1_VERSION/" -e "s/__PROJECT_NAME__/$name/" \
    "$TEMPLATES/settings.gradle.kts.txt" > "$dir/settings.gradle.kts"
  cp "$TEMPLATES/gradle.properties.txt" "$dir/gradle.properties"
  use_local_plugin "$dir"
  : > "$dir/codenameone_library_appended.properties"
}

# A library the published one uses in turn: its platform code must reach the
# applications too, as Maven's profiles deliver it through the whole graph.
echo "== inner library"
INNER="$WORKDIR/inner"
scaffold_lib "$INNER" inner
cat > "$INNER/build.gradle.kts" <<EOF
group = "$GROUP"
version = "$LIBVER"

publishing {
    repositories { maven(url = uri("$LIBREPO")) }
}
EOF
cat > "$INNER/src/main/java/$GROUP_PATH/Inner.java" <<EOF
package $GROUP;

public class Inner {
    public static String prefix() {
        return "Hello ";
    }
}
EOF
cat > "$INNER/src/android/java/$GROUP_PATH/InnerAndroid.java" <<EOF
package $GROUP;

public class InnerAndroid {
}
EOF
run_gradle "$INNER" publish > "$WORKDIR/publish-inner.log" 2>&1 \
  || { cat "$WORKDIR/publish-inner.log"; fail "publishing the inner library"; }

echo "== library"
LIB="$WORKDIR/greeter"
scaffold_lib "$LIB" greeter
mkdir -p "$LIB/src/main/css" "$LIB/src/ios/objectivec"
cat > "$LIB/build.gradle.kts" <<EOF
group = "$GROUP"
version = "$LIBVER"

repositories { maven(url = uri("$LIBREPO")) }

dependencies {
    cn1lib("$GROUP:inner-lib:$LIBVER")
    // Used by the JavaSE implementation alone: the -javase pom must name it.
    javaseImplementation("$GROUP:inner-common:$LIBVER")
}

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
        return Inner.prefix() + name;
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
grep -q "<artifactId>inner-common</artifactId>" "$P/greeter-javase/$LIBVER/greeter-javase-$LIBVER.pom" \
  || { cat "$P/greeter-javase/$LIBVER/greeter-javase-$LIBVER.pom"; fail "the -javase pom omits javaseImplementation"; }
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
# A Maven consumer reaches the inner library through this dependency, which only
# resolves as <type>pom</type>: a -lib artifact has no jar.
python3 - "$P/greeter-common/$LIBVER/greeter-common-$LIBVER.pom" <<'PY' \
  || fail "greeter-common does not depend on inner-lib as <type>pom</type>"
import sys, xml.etree.ElementTree as ET
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
root = ET.parse(sys.argv[1]).getroot()
for d in root.findall('m:dependencies/m:dependency', ns):
    if d.findtext('m:artifactId', namespaces=ns) == 'inner-lib':
        sys.exit(0 if d.findtext('m:type', namespaces=ns) == 'pom' else 1)
sys.exit(1)
PY

# app-dir -- stages android and ios and checks what the library contributed
check_consumer() {
  local label="$1" android_jar="$2" ios_jar="$3"
  assert_zip_has "$android_jar" "^$GROUP_PATH/Greeter\.class$"
  assert_zip_has "$android_jar" "^$GROUP_PATH/GreeterAndroid\.java$"
  assert_zip_has "$android_jar" "^$GROUP_PATH/Inner\.class$"
  assert_zip_has "$android_jar" "^$GROUP_PATH/InnerAndroid\.java$"
  assert_zip_lacks "$android_jar" "greeter_ios\.m$"
  assert_zip_has "$android_jar" "codenameone_library_appended\.properties$"
  assert_zip_has "$ios_jar" "^$GROUP_PATH/Greeter\.class$"
  assert_zip_has "$ios_jar" "greeter_ios\.m$"
  assert_zip_lacks "$ios_jar" "GreeterAndroid\.java$"
  assert_zip_lacks "$ios_jar" "InnerAndroid\.java$"
  echo "   $label: library classes, hints and per-platform sources staged, the inner library's too"
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
# Without a theme.css of its own the library's CSS cannot be compiled; the build
# must say so, as Maven's does, instead of dropping the library's styles.
mv "$GAPP/src/main/css/theme.css" "$WORKDIR/theme.css.bak"
if run_gradle "$GAPP" cn1Css > "$WORKDIR/gapp-nocss.log" 2>&1; then
  cat "$WORKDIR/gapp-nocss.log"
  fail "cn1Css succeeded without the theme.css the library's CSS needs"
fi
grep -q "Cannot compile CSS for this project" "$WORKDIR/gapp-nocss.log" \
  || { cat "$WORKDIR/gapp-nocss.log"; fail "cn1Css failed for another reason"; }
mv "$WORKDIR/theme.css.bak" "$GAPP/src/main/css/theme.css"

echo "== a cn1lib of the same build: cn1lib(project(...))"
generate_gradle_app localconsumer com.acme.localconsumer
LAPP="$WORKDIR/localconsumer-gradle"
mkdir -p "$LAPP/localmaps/src/main/java/$GROUP_PATH" "$LAPP/localmaps/src/android/java/$GROUP_PATH"
cat > "$LAPP/localmaps/build.gradle.kts" <<EOF
plugins { id("com.codenameone") }

group = "$GROUP"
version = "$LIBVER"
EOF
: > "$LAPP/localmaps/codenameone_library_appended.properties"
cat > "$LAPP/localmaps/src/main/java/$GROUP_PATH/LocalMaps.java" <<EOF
package $GROUP;

public class LocalMaps {
}
EOF
mkdir -p "$LAPP/localmaps/src/main/css"
cat > "$LAPP/localmaps/src/main/css/theme.css" <<'EOF'
LocalMapsLabel {
    color: #00ff00;
}
EOF
cat > "$LAPP/localmaps/src/android/java/$GROUP_PATH/LocalMapsAndroid.java" <<EOF
package $GROUP;

public class LocalMapsAndroid {
}
EOF
printf '\ninclude("localmaps")\n' >> "$LAPP/settings.gradle.kts"
python3 - "$LAPP/build.gradle.kts" <<'EOF'
import sys
path = sys.argv[1]
text = open(path).read()
assert "dependencies {" in text, text
open(path, "w").write(text.replace("dependencies {", 'dependencies {\n    cn1lib(project(":localmaps"))', 1))
EOF
run_gradle "$LAPP" buildAndroid -Pcodename1.stageOnly=true > "$WORKDIR/lapp-android.log" 2>&1 \
  || { cat "$WORKDIR/lapp-android.log"; fail "buildAndroid with a project cn1lib"; }
LJAR=$(staged_jar "$WORKDIR/lapp-android.log")
assert_zip_has "$LJAR" "^$GROUP_PATH/LocalMaps\.class$"
assert_zip_has "$LJAR" "^$GROUP_PATH/LocalMapsAndroid\.java$"
run_gradle "$LAPP" classes cn1Css > "$WORKDIR/lapp-css.log" 2>&1 \
  || { cat "$WORKDIR/lapp-css.log"; fail "cn1Css with a project cn1lib"; }
LTHEME=$(find "$LAPP/build" -name theme.res | head -1)
[ -n "$LTHEME" ] && grep -aq "LocalMapsLabel" "$LTHEME" || fail "the project cn1lib's CSS was not merged into the theme"
echo "   project cn1lib: classes, android sources and CSS reach the application"

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
