#!/usr/bin/env python3
"""The Flutter benchmark's regression baselines, on the ParparVM performance gate's model.

    python3 scripts/flutter-bench/flutter_baseline.py check [--base REF] [--pr N]
    python3 scripts/flutter-bench/flutter_baseline.py fold
    python3 scripts/flutter-bench/flutter_baseline.py calibrate [--pr N] [--reason TEXT]
            [--all] [--metric M,...] [--per-cpu] RESULT-OR-CANDIDATE.json ...
    python3 scripts/flutter-bench/flutter_baseline.py import-legacy --pr N --legacy DIR
            [--original DIR] [--reason TEXT]

Two different questions are asked of every run, and this file is only about the second:

  1. Is Codename One at least level with Flutter on every metric? benchlib.check_behind,
     head to head, no baseline. That is the benchmark's purpose and is untouched here.
  2. Did Codename One move against ITSELF? A regression gate against checked-in rows.

THE LAYOUT (scripts/flutter-bench/baseline/), exactly vm/selfhost/perf-baseline's:
  policy.json        the global tolerance and floor per metric, and the metrics a
                     platform records without gating ("ungated", each with its reason)
  base/<key>.json    the consolidated rows of one platform, <key> = platform, or
                     platform@cpu-model for a ratio calibrated per runner CPU:
                     {"gallery": {metric: {"value": v, "runs": n, "tolerance"?: {"value": t}}}}
  pr/<number>.json   ONE pull request's calibrations and rebaselines; folded nightly

The overlay rules (calibrate / rebaseline with "from" and a per-row "reason", chaining,
combining, the fold that provably changes no verdict, `check --base` refusing base/ and
other pull requests' overlays) are vm/selfhost/perf_baseline.py's, run with the FLUTTER
layout below rather than copied.

THE METRICS
  code_bytes, install_bytes, wire_bytes
      Codename One's own sizes, ABSOLUTE. They are deterministic for a source tree and
      toolchain, so the global tolerance is 0 and any move -- either way -- fails until the
      pull request that caused it rebaselines the row in its own overlay, saying why.
      Keyed by platform alone: a size does not depend on the runner's CPU.
  cold_start_ratio, idle_memory_ratio
      Codename One OVER Flutter, from the same run: the median of the per-round ratios of
      the interleaved launches (benchlib.paired_rounds), lower is better. The pinned
      Flutter build measured beside it on the same runner is the unit of measure, as JDK 25
      is for the ParparVM gate, so a slow runner slows both halves of a round and cancels.
      That is what runner_slowdown_discount and flutter_reference approximated one-sidedly
      for start-up alone; a ratio does it for both directions and both metrics, so both are
      gone. Keyed per runner CPU model once a platform has such rows (--per-cpu); until
      then one plain-platform row judges every CPU, as macos-arm64 does in the ParparVM
      gate.

A run is judged per metric. A RATIO more than its tolerance away from its row in EITHER
direction fails (an improvement nobody wrote down can be given back unseen). A SIZE fails
only by growing past its tolerance; a shrink past it is reported as "improved --
rebaseline to tighten" and does not fail (see judge() for why). A measured metric with no
row (uncalibrated) fails, and so does a row whose metric the run did not produce.
"""
import argparse
import importlib.util
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import benchlib  # noqa: E402

REPO = HERE.parents[1]
ROOT = HERE / 'baseline'
LEGACY_DIR = 'scripts/flutter-bench/baselines'   # the retired one-file-per-platform layout
APP = 'gallery'


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


perf_baseline = _load('perf_baseline', REPO / 'vm' / 'selfhost' / 'perf_baseline.py')
perf_gate = _load('perf_gate', REPO / 'vm' / 'selfhost' / 'perf-gate.py')
BaselineError = perf_baseline.BaselineError

SIZE_METRICS = ('code_bytes', 'install_bytes', 'wire_bytes')
# The report's sample list each ratio is paired from (benchlib.PAIRED_METRICS key).
RATIO_METRICS = {'cold_start_ratio': 'cold_start_ms', 'idle_memory_ratio': 'idle_memory_bytes'}
METRIC_IDS = SIZE_METRICS + tuple(RATIO_METRICS)
LABELS = {'code_bytes': 'Executable code', 'install_bytes': 'Installed size',
          'wire_bytes': 'Download size (zipped)',
          'cold_start_ratio': 'Cold start (Codename One / Flutter)',
          'idle_memory_ratio': 'Memory at rest (Codename One / Flutter)'}


def _row_default(policy_map, key, bench, cores):
    if policy_map is None:
        return None
    return {'value': policy_map.get(cores, 0.0)}


