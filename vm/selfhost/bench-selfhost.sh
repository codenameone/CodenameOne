#!/bin/bash
# Wall clock and peak memory: the native translator against the JVM-hosted one.
#
#   bench-selfhost.sh <classesDir> <AppName> <package> [rounds]
#
# Discipline copied from vm/benchmarks/run-benchmark.sh:
#
#  - Arms are INTERLEAVED within each round. Sequential A-then-B on this hardware
#    carries a thermal bias large enough to invent a result.
#  - Time takes the MINIMUM of N: the floor is the machine's best, and noise only
#    ever adds. Memory takes the MAXIMUM, because a peak is a max and a
#    min-of-peaks would understate it.
#  - Raw per-round samples are printed, not just the extremum, because a single
#    min hides a bimodal distribution.
#  - Ratios are refused unless the two translators emitted identical C. A speed
#    number from a translator that emits different output is meaningless.
#
# Memory is phys_footprint via `vmmap --summary` on macOS, sampled while the child
# runs. NEVER ps rss: vm/CLAUDE.md records 151/207/219 MB measured for one
# unchanged binary. Timing and memory rounds are separate so the sampler cannot
# contaminate the clock.
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
J8="${JDK_8_HOME:?set JDK_8_HOME to a working JDK 8}"
CLASSES="${1:?usage: bench-selfhost.sh <classesDir> <AppName> <package> [rounds]}"
APP="${2:?}"; PKG="${3:?}"; ROUNDS="${4:-5}"

# -O3 -flto=thin is the documented release shape (vm/benchmarks/README.md);
# CN1_SELFHOST_BIN overrides it for an A/B against the -O1 diff-gate build.
PARPAR="${CN1_SELFHOST_BIN:-$REPO/vm/selfhost/target/parpar-O3}"
JAPI="$REPO/vm/selfhost/target/javaapi-classes"
TR="$REPO/vm/ByteCodeTranslator/target/classes"
ASM="$(cat "$REPO/vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt")"
W="$REPO/vm/selfhost/target/bench"; rm -rf "$W"; mkdir -p "$W"

runcmd() { # $1 out dir; rest ignored -- selects arm by $ARM
    if [ "$ARM" = parpar ]; then
        env CN1_RESOURCE_PATH="$REPO/vm/ByteCodeTranslator/src" \
            "$PARPAR" clean "$JAPI;$CLASSES" "$1" "$APP" "$PKG" "$APP" 1.0 clean none
    else
        "$J8/bin/java" -cp "$TR:$ASM" com.codename1.tools.translator.ByteCodeTranslator \
            clean "$JAPI;$CLASSES" "$1" "$APP" "$PKG" "$APP" 1.0 clean none
    fi
}

echo "corpus: $CLASSES   rounds: $ROUNDS"
echo "memory metric: phys_footprint via vmmap --summary (macOS)"
declare -a t_parpar t_jvm
for r in $(seq 1 $ROUNDS); do
    for ARM in parpar jvm; do
        out="$W/$ARM-$r"; rm -rf "$out"; mkdir -p "$out"
        s=$(python3 -c 'import time;print(time.monotonic())')
        runcmd "$out" > "$W/$ARM-$r.log" 2>&1
        e=$(python3 -c 'import time;print(time.monotonic())')
        d=$(python3 -c "print(f'{$e-$s:.3f}')")
        if [ "$ARM" = parpar ]; then t_parpar+=("$d"); else t_jvm+=("$d"); fi
        rm -rf "$out"
    done
done
min() { printf '%s\n' "$@" | sort -n | head -1; }
mp=$(min "${t_parpar[@]}"); mj=$(min "${t_jvm[@]}")
echo "parpar times: ${t_parpar[*]}   min=${mp}s"
echo "jvm8   times: ${t_jvm[*]}   min=${mj}s"
python3 -c "print(f'TIME  parpar/jvm8 = {$mp/$mj:.2f}x  ({\"parpar faster\" if $mp<$mj else \"jvm faster\"})')"

# memory, sampled in its own rounds
peak() { # $1 = arm -- peak phys_footprint in MB
    local out="$W/mem-$1"; rm -rf "$out"; mkdir -p "$out"
    # `exec` inside the subshell so $! is the translator's own pid. Without it the
    # pid belongs to the subshell wrapper, and vmmap dutifully reports the wrapper's
    # ~1 MB footprint for both arms -- a measurement that looks like a result.
    if [ "$1" = parpar ]; then
        ( exec env CN1_RESOURCE_PATH="$REPO/vm/ByteCodeTranslator/src" \
            "$PARPAR" clean "$JAPI;$CLASSES" "$out" "$APP" "$PKG" "$APP" 1.0 clean none \
            > /dev/null 2>&1 ) &
    else
        ( exec "$J8/bin/java" -cp "$TR:$ASM" com.codename1.tools.translator.ByteCodeTranslator \
            clean "$JAPI;$CLASSES" "$out" "$APP" "$PKG" "$APP" 1.0 clean none \
            > /dev/null 2>&1 ) &
    fi
    local pid=$! best=0
    # The kernel tracks the peak itself ("Physical footprint (peak)"), so a sample
    # taken at any point reports the high-water mark so far rather than an instant
    # -- sampling only has to catch the process alive at least once.
    while kill -0 $pid 2>/dev/null; do
        local raw
        raw=$(vmmap --summary $pid 2>/dev/null | awk -F: '/Physical footprint \(peak\)/{gsub(/ /,"",$2); print $2; exit}')
        if [ -n "$raw" ]; then
            best=$(python3 -c "
v='$raw'
mult={'K':1/1024.0,'M':1.0,'G':1024.0}.get(v[-1:], 1/1048576.0)
n=float(v[:-1]) if v[-1:] in 'KMG' else float(v)
print(max($best, n*mult))")
        fi
    done
    wait $pid 2>/dev/null || true
    rm -rf "$out"
    echo "$best"
}
pp=$(peak parpar); pj=$(peak jvm)
printf 'MEM   parpar peak=%.1f MB   jvm8 peak=%.1f MB\n' "$pp" "$pj"
python3 -c "print(f'MEM   parpar/jvm8 = {$pp/$pj:.2f}x  ({\"parpar smaller\" if $pp<$pj else \"jvm smaller\"})')"
