#!/bin/bash
# Migrating to Gradle with cn1:convert-to-gradle, which needs no project of its
# own (it runs anywhere, with -Dcn1.sourceProject):
#
#  1. An Ant project -- Java and Kotlin mixed in src/, css/, native/<platform>,
#     a Java 8 target -- converts, builds (Kotlin included), and stages an
#     Android upload carrying its native implementation.
#  2. A Maven project whose backend has code of its own keeps the backend as a
#     subproject, and it builds.
#  3. A project with a legacy .cn1lib is refused, naming the file, and nothing
#     is written.
#
# Needs the reactor installed (mvn install) and a JDK 17+; see inc/gradle.sh.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"

WORKDIR="$SCRIPTPATH/build/gradle-migrate"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"
cd "$WORKDIR"

convert() {
  local src="$1" out="$2" log="$3"
  shift 3
  mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:convert-to-gradle" \
    -Dcn1.sourceProject="$src" -Dcn1.outputDir="$out" "$@" > "$log" 2>&1
}

echo "== Ant to Gradle"
ANT="$WORKDIR/AntApp"
mkdir -p "$ANT/src/com/acme/antapp" "$ANT/css" "$ANT/native/android/com/acme/antapp" "$ANT/native/ios"
echo '<project name="AntApp" default="jar"/>' > "$ANT/build.xml"
cat > "$ANT/codenameone_settings.properties" <<'EOF'
codename1.mainName=AntApp
codename1.packageName=com.acme.antapp
codename1.displayName=AntApp
codename1.version=1.0
codename1.cssTheme=true
codename1.arg.java.version=8
EOF
cp "$SCRIPTPATH/../cn1app-archetype/src/main/resources/archetype-resources/common/icon.png" "$ANT/icon.png" 2>/dev/null \
  || printf 'png' > "$ANT/icon.png"
cat > "$ANT/src/com/acme/antapp/AntApp.java" <<'EOF'
package com.acme.antapp;

import com.codename1.system.Lifecycle;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Form;

public class AntApp extends Lifecycle {
    @Override
    public void runApp() {
        Form f = new Form(new Helper().title());
        MyNative n = NativeLookup.create(MyNative.class);
        f.show();
    }
}
EOF
cat > "$ANT/src/com/acme/antapp/Helper.kt" <<'EOF'
package com.acme.antapp

class Helper {
    fun title(): String = "Converted"
}
EOF
# A Kotlin @RestClient returning a Java @Mapped type: Kotlin's annotations are
# processed before javac has run, so the processors must see the Java type too.
cat > "$ANT/src/com/acme/antapp/Pet.java" <<'EOF'
package com.acme.antapp;

import com.codename1.annotations.Mapped;

@Mapped
public class Pet {
    public Long id;
    public String name;

    public Pet() {
    }
}
EOF
cat > "$ANT/src/com/acme/antapp/PetApi.kt" <<'EOF'
package com.acme.antapp

import com.codename1.annotations.rest.GET
import com.codename1.annotations.rest.Path
import com.codename1.annotations.rest.RestClient
import com.codename1.io.rest.Response
import com.codename1.util.OnComplete

@RestClient
interface PetApi {
    @GET("/pet/{petId}")
    fun getPetById(@Path("petId") petId: Long?, callback: OnComplete<Response<Pet>>)
}
EOF
cat > "$ANT/src/com/acme/antapp/MyNative.java" <<'EOF'
package com.acme.antapp;

import com.codename1.system.NativeInterface;

public interface MyNative extends NativeInterface {
    int answer();
}
EOF
cat > "$ANT/native/android/com/acme/antapp/MyNativeImpl.java" <<'EOF'
package com.acme.antapp;

public class MyNativeImpl {
    public int answer() {
        return 42;
    }

    public boolean isSupported() {
        return true;
    }
}
EOF
echo "// ios" > "$ANT/native/ios/com_acme_antapp_MyNativeImpl.m"
echo "stale" > "$ANT/src/theme.res"
# A Kotlin unit test: Gradle compiles it apart from the Java tests, and cn1Test
# must still find it.
mkdir -p "$ANT/test/com/acme/antapp"
cat > "$ANT/test/com/acme/antapp/HelperTest.kt" <<'EOF'
package com.acme.antapp

import com.codename1.testing.AbstractTest

class HelperTest : AbstractTest() {
    override fun runTest(): Boolean = Helper().title() == "Converted"
}
EOF
echo "Form { color: red; }" > "$ANT/css/theme.css"

