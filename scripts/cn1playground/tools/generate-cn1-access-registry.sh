#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
SRC="$ROOT/tools/src/main/java/com/codenameone/playground/tools/GenerateCN1AccessRegistry.java"
OUT="$ROOT/common/src/main/java/bsh/cn1/GeneratedCN1Access.java"
BUILD_DIR="$ROOT/target/cn1-access-tool"

mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" "$SRC"
java -cp "$BUILD_DIR" com.codenameone.playground.tools.GenerateCN1AccessRegistry "$OUT"
