#!/bin/bash
# A/B two builds of the VM on the microbenchmark suite, the only way a perf claim
# from this tree is trustworthy.
#
#   ab-bench.sh                 # working tree vs HEAD
#   ab-bench.sh <git-ref>       # working tree vs that ref
#   ab-bench.sh <ref> <rounds>  # default 5 rounds
#
# WHY THIS EXISTS. Two conclusions in one session were wrong because a number was
# compared against a baseline from a DIFFERENT configuration:
#
#   - a "49% regression" that compared a commit against code ~70 revisions older,
#     because the baseline was read off a session-opening git status instead of
#     `git rev-parse <commit>^`;
#   - a "17.6% win" that compared a run-matrix.sh run (cores pinned through
#     CN1_GC_MARK_THREADS) against run-benchmark.sh at default cores.
#
# Both binaries here are built from the SAME tree by the SAME script, run
# interleaved with alternating order, and scored on the MEDIAN OF PER-ROUND
# PAIRED RATIOS -- so machine drift moves both arms of a pair together and
# divides out. An identical binary measured intArithmetic at 56.7ms and 88.8ms an
# hour apart on this host; that is the error unpaired comparison admits.
#
# CONTROLS ARE PRINTED, NOT ASSUMED. Benchmarks the change cannot plausibly touch
# must land at ~1.000. If they do not, the run is noise and no row means anything
# -- which is the check that turned a claimed 17.6% into a measured 9.7%.
set -e
cd "$(dirname "$0")"
REF="${1:-HEAD}"
ROUNDS="${2:-5}"
REPO="$(cd ../.. && pwd)"
SRC="$REPO/vm/ByteCodeTranslator/src"
OUT="target/ab"; mkdir -p "$OUT"
export CN1_BENCH_CFLAGS="${CN1_BENCH_CFLAGS--flto=thin}"

DIRTY=$(git -C "$REPO" status --porcelain -- vm/ | wc -l | tr -d ' ')
echo "== A/B: working tree vs $REF ($(git -C "$REPO" rev-parse --short "$REF")), $ROUNDS rounds =="
[ "$DIRTY" = "0" ] && echo "   NOTE: working tree is CLEAN -- both arms are the same code, expect all ratios ~1.000"

# Baseline: the ref's vm/ sources, staged in a scratch copy so the working tree
# is never modified (this repo carries stashes; nothing here may disturb them).
BASESRC="$OUT/base-src"; rm -rf "$BASESRC"; mkdir -p "$BASESRC"
git -C "$REPO" archive "$REF" vm/ByteCodeTranslator/src vm/JavaAPI/src | tar -x -C "$BASESRC"
SAVE="$OUT/worktree-src"; rm -rf "$SAVE"; mkdir -p "$SAVE"
cp -R "$SRC" "$SAVE/src"
cp -R "$REPO/vm/JavaAPI/src" "$SAVE/japi"

build() { ./translate-and-build.sh Bench "$1" >/dev/null 2>&1 || { echo "BUILD FAILED: $1"; exit 1; }; }
restore_worktree() {
    rm -rf "$SRC" "$REPO/vm/JavaAPI/src"
    cp -R "$SAVE/src" "$SRC"; cp -R "$SAVE/japi" "$REPO/vm/JavaAPI/src"
}
trap restore_worktree EXIT

echo "   building baseline ($REF)..."
rm -rf "$SRC" "$REPO/vm/JavaAPI/src"
cp -R "$BASESRC/vm/ByteCodeTranslator/src" "$SRC"
cp -R "$BASESRC/vm/JavaAPI/src" "$REPO/vm/JavaAPI/src"
build "$PWD/$OUT/bench-base"
echo "   building working tree..."
restore_worktree
build "$PWD/$OUT/bench-new"

python3 - "$PWD/$OUT/bench-base" "$PWD/$OUT/bench-new" "$ROUNDS" <<'PY'
import subprocess,re,sys,statistics
base,new,rounds=sys.argv[1],sys.argv[2],int(sys.argv[3])
# DEEP_REPS is the depth an unsettled row is RE-measured at, and it is not a
# tuning knob: five reps is too few for objectAllocation to settle at all. Its
# live set is tiny and a rep lasts ~34ms, so only a handful of GC cycles fit in
# the measured window and whether one lands inside it decides the number --
# 21.95ms to 50.56ms was measured for provably identical work. min-of-5 is then
# itself a noisy order statistic (four processes put the floor at 24.07 / 28.03 /
# 21.95 / 22.14, a 27.7% spread), which is how one A/B scored that row 0.861 and
# the next 1.167. Twenty-five CONSECUTIVE reps reach a sustained steady state and
# six processes agreed to 0.80%. The depth must be consecutive: a min over more
# SEPARATE rounds does not converge, it drifts downward, because a minimum over
# independent samples is monotonically non-increasing.
DEEP_REPS=25

