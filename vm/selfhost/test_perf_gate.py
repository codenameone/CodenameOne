import importlib.util
from pathlib import Path
import re
import unittest


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(file))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


gate = load('perf_gate', 'perf-gate.py')


def metric(median, baseline, verdict):
    return {'median': median, 'min': median, 'max': median, 'rounds': [median],
            'baseline': baseline, 'verdict': verdict}


def report(results, labels=None, skipped=(), regression=False, error=None, enforced=True):
    r = {'platform': 'linux-x64', 'rounds': 5, 'tolerance': {'time': 0.1, 'memory': 0.1},
         'available_cores': 4, 'skipped_cores': list(skipped), 'regression': regression,
         'labels': labels or {b: b for b in results},
         'results': {bench: {cores: {'enforced': enforced, 'time': t, 'memory': m}
                             for cores, (t, m) in per.items()}
                     for bench, per in results.items()}}
    if error:
        r['error'] = error
    return r


class VerdictTests(unittest.TestCase):
    def test_tolerance_is_relative_to_the_baseline(self):
        self.assertEqual('ok', gate.verdict(1.09, 1.0, 0.1))
        self.assertEqual('regression', gate.verdict(1.11, 1.0, 0.1))
        self.assertEqual('improved', gate.verdict(0.89, 1.0, 0.1))
        self.assertEqual('uncalibrated', gate.verdict(5.0, None, 0.1))

    def test_an_improvement_inside_the_floor_is_not_one(self):
        self.assertEqual('ok', gate.verdict(0.04, 0.06, 0.15, 0.05))
        self.assertEqual('improved', gate.verdict(0.40, 0.60, 0.15, 0.05))

    def test_a_ram_change_below_the_floor_is_not_a_regression(self):
        # 0.06x -> 0.08x is +33%, and under a megabyte for a few-MB process.
        self.assertEqual('ok', gate.verdict(0.08, 0.06, 0.15, 0.05))
        self.assertEqual('regression', gate.verdict(0.20, 0.06, 0.15, 0.05))
        self.assertEqual('regression', gate.verdict(0.80, 0.60, 0.15, 0.05))

    def test_platform_keys(self):
        self.assertRegex(gate.platform_key(), r'^(linux|macos|windows)-(x64|arm64)$')

    def test_every_bench_workload_is_gated(self):
        # Bench.main's workloads, read from the source, must all be in the gate's list:
        # one added there and not here would never be measured.
        source = (Path(__file__).resolve().parents[1] /
                  'benchmarks/src/com/bench/Bench.java').read_text()
        in_bench = re.findall(r'runBench\("(\w+)"', source)
        self.assertEqual(in_bench, gate.WORKLOADS)


class MarkdownTests(unittest.TestCase):
    def test_a_regression_names_the_benchmark_and_the_size(self):
        text = gate.render_markdown(report({
            'quicksort': {'1': (metric(1.3, 1.0, 'regression'), metric(0.9, 0.9, 'ok'))},
            'recursion': {'1': (metric(0.8, 0.8, 'ok'), metric(0.5, 0.5, 'ok'))}},
            regression=True))
        self.assertIn('quicksort at 1 core: time 1.30x against a 1.00x baseline (+30.0%, tolerance 10%)', text)
        self.assertIn('| quicksort | 1 | 1.30x (base 1.00x, +30.0%)', text)
        self.assertIn('**REGRESSION** (time +30.0%)', text)
        self.assertIn('**Result: performance regression**', text)

    def test_every_benchmark_gets_a_row(self):
        text = gate.render_markdown(report({
            b: {'1': (metric(0.9, None, 'uncalibrated'), metric(0.5, None, 'uncalibrated')),
                '4': (metric(0.8, None, 'uncalibrated'), metric(0.6, None, 'uncalibrated'))}
            for b in ['hello', 'translator'] + gate.WORKLOADS}))
        rows = [l for l in text.splitlines() if l.startswith('| ') and not l.startswith('| Benchmark')]
        self.assertEqual(2 * (2 + len(gate.WORKLOADS)), len(rows))
        self.assertIn('**NO BASELINE**', text)
        self.assertIn('**Result: no baseline: calibration required**', text)

    def test_an_uncalibrated_cpu_fails_and_says_how_to_fix_it(self):
        r = report({'quicksort': {'all': (metric(1.3, None, 'uncalibrated'),
                                          metric(0.9, None, 'uncalibrated'))}})
        r['cpu'] = 'AMD64 Family 25 Model 17 Stepping 1, AuthenticAMD'
        r['baseline_key'] = None
        text = gate.render_markdown(r)
        self.assertIn('Model 17', text)
        self.assertIn('**no baseline for this CPU model** -- the gate fails', text)
        self.assertIn('**Result: no baseline: calibration required**', text)

    def test_an_improvement_fails_and_says_how_to_rebaseline(self):
        r = report({'quicksort': {'all': (metric(0.7, 1.0, 'improved'), metric(0.9, 0.9, 'ok'))}})
        r['pr'] = 5931
        text = gate.render_markdown(r)
        self.assertIn('**IMPROVED: rebaseline** (time -30.0%)', text)
        self.assertIn('vm/selfhost/perf-baseline/pr/5931.json', text)
        self.assertIn('--pr 5931 --reason', text)
        self.assertIn('**Result: improved past the baseline: rebaseline required**', text)

    def test_a_calibration_names_this_pull_requests_overlay(self):
        r = report({'quicksort': {'all': (metric(1.3, None, 'uncalibrated'),
                                          metric(0.9, None, 'uncalibrated'))}})
        r['calibration'] = {'quicksort': {'all': {'time': 1.3, 'memory': 0.9}}}
        r['calibration_key'] = 'linux-x64@new'
        text = gate.render_markdown(r)
        self.assertIn('pr/<PR number>.json', text)
        self.assertIn('calibrate-perf-baseline.py --pr <PR number> perf-results.json', text)

    def test_an_incomplete_gate_says_so(self):
        text = gate.render_markdown(report({}, error='stale native build'))
        self.assertIn('could not complete', text)
        self.assertIn('stale native build', text)

    def test_an_all_cores_row_shows_the_runner_cpu_count(self):
        text = gate.render_markdown(report({
            'quicksort': {'all': (metric(1.3, 1.0, 'regression'), metric(0.9, 0.9, 'ok'))}},
            regression=True))
        self.assertIn('| quicksort | 4 | 1.30x', text)
        self.assertIn('quicksort on all 4 CPUs: time 1.30x', text)
        self.assertIn('unpinned', text)

    def test_logical_cores_are_marked(self):
        text = gate.render_markdown(report({
            'hello': {'2': (metric(0.9, 0.9, 'ok'), metric(0.5, 0.5, 'ok'))}}, enforced=False))
        self.assertIn('| hello | 2* |', text)
        self.assertIn('No CPU affinity', text)

    def test_only_ratios_are_printed(self):
        text = gate.render_markdown(report({
            'hello': {'1': (metric(0.7234, 0.72, 'ok'), metric(0.88, 0.9, 'ok'))}}))
        # Every decimal in a row is a ratio ("0.72x") or a percentage; a bare decimal
        # would be an absolute measurement leaking in.
        for line in [l for l in text.splitlines() if l.startswith('| hello')]:
            for number in re.findall(r'[-+]?\d+\.\d+[x%]?', line):
                self.assertTrue(number.endswith(('x', '%')), line)



