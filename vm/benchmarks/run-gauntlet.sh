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

TORTURES="MapTorture SbTorture StrCmp FusedTest IbpTest ExcTest ThreadChurn SoeTest"
mkdir -p target/host-classes target/bin
# FusedTest uses @com.codename1.annotations.Fused -- supply the annotation
# source for the host compile (ParparVM's JavaAPI carries its own copy)
"$J8/bin/javac" -nowarn -encoding UTF-8 -d target/host-classes \
    ../../CodenameOne/src/com/codename1/annotations/Fused.java src/com/bench/*.java

fail=0
for t in $TORTURES; do
    ./translate-and-build.sh "$t" "target/bin/$t" > /dev/null
    a="$(./target/bin/$t 2>/dev/null | grep -v '^\[')"
    b="$("$REF_JAVA" -cp target/host-classes "com.bench.$t" 2>/dev/null)"
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
