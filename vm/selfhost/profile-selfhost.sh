#!/bin/bash
# ============================================================================
# Where does the self-hosted translator's CPU actually go?
#
# The elapsed half of the gap against JDK 25 is the stable, gateable half (1.5%
# spread against peak memory's 13-21%), and on the hello corpus it is 1.257x. The
# translator is SINGLE-THREADED, so HotSpot's extra CPU-seconds (18.5 against our
# 13.3) are its JIT compiler threads and parallel GC burning other cores, not work
# that makes it finish sooner. Both mutators do the same job serially -- HotSpot in
# 5.03s, we in 6.35s. That is generated-code throughput, and the only honest way to
# choose what to fix is to look.
#
# REGISTRY's earlier profile put ITERATION at 19.3% of main-thread time and
# concluded the residual "is not iterator dispatch, and it is not frame overhead"
# -- but that was the TRANSLATOR corpus, 222 classes and under a second. This runs
# against whichever corpus is asked for, and hello is the one that matters.
#
#   ./profile-selfhost.sh [hello|translator] [seconds]
#
# Reads `sample`'s "sort by top of stack" section, which is self time per symbol --
# the cumulative call tree above it answers a different question and is the one
# people usually misread.
# ============================================================================
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
T="$REPO/vm/selfhost/target"
CORPUS_NAME="${1:-hello}"
SECONDS_TO_SAMPLE="${2:-8}"
BIN="${CN1_SELFHOST_BIN:-$T/parpar-O3}"
[ -x "$BIN" ] || { echo "no binary at $BIN -- run build-selfhost.sh -O3 first" >&2; exit 1; }

if [ "$CORPUS_NAME" = hello ]; then
    [ -d "$T/hello-corpus" ] || { echo "missing $T/hello-corpus" >&2; exit 1; }
    CORPUS="$T/javaapi-classes;$T/hello-corpus"
    APP=com_codenameone_examples_hellocodenameone_HelloCodenameOneStub
    PKG=com.codenameone.examples.hellocodenameone
else
    CORPUS="$T/javaapi-classes;$T/asm-classes;$T/classes"
    APP=com_codename1_tools_translator_ByteCodeTranslator
    PKG=com.codename1.tools.translator
fi

W="$(mktemp -d "${TMPDIR:-/tmp}/cn1profile.XXXXXX")"
mkdir -p "$W/out"
echo "profiling $BIN over the $CORPUS_NAME corpus for ${SECONDS_TO_SAMPLE}s"

# `exec` matters: without it $! is the SUBSHELL's pid, the subshell sits in wait4,
# and `sample` faithfully reports that the process is asleep -- 4187 samples in
# __wait4 and nothing else. exec replaces the subshell with the binary so $! is the
# thing being measured.
( cd "$W" && exec env -i PATH=/usr/bin:/bin HOME="$HOME" TMPDIR=/tmp LC_ALL=C \
    CN1_RESOURCE_PATH="$REPO/vm/ByteCodeTranslator/src" \
    "$BIN" clean "$CORPUS" "$W/out" "$APP" "$PKG" "$APP" 1.0 clean none ) > "$W/run.log" 2>&1 &
TARGET=$!
# A short grace period: the first moments are class parsing, which is real work but
# not the emission phase the ratio is dominated by.
sleep 1
sample "$TARGET" "$SECONDS_TO_SAMPLE" -file "$W/sample.txt" >/dev/null 2>&1 || true
wait "$TARGET" 2>/dev/null || true

echo
echo "=== SELF TIME BY SYMBOL (top of stack) ==="
# The section is "Sort by top of stack, same collapsed (when >= N):"; take what
# follows it, strip the binary suffix, and show the heaviest symbols.
awk '/Sort by top of stack/{flag=1;next} /^$/{if(flag && seen)exit} flag&&NF{seen=1;print}' \
    "$W/sample.txt" | head -30
echo
echo "evidence: $W/sample.txt   (run log: $W/run.log)"
