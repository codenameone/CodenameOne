#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
BUILD_DIR="$SCRIPT_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
JAR_PATH="$SCRIPT_DIR/cn1ss-tools.jar"
mkdir -p "$BUILD_DIR"
rm -rf "$CLASSES_DIR"
mkdir -p "$CLASSES_DIR"
find "$SRC_DIR" -name '*.java' >"$BUILD_DIR/sources.list"
if [ ! -s "$BUILD_DIR/sources.list" ]; then
  echo "No Java sources found in $SRC_DIR" >&2
  exit 1
fi
javac -encoding UTF-8 -d "$CLASSES_DIR" @"$BUILD_DIR/sources.list"
jar --create --file "$JAR_PATH" -C "$CLASSES_DIR" .
