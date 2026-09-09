#!/bin/bash
# Validation gates for the self-hosted translator.
#
#   verify-selfhost.sh <classesDir> <AppName> <package>
#
# Compares the C emitted by the JVM-hosted translator against the C emitted by the
# native one. The comparison is on the emitted SOURCE, never on the compiled binary:
# clang is not what is under test, and gating on object code would fail for toolchain
# reasons that have nothing to do with the VM.
#
# Gate D runs first and is the cheap one: the native translator against itself. If it
# is not self-consistent, nothing downstream means anything, and the cause is VM
# nondeterminism rather than a difference between the two runtimes.
#
# Gate A is the headline: same program, different runtime, identical output.
#
# Both sides run into the SAME absolute output path, sequentially, with the tree
# moved aside between runs. The generated CMakeLists embeds
# srcRoot.getAbsolutePath(), so running in one place removes a whole class of false
# differences rather than normalizing it away afterwards. Both also run under a
# constructed environment: the translator reads its knobs from getenv (see
# Util.getProperty), so a stray CN1_* variable would change one side's output.
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
J8="${JDK_8_HOME:?set JDK_8_HOME to a working JDK 8}"
CLASSES="${1:?usage: verify-selfhost.sh <classesDir> <AppName> <package>}"
APP="${2:?}"
PKG="${3:?}"

PARPAR="$REPO/vm/selfhost/target/parpar"
[ -x "$PARPAR" ] || { echo "no $PARPAR -- run build-selfhost.sh first"; exit 1; }
JAPI="$REPO/vm/selfhost/target/javaapi-classes"
TR="$REPO/vm/ByteCodeTranslator/target/classes"
ASM="$(cat "$REPO/vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt")"

W="$REPO/vm/selfhost/target/verify"
rm -rf "$W"; mkdir -p "$W"
OUT="$W/out"

run() {
    local tag=$1; shift
    mkdir -p "$OUT"
    ( cd "$W" && env -i PATH=/usr/bin:/bin HOME="$HOME" TMPDIR=/tmp LC_ALL=C \
        CN1_RESOURCE_PATH="$REPO/vm/ByteCodeTranslator/src" "$@" ) > "$W/$tag.log" 2>&1 \
        || { echo "$tag FAILED"; tail -20 "$W/$tag.log"; exit 1; }
    mv "$OUT" "$W/$tag-tree"
}

jvm_args=( "$J8/bin/java" -cp "$TR:$ASM" com.codename1.tools.translator.ByteCodeTranslator )
common=( clean "$JAPI;$CLASSES" "$OUT" "$APP" "$PKG" "$APP" 1.0 clean none )

run parpar1 "$PARPAR" "${common[@]}"
run parpar2 "$PARPAR" "${common[@]}"
run jvm "${jvm_args[@]}" "${common[@]}"

files=$(find "$W/jvm-tree" -type f | wc -l | tr -d ' ')
bytes=$(find "$W/jvm-tree" -type f -exec cat {} + | wc -c | tr -d ' ')
# A comparison of two empty trees is not a passing comparison.
[ "$files" -gt 10 ] || { echo "VACUOUS: only $files files emitted"; exit 1; }
echo "corpus: $APP -- $files files, $bytes bytes"

fail=0
if diff -rq "$W/parpar1-tree" "$W/parpar2-tree" > "$W/gateD.txt" 2>&1; then
    echo "GATE D (parpar vs parpar): PASS"
else
    echo "GATE D (parpar vs parpar): FAIL -- $(grep -c . "$W/gateD.txt") paths"; fail=1
fi
if diff -rq "$W/jvm-tree" "$W/parpar1-tree" > "$W/gateA.txt" 2>&1; then
    echo "GATE A (jvm vs parpar):    PASS -- $files files byte-identical"
else
    echo "GATE A (jvm vs parpar):    FAIL -- $(grep -c . "$W/gateA.txt") of $files paths differ"
    sed 's|.*/'"$APP"'-src/||;s| and .*||' "$W/gateA.txt" | head -20
    fail=1
fi

# Negative control: a comparator nobody has watched fail is not a comparator. Flip one
# byte and require the comparison to notice, so a pass above cannot be a pass by
# accident (a mis-set path, an empty tree, a diff invocation that never ran).
victim=$(find "$W/parpar1-tree" -name '*.c' | sort | head -1)
cp "$victim" "$W/victim.bak"
printf 'x' | dd of="$victim" bs=1 seek=40 conv=notrunc status=none
if diff -rq "$W/jvm-tree" "$W/parpar1-tree" > /dev/null 2>&1; then
    echo "NEGATIVE CONTROL: FAIL -- a corrupted tree still compared equal"; fail=1
else
    echo "NEGATIVE CONTROL: PASS -- corruption detected"
fi
cp "$W/victim.bak" "$victim"

exit $fail
