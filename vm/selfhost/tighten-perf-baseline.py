#!/usr/bin/env python3
"""Lower ParparVM performance baselines that master has beaten consistently.

A pull request that runs faster or smaller than its baseline passes the performance gate
rather than failing it: failing there turned every real VM win into a red master, and
red pull requests, until somebody rebaselined. The cost is that an unrecorded win could
be given back by a later change without failing anything. This closes that gap from the
other side: perf-baseline.yml runs it nightly over the perf-results.json of master's
recent gate runs, and a row whose EVERY recent run beat its baseline by a clear margin
gets the baseline lowered to the WORST of those runs. A later change that gives the win
back then fails as a regression.

A row is tightened only when all of these hold, per baseline key, benchmark, core count
and metric:

  - at least --min-runs master runs (default 3) were judged against the row's CURRENT
    value (a run judged against an older value says nothing about this one);
  - every one of them is below the baseline by more than half the row's tolerance, and
    for RAM by more than the policy floor in absolute terms;
  - no pull request overlay is pending for the row (the fold runs first and normally
    leaves none).

The new value is the largest of those runs' medians, so the tightened row still holds
every run that justified it, with the full tolerance above it for noise.

Usage: tighten-perf-baseline.py [--root DIR] [--min-runs N] RESULTS_DIR_OR_FILE...
Prints one line per tightened row; exit status 0 either way.
"""

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import perf_baseline  # noqa: E402

METRICS = ('time', 'memory')


def result_files(paths):
    for p in map(Path, paths):
        if p.is_dir():
            for f in sorted(p.rglob('perf-results.json')):
                yield f
        elif p.is_file():
            yield p


def observations(reports, base):
    """{(key, bench, cores, metric): [median, ...]} for master runs judged against the
    row's current value."""
    seen = {}
    for report in reports:
        if report.get('pr') or report.get('error'):
            continue
        key = report.get('baseline_key')
        if not key or key not in base:
            continue
        for bench, per_cores in (report.get('results') or {}).items():
            for cores, entry in per_cores.items():
                if 'failed' in entry:
                    continue
                row = base[key].get(bench, {}).get(cores)
                if row is None:
                    continue
                for metric in METRICS:
                    e = entry.get(metric) or {}
                    current = row.get(metric)
                    judged = e.get('baseline')
                    if current is None or judged is None or 'median' not in e:
                        continue
                    if abs(judged - current) > 1e-9:
                        continue
                    seen.setdefault((key, bench, cores, metric), []).append(e['median'])
    return seen


def plan(seen, base, policy, overlay_rows, min_runs):
    changes = []
    for (key, bench, cores, metric), medians in sorted(seen.items()):
        if len(medians) < min_runs or (key, bench, cores) in overlay_rows:
            continue
        row = base[key][bench][cores]
        current = row[metric]
        tolerance = max(row.get('tolerance', {}).get(metric, 0.0), policy['tolerance'][metric])
        floor = policy.get('floor', {}).get(metric, 0.0)
        worst = max(medians)
        if worst < current * (1 - tolerance / 2) and current - worst > floor:
            changes.append((key, bench, cores, metric, current, round(worst, 3), len(medians)))
    return changes


def overlay_row_keys(root):
    keys = set()
    for _, overlay in perf_baseline.load_overlays(root):
        for section in ('calibrate', 'rebaseline'):
            for key, benches in (overlay.get(section) or {}).items():
                for bench, per_cores in benches.items():
                    for cores in per_cores:
                        keys.add((key, bench, cores))
    return keys


def tighten(root, paths, min_runs=3):
    root = Path(root)
    base = perf_baseline.load_base(root)
    policy = perf_baseline.load_policy(root)
    reports = []
    for f in result_files(paths):
        try:
            reports.append(json.loads(f.read_text()))
        except (OSError, ValueError) as error:
            print('tighten: skipping %s: %s' % (f, error), file=sys.stderr)
    changes = plan(observations(reports, base), base, policy, overlay_row_keys(root), min_runs)
    for key, bench, cores, metric, _, new, _ in changes:
        base[key][bench][cores][metric] = new
    if changes:
        perf_baseline.write_base(root, base)
        # The result must still load and validate exactly like any other baseline.
        perf_baseline.load(root)
    return changes


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--root', default=str(perf_baseline.ROOT))
    parser.add_argument('--min-runs', type=int, default=3)
    parser.add_argument('paths', nargs='+')
    args = parser.parse_args(argv)
    changes = tighten(args.root, args.paths, args.min_runs)
    for key, bench, cores, metric, old, new, runs in changes:
        print('tightened %s %s/%s %s: %.3f -> %.3f (%d master runs, all below)'
              % (key, bench, cores, 'RAM' if metric == 'memory' else metric, old, new, runs))
    if not changes:
        print('tighten: no row was beaten consistently enough to tighten')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
