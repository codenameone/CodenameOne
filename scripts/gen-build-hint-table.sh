#!/bin/bash
# Renders the developer guide's build hint table from the catalog.
#
# The table is NOT checked in. It is generated from maven/build-hint-catalog
# every time the guide is rendered, so it cannot drift from the catalog and a
# hand edit has nothing to survive in -- which is what a generated file that
# lives in git always eventually invites.
#
# Both renderers call this first: the developer-guide-docs workflow and
# scripts/website/build.sh. Asciidoctor resolves the include relative to the
# including document, so the file has to land beside it.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-$REPO_ROOT/docs/developer-guide/_generated-build-hints.adoc}"

CLASSES="$REPO_ROOT/maven/build-hint-tools/target/classes"
CATALOG_CLASSES="$REPO_ROOT/maven/build-hint-catalog/target/classes"

# The generator reads the annotations out of their compiled classes, so ASM has
# to be on ITS classpath. Built in the SAME reactor invocation as the module, so
# the catalog resolves from the reactor rather than from the local repository --
# a separate `dependency:build-classpath -pl build-hint-tools` cannot see a
# sibling that has only been packaged, which on CI produced an empty classpath
# and a NoClassDefFoundError for org/objectweb/asm/ClassVisitor.
#
# Not silenced: an unresolvable classpath used to be swallowed by `|| true`, so
# the failure surfaced as a missing class much later instead of here.
build_and_classpath() {
  local out="$1"
  (cd "$REPO_ROOT/maven" && mvn -q -B -pl build-hint-tools -am package \
      dependency:build-classpath -DskipTests -Dmdep.outputFile="$out")
  if [ ! -s "$out" ]; then
    echo "could not resolve the build hint tools classpath" >&2
    exit 1
  fi
}

echo "gen-build-hint-table: building the catalog" >&2
CP_FILE="$(mktemp)"
build_and_classpath "$CP_FILE"

java -cp "$CLASSES:$CATALOG_CLASSES:$(cat "$CP_FILE")" com.codename1.build.shared.BuildHintCodeGenerator --table-only "$REPO_ROOT/CodenameOne/src" "$OUT"
echo "gen-build-hint-table: wrote $OUT" >&2