calibrate = load('calibrate_perf_baseline', 'calibrate-perf-baseline.py')
baselines = load('perf_baseline', 'perf_baseline.py')
POLICY = {'tolerance': {'time': 0.15, 'memory': 0.15}, 'floor': {'time': 0.0, 'memory': 0.05}}


class BaselineTree:
    """A throwaway perf-baseline directory: policy.json, base/, pr/."""

    def __init__(self, base=None, overlays=None):
        import json
        import tempfile
        self._tmp = tempfile.TemporaryDirectory()
        self.root = Path(self._tmp.name) / 'perf-baseline'
        (self.root / 'pr').mkdir(parents=True)
        (self.root / 'policy.json').write_text(json.dumps(POLICY))
        baselines.write_base(self.root, base or {})
        for number, overlay in (overlays or {}).items():
            (self.root / 'pr' / ('%d.json' % number)).write_text(json.dumps(overlay))

    def overlay(self, number):
        import json
        path = self.root / 'pr' / ('%d.json' % number)
        return json.loads(path.read_text()) if path.exists() else None

    def close(self):
        self._tmp.cleanup()


def row(t, m, runs=3, **tolerance):
    r = {'time': t, 'memory': m, 'runs': runs}
    if tolerance:
        r['tolerance'] = tolerance
    return r


