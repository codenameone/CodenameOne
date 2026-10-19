#!/usr/bin/env bash
# Imports, transpiles and compiles every app under scripts/flutter-apps with no
# edits (see Acceptance.java). Needs JDK 17 as JAVA17_HOME (or java on PATH),
# `flutter` on PATH for `pub get`, and the runtime modules already built:
#   cd maven && mvn -B install -pl core,dart-runtime,flutter-runtime,dart-transpiler -am -DskipTests
# Usage: run-acceptance.sh [app-dir ...]   (default: every app here)
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
M="$ROOT/maven"
JAVA_BIN="${JAVA17_HOME:+$JAVA17_HOME/bin/}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

ANTLR="$(find "${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}/org/antlr/antlr4-runtime" -name 'antlr4-runtime-*.jar' | sort | tail -1)"
if [ -z "$ANTLR" ]; then
  echo "antlr4-runtime not found; build maven/dart-transpiler first" >&2
  exit 1
fi

# The importer is compiled from source: build-engine's Maven build drags in the
# whole JavaSE port, and the importer itself needs only project-model.
mkdir -p "$WORK/importer"
"${JAVA_BIN}javac" -nowarn -d "$WORK/importer" \
  -sourcepath "$M/build-engine/src/main/java:$M/project-model/src/main/java" \
  "$M/build-engine/src/main/java/com/codename1/maven/FlutterProjectImporter.java"

CP="$WORK/importer:$M/dart-transpiler/target/classes:$ANTLR"
RT="$M/core/target/classes:$M/dart-runtime/target/classes:$M/flutter-runtime/target/classes"

if [ "$#" -eq 0 ]; then
  set -- $(find "$HERE" -mindepth 2 -maxdepth 2 -name pubspec.yaml -exec dirname {} \; | sort)
fi

failed=0
for app in "$@"; do
  name="$(basename "$app")"
  echo "::group::$name"
  (cd "$app" && flutter pub get)
  if "${JAVA_BIN}java" -cp "$CP" "$HERE/Acceptance.java" "$app" "$WORK/$name" \
      "$M/flutter-runtime/target/classes" "$RT"; then
    echo "$name: ACCEPTED"
  else
    echo "::error::$name was not accepted"
    failed=1
  fi
  echo "::endgroup::"
done
exit "$failed"
