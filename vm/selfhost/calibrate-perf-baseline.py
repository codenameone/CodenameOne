#!/usr/bin/env python3
"""Writes a pull request's baseline changes, pr/<number>.json, from CI runs' perf-results.json.

    python3 vm/selfhost/calibrate-perf-baseline.py [--pr N] [--reason TEXT] [--all]
                                                   [--only BENCH,...] [--metric time|memory]
                                                   RESULTS.json ...

Pass the perf-results.json of the runs the gate failed on -- from the artifacts
ci-perf-gate.sh leaves (linux-screenshot-raw-*/perf, windows-port-screenshot-raw-*/perf,
macos-ui-tests/perf). The rows land in vm/selfhost/perf-baseline/pr/<N>.json, which only
this pull request writes, so the change is in its diff and in nobody else's; the nightly
fold moves it into base/ after the merge (see perf_baseline.py). --pr defaults to the pull
request of the checked-out branch, asked of the GitHub CLI.

What it writes, per platform, benchmark and core setting the runs measured:
  calibrate   for a row the baseline does not have (a runner CPU model no run had met).
  rebaseline  for a row the runs put outside its tolerance, either way: a change that
              moved performance on purpose. Only the metric that moved is replaced, and
              --reason is required -- the reason is what a reviewer reads beside the
              number. --all rebaselines every measured row instead, for recalibrating a
              row's noise from several runs of unchanged code; --metric limits that to one
              metric, so recalibrating a noisy RAM figure leaves a steady time alone.

How a row is computed:
  baseline   the median of the runs' median ratios.
  tolerance  max(the global tolerance, SPREAD_MARGIN x the observed spread), where the spread
             is how far the runs' ratios reach from that median, in either direction --
             an improvement fails the gate too, so a tolerance learned only from the high
             side would fail unchanged code on its low runs. A handful of runs
             under-samples the tail, hence the margin. Measured: across five runs most
             rows moved under 5%, while objectAllocation moved 20-37% on every platform
             and Windows x64's translation rows up to 48% -- one global tolerance either
             fails those on noise or waves real regressions through the rest.
A row measured fewer than MIN_RUNS_FOR_OWN_SPREAD times cannot estimate its own spread.
A new row takes the larger of what it measured and the widest tolerance any row needed
for the same benchmark and metric; a rebaselined row keeps at least its own.

Rows are per CPU MODEL (`platform@model`, the model as perf-gate.cpu_class names it):
hosted pools mix microarchitectures whose ratios differ by more than any tolerance
absorbs. A run whose CPU is unknown feeds the plain `platform` rows instead.
"""
import argparse
import copy
import importlib.util
import json
import math
import statistics
import sys
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent


def _load(name, file):
    spec = importlib.util.spec_from_file_location(name, HERE / file)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


perf_gate = _load('perf_gate', 'perf-gate.py')
perf_baseline = _load('perf_baseline', 'perf_baseline.py')
SPREAD_MARGIN = 1.5
# Fewer runs than this cannot estimate a row's spread: EPYC 7763's hello row, from two
# runs, failed unchanged code at +15.7% against a 15% tolerance. Such a row takes the
# widest tolerance its benchmark needed anywhere, as a single-run row always did.
MIN_RUNS_FOR_OWN_SPREAD = 5
METRICS = perf_baseline.METRICS


def round_up(value, step=0.05):
    return round(math.ceil(value / step - 1e-9) * step, 2)


def spread_tolerance(values, base, floor):
    """The tolerance a set of runs needs to pass themselves, or `floor` if smaller."""
    if len(values) < 2:
        return floor
    spread = max(max(values) / base - 1, 1 - min(values) / base)
    return max(floor, round_up(spread * SPREAD_MARGIN))


