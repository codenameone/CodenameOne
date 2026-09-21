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
def run(b):
    o=subprocess.run([b],capture_output=True,text=True).stdout
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
moved=[]
for k in keys:
    b=[d[k]/1e6 for d in rb]; n=[d[k]/1e6 for d in rn]
    rat=[x/y for x,y in zip(n,b)]
    med=statistics.median(rat); spread=(max(rat)-min(rat))/min(rat)*100
    flag="!" if spread>10 else " "
    print(f"{k:20s}{statistics.median(b):10.2f}{statistics.median(n):10.2f}{med:9.3f}{spread:8.0f}%{flag}")
    if abs(med-1.0)>0.03 and spread<=10: moved.append((med,k))
print("-"*58)
print("ratio <1.000 = the working tree is FASTER.  '!' = per-round spread >10%,")
print("that row is not settled.  Rows the change cannot touch are the controls:")
print("if they are not ~1.000, the run is noise and nothing here means anything.")
if moved:
    moved.sort()
    print("\nmoved: " + ", ".join(f"{k} {m:.3f}" for m,k in moved))
else:
    print("\nnothing moved beyond 3% with a settled spread.")
PY
