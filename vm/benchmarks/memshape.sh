#!/bin/bash
# ============================================================================
# memshape.sh -- exact per-object MEMORY REPRESENTATION comparison, ParparVM
#                against HotSpot, from one driver and one source file.
#
# This is not a benchmark and it measures no time. The gap this repo is trying
# to close is 1.59x of JDK 25 on peak footprint, and a whole-program footprint
# number cannot say WHICH object shape is paying it. This can: MemShape retains
# exactly n instances of one shape and stops, so peak footprint is a straight
# line in n and its SLOPE is the deep retained cost of one instance.
#
# WHY A SLOPE AND NOT A DIFFERENCE FROM A BASELINE. A baseline subtraction
# assumes the fixed term is identical in both runs; it is not, because both VMs
# size their arena partly from how much is live. Three points and a least
# squares fit report the slope AND the residual, so a shape whose cost is not
# actually linear in n says so in the output instead of quietly returning a
# wrong number. Anything with a poor fit is printed with a warning and must not
# be quoted.
#
# WHY PEAK PHYSICAL FOOTPRINT AND NOT A HEAP REPORT. The two VMs share no
# live-bytes API: ParparVM's Runtime.freeMemory answers from the process
# footprint against physical RAM, HotSpot's from a heap carrying GC slack.
# Footprint is the one quantity both report the same way, it is what
# bench-rss-is-not-a-memory-metric requires on this platform, and it is the
# quantity the 1.59x is stated in.
#
#   ./memshape.sh                # every shape
#   ./memshape.sh strA8 alist8   # just these
#
# Environment:
#   JDK_8_HOME   JDK 8, to build JavaAPI and translate (required)
#   JDK_25_HOME  modern JDK, the HotSpot arm (required)
#   MEMSHAPE_SCALE  multiplies every shape's n (default 1)
#   MEMSHAPE_REPS   runs per point (default 3); the report shows the slope
#                   band between the min-fit and the max-fit
# ============================================================================
set -e
cd "$(dirname "$0")"
HERE="$(pwd)"
J8="${JDK_8_HOME:?set JDK_8_HOME to a JDK 8 home}"
J25="${JDK_25_HOME:?set JDK_25_HOME to a modern JDK home}"
SCALE="${MEMSHAPE_SCALE:-1}"
REPS="${MEMSHAPE_REPS:-3}"

case "$(uname -s)" in
    Darwin) ;;
    *) echo "memshape.sh currently reads peak footprint via /usr/bin/time -l (Darwin only)" >&2; exit 2 ;;
esac

# shape:n -- n chosen so the retained set is a few hundred MB, large enough that
# one instance is resolved to well under a byte and small enough to stay resident.
SHAPES=(
    "obj:2000000"
    "boxInt:2000000"
    "boxLong:2000000"
    "strA8:1000000"
    "strA32:1000000"
    "strA128:400000"
    "strCat8:1000000"
    "strCat32:600000"
    "strU8:1000000"
    "charArr8:1000000"
    "byteArr8:2000000"
    "byteArr32:1000000"
    "intArr8:1000000"
    "intArr32:500000"
    "objArr0:2000000"
    "objArr8:1000000"
    "objArr32:400000"
    "alist0:1000000"
    "alist8:600000"
    "alist32:250000"
    "hmap0:600000"
    "hmap8:200000"
    "sb0:1000000"
    "sbNarrow32:500000"
    "sbWide32:500000"
)

