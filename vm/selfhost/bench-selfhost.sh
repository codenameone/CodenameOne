#!/bin/bash
# Wall clock and peak memory: the native translator against JVM-hosted ones.
#
#   bench-selfhost.sh <classesDir> <AppName> <package> [rounds]
#
# Reference JVMs come from SELFHOST_REF_JAVAS (comma-separated java binaries).
# The default is JDK 25 first, then JDK 8. JDK 25 is the honest headline -- it is
# what HotSpot can actually do -- and JDK 8 is kept only because it is what the
# builders currently fork.
#
# Discipline, following vm/benchmarks/run-benchmark.sh:
#
#  - Arms are INTERLEAVED within each round. Sequential A-then-B on this hardware
#    carries a thermal bias large enough to invent a result.
#  - Time takes the MINIMUM of N: the floor is the machine's best, and noise only
#    ever adds. Memory takes the MAXIMUM, because a peak is a max.
#  - Raw per-round samples are printed, not just the extremum: a lone minimum
#    hides a bimodal distribution.
#  - Ratios are refused unless every arm emitted identical C. A speed number from
#    a translator that emits different output is meaningless.
#
# Memory is the peak phys_footprint reported by /usr/bin/time -l on macOS, which
# is the same quantity vmmap calls "Physical footprint (peak)". NEVER ps rss:
# vm/CLAUDE.md records 151/207/219 MB measured for one unchanged binary.
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
CLASSES="${1:?usage: bench-selfhost.sh <classesDir> <AppName> <package> [rounds]}"
APP="${2:?}"; PKG="${3:?}"; ROUNDS="${4:-5}"

T="$REPO/vm/selfhost/target"
# -O3 -flto=thin is the documented release shape (vm/benchmarks/README.md);
# CN1_SELFHOST_BIN overrides it for an A/B against the -O1 diff-gate build.
PARPAR="${CN1_SELFHOST_BIN:-$T/parpar-O3}"
JAPI="$T/javaapi-classes"
TR="$REPO/vm/ByteCodeTranslator/target/classes"
ASM="$(cat "$REPO/vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt")"
DEFAULT_JAVAS="/Users/shai/Library/Java/JavaVirtualMachines/azul-25/Contents/Home/bin/java,${JDK_8_HOME:-}/bin/java"
IFS=',' read -r -a REF_JAVAS <<< "${SELFHOST_REF_JAVAS:-$DEFAULT_JAVAS}"

W="$T/bench"; rm -rf "$W"; mkdir -p "$W"

# $1 = arm label, $2 = output dir; runs one translation
invoke() {
    local arm=$1 out=$2
    if [ "$arm" = parpar ]; then
        env CN1_RESOURCE_PATH="$REPO/vm/ByteCodeTranslator/src" "$PARPAR" \
            clean "$JAPI;$CLASSES" "$out" "$APP" "$PKG" "$APP" 1.0 clean none
    else
        "$arm" -cp "$TR:$ASM" com.codename1.tools.translator.ByteCodeTranslator \
            clean "$JAPI;$CLASSES" "$out" "$APP" "$PKG" "$APP" 1.0 clean none
    fi
}

# "25" -> jdk25, "1.8.0_372" -> jdk8. Taking the leading number alone turns 1.8.0
# into "jdk1", which is why the second sed exists.
label() {
    case "$1" in
        parpar) echo parpar;;
        *) "$1" -version 2>&1 | head -1 \
             | sed -e 's/.*version "\([0-9][0-9.]*\).*/\1/' \
                   -e 's/^1\.\([0-9]*\).*/\1/' -e 's/\..*//' -e 's/^/jdk/';;
    esac
}

ARMS=(parpar "${REF_JAVAS[@]}")
declare -a NAMES
for a in "${ARMS[@]}"; do NAMES+=("$(label "$a")"); done
echo "corpus : $CLASSES"
echo "arms   : ${NAMES[*]}    rounds: $ROUNDS"
echo "memory : peak phys_footprint (/usr/bin/time -l)"

