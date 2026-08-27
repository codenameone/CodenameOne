#!/usr/bin/env bash
#
# Renders the build hint ANNOTATIONS into the forms that cannot read them.
#
# The annotations in CodenameOne/src/com/codename1/annotations/buildhints are
# hand-written and are the source; nothing here writes into that package. This
# produces cn1-build-hints.json for the editors that are Codename One apps and so
# have no bytecode reader, and the developer guide's table.
#
#   scripts/gen-build-hint-annotations.sh            # render into build outputs
#   scripts/gen-build-hint-annotations.sh --check    # fail if it cannot be built
#
# NOTHING here is checked in. Every module that needs the data file renders it
# into its own target/classes during its build (maven/javase,
# maven/codenameone-maven-plugin, scripts/settings/common), so there is no
# committed copy to conflict on a merge and nothing to drift from. This script
# exists for a local render and for the CI check below.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
CATALOG="$REPO_ROOT/maven/build-hint-catalog"
TOOLS="$REPO_ROOT/maven/build-hint-tools"
CLASSES="$TOOLS/target/classes"
CATALOG_CLASSES="$CATALOG/target/classes"

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

ANN_ROOT="$REPO_ROOT/CodenameOne/src"
# A scratch directory: no module reads from here, and nothing is committed.
CATALOG_DATA="$(mktemp -d)"

check=0
[ "${1:-}" = "--check" ] && check=1

# Always rebuild. Skipping when the class merely exists meant that editing a
# BuildHints*.java source and rerunning this script regenerated every view from
# the previous build's bytecode -- reporting success while silently ignoring the
# edit, and in --check mode passing a tree that is genuinely out of date.
echo "gen-build-hint-annotations: building the catalog" >&2
CP_FILE="$(mktemp)"
build_and_classpath "$CP_FILE"

GUIDE_TABLE="$REPO_ROOT/docs/developer-guide/_generated-build-hints.adoc"

java -cp "$CLASSES:$CATALOG_CLASSES:$(cat "$CP_FILE")" com.codename1.build.shared.BuildHintCodeGenerator \
     "$ANN_ROOT" "$CATALOG_DATA" "$GUIDE_TABLE"

if [ "$check" -eq 1 ]; then
  # The developer guide's table is deliberately absent: it is not checked in, so
  # there is nothing for it to drift FROM. scripts/gen-build-hint-table.sh
  # renders it during the doc build instead, and this script still writes it for
  # a local asciidoctor run -- gitignored, so that copy is never reviewed.
  # NOT the annotation package. Those are hand-written -- they are the source of
  # truth for the hints they expose, and BuildHintAnnotationReader reads them
  # back rather than any file restating them. There is no committed rendering of
  # them either, so there is nothing to diff: what this checks is that the
  # rendering still SUCCEEDS and is not empty, which is the failure that would
  # otherwise surface as an editor with no hints in it.
  if [ ! -s "$CATALOG_DATA/cn1-build-hints.json" ]; then
    echo "::error::The build hint data file could not be rendered." >&2
    exit 1
  fi
  hints="$(grep -c '"name":' "$CATALOG_DATA/cn1-build-hints.json" || true)"
  if [ "${hints:-0}" -lt 50 ]; then
    echo "::error::Only ${hints} annotated hints were rendered; the annotations are not being read." >&2
    exit 1
  fi
  echo "gen-build-hint-annotations: rendered ${hints} annotated hints"
fi
