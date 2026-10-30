#!/usr/bin/env bash
# Translates a Codename One application into the CMake project the Linux and
# embedded Linux targets build, without the build service: the same inputs
# LinuxNativeBuilder hands ParparVM (core, the Linux port, JavaAPI and the
# port's native sources), from this checkout's own build output.
#
#   scripts/embedded/translate.sh <app-classes-dir> <MainClass> <out-dir> [extra-source-dir ...]
#
# Needs maven/core, maven/linux, vm/JavaAPI and vm/ByteCodeTranslator built
# (see scripts/embedded/README.md). The result is <out-dir>/dist/CMakeLists.txt;
# build it on Linux, or in the cn1-embedded-dev container, with
# scripts/embedded/build-dist.sh.
set -euo pipefail

if [ $# -lt 3 ]; then
  sed -n '2,13p' "$0"
  exit 2
fi
APP_CLASSES=$(cd "$1" && pwd)
MAIN=$2
OUT=$3
shift 3
ROOT=$(cd "$(dirname "$0")/../.." && pwd)

CORE="$ROOT/maven/core/target/classes"
PORT="$ROOT/maven/linux/target/classes"
JAPI_JAR="$ROOT/vm/JavaAPI/target/JavaAPI-1.0-SNAPSHOT.jar"
TRANSLATOR="$ROOT/vm/ByteCodeTranslator/target/ByteCodeTranslator-1.0-SNAPSHOT.jar"
for f in "$CORE/com/codename1/ui/Form.class" "$PORT/com/codename1/impl/linux/LinuxImplementation.class" "$JAPI_JAR" "$TRANSLATOR"; do
  if [ ! -e "$f" ]; then
    echo "Missing $f -- build core, the Linux port, JavaAPI and the translator first." >&2
    exit 1
  fi
done

mkdir -p "$OUT"
OUT=$(cd "$OUT" && pwd)
STAGE="$OUT/stage"
rm -rf "$STAGE" "$OUT/dist"
mkdir -p "$STAGE/japi" "$STAGE/native" "$STAGE/res"
(cd "$STAGE/japi" && unzip -q -o "$JAPI_JAR" -x 'META-INF/*')
cp "$ROOT"/Ports/LinuxPort/nativeSources/* "$STAGE/native/"
if [ -f "$ROOT/CodenameOne/src/material-design-font.ttf" ]; then
  cp "$ROOT/CodenameOne/src/material-design-font.ttf" "$STAGE/res/"
fi

SOURCES="$APP_CLASSES;$CORE;$PORT;$STAGE/japi;$STAGE/native;$STAGE/res"
for extra in "$@"; do
  SOURCES="$SOURCES;$(cd "$extra" && pwd)"
done

JAVA=${JAVA_HOME:+$JAVA_HOME/bin/}java
"$JAVA" -Dcn1.sqlite=true -Dcn1.linux.embedded=${CN1_EMBEDDED:-true} -cp "$TRANSLATOR:$ROOT/vm/ByteCodeTranslator/src" \
  com.codename1.tools.translator.ByteCodeTranslator \
  clean "$SOURCES" "$OUT" "$MAIN" com.codename1.embedded.app "$MAIN" 1.0 linux none
echo "Translated to $OUT/dist"
