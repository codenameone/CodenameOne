#!/usr/bin/env bash
#
# Fails when our own code relies on a failing cast throwing ClassCastException.
# ParparVM's CHECKCAST is unchecked, so such a handler never runs on iOS and the
# wrong object is used instead (issue #5531). See CastSemanticsVerifier.
#
#   scripts/check-cast-semantics.sh [--write-baseline] [class-dir-or-jar ...]
#
# With no paths, checks every module that is already built; a module that has not
# been compiled is skipped with a note rather than silently passing.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
BASELINE="$SCRIPT_DIR/cast-semantics-baseline.txt"
TRANSLATOR="$REPO_ROOT/vm/ByteCodeTranslator/target/classes"
ASM_CP_FILE="$REPO_ROOT/vm/ByteCodeTranslator/target/cast-semantics-asm-classpath.txt"

# The modules whose bytecode ParparVM translates and that we own. Ports/retro is
# excluded on purpose: it is vendored retroweaver code, not ours to restyle.
DEFAULT_ROOTS=(
  "vm/JavaAPI/target/classes"
  "maven/core/target/classes"
  "maven/android/target/classes"
  "maven/ios/target/classes"
)

write_baseline=0
roots=()
for arg in "$@"; do
  if [[ "$arg" == "--write-baseline" ]]; then
    write_baseline=1
  else
    roots+=("$arg")
  fi
done

if [[ ! -f "$TRANSLATOR/com/codename1/tools/translator/CastSemanticsVerifier.class" ]]; then
  echo "check-cast-semantics: building the translator" >&2
  (cd "$REPO_ROOT/vm" && mvn -q -B -pl ByteCodeTranslator -am package -DskipTests)
fi
if [[ ! -f "$ASM_CP_FILE" ]]; then
  (cd "$REPO_ROOT/vm" && mvn -q -B -pl ByteCodeTranslator \
     dependency:build-classpath "-Dmdep.outputFile=target/cast-semantics-asm-classpath.txt")
fi

if [[ ${#roots[@]} -eq 0 ]]; then
  for candidate in "${DEFAULT_ROOTS[@]}"; do
    if [[ -d "$REPO_ROOT/$candidate" ]]; then
      roots+=("$REPO_ROOT/$candidate")
    else
      echo "check-cast-semantics: skipping $candidate (not built)" >&2
    fi
  done
fi

if [[ ${#roots[@]} -eq 0 ]]; then
  echo "check-cast-semantics: nothing to check -- no module is built" >&2
  exit 2
fi

args=()
if [[ "$write_baseline" -eq 1 ]]; then
  args+=(--write-baseline "$BASELINE")
fi
args+=(--baseline "$BASELINE")

exec java -cp "$TRANSLATOR:$(cat "$ASM_CP_FILE")" \
  com.codename1.tools.translator.CastSemanticsVerifier "${args[@]}" "${roots[@]}"
