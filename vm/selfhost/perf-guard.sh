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
# RESOLVE THE BINARY THAT EXISTS, preferring the release shape. build-selfhost.sh -O3
# writes parpar-O3 and a bare build-selfhost.sh writes parpar, so hardcoding either one
# makes this fail on a missing file rather than on a regression -- which is how the CI
# step would have died on its first run, since that workflow builds without -O3.
if [ -z "${CN1_SELFHOST_BIN:-}" ]; then
    if [ -x "$T/parpar-O3" ]; then
        CN1_SELFHOST_BIN="$T/parpar-O3"
    elif [ -x "$T/parpar" ]; then
        CN1_SELFHOST_BIN="$T/parpar"
    else
        echo "perf-guard: no self-hosted binary in $T -- run build-selfhost.sh first." >&2
        exit 1
    fi
fi
export CN1_SELFHOST_BIN
echo "perf-guard: measuring $CN1_SELFHOST_BIN"
# WHICH CORPUS. THIS GATE MEASURED THE WRONG ONE AND SAID SO IN ITS SUCCESS MESSAGE.
# It ran the translator's own classes -- 222 classes, ~0.8s -- reported
# "ParparVM beats JDK 25" at 0.837x/0.602x, and that was true only of that corpus.
# On the HelloCodenameOne app corpus (5,326 classes, ~6.3s, the shape of a real
# customer build) the same binary LOSES: 1.263x elapsed, 1.349x peak. A gate whose
# corpus is easier than the workload is worse than no gate, because it certifies the
# drift. Hello is the default whenever its corpus is present, and every line this
# script prints names the corpus it measured.
T_CORPUS="$T/javaapi-classes;$T/asm-classes;$T/classes"
T_APP=com_codename1_tools_translator_ByteCodeTranslator
T_PKG=com.codename1.tools.translator
if [ -d "$T/hello-corpus" ] && [ "${CN1_PERF_CORPUS:-hello}" = "hello" ]; then
    CORPUS_NAME=hello
    CORPUS="$T/javaapi-classes;$T/hello-corpus"
    APP=com_codenameone_examples_hellocodenameone_HelloCodenameOneStub
    PKG=com.codenameone.examples.hellocodenameone
else
    CORPUS_NAME=translator
    CORPUS="$T_CORPUS"; APP="$T_APP"; PKG="$T_PKG"
    if [ "${CN1_PERF_CORPUS:-hello}" = "hello" ]; then
        echo "perf-guard: WARNING -- $T/hello-corpus is absent, falling back to the"
        echo "  TRANSLATOR corpus. It is smaller, shorter and easier, and a pass here"
        echo "  does NOT mean ParparVM beats JDK 25 on a real application."
    fi
fi

# WHAT IS GATED DEPENDS ON WHAT IS ACTUALLY MEASURABLE, AND ON THE HELLO CORPUS THAT
# IS THE OPPOSITE OF WHAT THE TRANSLATOR CORPUS SUGGESTED.
#
#   elapsed  STABLE. Spread 1.9% on parpar, 1.8% on jdk25. Gate it.
#   peak     NOT STABLE, and not because of the machine: the SAME binary on the SAME
#            input over six standalone runs measured 1440/1543/1551/1554/1568/1649MB
#            -- 14.6% -- while JDK 25 held 1.6%. Interleaved in the bench it reached
#            21.5% (1527..1855MB), which makes the reported ratio anything from
#            1.147x (min/min) to 1.349x (max/max) depending only on which statistic
#            is chosen. That is collector pacing variance, not host RAM:
#            cn1_available_memory() on this clean-C target returns hw.memsize/2, a
#            constant.
#
# So peak is REPORTED WITH ITS SPREAD and gated only when the run was steady enough
# for the number to mean anything. Asserting on a metric with 15-20% of internal
# variance would either fail at random or pass a real regression, and a perf gate
# that does either gets ignored.
#
# This is worth fixing rather than tolerating: our BEST case (1440MB) is already
# close to JDK 25 (1354-1375MB). The gap is not representation, it is that the
# collector does not reliably reach its own best case.
case "$(uname -s)" in
    Darwin) DEF_TIME=1.35; DEF_MEM=1.45 ;;
    *)      DEF_TIME=1.50; DEF_MEM=1.60 ;;
