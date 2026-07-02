#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
ANDROID_HOME_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"

if [ -z "$ANDROID_HOME_DIR" ]; then
  if [ -d "$HOME/Library/Android/sdk" ]; then
    ANDROID_HOME_DIR="$HOME/Library/Android/sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then
    ANDROID_HOME_DIR="$HOME/Android/Sdk"
  fi
fi

if [ -z "$ANDROID_HOME_DIR" ] || [ ! -d "$ANDROID_HOME_DIR/platforms" ]; then
  echo "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT." >&2
  exit 1
fi

ANDROID_JAR="$(find "$ANDROID_HOME_DIR/platforms" -maxdepth 2 -name android.jar | sort -V | tail -n 1)"
if [ -z "$ANDROID_JAR" ]; then
  echo "No android.jar found under $ANDROID_HOME_DIR/platforms." >&2
  exit 1
fi

CORE_JAR="$HOME/.m2/repository/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar"
if [ ! -f "$CORE_JAR" ] && [ -f "$ROOT_DIR/maven/core/target/codenameone-core-8.0-SNAPSHOT.jar" ]; then
  CORE_JAR="$ROOT_DIR/maven/core/target/codenameone-core-8.0-SNAPSHOT.jar"
fi

ANDROID_CN1_JAR="$ROOT_DIR/maven/android/target/codenameone-android-8.0-SNAPSHOT-with-android-dependencies.jar"
if [ ! -f "$ANDROID_CN1_JAR" ]; then
  ANDROID_CN1_JAR="$HOME/.m2/repository/com/codenameone/codenameone-android/8.0-SNAPSHOT/codenameone-android-8.0-SNAPSHOT-with-android-dependencies.jar"
fi
COMMON_CLASSES="$ROOT_DIR/docs/demos/common/target/classes"

for required in "$CORE_JAR" "$ANDROID_CN1_JAR"; do
  if [ ! -f "$required" ]; then
    echo "Required Codename One jar not found: $required" >&2
    echo "Run the local Codename One Maven artifact install before compiling Android demo sources." >&2
    exit 1
  fi
done

if [ ! -d "$COMMON_CLASSES" ]; then
  echo "Common demo classes not found: $COMMON_CLASSES" >&2
  echo "Run docs/demos JavaSE test/compile before compiling Android demo sources." >&2
  exit 1
fi

WORK_DIR="${TMPDIR:-/tmp}/cn1-docs-android-compile"
CLASSES_DIR="$WORK_DIR/classes"
SOURCES_FILE="$WORK_DIR/android-sources.txt"

rm -rf "$WORK_DIR"
mkdir -p "$CLASSES_DIR"
find "$ROOT_DIR/docs/demos/android/src/main/java" -name '*.java' -print | sort > "$SOURCES_FILE"

javac \
  --release 17 \
  -cp "$ANDROID_JAR:$COMMON_CLASSES:$CORE_JAR:$ANDROID_CN1_JAR" \
  -d "$CLASSES_DIR" \
  @"$SOURCES_FILE"
