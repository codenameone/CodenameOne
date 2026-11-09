#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The hero ports measured: llm and MarkItDown, unmodified from PyPI, run by
CPython and transpiled to Java.

Each workload under workloads/ is built the way an application is -- its
requirements resolved from PyPI by the build's own resolver, the program
and every package transpiled, the package data packed -- and the same file
runs under CPython in a virtual environment holding the same pins. Both
sides print a RESULT digest, which must be identical, and their own
timings (IMPORT, BENCH, COLD, TOTAL); the harness adds wall time, peak
resident memory and the size of what each side ships.

    run_heroes.py --java-home J --build-cp B --runtime-jar R --core-jar C
                  --run-cp T --markitdown-tests DIR [--llm-tests DIR]
                  [--rounds 3] [--workload NAME] [--json out.json]
                  [--check | --write-baseline]
    run_heroes.py --fold python-heroes.json

--build-cp is build-engine with its dependencies (the transpiler among
them); --run-cp holds the python-transpiler test classes (the JVM file
system, SQLite and HTTP hooks) and sqlite-jdbc. The test directories are the
upstream checkouts' own (`tests` of llm at its pinned tag,
`packages/markitdown/tests` of MarkItDown): MarkItDown's test documents are
the conversion workload's input, and with --llm-tests both upstream suites
also run, transpiled and under CPython's pytest, and must pass the same
number of tests.
"""
import re
import argparse
import filecmp
import json
import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
HEROES = os.path.dirname(HERE)
sys.path.insert(0, os.path.join(os.path.dirname(HEROES), "python-bench"))
from run_bench import peak_rss, timed, tree_bytes  # noqa: E402

BASELINE = os.path.join(HERE, "baseline", "ratios.json")
HARNESS = os.path.join(HERE, "harness")
REPO = os.path.dirname(os.path.dirname(HEROES))

# Each workload runs against the pins of the application it stands for;
# `extra` is what the workload needs beyond the application.
WORKLOADS = {
    "llm_session": {"requirements": os.path.join(HEROES, "llm-mobile", "requirements.txt"),
                    "extra": ["llm-echo==0.3a3"]},
    "markitdown_convert": {"requirements": os.path.join(HEROES, "markitdown-mobile", "requirements.txt"),
                           "extra": [],
                           # The runtime provides pandas, so requirements.txt
                           # does not name it; CPython's XLSX converter needs it.
                           "cpython": ["pandas==3.1.1"]},
}

TIMINGS = ("IMPORT", "BENCH", "COLD", "TOTAL")


# The upstream suites: the driver under tests/, the application's pins, and
# what CPython's pytest needs besides (the transpiled side has its own pytest,
# pytest-asyncio and pytest-httpx built in).
SUITES = {
    "llm": {"workload": "llm_session", "test_requirements": [
        "pytest==9.2.0", "pytest-asyncio==1.4.1.post1", "httpx2-pytest==2.0.0", "numpy==2.5.4"]},
    "markitdown": {"workload": "markitdown_convert", "test_requirements": ["pytest==9.2.0"],
                   # Tests that fail transpiled by design, each for a decision the
                   # developer guide documents (see Python-Hero-Ports).
                   "diverges": {
                       # test_mskanji.csv, in Shift-JIS: legacy encodings other than
                       # Latin-1 are not supported, so it fails to decode.
                       "test_module_vectors::*[test_vector16]": "Shift-JIS is not a supported encoding",
                       "test_module_misc::test_a_declared_charset_still_wins": "GB18030 is not supported",
                       "test_module_misc::test_explicit_charset_still_takes_precedence":
                           "Windows-1252 is not supported",
                       # The tests lower sys.setrecursionlimit to provoke a
                       # RecursionError; the limit is not enforced, so the deep
                       # document converts in full instead of falling back.
                       "test_html::test_deeply_nested_html_fallback": "recursion limit not enforced",
                       "test_rss::test_deep_xml_body_uses_rendering_fallback[*]": "recursion limit not enforced",
                       "test_rss::test_deeply_nested_rss_item_fallback": "recursion limit not enforced",
                   }},
}


def parse(output):
    found = {}
    for line in output.splitlines():
        parts = line.split()
        if len(parts) >= 3 and parts[0] == "RESULT":
            found["result"] = parts[2]
        elif len(parts) >= 3 and parts[0] in TIMINGS:
            found[parts[0].lower() + "_us"] = int(parts[2])
    missing = [k for k in ["result"] + [t.lower() + "_us" for t in TIMINGS] if k not in found]
    if missing:
        raise RuntimeError("workload printed no %s:\n%s" % (", ".join(missing), output))
    return found


def requirements(name, work):
    path = os.path.join(work, name + "-requirements.txt")
    with open(WORKLOADS[name]["requirements"]) as f:
        text = f.read()
    with open(path, "w") as f:
        f.write(text + "\n" + "\n".join(WORKLOADS[name]["extra"]) + "\n")
    return path


def java(args, tool):
    return os.path.join(args.java_home, "bin", tool)


def compile_harness(args, work):
    out = os.path.join(work, "harness")
    subprocess.run([java(args, "javac"), "-nowarn", "--release", "17", "-cp", args.build_cp, "-d", out,
                    os.path.join(HARNESS, "HeroBuild.java")], check=True)
    return out


def build(name, program, reqs, work, harness, args, packages=()):
    """Resolves, transpiles and compiles one program; answers its classes."""
    dest = os.path.join(work, name)
    subprocess.run([java(args, "java"), "-cp", harness + os.pathsep + args.build_cp, "HeroBuild",
                    program, reqs, dest] + list(packages), check=True)
    classes = os.path.join(dest, "classes")
    # HeroMain's image reader is the one the client PIL conformance tests use.
    sources = [os.path.join(HARNESS, "HeroMain.java"),
               os.path.join(REPO, "maven", "python-transpiler", "src", "test", "resources", "conformance",
                            "pilclient_", "pilclienttest", "JdkImageIO.java")]
    for root, _, files in os.walk(os.path.join(dest, "gen")):
        sources += [os.path.join(root, f) for f in files if f.endswith(".java")]
    listing = os.path.join(dest, "sources.txt")
    with open(listing, "w") as f:
        f.write("\n".join(sources))
    cp = os.pathsep.join([args.runtime_jar, args.core_jar, args.run_cp])
    subprocess.run([java(args, "javac"), "-nowarn", "--release", "17", "-cp", cp, "-d", classes,
                    "@" + listing], check=True)
    # What remap-compat does to an application: the generated Java's line
    # tables point back at the .py sources. Without it there are no Python
    # frames, and sys._getframe (pydantic calls it at import) raises.
    subprocess.run([java(args, "java"), "-cp", args.build_cp, "com.codename1.maven.PythonLineMapper", classes],
                   check=True)
    return classes


def cpython_env(name, reqs, work, args, extra=(), python=None):
    python = python or args.python
    env = os.path.join(work, name + "-" + os.path.basename(python) + "-venv")
    subprocess.run([python, "-m", "venv", env], check=True)
    py = os.path.join(env, "bin", "python")
    # CPython needs what the Codename One runtime provides itself (httpx,
    # pydantic, PyYAML...) and magika's real model; pip finds those as the
    # pinned packages' own dependencies.
    if subprocess.run([py, "-m", "pip", "install", "-q", "-r", reqs] + list(extra)).returncode != 0:
        if python == args.python:
            raise RuntimeError("the pins do not install under " + python)
        # PyPy and GraalPy trail CPython's language version, and some pins
        # need a newer one; the interpreter then gets the newest release of
        # each package it supports. The digest check still holds it to the
        # same output.
        with open(reqs) as f:
            names = [re.split("[=<>!~ ;]", line.strip())[0] for line in f
                     if line.strip() and not line.startswith(("#", "-"))]
        subprocess.run([py, "-m", "pip", "install", "-q"] + names + [re.split("[=<>]", e)[0] for e in extra],
                       check=True)
    site = subprocess.run([py, "-c", "import sysconfig;print(sysconfig.get_paths()['purelib'])"],
                          stdout=subprocess.PIPE, universal_newlines=True, check=True).stdout.strip()
    return py, site


def side(cmd, cwd, rounds, env):
    best = None
    walls = []
    for _ in range(rounds):
        out, seconds = timed(cmd, cwd=cwd, env=env)
        found = parse(out)
        if best is not None and found["result"] != best["result"]:
            raise RuntimeError("%s is not deterministic" % " ".join(cmd))
        walls.append(seconds)
        if best is None:
            best = found
        else:
            # The fastest of each timing, as run_bench takes.
            for k, v in found.items():
                if k != "result":
                    best[k] = min(best[k], v)
    best["wall_ms"] = int(min(walls) * 1000)
    best["peak_rss"] = peak_rss(cmd, cwd=cwd, env=env)
    return best


def matches(test, pattern):
    """Whether a test id matches a `diverges` pattern, where `*` is the only
    wildcard: test ids carry their parameters in brackets, which fnmatch
    would read as a character class."""
    return re.fullmatch(".*".join(re.escape(part) for part in pattern.split("*")), test) is not None


def differing_outputs(fs, ours, theirs, work, ours_env, theirs_env):
    """Runs both sides once more, each writing every output to a directory
    (MARKITDOWN_DUMP), and names the outputs that differ and where they are."""
    mine, cpython = os.path.join(fs, "dump"), os.path.join(work, "dump-cpython")
    os.makedirs(mine, exist_ok=True)
    os.makedirs(cpython, exist_ok=True)
    subprocess.run(ours[:-1] + ["1"], cwd=work, env=dict(ours_env, MARKITDOWN_DUMP="/dump"),
                   stdout=subprocess.DEVNULL, check=False)
    subprocess.run(theirs[:-1] + ["1"], cwd=work, env=dict(theirs_env, MARKITDOWN_DUMP=cpython),
                   stdout=subprocess.DEVNULL, check=False)
    names = sorted(set(os.listdir(mine)) | set(os.listdir(cpython)))
    if not names:
        return ""
    differ = [n for n in names if not (os.path.exists(os.path.join(mine, n))
                                       and os.path.exists(os.path.join(cpython, n))
                                       and filecmp.cmp(os.path.join(mine, n), os.path.join(cpython, n),
                                                       shallow=False))]
    return "\n  outputs that differ: %s\n  cn1 in %s, cpython in %s" % (", ".join(differ), mine, cpython)


def measure(name, work, harness, args):
    reqs = requirements(name, work)
    classes = build(name, os.path.join(HERE, "workloads", name + ".py"), reqs, work, harness, args)
    py, site = cpython_env(name, reqs, work, args, WORKLOADS[name].get("cpython", ()))
    fs = os.path.join(work, name, "fs")
    fixtures = os.path.join(fs, "fixtures")
    shutil.copytree(os.path.join(args.markitdown_tests, "test_files"), fixtures)
    # The JVM side sees its file system root as "/", CPython the real path.
    ours_env = dict(os.environ, MARKITDOWN_FIXTURES="/fixtures")
    theirs_env = dict(os.environ, MARKITDOWN_FIXTURES=fixtures)
    run_cp = os.pathsep.join([classes, args.runtime_jar, args.core_jar, args.run_cp])
    ours = [java(args, "java"), "-cp", run_cp, "HeroMain", fs, str(args.rounds)]
    theirs = [py, "-B", os.path.join(HERE, "workloads", name + ".py"), str(args.rounds)]
    sides = {"cpython": side(theirs, work, 1, theirs_env), "cn1_jvm": side(ours, work, 1, ours_env)}
    if sides["cpython"]["result"] != sides["cn1_jvm"]["result"]:
        raise RuntimeError("%s computes something else:\n  cpython %s\n  cn1     %s%s"
                           % (name, sides["cpython"]["result"], sides["cn1_jvm"]["result"],
                              differing_outputs(fs, ours, theirs, work, ours_env, theirs_env)))
    sides["cn1_jvm"]["ship_bytes"] = tree_bytes(classes) + os.path.getsize(args.runtime_jar)
    # The arm that stands for a device: the same classes translated to C by
    # ParparVM and compiled ahead of time, as an iOS build is. No JVM, no JIT.
    binary = os.path.join(work, name, name + "-native")
    subprocess.run([os.path.join(HERE, "native", "build_native.sh"), os.path.join(work, name), binary],
                   check=True, env=dict(os.environ, JAVA17_HOME=args.java_home,
                                        PYBENCH_RUNTIME=args.runtime_jar))
    native_env = dict(theirs_env, CN1_RESOURCE_PATH=classes)
    sides["cn1_native"] = side([binary, str(args.rounds)], work, 1, native_env)
    if sides["cn1_native"]["result"] != sides["cpython"]["result"]:
        raise RuntimeError("%s computes something else natively: %s" % (name, sides["cn1_native"]["result"]))
    sides["cn1_native"]["ship_bytes"] = os.path.getsize(binary) + os.path.getsize(
        os.path.join(classes, "cn1python.dat"))
    sides["cpython"]["ship_bytes"] = tree_bytes(site)
    # The other interpreters run the same file against the same pins and must
    # compute the same digest; they are reported beside CPython, not judged.
    for label, python in (args.compare or ()):
        try:
            opy, osite = cpython_env(name, reqs, work, args, WORKLOADS[name].get("cpython", ()), python)
        except subprocess.CalledProcessError:
            sides[label] = {"error": "the requirements do not install"}
            continue
        try:
            other = side([opy, os.path.join(HERE, "workloads", name + ".py"), str(args.rounds)], work, 1,
                         theirs_env)
        except RuntimeError as e:
            # Reported, not fatal: GraalPy, for one, has no BLAKE2 with a
            # digest size of its own, which llm's embeddings ask for.
            lines = [x for x in str(e).splitlines() if x.strip() and not x.startswith((" ", "\t"))]
            sides[label] = {"error": lines[-1] if lines else str(e)}
            continue
        if other["result"] != sides["cpython"]["result"]:
            raise RuntimeError("%s computes something else under %s: %s" % (name, label, other["result"]))
        other["ship_bytes"] = tree_bytes(osite)
        sides[label] = other
    ratios = {}
    for key in ("import_us", "bench_us", "cold_us", "wall_ms", "peak_rss"):
        ratios[key] = round(sides["cn1_native"][key] / float(max(1, sides["cpython"][key])), 3)
    return {"sides": sides, "ratios": ratios}


def counts(text):
    """Sums pytest's "N passed, M failed" summaries in text."""
    total = {"passed": 0, "failed": 0, "skipped": 0, "error": 0}
    for line in text.splitlines():
        if not (line.startswith("=") or re.match(r"^\d+ (passed|failed)", line)):
            continue
        for n, kind in re.findall(r"(\d+) (passed|failed|skipped|errors?)\b", line):
            total["error" if kind.startswith("error") else kind] += int(n)
    return total