class CalibrationTest(unittest.TestCase):
    """calibrate-perf-baseline.py: medians as baselines, spread-driven tolerances, written
    into the pull request's own overlay."""

    def run_calibration(self, runs, existing=None, overlays=None, pr=7, reason=None,
                        everything=False, metric=None):
        import json
        tree = BaselineTree(existing, overlays)
        try:
            files = []
            for i, run in enumerate(runs):
                platform, per_bench = run[0], run[1]
                results = {b: {'all': {'time': {'median': t}, 'memory': {'median': m}}}
                           for b, (t, m) in per_bench.items()}
                report = {'platform': platform, 'results': results}
                if len(run) > 2:
                    report['cpu'] = run[2]
                f = tree.root.parent / ('run%d.json' % i)
                f.write_text(json.dumps(report))
                files.append(str(f))
            calibrate.main(['--root', str(tree.root), '--pr', str(pr)] +
                           (['--reason', reason] if reason else []) +
                           (['--all'] if everything else []) +
                           (['--metric', metric] if metric else []) + files)
            return tree.overlay(pr), baselines.load(tree.root)['platforms']
        finally:
            tree.close()

    def test_median_baseline_and_global_tolerance_for_a_steady_row(self):
        overlay, rows = self.run_calibration([('linux-x64', {'quicksort': (1.00, 0.10)}),
                                              ('linux-x64', {'quicksort': (1.02, 0.10)}),
                                              ('linux-x64', {'quicksort': (1.01, 0.10)})])
        r = overlay['calibrate']['linux-x64']['quicksort']['all']
        self.assertEqual(r['time'], 1.01)
        self.assertNotIn('tolerance', r)  # 1% spread: the global 15% already covers it
        self.assertEqual(r['runs'], 3)
        self.assertEqual(rows['linux-x64']['quicksort']['all'], r)

    def test_noisy_row_gets_a_wider_tolerance_that_covers_every_run_both_ways(self):
        runs = [('linux-x64', {'objectAllocation': (v, 0.4)}) for v in (3.5, 5.5, 6.5)]
        overlay, _ = self.run_calibration(runs)
        r = overlay['calibrate']['linux-x64']['objectAllocation']['all']
        self.assertEqual(r['time'], 5.5)
        tol = r['tolerance']['time']
        self.assertGreater(tol, 0.15)
        # An improvement fails too, so the LOW run (3.5, -36%) must pass as well.
        for _, per in runs:
            self.assertEqual(gate.verdict(per['objectAllocation'][0], r['time'], tol), 'ok')
        # ...and it still bites: well past the observed spread is a regression.
        self.assertEqual(gate.verdict(r['time'] * (1 + tol) * 1.01, r['time'], tol), 'regression')

    def test_single_run_row_borrows_the_widest_tolerance_seen_elsewhere(self):
        runs = [('linux-x64', {'objectAllocation': (v, 0.4)}) for v in (5.0, 5.5, 6.5)]
        runs.append(('macos-arm64', {'objectAllocation': (3.0, 0.3)}))
        overlay, _ = self.run_calibration(runs)
        linux = overlay['calibrate']['linux-x64']['objectAllocation']['all']['tolerance']['time']
        mac = overlay['calibrate']['macos-arm64']['objectAllocation']['all']
        self.assertEqual(mac['runs'], 1)
        self.assertEqual(mac['tolerance']['time'], linux)

    def test_a_single_run_row_borrows_tolerance_from_the_baseline(self):
        old = {'windows-x64@a': {'arraySequential': {'all': row(1.35, 0.4, time=0.7)}}}
        overlay, _ = self.run_calibration(
            [('windows-x64', {'arraySequential': (1.67, 0.38)}, 'Model B')], existing=old)
        self.assertEqual(overlay['calibrate']['windows-x64@model-b']['arraySequential']['all']
                         ['tolerance']['time'], 0.7)

    def test_rows_are_per_cpu_model_and_an_unseen_model_is_not_gated(self):
        zen3 = 'AMD64 Family 25 Model 1 Stepping 1, AuthenticAMD'
        zen4 = 'AMD64 Family 25 Model 17 Stepping 1, AuthenticAMD'
        intel = 'Intel64 Family 6 Model 207 Stepping 2, GenuineIntel'
        runs = [('windows-x64', {'arraySequential': (v, 0.4)}, zen3) for v in (1.35, 1.36, 1.38)]
        runs += [('windows-x64', {'arraySequential': (1.98, 0.38)}, intel)]
        _, b = self.run_calibration(runs)
        self.assertEqual(sorted(b), ['windows-x64@amd64-family-25-model-1-authenticamd',
                                     'windows-x64@intel64-family-6-model-207-genuineintel'])
        self.assertEqual(gate.baseline_key(b, 'windows-x64', zen3.replace('Stepping 1', 'Stepping 2')),
                         'windows-x64@amd64-family-25-model-1-authenticamd')
        # A model no run has measured is not judged against another model's ratios.
        self.assertIsNone(gate.baseline_key(b, 'windows-x64', zen4))
        self.assertIsNone(gate.baseline_key(b, 'windows-x64', None))

    def test_a_run_with_no_known_cpu_feeds_the_plain_row(self):
        _, b = self.run_calibration([('macos-arm64', {'quicksort': (1.0, 0.1)})])
        self.assertEqual(sorted(b), ['macos-arm64'])
        self.assertEqual(gate.baseline_key(b, 'macos-arm64', 'Apple M1 (Virtual)'), 'macos-arm64')

    def test_a_run_feeds_the_row_that_judged_it(self):
        # macOS reports a CPU model but is judged by the plain platform row.
        old = {'macos-arm64': {'quicksort': {'all': row(1.0, 0.1)}}}
        overlay, rows = self.run_calibration(
            [('macos-arm64', {'quicksort': (0.5, 0.1)}, 'Apple M1 (Virtual)')],
            existing=old, reason='faster')
        self.assertEqual(sorted(overlay['rebaseline']), ['macos-arm64'])
        self.assertNotIn('calibrate', overlay)
        self.assertEqual(sorted(rows), ['macos-arm64'])

    def test_a_row_inside_its_tolerance_is_left_alone(self):
        old = {'linux-x64': {'quicksort': {'all': row(1.0, 0.1)},
                             'recursion': {'all': row(1.0, 0.1)}}}
        overlay, _ = self.run_calibration(
            [('linux-x64', {'quicksort': (1.05, 0.1), 'recursion': (1.4, 0.1)})],
            existing=old, reason='recursion got slower on purpose')
        self.assertEqual(sorted(overlay['rebaseline']['linux-x64']), ['recursion'])
        self.assertNotIn('calibrate', overlay)

    def test_a_moved_row_is_rebaselined_from_its_old_value_and_needs_a_reason(self):
        old = {'linux-x64': {'quicksort': {'all': row(1.0, 0.1, memory=0.3)}}}
        with self.assertRaises(SystemExit):
            self.run_calibration([('linux-x64', {'quicksort': (0.7, 0.1)})], existing=old)
        overlay, rows = self.run_calibration([('linux-x64', {'quicksort': (0.7, 0.1)})],
                                             existing=old, reason='faster partitioning')
        r = overlay['rebaseline']['linux-x64']['quicksort']['all']
        self.assertEqual(r['from'], {'time': 1.0, 'memory': 0.1, 'tolerance': {'memory': 0.3}})
        self.assertEqual(r['time'], 0.7)
        # Only the metric that moved is replaced; RAM keeps its baseline and tolerance.
        self.assertEqual(r['memory'], 0.1)
        self.assertEqual(r['tolerance'], {'memory': 0.3})
        self.assertEqual(overlay['reason'], 'faster partitioning')
        self.assertEqual(rows['linux-x64']['quicksort']['all']['time'], 0.7)

    def test_all_recalibrates_every_measured_row(self):
        old = {'linux-x64': {'quicksort': {'all': row(1.0, 0.1)}}}
        overlay, _ = self.run_calibration(
            [('linux-x64', {'quicksort': (v, 0.1)}) for v in (0.97, 1.0, 1.02, 1.01, 0.99)],
            existing=old, reason='five runs of unchanged code', everything=True)
        r = overlay['rebaseline']['linux-x64']['quicksort']['all']
        self.assertEqual((r['time'], r['runs']), (1.0, 5))

    def test_metric_limits_a_recalibration_to_one_metric(self):
        old = {'linux-x64': {'quicksort': {'all': row(1.0, 0.3, time=0.4)}}}
        overlay, _ = self.run_calibration(
            [('linux-x64', {'quicksort': (t, m)}) for t, m in
             ((1.1, 0.32), (0.9, 0.27), (1.05, 0.32), (0.95, 0.27), (1.0, 0.32))],
            existing=old, reason='bimodal RAM', everything=True, metric='memory')
        r = overlay['rebaseline']['linux-x64']['quicksort']['all']
        self.assertEqual((r['time'], r['tolerance']['time']), (1.0, 0.4))   # untouched
        self.assertEqual(r['memory'], 0.32)
        self.assertGreaterEqual(r['tolerance']['memory'], 0.25)   # covers the 0.27 mode

    def test_rerunning_replaces_this_pull_requests_own_rows(self):
        old = {'linux-x64': {'quicksort': {'all': row(1.0, 0.1)}}}
        mine = {'pr': 7, 'reason': 'first try', 'rebaseline': {'linux-x64': {'quicksort': {
            'all': dict(row(0.7, 0.1), **{'from': {'time': 1.0, 'memory': 0.1}})}}}}
        overlay, rows = self.run_calibration([('linux-x64', {'quicksort': (0.5, 0.1)})],
                                             existing=old, overlays={7: mine})
        r = overlay['rebaseline']['linux-x64']['quicksort']['all']
        # Still FROM the base value, not from this pull request's own earlier 0.7.
        self.assertEqual(r['from']['time'], 1.0)
        self.assertEqual(rows['linux-x64']['quicksort']['all']['time'], 0.5)

    def test_a_stale_own_rebaseline_is_rewritten_from_fresh_runs(self):
        # pr/7 rebaselined quicksort from 1.0; another merged change has since moved it to
        # 0.8, so pr/7 is stale and the gate judged this run without it.
        moved = {'linux-x64': {'quicksort': {'all': row(0.8, 0.1)}}}
        mine = {'pr': 7, 'reason': 'first try', 'rebaseline': {'linux-x64': {'quicksort': {
            'all': dict(row(0.7, 0.1), **{'from': {'time': 1.0, 'memory': 0.1}})}}}}
        # The fresh run is inside 0.8's tolerance, so only staleness can make it rewrite.
        overlay, rows = self.run_calibration([('linux-x64', {'quicksort': (0.82, 0.1)})],
                                             existing=moved, overlays={7: mine})
        r = overlay['rebaseline']['linux-x64']['quicksort']['all']
        self.assertEqual(r['from'], {'time': 0.8, 'memory': 0.1})
        self.assertEqual(rows['linux-x64']['quicksort']['all']['time'], 0.82)

    def test_a_stale_row_the_runs_did_not_measure_is_reported(self):
        moved = {'linux-x64': {'quicksort': {'all': row(0.8, 0.1)}, 'recursion': {'all': row(1.0, 0.1)}}}
        mine = {'pr': 7, 'reason': 'first try', 'rebaseline': {'linux-x64': {'quicksort': {
            'all': dict(row(0.7, 0.1), **{'from': {'time': 1.0, 'memory': 0.1}})}}}}
        with self.assertRaises(SystemExit) as caught:
            self.run_calibration([('linux-x64', {'recursion': (1.0, 0.1)})],
                                 existing=moved, overlays={7: mine})
        self.assertIn('quicksort', str(caught.exception))

    def test_a_row_this_pull_request_calibrated_stays_a_calibration(self):
        mine = {'pr': 7, 'calibrate': {'linux-x64@new': {'quicksort': {'all': row(1.0, 0.1, 1)}}}}
        overlay, _ = self.run_calibration([('linux-x64', {'quicksort': (1.5, 0.1)}, 'New')],
                                          overlays={7: mine})
        self.assertNotIn('rebaseline', overlay)
        self.assertEqual(overlay['calibrate']['linux-x64@new']['quicksort']['all']['time'], 1.5)


