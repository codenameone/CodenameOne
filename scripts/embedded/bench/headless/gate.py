#!/usr/bin/env python3
"""Holds the Codename One backend to its headless benchmark results.

    gate.py <results.txt> <census-line> [summary.md]

results.txt is what run.sh printed with RUNTIMES including codenameone and rust;
census-line is census.sh's output ("bytes_per_request objects_per_request").
Prints a table of the medians beside the limits, appends it to summary.md when
given (CI passes $GITHUB_STEP_SUMMARY), and exits 1 if any limit is broken.

Speed is judged against Rust's axum measured in the same job, never in absolute
requests per second: a CI runner's speed varies from one machine to the next,
while two servers taking turns on the same cores see the same machine. Memory
and allocation are judged absolutely, since neither depends on the CPU.

The limits sit below what the developer guide reports, with room for runner
noise. When a change moves a number for good, move its limit with it, in the
same change, and refresh the guide from the job's uploaded results.
"""

import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import charts  # noqa: E402

# Columns of charts.server_medians.
START, IDLE_PSS, STEADY_CPU, PEAK_RPS, PEAK_P99, PEAK_PSS = 0, 1, 5, 7, 8, 9

# A request may allocate the one object the demo's handler returns and nothing
# else; the runtime's own share is zero. The counters are not atomic, so a
# busy run can read a little under one object per request, never over it.
MAX_OBJECTS_PER_REQUEST = 1.05
# The census counts what was asked for: the handler's Message is 16 bytes,
# which the heap rounds up to a 24 byte slot.
MAX_BYTES_PER_REQUEST = 24.0
# Peak throughput as a share of Rust's; the guide reports about 1.3. On the
# CI runner the two measure within a few percent of each other, and passing runs
# have led Rust by 0.5% to 2.7% -- one fell 0.7% short with the request path
# unchanged. The floor sits below that spread so it catches a real loss, not
# a noisy run.
MIN_RPS_VS_RUST = 0.97
# p99 at peak load, against Rust's plus a fixed allowance for the timer's grain.
MAX_P99_VS_RUST = 1.5
P99_ALLOWANCE_MS = 0.1
# Start, against Rust's plus an allowance: both are a few tens of milliseconds.
MAX_START_VS_RUST = 1.5
START_ALLOWANCE_MS = 20.0
# CPU at a steady 200 requests per second, against Rust's.
MAX_STEADY_CPU_VS_RUST = 1.5
STEADY_CPU_ALLOWANCE = 1.0
# Proportional set size in MB; the guide reports 9.2 idle and about 23 at peak.
MAX_IDLE_MB = 12.0
MAX_PEAK_MB = 32.0
# Runs per runtime the medians need after the first round is dropped.
MIN_RUNS = 2


def warm_start(rows, runtime):
    """The median start of the warm runs after the first round, as server_medians skips it."""
    starts = [charts.number(fields[2]) for fields in rows if fields[0] == runtime][2:]
    modes = [fields[1] for fields in rows if fields[0] == runtime][2:]
    return charts.median([v for v, mode in zip(starts, modes) if mode == "warm"])


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    results, census = sys.argv[1], sys.argv[2].split()
    rows = charts.read_rows(results, 12)
    failed_runs = [line.strip() for line in open(results) if " failed:" in line]
    medians = charts.server_medians(results)
    counts = {}
    for fields in rows:
        counts[fields[0]] = counts.get(fields[0], 0) + 1

    checks = []

    def check(name, value, limit, ok, unit=""):
        checks.append((name, value, limit, ok, unit))

    for line in failed_runs:
        check("run failed: " + line, 0, 0, False)
    for runtime in ("codenameone", "rust"):
        kept = counts.get(runtime, 0) - 2
        check(runtime + " runs measured", kept, MIN_RUNS, kept >= MIN_RUNS)
    if all(ok for _, _, _, ok, _ in checks) and len(census) == 2:
        cn1, rust = medians["codenameone"], medians["rust"]
        bytes_per, objects_per = float(census[0]), float(census[1])
        check("objects allocated per request", objects_per, MAX_OBJECTS_PER_REQUEST,
              objects_per <= MAX_OBJECTS_PER_REQUEST)
        check("bytes allocated per request", bytes_per, MAX_BYTES_PER_REQUEST,
              bytes_per <= MAX_BYTES_PER_REQUEST, " B")
        floor = rust[PEAK_RPS] * MIN_RPS_VS_RUST
        check("peak requests/s (Rust {:,.0f})".format(rust[PEAK_RPS]), cn1[PEAK_RPS], floor,
              cn1[PEAK_RPS] >= floor)
        ceiling = rust[PEAK_P99] * MAX_P99_VS_RUST + P99_ALLOWANCE_MS
        check("peak p99 (Rust {:.2f} ms)".format(rust[PEAK_P99]), cn1[PEAK_P99], ceiling,
              cn1[PEAK_P99] <= ceiling, " ms")
        # Warm starts only. A cold start follows a page-cache drop and measures the
        # runner's disk: Rust's swung from 36 to 192 ms within one job. The median
        # of two cold and two warm runs is then the mean of one of each, and moved
        # between 33 and 71 ms with no change to the server.
        cn1_warm, rust_warm = warm_start(rows, "codenameone"), warm_start(rows, "rust")
        ceiling = rust_warm * MAX_START_VS_RUST + START_ALLOWANCE_MS
        check("warm start (Rust {:.0f} ms)".format(rust_warm), cn1_warm, ceiling,
              cn1_warm <= ceiling, " ms")
        ceiling = rust[STEADY_CPU] * MAX_STEADY_CPU_VS_RUST + STEADY_CPU_ALLOWANCE
        check("steady CPU % (Rust {:.1f})".format(rust[STEADY_CPU]), cn1[STEADY_CPU], ceiling,
              cn1[STEADY_CPU] <= ceiling)
        check("idle memory", cn1[IDLE_PSS] / 1000.0, MAX_IDLE_MB,
              cn1[IDLE_PSS] / 1000.0 <= MAX_IDLE_MB, " MB")
        check("peak memory", cn1[PEAK_PSS] / 1000.0, MAX_PEAK_MB,
              cn1[PEAK_PSS] / 1000.0 <= MAX_PEAK_MB, " MB")
    elif len(census) != 2:
        check("census line '{}'".format(" ".join(census)), 0, 0, False)

    lines = ["| Check | Codename One | Limit | |", "|---|---|---|---|"]
    for name, value, limit, ok, unit in checks:
        lines.append("| {} | {:,.2f}{} | {:,.2f}{} | {} |".format(
            name, value, unit, limit, unit, "pass" if ok else "**FAIL**"))
    report = "\n".join(lines)
    print(report)
    if len(sys.argv) > 3:
        with open(sys.argv[3], "a") as summary:
            summary.write("### Backend performance gate\n\n" + report + "\n")
    return 0 if all(ok for _, _, _, ok, _ in checks) else 1


if __name__ == "__main__":
    sys.exit(main())
