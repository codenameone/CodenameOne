#!/usr/bin/env python3
# Measures Codename One's native Python against CPython and PyPy (and GraalPy
# when one is given) on the same programs, on this machine, and writes the
# numbers as Markdown. Nothing here is estimated: every cell is a run.
#
#   compare.py --cn1-kernels DIR --cn1-server BIN [--pypy P] [--graalpy G]
#              [--cpython C] [--out report.md]
#
# --cn1-kernels is scripts/python-bench/target (build_parparvm.sh output,
# one <kernel>/<kernel> binary each); --cn1-server is the flask-todo sample
# built by scripts/python-backend/build.sh. CPython, PyPy and GraalPy need
# Flask installed for the server rows (a row is skipped without it).
import argparse
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
KERNELS = os.path.join(REPO, "scripts", "python-bench", "kernels")
SERVER_APP = os.path.join(REPO, "scripts", "python-backend", "samples", "flask-todo", "src", "main.py")
KERNEL_NAMES = ["nbody", "richards", "fannkuch", "spectral_norm", "dicts_strings"]
PORT = 8080
CN1 = "Codename One Python"


def timed(cmd, cwd=None, env=None):
    """Runs cmd under /usr/bin/time -l; answers (stdout, wall s, peak memory MB).

    Peak memory is the PHYSICAL FOOTPRINT where the OS reports one (macOS): it is what
    the kernel meters a process by and what a per-process budget is measured in, so it
    is the number the budgeted rows below are held to. Elsewhere it is peak RSS."""
    t0 = time.perf_counter()
    p = subprocess.run(["/usr/bin/time", "-l"] + cmd, cwd=cwd, capture_output=True, text=True, env=env)
    wall = time.perf_counter() - t0
    if p.returncode != 0:
        raise RuntimeError("%s failed: %s" % (cmd, p.stderr[-500:]))
    m = re.search(r"(\d+)\s+peak memory footprint", p.stderr) \
        or re.search(r"(\d+)\s+maximum resident set size", p.stderr)
    return p.stdout, wall, int(m.group(1)) / (1024.0 * 1024.0)


def budget_env(mb):
    """The environment that gives a Codename One binary a per-process memory budget.

    CN1_SIMULATE_PROC_MEMORY_LIMIT is the VM's own budget input -- the same code path an
    iOS per-process limit drives -- so the budgeted rows measure what the VM does when it
    knows memory is scarce, not a different build."""
    env = dict(os.environ)
    env["CN1_SIMULATE_PROC_MEMORY_LIMIT"] = str(int(mb) * 1024 * 1024)
    return env


def du_mb(path):
    total = 0
    if os.path.isfile(path):
        return os.path.getsize(path) / 1e6
    for root, _, files in os.walk(path):
        for f in files:
            fp = os.path.join(root, f)
            if not os.path.islink(fp):
                total += os.path.getsize(fp)
    return total / 1e6


def interpreter_home(exe):
    out = subprocess.run([exe, "-c", "import sys; print(sys.base_prefix)"],
                         capture_output=True, text=True).stdout.strip()
    return out


def has_flask(exe):
    return subprocess.run([exe, "-c", "import flask"], capture_output=True).returncode == 0


def flask_size(exe):
    code = ("import os, flask, werkzeug, jinja2, markupsafe, itsdangerous, click, blinker\n"
            "print('\\n'.join(os.path.dirname(m.__file__) for m in "
            "(flask, werkzeug, jinja2, markupsafe, itsdangerous, click, blinker)))")
    out = subprocess.run([exe, "-c", code], capture_output=True, text=True).stdout.split()
    return sum(du_mb(d) for d in out)


def kernel_rows(arms, cn1_dir, budgets):
    rows = []
    for k in KERNEL_NAMES:
        row = {"kernel": k}
        for name, exe in arms:
            out, wall, rss = timed([exe, os.path.join(KERNELS, k + ".py")], cwd=KERNELS)
            row[name] = (best_ms(out), rss)
        out, wall, rss = timed([os.path.join(cn1_dir, k, k)])
        row[CN1] = (best_ms(out), rss)
        for budget in budgets:
            out, wall, rss = timed([os.path.join(cn1_dir, k, k)], env=budget_env(budget))
            row[cn1_budgeted(budget)] = (best_ms(out), rss)
        rows.append(row)
    return rows


def cn1_budgeted(mb):
    return "%s, %d MB budget" % (CN1, mb)


def best_ms(out):
    m = re.search(r"^BENCH \S+ (\d+)", out, re.M)
    return int(m.group(1)) / 1000.0


def wait_up(url, deadline):
    while time.perf_counter() < deadline:
        try:
            urllib.request.urlopen(url, timeout=0.5).read()
            return True
        except Exception:
            time.sleep(0.005)
    return False