class OverlayTests(unittest.TestCase):
    """perf_baseline.py: how base/ and the pull requests' overlays become one baseline."""

    BASE = {'linux-x64@a': {'quicksort': {'all': row(1.0, 0.1)}}}

    def resolve(self, overlays, base=None):
        for number, overlay in overlays:
            baselines.validate_overlay(number, overlay, 'pr/%d.json' % number)
        return baselines.resolve(base if base is not None else self.BASE, overlays)

    def rebase(self, number, t, frm=1.0, reason='moved'):
        return (number, {'pr': number, 'reason': reason, 'rebaseline': {'linux-x64@a': {
            'quicksort': {'all': dict(row(t, 0.1), **{'from': {'time': frm, 'memory': 0.1}})}}}})

    def calib(self, number, t, key='linux-x64@b', runs=1, **tol):
        return (number, {'pr': number, 'calibrate': {key: {'quicksort': {'all': row(t, 0.2, runs, **tol)}}}})

    def test_a_rebaseline_replaces_the_row(self):
        rows, _ = self.resolve([self.rebase(12, 0.8)])
        self.assertEqual(rows['linux-x64@a']['quicksort']['all'], row(0.8, 0.1))

    def test_two_rebaselines_of_one_row_are_a_conflict_naming_both(self):
        with self.assertRaises(baselines.BaselineError) as caught:
            self.resolve([self.rebase(12, 0.8), self.rebase(15, 0.9)])
        self.assertIn('pr/12.json', str(caught.exception))
        self.assertIn('pr/15.json', str(caught.exception))

    def test_a_rebaseline_from_a_stale_value_is_rejected(self):
        # pr/12 was folded into base; pr/15 measured against the value before it.
        moved = {'linux-x64@a': {'quicksort': {'all': row(0.8, 0.1)}}}
        with self.assertRaises(baselines.BaselineError) as caught:
            self.resolve([self.rebase(15, 0.9, frm=1.0)], base=moved)
        self.assertIn('another merged change moved it first', str(caught.exception))

    def test_a_rebaseline_from_a_row_whose_tolerance_moved_is_rejected(self):
        # An earlier --all recalibration changed only the tolerance; the ratios match.
        retuned = {'linux-x64@a': {'quicksort': {'all': row(1.0, 0.1, time=0.4)}}}
        with self.assertRaises(baselines.BaselineError):
            self.resolve([self.rebase(15, 0.8)], base=retuned)
        number, overlay = self.rebase(15, 0.8)
        overlay['rebaseline']['linux-x64@a']['quicksort']['all']['from']['tolerance'] = {'time': 0.4}
        rows, _ = self.resolve([(number, overlay)], base=retuned)
        self.assertEqual(rows['linux-x64@a']['quicksort']['all']['time'], 0.8)

    def test_bad_values_are_baseline_errors_not_crashes(self):
        number, overlay = self.rebase(12, 0.8)
        overlay['rebaseline']['linux-x64@a']['quicksort']['all']['from']['time'] = 'fast'
        with self.assertRaises(baselines.BaselineError):
            baselines.validate_overlay(number, overlay, 'pr/12.json')
        number, overlay = self.rebase(12, float('inf'))
        with self.assertRaises(baselines.BaselineError):
            baselines.validate_overlay(number, overlay, 'pr/12.json')

    def test_wildly_disagreeing_calibrations_are_refused_not_combined(self):
        overlays = [self.calib(12, 1.0), self.calib(13, 1.0), self.calib(14, 10.0)]
        for n, o in overlays:
            baselines.validate_overlay(n, o, 'pr/%d.json' % n)
        with self.assertRaises(baselines.BaselineError) as caught:
            baselines.resolve(self.BASE, overlays, POLICY['tolerance'])
        self.assertIn('pr/14.json', str(caught.exception))

    def test_a_rebaseline_needs_a_reason_and_a_from(self):
        number, overlay = self.rebase(12, 0.8, reason='TODO')
        with self.assertRaises(baselines.BaselineError):
            baselines.validate_overlay(number, overlay, 'pr/12.json')
        number, overlay = self.rebase(12, 0.8)
        del overlay['rebaseline']['linux-x64@a']['quicksort']['all']['from']
        with self.assertRaises(baselines.BaselineError):
            baselines.validate_overlay(number, overlay, 'pr/12.json')

    def test_a_rebaseline_of_a_missing_row_is_rejected(self):
        with self.assertRaises(baselines.BaselineError):
            self.resolve([self.rebase(12, 0.8)], base={})

    def test_the_overlay_number_must_match_its_file(self):
        with self.assertRaises(baselines.BaselineError):
            baselines.validate_overlay(13, self.calib(12, 1.0)[1], 'pr/13.json')

    def test_two_branches_calibrating_one_cpu_are_combined_in_any_order(self):
        a, b = self.calib(12, 1.0, time=0.3), self.calib(15, 1.2, runs=2)
        rows_ab, _ = self.resolve([a, b])
        rows_ba, _ = self.resolve([b, a])
        self.assertEqual(rows_ab, rows_ba)
        combined = rows_ab['linux-x64@b']['quicksort']['all']
        self.assertEqual((combined['time'], combined['runs']), (1.1, 3))
        # Wide enough for 1.0x +/- 30% around the 1.1x median: down to 0.7x is -36%.
        self.assertEqual(combined['tolerance'], {'time': 0.4})

    def test_combined_calibrations_still_pass_the_runs_they_came_from(self):
        tol = POLICY['tolerance']
        a, b = self.calib(12, 1.0), self.calib(15, 2.0)
        for n, o in (a, b):
            baselines.validate_overlay(n, o, 'pr/%d.json' % n)
        rows, _ = baselines.resolve(self.BASE, [a, b], tol)
        r = rows['linux-x64@b']['quicksort']['all']
        self.assertEqual(r['time'], 1.5)
        for ratio in (1.0, 2.0, 1.0 * 0.85, 2.0 * 1.15):
            self.assertEqual(gate.verdict(ratio, r['time'], r['tolerance']['time']), 'ok', ratio)
        self.assertNotIn('memory', r.get('tolerance', {}))   # 0.2 and 0.2: the global 15%

    def test_a_calibration_of_an_existing_row_is_superseded_not_fatal(self):
        rows, notes = self.resolve([self.calib(12, 9.9, key='linux-x64@a')])
        self.assertEqual(rows['linux-x64@a']['quicksort']['all']['time'], 1.0)
        self.assertIn('superseded', notes[0])

    def test_a_rebaseline_can_move_a_row_another_branch_calibrated(self):
        frm = (15, {'pr': 15, 'reason': 'moved', 'rebaseline': {'linux-x64@b': {'quicksort': {
            'all': dict(row(0.5, 0.2), **{'from': {'time': 1.0, 'memory': 0.2}})}}}})
        rows, _ = self.resolve([self.calib(12, 1.0), frm])
        self.assertEqual(rows['linux-x64@b']['quicksort']['all']['time'], 0.5)

    def test_fold_moves_everything_into_base_and_changes_no_verdict(self):
        tree = BaselineTree(self.BASE, dict([self.rebase(12, 0.8), self.calib(15, 1.0)]))
        try:
            before = baselines.load(tree.root)['platforms']
            self.assertEqual(baselines.fold(tree.root), [12, 15])
            self.assertEqual(list((tree.root / 'pr').iterdir()), [])
            self.assertEqual(baselines.load_base(tree.root), before)
            self.assertEqual(sorted(p.name for p in (tree.root / 'base').iterdir()),
                             ['linux-x64@a.json', 'linux-x64@b.json'])
            self.assertEqual(baselines.fold(tree.root), [])
        finally:
            tree.close()

    def test_fold_refuses_contradicting_overlays(self):
        tree = BaselineTree(self.BASE, dict([self.rebase(12, 0.8), self.rebase(15, 0.9)]))
        try:
            with self.assertRaises(baselines.BaselineError):
                baselines.fold(tree.root)
            self.assertEqual(len(list((tree.root / 'pr').iterdir())), 2)
        finally:
            tree.close()

    def test_the_checked_in_baselines_resolve(self):
        data = baselines.load()
        self.assertTrue(data['platforms'])
        for key in data['platforms']:
            self.assertRegex(key, baselines.KEY_RE)

    def test_import_legacy_keeps_what_master_changed_in_the_same_row(self):
        # master added a time tolerance after the branch point; the branch re-measured RAM.
        tree = BaselineTree({'linux-x64@a': {'quicksort': {'all': row(1.0, 0.1, time=0.4)}}})
        try:
            original = dict(POLICY, platforms={'linux-x64@a': {'quicksort': {'all': row(1.0, 0.1)}}})
            legacy = dict(POLICY, platforms={'linux-x64@a': {'quicksort': {'all': row(1.0, 0.3)}}})
            baselines.import_legacy(tree.root, 33, legacy, original, 'ram re-measured')
            r = tree.overlay(33)['rebaseline']['linux-x64@a']['quicksort']['all']
            self.assertEqual((r['memory'], r['tolerance']), (0.3, {'time': 0.4}))
            # Both sides changing the SAME field differently is refused, not guessed.
            clash = dict(POLICY, platforms={'linux-x64@a': {'quicksort': {'all': row(1.0, 0.1, time=0.2)}}})
            with self.assertRaises(baselines.BaselineError) as caught:
                baselines.import_legacy(tree.root, 34, clash, original, 'x')
            self.assertIn('tolerance.time', str(caught.exception))
        finally:
            tree.close()

    def test_import_legacy_refuses_deleted_rows(self):
        # The old calibrator's --fresh dropped rows a run did not re-measure.
        tree = BaselineTree({'linux-x64@a': {'quicksort': {'all': row(1.0, 0.1)},
                                             'recursion': {'all': row(2.0, 0.1)}}})
        try:
            original = dict(POLICY, platforms={'linux-x64@a': {
                'quicksort': {'all': row(1.0, 0.1)}, 'recursion': {'all': row(2.0, 0.1)}}})
            legacy = dict(POLICY, platforms={'linux-x64@a': {'quicksort': {'all': row(1.0, 0.1)}}})
            with self.assertRaises(baselines.BaselineError) as caught:
                baselines.import_legacy(tree.root, 35, legacy, original, 'x')
            self.assertIn('linux-x64@a recursion/all', str(caught.exception))
            self.assertIsNone(tree.overlay(35))
        finally:
            tree.close()

    def test_import_legacy_refuses_policy_edits(self):
        tree = BaselineTree(self.BASE)
        try:
            original = dict(POLICY, platforms={})
            legacy = dict(POLICY, tolerance={'time': 0.2, 'memory': 0.15}, platforms={})
            with self.assertRaises(baselines.BaselineError) as caught:
                baselines.import_legacy(tree.root, 36, legacy, original, 'x')
            self.assertIn('policy.json', str(caught.exception))
        finally:
            tree.close()

    def test_a_broken_policy_is_refused(self):
        import json
        for bad in ({'time': '0.15', 'memory': 0.15}, {'time': -0.1, 'memory': 0.15},
                    {'time': 0, 'memory': 0.15}):
            tree = BaselineTree()
            try:
                (tree.root / 'policy.json').write_text(json.dumps(dict(POLICY, tolerance=bad)))
                with self.assertRaises(baselines.BaselineError):
                    baselines.load_policy(tree.root)
            finally:
                tree.close()

    def test_import_legacy_takes_only_the_branchs_own_edits(self):
        tree = BaselineTree({'linux-x64@a': {'quicksort': {'all': row(1.0, 0.1, memory=0.3)},
                                             'recursion': {'all': row(2.0, 0.1)}}})
        try:
            # The branch started before master gave quicksort its RAM tolerance, so its
            # copy has the OLD quicksort row -- that is not an edit of its own.
            original = dict(POLICY, platforms={'linux-x64@a': {
                'quicksort': {'all': row(1.0, 0.1)}, 'recursion': {'all': row(2.0, 0.1)}}})
            legacy = dict(POLICY, platforms={
                'linux-x64@a': {'quicksort': {'all': row(1.0, 0.1)},
                                'recursion': {'all': row(1.5, 0.1)}},
                'linux-x64@b': {'quicksort': {'all': row(2.0, 0.2, 1)}}})
            baselines.import_legacy(tree.root, 31, legacy, original, 'imported')
            overlay = tree.overlay(31)
            self.assertEqual(overlay['calibrate']['linux-x64@b']['quicksort']['all']['time'], 2.0)
            self.assertEqual(sorted(overlay['rebaseline']['linux-x64@a']), ['recursion'])
            self.assertEqual(overlay['rebaseline']['linux-x64@a']['recursion']['all']['from'],
                             {'time': 2.0, 'memory': 0.1})
            # A row both the branch and master calibrated stays master's, and is no rebaseline.
            master_too = dict(legacy, platforms=dict(legacy['platforms'], **{
                'linux-x64@c': {'quicksort': {'all': row(3.0, 0.3, 1)}}}))
            tree2 = BaselineTree({'linux-x64@c': {'quicksort': {'all': row(2.9, 0.3, 2)}}})
            try:
                path, notes = baselines.import_legacy(tree2.root, 32, master_too, original, 'x')
                self.assertNotIn('rebaseline', tree2.overlay(32) or {})
                self.assertTrue(any('linux-x64@c' in n for n in notes), notes)
            finally:
                tree2.close()
        finally:
            tree.close()


