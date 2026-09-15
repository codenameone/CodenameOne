#!/bin/bash
# Performance ratchet for the self-hosted translator.
#
# WHY A GATE AND NOT A README LINE: the two defaults this protects were each set by a
# measurement that a later configuration change silently invalidated, and neither
# regression would have failed a single functional test. Marker count and mark-worklist
# size are worth ~5x wall clock and ~25% of peak memory between them, and losing either
# looks exactly like normal code that passes every gate in the tree.
#
#   scripts/../vm/selfhost/perf-guard.sh [rounds]
#
# Fails when the ratio against JDK 25 exceeds the ceilings below. The bench itself
# refuses to print a ratio unless every arm emitted byte-identical C, so a green result
# here is also a correctness result.
#
# CEILINGS carry headroom over the measured values rather than hugging them, because
# this box's wall clock moves with load and peak footprint moves run to run:
#
#             measured (5 rounds, quiet)   ceiling   what breaching it means
#   time      1.33-1.35x                   2.00x     serial marking is back
#   memory    1.59-1.80x                   2.10x     the 64K mark worklist is back
#
# Before these two landed the same corpus measured 5.09x time and 2.35x memory, so both
# ceilings sit well clear of noise and well below the regression they exist to catch.
#
# VALIDATED BY WATCHING IT FAIL, against a deliberately regressed build
# (-DCN1_GC_SERIAL_MARK): reports time=31.74x memory=2.42x, prints both REGRESSION lines
# and exits 1.
#
# DO NOT PIPE IT. `perf-guard.sh | tee log` reports $? from tee, not from this script, so
# the run looks green while the gate is screaming -- which is exactly how the failure
# above first read as a pass. Redirect instead (`perf-guard.sh > log 2>&1`), or set
# `set -o pipefail` in the caller.
set -e
cd "$(dirname "$0")"
ROUNDS="${1:-5}"
T="$(cd ../.. && pwd)/vm/selfhost/target"
: "${CN1_SELFHOST_BIN:=$T/parpar-O3}"
export CN1_SELFHOST_BIN
MAX_TIME="${CN1_PERF_MAX_TIME:-2.00}"
MAX_MEM="${CN1_PERF_MAX_MEM:-2.10}"
OUT=$(mktemp -t cn1perf)
./bench-selfhost.sh "$T/javaapi-classes;$T/asm-classes;$T/classes" \
    com_codename1_tools_translator_ByteCodeTranslator com.codename1.tools.translator \
    "$ROUNDS" | tee "$OUT"
LINE=$(grep '^vs jdk25:' "$OUT" || true)
if [ -z "$LINE" ]; then
    echo "perf-guard: FAIL -- no jdk25 ratio line. The bench refuses to print ratios when"
    echo "  the arms did not emit identical C, so treat this as a correctness failure."
    exit 1
fi
TIME=$(echo "$LINE" | sed -n 's/.*time \([0-9.]*\)x.*/\1/p')
MEM=$(echo "$LINE" | sed -n 's/.*memory \([0-9.]*\)x.*/\1/p')
echo "perf-guard: time=${TIME}x (max ${MAX_TIME}x)  memory=${MEM}x (max ${MAX_MEM}x)"
FAIL=0
awk -v v="$TIME" -v m="$MAX_TIME" 'BEGIN{exit !(v>m)}' && { echo "perf-guard: TIME REGRESSION"; FAIL=1; }
awk -v v="$MEM"  -v m="$MAX_MEM"  'BEGIN{exit !(v>m)}' && { echo "perf-guard: MEMORY REGRESSION"; FAIL=1; }
[ "$FAIL" = 0 ] && echo "perf-guard: OK"
exit $FAIL
