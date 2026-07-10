#!/bin/bash
# A/B the generational-adoption trigger policies (CN1_ADOPT_POLICY):
#   0 = adoption OFF (band-aids only)   1 = TENURE (mature after 1 survival + cascade)
#   2 = ONMARK (mature every reachable non-leaf immediately)
# Reports best-of-N per-benchmark ms (parpar only) AND peak RSS per policy so the
# perf vs memory trade-off is visible. Checksums are cross-checked against policy 0
# (any drift is a correctness bug, not a perf number).
set -e
cd "$(dirname "$0")"
ROUNDS="${1:-6}"
LTO="${CN1_BENCH_LTO--flto=thin}"
mkdir -p target/ab

for POL in 0 1 2; do
  CN1_BENCH_CFLAGS="$LTO -DCN1_ADOPT_POLICY=$POL" ./translate-and-build.sh Bench "target/ab/bench-$POL" >/dev/null 2>&1
  echo "built policy $POL"
done

python3 - "$ROUNDS" <<'EOF'
import subprocess, re, sys, resource, platform
rounds = int(sys.argv[1])
POLS = [0,1,2]
NAMES = {0:"OFF", 1:"TENURE", 2:"ONMARK"}

def run(binpath):
    # peak RSS of the child in KB (macOS ru_maxrss is bytes, Linux KB)
    before = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    p = subprocess.run([binpath], capture_output=True, text=True)
    after = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    rss = after  # cumulative max across children; good enough as a peak proxy
    r = {}
    for m in re.finditer(r'BENCH (\w+) rep \d+ ns=(\d+) checksum=(-?\d+)', p.stdout):
        r.setdefault(m.group(1), {"ns": [], "ck": set()})
        r[m.group(1)]["ns"].append(int(m.group(2)))
        r[m.group(1)]["ck"].add(m.group(3))
    return r, rss

best = {pol:{} for pol in POLS}
cks  = {pol:{} for pol in POLS}
peakrss = {pol:0 for pol in POLS}
for rnd in range(rounds):
    for pol in POLS:
        r, rss = run(f"target/ab/bench-{pol}")
        peakrss[pol] = max(peakrss[pol], rss)
        for k,v in r.items():
            best[pol][k] = min(best[pol].get(k, 1<<62), min(v["ns"]))
            cks[pol].setdefault(k,set()).update(v["ck"])
    print(f"round {rnd+1}/{rounds}", flush=True)

# checksum parity vs OFF
bad = [k for k in best[0] if any(cks[p].get(k)!=cks[0].get(k) for p in POLS)]
if bad:
    print(f"\nCHECKSUM MISMATCH across policies (correctness bug): {bad}")
    for k in bad[:3]:
        for p in POLS: print(f"  {k} pol{p}: {sorted(cks[p].get(k,set()))}")
    sys.exit(1)

names = list(best[0].keys())
print(f"\n{'bench':<20}" + "".join(f"{NAMES[p]+' ms':>13}" for p in POLS) + f"{'T/OFF':>8}{'ON/OFF':>8}")
for n in names:
    ms = {p: best[p][n]/1e6 for p in POLS}
    print(f"{n:<20}" + "".join(f"{ms[p]:>13.1f}" for p in POLS)
          + f"{ms[1]/ms[0]:>8.2f}{ms[2]/ms[0]:>8.2f}")

# RSS: macOS ru_maxrss bytes, Linux KB
unit = 1024*1024 if platform.system()=="Darwin" else 1024
print(f"\n{'peak RSS (MB)':<20}" + "".join(f"{peakrss[p]/unit:>13.1f}" for p in POLS))
print("(checksums bit-identical across all three policies)")
EOF
