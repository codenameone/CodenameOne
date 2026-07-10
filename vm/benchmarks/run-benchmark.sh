#!/bin/bash
# Runs the ParparVM performance benchmark against a host JVM reference:
# best-of-N interleaved rounds, per-benchmark ratio table, geomean, and a
# bit-identical checksum cross-check (any checksum mismatch is a VM bug, not
# a perf number).
#
#   run-benchmark.sh [rounds]           (default 5)
#
# Requirements: JDK_8_HOME (build), BENCH_JAVA or `java` on PATH (the reference
# JVM to compare against -- use the newest JDK you care about, warmed by the
# harness's in-process repetitions), Maven, clang.
#
# The release shape is ThinLTO; set CN1_BENCH_CFLAGS="" to measure without it.
set -e
cd "$(dirname "$0")"
ROUNDS="${1:-5}"
REF_JAVA="${BENCH_JAVA:-java}"
export CN1_BENCH_CFLAGS="${CN1_BENCH_CFLAGS--flto=thin}"

OUT="target/bench-bin"
mkdir -p target
./translate-and-build.sh Bench "$OUT"

# host-JVM reference classes
J8="${JDK_8_HOME:?set JDK_8_HOME}"
mkdir -p target/host-classes
"$J8/bin/javac" -nowarn -encoding UTF-8 -d target/host-classes src/com/bench/Bench.java

python3 - "$OUT" "$REF_JAVA" "$ROUNDS" <<'EOF'
import subprocess, re, math, sys
binpath, ref_java, rounds = sys.argv[1], sys.argv[2], int(sys.argv[3])

def parse(out):
    r = {}
    for m in re.finditer(r'BENCH (\w+) rep \d+ ns=(\d+) checksum=(-?\d+)', out):
        r.setdefault(m.group(1), {"ns": [], "ck": set()})
        r[m.group(1)]["ns"].append(int(m.group(2)))
        r[m.group(1)]["ck"].add(m.group(3))
    return r

best_p, best_h, cks_p, cks_h = {}, {}, {}, {}
for rnd in range(rounds):
    p = parse(subprocess.run([binpath], capture_output=True, text=True).stdout)
    h = parse(subprocess.run([ref_java, '-cp', 'target/host-classes', 'com.bench.Bench'],
                             capture_output=True, text=True).stdout)
    for k, v in p.items():
        best_p[k] = min(best_p.get(k, 1 << 62), min(v["ns"]))
        cks_p.setdefault(k, set()).update(v["ck"])
    for k, v in h.items():
        best_h[k] = min(best_h.get(k, 1 << 62), min(v["ns"]))
        cks_h.setdefault(k, set()).update(v["ck"])
    print(f"round {rnd + 1}/{rounds} done", flush=True)

bad = [k for k in best_p if cks_p.get(k) != cks_h.get(k)]
if bad:
    print(f"\nCHECKSUM MISMATCH (VM bug, ratios are meaningless): {bad}")
    sys.exit(1)

ratios = []
print(f"\n{'bench':<20}{'parpar ms':>10}{'host ms':>10}{'ratio':>7}")
for n in best_p:
    p, h = best_p[n] / 1e6, best_h[n] / 1e6
    ratios.append(p / h)
    print(f"{n:<20}{p:>10.1f}{h:>10.1f}{p/h:>7.2f}")
g = math.exp(sum(math.log(r) for r in ratios) / len(ratios))
print(f"\nGEOMEAN {g:.2f}x   (all checksums bit-identical to the host JVM)")
EOF
