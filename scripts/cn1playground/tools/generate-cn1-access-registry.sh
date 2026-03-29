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
if [ -z "$RUNTIME_CP" ]; then
  echo "ERROR: No CN1 runtime classpath found." >&2
  echo "Set CN1_RUNTIME_CLASSPATH or install codenameone-core/CodenameOne.jar locally before regenerating." >&2
  exit 1
fi
TOOL_CP="$TOOL_CP:$RUNTIME_CP"

echo "Using runtime validation classpath: $RUNTIME_CP" >&2
JAVA_OPTS="-Dcn1playground.validateCn1Runtime=true"
java $JAVA_OPTS -cp "$TOOL_CP" com.codenameone.playground.tools.GenerateCN1AccessRegistry "$OUT"
