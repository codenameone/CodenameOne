#!/bin/bash
# Compiles the Java host for the Windows native simulator backend into
# maven/cn1-sim-native/target/cn1-sim-native-win-host.jar (separate from the
# iOS host jar - both ports override core classes like com.codename1.ui.Accessor
# and must not share a compile).
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MODULE="$REPO_ROOT/maven/cn1-sim-native"
OUT="$MODULE/target/win-host-classes"
rm -rf "$OUT" && mkdir -p "$OUT"

CORE_JAR="$HOME/.m2/repository/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar"
if [ -d "$REPO_ROOT/maven/core/target/classes" ]; then
    CORE_JAR="$REPO_ROOT/maven/core/target/classes"
fi

JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
"$JAVAC" -nowarn -Xprefer:source \
    -cp "$CORE_JAR" \
    -sourcepath "$REPO_ROOT/Ports/WindowsPort/src:$MODULE/host/src" \
    -d "$OUT" \
    "$MODULE"/host/src/com/codename1/impl/windows/sim/*.java \
    "$MODULE"/host/src/com/codename1/impl/windows/*.java

JAR="${JAVA_HOME:+$JAVA_HOME/bin/}jar"
"$JAR" cf "$MODULE/target/cn1-sim-native-win-host.jar" -C "$OUT" .
echo "[build-win-host] Built $MODULE/target/cn1-sim-native-win-host.jar"
