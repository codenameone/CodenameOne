#!/bin/bash
# Is this change actually a VM change, or did it just make the benchmark faster?
#
# The self-hosting benchmark measures the translator translating a corpus, so the
# program being measured IS the translator's own Java. That gives two very different
# ways to make the number go down:
#
#   1. Make the VM faster -- better generated C, a better runtime, a better
#      vm/JavaAPI. This is the goal, and it moves the ParparVM arm only.
#   2. Make the TRANSLATOR'S JAVA faster -- a cheaper collection, a cached string, a
#      tighter loop in the translator itself. This moves BOTH arms, because the JDK
#      arm runs that same Java. It cannot close the gap against HotSpot, and it
#      quietly changes the benchmark so absolute numbers stop comparing to earlier
#      sessions.
#
# This exists because the mistake was made: six edits to the translator's own
# collections and string handling, measured as a ~2% win, which never moved the ratio
# because the JDK arm got the same ~2%.
#
# WHY THIS DOES NOT MEASURE TIME. The obvious test is "did the JDK arm get faster",
# and that was tried first. It cannot work here: the mistake was worth about 2%, and
# this harness's own run-to-run spread on a shared machine is 4-9%, so the signal sits
# under the noise. Worse, an earlier timing version of this gate failed on two
# IDENTICAL class trees. A gate that fails on noise gets switched off.
#
# So the question is asked deterministically instead, and the answer is exact:
#
#   Translate one fixed corpus with the BASE translator and with THIS one. If the
#   emitted C is identical, and the copied C runtime is identical, then nothing about
#   the VM changed -- and a translator source edit that changes neither can only have
#   changed the benchmark.
#
# Usage: check-workload-neutral.sh <base-ref|base-classes-dir>
#   <base-ref>  git ref to compare against (default origin/master)
#               a DIRECTORY is taken as prebuilt base translator classes
#
# Exit 0 when the change touches the VM, 1 when it is workload-only, 2 when it could
# not run.
set -u

BASE_REF="${1:-origin/master}"
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
T="$HERE/target"
J8="${JDK_8_HOME:?set JDK_8_HOME to a working JDK 8}"

for d in "$T/classes" "$T/asm-classes" "$T/javaapi-classes" "$T/hello-corpus"; do
    [ -d "$d" ] || { echo "check-workload-neutral: missing $d -- run build-selfhost.sh first."; exit 2; }
done

WORK="$(mktemp -d)"
cleanup() {
    [ -d "$WORK/base" ] && git -C "$ROOT" worktree remove --force "$WORK/base" > /dev/null 2>&1
    rm -rf "$WORK"
}
trap cleanup EXIT

if [ -d "$BASE_REF" ]; then
    BASE_CLASSES="$(cd "$BASE_REF" && pwd)"
    BASE_IS_REF=0
    echo "check-workload-neutral: base is a prebuilt class tree"
else
    BASE_IS_REF=1
    echo "check-workload-neutral: building the translator at $BASE_REF"
    git -C "$ROOT" rev-parse --verify "$BASE_REF" > /dev/null 2>&1 \
        || { echo "  $BASE_REF does not resolve."; exit 2; }
    git -C "$ROOT" worktree add --detach "$WORK/base" "$BASE_REF" > "$WORK/wt.log" 2>&1 \
        || { echo "  could not create a worktree:"; sed 's/^/    /' "$WORK/wt.log"; exit 2; }
    # Its own repo-local .m2, never ~/.m2 and never a shared path: sibling checkouts
    # of this repo build concurrently and clobber each other's 8.0-SNAPSHOT.
    ( cd "$WORK/base/vm/ByteCodeTranslator" \
        && mvn -q -o -Dmaven.repo.local="$ROOT/.m2-repo" package -DskipTests ) > "$WORK/build.log" 2>&1 \
        || { echo "  the base translator did not build:"; tail -20 "$WORK/build.log" | sed 's/^/    /'; exit 2; }
    BASE_CLASSES="$WORK/base/vm/ByteCodeTranslator/target/classes"
    [ -d "$BASE_CLASSES" ] || { echo "  no classes at $BASE_CLASSES"; exit 2; }
fi

# Both sides translate the SAME corpus into the SAME absolute path, under the same
# constructed environment -- the generated CMakeLists embeds the output path and the
# translator reads knobs from getenv, so anything else shows up as a false difference.
translate() {
    local classes="$1"
    local side="$2"
    rm -rf "$WORK/out"
    mkdir -p "$WORK/out"
    ( cd "$WORK" && env -i PATH=/usr/bin:/bin HOME="$HOME" TMPDIR=/tmp LC_ALL=C \
        CN1_RESOURCE_PATH="$ROOT/vm/ByteCodeTranslator/src" \
        "$J8/bin/java" -cp "$classes:$T/asm-classes:$ROOT/vm/ByteCodeTranslator/src" \
        com.codename1.tools.translator.ByteCodeTranslator clean \
        "$T/javaapi-classes;$T/hello-corpus" "$WORK/out" \
        com_codenameone_examples_hellocodenameone_HelloCodenameOneStub \
        com.codenameone.examples.hellocodenameone \
        com_codenameone_examples_hellocodenameone_HelloCodenameOneStub 1.0 clean none \
        ) > "$WORK/$side.log" 2>&1 \
        || { echo "check-workload-neutral: the $side translation failed"; tail -12 "$WORK/$side.log" | sed 's/^/    /'; return 1; }
    mv "$WORK/out" "$WORK/$side-tree"
}