def suite(name, work, harness, args):
    """Runs one upstream suite both ways; answers the two sides' counts."""
    spec = SUITES[name]
    upstream = args.llm_tests if name == "llm" else args.markitdown_tests
    tests = os.path.join(work, name + "-suite", "tests")
    shutil.copytree(upstream, tests, ignore=shutil.ignore_patterns("__pycache__"))
    init = os.path.join(tests, "__init__.py")
    if not os.path.exists(init):
        # llm's tests are not a package upstream; the driver imports them
        # as one. Written into this copy only.
        open(init, "w").close()
    reqs = requirements(spec["workload"], work)
    driver = os.path.join(HEROES, "tests", name + "_suite.py")
    classes = build(name + "-suite", driver, reqs, work, harness, args, [tests])
    fs = os.path.join(work, name + "-suite", "fs")
    run_cp = os.pathsep.join([classes, args.runtime_jar, args.core_jar, args.run_cp])
    ours = subprocess.run([java(args, "java"), "-cp", run_cp, "HeroMain", fs], cwd=work,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True)
    with open(os.path.join(work, name + "-suite.cn1.txt"), "w") as f:
        f.write(ours.stdout)
    test_reqs = os.path.join(work, name + "-test-requirements.txt")
    with open(test_reqs, "w") as f:
        f.write("-r %s\n%s\n" % (reqs, "\n".join(spec["test_requirements"])))
    py, _ = cpython_env(name + "-suite", test_reqs, work, args)
    with open(driver) as f:
        modules = re.findall(r"\btest_\w+", f.read().split("MODULES", 1)[1])
    files = sorted(set(os.path.join("tests", m + ".py") for m in modules))
    theirs = subprocess.run([py, "-B", "-m", "pytest", "-q", "-rfE", "-p", "no:cacheprovider"] + files,
                            cwd=os.path.dirname(tests), stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            universal_newlines=True)
    with open(os.path.join(work, name + "-suite.cpython.txt"), "w") as f:
        f.write(theirs.stdout)
    result = {"cpython": counts(theirs.stdout.splitlines()[-1] if theirs.stdout else ""),
              "cn1_jvm": counts(ours.stdout)}
    # A test failing transpiled is explained when it fails under CPython too
    # (an optional dependency neither side installs) or is a documented
    # divergence; anything else is a regression.
    cpython_failed = set(failed_ids(theirs.stdout, ".py::"))
    diverges = spec.get("diverges", {})
    result["diverged"] = []
    result["unexplained"] = []
    for test in failed_ids(ours.stdout, "::"):
        if test in cpython_failed:
            continue
        if any(matches(test, pattern) for pattern in diverges):
            result["diverged"].append(test)
        else:
            result["unexplained"].append(test)
    return result


