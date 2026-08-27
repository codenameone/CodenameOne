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

echo "gen-build-hint-table: building the catalog" >&2
(cd "$REPO_ROOT/maven" && mvn -q -B -pl build-hint-tools -am package -DskipTests)

CLASSES="$REPO_ROOT/maven/build-hint-tools/target/classes"
CATALOG_CLASSES="$REPO_ROOT/maven/build-hint-catalog/target/classes"

# The generator reads the annotations out of their compiled classes, so ASM has
# to be on ITS classpath. Provided scope keeps it off the Settings tool's, which
# is why it is not simply a compile dependency.
asm_cp() {
  local out
  out="$(mktemp)"
  (cd "$REPO_ROOT/maven" && mvn -q -B -pl build-hint-tools dependency:build-classpath \
      -Dmdep.outputFile="$out" >/dev/null 2>&1) || true
  if [ -s "$out" ]; then
    printf '%s' ":$(cat "$out")"
  fi
  rm -f "$out"
}

java -cp "$CLASSES:$CATALOG_CLASSES$(asm_cp)" com.codename1.build.shared.BuildHintCodeGenerator --table-only "$REPO_ROOT/CodenameOne/src" "$OUT"
echo "gen-build-hint-table: wrote $OUT" >&2