def _round_value(value):
    return int(value) if float(value).is_integer() else round(value, 3)


FLUTTER = perf_baseline.Layout(
    ROOT, ('value',), re.compile(r'^[a-z]+(@[a-z0-9-]+)?$'), LEGACY_DIR,
    # policy.json, not base/: base/ may legitimately be empty between folds, and its
    # absence on master must not let a later pull request pass as "the migration".
    migration_marker='policy.json',
    policy_metrics=METRIC_IDS, zero_tolerance=True, value_word='number',
    row_default=_row_default,
    widest_key=lambda bench, cores, metric: (bench, cores, metric),
    round_value=_round_value,
    # A size is deterministic up to a few bytes of packaging jitter; 5% steps would turn
    # that into megabytes of slack.
    tolerance_step=lambda key, bench, cores: 0.001 if cores in SIZE_METRICS else 0.05,
    calibrate_command='flutter_baseline.py calibrate --pr N',
    legacy_message='%s/ changed: those files are retired and nothing reads them. Express '
                   'the change as rows in scripts/flutter-bench/baseline/pr/<N>.json '
                   '(flutter_baseline.py calibrate --pr N, or import-legacy for a branch '
                   'that edited the old files) and leave the old files deleted.' % LEGACY_DIR)


# ----------------------------------------------------------------------
# Loading, with the checks the shared engine cannot know about
# ----------------------------------------------------------------------

def _check_names(where, tree):
    for key, bench, cores, _row in perf_baseline._rows(where, tree, FLUTTER):
        if bench != APP:
            raise BaselineError('%s: %s/%s: the only application is %r' % (where, key, bench, APP))
        if cores not in METRIC_IDS:
            raise BaselineError('%s: %s: unknown metric %r (one of %s)'
                                % (where, key, cores, ', '.join(METRIC_IDS)))
        if cores in SIZE_METRICS and '@' in key:
            raise BaselineError('%s: %s %s: a size does not depend on the runner CPU; it '
                                'belongs under the plain platform key' % (where, key, cores))


def _check_policy(policy):
    ungated = policy.get('ungated', {})
    if not isinstance(ungated, dict):
        raise BaselineError('policy.json: ungated must map platform to {metric: reason}')
    for platform, metrics in ungated.items():
        if not isinstance(metrics, dict):
            raise BaselineError('policy.json: ungated %s must map metric to its reason' % platform)
        for metric, reason in metrics.items():
            if metric not in METRIC_IDS:
                raise BaselineError('policy.json: ungated %s: unknown metric %r' % (platform, metric))
            if not isinstance(reason, str) or not reason.strip():
                raise BaselineError('policy.json: ungated %s %s needs the reason it is not '
                                    'gated' % (platform, metric))
    unknown = set(policy) - {'tolerance', 'floor', 'ungated', 'notes'}
    if unknown:
        raise BaselineError('policy.json: unknown field(s) %s' % ', '.join(sorted(unknown)))


def _check_tree(root):
    root = Path(root)
    for path in sorted((root / 'base').glob('*.json')):
        _check_names(str(path), {path.stem: perf_baseline._read_json(path)})
    for number, overlay in perf_baseline.load_overlays(root, FLUTTER):
        for kind in ('calibrate', 'rebaseline'):
            _check_names('pr/%d.json %s' % (number, kind), overlay.get(kind, {}))


def load(root=ROOT, exclude=None):
    """The policy and the resolved rows, as perf_baseline.load gives them."""
    _check_tree(root)
    data = perf_baseline.load(root, exclude, FLUTTER)
    _check_policy(data['policy'])
    data['ungated'] = data['policy'].get('ungated', {})
    return data


def check(root=ROOT, base_ref=None, number=None):
    problems = perf_baseline.check(root, base_ref, number, FLUTTER)
    try:
        _check_tree(root)
        _check_policy(perf_baseline.load_policy(root, FLUTTER))
    except BaselineError as error:
        problems.append(str(error))
    return problems


# ----------------------------------------------------------------------
# What a run measured, and which row judges it
# ----------------------------------------------------------------------

def ratio(report, metric):
    """Codename One over Flutter for a ratio metric: the median of the per-round ratios,
    or None when no round was measured by both sides. Flutter's figure in a round is the
    end of its bracket least favourable to Codename One, as in the head-to-head verdict."""
    pairs, _reason = benchlib.paired_rounds(report.get('codenameone') or {},
                                            report.get('flutter') or {}, RATIO_METRICS[metric])
    if not pairs:
        return None
    return benchlib.median([ours / float(min(upper, lower) if lower is not None else upper)
                            for ours, upper, lower in pairs])