# --- correctness precondition: every arm must emit the same C -------------------
# Same absolute output path for all arms, sequentially, because the generated
# CMakeLists embeds srcRoot.getAbsolutePath().
OUT="$W/out"
for i in "${!ARMS[@]}"; do
    mkdir -p "$OUT"
    invoke "${ARMS[$i]}" "$OUT" > "$W/${NAMES[$i]}.log" 2>&1 || { echo "${NAMES[$i]} FAILED"; tail -5 "$W/${NAMES[$i]}.log"; exit 1; }
    mv "$OUT" "$W/tree-${NAMES[$i]}"
done
for i in "${!ARMS[@]}"; do
    [ "$i" -eq 0 ] && continue
    if ! diff -rq "$W/tree-${NAMES[0]}" "$W/tree-${NAMES[$i]}" > "$W/diff-${NAMES[$i]}.txt" 2>&1; then
        echo "DIVERGENCE (${NAMES[0]} vs ${NAMES[$i]}) -- ratios would be meaningless:"
        sed "s|.*/$APP-src/||;s| and .*||" "$W/diff-${NAMES[$i]}.txt" | head -5
        exit 1
    fi
done
files=$(find "$W/tree-${NAMES[0]}" -type f | wc -l | tr -d ' ')
[ "$files" -gt 10 ] || { echo "VACUOUS: only $files files"; exit 1; }
echo "output : $files files, identical across all arms"
echo

# --- timing, interleaved --------------------------------------------------------
declare -a SAMPLES
for r in $(seq 1 "$ROUNDS"); do
    for i in "${!ARMS[@]}"; do
        rm -rf "$W/run"; mkdir -p "$W/run"
        s=$(python3 -c 'import time;print(time.monotonic())')
        invoke "${ARMS[$i]}" "$W/run" > /dev/null 2>&1
        e=$(python3 -c 'import time;print(time.monotonic())')
        SAMPLES[$i]="${SAMPLES[$i]} $(python3 -c "print(f'{$e-$s:.2f}')")"
    done
done
declare -a MINS
for i in "${!ARMS[@]}"; do
    MINS[$i]=$(printf '%s\n' ${SAMPLES[$i]} | sort -n | head -1)
    printf "time %-8s min %6ss   samples:%s\n" "${NAMES[$i]}" "${MINS[$i]}" "${SAMPLES[$i]}"
done

# --- memory, measured separately so the probe cannot perturb the clock ----------
declare -a PEAKS
for i in "${!ARMS[@]}"; do
    rm -rf "$W/run"; mkdir -p "$W/run"
    if [ "${ARMS[$i]}" = parpar ]; then
        env CN1_RESOURCE_PATH="$REPO/vm/ByteCodeTranslator/src" /usr/bin/time -l "$PARPAR" \
            clean "$JAPI;$CLASSES" "$W/run" "$APP" "$PKG" "$APP" 1.0 clean none 2>"$W/mem.txt" >/dev/null
    else
        /usr/bin/time -l "${ARMS[$i]}" -cp "$TR:$ASM" com.codename1.tools.translator.ByteCodeTranslator \
            clean "$JAPI;$CLASSES" "$W/run" "$APP" "$PKG" "$APP" 1.0 clean none 2>"$W/mem.txt" >/dev/null
    fi
    PEAKS[$i]=$(awk '/peak memory footprint/{print $1}' "$W/mem.txt")
    printf "mem  %-8s peak %8.0f MB\n" "${NAMES[$i]}" "$(python3 -c "print(${PEAKS[$i]}/1048576.0)")"
done

echo
for i in "${!ARMS[@]}"; do
    [ "$i" -eq 0 ] && continue
    python3 -c "
t=${MINS[0]}/${MINS[$i]}; m=${PEAKS[0]}/${PEAKS[$i]}
print(f'vs ${NAMES[$i]}: time {t:.2f}x ({\"parpar faster\" if t<1 else \"parpar slower\"}), memory {m:.2f}x ({\"parpar smaller\" if m<1 else \"parpar larger\"})')"
done
