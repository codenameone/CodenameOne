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
# Memory is a PEAK, and which peak depends on the platform -- so every line that
# reports one says which metric produced it, and so does the ratio line.
#
#   Darwin  peak phys_footprint, from /usr/bin/time -l. The same quantity vmmap
#           calls "Physical footprint (peak)", and the one vm/CLAUDE.md requires.
#   Linux   peak RSS (VmHWM), from /usr/bin/time -v or getrusage. Linux has no
#           phys_footprint; RSS is the closest peak it exposes.
#
# The two are NOT the same quantity and a Darwin figure must never be compared to
# a Linux one. Comparing arms WITHIN one run is fine and is all this script does:
# every arm on a given host is measured the same way.
#
# NEVER ps rss: vm/CLAUDE.md records 151/207/219 MB measured for one unchanged
# binary. /usr/bin/time's peak is a high-water mark the kernel keeps, which is a
# different and reproducible thing.
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
CLASSES="${1:?usage: bench-selfhost.sh <classesDir> <AppName> <package> [rounds]}"
APP="${2:?}"; PKG="${3:?}"; ROUNDS="${4:-5}"

T="$REPO/vm/selfhost/target"
# -O3 -flto=thin is the documented release shape (vm/benchmarks/README.md);
# CN1_SELFHOST_BIN overrides it for an A/B against the -O1 diff-gate build.
# The binary the GATES verify, not a separate optimised build nobody refreshes.
# It defaulted to $T/parpar-O3, which build-selfhost.sh does not produce and
# nothing kept current: the bench spent four runs comparing a months-old binary
# against a current JVM translator and correctly refusing to print a ratio, while
# Gate A passed byte-identical on the same corpus. Benchmarking a binary the gates
# have not verified cannot produce a meaningful number.
PARPAR="${CN1_SELFHOST_BIN:-$T/parpar}"
JAPI="$T/javaapi-classes"
TR="$REPO/vm/ByteCodeTranslator/target/classes"
ASM="$(cat "$REPO/vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt")"
# Reference JVMs, resolved rather than hard-coded. A developer-specific absolute
# path here meant anyone else running the documented command died under `set -e`
# while BUILDING the arm list, before a single measurement -- the benchmark was
# runnable by one machine.
#   SELFHOST_REF_JAVAS  explicit comma-separated list, wins outright
#   JDK_25_HOME         a modern JDK to compare against
#   java on PATH        whatever this shell would run
cn1_first_java() {
    for c in "${JDK_25_HOME:-}/bin/java" "$(command -v java 2>/dev/null || true)"; do
        [ -n "$c" ] && [ -x "$c" ] && { echo "$c"; return; }
    done
}
DEFAULT_JAVAS="$(cn1_first_java),${JDK_8_HOME:-}/bin/java"
IFS=',' read -r -a REF_JAVAS <<< "${SELFHOST_REF_JAVAS:-$DEFAULT_JAVAS}"

W="$T/bench"; rm -rf "$W"; mkdir -p "$W"

