#!/usr/bin/env python3
"""Formats and compares JavaScript-target throughput benchmark results.

Split out of run-javascript-throughput-benchmark.sh so the comparison rule
lives somewhere it can be read: a workload whose CHECKSUM moved is not
comparable on time, and saying so is the whole point. "Fewer generators" and
"fewer calls" are easy to confuse, so a run that got faster by doing less work
is the failure mode this benchmark is most exposed to.
"""
import json
import os
import re
import sys

BENCH_RE = re.compile(r"^BENCH id=(\S+) ns=(\d+) checksum=(-?\d+)$")
SUITE_RE = re.compile(r"^BENCHSUITE checksum=(-?\d+)$")
PROBE_RE = re.compile(r"^BENCHPROBE syncDroveGenerator=(\d+)$")
# A workload the dispatch work cannot affect. If these move, the run measured
# the machine and every other delta in it is suspect.
CONTROLS = ("arithControl", "suspendControl")


def parse_raw(path):
    workloads, suite, drove = {}, None, 0
    with open(path, encoding="utf-8", errors="replace") as handle:
        for line in handle:
            line = line.strip()
            match = BENCH_RE.match(line)
            if match:
                workloads[match.group(1)] = {
                    "ns": int(match.group(2)),
                    "checksum": int(match.group(3)),
                }
                continue
            match = SUITE_RE.match(line)
            if match:
                suite = int(match.group(1))
                continue
            match = PROBE_RE.match(line)
            if match:
                drove = int(match.group(1))
    return workloads, suite, drove


def parse_suspension(path):
    if not path or not os.path.isfile(path):
        return {}
    out = {}
    with open(path, encoding="utf-8", errors="replace") as handle:
        for line in handle:
            parts = line.split()
            if len(parts) == 2 and parts[0] in ("TOTAL", "SYNC", "SUSPENDING", "SUSPENDING_SIGS"):
                out[parts[0].lower()] = int(parts[1])
            elif line.startswith("#") or line.startswith("M "):
                if line.startswith("M "):
                    break
    return out


def main():
    workloads, suite, drove = parse_raw(os.environ["RAW"])
    if not workloads or suite is None:
        print("no benchmark results parsed", file=sys.stderr)
        return 1

    result = {
        "workloads": workloads,
        "suite_checksum": suite,
        "bundle": {
            "translated_bytes": int(os.environ.get("BUNDLE_BYTES", 0)),
            "yield_sites": int(os.environ.get("YIELDS", 0)),
            "generators": int(os.environ.get("GENERATORS", 0)),
        },
        "suspension": parse_suspension(os.environ.get("REPORT")),
        # Non-zero means the sync dispatcher met a generator: the
        # classification was wrong and the runtime absorbed it.
        "sync_drove_generator": drove,
    }

    baseline_path = os.environ.get("BASELINE") or ""
    baseline = None
    if baseline_path:
        with open(baseline_path, encoding="utf-8") as handle:
            baseline = json.load(handle)

    width = max(len(name) for name in workloads)
    if baseline is None:
        print("%-*s %12s" % (width, "workload", "ms"))
        for name in sorted(workloads):
            print("%-*s %12.3f" % (width, name, workloads[name]["ns"] / 1e6))
    else:
        print("%-*s %12s %12s %9s" % (width, "workload", "base ms", "new ms", "change"))
        mismatched = []
        for name in sorted(workloads):
            new = workloads[name]
            old = baseline.get("workloads", {}).get(name)
            if old is None:
                print("%-*s %12s %12.3f %9s" % (width, name, "-", new["ns"] / 1e6, "new"))
                continue
            if old["checksum"] != new["checksum"]:
                mismatched.append(name)
                print("%-*s %12.3f %12.3f %9s" % (
                    width, name, old["ns"] / 1e6, new["ns"] / 1e6, "CHECKSUM"))
                continue
            # Negative percent means faster.
            change = (new["ns"] - old["ns"]) / float(old["ns"]) * 100.0
            flag = " <-control" if name in CONTROLS else ""
            print("%-*s %12.3f %12.3f %8.1f%%%s" % (
                width, name, old["ns"] / 1e6, new["ns"] / 1e6, change, flag))
        for key, label in (("translated_bytes", "translated bytes"),
                           ("yield_sites", "yield* sites"),
                           ("generators", "generators")):
            old = baseline.get("bundle", {}).get(key)
            new = result["bundle"][key]
            if old:
                print("%-*s %12d %12d %8.1f%%" % (
                    width, label, old, new, (new - old) / float(old) * 100.0))
        if mismatched:
            print("\nREFUSING the comparison: checksum changed for %s."
                  "\nA workload that computes something different cannot be compared on time."
                  % ", ".join(mismatched), file=sys.stderr)
            return 1

    if drove:
        print("\nWARNING: the sync virtual dispatcher met a generator %d time(s)."
              "\nThe analysis classified a signature synchronous that is not; the runtime"
              "\nabsorbed it by stepping the generator once. Fix the classification --"
              "\ndo not read the timings as a clean result." % drove, file=sys.stderr)

    if os.environ.get("JSON_OUT"):
        with open(os.environ["JSON_OUT"], "w", encoding="utf-8") as handle:
            json.dump(result, handle, indent=2, sort_keys=True)
            handle.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