def measurements(report):
    """{metric: value} for every metric this run produced. Accepts a run's result JSON or
    the candidate run_bench.py writes beside it (which carries them precomputed)."""
    if 'values' in report:
        return {k: v for k, v in report['values'].items() if v is not None}
    out = {}
    for metric in SIZE_METRICS:
        value = (report.get('codenameone') or {}).get(metric)
        if value:
            out[metric] = value
    for metric in RATIO_METRICS:
        value = ratio(report, metric)
        if value:
            out[metric] = value
    return out


def candidate(report):
    """What run_bench.py --baseline-out writes: this run's measurements in the form the
    calibrator reads, so re-baselining is `calibrate --pr N <this file>`."""
    return {'schema_version': 2, 'platform': report.get('platform'),
            'cpu': report.get('cpu'), 'generated_at': report.get('generated_at'),
            'values': {m: (round(v, 4) if m in RATIO_METRICS else v)
                       for m, v in measurements(report).items()}}


def row_key(rows, platform, cpu, metric):
    """The key whose row judges `metric` for this runner, or None.

    A size: the plain platform. A ratio: platform@model when that model has a row; None
    when the platform has per-model rows for the metric but not this model (another
    microarchitecture's ratio must not judge it -- it fails as uncalibrated); otherwise
    the plain platform."""
    has = lambda key: metric in rows.get(key, {}).get(APP, {})
    if metric in SIZE_METRICS:
        return platform if has(platform) else None
    cls = perf_gate.cpu_class(cpu)
    if cls and has('%s@%s' % (platform, cls)):
        return '%s@%s' % (platform, cls)
    if any(k.startswith(platform + '@') and has(k) for k in rows):
        return None
    return platform if has(platform) else None


def judge(report, data):
    """Every finding of the regression gate for one platform's run, as a list of dicts:
    verdict 'regression', 'improved', 'uncalibrated' or 'missing'. Empty means within
    tolerance. Ungated metrics are never findings."""
    platform = report.get('platform')
    rows = data['platforms']
    ungated = data['ungated'].get(platform, {})
    values = measurements(report)
    findings = []
    for metric in METRIC_IDS:
        if metric in ungated:
            continue
        key = row_key(rows, platform, report.get('cpu'), metric)
        row = rows.get(key, {}).get(APP, {}).get(metric) if key else None
        value = values.get(metric)
        if row is None:
            if value is not None:
                try:
                    key = key or _calibration_key(rows, platform, report.get('cpu'), metric)
                except SystemExit:
                    key = '%s@<undecodable CPU>' % platform
                findings.append({'metric': metric, 'label': LABELS[metric], 'actual': value,
                                 'verdict': 'uncalibrated', 'key': key})
            continue
        tolerance = row.get('tolerance', {}).get('value', data['tolerance'][metric])
        if value is None:
            findings.append({'metric': metric, 'label': LABELS[metric], 'baseline': row['value'],
                             'actual': None, 'tolerance': tolerance, 'verdict': 'missing',
                             'key': key})
            continue
        outcome = perf_gate.verdict(value, row['value'], tolerance, data['floor'][metric])
        if outcome != 'ok':
            finding = {'metric': metric, 'label': LABELS[metric], 'baseline': row['value'],
                       'actual': value, 'tolerance': tolerance, 'verdict': outcome,
                       'key': key,
                       'moved_by': round((value / float(row['value']) - 1.0) * 100.0, 2)}
            if outcome == 'improved' and metric in SIZE_METRICS:
                # A SIZE judged one-sided, unlike the ParparVM gate and unlike the ratio
                # rows here: a shrink is reported ("rebaseline to tighten") but does not
                # fail. flutter-bench.yml's paths filter means the pull requests that
                # shrink the app -- core changes -- mostly never run this benchmark, so a
                # two-sided size row would turn the NIGHTLY run on master red for a good
                # change nobody can rebaseline in their own pull request. Growth still
                # fails. Ratios stay two-sided: they move with the runtimes being
                # compared, not with every core edit, and an unrecorded speed-up there is
                # what a later change could silently give back.
                finding['advisory'] = True
            findings.append(finding)
    return findings


def failing(findings):
    """The findings that fail the leg: all but an advisory (a shrunk size)."""
    return [f for f in findings if not f.get('advisory')]