def collect(paths, only=None, rows=None):
    """platform-key -> (benchmark, cores) -> metric -> [median ratio per run].

    A run feeds the row that JUDGED it, chosen exactly as the gate chooses
    (perf_gate.baseline_key over `rows`): macOS reports "Apple M1 (Virtual)" yet is judged
    by the plain macos-arm64 row, and keying it by model would have rebaselined nothing and
    calibrated a new per-model row beside the one that failed. A run no row judged
    calibrates its own CPU model's row."""
    runs = defaultdict(lambda: defaultdict(lambda: defaultdict(list)))
    for path in paths:
        report = json.loads(Path(path).read_text())
        if report.get('error') or report.get('failures'):
            raise SystemExit('%s did not complete; calibrate only from complete runs' % path)
        cls = perf_gate.cpu_class(report.get('cpu'))
        key = perf_gate.baseline_key(rows or {}, report['platform'], report.get('cpu'))
        if key is None or key not in (rows or {}):
            key = '%s@%s' % (report['platform'], cls) if cls else report['platform']
        for bench, by_cores in report['results'].items():
            if only and bench not in only:
                continue
            for cores, entry in by_cores.items():
                for metric in METRICS:
                    runs[key][(bench, cores)][metric].append(entry[metric]['median'])
    return runs


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--root', default=str(perf_baseline.ROOT))
    parser.add_argument('--pr', type=int, help='the pull request number (default: ask gh)')
    parser.add_argument('--reason', help='why the rebaselined rows moved')
    parser.add_argument('--all', action='store_true',
                        help='rebaseline every measured row, not only those out of tolerance')
    parser.add_argument('--only', help='comma-separated benchmark ids to take from the runs')
    parser.add_argument('--metric', choices=METRICS,
                        help='rebaseline only this metric (a new row always gets both)')
    parser.add_argument('results', nargs='+')
    args = parser.parse_args(argv)

    root = Path(args.root)
    number = args.pr or perf_baseline.pr_number()
    if number is None:
        raise SystemExit('No pull request number: pass --pr N (the overlay is named after it)')
    policy = perf_baseline.load_policy(root)
    tolerance, floor = policy['tolerance'], policy['floor']
    base = perf_baseline.load_base(root)
    overlays = perf_baseline.load_overlays(root)
    own = dict(overlays).get(number, {})
    # What the gate judged against (this pull request's earlier rows included), and the
    # baseline as it stands without them -- which is what a rebaseline's "from" names,
    # since this run replaces this pull request's earlier rebaseline rather than stacking.
    others, _ = perf_baseline.resolve(base, [o for o in overlays if o[0] != number], tolerance)
    try:
        judged, _ = perf_baseline.resolve(base, overlays, tolerance)
    except perf_baseline.BaselineError:
        # This pull request's own overlay went stale (another merged change moved a row it
        # rebaselines), and perf-gate.py then judged the run without it. Do the same, and
        # rewrite every stale row below from these fresh runs -- which is the recovery the
        # stale-overlay message asks for.
        judged = others
    stale = set()
    for key, bench, cores, row in perf_baseline._rows('own', own.get('rebaseline', {})):
        now = others.get(key, {}).get(bench, {}).get(cores)
        if now is None or not perf_baseline._same(row['from'], now):
            stale.add((key, bench, cores))
    runs = collect(args.results, set(args.only.split(',')) if args.only else None, judged)

    widest = defaultdict(float)   # (benchmark, metric) -> widest tolerance any row has
    for _, bench, _, row in perf_baseline._rows('baseline', judged):
        for metric, tol in row.get('tolerance', {}).items():
            widest[(bench, metric)] = max(widest[(bench, metric)], tol)
    for benches in runs.values():
        for (bench, _), metrics in benches.items():
            for metric, values in metrics.items():
                own_tol = spread_tolerance(values, statistics.median(values), tolerance[metric])
                if len(values) > 1:
                    widest[(bench, metric)] = max(widest[(bench, metric)], own_tol)

    calibrate, rebaseline = {}, {}
    for key, benches in sorted(runs.items()):
        for (bench, cores), metrics in sorted(benches.items()):
            current = judged.get(key, {}).get(bench, {}).get(cores)
            before = others.get(key, {}).get(bench, {}).get(cores)
            # This pull request's own calibration counts only while it is the row: once
            # another pull request's calibration of the same CPU has been folded into base/,
            # resolve() supersedes ours, and writing a calibration again would be ignored
            # just the same -- the gate would fail forever. Then the row is base/'s, and a
            # move past it is a rebaseline FROM it (write_overlay drops the stale entry).
            new_row = current is None or (
                before is None and cores in own.get('calibrate', {}).get(key, {}).get(bench, {}))
            row = {} if current is None else copy.deepcopy(current)
            moved = []
            for metric in METRICS:
                values = metrics[metric]
                if args.metric and metric != args.metric and not new_row:
                    continue
                if not (args.all or new_row or (key, bench, cores) in stale or any(
                        perf_gate.verdict(v, current[metric],
                                          current.get('tolerance', {}).get(metric, tolerance[metric]),
                                          floor[metric]) != 'ok' for v in values)):
                    continue
                moved.append(metric)
                median = statistics.median(values)
                row[metric] = round(median, 3)
                tol = spread_tolerance(values, median, tolerance[metric])
                if len(values) < MIN_RUNS_FOR_OWN_SPREAD:
                    kept = (current or {}).get('tolerance', {}).get(metric, 0.0)
                    tol = max(tol, kept, widest.get((bench, metric), 0.0) if new_row else 0.0)
                row.setdefault('tolerance', {})[metric] = tol
            if not moved:
                continue
            # A row whose tolerance is just the global one does not need to repeat it.
            tol = {m: t for m, t in row.pop('tolerance', {}).items() if t > tolerance[m]}
            if tol:
                row['tolerance'] = tol
            row['runs'] = min(len(metrics[m]) for m in moved)
            if new_row or before is None:
                calibrate.setdefault(key, {}).setdefault(bench, {})[cores] = row
            else:
                row['from'] = perf_baseline.from_row(before)
                rebaseline.setdefault(key, {}).setdefault(bench, {})[cores] = row

    left = sorted(stale - {(k, b, c) for k, benches in rebaseline.items()
                           for b, per in benches.items() for c in per})
    if left:
        raise SystemExit('These rows of pr/%d.json are stale and these runs did not measure '
                         'them; pass runs that do: %s' % (number, ', '.join(
                             '%s %s/%s' % row for row in left)))
    if not calibrate and not rebaseline:
        print('Every measured row is inside its tolerance; nothing to write.')
        return 0
    if rebaseline and not (args.reason or own.get('reason')):
        raise SystemExit('These rows moved past their tolerance and would be rebaselined; say '
                         'why with --reason:\n' + json.dumps(rebaseline, indent=1))
    try:
        path = perf_baseline.write_overlay(root, number, calibrate, rebaseline, args.reason)
        perf_baseline.load(root)   # the overlay must resolve against everything else
    except perf_baseline.BaselineError as error:
        raise SystemExit(str(error))
    for kind, tree in (('calibrated', calibrate), ('rebaselined', rebaseline)):
        for key, benches in sorted(tree.items()):
            print('%s %s: %s' % (kind, key, ', '.join(sorted(benches))))
    print('Wrote %s -- commit it with this pull request.' % path)
    return 0


if __name__ == '__main__':
    sys.exit(main())