# A fixed rep count cannot settle every row, because the rows differ in DURATION,
# not just in variance. valueEscape runs in ~4ms, close enough to scheduling and
# timer resolution that 25 reps still left it at +-10%. So the depth is chosen to
# give every deep measurement the same wall-time budget: reps = BUDGET / observed
# median, floored at DEEP_REPS. objectAllocation (~31ms) stays near 25; a 4ms row
# gets ~125. Capped so a pathologically fast row cannot ask for a million reps.
DEEP_BUDGET_MS=800
DEEP_REPS_MAX=400

def deep_reps(observed_ms):
    if observed_ms <= 0:
        return DEEP_REPS
    return max(DEEP_REPS, min(DEEP_REPS_MAX, int(DEEP_BUDGET_MS / observed_ms)))

def run(b,reps=None,only=None):
    cmd=[b]
    if reps is not None:
        cmd.append(str(reps))
        if only is not None: cmd.append(only)
    o=subprocess.run(cmd,capture_output=True,text=True).stdout
    d={}
    for m in re.finditer(r'BENCH (\w+) rep \d+ ns=(\d+)',o):
        k,v=m.group(1),int(m.group(2)); d[k]=min(d.get(k,1<<62),v)
    return d
rb,rn=[],[]
for r in range(rounds):
    if r%2==0: rb.append(run(base)); rn.append(run(new))
    else:      rn.append(run(new));  rb.append(run(base))
keys=sorted(set(rb[0])&set(rn[0]))
print(f"\n{'benchmark':20s}{'base ms':>10s}{'new ms':>10s}{'ratio':>9s}{'spread':>9s}")
print("-"*58)
moved=[]; unsettled=[]
for k in keys:
    b=[d[k]/1e6 for d in rb]; n=[d[k]/1e6 for d in rn]
    rat=[x/y for x,y in zip(n,b)]
    med=statistics.median(rat); spread=(max(rat)-min(rat))/min(rat)*100
    flag="!" if spread>10 else " "
    print(f"{k:20s}{statistics.median(b):10.2f}{statistics.median(n):10.2f}{med:9.3f}{spread:8.0f}%{flag}")
    if abs(med-1.0)>0.03 and spread<=10: moved.append((med,k))
    # Deepen a row that is flagged OR that appears to have MOVED. The '!' trigger
    # alone is not sufficient: stringBuilding scored 0.964 in one run and 1.069 in
    # the next while BOTH self-reported 3-4% spread and neither was flagged, so a
    # row can be unreproducible without ever tripping the threshold. Anything that
    # looks like a result gets re-measured before it is believed.
    if flag=="!" or abs(med-1.0)>0.03: unsettled.append(k)
print("-"*58)
print("ratio <1.000 = the working tree is FASTER.  '!' = per-round spread >10%,")
print("that row is not settled.  Rows the change cannot touch are the controls:")
print("if they are not ~1.000, the run is noise and nothing here means anything.")
# An unsettled row is re-measured at depth rather than reported as a number
# nobody can use. Only the flagged rows are re-run, so the deeper rep count is
# never charged to the other benchmarks.
if unsettled:
    print(f"\nre-measuring {len(unsettled)} unsettled row(s) at {DEEP_REPS} reps: "
          + ", ".join(unsettled))
    print("  (deep runs execute that benchmark ALONE, so it starts from a different")
    print("   heap state than in-suite -- compare the RATIO, never these absolute ms")
    print("   against the table above: measured 19.9ms alone vs 28.6ms in-suite.)")
    for k in unsettled:
        seen=[d[k]/1e6 for d in rb+rn if k in d]
        reps=deep_reps(statistics.median(seen)) if seen else DEEP_REPS
        db=[run(base,reps,k) for _ in range(3)]
        dn=[run(new,reps,k) for _ in range(3)]
        if not all(k in d for d in db+dn):
            print(f"  {k:18s} deep re-run produced no samples -- SKIPPED")
            continue
        bv=[d[k]/1e6 for d in db]; nv=[d[k]/1e6 for d in dn]
        rat=statistics.median(nv)/statistics.median(bv)
        # Spread is WITHIN each arm, never across the pooled set. Pooling them
        # subtracts nothing and adds the real base-vs-new difference to the
        # number, so a row where the change genuinely moved time reports a huge
        # "spread" and is declared unsettled no matter how stable each arm is.
        spb=(max(bv)-min(bv))/min(bv)*100
        spn=(max(nv)-min(nv))/min(nv)*100
        sp=max(spb,spn)
        state="settled" if sp<=10 else "STILL UNSETTLED"
        print(f"  {k:18s} base {statistics.median(bv):7.2f}ms (+-{spb:.0f}%)"
              f"  new {statistics.median(nv):7.2f}ms (+-{spn:.0f}%)"
              f"  ratio {rat:.3f}  {reps} reps  {state}")

if moved:
    moved.sort()
    print("\nmoved: " + ", ".join(f"{k} {m:.3f}" for m,k in moved))
else:
    print("\nnothing moved beyond 3% with a settled spread.")
PY
