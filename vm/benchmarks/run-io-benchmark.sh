#!/bin/bash
# ParparVM vs a reference JVM on FILE I/O, using the translator's own API shapes.
#
# Why this exists: the self-hosting profile spends 11.8% of mutator time in
# open/write/close, and that was waved away as "HotSpot does the same I/O" without
# ever being measured. The split inside that 11.8% argues otherwise -- open 7.0%,
# close 3.9%, bulk write not in the top ten -- which points at the per-file syscall
# path rather than at moving bytes.
#
#   run-io-benchmark.sh [rounds]          (default 5)
#
# Arms are interleaved within each round (sequential A-then-B carries a thermal bias
# on this hardware), time is best-of-N, and the checksum cross-check is kept: a VM
# that reads or writes the wrong bytes must not be allowed to post a fast number.
#
# EACH ARM GETS ITS OWN DIRECTORY. Sharing one would let the first arm warm the page
# cache for the second, which on a read benchmark is the whole measurement.
set -e
cd "$(dirname "$0")"
ROUNDS="${1:-5}"
REF_JAVA="${BENCH_JAVA:-java}"
export CN1_BENCH_CFLAGS="${CN1_BENCH_CFLAGS--flto=thin}"

OUT="target/iobench-bin"
mkdir -p target
./translate-and-build.sh IoBench "$OUT"

J8="${JDK_8_HOME:?set JDK_8_HOME}"
mkdir -p target/io-host-classes
"$J8/bin/javac" -nowarn -encoding UTF-8 -d target/io-host-classes src/com/bench/IoBench.java

python3 - "$OUT" "$REF_JAVA" "$ROUNDS" <<'PY'
import subprocess, re, sys, os, shutil, tempfile
binpath, ref_java, rounds = sys.argv[1], sys.argv[2], int(sys.argv[3])
binpath = os.path.abspath(binpath)

def verify(tag, out):
    # A write arm that buffers can post a perfect checksum while dropping bytes on
    # close; the in-VM read-back is what rules that out. Absent line = treat as failure,
    # because "the check did not run" must not read as "the check passed".
    if 'IOBENCH VERIFY OK' not in out:
        bad = [l for l in out.splitlines() if 'IOBENCH VERIFY' in l]
        print(f"{tag}: CONTENT VERIFICATION FAILED -- {bad if bad else 'no verify line emitted'}")
        return False
    return True

def parse(out):
    r = {}
    for m in re.finditer(r'BENCH (\w+) rep \d+ ns=(\d+) checksum=(-?\d+)', out):
        r.setdefault(m.group(1), {"ns": [], "ck": set()})
        r[m.group(1)]["ns"].append(int(m.group(2)))
        r[m.group(1)]["ck"].add(m.group(3))
    return r

best_p, best_h, cks_p, cks_h, order = {}, {}, {}, {}, []
for rnd in range(rounds):
    pdir = tempfile.mkdtemp(prefix='iob-p-')
    hdir = tempfile.mkdtemp(prefix='iob-h-')
    try:
        praw = subprocess.run([binpath, pdir], capture_output=True, text=True).stdout
        hraw = subprocess.run([ref_java, '-cp', 'target/io-host-classes',
                               'com.bench.IoBench', hdir],
                              capture_output=True, text=True).stdout
        if not verify('parpar', praw) or not verify('jvm', hraw):
            sys.exit(1)
        p = parse(praw)
        h = parse(hraw)
    finally:
        shutil.rmtree(pdir, ignore_errors=True)
        shutil.rmtree(hdir, ignore_errors=True)
    for k, v in p.items():
        if k not in order: order.append(k)
        best_p[k] = min(best_p.get(k, 1 << 62), min(v["ns"]))
        cks_p.setdefault(k, set()).update(v["ck"])
    for k, v in h.items():
        best_h[k] = min(best_h.get(k, 1 << 62), min(v["ns"]))
        cks_h.setdefault(k, set()).update(v["ck"])
    print(f"round {rnd + 1}/{rounds} done", flush=True)

if not best_p or not best_h:
    print("NO RESULTS -- one arm produced no BENCH lines")
    sys.exit(1)
bad = [k for k in best_p if cks_p.get(k) != cks_h.get(k)]
if bad:
    print(f"\nCHECKSUM MISMATCH (VM bug, ratios are meaningless): {bad}")
    print({k: (cks_p.get(k), cks_h.get(k)) for k in bad})
    sys.exit(1)

print(f"\n{'benchmark':18s} {'parpar ms':>11s} {'jvm ms':>10s} {'ratio':>8s}")
worst = None
for k in order:
    if k not in best_h: continue
    pm, hm = best_p[k] / 1e6, best_h[k] / 1e6
    ratio = best_p[k] / best_h[k]
    flag = '  <-- slower' if ratio > 1.15 else ''
    print(f"{k:18s} {pm:11.2f} {hm:10.2f} {ratio:7.3f}x{flag}")
    if worst is None or ratio > worst[1]: worst = (k, ratio)
print(f"\nworst arm: {worst[0]} at {worst[1]:.3f}x")
print("ratios are best-of-N per arm; >1.0 means ParparVM is slower than the JVM")
PY