def failed_ids(text, separator):
    """`test_module::name[param]` of each FAILED line, from either runner:
    CPython's pytest names `tests/test_x.py::t`, the transpiled `tests.test_x::t`."""
    ids = []
    for line in text.splitlines():
        if line.startswith("FAILED ") or line.startswith("ERROR "):
            node = line.split()[1]
            module, _, test = node.partition(separator if separator in node else "::")
            ids.append(re.split(r"[./]", module)[-1] + "::" + test)
    return ids


def check(report, baseline):
    failures = []
    tolerance = baseline.get("tolerance", {})
    for name, entry in report["workloads"].items():
        base = baseline.get("ratios", {}).get(name)
        if base is None:
            failures.append("%s has no baseline; run with --write-baseline" % name)
            continue
        for key, ratio in entry["ratios"].items():
            limit = base[key] * (1 + tolerance.get(key, 0.25))
            if ratio > limit:
                failures.append("%s %s: ratio %.3f exceeds baseline %.3f (limit %.3f)"
                                % (name, key, ratio, base[key], limit))
    return failures


def write_baseline(report, path=BASELINE):
    baseline = {"tolerance": {"import_us": 0.3, "bench_us": 0.3, "cold_us": 0.3, "wall_ms": 0.3,
                              "peak_rss": 0.15},
                "ratios": {k: e["ratios"] for k, e in report["workloads"].items()},
                "python": report.get("python")}
    if os.path.exists(path):
        with open(path) as f:
            baseline["tolerance"] = json.load(f).get("tolerance", baseline["tolerance"])
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(baseline, f, indent=2, sort_keys=True)
        f.write("\n")


