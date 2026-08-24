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
(cd "$REPO_ROOT/maven" && mvn -q -B -pl build-hint-catalog package -DskipTests)

CLASSES="$REPO_ROOT/maven/build-hint-catalog/target/classes"
java -cp "$CLASSES" com.codename1.build.shared.BuildHintCodeGenerator --table-only "$OUT"
echo "gen-build-hint-table: wrote $OUT" >&2
