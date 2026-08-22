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
ANN_ROOT="$REPO_ROOT/CodenameOne/src"
CATALOG_SRC="$CATALOG/src/main/java"

check=0
[ "${1:-}" = "--check" ] && check=1

if [ ! -f "$CLASSES/com/codename1/build/shared/BuildHintCodeGenerator.class" ]; then
  echo "gen-build-hint-annotations: building the catalog" >&2
  (cd "$REPO_ROOT/maven" && mvn -q -B -pl build-hint-catalog package -DskipTests)
fi

SKILL_REF="$REPO_ROOT/scripts/initializr/common/src/main/resources/skill/references/build-hints.md"
JAVASE_SRC="$REPO_ROOT/Ports/JavaSE/src"
GUIDE_TABLE="$REPO_ROOT/docs/developer-guide/_generated-build-hints.adoc"

java -cp "$CLASSES" com.codename1.build.shared.BuildHintCodeGenerator \
     "$ANN_ROOT" "$CATALOG_SRC" "$SKILL_REF" "$JAVASE_SRC" "$GUIDE_TABLE"

if [ "$check" -eq 1 ]; then
  targets=("CodenameOne/src/com/codename1/annotations/buildhints"
           "maven/build-hint-catalog/src/main/java/com/codename1/build/shared/BuildHintAnnotationBinding.java"
           "scripts/initializr/common/src/main/resources/skill/references/build-hints.md"
           "Ports/JavaSE/src/com/codename1/impl/javase/BuildHintCatalogDefaults.java"
           "docs/developer-guide/_generated-build-hints.adoc")
  if ! git -C "$REPO_ROOT" diff --quiet -- "${targets[@]}" \
     || [ -n "$(git -C "$REPO_ROOT" ls-files --others --exclude-standard -- "${targets[@]}")" ]; then
    echo "::error::Generated build hint annotations are out of date." >&2
    echo "Run scripts/gen-build-hint-annotations.sh and commit the result." >&2
    git -C "$REPO_ROOT" --no-pager diff -- "${targets[@]}" >&2 || true
    git -C "$REPO_ROOT" ls-files --others --exclude-standard -- "${targets[@]}" >&2 || true
    exit 1
  fi
  echo "gen-build-hint-annotations: generated sources are up to date"
fi
