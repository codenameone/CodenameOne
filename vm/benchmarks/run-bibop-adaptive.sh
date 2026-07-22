#!/bin/bash
# Correctness + policy validation + performance/RAM A/B for the sustained
# survivor-heavy small-array shape. Collector flags below are QA controls only;
# production always uses the single adaptive BiBOP behavior.
set -euo pipefail
cd "$(dirname "$0")"

ROUNDS="${1:-3}"
LTO="${CN1_BENCH_LTO--flto=thin}"
mkdir -p target/bibop-adaptive

build() {
    local name="$1"
    shift
    CN1_BENCH_CFLAGS="$LTO $*" ./translate-and-build.sh BiBopAdaptive \
        "target/bibop-adaptive/$name" >/dev/null
    echo "built $name"
}

build adaptive-instrument -DCN1_GC_INSTRUMENT
build adaptive
build legacy -DCN1_DISABLE_BIBOP
build no-pacing -DCN1_BIBOP_NO_PACING

python3 - "$ROUNDS" <<'PY'
import json
import os
import re
import subprocess
import sys

rounds = int(sys.argv[1])
root = os.path.join("target", "bibop-adaptive")
bins = {name: os.path.join(root, name) for name in
        ("adaptive", "legacy", "no-pacing")}

diag = subprocess.run([os.path.join(root, "adaptive-instrument")],
                      stdout=subprocess.PIPE, stderr=subprocess.PIPE)
if diag.returncode != 0:
    sys.stderr.buffer.write(diag.stderr)
    raise SystemExit("instrumented adaptive workload failed")
lines = diag.stderr.decode("utf-8", "replace").splitlines()
adapt = [line for line in lines if line.startswith("[BIBOP-ADAPT]")]
if not adapt:
    raise SystemExit("no BiBOP adaptive diagnostics were emitted")

def maximum(field):
    values = []
    for line in adapt:
        match = re.search(r"\b%s=([0-9.]+)" % re.escape(field), line)
        if match:
            values.append(float(match.group(1)))
    return max(values) if values else 0.0

def last(field):
    values = []
    for line in adapt:
        match = re.search(r"\b%s=([0-9.]+)" % re.escape(field), line)
        if match:
            values.append(float(match.group(1)))
    return values[-1] if values else 0.0

checks = {
    "thread throughput promotion": maximum("promotions") >= 1,
    "dynamic trigger growth": maximum("triggerMB") > 24.0,
    "dynamic trigger contraction": last("triggerMB") <= 24.0,
    "survivor legacy bypass": maximum("bypass") >= 1,
    "legacy bypass allocations": maximum("bypassAllocs") >= 1,
    "fresh-page grace scan": maximum("freshPages") >= 1,
    "no unconditional mark belt": maximum("beltRuns") == 0,
}
failed = [name for name, ok in checks.items() if not ok]
if failed:
    print("last adaptive diagnostics:", file=sys.stderr)
    for line in adapt[-8:]:
        print(line, file=sys.stderr)
    raise SystemExit("adaptive policy checks failed: " + ", ".join(failed))

# Run each measurement in a fresh Python process. This makes RUSAGE_CHILDREN's
# ru_maxrss a per-binary value instead of the cumulative-max proxy used by the old
# adoption A/B script.
helper = r'''
import json, platform, resource, subprocess, sys, time
t0 = time.monotonic()
p = subprocess.run([sys.argv[1]], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
elapsed = time.monotonic() - t0
rss = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
if platform.system() != "Darwin":
    rss *= 1024
print(json.dumps({"returncode": p.returncode, "seconds": elapsed, "rss": rss}))
'''

samples = {name: [] for name in bins}
for round_no in range(rounds):
    for name, path in bins.items():
        measured = subprocess.run([sys.executable, "-c", helper, path],
                                  stdout=subprocess.PIPE, text=True, check=True)
        sample = json.loads(measured.stdout)
        if sample["returncode"] != 0:
            raise SystemExit("%s benchmark run failed" % name)
        samples[name].append(sample)
    print("round %d/%d" % (round_no + 1, rounds), flush=True)

best = {name: min(s["seconds"] for s in values)
        for name, values in samples.items()}
peak = {name: max(s["rss"] for s in values)
        for name, values in samples.items()}

print("\n%-14s %10s %14s" % ("variant", "best sec", "peak RSS MB"))
for name in ("adaptive", "legacy", "no-pacing"):
    print("%-14s %10.3f %14.1f" %
          (name, best[name], peak[name] / (1024.0 * 1024.0)))
print("adaptive/legacy: time %.2fx, RSS %.2fx" %
      (best["adaptive"] / best["legacy"], peak["adaptive"] / float(peak["legacy"])))

# Generous gates catch real regressions without turning normal scheduler noise into
# churn. The adaptive collector must remain competitive with the working legacy
# collector and its page retention must stay within a bounded additive allowance.
if best["adaptive"] > best["legacy"] * 1.35:
    raise SystemExit("adaptive collector is more than 35% slower than legacy")
rss_limit = max(peak["legacy"] * 1.50,
                peak["legacy"] + 64 * 1024 * 1024)
if peak["adaptive"] > rss_limit:
    raise SystemExit("adaptive collector peak RSS exceeds legacy regression bound")

print("BIBOP ADAPTIVE BENCHMARK GREEN (correctness + policy + perf/RAM)")
PY
