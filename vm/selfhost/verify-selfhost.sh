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

# RESOLVE THE BINARY THAT EXISTS, preferring the release shape, and honour an explicit
# CN1_SELFHOST_BIN. build-selfhost.sh -O3 writes parpar-O3 and a bare build-selfhost.sh
# writes parpar, so hardcoding either one makes this die on a missing file rather than
# on a divergence -- which is what it did for anyone who had only ever built -O3.
# Optimisation level cannot change the emitted C (that is the property these gates
# exist to check), so either binary is a valid subject; the name is printed so a run
# is never ambiguous about which one it verified.
if [ -z "${CN1_SELFHOST_BIN:-}" ]; then
    for c in "$REPO/vm/selfhost/target/parpar-O3" "$REPO/vm/selfhost/target/parpar"; do
        [ -x "$c" ] && { CN1_SELFHOST_BIN="$c"; break; }
    done
fi
PARPAR="${CN1_SELFHOST_BIN:-}"
[ -n "$PARPAR" ] && [ -x "$PARPAR" ] || {
    echo "no self-hosted binary in $REPO/vm/selfhost/target -- run build-selfhost.sh first"
    exit 1; }
echo "verify-selfhost: subject $PARPAR"
JAPI="$REPO/vm/selfhost/target/javaapi-classes"
TR="$REPO/vm/ByteCodeTranslator/target/classes"
ASM_CP_FILE="$REPO/vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt"
# A MISSING CLASSPATH FILE IS A SETUP ERROR, NOT A CORRECTNESS FAILURE, and it did
# not read as one: `mvn clean package` (which translate-and-build.sh runs whenever
# the translator sources change, i.e. after running the gauntlet) deletes target/
# and takes this file with it. The bare `cat` that used to be here then failed with
# one line of shell noise, the gate exited non-zero, and perf-guard's own message
# said to "treat this as a correctness failure". It is not; it just needs the
# regenerating build to have run.
[ -r "$ASM_CP_FILE" ] || {
    echo "missing $ASM_CP_FILE" >&2
    echo "  mvn clean package removes target/, and the gauntlet triggers one." >&2
    echo "  Run $REPO/vm/selfhost/build-selfhost.sh -O3 first; it regenerates it." >&2
    exit 1; }
ASM="$(cat "$ASM_CP_FILE")"

# The JVM side of gate A runs target/classes, which nothing in this script builds
# -- build-selfhost.sh compiles the translator only for the NATIVE side. A source
# edit that has not been through `mvn package` therefore makes gate A compare the
# new translator against the old one, and it reports the intended change as a VM
# divergence. That has happened; the diff pointed at java_util_ArrayDeque.c and
# looked exactly like a real one. Maven's own incremental check does not save us
# here either -- it answered "Nothing to compile - all classes are up to date"
# for a source three hours newer than its class, so this compares the trees
# directly rather than trusting it.
newest_src="$(find "$REPO/vm/ByteCodeTranslator/src" -name '*.java' -newer "$TR" -print -quit 2>/dev/null || true)"
if [ -n "$newest_src" ]; then
    echo "STALE: $TR is older than $newest_src" >&2
    echo "gate A would compare the new translator against the old one. Run:" >&2
    echo "  (cd $REPO/vm && mvn -q -B -pl ByteCodeTranslator clean package -DskipTests)" >&2
    echo "and restore target/selfhost-asm-classpath.txt, which clean removes." >&2
    exit 1
fi

# THE SUBJECT BINARY GETS THE SAME STALENESS GUARD, AND IT IS NOT A HYPOTHETICAL.
# The chooser above prefers parpar-O3, while `build-selfhost.sh` with no arguments
# writes parpar -- so a parpar-O3 left over from an earlier session is verified in
# place of what was just built, and it reports a translator change as a VM divergence
# on exactly the files that change was supposed to touch. That cost a correct
# optimization: it was measured, it failed gate A against a binary three hours older
# than the source, and it was withdrawn. Printing the subject is not enough, because
# the name says nothing about the binary's age.
newest_bin_src="$(find "$REPO/vm/ByteCodeTranslator/src" -type f -newer "$PARPAR" -print -quit 2>/dev/null || true)"
if [ -n "$newest_bin_src" ]; then
    echo "STALE: subject $PARPAR is older than $newest_bin_src" >&2
    echo "gate A would compare the new translator against an old BINARY. Run:" >&2
    echo "  $REPO/vm/selfhost/build-selfhost.sh          # writes target/parpar" >&2
    echo "  $REPO/vm/selfhost/build-selfhost.sh -O3      # writes target/parpar-O3" >&2
    echo "or pin one with CN1_SELFHOST_BIN=<path>. Building only one of the two leaves" >&2
    echo "the other stale, and the chooser prefers parpar-O3." >&2
    exit 1
fi

W="$REPO/vm/selfhost/target/verify"
rm -rf "$W"; mkdir -p "$W"
OUT="$W/out"

# CN1_NATIVE_VERIFY is forwarded explicitly. `env -i` starts from an EMPTY
# environment, so a workflow-level `CN1_NATIVE_VERIFY: strict` never reached the
# translator here and NativeSignatureVerifier.mode() defaulted to OFF -- the gate
# reported a mode it was not running in, which is the failure this whole script
# exists to prevent. Forwarded rather than hard-coded so a local run without it set
# behaves as it always did.
run() {
    local tag=$1; shift
    mkdir -p "$OUT"
    ( cd "$W" && env -i PATH=/usr/bin:/bin HOME="$HOME" TMPDIR=/tmp LC_ALL=C \
        CN1_NATIVE_VERIFY="${CN1_NATIVE_VERIFY:-}" \
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
