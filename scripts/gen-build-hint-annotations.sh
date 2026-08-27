#!/usr/bin/env bash
#
# Renders the build hint ANNOTATIONS into the forms that cannot read them.
#
# The annotations in CodenameOne/src/com/codename1/annotations/buildhints are
# hand-written and are the source; nothing here writes into that package. This
# produces cn1-build-hints.json for the two editors that are Codename One apps
# and so have no bytecode reader -- the Settings tool and the simulator's hint
# editor -- and the developer guide's table, which is not checked in.
#
#   scripts/gen-build-hint-annotations.sh            # write into the tree
#   scripts/gen-build-hint-annotations.sh --check    # fail if anything changed
#
# The data file is checked in because it ships inside those applications, and
# --check is what stops it drifting from the annotations beside it.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
CATALOG="$REPO_ROOT/maven/build-hint-catalog"
CLASSES="$CATALOG/target/classes"

# The generator reads the annotations out of their compiled classes, so ASM has
# to be on ITS classpath. Provided scope keeps it off the Settings tool's, which
# is why it is not simply a compile dependency.
asm_cp() {
  local out
  out="$(mktemp)"
  (cd "$REPO_ROOT/maven" && mvn -q -B -pl build-hint-catalog dependency:build-classpath \
      -Dmdep.includeScope=provided -Dmdep.outputFile="$out" >/dev/null 2>&1) || true
  if [ -s "$out" ]; then
    printf '%s' ":$(cat "$out")"
  fi
  rm -f "$out"
}

ANN_ROOT="$REPO_ROOT/CodenameOne/src"
CATALOG_DATA="$CATALOG/src/main/resources"

check=0
[ "${1:-}" = "--check" ] && check=1

# Always rebuild. Skipping when the class merely exists meant that editing a
# BuildHints*.java source and rerunning this script regenerated every view from
# the previous build's bytecode -- reporting success while silently ignoring the
# edit, and in --check mode passing a tree that is genuinely out of date.
echo "gen-build-hint-annotations: building the catalog" >&2
(cd "$REPO_ROOT/maven" && mvn -q -B -pl build-hint-catalog package -DskipTests)

# The simulator's copy of the data file. maven/javase does not depend on the
# catalog module, so it carries its own resource; both copies are written in
# this one run and both are checked below, so they cannot disagree.
JAVASE_DATA="$REPO_ROOT/maven/javase/src/main/resources"
GUIDE_TABLE="$REPO_ROOT/docs/developer-guide/_generated-build-hints.adoc"

java -cp "$CLASSES$(asm_cp)" com.codename1.build.shared.BuildHintCodeGenerator \
     "$ANN_ROOT" "$CATALOG_DATA" "$JAVASE_DATA" "$GUIDE_TABLE"

if [ "$check" -eq 1 ]; then
  # The developer guide's table is deliberately absent: it is not checked in, so
  # there is nothing for it to drift FROM. scripts/gen-build-hint-table.sh
  # renders it during the doc build instead, and this script still writes it for
  # a local asciidoctor run -- gitignored, so that copy is never reviewed.
  # NOT the annotation package. Those are hand-written -- they are the source of
  # truth for the hints they expose, and BuildHintAnnotationReader reads them
  # back rather than any file restating them. Policing them here would report a
  # deliberate edit as drift.
  # No generated Java at all: the processor reads the annotation package off the
  # classpath, and these two are the data file the editors read.
  targets=("maven/build-hint-catalog/src/main/resources/cn1-build-hints.json"
           "maven/javase/src/main/resources/cn1-build-hints.json")
  if ! git -C "$REPO_ROOT" diff --quiet -- "${targets[@]}" \
     || [ -n "$(git -C "$REPO_ROOT" ls-files --others --exclude-standard -- "${targets[@]}")" ]; then
    echo "::error::Generated build hint views are out of date." >&2
    echo "Run scripts/gen-build-hint-annotations.sh and commit the result." >&2
    git -C "$REPO_ROOT" --no-pager diff -- "${targets[@]}" >&2 || true
    git -C "$REPO_ROOT" ls-files --others --exclude-standard -- "${targets[@]}" >&2 || true
    exit 1
  fi
  echo "gen-build-hint-annotations: generated sources are up to date"
fi