class StaleOverlayGateTests(unittest.TestCase):
    """perf-gate.py with this pull request's own overlay gone stale: it must still measure
    (here it gets as far as looking for the binaries) and record why it will fail."""

    def test_the_gate_measures_without_its_own_stale_overlay(self):
        import json
        import os
        moved = {'linux-x64': {'quicksort': {'all': row(0.8, 0.1)}}}
        mine = {'pr': 7, 'reason': 'first try', 'rebaseline': {'linux-x64': {'quicksort': {
            'all': dict(row(0.7, 0.1), **{'from': {'time': 1.0, 'memory': 0.1}})}}}}
        tree = BaselineTree(moved, {7: mine})
        saved = os.environ.get('CN1_PR_NUMBER')
        os.environ['CN1_PR_NUMBER'] = '7'
        try:
            out = tree.root.parent / 'results.json'
            gate.main(['--baseline', str(tree.root), '--out', str(out), '--platform', 'linux-x64',
                       '--binary', str(tree.root.parent / 'no-such-binary')])
            report = json.loads(out.read_text())
            self.assertIn('another merged change moved it first', report['stale_overlay'])
            # It got past the baseline: what stopped it is the missing binary, not the overlay.
            self.assertFalse(report['error'].startswith('perf-baseline'), report['error'])
            text = gate.render_markdown(dict(report, error=None, results={}))
            self.assertIn('perf-baseline/pr/7.json` is stale', text)
            self.assertIn('**Result: stale overlay: re-measure required**', text)
        finally:
            if saved is None:
                os.environ.pop('CN1_PR_NUMBER', None)
            else:
                os.environ['CN1_PR_NUMBER'] = saved
            tree.close()


