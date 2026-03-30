#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
CN1_DIR="$ROOT_DIR/CodenameOne"

JDK_HOME="${JDK_25_HOME:-${JAVA_HOME:-}}"
if [ -n "$JDK_HOME" ] && [ -x "$JDK_HOME/bin/javadoc" ]; then
  JAVADOC_CMD="$JDK_HOME/bin/javadoc"
else
  JAVADOC_CMD="javadoc"
fi

run_with_timeout() {
  local seconds="$1"
  shift
  if command -v timeout >/dev/null 2>&1; then
    timeout "${seconds}" "$@"
    return $?
  fi
  "$@"
}

rm -rf "$CN1_DIR/dist/javadoc"
rm -rf "$CN1_DIR/build/tempJavaSources"

mkdir -p "$CN1_DIR/build/tempJavaSources"
mkdir -p "$CN1_DIR/dist/javadoc"

cp -r "$CN1_DIR/src/"* "$CN1_DIR/build/tempJavaSources/"

VALIDATE_SNIPPETS_SCRIPT="${SCRIPT_DIR}/validate-extracted-javadoc-snippets.sh"
if [ -x "${VALIDATE_SNIPPETS_SCRIPT}" ]; then
  echo "Validating JavaDoc snippets prior to JavaDoc generation..." >&2
  if ! run_with_timeout 300 "${VALIDATE_SNIPPETS_SCRIPT}"; then
    status=$?
    if [ "${status}" -eq 124 ]; then
      echo "JavaDoc snippet validation timed out after 300 seconds." >&2
    fi
    exit "${status}"
  fi
fi

cat > "$CN1_DIR/build/tempJavaSources/com/codename1/impl/ImplementationFactory.java" <<'EOF'
package com.codename1.impl;

public class ImplementationFactory {
    public static ImplementationFactory getInstance() {
        return null;
    }

    public Object createImplementation() {
        return null;
    }
}
EOF

find "$CN1_DIR/build/tempJavaSources" "$ROOT_DIR/Ports/CLDC11/src" -name "*.java" \
  | /usr/bin/xargs "$JAVADOC_CMD" \
    --allow-script-in-comments \
    --add-stylesheet "$ROOT_DIR/maven/javadoc-resources/highlight.css" \
    --add-script "$ROOT_DIR/maven/javadoc-resources/highlight.min.js" \
    --add-script "$ROOT_DIR/maven/javadoc-resources/javadoc-highlight-init.js" \
    --release 8 \
    -exclude com.codename1.impl \
    -Xdoclint:none \
    -quiet \
    -protected \
    -d "$CN1_DIR/dist/javadoc" \
    -windowtitle "Codename One API" || true

(
  cd "$CN1_DIR/dist/javadoc"
  zip -r "$CN1_DIR/javadocs.zip" .
)
