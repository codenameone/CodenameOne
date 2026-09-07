#!/bin/bash
# A/B the tagged immediates ("poor man's Valhalla") in three arms, so the two questions
# are separated rather than conflated:
#
#   0 HEAP  -DCN1_DISABLE_TAGGED_INT     nothing tagged; every valueOf allocates
#   1 INT   -DCN1_DISABLE_TAGGED_VALUES  Integer only -- what shipped before this work
#   2 ALL   (default)                    Integer, Long, Double, Float, Character, Short
#
# Arm 1 is the baseline that matters: arm 0 vs 1 re-measures a win that was already taken,
# and only 1 vs 2 says what the five extra types are worth.
#
# Reports best-of-N per-benchmark ms and peak RSS, and cross-checks every checksum against
# arm 0 -- a divergence is a correctness bug, not a perf number, and exits non-zero.
#
# Usage: ab-tagged.sh [rounds] [driver]     (driver defaults to BoxBench; Bench also works)
set -e
cd "$(dirname "$0")"
ROUNDS="${1:-6}"
DRIVER="${2:-BoxBench}"
LTO="${CN1_BENCH_LTO--flto=thin}"
mkdir -p target/ab

build() { # arm, extra cflags
  CN1_BENCH_CFLAGS="$LTO $2" ./translate-and-build.sh "$DRIVER" "target/ab/tag-$1" >/dev/null 2>&1
  echo "built arm $1 ($DRIVER)"
}
build 0 "-DCN1_DISABLE_TAGGED_INT"
build 1 "-DCN1_DISABLE_TAGGED_VALUES"
build 2 ""

python3 - "$ROUNDS" <<'EOF'
import subprocess, re, sys, resource, platform
rounds = int(sys.argv[1])
ARMS = [0, 1, 2]
NAMES = {0: "HEAP", 1: "INT", 2: "ALL"}

def run(binpath):
    before = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    p = subprocess.run([binpath], capture_output=True, text=True)
    after = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    r, cov = {}, None
    for m in re.finditer(r'BENCH (\w+) rep \d+ ns=(\d+) checksum=(-?\d+)', p.stdout):
        r.setdefault(m.group(1), {"ns": [], "ck": set()})
        r[m.group(1)]["ns"].append(int(m.group(2)))
        r[m.group(1)]["ck"].add(m.group(3))
    c = re.search(r'^COVERAGE .*$', p.stdout, re.M)
    if c:
        cov = c.group(0)
    return r, after, cov

best = {a: {} for a in ARMS}
cks = {a: {} for a in ARMS}
peakrss = {a: 0 for a in ARMS}
covs = {}
for rnd in range(rounds):
    for a in ARMS:
        r, rss, cov = run(f"target/ab/tag-{a}")
        peakrss[a] = max(peakrss[a], rss)
        if cov:
            covs[a] = cov
        for k, v in r.items():
            best[a][k] = min(best[a].get(k, 1 << 62), min(v["ns"]))
            cks[a].setdefault(k, set()).update(v["ck"])
    print(f"round {rnd+1}/{rounds}", flush=True)

bad = [k for k in best[0] if any(cks[x].get(k) != cks[0].get(k) for x in ARMS)]
if bad:
    print(f"\nCHECKSUM MISMATCH across arms (correctness bug, not a perf result): {bad}")
    for k in bad[:3]:
        for x in ARMS:
            print(f"  {k} arm{x}: {sorted(cks[x].get(k, set()))}")
    sys.exit(1)

names = list(best[0].keys())
print(f"\n{'bench':<20}" + "".join(f"{NAMES[x]+' ms':>12}" for x in ARMS)
      + f"{'INT/HEAP':>10}{'ALL/INT':>10}")
for n in names:
    ms = {x: best[x][n] / 1e6 for x in ARMS}
    print(f"{n:<20}" + "".join(f"{ms[x]:>12.1f}" for x in ARMS)
          + f"{ms[1]/ms[0]:>10.2f}{ms[2]/ms[1]:>10.2f}")

unit = 1024 * 1024 if platform.system() == "Darwin" else 1024
print(f"\n{'peak RSS (MB)':<20}" + "".join(f"{peakrss[x]/unit:>12.1f}" for x in ARMS))
for x in ARMS:
    if x in covs:
        print(f"arm {NAMES[x]}: {covs[x]}")
print("(checksums bit-identical across all three arms)")
EOF