class CheckTests(unittest.TestCase):
    """perf_baseline.py check --base: what a pull request may change."""

    def check(self, changed, number=31, migrating=False, merged=(), legacy=False):
        originals = baselines.changed_files, baselines.exists_at, baselines.legacy_touched
        baselines.changed_files = lambda base_ref, root=None: changed
        baselines.exists_at = lambda ref, path, root=None: (
            not migrating if path == 'base' else path in merged)
        baselines.legacy_touched = lambda base_ref: legacy
        try:
            return baselines.check(baselines.ROOT, 'base-sha', number)
        finally:
            baselines.changed_files, baselines.exists_at, baselines.legacy_touched = originals

    def test_a_merged_overlay_may_be_repaired(self):
        # Two merged pull requests that rebaselined one row leave master unresolvable;
        # editing or deleting one of their overlays is the fix, and must pass.
        self.assertEqual(self.check(['pr/30.json'], merged=('pr/30.json',)), [])

    def test_the_retired_file_is_refused(self):
        problems = self.check([], legacy=True)
        self.assertEqual(len(problems), 1)
        self.assertIn('import-legacy', problems[0])

    def test_the_migration_that_creates_base_may(self):
        self.assertEqual(self.check(['base/linux-x64@a.json'], migrating=True), [])

    def test_a_pull_request_may_write_its_own_overlay_and_the_policy(self):
        self.assertEqual(self.check(['pr/31.json', 'policy.json']), [])

    def test_base_is_the_folds_alone(self):
        problems = self.check(['base/linux-x64@a.json'])
        self.assertEqual(len(problems), 1)
        self.assertIn('nightly fold', problems[0])

    def test_another_pull_requests_overlay_is_off_limits(self):
        self.assertIn('pr/31.json', self.check(['pr/30.json'])[0])