# --- peak-memory probe, resolved per platform -----------------------------------
# cn1_time_peak runs a command under the platform's resource-usage wrapper and
# echoes the peak in BYTES. It must not be silently skipped: a missing probe used
# to leave PEAKS empty, which turns the ratio line into a division by nothing.
case "$(uname -s)" in
    Darwin)
        CN1_MEM_METRIC="peak phys_footprint"
        # BSD time -l; the figure is already in bytes.
        cn1_time_peak() { local o=$1; shift
            /usr/bin/time -l "$@" 2>"$o" >/dev/null || true
            awk '/peak memory footprint/{print $1; found=1} END{if(!found) print ""}' "$o"; }
        ;;
    *)
        CN1_MEM_METRIC="peak RSS (VmHWM)"
        # GNU time -v reports "Maximum resident set size (kbytes)".
        cn1_time_peak() { local o=$1; shift
            /usr/bin/time -v "$@" 2>"$o" >/dev/null || true
            awk -F: '/Maximum resident set size/{gsub(/ /,"",$2); print $2*1024; found=1}
                     END{if(!found) print ""}' "$o"; }
        ;;
esac
# Prove the probe works before measuring anything with it. A probe that answers
# nothing reports every arm as 0 bytes, and 0/0 is not a regression, it is a bug
# wearing a green tick.
if [ -z "$(cn1_time_peak "$W/probe.txt" /bin/sh -c 'exit 0')" ]; then
    echo "REFUSING: no peak-memory probe on this platform ($(uname -s))."
    echo "  /usr/bin/time did not report $CN1_MEM_METRIC. Install GNU time (Debian:"
    echo "  apt-get install -y time) or teach cn1_time_peak this platform."
    exit 1
fi

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
# Names have to be UNIQUE, not merely descriptive: every tree, log and diff is filed
# under one, so two arms sharing a label make the second `mv` land inside the first
# arm's directory and the correctness check then compares a tree against itself
# nested one level down -- a divergence that is an artefact of naming. Two arms
# collide easily now that JDK 25 is discovered rather than hard-coded: with no
# JDK_25_HOME the PATH fallback and JDK_8_HOME can both be Java 8.
for a in "${ARMS[@]}"; do
    base="$(label "$a")"
    name="$base"; k=2
    for prev in "${NAMES[@]}"; do
        if [ "$prev" = "$name" ]; then name="${base}#${k}"; k=$((k+1)); fi
    done
    NAMES+=("$name")
done
# Say which executable each arm actually is, so a "#2" suffix is never a mystery.
for i in "${!ARMS[@]}"; do echo "arm    : ${NAMES[$i]} -> ${ARMS[$i]}"; done
echo "corpus : $CLASSES"
echo "arms   : ${NAMES[*]}    rounds: $ROUNDS"
echo "memory : $CN1_MEM_METRIC"
# STALENESS: a benchmark of yesterday's binary is worse than no benchmark, because
# the number looks like a measurement. Refuse rather than warn.
if [ -n "$(find "$REPO/vm/ByteCodeTranslator/src" "$REPO/vm/JavaAPI/src" -type f -newer "$PARPAR" -print -quit 2>/dev/null)" ]; then
    echo "REFUSING: $PARPAR is older than the translator/JavaAPI sources it was built from."
    echo "  run vm/selfhost/build-selfhost.sh first."
    exit 1
fi

# --- correctness precondition: every arm must emit the same C -------------------
# Same absolute output path for all arms, sequentially, because the generated
# CMakeLists embeds srcRoot.getAbsolutePath().
OUT="$W/out"
for i in "${!ARMS[@]}"; do
    mkdir -p "$OUT"
    invoke "${ARMS[$i]}" "$OUT" > "$W/${NAMES[$i]}.log" 2>&1 || { echo "${NAMES[$i]} FAILED"; tail -5 "$W/${NAMES[$i]}.log"; exit 1; }
    # CLEAR the destination first. `mv` into an EXISTING directory nests the new
    # tree inside it (tree-parpar/out/...) and leaves the previous run's dist/ in
    # place, so the correctness check below compares a stale tree against a fresh
    # one and reports a divergence that is pure harness. Observed: a tree-parpar
    # left by an earlier session made every later run declare the VM divergent
    # while Gate A passed byte-identical on the same corpus.
    rm -rf "$W/tree-${NAMES[$i]}"
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
#
# Sampled in EVERY round and reduced with max, because the header promises the
# maximum of N samples and a peak is a max. Measuring each arm once let a single
# noisy run decide the reported ratio, which is the same mistake as quoting a
# memory figure from one process: the number looked like a measurement and was a
# sample.
declare -a PEAKS PEAK_MAX
for i in "${!ARMS[@]}"; do PEAK_MAX[$i]=0; done
for round in $(seq 1 "$ROUNDS"); do
for i in "${!ARMS[@]}"; do
    rm -rf "$W/run"; mkdir -p "$W/run"
    if [ "${ARMS[$i]}" = parpar ]; then
        PEAKS[$i]=$(env CN1_RESOURCE_PATH="$REPO/vm/ByteCodeTranslator/src" \
            cn1_time_peak "$W/mem.txt" "$PARPAR" \
            clean "$JAPI;$CLASSES" "$W/run" "$APP" "$PKG" "$APP" 1.0 clean none)
    else
        PEAKS[$i]=$(cn1_time_peak "$W/mem.txt" "${ARMS[$i]}" -cp "$TR:$ASM" \
            com.codename1.tools.translator.ByteCodeTranslator \
            clean "$JAPI;$CLASSES" "$W/run" "$APP" "$PKG" "$APP" 1.0 clean none)
    fi
    if [ -z "${PEAKS[$i]}" ]; then
        echo "REFUSING: the ${NAMES[$i]} arm produced no $CN1_MEM_METRIC reading."
        tail -5 "$W/mem.txt"
        exit 1
    fi
    printf "mem  %-8s round %d %s %8.0f MB\n" "${NAMES[$i]}" "$round" "$CN1_MEM_METRIC" \
        "$(python3 -c "print(${PEAKS[$i]}/1048576.0)")"
    [ "${PEAKS[$i]}" -gt "${PEAK_MAX[$i]}" ] && PEAK_MAX[$i]="${PEAKS[$i]}"
done
done
for i in "${!ARMS[@]}"; do
    PEAKS[$i]="${PEAK_MAX[$i]}"
    printf "mem  %-8s MAX over %d round(s) %8.0f MB  (%s)\n" "${NAMES[$i]}" "$ROUNDS" \
        "$(python3 -c "print(${PEAKS[$i]}/1048576.0)")" "$CN1_MEM_METRIC"
done

echo
for i in "${!ARMS[@]}"; do
    [ "$i" -eq 0 ] && continue
    python3 -c "
t=${MINS[0]}/${MINS[$i]}; m=${PEAKS[0]}/${PEAKS[$i]}
print(f'vs ${NAMES[$i]}: time {t:.2f}x ({\"parpar faster\" if t<1 else \"parpar slower\"}), memory {m:.2f}x ({\"parpar smaller\" if m<1 else \"parpar larger\"}) [${CN1_MEM_METRIC}]')"
done