def _calibration_key(rows, platform, cpu, metric, per_cpu=False):
    if metric in SIZE_METRICS:
        return platform
    cls = perf_gate.cpu_class(cpu)
    per_model = any(k.startswith(platform + '@') and metric in rows.get(k, {}).get(APP, {})
                    for k in rows)
    if cls and (per_cpu or per_model):
        return '%s@%s' % (platform, cls)
    if per_model or per_cpu:
        raise SystemExit('%s: the runner reported no decodable CPU model (%r), and %s %s is '
                         'calibrated per CPU model; fix perf-gate.cpu_model() for this runner'
                         % (platform, cpu, platform, metric))
    return platform


def describe(item):
    """One line for a gate finding."""
    metric = item['metric']

    def fmt(value):
        if value is None:
            return '--'
        if metric in SIZE_METRICS:
            return '{:,} bytes'.format(int(value))
        return '%.3fx' % value

    if item['verdict'] == 'uncalibrated':
        return ('%s has no baseline row (%s); measured %s'
                % (item['label'], item['key'], fmt(item['actual'])))
    if item['verdict'] == 'missing':
        return ('%s was not measured, but the baseline gates it at %s'
                % (item['label'], fmt(item['baseline'])))
    return ('%s %s: %s against a baseline of %s (%+.2f%%, tolerance %s%%)'
            % (item['label'], 'REGRESSED' if item['verdict'] == 'regression'
               else 'improved -- rebaseline to tighten (not a failure)' if item.get('advisory')
               else 'IMPROVED (rebaseline it)', fmt(item['actual']), fmt(item['baseline']),
               item['moved_by'], ('%g' % (item['tolerance'] * 100))))


def fix_command(number, findings, artifact):
    moved = any(f['verdict'] in ('regression', 'improved') for f in findings)
    # (an advisory shrink counts: tightening the row rebaselines it, which needs a reason)
    return ('python3 scripts/flutter-bench/flutter_baseline.py calibrate --pr %s%s %s'
            % (number or '<PR number>', ' --reason "<why these moved>"' if moved else '',
               artifact))


# ----------------------------------------------------------------------
# Calibration: the shared arithmetic, fed this benchmark's runs
# ----------------------------------------------------------------------

def collect(paths, rows, ungated, only=None, per_cpu=False):
    """{key: {(app, metric): {'value': [one value per run]}}}, each run feeding the row
    that judged it (or the row it would calibrate)."""
    runs = {}
    for path in paths:
        report = json.loads(Path(path).read_text())
        if report.get('status') not in (None, 'measured'):
            raise SystemExit('%s was not measured (%s); calibrate only from complete runs'
                             % (path, report.get('reason')))
        platform = report.get('platform')
        if not platform:
            raise SystemExit('%s names no platform' % path)
        for metric, value in measurements(report).items():
            if metric not in METRIC_IDS or (only and metric not in only) \
                    or metric in ungated.get(platform, {}):
                continue
            key = None if per_cpu and metric in RATIO_METRICS else \
                row_key(rows, platform, report.get('cpu'), metric)
            if key is None:
                key = _calibration_key(rows, platform, report.get('cpu'), metric, per_cpu)
            runs.setdefault(key, {}).setdefault((APP, metric), {'value': []})['value'] \
                .append(value)
    return runs


def calibrate(root, number, paths, reason=None, everything=False, only=None, per_cpu=False):
    context = perf_baseline.calibration_context(root, number, FLUTTER)
    policy = context['policy']
    _check_policy(policy)
    runs = collect(paths, context['judged'], policy.get('ungated', {}), only, per_cpu)
    plan_calibrate, plan_rebaseline = perf_baseline.plan_calibration(
        context, runs, perf_gate.verdict, everything=everything)
    return perf_baseline.finish_calibration(root, context, plan_calibrate, plan_rebaseline,
                                            reason)


# ----------------------------------------------------------------------
# The retired layout: baselines/<platform>.json
# ----------------------------------------------------------------------

def legacy_rows(files, policy):
    """The rows a set of old baselines/<platform>.json files expresses, and what it cannot.

    Sizes carry over exactly: value, and a tolerance where it differs from the policy's.
    Absolute start-up and memory figures have no equivalent -- the gate now judges a
    ratio against Flutter -- so they are returned separately, as the metrics the old file
    GATED (a tolerance present) and the ones it recorded without gating."""
    rows, gated, recorded = {}, {}, {}
    for platform, legacy in sorted(files.items()):
        values = legacy.get('codenameone', {})
        tolerances = legacy.get('tolerances', {})
        for metric in SIZE_METRICS:
            if values.get(metric) is None:
                continue
            if tolerances.get(metric) is None:
                raise BaselineError('%s %s is recorded but not gated in the old file; the '
                                    'new layout gates every size' % (platform, metric))
            row = {'value': values[metric], 'runs': 1}
            if tolerances[metric] > policy['tolerance'][metric]:
                row['tolerance'] = {'value': tolerances[metric]}
            rows.setdefault(platform, {}).setdefault(APP, {})[metric] = row
        for old, new in (('cold_start_ms', 'cold_start_ratio'),
                         ('idle_memory_bytes', 'idle_memory_ratio')):
            if values.get(old) is not None:
                (gated if tolerances.get(old) is not None else recorded) \
                    .setdefault(platform, []).append(new)
    return rows, gated, recorded