convert "$ANT" "$WORKDIR/AntApp-gradle" "$WORKDIR/convert-ant.log" || { cat "$WORKDIR/convert-ant.log"; fail "converting the Ant project"; }
AG="$WORKDIR/AntApp-gradle"
use_local_plugin "$AG"
[ -f "$AG/src/main/kotlin/com/acme/antapp/Helper.kt" ] || fail "Kotlin sources belong in src/main/kotlin"
[ -f "$AG/src/android/java/com/acme/antapp/MyNativeImpl.java" ] || fail "native/android was not carried over"
[ -f "$AG/src/ios/objectivec/com_acme_antapp_MyNativeImpl.m" ] || fail "native/ios was not carried over"
[ ! -e "$AG/src/main/resources/theme.res" ] || fail "the saved theme.res was carried over"
grep -q "kotlin(\"jvm\")" "$AG/build.gradle.kts" || fail "a Kotlin project needs the Kotlin plugin"
grep -q "codename1.arg.java.version=17" "$AG/codenameone_settings.properties" || fail "the target was not raised to 17"
grep -q "Gradle projects compile for Java 17" "$WORKDIR/convert-ant.log" || fail "raising the target was not reported"

run_gradle "$AG" classes cn1Css > "$WORKDIR/ant-classes.log" 2>&1 || { cat "$WORKDIR/ant-classes.log"; fail "the converted Ant project does not build"; }
run_gradle "$AG" buildAndroid -Pcodename1.stageOnly=true > "$WORKDIR/ant-android.log" 2>&1 \
  || { cat "$WORKDIR/ant-android.log"; fail "the converted Ant project does not stage"; }
JAR=$(sed -n 's/.*codename1.stageOnly is set: staged \(.*\) ([0-9]* bytes) for .*/\1/p' "$WORKDIR/ant-android.log" | tail -1)
assert_zip_has "$JAR" "^com/acme/antapp/AntApp\.class$"
assert_zip_has "$JAR" "^com/acme/antapp/Helper\.class$"
assert_zip_has "$JAR" "^com/acme/antapp/PetApiImpl\.class$"
assert_zip_has "$JAR" "^com/acme/antapp/MyNativeImpl\.java$"
run_gradle "$AG" cn1Test > "$WORKDIR/ant-test.log" 2>&1 || { cat "$WORKDIR/ant-test.log"; fail "cn1Test on the converted Ant project"; }
grep -q "com.acme.antapp.HelperTest passed" "$WORKDIR/ant-test.log" \
  || { cat "$WORKDIR/ant-test.log"; fail "cn1Test did not run the Kotlin test"; }
echo "   Ant app converted, built with its Kotlin, staged, and its Kotlin test run"

echo "== Maven to Gradle, with a backend of its own"
rm -rf mvnapp
mvn_local archetype:generate -DarchetypeArtifactId=cn1app-archetype -DarchetypeGroupId=com.codenameone \
  -DplatformModules=all -DprojectType=app-with-backend \
  -DarchetypeVersion="$CN1_VERSION" -DartifactId=mvnapp -DgroupId=com.acme.mvnapp -Dpackage=com.acme.mvnapp \
  -Dversion=1.0-SNAPSHOT -DmainName=MvnApp -DjavaVersion=17 -DinteractiveMode=false > "$WORKDIR/archetype.log" 2>&1 \
  || { tail -40 "$WORKDIR/archetype.log"; fail "archetype:generate"; }
API=$(find mvnapp/backend/src/main/java -name Api.java | head -1)
[ -n "$API" ] || fail "the archetype has no backend Api.java"
PKG=$(sed -n 's/^package \(.*\);/\1/p' "$API")
cat > "$(dirname "$API")/Orders.java" <<EOF
package $PKG;

import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.RestController;

@RestController
public class Orders {
    @GetMapping("/orders")
    public String list() {
        return "[]";
    }
}
EOF
convert "$WORKDIR/mvnapp" "$WORKDIR/mvnapp-gradle" "$WORKDIR/convert-mvn.log" \
  || { cat "$WORKDIR/convert-mvn.log"; fail "converting the Maven project"; }
MG="$WORKDIR/mvnapp-gradle"
use_local_plugin "$MG"
[ -f "$MG/backend/application.properties" ] || fail "a backend with code of its own must be converted"
find "$MG/backend/src" -name Orders.java | grep -q . || fail "the backend's own controller was lost"
run_gradle "$MG" classes :backend:classes > "$WORKDIR/mvn-classes.log" 2>&1 \
  || { cat "$WORKDIR/mvn-classes.log"; fail "the converted Maven project does not build"; }
grep -q "generated 2 @RestController router" "$WORKDIR/mvn-classes.log" \
  || { cat "$WORKDIR/mvn-classes.log"; fail "the backend's controllers were not both processed"; }
echo "   Maven app converted with its backend subproject"

echo "== legacy .cn1lib is refused"
mkdir -p "$ANT/lib"
printf 'PK' > "$ANT/lib/OldMaps.cn1lib"
if convert "$ANT" "$WORKDIR/refused" "$WORKDIR/convert-refused.log"; then
  cat "$WORKDIR/convert-refused.log"
  fail "a project with a legacy .cn1lib was converted"
fi
grep -q "OldMaps.cn1lib" "$WORKDIR/convert-refused.log" || { cat "$WORKDIR/convert-refused.log"; fail "the refusal does not name the library"; }
[ ! -e "$WORKDIR/refused/settings.gradle.kts" ] || fail "a refused conversion wrote a project"
echo "   refused, naming OldMaps.cn1lib"

echo "gradle-migrate-test: OK"