esac
MAX_TIME="${CN1_PERF_MAX_TIME:-$DEF_TIME}"
MAX_MEM="${CN1_PERF_MAX_MEM:-$DEF_MEM}"
# Above this per-arm elapsed spread the wall-clock ratio is reported, not gated.
MAX_SPREAD="${CN1_PERF_MAX_SPREAD:-15}"
# Above this per-arm PEAK spread the memory ratio is reported, not gated. Today
# parpar measures 15-21% here, so memory is effectively ungated until the pacing
# variance is fixed -- which is the honest state of affairs, not a loophole.
MAX_MEM_SPREAD="${CN1_PERF_MAX_MEM_SPREAD:-8}"
OUT=$(mktemp "${TMPDIR:-/tmp}/cn1perf.XXXXXX")   # GNU mktemp needs the Xs
echo "perf-guard: corpus=$CORPUS_NAME"
./bench-selfhost.sh "$CORPUS" "$APP" "$PKG" "$ROUNDS" | tee "$OUT"
# The bench prints "vs jdk25: elapsed 0.843x, peak memory 0.599x". Both halves of
# that were wrong here and the gate could never fire: it grepped for an arm named
# "jdk25" while bench-selfhost.py named arms POSITIONALLY ("jdk-1"), and it parsed
# "time Nx" while the bench prints "elapsed Nx". A gate that cannot pass is not a
# gate -- it reported FAIL on a run that was in fact 0.843x and 0.599x. The arm is
# now named after the JDK's real feature version, so this grep matches what it means.
LINE=$(grep '^vs jdk25:' "$OUT" || true)
if [ -z "$LINE" ]; then
    echo "perf-guard: FAIL -- no jdk25 ratio line."
    echo "  Either the arms did not emit identical C (the bench refuses to print ratios"
    echo "  then, and that is a correctness failure), or no reference arm was JDK 25 --"
    echo "  check JDK_25_HOME. These lines were printed:"
    grep '^vs ' "$OUT" | sed 's/^/    /' || echo "    (none)"
    exit 1
fi
TIME=$(echo "$LINE" | sed -n 's/.*elapsed \([0-9.]*\)x.*/\1/p')
MEM=$(echo "$LINE" | sed -n 's/.*peak memory \([0-9.]*\)x.*/\1/p')
if [ -z "$TIME" ] || [ -z "$MEM" ]; then
    echo "perf-guard: FAIL -- could not parse the ratio line: $LINE"
    exit 1
fi
SPREAD=$(sed -n 's/^spread parpar:.*elapsed[^(]*(\([0-9.]*\)%).*/\1/p' "$OUT" | head -1)
MEMSPREAD=$(sed -n 's/^spread parpar:.*peak[^(]*(\([0-9.]*\)%).*/\1/p' "$OUT" | head -1)
echo "perf-guard: corpus=$CORPUS_NAME time=${TIME}x (max ${MAX_TIME}x) memory=${MEM}x (max ${MAX_MEM}x)"
echo "perf-guard: parpar spread -- elapsed ${SPREAD:-?}% peak ${MEMSPREAD:-?}%"
FAIL=0

# ELAPSED is the stable half on the hello corpus (1.9%), so it is the real gate.
if [ -z "$SPREAD" ]; then
    echo "perf-guard: no elapsed spread line -- cannot tell whether the ratio is meaningful"
    FAIL=1
elif awk -v s="$SPREAD" -v m="$MAX_SPREAD" 'BEGIN{exit !(s>m)}'; then
    echo "perf-guard: TIME NOT GATED -- elapsed spread ${SPREAD}% exceeds ${MAX_SPREAD}%"
else
    awk -v v="$TIME" -v m="$MAX_TIME" 'BEGIN{exit !(v>m)}' && { echo "perf-guard: TIME REGRESSION"; FAIL=1; }
fi

# PEAK carries 15-21% of internal variance on this workload (same binary, same input),
# so it is gated only on a run steady enough for the number to mean something. Today
# that condition is usually FALSE, and saying so every run is the point: it keeps the
# instability visible instead of letting a noisy pass read as a result.
if [ -z "$MEMSPREAD" ]; then
    echo "perf-guard: no peak spread line -- memory not gated"
elif awk -v s="$MEMSPREAD" -v m="$MAX_MEM_SPREAD" 'BEGIN{exit !(s>m)}'; then
    echo "perf-guard: MEMORY NOT GATED -- peak spread ${MEMSPREAD}% exceeds ${MAX_MEM_SPREAD}%."
    echo "  Collector pacing variance, not the machine. Until it is fixed this benchmark"
    echo "  cannot resolve a memory change smaller than that spread."
else
    awk -v v="$MEM" -v m="$MAX_MEM" 'BEGIN{exit !(v>m)}' && { echo "perf-guard: MEMORY REGRESSION"; FAIL=1; }
fi

if [ "$FAIL" = 0 ]; then
    echo "perf-guard: OK -- no regression on the $CORPUS_NAME corpus"
    if [ "$CORPUS_NAME" != hello ]; then
        echo "  NOTE: this is NOT the hello corpus and does not show parity with JDK 25."
    fi
fi
exit $FAIL