class SummaryTests(unittest.TestCase):
    def test_one_entry_per_platform_with_the_spread_of_its_cpus(self):
        rows = {'linux-x64@a': {'quicksort': {'all': row(1.0, 0.1)}},
                'linux-x64@b': {'quicksort': {'all': row(1.4, 0.3)}},
                'linux-x64@c': {'quicksort': {'all': row(1.2, 0.2)}},
                'macos-arm64': {'quicksort': {'all': row(0.9, 0.1)}}}
        s = baselines.summary(rows)
        self.assertEqual([p['id'] for p in s['platforms']], ['linux-x64', 'macos-arm64'])
        linux = s['platforms'][0]
        self.assertEqual(linux['name'], 'Linux x64')
        self.assertEqual(linux['cpus'], ['a', 'b', 'c'])
        self.assertEqual(linux['benchmarks']['quicksort']['time'],
                         {'median': 1.2, 'min': 1.0, 'max': 1.4})

    def test_every_gated_benchmark_is_described(self):
        self.assertEqual([b for b, _, _ in baselines.BENCHMARKS],
                         ['hello', 'translator'] + gate.WORKLOADS)

    def test_shared_workloads_read_as_the_absolute_table_does(self):
        # The workloads are CommonWorkloads, which the Port Status page's absolute table
        # also runs; one benchmark must not be described two ways on one page.
        import json
        support = json.loads((Path(__file__).resolve().parents[2] /
                              'docs/website/data/port_status_support.json').read_text())
        mine = {b: (n, d) for b, n, d in baselines.BENCHMARKS}
        for r in support['benchmark']['rows']:
            self.assertEqual(mine[r['id']], (r['name'], r['description']), r['id'])


