#!/bin/bash
# Exercise the real Cn1BuildTask with Kotlin and added source roots. Requires
# locally installed build-engine/Gradle-plugin artifacts and JDK 17+ for Gradle.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"

WORKDIR="$SCRIPTPATH/build/gradle-main-class-preflight"
APP="$WORKDIR/app"
rm -rf "$WORKDIR"
mkdir -p "$APP/gradle/wrapper" "$APP/src/main/java" "$APP/src/main/kotlin" \
  "$APP/generated/app" "$APP/extra/kotlin"
TEMPLATE="$SCRIPTPATH/../build-engine/src/main/resources/com/codename1/project/templates/gradle"
cp "$TEMPLATE/gradlew" "$APP/gradlew"
cp "$TEMPLATE/gradle/wrapper/"* "$APP/gradle/wrapper/"
chmod +x "$APP/gradlew"
cat > "$APP/settings.gradle.kts" <<EOF_SETTINGS
pluginManagement {
    repositories {
        maven("https://repo.codenameone.com/maven2")
        gradlePluginPortal()
    }
}
plugins { id("com.codenameone") version "$CN1_VERSION" }
rootProject.name = "preflight"
EOF_SETTINGS
use_local_plugin "$APP"
cat > "$APP/codenameone_settings.properties" <<'EOF_SETTINGS'
codename1.packageName=example
codename1.mainName=CustomMain
codename1.displayName=Preflight
codename1.version=1.0
codename1.vendor=Tests
codename1.arg.java.version=17
EOF_SETTINGS
cat > "$APP/build.gradle.kts" <<'EOF_BUILD'
plugins { kotlin("jvm") version "2.2.10" }
// Realize the build task before adding roots: its source list must stay lazy.
tasks.named("buildAndroid").get()
sourceSets.named("main") { java.srcDir("generated/app") }
kotlin.sourceSets.named("main") { kotlin.srcDir("extra/kotlin") }
EOF_BUILD
cat > "$APP/generated/app/CustomMain.java" <<'EOF_JAVA'
package example;
public class CustomMain {
    public void init(Object context) {}
    public void start() {}
    public void stop() {}
    public void destroy() {}
}
EOF_JAVA
cat > "$APP/src/main/kotlin/Entry.kt" <<'EOF_KOTLIN'
package example
class KotlinMain {
    fun init(context: Any?) {}
    fun start() {}
    fun stop() {}
    fun destroy() {}
}
EOF_KOTLIN
sed 's/KotlinMain/GeneratedMain/' "$APP/src/main/kotlin/Entry.kt" > "$APP/extra/kotlin/Generated.kt"

# Repeat identical properties consecutively. Gradle 8.5 invalidates the previous
# configuration-cache entry when -Pcodename1.mainName changes; newer Gradle
# versions can retain multiple entries, but the test must work with either.
for main in CustomMain CustomMain KotlinMain GeneratedMain; do
  log="$WORKDIR/$main.log"
  run_gradle "$APP" buildAndroid --configuration-cache -Pcodename1.stageOnly=true \
    "-Pcodename1.mainName=$main" > "$log" 2>&1 \
    || { cat "$log"; fail "staging $main from its source root"; }
  jar=$(sed -n 's/.*codename1.stageOnly is set: staged \(.*\) ([0-9]* bytes) for .*/\1/p' "$log" | tail -1)
  [ -f "$jar" ] || fail "$main produced no staged jar"
  assert_zip_has "$jar" "^example/$main.class$"
done
grep -q "Configuration cache entry reused" "$WORKDIR/CustomMain.log" \
  || { cat "$WORKDIR/CustomMain.log"; fail "source roots did not survive configuration-cache reuse"; }

# Retain compiled output to prove that supplying all roots still rejects a real orphan.
rm "$APP/generated/app/CustomMain.java"
if run_gradle "$APP" buildAndroid --configuration-cache -x compileJava -x compileKotlin \
    -Pcodename1.stageOnly=true -Pcodename1.mainName=CustomMain > "$WORKDIR/orphan.log" 2>&1; then
  fail "an orphaned main class was accepted"
fi
grep -q "compiled class is stale" "$WORKDIR/orphan.log" \
  || { cat "$WORKDIR/orphan.log"; fail "the failure did not identify the stale main class"; }
echo "PASS: Gradle main-class preflight accepts Kotlin/added roots, reuses configuration cache, and rejects orphans"