echo "check-workload-neutral: translating the hello corpus with each translator"
translate "$BASE_CLASSES" base || exit 2
translate "$T/classes" head || exit 2

BASE_SRC="$(find "$WORK/base-tree" -type d -name '*-src' | head -1)"
HEAD_SRC="$(find "$WORK/head-tree" -type d -name '*-src' | head -1)"
[ -n "$BASE_SRC" ] && [ -n "$HEAD_SRC" ] \
    || { echo "check-workload-neutral: no generated source tree in one of the outputs."; exit 2; }

# The copied C runtime is separated from the GENERATED classes, because they answer
# different halves of the question: a change to cn1_globals.h is a VM runtime change
# even when every generated class is byte-identical, and excluding those files (as
# the codegen-only comparison in verify-output-neutral.sh does, correctly, for its
# own purpose) would report such a change as workload-only.
RUNTIME_RE='^(cn1_globals\.[chm]|nativeMethods\.[cm]|cn1_intrinsics\.h|cn1_collections\.h|java_io_File_runtime\.c|cn1-source-manifest\.txt)$'

( cd "$BASE_SRC" && ls ) > "$WORK/b.list" 2>/dev/null || : > "$WORK/b.list"
( cd "$HEAD_SRC" && ls ) > "$WORK/h.list" 2>/dev/null || : > "$WORK/h.list"
sort -u "$WORK/b.list" "$WORK/h.list" > "$WORK/all.list"

GEN_DIFF=0
RT_DIFF=0
GEN_EXAMPLES=""
RT_EXAMPLES=""
while read -r f; do
    [ -n "$f" ] || continue
    case "$f" in *.c|*.h|*.m|*.txt) ;; *) continue ;; esac
    bf="$BASE_SRC/$f"
    hf="$HEAD_SRC/$f"
    if echo "$f" | grep -qE "$RUNTIME_RE"; then
        if [ ! -f "$bf" ] || [ ! -f "$hf" ] || ! cmp -s "$bf" "$hf"; then
            RT_DIFF=$((RT_DIFF+1))
            [ $RT_DIFF -le 6 ] && RT_EXAMPLES="$RT_EXAMPLES $f"
        fi
        continue
    fi
    if [ ! -f "$bf" ] || [ ! -f "$hf" ] || ! cmp -s "$bf" "$hf"; then
        GEN_DIFF=$((GEN_DIFF+1))
        [ $GEN_DIFF -le 6 ] && GEN_EXAMPLES="$GEN_EXAMPLES $f"
    fi
done < "$WORK/all.list"

TOTAL=$(wc -l < "$WORK/all.list" | tr -d ' ')
echo
echo "check-workload-neutral: $TOTAL emitted file(s) compared"
echo "  generated classes differing: $GEN_DIFF${GEN_EXAMPLES:+ --$GEN_EXAMPLES}"
echo "  copied C runtime differing:  $RT_DIFF${RT_EXAMPLES:+ --$RT_EXAMPLES}"

JAVAAPI=0
if [ "$BASE_IS_REF" = "1" ]; then
    JAVAAPI=$(git -C "$ROOT" diff --name-only "$BASE_REF" -- vm/JavaAPI 2>/dev/null | wc -l | tr -d ' ')
    echo "  vm/JavaAPI source files changed: $JAVAAPI"
fi

if [ "$GEN_DIFF" != "0" ]; then
    echo
    echo "check-workload-neutral: OK -- CODEGEN CHANGE. The C emitted for the same"
    echo "  corpus is different, so this change reaches the VM."
    exit 0
fi
if [ "$RT_DIFF" != "0" ]; then
    echo
    echo "check-workload-neutral: OK -- VM RUNTIME CHANGE. The emitted classes are"
    echo "  identical but the copied C runtime is not, so this change reaches the VM."
    exit 0
fi
if [ "$JAVAAPI" != "0" ]; then
    echo
    echo "check-workload-neutral: OK -- vm/JavaAPI CHANGE. Nothing emitted moved, but"
    echo "  the VM's own Java library did, and that is what the translated program"
    echo "  runs against. Note vm/JavaAPI is ALSO part of the corpus, so it perturbs"
    echo "  both arms' absolute times; the ratio is still the honest number."
    exit 0
fi

echo
echo "check-workload-neutral: WORKLOAD-ONLY -- nothing about the VM changed."
echo
echo "  Same corpus in, byte-identical C out, and an identical C runtime. Whatever"
echo "  changed, changed only how the TRANSLATOR ITSELF computes things -- which is"
echo "  the program the benchmark measures, and which the JDK arm runs too. A speedup"
echo "  from here lands on both arms and cannot close the gap against HotSpot."
echo
echo "  This is not necessarily a bad change. It is just not a VM change, and it must"
echo "  not be measured or reported as one -- and it does move the benchmark, so"
echo "  earlier absolute numbers stop being comparable."
if [ "$BASE_IS_REF" = "1" ]; then
    echo
    echo "  Translator sources changed against $BASE_REF:"
    git -C "$ROOT" diff --stat "$BASE_REF" -- vm/ByteCodeTranslator/src/com 2>/dev/null \
        | sed 's/^/    /' | tail -20
fi
exit 1
