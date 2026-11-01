#!/bin/bash
# Flutter sources (src/main/flutter) in a Gradle application. An archetype app
# gets a Dart counter and an asset, is built with Maven, converted to Gradle
# and built again; both builds must produce the same transpiled classes and
# flattened assets, and both must point those classes at the Dart source
# (SourceFile main.dart, a JSR-45 map) for the debugger. Then the Flutter
# sources are deleted and the Gradle build must drop everything they produced.
#
# Needs the reactor installed (mvn install) with codenameone-flutter-runtime,
# which builds only on JDK 17+, and a JDK 17+; see inc/gradle.sh.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"

WORKDIR="$SCRIPTPATH/build/gradle-flutter"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"
cd "$WORKDIR"

# The transpiler refuses to run below JDK 17, and so does the Maven build of it.
mvn17() {
  JAVA_HOME="$GRADLE_JDK" mvn_local "$@"
}

PKG=com.acme.flutterapp
mvn17 archetype:generate -DarchetypeArtifactId=cn1app-archetype -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion="$CN1_VERSION" -DartifactId=flutterapp -DgroupId=$PKG -Dpackage=$PKG \
  -Dversion=1.0-SNAPSHOT -DmainName=FlutterApp -DjavaVersion=17 -DinteractiveMode=false > "$WORKDIR/archetype.log" 2>&1 \
  || { tail -40 "$WORKDIR/archetype.log"; fail "archetype:generate"; }
APP="$WORKDIR/flutterapp"

# The runtime dependency the Maven goal asks for; the Gradle plugin adds its own.
python3 - "$APP/common/pom.xml" <<'EOF'
import sys
path = sys.argv[1]
text = open(path).read()
dep = """    <dependencies>
        <dependency>
            <groupId>com.codenameone</groupId>
            <artifactId>codenameone-flutter-runtime</artifactId>
            <version>${cn1.version}</version>
        </dependency>"""
assert "<dependencies>" in text
open(path, "w").write(text.replace("    <dependencies>", dep, 1))
EOF
FL="$APP/common/src/main/flutter"
mkdir -p "$FL/assets/img"
cp "$SCRIPTPATH/../dart-transpiler/src/test/resources/golden/counter/main.dart" "$FL/main.dart"
printf 'not really a png' > "$FL/assets/img/logo_1.png"

# process-classes: remap-compat, which maps the classes to the Dart source, runs there.
mvn17 -f "$APP/pom.xml" -pl common -am process-classes > "$WORKDIR/maven.log" 2>&1 \
  || { tail -60 "$WORKDIR/maven.log"; fail "the Maven build of the Flutter app"; }
MCLASSES="$APP/common/target/classes"
FPKG=com/codename1/generated/flutter
[ -f "$MCLASSES/$FPKG/MyApp.class" ] || fail "Maven transpiled no Flutter classes"

mvn17 "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:convert-to-gradle" \
  -Dcn1.sourceProject="$APP" -Dcn1.outputDir="$WORKDIR/gradle" > "$WORKDIR/convert.log" 2>&1 \
  || { cat "$WORKDIR/convert.log"; fail "converting the Flutter app"; }
G="$WORKDIR/gradle"
use_local_plugin "$G"
[ -f "$G/src/main/flutter/main.dart" ] || fail "the conversion did not carry src/main/flutter"
grep -q 'codenameone-flutter-runtime' "$G/build.gradle.kts" \
  && fail "the conversion declared the runtime the plugin adds itself"

run_gradle "$G" classes > "$WORKDIR/gradle.log" 2>&1 || { tail -60 "$WORKDIR/gradle.log"; fail "the Gradle build of the Flutter app"; }
GCLASSES="$G/build/classes/java/main"

# Same transpiled classes both ways.
(cd "$MCLASSES" && find $FPKG -name '*.class' | sort) > "$WORKDIR/maven-classes.txt"
(cd "$GCLASSES" && find $FPKG -name '*.class' | sort) > "$WORKDIR/gradle-classes.txt"
diff "$WORKDIR/maven-classes.txt" "$WORKDIR/gradle-classes.txt" \
  || fail "Maven and Gradle transpiled different Flutter classes"

# Same flattened assets, and the manifest the runtime reads.
ASSET=cn1f_assets_simg_slogo__1.png
[ -f "$MCLASSES/$ASSET" ] || fail "Maven did not flatten the asset"
[ -f "$G/build/cn1-flutter/resources/$ASSET" ] || fail "Gradle did not flatten the asset"
[ -f "$G/build/cn1-flutter/resources/cn1f__AssetManifest.txt" ] || fail "Gradle wrote no asset manifest"

# Both builds point the classes at the Dart source.
for c in "$MCLASSES" "$GCLASSES"; do
  OUT=$("$GRADLE_JDK/bin/javap" -v -cp "$c" com.codename1.generated.flutter._MyHomePageState)
  echo "$OUT" | grep -q 'SourceFile: "main.dart"' || fail "$c: the class does not name main.dart"
  echo "$OUT" | grep -q 'SourceDebugExtension' || fail "$c: the class carries no source map"
  echo "$OUT" | grep -q 'cn1DartLines' && fail "$c: the line table was left in the class"
done

# The jar holds the assets: they are part of the main output.
run_gradle "$G" jar > "$WORKDIR/jar.log" 2>&1 || { tail -40 "$WORKDIR/jar.log"; fail "gradle jar"; }
assert_zip_has "$(ls "$G"/build/libs/*.jar | head -1)" "^$ASSET\$"

# Removing the Flutter sources removes what they produced.
rm -r "$G/src/main/flutter"
run_gradle "$G" classes > "$WORKDIR/gradle-removed.log" 2>&1 \
  || { tail -60 "$WORKDIR/gradle-removed.log"; fail "the Gradle build after removing the Flutter sources"; }
[ -f "$GCLASSES/$FPKG/MyApp.class" ] && fail "the transpiled classes outlived their Dart sources"
[ -f "$G/build/cn1-flutter/resources/$ASSET" ] && fail "the flattened asset outlived its source"

echo "PASS: Flutter sources build the same with Gradle as with Maven"