def render_suites(report):
    lines = ["| upstream suite | CPython passed | Codename One passed | documented divergences "
             "| failed only transpiled |", "|---|---|---|---|---|"]
    for name, e in sorted(report.get("suites", {}).items()):
        lines.append("| %s | %d | %d | %d | %d |" % (name, e["cpython"]["passed"], e["cn1_jvm"]["passed"],
                                                    len(e["diverged"]), len(e["unexplained"])))
    return "\n".join(lines)


def render(report):
    lines = ["| workload | side | import ms | first round ms | best round ms | wall ms | peak RSS MB | ships MB |",
             "|---|---|---|---|---|---|---|---|"]
    for name, e in sorted(report["workloads"].items()):
        others = sorted(k for k in e["sides"] if k not in ("cpython", "cn1_jvm", "cn1_native"))
        for key in ["cpython"] + others + ["cn1_native", "cn1_jvm"]:
            label = {"cpython": "CPython", "pypy": "PyPy", "graalpy": "GraalPy",
                     "cn1_native": "Codename One (native, ParparVM)",
                     "cn1_jvm": "Codename One (JVM simulator)"}.get(key, key)
            s = e["sides"][key]
            if "error" in s:
                lines.append("| %s | %s | fails: %s |||||| " % (name, label, s["error"][:90]))
                continue
            lines.append("| %s | %s | %d | %d | %d | %d | %.0f | %.1f |" % (
                name, label, s["import_us"] // 1000, s["cold_us"] // 1000, s["bench_us"] // 1000,
                s["wall_ms"], s["peak_rss"] / 1048576.0, s["ship_bytes"] / 1048576.0))
    lines.append("")
    lines.append("CPython: %s. Both sides computed the same RESULT digest." % report["python"])
    return "\n".join(lines)


