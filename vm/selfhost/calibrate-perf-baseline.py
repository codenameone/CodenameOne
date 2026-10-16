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
              number; a row this pull request already rebaselined keeps its reason when
              re-measured. --all rebaselines every measured row instead, for recalibrating a
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
import importlib.util
import json
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
# The arithmetic lives in perf_baseline, shared with the Flutter benchmark's calibrator.
SPREAD_MARGIN = perf_baseline.SPREAD_MARGIN
MIN_RUNS_FOR_OWN_SPREAD = perf_baseline.MIN_RUNS_FOR_OWN_SPREAD
METRICS = perf_baseline.METRICS
round_up = perf_baseline.round_up
spread_tolerance = perf_baseline.spread_tolerance


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
            if cls is None and any(k.startswith(report['platform'] + '@') for k in rows or {}):
                # An undecodable CPU on a platform with per-model rows: baseline_key() selects
                # NO row for it (judging an unknown model by another model's ratios is what
                # failed unchanged code), so a plain-platform row written here would never
                # be read and the gate would stay uncalibrated forever. The fix is to teach
                # perf-gate.cpu_model() to read this runner, not to calibrate.
                raise SystemExit('%s: the runner reported no decodable CPU model (%r) on %s, '
                                 'which has per-model rows; no baseline row can be selected '
                                 'for it. Fix perf-gate.cpu_model() for this runner.'
                                 % (path, report.get('cpu'), report['platform']))
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
    context = perf_baseline.calibration_context(root, number)
    runs = collect(args.results, set(args.only.split(',')) if args.only else None,
                   context['judged'])
    calibrate, rebaseline = perf_baseline.plan_calibration(
        context, runs, perf_gate.verdict, everything=args.all, metric=args.metric)
    perf_baseline.finish_calibration(root, context, calibrate, rebaseline, args.reason)
    return 0


if __name__ == '__main__':
    sys.exit(main())
