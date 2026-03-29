#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
SRC="$ROOT/tools/src/main/java/com/codenameone/playground/tools/GenerateCN1AccessRegistry.java"
OUT="$ROOT/common/src/main/java/bsh/cn1/GeneratedCN1Access.java"
BUILD_DIR="$ROOT/target/cn1-access-tool"

mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" "$SRC"

resolve_runtime_classpath() {
  if [ -n "${CN1_RUNTIME_CLASSPATH:-}" ]; then
    printf "%s" "$CN1_RUNTIME_CLASSPATH"
    return
  fi

  latest_core=""
  if [ -d "$HOME/.m2/repository/com/codenameone/codenameone-core" ]; then
    latest_core="$(find "$HOME/.m2/repository/com/codenameone/codenameone-core" -name 'codenameone-core-*.jar' | sort | tail -n 1)"
  fi
  if [ -n "$latest_core" ]; then
    printf "%s" "$latest_core"
    return
  fi

  updates_core=""
  if [ -d "$HOME/.codenameone" ]; then
    updates_core="$(find "$HOME/.codenameone" -name 'CodenameOne.jar' | sort | tail -n 1)"
  fi
  if [ -n "$updates_core" ]; then
    printf "%s" "$updates_core"
  fi
}

RUNTIME_CP="$(resolve_runtime_classpath)"
TOOL_CP="$BUILD_DIR"
if [ -n "$RUNTIME_CP" ]; then
  TOOL_CP="$TOOL_CP:$RUNTIME_CP"
fi

echo "Using runtime validation classpath: ${RUNTIME_CP:-<none>}" >&2
JAVA_OPTS=""
if [ -n "$RUNTIME_CP" ]; then
  JAVA_OPTS="-Dcn1playground.validateCn1Runtime=true"
fi
java $JAVA_OPTS -cp "$TOOL_CP" com.codenameone.playground.tools.GenerateCN1AccessRegistry "$OUT"
