#!/bin/bash
# Profile-guided build of the self-hosted translator.
#
# WHY THIS EXISTS, measured rather than assumed.
#
# On the hello corpus the self-hosted binary was 1.20x JDK 25 on wall clock, and the
# obvious reading -- that our generated code is worse -- is wrong. Hardware counters
# say the opposite:
#
#                        parpar        jdk25
#     real                 6.20s        4.85s     1.278x
#     instructions        129.8B       209.8B     0.619
#     cycles               49.0B        69.9B     0.701
#     cores busy            1.78         3.49
#
# We retire 38% FEWER instructions and burn 30% fewer cycles, and still lose on the
# clock. Restricting HotSpot's helper threads shows where its time goes: with
# -XX:CICompilerCount=2 its instruction count drops from 216.5B to 149.0B, so 67.5B
# instructions -- 31% of everything it executes -- are JIT COMPILATION, running on
# background threads. That parallel compilation buys it 0.42s of wall clock, and
# parallel GC another 0.5s. At equal core usage the two are at parity.
#
# So HotSpot is not out-generating us; it is converting spare cores into better
# hot-path code while the program runs. The AOT answer is to do the same work at
# BUILD time, which is what this script does.
#
# Measured, interleaved, profile trained on the TRANSLATOR corpus and timed on the
# HELLO corpus -- deliberately not the corpus it was trained on:
#
#     plain -O3   5.760s min     1.198x of JDK 25
#     PGO         5.180s min     1.077x of JDK 25      -10.1%
#
# Instruction count falls with it, 129-133B to 105-108B, so this is inlining and
# dead-path elimination rather than layout alone. Training and timing on the SAME
# corpus gives -10.4%, i.e. essentially the same, so the profile generalises.
#
# Usage: build-pgo.sh [train-corpus]
#   train-corpus  "self" (default) trains on the translator's own classes, "hello"
#                 trains on the hello corpus. Train on one and time on the other if
#                 the number is going to be quoted anywhere.
set -e
cd "$(dirname "$0")"
HERE="$(pwd)"
REPO="$(cd ../.. && pwd)"
T="$HERE/target"
TRAIN="${1:-self}"
PROFDIR="$T/pgo"
PROFDATA="$PROFDIR/cn1.profdata"

PROFDATA_TOOL="$(command -v llvm-profdata || true)"
if [ -z "$PROFDATA_TOOL" ]; then
    PROFDATA_TOOL="$(xcrun --find llvm-profdata 2>/dev/null || true)"
fi
if [ -z "$PROFDATA_TOOL" ]; then
    echo "build-pgo: no llvm-profdata on PATH and xcrun cannot find one."
    echo "  It ships with LLVM (brew install llvm) and with the Xcode toolchain."
    exit 2
fi

rm -rf "$PROFDIR"; mkdir -p "$PROFDIR"

# 1. Instrumented build. -fprofile-generate composes with the -flto=thin that -O3
#    already adds; the counters are emitted per function and merged below.
echo "build-pgo: 1/3 instrumented build"
CN1_SELFHOST_CFLAGS="-fprofile-generate=$PROFDIR $CN1_SELFHOST_CFLAGS" ./build-selfhost.sh -O3 > "$PROFDIR/build1.log" 2>&1 \
    || { echo "  instrumented build failed:"; tail -20 "$PROFDIR/build1.log"; exit 1; }
cp "$T/parpar-O3" "$PROFDIR/parpar-instrumented"

# 2. Training run. The corpus only has to EXERCISE the translator; it does not have
#    to be the one that will be translated later, and using a different one is the
#    honest way to quote a number.
if [ "$TRAIN" = "hello" ]; then
    CORPUS="$T/javaapi-classes;$T/hello-corpus"
    MAIN=com_codenameone_examples_hellocodenameone_HelloCodenameOneStub
    PKG=com.codenameone.examples.hellocodenameone
else
    CORPUS="$T/asm-classes;$T/classes"
    MAIN=com_codename1_tools_translator_ByteCodeTranslator
    PKG=com.codename1.tools.translator
fi
for d in $(echo "$CORPUS" | tr ';' ' '); do
    [ -d "$d" ] || { echo "build-pgo: missing training corpus $d -- run build-selfhost.sh first."; exit 2; }
done
echo "build-pgo: 2/3 training run on the $TRAIN corpus"
W="$PROFDIR/train"; rm -rf "$W"; mkdir -p "$W/out"
( cd "$W" && env -i PATH=/usr/bin:/bin HOME="$HOME" TMPDIR=/tmp LC_ALL=C \
    CN1_RESOURCE_PATH="$REPO/vm/ByteCodeTranslator/src" \
    LLVM_PROFILE_FILE="$PROFDIR/cn1-%p.profraw" \
    "$PROFDIR/parpar-instrumented" clean "$CORPUS" "$W/out" "$MAIN" "$PKG" "$MAIN" 1.0 clean none \
    ) > "$PROFDIR/train.log" 2>&1 \
    || { echo "  training run failed:"; tail -20 "$PROFDIR/train.log"; exit 1; }
rm -rf "$W"
ls "$PROFDIR"/*.profraw > /dev/null 2>&1 \
    || { echo "build-pgo: the training run wrote no .profraw -- nothing to optimise with."; exit 1; }
"$PROFDATA_TOOL" merge -output="$PROFDATA" "$PROFDIR"/*.profraw

# 3. Optimised build. The two -Wno- flags matter: a profile taken from a slightly
#    older source is a WARNING, and this tree builds with warnings as errors in some
#    configurations. A stale profile is still worth using -- it degrades to no
#    information for the functions that moved.
echo "build-pgo: 3/3 optimised build"
CN1_SELFHOST_CFLAGS="-fprofile-use=$PROFDATA -Wno-profile-instr-out-of-date -Wno-profile-instr-unprofiled $CN1_SELFHOST_CFLAGS" \
    ./build-selfhost.sh -O3 > "$PROFDIR/build2.log" 2>&1 \
    || { echo "  optimised build failed:"; tail -20 "$PROFDIR/build2.log"; exit 1; }
echo "build-pgo: built $T/parpar-O3 with a profile from the $TRAIN corpus"
