#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
SRC="$ROOT/tools/src/main/java/com/codenameone/playground/tools/GenerateCN1AccessRegistry.java"
OUT="$ROOT/common/src/main/java/bsh/cn1/GeneratedCN1Access.java"
BUILD_DIR="$ROOT/target/cn1-access-tool"
POM="$ROOT/pom.xml"

read_cn1_version() {
  perl -0777 -ne 'if (m|<properties>.*?<cn1\.version>([^<]+)</cn1\.version>|s) { print $1; exit 0 }' "$POM"
}

CN1_VERSION="${CN1_VERSION:-$(read_cn1_version)}"
if [ -z "$CN1_VERSION" ]; then
  echo "Unable to determine cn1.version from $POM" >&2
  exit 1
fi

SOURCES_BASE="$BUILD_DIR/sources/$CN1_VERSION"
mkdir -p "$SOURCES_BASE"

download_source_jar() {
  artifact="$1"
  jar="$SOURCES_BASE/${artifact}-${CN1_VERSION}-sources.jar"
  if [ ! -f "$jar" ]; then
    url="https://repo.maven.apache.org/maven2/com/codenameone/${artifact}/${CN1_VERSION}/${artifact}-${CN1_VERSION}-sources.jar"
    curl -fsSL "$url" -o "$jar"
  fi
  dest="$SOURCES_BASE/$artifact"
  mkdir -p "$dest"
  unzip -q -o "$jar" -d "$dest"
  printf '%s' "$dest"
}

CORE_SRC_DIR="$(download_source_jar codenameone-core)"
RUNTIME_SRC_DIR="$(download_source_jar java-runtime)"
JAVASE_SRC_DIR="$(download_source_jar codenameone-javase)"
CN1_SOURCE_ROOTS="${CORE_SRC_DIR}:${RUNTIME_SRC_DIR}:${JAVASE_SRC_DIR}"

mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" "$SRC"
CN1_SOURCE_ROOTS="$CN1_SOURCE_ROOTS" java -cp "$BUILD_DIR" com.codenameone.playground.tools.GenerateCN1AccessRegistry "$OUT"