WANT=("$@")
selected() {
    [ ${#WANT[@]} -eq 0 ] && return 0
    for w in "${WANT[@]}"; do [ "$w" = "$1" ] && return 0; done
    return 1
}

OUT="$HERE/target/memshape"
mkdir -p "$OUT"

echo "== building ParparVM arm =="
./translate-and-build.sh MemShape "$OUT/memshape-parpar" >/dev/null

echo "== building HotSpot arm =="
rm -rf "$OUT/hs-classes"; mkdir -p "$OUT/hs-classes"
"$J25/bin/javac" -nowarn -d "$OUT/hs-classes" src/com/bench/MemShape.java 2>/dev/null

# HotSpot flags: SerialGC has the smallest and most predictable footprint of the
# collectors, and tight free ratios keep heap slack from dominating the slope.
# A generous -Xmx only caps; it does not reserve footprint.
HSFLAGS="-XX:+UseSerialGC -Xmx8g -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20"

# One run. Prints peak physical footprint in bytes, or empty on failure.
peak1() {
    local tmp
    tmp="$(mktemp "${TMPDIR:-/tmp}/memshape.XXXXXX")"
    if ! /usr/bin/time -l "$@" >"$tmp.out" 2>"$tmp"; then
        echo "  RUN FAILED: $*" >&2
        sed -n '1,12p' "$tmp.out" "$tmp" >&2
        rm -f "$tmp" "$tmp.out"
        return 1
    fi
    grep -q '^SHAPE=' "$tmp.out" || { echo "  NO OUTPUT: $*" >&2; rm -f "$tmp" "$tmp.out"; return 1; }
    awk '/peak memory footprint/ { print $1 }' "$tmp"
    rm -f "$tmp" "$tmp.out"
}

# MEMSHAPE_REPS runs of the same point; prints "min max".
#
# A SINGLE SAMPLE IS NOT A BYTE COUNT, and quoting one to two decimal places
# said otherwise. Peak footprint is an OS page-accounting high-water mark, so it
# carries collector and allocator timing; with three equally spaced points the
# least-squares slope is (y3-y1)/2h, which means an error at either END POINT
# enters the slope at half its size and the middle point cannot catch it. The
# harness therefore fits the per-point minima AND the per-point maxima and
# prints the gap between the two slopes, so every number below comes with the
# width of the band it was read from instead of an implied precision it does
# not have.
peakN() {
    local lo="" hi="" v r
    for r in $(seq 1 "$REPS"); do
        v="$(peak1 "$@")" || return 1
        [ -z "$lo" ] && lo="$v" && hi="$v"
        [ "$v" -lt "$lo" ] && lo="$v"
        [ "$v" -gt "$hi" ] && hi="$v"
    done
    echo "$lo $hi"
}

# Least squares slope through (n_i, bytes_i) with an intercept, plus the largest
# relative residual. A shape whose footprint is not linear in n is not measuring
# a per-instance cost and the caller has to be told rather than handed a number.
fit() {
    python3 -c '
import sys
pts = [tuple(map(float, p.split(","))) for p in sys.argv[1:]]
n = len(pts)
sx = sum(p[0] for p in pts); sy = sum(p[1] for p in pts)
sxx = sum(p[0]*p[0] for p in pts); sxy = sum(p[0]*p[1] for p in pts)
den = n*sxx - sx*sx
if den == 0:
    print("nan nan"); sys.exit()
m = (n*sxy - sx*sy)/den
b = (sy - m*sx)/n
worst = 0.0
for x, y in pts:
    pred = m*x + b
    if y:
        worst = max(worst, abs(y-pred)/abs(y))
print("%.3f %.4f" % (m, worst))
' "$@"
}

printf '\n%-12s %10s %10s %8s %s\n' SHAPE PARPAR HOTSPOT RATIO 'BAND / NOTE'
printf -- '---------------------------------------------------------------\n'

badfit=0
for entry in "${SHAPES[@]}"; do
    shape="${entry%%:*}"; base="${entry##*:}"
    selected "$shape" || continue
    base=$(( base * SCALE ))

    ppts=(); hpts=(); ppts_hi=(); hpts_hi=()
    ok=1
    for k in 1 2 3; do
        n=$(( base * k ))
        read -r plo phi <<<"$(peakN "$OUT/memshape-parpar" "$shape" "$n")" || { ok=0; break; }
        [ -n "$plo" ] || { ok=0; break; }
        read -r hlo hhi <<<"$(peakN "$J25/bin/java" $HSFLAGS -cp "$OUT/hs-classes" com.bench.MemShape "$shape" "$n")" || { ok=0; break; }
        [ -n "$hlo" ] || { ok=0; break; }
        ppts+=("$n,$plo"); ppts_hi+=("$n,$phi")
        hpts+=("$n,$hlo"); hpts_hi+=("$n,$hhi")
    done
    [ $ok -eq 1 ] || { printf '%-12s %10s %10s %8s %s\n' "$shape" - - - "RUN FAILED"; continue; }

    read -r pslope pres <<<"$(fit "${ppts[@]}")"
    read -r hslope hres <<<"$(fit "${hpts[@]}")"
    read -r pslope_hi _ <<<"$(fit "${ppts_hi[@]}")"
    read -r hslope_hi _ <<<"$(fit "${hpts_hi[@]}")"
    band="$(python3 -c "print('%.1f' % max(abs($pslope_hi-$pslope), abs($hslope_hi-$hslope)))")"
    ratio="$(python3 -c "print('%.2f' % ($pslope/$hslope)) if $hslope else print('inf')")"
    note="+/-$band"
    if python3 -c "import sys; sys.exit(0 if max($pres,$hres) > 0.02 else 1)"; then
        note="$note NONLINEAR residual $(python3 -c "print('%.1f%%' % (100*max($pres,$hres)))") -- do not quote"
        badfit=1
    fi
    case "$shape" in
        strU8) note="$note  CONSTRUCTION GARBAGE on the HotSpot arm: its"
               note="$note String(char[],int,int) calls StringUTF16.compress, which"
               note="$note allocates a byte[] and discards it when compression fails --"
               note="$note every iteration, for the non-Latin-1 shape only. Ours scans"
               note="$note first (toLatin1) and allocates once. NOT a representation ratio." ;;
    esac
    printf '%-12s %10s %10s %8s %s\n' "$shape" "$pslope" "$hslope" "$ratio" "$note"
done

printf -- '---------------------------------------------------------------\n'
echo "PARPAR/HOTSPOT: bytes of peak process footprint per retained instance."
[ $badfit -eq 1 ] && echo "WARNING: at least one shape did not fit a line; its number is not a per-instance cost."
exit 0
