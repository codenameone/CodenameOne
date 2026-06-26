#!/bin/bash
# Compiles the Java host for the iOS native simulator backend:
# IOSDesktopImplementation + CN1SimHost + IOSSimulatorBackend, pulling the
# iOS port Java sources transitively via -sourcepath. Produces
# maven/cn1-sim-native/target/cn1-sim-native-host.jar
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MODULE="$REPO_ROOT/maven/cn1-sim-native"
OUT="$MODULE/target/host-classes"
mkdir -p "$OUT"

CORE_JAR="$HOME/.m2/repository/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar"
if [ -d "$REPO_ROOT/maven/core/target/classes" ]; then
    CORE_JAR="$REPO_ROOT/maven/core/target/classes"
fi
JAVASE_JAR="$REPO_ROOT/Ports/JavaSE/dist/JavaSE.jar"
if [ ! -f "$JAVASE_JAR" ]; then
    JAVASE_JAR="$HOME/.m2/repository/com/codenameone/codenameone-javase/8.0-SNAPSHOT/codenameone-javase-8.0-SNAPSHOT.jar"
fi

JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
"$JAVAC" -nowarn -Xprefer:source \
    -cp "$CORE_JAR:$JAVASE_JAR" \
    -sourcepath "$REPO_ROOT/Ports/iOSPort/src:$MODULE/host/src" \
    -d "$OUT" \
    "$MODULE"/host/src/com/codename1/impl/ios/sim/*.java \
    "$MODULE"/host/src/com/codename1/impl/ios/sim/bridge/*.java \
    "$MODULE"/host/src/com/codename1/impl/ios/sim/child/*.java \
    "$MODULE"/host/src/com/codename1/impl/ios/sim/shell/*.java \
    "$MODULE"/host/src/com/codename1/impl/ios/*.java

cp -R "$MODULE/host/resources/" "$OUT/"
JAR="${JAVA_HOME:+$JAVA_HOME/bin/}jar"
"$JAR" cf "$MODULE/target/cn1-sim-native-host.jar" -C "$OUT" .
echo "[build-host] Built $MODULE/target/cn1-sim-native-host.jar"