def footprint_of(pid):
    """(current, peak) physical footprint in MB from vmmap on macOS; RSS twice elsewhere."""
    out = subprocess.run(["vmmap", "-summary", str(pid)], capture_output=True, text=True).stdout \
        if shutil.which("vmmap") else ""

    def mb(label):
        m = re.search(label + r":\s+([\d.]+)([KMG])", out)
        if not m:
            return None
        return float(m.group(1)) * {"K": 1 / 1024.0, "M": 1.0, "G": 1024.0}[m.group(2)]
    cur, peak = mb(r"Physical footprint"), mb(r"Physical footprint \(peak\)")
    if cur is None:
        cur = peak = rss_of(pid)
    return cur, peak


def rss_of(pid):
    out = subprocess.run(["ps", "-o", "rss=", "-p", str(pid)], capture_output=True, text=True).stdout
    return int(out.strip() or 0) / 1024.0


LOAD_SECONDS = 30
SETTLE_SECONDS = 5
FLASK_KEYS = ("first_ms", "idle_mb", "loaded_mb", "settled_mb", "peak_mb", "loaded_rss_mb", "rps", "p99_ms")


def server_row(cmd, env=None):
    """One server under the same fixed DURATION of full load, not the same request count.

    A fixed count finishes in under a second on a fast server and in half a minute on a
    slow one, so the fast one is measured before its collector has settled and the slow
    one after; the same thirty seconds of load is the same question asked of both."""
    url = "http://127.0.0.1:%d/todos" % PORT
    t0 = time.perf_counter()
    p = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, env=env)
    try:
        if not wait_up(url, t0 + 60):
            raise RuntimeError("server did not start: %s" % cmd)
        first = time.perf_counter() - t0
        idle = footprint_of(p.pid)[0]
        # Warm up, then the measured run; one connection at a time per client,
        # keep-alive off, the same for every server.
        target = "http://127.0.0.1:%d/fib/30" % PORT
        subprocess.run(["ab", "-q", "-n", "2000", "-c", "8", target], capture_output=True)
        res = subprocess.run(["ab", "-q", "-s", "30", "-t", str(LOAD_SECONDS), "-n", "100000000",
                              "-c", "8", target], capture_output=True, text=True)
        out = res.stdout
        m = re.search(r"Requests per second:\s+([\d.]+)", out)
        if m is None:
            raise RuntimeError("ab failed against %s (budget %s): %s"
                               % (cmd, (env or {}).get("CN1_SIMULATE_PROC_MEMORY_LIMIT", "none"),
                                  (res.stderr or out).strip()[-300:]))
        rps = float(m.group(1))
        p99 = re.search(r"\s99%\s+(\d+)", out)
        loaded_rss = rss_of(p.pid)
        loaded, peak = footprint_of(p.pid)
        time.sleep(SETTLE_SECONDS)
        settled = footprint_of(p.pid)[0]
        return (first * 1000.0, idle, loaded, settled, peak, loaded_rss, rps,
                int(p99.group(1)) if p99 else None)
    finally:
        p.terminate()
        try:
            p.wait(5)
        except subprocess.TimeoutExpired:
            p.kill()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cpython", default=shutil.which("python3"))
    ap.add_argument("--pypy")
    ap.add_argument("--graalpy")
    ap.add_argument("--cn1-kernels", required=True)
    ap.add_argument("--cn1-hello", help="native binary of hello.py, for the startup row")
    ap.add_argument("--cn1-server", required=True)
    ap.add_argument("--budget-mb", default="128",
                    help="comma-separated per-process memory budgets; each adds a Codename One arm")
    ap.add_argument("--out")
    ap.add_argument("--json", help="the measurements, for render_charts.py --compare")
    a = ap.parse_args()
    arms = [("CPython", a.cpython)]
    if a.pypy:
        arms.append(("PyPy", a.pypy))
    if a.graalpy:
        arms.append(("GraalPy", a.graalpy))
    budgets = [int(b) for b in a.budget_mb.split(",") if b.strip()]
    CN1BS = [cn1_budgeted(b) for b in budgets]
    names = [n for n, _ in arms] + [CN1] + CN1BS
    lines = []
    w = lines.append
    w("# Codename One Python vs %s\n" % ", ".join(n for n, _ in arms))
    w("Measured on %s, %s. Codename One rows are native binaries "
      "(Python -> Java -> ParparVM C -> clang -O3).\n" % (
          subprocess.run(["uname", "-sm"], capture_output=True, text=True).stdout.strip(),
          time.strftime("%Y-%m-%d")))
    for n, exe in arms:
        v = subprocess.run([exe, "-c", "import sys; print(sys.version.split()[0], sys.implementation.name)"],
                           capture_output=True, text=True).stdout.strip()
        w("- %s: `%s` (%s)" % (n, exe, v))
    w("")

    hello = os.path.join(HERE, "hello.py")
    w("## Startup: `print('hello')`\n")
    w("| | wall ms (best of 10) | peak footprint MB |")
    w("|---|---:|---:|")
    startup = [(n, [exe, hello]) for n, exe in arms]
    if a.cn1_hello:
        startup.append((CN1, [a.cn1_hello]))
    data = {"arms": names, "startup": {}, "kernels": {}, "flask": {}, "deploy": {}}
    for n, cmd in startup:
        runs = [timed(cmd) for _ in range(10)]
        ms, mb = min(r[1] for r in runs) * 1000, min(r[2] for r in runs)
        data["startup"][n] = {"ms": ms, "rss_mb": mb}
        w("| %s | %.1f | %.1f |" % (n, ms, mb))
    w("")

    w("## Two ways to run the same binary\n")
    w("Every Codename One row appears more than once. **%s** is the binary as it ships, on a "
      "machine with memory to spare. Its collector is adaptive: while it has CPU to spare "
      "it holds the heap near the live set, and when collecting starts to cost the "
      "program throughput it lets the heap grow instead, because unused memory buys "
      "nothing and the speed it pays for is real. **%s** is the same binary given a "
      "per-process memory budget, the way iOS limits every app and a container limits a "
      "server. Then memory is the scarce resource and the trade flips: the heap is held "
      "tight however busy the collector is, and the program pays in collections, not "
      "memory.\n" % (CN1, "** and **".join(CN1BS)))
    w("Memory is **physical footprint**, what the kernel meters a process by and what a "
      "budget is measured in, for every runtime alike.\n")
    w("## CPU kernels (fastest of the kernel's rounds, ms; peak footprint MB)\n")
    w("| kernel | " + " | ".join("%s ms | %s MB" % (n, n) for n in names) + " |")
    w("|---|" + "---:|---:|" * len(names))
    for r in kernel_rows(arms, a.cn1_kernels, budgets):
        data["kernels"][r["kernel"]] = {n: {"ms": r[n][0], "rss_mb": r[n][1]} for n in names}
        w("| %s | " % r["kernel"] + " | ".join("%.1f | %.1f" % r[n] for n in names) + " |")
    w("")

    w("## Flask JSON API (`scripts/python-backend/samples/flask-todo`)\n")
    w("Thirty seconds of `ab -c 8 /fib/30` after a 2000-request warm-up -- the same "
      "length of load for every server, not the same request count, so a fast server is "
      "not measured before its collector settles. Footprint is read as the load ends, "
      "again %d seconds later, and at its peak. CPython, PyPy and GraalPy serve with "
      "`app.run()` (Werkzeug's threaded server), Codename One with the same call, which "
      "it lowers onto the native backend HTTP server.\n" % SETTLE_SECONDS)
    w("| | first response ms | idle MB | after load MB | %ds later MB | peak MB | RSS after load MB | req/s | p99 ms |"
      % SETTLE_SECONDS)
    w("|---|---:|---:|---:|---:|---:|---:|---:|---:|")
    servers = [(n, [exe, SERVER_APP], None) for n, exe in arms if has_flask(exe)]
    servers += [(CN1, [a.cn1_server], None)]
    servers += [(cn1_budgeted(b), [a.cn1_server], budget_env(b)) for b in budgets]
    for n, exe in arms:
        if not has_flask(exe):
            w("| %s | (Flask not installed) | | | | | | | |" % n)
    for n, cmd, env in servers:
        r = server_row(cmd, env)
        data["flask"][n] = dict(zip(FLASK_KEYS, r))
        w("| %s | %.0f | %.1f | %.1f | %.1f | %.1f | %.1f | %.0f | %s |" % ((n,) + r))
    w("")
    fl = data["flask"]
    if "CPython" in fl:
        free, cp = fl[CN1], fl["CPython"]
        w("Unconstrained, the server spends memory for speed: %.0f MB after load at "
          "%.0f requests a second. CPython serves %.0f a second in %.0f MB.\n"
          % (free["loaded_mb"], free["rps"], cp["rps"], cp["loaded_mb"]))
        for b in budgets:
            held = fl[cn1_budgeted(b)]
            w("- Under a %d MB budget: %.0f MB after load, %.0f MB at peak, %.0f requests "
              "a second -- %.0f%% of the unconstrained throughput, %.0f times CPython's."
              % (b, held["loaded_mb"], held["peak_mb"], held["rps"],
                 100.0 * held["rps"] / free["rps"], held["rps"] / cp["rps"]))
        w("")

    w("## What you deploy\n")
    w("| | size MB | what |")
    w("|---|---:|---|")
    for n, exe in arms:
        home = interpreter_home(exe)
        extra = flask_size(exe) if has_flask(exe) else 0.0
        data["deploy"][n] = du_mb(home) + extra
        w("| %s | %.1f | interpreter install (`%s`) + Flask and its dependencies (%.1f) |"
          % (n, du_mb(home) + extra, home, extra))
    w("| " + CN1 + " | %.1f | one self-contained executable, Flask included |" % du_mb(a.cn1_server))
    w("")
    for n in [CN1] + CN1BS:
        data["deploy"][n] = du_mb(a.cn1_server)
    if a.json:
        import json
        with open(a.json, "w") as f:
            json.dump(data, f, indent=1)
    text = "\n".join(lines)
    if a.out:
        with open(a.out, "w") as f:
            f.write(text + "\n")
    print(text)


if __name__ == "__main__":
    sys.exit(main())
