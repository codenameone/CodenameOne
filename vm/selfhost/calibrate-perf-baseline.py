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
A platform measured only ONCE has no spread of its own; each of its rows takes the widest
tolerance any other platform needed for the same benchmark and metric.

The global tolerance and the absolute floor (which keeps the tiny memory ratios, 0.03x of
the JDK, from failing on a few kilobytes) are kept from the existing file.
"""
import argparse
import json
import math
import statistics
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPREAD_MARGIN = 1.5
METRICS = ('time', 'memory')


def round_up(value, step=0.05):
    return round(math.ceil(value / step - 1e-9) * step, 2)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--out', default=str(HERE / 'perf-baseline.json'))
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
        for bench, by_cores in report['results'].items():
            for cores, entry in by_cores.items():
                for metric in METRICS:
                    runs[report['platform']][(bench, cores)][metric].append(entry[metric]['median'])

    rows = {}
    widest = defaultdict(float)  # (benchmark, metric) -> widest tolerance any platform needed
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
        if count == 1:
            row['tolerance'] = {m: max(tolerance[m], widest.get((bench, m), tolerance[m]))
                                for m in METRICS}
        # A row whose tolerance is just the global one does not need to repeat it.
        tol = {m: t for m, t in row.pop('tolerance', {}).items() if t > tolerance[m]}
        if tol:
            row['tolerance'] = tol
        row['runs'] = count
        platforms.setdefault(platform, {}).setdefault(bench, {})[cores] = row

    existing['platforms'] = platforms
    Path(args.out).write_text(json.dumps(existing, indent=1, sort_keys=True) + '\n')
    for platform in sorted(platforms):
        print('%-14s %d benchmarks from %d run(s)' % (
            platform, len(platforms[platform]),
            max(r['runs'] for b in platforms[platform].values() for r in b.values())))


if __name__ == '__main__':
    main()
