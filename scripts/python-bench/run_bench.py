#!/usr/bin/env python3
"""Python vs Codename One: the same Python kernels run by CPython and run
transpiled to Java.

Every kernel prints a RESULT line, which must be identical on both sides (a
benchmark that computes something else is not a benchmark), and a BENCH line
with its fastest round in microseconds, timed inside the program so that
start-up is measured separately. The harness times the whole process and reads
its peak resident memory from the operating system.

    run_bench.py --java-home J --runtime-jar R --core-jar C --transpiler-cp T
                 [--rounds 3] [--json out.json] [--check | --write-baseline]
    run_bench.py --fold python-bench.json

The gate compares the ratio ours/CPython of each metric with
baseline/ratios.json and fails when one grows by more than its tolerance.
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time

HERE = os.path.dirname(os.path.abspath(__file__))
KERNELS_DIR = os.path.join(HERE, "kernels")
BASELINE = os.path.join(HERE, "baseline", "ratios.json")
GENERATED_PACKAGE = "com.codename1.generated.python"

LAUNCHER = """
public class BenchMain {
    public static void main(String[] args) {
        com.codename1.python.runtime.Py.setArgv("bench", args);
        System.exit(%s.PythonModules.runMain());
    }
}
""" % GENERATED_PACKAGE


def kernels():
    return sorted(f[:-3] for f in os.listdir(KERNELS_DIR)
                  if f.endswith(".py") and not f.startswith("_"))


def timed(cmd, cwd=None):
    """Runs cmd and answers (stdout, wall seconds)."""
    start = time.perf_counter()
    proc = subprocess.Popen(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = proc.communicate()
    wall = time.perf_counter() - start
    if proc.returncode != 0:
        raise RuntimeError("%s failed (%d):\n%s" % (" ".join(cmd), proc.returncode, err.decode("utf-8", "replace")))
    return out.decode("utf-8"), wall


def peak_rss(cmd, cwd=None):
    """Peak resident set of one run of cmd, measured in a child of its own so
    that RUSAGE_CHILDREN holds that run and nothing else."""
    probe = ("import resource,subprocess,sys;"
             "subprocess.run(sys.argv[1:],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,check=True);"
             "r=resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss;"
             "print(r if sys.platform=='darwin' else r*1024)")
    out = subprocess.run([sys.executable, "-c", probe] + cmd, cwd=cwd, check=True,
                         stdout=subprocess.PIPE).stdout
    return int(out.strip())


def parse(output):
    result = bench = None
    for line in output.splitlines():
        if line.startswith("RESULT "):
            result = line.split(" ", 2)[2]
        elif line.startswith("BENCH "):
            bench = int(line.split()[2])
    if result is None or bench is None:
        raise RuntimeError("kernel printed no RESULT/BENCH line:\n" + output)
    return result, bench


def tree_bytes(path):
    total = 0
    for root, _, files in os.walk(path):
        for f in files:
            total += os.path.getsize(os.path.join(root, f))
    return total


def build(kernel, work, args):
    """Transpiles and compiles one kernel; answers its classes directory."""
    src = os.path.join(work, kernel, "src")
    gen = os.path.join(work, kernel, "gen")
    cls = os.path.join(work, kernel, "classes")
    os.makedirs(src)
    for f in (kernel + ".py", "_harness.py"):
        shutil.copy(os.path.join(KERNELS_DIR, f), src)
    java = os.path.join(args.java_home, "bin", "java")
    javac = os.path.join(args.java_home, "bin", "javac")
    subprocess.run([java, "-cp", args.transpiler_cp, "com.codename1.python.transpiler.api.Main",
                    src, gen, kernel], check=True)
    with open(os.path.join(gen, "BenchMain.java"), "w") as f:
        f.write(LAUNCHER)
    sources = []
    for root, _, files in os.walk(gen):
        sources += [os.path.join(root, f) for f in files if f.endswith(".java")]
    subprocess.run([javac, "-nowarn", "--release", "17", "-cp",
                    args.runtime_jar + os.pathsep + args.core_jar, "-d", cls] + sources, check=True)
    return src, cls


def measure(kernel, work, args):
    src, cls = build(kernel, work, args)
    java = os.path.join(args.java_home, "bin", "java")
    ours_cmd = [java, "-cp", os.pathsep.join([cls, args.runtime_jar, args.core_jar]), "BenchMain"]
    theirs_cmd = [args.python, os.path.join(src, kernel + ".py")]
    sides = {}
    for name, cmd in (("cpython", theirs_cmd), ("cn1_jvm", ours_cmd)):
        compute, wall, result = [], [], None
        for _ in range(args.rounds):
            out, seconds = timed(cmd, cwd=src)
            r, us = parse(out)
            if result is not None and r != result:
                raise RuntimeError("%s: %s is not deterministic" % (kernel, name))
            result = r
            compute.append(us)
            wall.append(seconds)
        sides[name] = {
            "result": result,
            "compute_us": min(compute),
            "wall_ms": int(min(wall) * 1000),
            "peak_rss": peak_rss(cmd, cwd=src),
        }
    if sides["cpython"]["result"] != sides["cn1_jvm"]["result"]:
        raise RuntimeError("%s computes something else:\n  cpython %s\n  cn1     %s"
                           % (kernel, sides["cpython"]["result"], sides["cn1_jvm"]["result"]))
    sides["cn1_jvm"]["class_bytes"] = tree_bytes(cls)
    ratios = {}
    for key in ("compute_us", "wall_ms", "peak_rss"):
        ratios[key] = round(sides["cn1_jvm"][key] / float(max(1, sides["cpython"][key])), 3)
    return {"sides": sides, "ratios": ratios}


def check(report, baseline):
    failures = []
    tolerance = baseline.get("tolerance", {})
    for kernel, entry in report["kernels"].items():
        base = baseline.get("ratios", {}).get(kernel)
        if base is None:
            failures.append("%s has no baseline; run with --write-baseline" % kernel)
            continue
        for key, ratio in entry["ratios"].items():
            limit = base[key] * (1 + tolerance.get(key, 0.25))
            if ratio > limit:
                failures.append("%s %s: ratio %.3f exceeds baseline %.3f (limit %.3f)"
                                % (kernel, key, ratio, base[key], limit))
    return failures


def write_baseline(report, path=BASELINE):
    baseline = {"tolerance": {"compute_us": 0.25, "wall_ms": 0.25, "peak_rss": 0.15},
                "ratios": {k: e["ratios"] for k, e in report["kernels"].items()},
                "python": report.get("python")}
    if os.path.exists(path):
        with open(path) as f:
            baseline["tolerance"] = json.load(f).get("tolerance", baseline["tolerance"])
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(baseline, f, indent=2, sort_keys=True)
        f.write("\n")


def render(report):
    lines = ["| kernel | compute ours/CPython | wall ours/CPython | RSS ours/CPython | class bytes |",
             "|---|---|---|---|---|"]
    for kernel, e in sorted(report["kernels"].items()):
        r = e["ratios"]
        lines.append("| %s | %.2fx | %.2fx | %.2fx | %d |" % (
            kernel, r["compute_us"], r["wall_ms"], r["peak_rss"], e["sides"]["cn1_jvm"]["class_bytes"]))
    lines.append("")
    lines.append("Runtime jar: %d bytes" % report["runtime_jar_bytes"])
    return "\n".join(lines)


def main():
    if len(sys.argv) == 3 and sys.argv[1] == "--fold":
        # Seeds or moves the baseline from a report CI produced, without
        # measuring anything here.
        with open(sys.argv[2]) as f:
            write_baseline(json.load(f))
        return 0
    p = argparse.ArgumentParser()
    p.add_argument("--java-home", required=True)
    p.add_argument("--runtime-jar", required=True)
    p.add_argument("--core-jar", required=True)
    p.add_argument("--transpiler-cp", required=True)
    p.add_argument("--python", default=sys.executable)
    p.add_argument("--rounds", type=int, default=3)
    p.add_argument("--kernel", action="append")
    p.add_argument("--json")
    g = p.add_mutually_exclusive_group()
    g.add_argument("--check", action="store_true")
    g.add_argument("--write-baseline", action="store_true")
    args = p.parse_args()
    # Kernels run from their own directory, so every path given here must
    # survive the change of working directory.
    args.runtime_jar = os.path.abspath(args.runtime_jar)
    args.core_jar = os.path.abspath(args.core_jar)
    args.transpiler_cp = os.pathsep.join(os.path.abspath(x) for x in args.transpiler_cp.split(os.pathsep))

    work = tempfile.mkdtemp(prefix="python-bench-")
    report = {"python": subprocess.run([args.python, "--version"], stdout=subprocess.PIPE,
                                       universal_newlines=True).stdout.strip(),
              "runtime_jar_bytes": os.path.getsize(args.runtime_jar),
              "kernels": {}}
    for k in args.kernel or kernels():
        print("measuring", k, file=sys.stderr)
        report["kernels"][k] = measure(k, work, args)
    print(render(report))
    if args.json:
        with open(args.json, "w") as f:
            json.dump(report, f, indent=2, sort_keys=True)
    if args.write_baseline:
        write_baseline(report)
    if args.check and not os.path.exists(BASELINE):
        # Ratios depend on the machine and the interpreter, so the baseline
        # is seeded from the CI runner's own report, never from a laptop.
        print("no baseline yet (%s): reporting only. Seed it with "
              "run_bench.py --fold <this run's python-bench.json>."
              % os.path.relpath(BASELINE), file=sys.stderr)
        return 0
    if args.check:
        with open(BASELINE) as f:
            failures = check(report, json.load(f))
        for line in failures:
            print("REGRESSION " + line, file=sys.stderr)
        return 1 if failures else 0
    return 0


if __name__ == "__main__":
    sys.exit(main())
