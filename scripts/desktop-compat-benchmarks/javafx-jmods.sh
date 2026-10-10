#!/usr/bin/env bash
#
# Builds OpenJFX jmods for this machine from the platform jars on Maven Central.
#
# Usage: javafx-jmods.sh <jdk-home> <output-dir> [version]
#
# Each OpenJFX platform jar is a module's classes with its native libraries at
# the root, and a jmod is the same two things in separate sections. Linked from
# jmods, a jlink runtime image carries the libraries in lib/ like a JDK's own,
# instead of unpacking them into ~/.openjfx/cache on first start the way an
# image linked from the jars does -- which is the layout a packaged JavaFX
# application normally ships, and the fairer one to time.
#
# The default version is the newest OpenJFX that both runs on JDK 21 and
# publishes a linux-aarch64 build.
set -euo pipefail

JDK_HOME="${1:?usage: javafx-jmods.sh <jdk-home> <output-dir> [version]}"
OUT="${2:?usage: javafx-jmods.sh <jdk-home> <output-dir> [version]}"
VERSION="${3:-${BENCH_JAVAFX_VERSION:-23.0.2}}"

if [ -f "$OUT/VERSION" ] && [ "$(cat "$OUT/VERSION")" = "$VERSION" ]; then
  exit 0
fi

case "$(uname -s)-$(uname -m)" in
  Linux-aarch64|Linux-arm64) CLASSIFIER="linux-aarch64"; LIB_GLOB='*.so'; SECTION="--libs" ;;
  Linux-*) CLASSIFIER="linux"; LIB_GLOB='*.so'; SECTION="--libs" ;;
  Darwin-arm64) CLASSIFIER="mac-aarch64"; LIB_GLOB='*.dylib'; SECTION="--libs" ;;
  Darwin-*) CLASSIFIER="mac"; LIB_GLOB='*.dylib'; SECTION="--libs" ;;
  # A Windows runtime image keeps its DLLs in bin/, the "cmds" section of a
  # jmod. Only CI has run this branch.
  MINGW*|MSYS*|CYGWIN*) CLASSIFIER="win"; LIB_GLOB='*.dll'; SECTION="--cmds" ;;
  *) echo "javafx-jmods.sh: unsupported platform $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

STAGE="$OUT.stage"
rm -rf "$STAGE" "$OUT"
mkdir -p "$STAGE/jmods"
for MODULE in base graphics controls fxml media swing; do
  JAR="$STAGE/javafx-$MODULE.jar"
  curl -fsSL --retry 3 -o "$JAR" \
    "https://repo1.maven.org/maven2/org/openjfx/javafx-$MODULE/$VERSION/javafx-$MODULE-$VERSION-$CLASSIFIER.jar"
  mkdir -p "$STAGE/$MODULE/classes" "$STAGE/$MODULE/libs"
  unzip -q "$JAR" -d "$STAGE/$MODULE/classes"
  find "$STAGE/$MODULE/classes" -maxdepth 1 -name "$LIB_GLOB" -exec mv {} "$STAGE/$MODULE/libs/" \;
  rm -rf "$STAGE/$MODULE/classes/META-INF"
  "$JDK_HOME/bin/jmod" create --class-path "$STAGE/$MODULE/classes" "$SECTION" "$STAGE/$MODULE/libs" \
    --module-version "$VERSION" "$STAGE/jmods/javafx.$MODULE.jmod"
done
mv "$STAGE/jmods" "$OUT"
rm -rf "$STAGE"
echo "$VERSION" > "$OUT/VERSION"
