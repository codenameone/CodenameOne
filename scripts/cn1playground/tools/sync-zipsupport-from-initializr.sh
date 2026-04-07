#!/usr/bin/env bash
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
INITIALIZR_CN1LIBS_DIR="$ROOT_DIR/../initializr/cn1libs"
TARGET_CN1LIBS_DIR="$ROOT_DIR/cn1libs"

if [ ! -f "$INITIALIZR_CN1LIBS_DIR/ZipSupport.cn1lib" ]; then
  echo "Missing source file: $INITIALIZR_CN1LIBS_DIR/ZipSupport.cn1lib" >&2
  exit 1
fi
if [ ! -f "$INITIALIZR_CN1LIBS_DIR/ZipSupport.ver" ]; then
  echo "Missing source file: $INITIALIZR_CN1LIBS_DIR/ZipSupport.ver" >&2
  exit 1
fi
if [ ! -f "$INITIALIZR_CN1LIBS_DIR/ZipSupport/jars/main.zip" ]; then
  echo "Missing source file: $INITIALIZR_CN1LIBS_DIR/ZipSupport/jars/main.zip" >&2
  exit 1
fi

mkdir -p "$TARGET_CN1LIBS_DIR/ZipSupport/jars"
cp "$INITIALIZR_CN1LIBS_DIR/ZipSupport.cn1lib" "$TARGET_CN1LIBS_DIR/ZipSupport.cn1lib"
cp "$INITIALIZR_CN1LIBS_DIR/ZipSupport.ver" "$TARGET_CN1LIBS_DIR/ZipSupport.ver"
cp "$INITIALIZR_CN1LIBS_DIR/ZipSupport/jars/main.zip" "$TARGET_CN1LIBS_DIR/ZipSupport/jars/main.zip"

echo "Synced ZipSupport assets from scripts/initializr/cn1libs"