def main():
    if len(sys.argv) == 3 and sys.argv[1] == "--fold":
        with open(sys.argv[2]) as f:
            write_baseline(json.load(f))
        return 0
    p = argparse.ArgumentParser()
    p.add_argument("--java-home", required=True)
    p.add_argument("--build-cp", required=True)
    p.add_argument("--runtime-jar", required=True)
    p.add_argument("--core-jar", required=True)
    p.add_argument("--run-cp", required=True)
    p.add_argument("--markitdown-tests", required=True)
    p.add_argument("--llm-tests")
    p.add_argument("--python", default=sys.executable)
    p.add_argument("--compare", action="append", type=lambda v: tuple(v.split("=", 1)),
                   help="label=interpreter, another Python measured beside CPython (pypy, graalpy)")
    p.add_argument("--rounds", type=int, default=3)
    p.add_argument("--workload", action="append")
    p.add_argument("--json")
    g = p.add_mutually_exclusive_group()
    g.add_argument("--check", action="store_true")
    g.add_argument("--write-baseline", action="store_true")
    args = p.parse_args()
    for key in ("runtime_jar", "core_jar", "markitdown_tests", "llm_tests"):
        if getattr(args, key):
            setattr(args, key, os.path.abspath(getattr(args, key)))
    for key in ("build_cp", "run_cp"):
        setattr(args, key, os.pathsep.join(os.path.abspath(x) for x in getattr(args, key).split(os.pathsep)))

    work = tempfile.mkdtemp(prefix="python-heroes-")
    harness = compile_harness(args, work)
    report = {"python": subprocess.run([args.python, "--version"], stdout=subprocess.PIPE,
                                       universal_newlines=True).stdout.strip(),
              "workloads": {}}
    for name in args.workload or sorted(WORKLOADS):
        print("measuring", name, file=sys.stderr)
        report["workloads"][name] = measure(name, work, harness, args)
    print(render(report))
    suite_failures = []
    if args.llm_tests:
        report["suites"] = {}
        for name in sorted(SUITES):
            print("running the %s suite" % name, file=sys.stderr)
            e = report["suites"][name] = suite(name, work, harness, args)
            short = e["cpython"]["passed"] - e["cn1_jvm"]["passed"] - len(e["diverged"])
            if e["unexplained"] or short > 0:
                suite_failures.append("%s suite: %d passed transpiled, %d under CPython, %d documented "
                                      "divergences; outputs in %s" % (name, e["cn1_jvm"]["passed"],
                                                                      e["cpython"]["passed"], len(e["diverged"]),
                                                                      work))
                suite_failures += ["  failed only transpiled: " + t for t in e["unexplained"]]
        print("")
        print(render_suites(report))
    if args.json:
        with open(args.json, "w") as f:
            json.dump(report, f, indent=2, sort_keys=True)
    if args.write_baseline:
        write_baseline(report)
    for line in suite_failures:
        print("SUITE " + line, file=sys.stderr)
    if suite_failures:
        return 1
    if args.check and not os.path.exists(BASELINE):
        # Seeded from the CI runner's own report, never from a laptop: the
        # ratios depend on the machine.
        print("no baseline yet (%s): reporting only. Seed it with "
              "run_heroes.py --fold <this run's python-heroes.json>." % os.path.relpath(BASELINE),
              file=sys.stderr)
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
