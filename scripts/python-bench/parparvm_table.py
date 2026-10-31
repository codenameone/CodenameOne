#!/usr/bin/env python3
"""Builds every kernel with build_parparvm.sh and compares it with CPython:
compute (best of rounds, timed inside the program), process wall time, peak
resident memory and binary size. Results must match CPython's exactly.

    parparvm_table.py [--rounds 3] [--no-build] [--m2 DIR] [--impl NAME=PATH ...]
                      [--json out.json] [--check] [kernel ...]
    parparvm_table.py --fold <report.json>

--check gates the ParparVM/CPython ratios against
baseline/parparvm-ratios.json, with run_bench.py's tolerances; --fold seeds
or moves that baseline from a report CI produced (never from a laptop: the
ratios depend on the machine).

--impl adds another Python (PyPy, GraalPy) as extra columns: its compute
time and peak RSS, with the same result check. Compute is the best round
inside one process, so a JIT is measured warm; wall time includes its warm-up.
"""
import argparse
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import run_bench  # noqa: E402

BASELINE = os.path.join(HERE, "baseline", "parparvm-ratios.json")


def run(cmd, cwd, rounds):
    best = None
    for _ in range(rounds):
        p = subprocess.run(["/usr/bin/time", "-l" if sys.platform == "darwin" else "-v"] + cmd,
                           cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
        if p.returncode != 0:
            raise RuntimeError("%s failed:\n%s" % (cmd, p.stderr[-3000:]))
        result = re.search(r"RESULT \S+ (.*)", p.stdout).group(1)
        us = int(re.search(r"BENCH \S+ (\d+)", p.stdout).group(1))
        if sys.platform == "darwin":
            wall = float(re.search(r"([\d.]+) real", p.stderr).group(1))
            rss = int(re.search(r"(\d+)\s+maximum resident", p.stderr).group(1))
        else:
            m = re.search(r"Elapsed \(wall clock\) time.*: (?:(\d+):)?(\d+):([\d.]+)", p.stderr)
            wall = int(m.group(1) or 0) * 3600 + int(m.group(2)) * 60 + float(m.group(3))
            rss = int(re.search(r"Maximum resident set size \(kbytes\): (\d+)", p.stderr).group(1)) * 1024
        cold = int(re.search(r"COLD \S+ (\d+)", p.stdout).group(1))
        total = int(re.search(r"TOTAL \S+ (\d+)", p.stdout).group(1))
        r = (us, wall, rss, result, cold, total)
        if best is None or us < best[0]:
            best = r
    return best


def main():
    if len(sys.argv) == 3 and sys.argv[1] == "--fold":
        with open(sys.argv[2]) as f:
            run_bench.write_baseline(json.load(f), BASELINE)
        return 0
    p = argparse.ArgumentParser()
    p.add_argument("--m2", help="Maven repository build_parparvm.sh reads (default <repo>/.m2-repo)")
    p.add_argument("--json")
    p.add_argument("--check", action="store_true")
    p.add_argument("--rounds", type=int, default=3)
    p.add_argument("--no-build", action="store_true")
    p.add_argument("--impl", action="append", default=[], metavar="NAME=PATH")
    p.add_argument("kernel", nargs="*")
    a = p.parse_args()
    kernels = a.kernel or sorted(f[:-3] for f in os.listdir(os.path.join(HERE, "kernels"))
                                 if f.endswith(".py") and not f.startswith("_"))
    impls = [i.split("=", 1) for i in a.impl]
    print("| kernel | impl | best | cold | all rounds | process wall | peak RSS | best vs CPython |")
    print("|---|---|---|---|---|---|---|---|")
    failed = False
    report = {"python": subprocess.run([sys.executable, "--version"], stdout=subprocess.PIPE,
                                       universal_newlines=True).stdout.strip(), "kernels": {}}
    for k in kernels:
        work = os.path.join(HERE, "target", k)
        if not a.no_build:
            subprocess.run([os.path.join(HERE, "build_parparvm.sh"), k] + ([a.m2] if a.m2 else []), check=True,
                           stdout=subprocess.DEVNULL)
        src = os.path.join(work, "src")
        c = run([sys.executable, k + ".py"], src, a.rounds)
        n = run([os.path.join(work, k)], src, a.rounds)
        same = c[3] == n[3]
        failed |= not same
        report["kernels"][k] = {
            "ratios": {"compute_us": round(n[0] / float(max(1, c[0])), 3),
                       "wall_ms": round(n[1] / max(0.001, c[1]), 3),
                       "peak_rss": round(n[2] / float(max(1, c[2])), 3)},
            "sides": {"cpython": {"compute_us": c[0], "wall_ms": int(c[1] * 1000), "peak_rss": c[2]},
                      "cn1_parparvm": {"compute_us": n[0], "wall_ms": int(n[1] * 1000), "peak_rss": n[2],
                                       "binary_bytes": os.path.getsize(os.path.join(work, k))}}}
        rows = [("CPython", c), ("ParparVM", n)]
        for name, path in impls:
            rows.append((name, run([path, k + ".py"], src, a.rounds)))
        for name, r in rows:
            ok = r[3] == c[3]
            failed |= not ok
            print("| %s | %s%s | %.1f ms | %.1f ms | %.1f ms | %.2fs | %d MB | %.2fx |" % (
                k, name, "" if ok else " **WRONG RESULT**", r[0] / 1000.0, r[4] / 1000.0, r[5] / 1000.0,
                r[1], r[2] >> 20, r[0] / float(c[0])))
        sys.stdout.flush()
    if a.json:
        with open(a.json, "w") as f:
            json.dump(report, f, indent=2, sort_keys=True)
    if a.check:
        if not os.path.exists(BASELINE):
            print("no baseline yet (%s): reporting only. Seed it with "
                  "parparvm_table.py --fold <this run's report>." % os.path.relpath(BASELINE), file=sys.stderr)
        else:
            with open(BASELINE) as f:
                for line in run_bench.check(report, json.load(f)):
                    print("REGRESSION " + line, file=sys.stderr)
                    failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
