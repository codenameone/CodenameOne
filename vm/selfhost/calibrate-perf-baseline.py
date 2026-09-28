#!/usr/bin/env python3
"""Writes perf-baseline.json from the perf-results.json of several CI runs of UNCHANGED code.

    python3 vm/selfhost/calibrate-perf-baseline.py [--out perf-baseline.json] RESULTS.json ...

Pass every perf-results.json you have -- several runs per platform, from the artifacts
ci-perf-gate.sh leaves (linux-screenshot-raw-*/perf, windows-port-screenshot-raw-*/perf,
macos-ui-tests/perf). The runs must all measure the same code: the point is to learn how
far each ratio moves when nothing changed.

For every platform, benchmark and core setting:
  baseline   the median of the runs' median ratios.
  tolerance  max(the global tolerance, SPREAD_MARGIN x the observed spread), where the spread
             is how far the runs' ratios reach above that median. A handful of runs
             under-samples the tail, hence the margin. Measured on this branch: across five
             runs most rows moved under 5%, while objectAllocation moved 20-37% on every
             platform and Windows x64's translation rows up to 48% -- one global tolerance
             either fails those on noise or waves real regressions through the rest.
A row measured fewer than MIN_RUNS_FOR_OWN_SPREAD times cannot estimate its own spread;
it takes the larger of what it measured and the widest tolerance any other row needed
for the same benchmark and metric.

Runs record the runner's CPU, and rows are written per CPU MODEL (`platform@model`, the
model as perf-gate.cpu_class names it): hosted pools mix microarchitectures whose ratios
differ by more than any tolerance absorbs. A runner on a model with no rows is reported
and FAILS the gate until a run of it is calibrated in -- which is what this script is
for: pass that run's perf-results.json and commit the result. A run whose CPU is unknown
feeds the plain `platform` rows instead.

Only the rows these results measure are replaced; every other row in the file is kept,
so adding one new CPU model from one run leaves the rest alone. A single-run row borrows
the widest tolerance seen for its benchmark across these runs AND the existing file.
--fresh starts from an empty set of rows instead: a full recalibration after the code
changed, where a kept row would be a baseline for code that no longer exists.

The global tolerance and the absolute floor (which keeps the tiny memory ratios, 0.03x of
the JDK, from failing on a few kilobytes) are kept from the existing file.
"""
import argparse
import importlib.util
import json
import math
import statistics
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location('perf_gate', HERE / 'perf-gate.py')
perf_gate = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(perf_gate)
SPREAD_MARGIN = 1.5
# Fewer runs than this cannot estimate a row's spread: EPYC 7763's hello row, from two
# runs, failed unchanged code at +15.7% against a 15% tolerance. Such a row takes the
# widest tolerance its benchmark needed anywhere, as a single-run row always did.
MIN_RUNS_FOR_OWN_SPREAD = 5
METRICS = ('time', 'memory')


def round_up(value, step=0.05):
    return round(math.ceil(value / step - 1e-9) * step, 2)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--out', default=str(HERE / 'perf-baseline.json'))
    parser.add_argument('--fresh', action='store_true',
                        help='drop every row these results do not re-measure')
    parser.add_argument('results', nargs='+')
    args = parser.parse_args(argv)

    existing = json.loads(Path(args.out).read_text())
    tolerance = existing['tolerance']
    # platform -> (benchmark, cores) -> metric -> [median ratio per run]
    runs = defaultdict(lambda: defaultdict(lambda: defaultdict(list)))
    for path in args.results:
        report = json.loads(Path(path).read_text())
        if report.get('error') or report.get('failures'):
            raise SystemExit('%s did not complete; calibrate only from complete runs' % path)
        cls = perf_gate.cpu_class(report.get('cpu'))
        key = '%s@%s' % (report['platform'], cls) if cls else report['platform']
        for bench, by_cores in report['results'].items():
            for cores, entry in by_cores.items():
                for metric in METRICS:
                    runs[key][(bench, cores)][metric].append(entry[metric]['median'])

    rows = {}
    widest = defaultdict(float)  # (benchmark, metric) -> widest tolerance any platform needed
    kept = {} if args.fresh else {k: v for k, v in existing.get('platforms', {}).items()
                                  if k not in runs}
    for benches in kept.values():
        for bench, by_cores in benches.items():
            for row in by_cores.values():
                for metric, tol in row.get('tolerance', {}).items():
                    widest[(bench, metric)] = max(widest[(bench, metric)], tol)
    for platform, benches in runs.items():
        for key, metrics in benches.items():
            row = {}
            for metric, values in metrics.items():
                base = statistics.median(values)
                row[metric] = round(base, 3)
                if len(values) > 1:
                    spread = max(values) / base - 1
                    tol = max(tolerance[metric], round_up(spread * SPREAD_MARGIN))
                    row.setdefault('tolerance', {})[metric] = tol
                    widest[(key[0], metric)] = max(widest[(key[0], metric)], tol)
            rows[(platform, key)] = (row, min(len(v) for v in metrics.values()))

    platforms = {}
    for (platform, (bench, cores)), (row, count) in sorted(rows.items()):
        if count < MIN_RUNS_FOR_OWN_SPREAD:
            own = row.get('tolerance', {})
            row['tolerance'] = {m: max(tolerance[m], own.get(m, 0.0),
                                       widest.get((bench, m), tolerance[m]))
                                for m in METRICS}
        # A row whose tolerance is just the global one does not need to repeat it.
        tol = {m: t for m, t in row.pop('tolerance', {}).items() if t > tolerance[m]}
        if tol:
            row['tolerance'] = tol
        row['runs'] = count
        platforms.setdefault(platform, {}).setdefault(bench, {})[cores] = row

    # Rows these results did not measure are kept (unless --fresh): a calibration that got
    # no macOS runner must not leave macOS without a baseline, and adding one CPU model
    # must not drop the platform's others.
    platforms.update(kept)
    existing['platforms'] = platforms
    Path(args.out).write_text(json.dumps(existing, indent=1, sort_keys=True) + '\n')
    for platform in sorted(platforms):
        print('%-14s %d benchmarks from %d run(s)' % (
            platform, len(platforms[platform]),
            max(r['runs'] for b in platforms[platform].values() for r in b.values())))


if __name__ == '__main__':
    main()
