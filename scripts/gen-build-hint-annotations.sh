#!/usr/bin/env bash
#
# Regenerates the build hint annotations in
# CodenameOne/src/com/codename1/annotations/buildhints from the catalog in
# maven/build-hint-catalog, along with the BuildHintAnnotationBinding table the
# annotation processor reads back.
#
#   scripts/gen-build-hint-annotations.sh            # write into the tree
#   scripts/gen-build-hint-annotations.sh --check    # fail if anything changed
#
# The output is checked in. CodenameOne/src is compiled by the Maven core
# module, the Ant/NetBeans project and the IDE projects alike, and only the
# first would see sources generated into target/ -- the others would quietly
# build a codenameone-core.jar without the annotations in it. Checking the
# sources in also means @Ios( completes in the IDE, which is the point.
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
CATALOG_SRC="$CATALOG/src/main/java"

check=0
[ "${1:-}" = "--check" ] && check=1

# Always rebuild. Skipping when the class merely exists meant that editing a
# BuildHints*.java source and rerunning this script regenerated every view from
# the previous build's bytecode -- reporting success while silently ignoring the
# edit, and in --check mode passing a tree that is genuinely out of date.
echo "gen-build-hint-annotations: building the catalog" >&2
(cd "$REPO_ROOT/maven" && mvn -q -B -pl build-hint-catalog package -DskipTests)

JAVASE_SRC="$REPO_ROOT/Ports/JavaSE/src"
GUIDE_TABLE="$REPO_ROOT/docs/developer-guide/_generated-build-hints.adoc"

java -cp "$CLASSES$(asm_cp)" com.codename1.build.shared.BuildHintCodeGenerator \
     "$ANN_ROOT" "$CATALOG_SRC" "$JAVASE_SRC" "$GUIDE_TABLE"

if [ "$check" -eq 1 ]; then
  # The developer guide's table is deliberately absent: it is not checked in, so
  # there is nothing for it to drift FROM. scripts/gen-build-hint-table.sh
  # renders it during the doc build instead, and this script still writes it for
  # a local asciidoctor run -- gitignored, so that copy is never reviewed.
  # NOT the annotation package. Those are hand-written -- they are the source of
  # truth for the hints they expose, and BuildHintAnnotationReader reads them
  # back rather than any file restating them. Policing them here would report a
  # deliberate edit as drift.
  targets=("maven/build-hint-catalog/src/main/java/com/codename1/build/shared/BuildHintAnnotationBinding.java"
           "maven/build-hint-catalog/src/main/java/com/codename1/build/shared/BuildHintsFromAnnotations.java"
           "Ports/JavaSE/src/com/codename1/impl/javase/BuildHintCatalogDefaults.java")
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
