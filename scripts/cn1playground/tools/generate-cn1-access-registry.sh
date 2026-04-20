#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
SRC="$ROOT/tools/src/main/java/com/codenameone/playground/tools/GenerateCN1AccessRegistry.java"
OUT="$ROOT/common/src/main/java/bsh/cn1/GeneratedCN1Access.java"
BUILD_DIR="$ROOT/target/cn1-access-tool"
POM="$ROOT/pom.xml"
REPO_ROOT="$(CDPATH= cd -- "$ROOT/../.." && pwd)"
LOCAL_CORE_SRC="$REPO_ROOT/CodenameOne/src"
LOCAL_CLDC_SRC="$REPO_ROOT/Ports/CLDC11/src"

read_cn1_version() {
  perl -0777 -ne 'if (m|<properties>.*?<cn1\.version>([^<]+)</cn1\.version>|s) { print $1; exit 0 }' "$POM"
}

USE_LOCAL_SOURCES="${CN1_ACCESS_USE_LOCAL_SOURCES:-false}"

CN1_SOURCE_ROOTS_VALUE=""
if [ "$USE_LOCAL_SOURCES" = "true" ]; then
  if [ ! -d "$LOCAL_CORE_SRC" ] || [ ! -d "$LOCAL_CLDC_SRC" ]; then
    echo "Local source mode requested but expected roots are missing:" >&2
    echo "  $LOCAL_CORE_SRC" >&2
    echo "  $LOCAL_CLDC_SRC" >&2
    exit 1
  fi
  echo "Using local Codename One sources for registry generation." >&2
  CN1_SOURCE_ROOTS_VALUE="${LOCAL_CORE_SRC}:${LOCAL_CLDC_SRC}"
  # Use the locally-installed SNAPSHOT binary jars so runtime
  # validation (inherited-method filtering, generic erasure checks)
  # works the same as release-jar mode. Falls back gracefully if the
  # snapshot isn't installed.
  LOCAL_VERSION="${CN1_LOCAL_VERSION:-8.0-SNAPSHOT}"
  LOCAL_M2="$HOME/.m2/repository/com/codenameone"
  LOCAL_JARS=""
  for artifact in codenameone-core java-runtime codenameone-javase; do
    candidate="$LOCAL_M2/$artifact/$LOCAL_VERSION/${artifact}-${LOCAL_VERSION}.jar"
    if [ -f "$candidate" ]; then
      LOCAL_JARS="${LOCAL_JARS:+$LOCAL_JARS:}$candidate"
    fi
  done
  if [ -n "$LOCAL_JARS" ]; then
    CN1_BINARY_JARS="$LOCAL_JARS"
    echo "Runtime validation against local $LOCAL_VERSION binaries." >&2
  else
    echo "Warning: local $LOCAL_VERSION jars not found in $LOCAL_M2 — runtime validation will be skipped." >&2
  fi
else
  CN1_VERSION="${CN1_VERSION:-$(read_cn1_version)}"
  if [ -z "$CN1_VERSION" ]; then
    echo "Unable to determine cn1.version from $POM" >&2
    exit 1
  fi
  echo "Using release source jars for Codename One version: $CN1_VERSION" >&2

  SOURCES_BASE="$BUILD_DIR/sources/$CN1_VERSION"
  mkdir -p "$SOURCES_BASE"

  download_source_jar() {
    artifact="$1"
    jar="$SOURCES_BASE/${artifact}-${CN1_VERSION}-sources.jar"
    if [ ! -f "$jar" ]; then
      url="https://repo.maven.apache.org/maven2/com/codenameone/${artifact}/${CN1_VERSION}/${artifact}-${CN1_VERSION}-sources.jar"
      if ! curl -fsSL "$url" -o "$jar"; then
        echo "Failed to download source jar: $url" >&2
        exit 1
      fi
    fi
    dest="$SOURCES_BASE/$artifact"
    mkdir -p "$dest"
    if ! unzip -q -o "$jar" -d "$dest"; then
      echo "Failed to extract source jar: $jar" >&2
      exit 1
    fi
    printf '%s' "$dest"
  }

  CORE_SRC_DIR="$(download_source_jar codenameone-core)"
  RUNTIME_SRC_DIR="$(download_source_jar java-runtime)"
  JAVASE_SRC_DIR="$(download_source_jar codenameone-javase)"
  CN1_SOURCE_ROOTS_VALUE="${CORE_SRC_DIR}:${RUNTIME_SRC_DIR}:${JAVASE_SRC_DIR}"

  # Download the matching binary jars too. Having them on the generator's
  # runtime classpath lets it reflectively validate inherited methods on CN1
  # types (e.g. com.codename1.io.Properties extends HashMap<String,String>),
  # which is otherwise skipped when the runtime class can't be loaded.
  download_jar() {
    artifact="$1"
    jar="$SOURCES_BASE/${artifact}-${CN1_VERSION}.jar"
    if [ ! -f "$jar" ]; then
      url="https://repo.maven.apache.org/maven2/com/codenameone/${artifact}/${CN1_VERSION}/${artifact}-${CN1_VERSION}.jar"
      if ! curl -fsSL "$url" -o "$jar"; then
        echo "Failed to download jar: $url" >&2
        exit 1
      fi
    fi
    printf '%s' "$jar"
  }
  CN1_BINARY_JARS="$(download_jar codenameone-core):$(download_jar java-runtime):$(download_jar codenameone-javase)"
fi

mkdir -p "$BUILD_DIR"
TOOLS_JAR="${JAVA_HOME:-}/lib/tools.jar"
EXTRA_CP=""
if [ -f "$TOOLS_JAR" ]; then
  EXTRA_CP="$TOOLS_JAR"
fi

if [ -n "$EXTRA_CP" ]; then
  javac -cp "$EXTRA_CP" -d "$BUILD_DIR" "$SRC"
else
  javac -d "$BUILD_DIR" "$SRC"
fi

RUNTIME_CP="$BUILD_DIR"
if [ -n "$EXTRA_CP" ]; then
  RUNTIME_CP="$BUILD_DIR:$EXTRA_CP"
fi
if [ -n "${CN1_BINARY_JARS:-}" ]; then
  RUNTIME_CP="$RUNTIME_CP:$CN1_BINARY_JARS"
fi

if [ -n "$CN1_SOURCE_ROOTS_VALUE" ]; then
  CN1_SOURCE_ROOTS="$CN1_SOURCE_ROOTS_VALUE" java -cp "$RUNTIME_CP" com.codenameone.playground.tools.GenerateCN1AccessRegistry "$OUT"
else
  java -cp "$RUNTIME_CP" com.codenameone.playground.tools.GenerateCN1AccessRegistry "$OUT"
fi
