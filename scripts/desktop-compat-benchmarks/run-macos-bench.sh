#!/usr/bin/env bash
#
# The macOS runtime half of the desktop-compat benchmarks. RUN IT BY HAND: it
# opens the applications' windows, one after another, on the logged-in desktop.
#
#   1. bench.sh --stage build ... --work DIR --name NAME      (sizes; opens nothing)
#   2. run-macos-bench.sh --work DIR --name NAME              (this script)
#   3. report.py DIR/NAME/result.json
#
# For the JVM baseline (the jlink image bench.sh built, default flags, and the
# same tuned flags the Linux leg uses with a fixed -Xmx) and for the native
# Codename One .app, it measures per launch:
#
#   start-up     process start until its first window is on screen (firstwindow.swift)
#   idle RAM     RSS from ps, and the physical footprint from footprint(1), after
#                the idle window
#   idle CPU     CPU time accumulated over the second half of the idle window
#   peak RSS     /usr/bin/time -l
#
# There is no scripted input here: synthesising clicks on macOS needs the
# Accessibility permission for whatever runs this script, and a benchmark that
# silently measures a different thing when the permission is missing is worse
# than one that does not claim to. "After interaction" cells stay empty on macOS.
#
# Usage: run-macos-bench.sh --work DIR --name NAME [--runs N] [--idle SECONDS]
#                           [--tuned-xmx MB]
set -euo pipefail

HERE="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)"
WORK="" NAME="" RUNS=7 IDLE=10 TUNED_XMX=64
while [ "$#" -gt 0 ]; do
  case "$1" in
    --work) WORK="$2"; shift 2 ;;
    --name) NAME="$2"; shift 2 ;;
    --runs) RUNS="$2"; shift 2 ;;
    --idle) IDLE="$2"; shift 2 ;;
    --tuned-xmx) TUNED_XMX="$2"; shift 2 ;;
    -h|--help) sed -n '2,27p' "$0"; exit 0 ;;
    *) echo "run-macos-bench.sh: unknown argument $1" >&2; exit 2 ;;
  esac
done
if [ -z "$WORK" ] || [ -z "$NAME" ]; then
  echo "run-macos-bench.sh: --work and --name are required" >&2
  exit 2
fi
if [ "$(uname -s)" != "Darwin" ]; then
  echo "run-macos-bench.sh: this is the macOS leg; use bench.sh on Linux" >&2
  exit 2
fi

D="$(CDPATH='' cd -- "$WORK/$NAME" && pwd)"
PARTS="$D/parts" LOGS="$D/logs/macos-run" TMP="$D/tmp"
mkdir -p "$PARTS" "$LOGS" "$TMP"

HELPER="$TMP/firstwindow"
if [ ! -x "$HELPER" ] || [ "$HERE/firstwindow.swift" -nt "$HELPER" ]; then
  xcrun swiftc -O -o "$HELPER" "$HERE/firstwindow.swift"
fi

