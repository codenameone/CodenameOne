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


def timed(cmd, cwd=None):
    """Runs cmd under /usr/bin/time -l; answers (stdout, wall s, peak RSS MB)."""
    t0 = time.perf_counter()
    p = subprocess.run(["/usr/bin/time", "-l"] + cmd, cwd=cwd, capture_output=True, text=True)
    wall = time.perf_counter() - t0
    if p.returncode != 0:
        raise RuntimeError("%s failed: %s" % (cmd, p.stderr[-500:]))
    m = re.search(r"(\d+)\s+maximum resident set size", p.stderr)
    return p.stdout, wall, int(m.group(1)) / (1024.0 * 1024.0)


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


def kernel_rows(arms, cn1_dir):
    rows = []
    for k in KERNEL_NAMES:
        row = {"kernel": k}
        for name, exe in arms:
            out, wall, rss = timed([exe, os.path.join(KERNELS, k + ".py")], cwd=KERNELS)
            row[name] = (best_ms(out), rss)
        out, wall, rss = timed([os.path.join(cn1_dir, k, k)])
        row["Codename One"] = (best_ms(out), rss)
        rows.append(row)
    return rows


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


def rss_of(pid):
    out = subprocess.run(["ps", "-o", "rss=", "-p", str(pid)], capture_output=True, text=True).stdout
    return int(out.strip() or 0) / 1024.0


def server_row(cmd, env=None):
    url = "http://127.0.0.1:%d/todos" % PORT
    t0 = time.perf_counter()
    p = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, env=env)
    try:
        if not wait_up(url, t0 + 60):
            raise RuntimeError("server did not start: %s" % cmd)
        first = time.perf_counter() - t0
        idle = rss_of(p.pid)
        # Warm up, then the measured run; one connection at a time per
        # client, keep-alive off, the same for every server.
        ab = ["ab", "-q", "-n", "20000", "-c", "8", "http://127.0.0.1:%d/fib/30" % PORT]
        subprocess.run(ab[:2] + ["-n", "2000"] + ab[4:], capture_output=True)
        out = subprocess.run(ab, capture_output=True, text=True).stdout
        rps = float(re.search(r"Requests per second:\s+([\d.]+)", out).group(1))
        p99 = re.search(r"\s99%\s+(\d+)", out)
        loaded = rss_of(p.pid)
        return first * 1000.0, idle, loaded, rps, int(p99.group(1)) if p99 else None
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
    ap.add_argument("--out")
    a = ap.parse_args()
    arms = [("CPython", a.cpython)]
    if a.pypy:
        arms.append(("PyPy", a.pypy))
    if a.graalpy:
        arms.append(("GraalPy", a.graalpy))
    names = [n for n, _ in arms] + ["Codename One"]
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
    w("| | wall ms (best of 10) | peak RSS MB |")
    w("|---|---:|---:|")
    startup = [(n, [exe, hello]) for n, exe in arms]
    if a.cn1_hello:
        startup.append(("Codename One", [a.cn1_hello]))
    for n, cmd in startup:
        runs = [timed(cmd) for _ in range(10)]
        w("| %s | %.1f | %.1f |" % (n, min(r[1] for r in runs) * 1000, min(r[2] for r in runs)))
    w("")

    w("## CPU kernels (fastest of the kernel's rounds, ms; peak RSS MB)\n")
    w("| kernel | " + " | ".join("%s ms | %s MB" % (n, n) for n in names) + " |")
    w("|---|" + "---:|---:|" * len(names))
    for r in kernel_rows(arms, a.cn1_kernels):
        w("| %s | " % r["kernel"] + " | ".join("%.1f | %.1f" % r[n] for n in names) + " |")
    w("")

    w("## Flask JSON API (`scripts/python-backend/samples/flask-todo`)\n")
    w("`ab -n 20000 -c 8 /fib/30` after a 2000-request warm-up. CPython, PyPy and "
      "GraalPy serve with `app.run()` (Werkzeug's threaded server), Codename One "
      "with the same call, which it lowers onto the native backend HTTP server.\n")
    w("| | first response ms | idle RSS MB | RSS after load MB | req/s | p99 ms |")
    w("|---|---:|---:|---:|---:|---:|")
    for n, exe in arms:
        if not has_flask(exe):
            w("| %s | (Flask not installed) | | | | |" % n)
            continue
        r = server_row([exe, SERVER_APP])
        w("| %s | %.0f | %.1f | %.1f | %.0f | %s |" % ((n,) + r))
    r = server_row([a.cn1_server])
    w("| Codename One | %.0f | %.1f | %.1f | %.0f | %s |" % (r[0], r[1], r[2], r[3], r[4]))
    w("")

    w("## What you deploy\n")
    w("| | size MB | what |")
    w("|---|---:|---|")
    for n, exe in arms:
        home = interpreter_home(exe)
        extra = flask_size(exe) if has_flask(exe) else 0.0
        w("| %s | %.1f | interpreter install (`%s`) + Flask and its dependencies (%.1f) |"
          % (n, du_mb(home) + extra, home, extra))
    w("| Codename One | %.1f | one self-contained executable, Flask included |" % du_mb(a.cn1_server))
    w("")
    text = "\n".join(lines)
    if a.out:
        with open(a.out, "w") as f:
            f.write(text + "\n")
    print(text)


if __name__ == "__main__":
    sys.exit(main())