class VerdictStepTests(unittest.TestCase):
    """ci-perf-gate.sh verdict: the job's last step, which decides pass or fail."""

    def verdict(self, results):
        # The verdict is the Python heredoc inside ci-perf-gate.sh; run THAT text with this
        # interpreter. Not through bash: on a Windows runner a bare "bash" can resolve to
        # System32's WSL launcher, which has no distribution installed and fails.
        import json
        import subprocess
        import sys
        import tempfile
        script = (Path(__file__).parent / 'ci-perf-gate.sh').read_text()
        start = script.index("<<'PY'", script.index('if [ "$MODE" = verdict ]'))
        start = script.index('\n', start) + 1
        body = script[start:script.index('\nPY\n', start)]
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'perf-results.json'
            path.write_text(json.dumps(results))
            return subprocess.run([sys.executable, '-', str(path)], input=body,
                                  capture_output=True, text=True)

    def row(self, verdict_):
        return {'all': {'time': {'median': 0.7 if verdict_ == 'improved' else 1.0,
                                 'baseline': None if verdict_ == 'uncalibrated' else 1.0,
                                 'verdict': verdict_},
                        'memory': {'median': 0.5, 'baseline': 0.5, 'verdict': 'ok'}}}

    def test_a_missing_baseline_fails_the_job(self):
        r = self.verdict({'platform': 'windows-x64', 'cpu': 'AMD64 Family 25 Model 17',
                          'calibration_key': 'windows-x64@amd64-family-25-model-17',
                          'results': {'quicksort': self.row('uncalibrated')},
                          'calibration': {'quicksort': {'all': {'time': 1.0, 'memory': 0.5}}},
                          'regression': False})
        self.assertEqual(r.returncode, 1, r.stdout + r.stderr)
        self.assertIn('NO BASELINE', r.stdout)
        self.assertIn('calibrate-perf-baseline.py', r.stdout)

    def test_an_improvement_fails_the_job(self):
        r = self.verdict({'platform': 'linux-x64', 'pr': 5931, 'labels': {},
                          'results': {'quicksort': self.row('improved')}, 'regression': False})
        self.assertEqual(r.returncode, 1, r.stdout + r.stderr)
        self.assertIn('IMPROVED', r.stdout)
        self.assertIn('perf-baseline/pr/5931.json', r.stdout)

    def test_a_stale_overlay_fails_the_job(self):
        r = self.verdict({'platform': 'linux-x64', 'pr': 7, 'labels': {},
                          'results': {'quicksort': self.row('ok')}, 'regression': False,
                          'stale_overlay': 'pr/7.json rebaselines ... moved it first'})
        self.assertEqual(r.returncode, 1, r.stdout + r.stderr)
        self.assertIn('pr/7.json is stale', r.stdout)

    def test_a_judged_run_passes(self):
        r = self.verdict({'platform': 'linux-x64', 'results': {'quicksort': self.row('ok')},
                          'regression': False})
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)


class CpuClassTests(unittest.TestCase):
    def test_one_model_is_one_key(self):
        c = gate.cpu_class
        self.assertEqual(c('AMD64 Family 25 Model 1 Stepping 1, AuthenticAMD'),
                         c('AMD64 Family 25 Model 1 Stepping 2, AuthenticAMD'))
        self.assertNotEqual(c('AMD64 Family 25 Model 1 Stepping 1, AuthenticAMD'),
                            c('AMD64 Family 25 Model 17 Stepping 1, AuthenticAMD'))
        self.assertEqual(c('Intel(R) Xeon(R) Platinum 8370C CPU @ 2.80GHz'),
                         'intel-xeon-platinum-8370c')
        self.assertEqual(c('Neoverse-N2'), 'neoverse-n2')

    def test_an_undecodable_cpu_is_unknown(self):
        for cpu in (None, '', 'aarch64', 'unknown', 'AMD64'):
            self.assertIsNone(gate.cpu_class(cpu), cpu)


if __name__ == '__main__':
    unittest.main()