measure() { # <label> <command...>
  local LABEL="$1"
  shift
  python3 - "$LABEL" "$PARTS/run.$LABEL.json" "$LOGS" "$HELPER" "$RUNS" "$IDLE" "$D" "$@" <<'PY'
import json, os, re, signal, statistics, subprocess, sys, time

label, out, logs, helper, runs, idle, cwd = sys.argv[1:8]
command = sys.argv[8:]
runs, idle = int(runs), float(idle)


def children(pid):
    listing = subprocess.run(["pgrep", "-P", str(pid)], stdout=subprocess.PIPE,
                             universal_newlines=True).stdout.split()
    return [int(p) for p in listing]


def cpu_seconds(pid):
    text = subprocess.run(["ps", "-o", "time=", "-p", str(pid)], stdout=subprocess.PIPE,
                          universal_newlines=True).stdout.strip()
    if not text:
        return None
    parts = [float(p) for p in text.split(":")]
    seconds = 0.0
    for part in parts:
        seconds = seconds * 60.0 + part
    return seconds


def rss_kb(pid):
    text = subprocess.run(["ps", "-o", "rss=", "-p", str(pid)], stdout=subprocess.PIPE,
                          universal_newlines=True).stdout.strip()
    return int(text) if text.isdigit() else None


def footprint_kb(pid):
    try:
        text = subprocess.run(["footprint", "-p", str(pid)], stdout=subprocess.PIPE,
                              stderr=subprocess.DEVNULL, universal_newlines=True, timeout=30).stdout
    except (OSError, subprocess.SubprocessError):
        return None
    match = re.search(r"phys_footprint:\s*([\d.]+)\s*(KB|MB|GB)", text)
    if not match:
        return None
    scale = {"KB": 1, "MB": 1024, "GB": 1024 * 1024}[match.group(2)]
    return int(float(match.group(1)) * scale)


def launch(tag, idle_seconds):
    report = os.path.join(logs, "%s-%s.time" % (label, tag))
    log = open(os.path.join(logs, "%s-%s.log" % (label, tag)), "wb")
    started = time.time()
    process = subprocess.Popen(["/usr/bin/time", "-l", "-o", report] + command, cwd=cwd,
                               stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    result = {"tag": tag, "ok": False}
    pid = None
    try:
        deadline = time.time() + 20
        while time.time() < deadline and not pid:
            kids = children(process.pid)
            pid = kids[0] if kids else None
            if not pid:
                time.sleep(0.002)
        if not pid:
            result["error"] = "the application did not start"
            return result
        seen = subprocess.run([helper, str(pid), "90"], stdout=subprocess.PIPE, universal_newlines=True)
        if seen.returncode != 0:
            result["error"] = "no window within 90 s"
            return result
        result["window_ms"] = round((float(seen.stdout.strip()) - started) * 1000.0, 1)
        result["ok"] = True
        if idle_seconds > 0:
            time.sleep(idle_seconds / 2.0)
            before, wall = cpu_seconds(pid), time.time()
            time.sleep(idle_seconds / 2.0)
            after = cpu_seconds(pid)
            idle_result = {"rss_kb": rss_kb(pid), "footprint_kb": footprint_kb(pid)}
            if before is not None and after is not None:
                idle_result["cpu_percent"] = round(100.0 * (after - before) / (time.time() - wall), 2)
            result["idle"] = idle_result
        return result
    finally:
        if pid:
            try:
                os.kill(pid, signal.SIGTERM)
            except OSError:
                pass
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
        log.close()
        try:
            with open(report) as handle:
                match = re.search(r"(\d+)\s+maximum resident set size", handle.read())
            if match:
                result["peak_rss_kb"] = int(match.group(1)) // 1024
        except OSError:
            pass


def summary(values):
    if not values:
        return None
    return {"median": round(statistics.median(values), 1), "min": round(min(values), 1),
            "max": round(max(values), 1), "runs": len(values)}


launch("primer", 0)
warm = [launch("warm-%d" % i, 0) for i in range(runs)]
sessions = [launch("session-%d" % i, idle) for i in range(3)]
good = [s for s in sessions if s.get("ok") and "idle" in s]


def middle(field):
    values = [s["idle"][field] for s in good if s["idle"].get(field) is not None]
    return round(statistics.median(values), 2) if values else None


peaks = [s["peak_rss_kb"] for s in good if "peak_rss_kb" in s]
result = {
    "label": label, "command": command, "idle_seconds": idle, "ok": bool(good),
    "errors": ["%s: %s" % (s["tag"], s["error"]) for s in warm + sessions if "error" in s],
    # Only warm: purging the page cache on a desktop Mac needs sudo, and this
    # script asks for nothing it does not need.
    "startup_warm": {"window_ms": summary([s["window_ms"] for s in warm if s.get("ok")]),
                     "first_paint_ms": None, "samples": warm},
    "idle": {f: middle(f) for f in ("rss_kb", "footprint_kb", "cpu_percent")},
    "peak_rss_kb": round(statistics.median(peaks)) if peaks else None,
    "sessions": sessions,
}
with open(out, "w") as handle:
    json.dump(result, handle, indent=2, sort_keys=True)
print("%s: window %s ms, idle %s" % (label, result["startup_warm"]["window_ms"], result["idle"]))
PY
}

if [ -x "$D/baseline/runtime/bin/java" ]; then
  CP=""
  for JAR in "$D/baseline/app"/*.jar; do
    CP="$CP:$JAR"
  done
  CP="${CP#:}"
  MAIN="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["main_class"])' "$PARTS/baseline.json")"
  measure jvm_default "$D/baseline/runtime/bin/java" -cp "$CP" "$MAIN"
  measure jvm_tuned "$D/baseline/runtime/bin/java" -XX:+UseSerialGC -Xss256k -XX:TieredStopAtLevel=1 \
    "-Xmx${TUNED_XMX}m" -cp "$CP" "$MAIN"
  python3 - "$PARTS/tuned.json" "$TUNED_XMX" <<'PY'
import json, sys
with open(sys.argv[1], "w") as handle:
    json.dump({"xmx_mb": int(sys.argv[2]), "appcds_bytes": None,
               "flags": "-XX:+UseSerialGC -Xss256k -XX:TieredStopAtLevel=1 -Xmx%sm (fixed, not searched; no AppCDS)"
                        % sys.argv[2]}, handle, indent=2)
PY
else
  echo "no baseline runtime image in $D/baseline; run bench.sh --stage build first" >&2
fi

if [ -f "$D/cn1/executable.txt" ]; then
  BUNDLE="$D/cn1/dist/$(cat "$D/cn1/executable.txt")"
  measure cn1_native "$BUNDLE/Contents/MacOS/$(basename "$BUNDLE" .app)"
else
  echo "no native Codename One build in $D/cn1; its cells stay empty" >&2
fi

python3 "$HERE/benchutil.py" assemble "$PARTS" "$D/result.json"
echo "wrote $D/result.json"
