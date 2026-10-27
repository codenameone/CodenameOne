#!/usr/bin/env python3
# Runs vm/benchmarks' CommonWorkloads, ported to Python
# (scripts/python-bench/common), on every Python runtime given, plus the
# Codename One build twice: the Python as written, and the same program with
# the hot spot in Java. Each workload runs in a process of its own, so every
# row has its own peak RSS. Checksums are checked against Java's.
#
#   common_compare.py --cn1 <common_bench binary> --cpython P [--pypy P]
#                     [--graalpy P] [--json out.json] [--md out.md]
import argparse
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
COMMON = os.path.join(os.path.dirname(HERE), "python-bench", "common")

# com.bench.CommonWorkloads on HotSpot: the contract every arm must meet.
EXPECTED = {
    "intArithmetic": 1307491170054,
    "longArithmetic": 6887886960473257608,
    "mathTranscendental": 4729652805076374709,
    "arraySequential": 0,
    "arrayRandom": -2288487891715278,
    "objectAllocation": 2999790376128,
    "valueEscape": 1026483456,
    "hashMapChurn": 49941,
    "stringBuilding": -609121604069,
    "recursion": 33385282,
    "quicksort": 809667393311589960,
}


def run(cmd, timeout):
    p = subprocess.run(["/usr/bin/time", "-l"] + cmd, capture_output=True, text=True,
                       cwd=COMMON, timeout=timeout)
    if p.returncode != 0:
        raise RuntimeError("%s: %s" % (cmd, p.stderr[-400:]))
    m = re.search(r"BENCH (\S+) ms=([\d.]+) checksum=(-?\d+)", p.stdout)
    rss = int(re.search(r"(\d+)\s+maximum resident set size", p.stderr).group(1)) / 1048576.0
    return float(m.group(2)), int(m.group(3)), rss


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cn1", required=True)
    ap.add_argument("--cpython", required=True)
    ap.add_argument("--pypy")
    ap.add_argument("--graalpy")
    ap.add_argument("--warmup", type=int, default=1)
    ap.add_argument("--measured", type=int, default=3)
    ap.add_argument("--json")
    ap.add_argument("--md")
    ap.add_argument("--only", help="comma separated workloads")
    a = ap.parse_args()
    script = os.path.join(COMMON, "common_bench.py")
    arms = [("CPython", lambda w: [a.cpython, script, w])]
    if a.pypy:
        arms.append(("PyPy", lambda w: [a.pypy, script, w]))
    if a.graalpy:
        arms.append(("GraalPy", lambda w: [a.graalpy, script, w]))
    arms.append(("Codename One", lambda w: [a.cn1, w, "python"]))
    arms.append(("Codename One, hot spot in Java", lambda w: [a.cn1, w, "java"]))
    reps = [str(a.warmup), str(a.measured)]
    names = [w for w in EXPECTED if not a.only or w in a.only.split(",")]
    results = {}
    for w in names:
        results[w] = {}
        for arm, cmd in arms:
            ms, checksum, rss = run(cmd(w) + _reps(arm, reps), 3600)
            if checksum != EXPECTED[w]:
                raise RuntimeError("%s on %s: checksum %d, Java's is %d" % (w, arm, checksum, EXPECTED[w]))
            results[w][arm] = {"ms": ms, "rss_mb": rss}
            print("%-20s %-30s %10.1f ms %8.1f MB" % (w, arm, ms, rss), flush=True)
    out = {"arms": [n for n, _ in arms], "results": results}
    if a.json:
        with open(a.json, "w") as f:
            json.dump(out, f, indent=1)
    if a.md:
        with open(a.md, "w") as f:
            f.write(markdown(out))


def _reps(arm, reps):
    # common_bench.py takes <workload> <mode> <warmup> <measured>; the Python
    # interpreters are given the mode here, the native binary already has it.
    return reps if arm.startswith("Codename") else ["python"] + reps


def markdown(out):
    arms = out["arms"]
    lines = ["| workload | " + " | ".join("%s ms | MB" % n for n in arms) + " |",
             "|---|" + "---:|---:|" * len(arms)]
    for w, row in out["results"].items():
        lines.append("| %s | " % w + " | ".join(
            "%.1f | %.1f" % (row[n]["ms"], row[n]["rss_mb"]) for n in arms) + " |")
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    sys.exit(main())
