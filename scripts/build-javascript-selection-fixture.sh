#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="${1:-$(mktemp -d "${TMPDIR:-/tmp}/cn1-selection-XXXXXX")}"
if [ -z "${JAVA_HOME:-}" ]; then
  if [ -x /usr/libexec/java_home ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"
  else
    JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
  fi
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
mvn -B -f "$ROOT/maven/pom.xml" -pl parparvm -am -DskipTests -Dmaven.javadoc.skip=true package
mkdir -p "$BUILD_DIR/classes"
BUILD_DIR="$(cd "$BUILD_DIR" && pwd)"
BUNDLE="$ROOT/maven/parparvm/target/bundle"
CORE="$ROOT/maven/core/target/codenameone-core-8.0-SNAPSHOT.jar"
(
  cd "$BUILD_DIR/classes"
  jar xf "$CORE"
  jar xf "$BUNDLE/parparvm-java-api.jar"
)
cp -R "$ROOT/maven/parparvm/target/javascript-port-classes"/. "$BUILD_DIR/classes/"
javac -encoding UTF-8 -source 8 -target 8 -cp "$CORE:$BUNDLE/JavaScriptPort.jar" \
  -d "$BUILD_DIR/classes" "$ROOT/Ports/JavaScriptPort/tests/fixtures/JavaScriptSelectionApp.java"
java -Dcodename1.javascriptport.webapp="$ROOT/Ports/JavaScriptPort/src/main/webapp" \
  -cp "$BUNDLE/parparvm-compiler.jar" com.codename1.tools.translator.ByteCodeTranslator \
  javascript "$BUILD_DIR/classes" "$BUILD_DIR/output" JavaScriptSelectionApp \
  com.codename1.jsfixture Selection 1.0 ios none
DIST="$BUILD_DIR/output/dist/JavaScriptSelectionApp-js"
mkdir -p "$DIST/assets"
cp "$BUILD_DIR/classes/material-design-font.ttf" "$DIST/assets/"
echo "$DIST"