def read_legacy_dir(directory):
    return {p.stem: json.loads(p.read_text()) for p in sorted(Path(directory).glob('*.json'))}


def import_legacy(root, number, legacy, original=None, reason=None):
    """A branch's edits to the old files, as its overlay. Only rows that differ between the
    branch's copy (`legacy`) and the copy it started from (`original`) are its own; a size
    the baseline lacks becomes a calibration, one it has a rebaseline from its current
    value. Returns the overlay's path, or None if the branch changed no size."""
    current = load(root)['platforms']
    policy = perf_baseline.load_policy(root, FLUTTER)
    new, _, _ = legacy_rows(legacy, policy)
    old, _, _ = legacy_rows(original or {}, policy)
    calibrate_, rebaseline = {}, {}
    for key, bench, metric, row in perf_baseline._rows('legacy', new, FLUTTER):
        if old.get(key, {}).get(bench, {}).get(metric) == row:
            continue
        existing = current.get(key, {}).get(bench, {}).get(metric)
        if existing is None:
            calibrate_.setdefault(key, {}).setdefault(bench, {})[metric] = row
        elif existing != row:
            moved = dict(row, **{'from': perf_baseline.from_row(existing, FLUTTER)})
            rebaseline.setdefault(key, {}).setdefault(bench, {})[metric] = moved
    if not calibrate_ and not rebaseline:
        return None
    return perf_baseline.write_overlay(root, number, calibrate_, rebaseline, reason, FLUTTER)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--root', default=str(ROOT))
    sub = parser.add_subparsers(dest='command', required=True)
    p = sub.add_parser('check', help='validate the baselines (and, with --base, a PR diff)')
    p.add_argument('--base')
    p.add_argument('--pr', type=int)
    sub.add_parser('fold', help='fold every overlay into base/ (nightly, on master)')
    p = sub.add_parser('calibrate', help="write a pull request's overlay from runs")
    p.add_argument('--pr', type=int)
    p.add_argument('--reason')
    p.add_argument('--all', action='store_true',
                   help='rebaseline every measured row, not only those out of tolerance')
    p.add_argument('--metric', help='comma-separated metric ids to take from the runs')
    p.add_argument('--per-cpu', action='store_true',
                   help='calibrate ratio rows per runner CPU model (platform@model)')
    p.add_argument('results', nargs='+')
    p = sub.add_parser('import-legacy', help="convert a branch's baselines/*.json edits")
    p.add_argument('--pr', type=int)
    p.add_argument('--reason')
    p.add_argument('--legacy', required=True, help="the branch's baselines/ directory")
    p.add_argument('--original', help='the baselines/ directory the branch started from')
    args = parser.parse_args(argv)
    root = Path(args.root)
    try:
        if args.command == 'check':
            problems = check(root, args.base, args.pr)
            for problem in problems:
                print('flutter-baseline: ' + problem)
            if problems:
                return 1
            print('flutter-baseline: OK')
        elif args.command == 'fold':
            _check_tree(root)
            folded = perf_baseline.fold(root, FLUTTER)
            print('flutter-baseline: folded %s' % (', '.join('pr/%d.json' % n for n in folded)
                                                    if folded else 'nothing'))
        elif args.command == 'calibrate':
            number = args.pr or perf_baseline.pr_number()
            if number is None:
                raise SystemExit('No pull request number: pass --pr N')
            only = set(args.metric.split(',')) if args.metric else None
            if only and only - set(METRIC_IDS):
                raise SystemExit('unknown metric(s): %s' % ', '.join(sorted(only - set(METRIC_IDS))))
            calibrate(root, number, args.results, args.reason, args.all, only, args.per_cpu)
        elif args.command == 'import-legacy':
            number = args.pr or perf_baseline.pr_number()
            if number is None:
                raise SystemExit('No pull request number: pass --pr N')
            path = import_legacy(root, number, read_legacy_dir(args.legacy),
                                 read_legacy_dir(args.original) if args.original else None,
                                 args.reason)
            print('flutter-baseline: %s' % (path or 'the branch changed no size row'))
    except BaselineError as error:
        print('flutter-baseline: ' + str(error))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
