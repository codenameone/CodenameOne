#!/bin/bash
# ParparVM correctness gauntlet: every torture suite must produce output
# BYTE-IDENTICAL to the host JVM, plus GC stress rounds in both cooperative
# and forced-signal thread-stop modes. This is the gate every VM change must
# pass -- a checksum divergence is a codegen/GC bug by definition.
#
#   run-gauntlet.sh
#
# Requirements: JDK_8_HOME, BENCH_JAVA or `java` on PATH, Maven, clang.
set -e
cd "$(dirname "$0")"
REF_JAVA="${BENCH_JAVA:-java}"
J8="${JDK_8_HOME:?set JDK_8_HOME}"

# TaggedSync guards Java monitor semantics on tagged boxed Integers (mutual
# exclusion + wait/notify) -- regression test for the tagged monitorEnter/Exit
# no-op bug a review caught.
#
# BoxEdge is the gate for the tagged immediates as a whole: all six boxed types
# (Integer/Long/Double/Float/Character/Short) crossed with tagged, heap-allocated,
# null and wrong-type receivers, over getClass/instanceof/equals/hashCode/compareTo,
# the collections, NaN and both zeroes, monitors, and Long/Double values chosen to
# fall OUTSIDE the taggable range so the heap fallback is exercised rather than
# assumed. It catches the failure this scheme makes easy -- a wrong class or a wrong
# hash, returned silently with nothing thrown.
#
# ConcatCorrupt is heap integrity ACROSS the fused string-concat natives, which take
# raw interior pointers into their source strings' byte[] and then ALLOCATE. Read its
# javadoc before trusting it: the sources turn out to be rooted in BOTH configurations
# (conservatively via the native frame, precisely via the caller's stack slots), so its
# non-vacuity is UNPROVEN. It earns its place as a heap-integrity torture -- 256 int[]
# verified across 400,000 concatenations -- not as proof of a rooting claim.
TORTURES="MapTorture IdmTorture HtTorture SbTorture StrCmp FusedTest IbpTest ExcTest ThreadChurn SoeTest TaggedSync BoxEdge ConcatCorrupt"
mkdir -p target/host-classes target/bin
# FusedTest uses @com.codename1.annotations.Fused -- supply the annotation
# source for the host compile (ParparVM's JavaAPI carries its own copy)
"$J8/bin/javac" -nowarn -encoding UTF-8 -d target/host-classes \
    ../../CodenameOne/src/com/codename1/annotations/Fused.java \
    common/src/main/java/com/bench/CommonWorkloads.java src/com/bench/*.java

fail=0
for t in $TORTURES; do
    ./translate-and-build.sh "$t" "target/bin/$t" > /dev/null
    a="$(./target/bin/$t 2>/dev/null | grep -v '^\[')"
    # The SAME filter on both sides. It used to be applied only to the target, so a
    # torture that emitted a "[...]" diagnostic diverged against its own host run -- and
    # stderr is no way around that, because System.err reaches fd 1 on the clean target.
    b="$("$REF_JAVA" -cp target/host-classes "com.bench.$t" 2>/dev/null | grep -v '^\[')"
    if [ -n "$a" ] && [ "$a" = "$b" ]; then
        echo "$t: MATCH"
    else
        echo "$t: DIVERGE"
        fail=1
    fi
done

./translate-and-build.sh GcStress target/bin/GcStress > /dev/null
./translate-and-build.sh MtStress target/bin/MtStress > /dev/null
for mode in "" "CN1_GC_SIGNAL_STOP=1"; do
    label="${mode:-cooperative}"
    for i in 1 2 3 4 5; do
        out="$(env $mode ./target/bin/GcStress 2>/dev/null | tail -1)"
        case "$out" in *FAIL*|"") echo "GcStress[$label] run $i: FAILED ($out)"; fail=1;; esac
    done
    for i in 1 2 3; do
        out="$(env $mode ./target/bin/MtStress 2>/dev/null | tail -1)"
        case "$out" in *FAIL*|"") echo "MtStress[$label] run $i: FAILED ($out)"; fail=1;; esac
    done
    echo "GcStress+MtStress[$label]: done"
done

[ "$fail" -eq 0 ] && echo "GAUNTLET GREEN" || { echo "GAUNTLET FAILED"; exit 1; }
