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
        self.assertIn('not gated (no baseline)', text)

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


class CalibrationTest(unittest.TestCase):
    """calibrate-perf-baseline.py: medians as baselines, spread-driven tolerances."""

    def run_calibration(self, runs, existing=None):
        import json
        import tempfile
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / 'baseline.json'
            out.write_text(json.dumps({'tolerance': {'time': 0.15, 'memory': 0.15},
                                       'floor': {'time': 0.0, 'memory': 0.05},
                                       'platforms': existing or {}}))
            files = []
            for i, run in enumerate(runs):
                platform, per_bench = run[0], run[1]
                results = {b: {'all': {'time': {'median': t}, 'memory': {'median': m}}}
                           for b, (t, m) in per_bench.items()}
                report = {'platform': platform, 'results': results}
                if len(run) > 2:
                    report['cpu'] = run[2]
                f = Path(tmp) / ('run%d.json' % i)
                f.write_text(json.dumps(report))
                files.append(str(f))
            calibrate.main(['--out', str(out)] + files)
            return json.loads(out.read_text())

    def test_median_baseline_and_global_tolerance_for_a_steady_row(self):
        b = self.run_calibration([('linux-x64', {'quicksort': (1.00, 0.10)}),
                                  ('linux-x64', {'quicksort': (1.02, 0.10)}),
                                  ('linux-x64', {'quicksort': (1.01, 0.10)})])
        row = b['platforms']['linux-x64']['quicksort']['all']
        self.assertEqual(row['time'], 1.01)
        self.assertNotIn('tolerance', row)  # 1% spread: the global 15% already covers it
        self.assertEqual(row['runs'], 3)

    def test_noisy_row_gets_a_wider_tolerance_that_covers_every_run(self):
        runs = [('linux-x64', {'objectAllocation': (v, 0.4)}) for v in (5.0, 5.5, 6.5)]
        b = self.run_calibration(runs)
        row = b['platforms']['linux-x64']['objectAllocation']['all']
        self.assertEqual(row['time'], 5.5)
        tol = row['tolerance']['time']
        self.assertGreater(tol, 0.15)
        for _, per in runs:
            self.assertNotEqual(gate.verdict(per['objectAllocation'][0], row['time'], tol), 'regression')
        # ...and it still bites: well past the observed spread is a regression.
        self.assertEqual(gate.verdict(row['time'] * (1 + tol) * 1.01, row['time'], tol), 'regression')

    def test_single_run_platform_borrows_the_widest_tolerance_seen_elsewhere(self):
        runs = [('linux-x64', {'objectAllocation': (v, 0.4)}) for v in (5.0, 5.5, 6.5)]
        runs.append(('macos-arm64', {'objectAllocation': (3.0, 0.3)}))
        b = self.run_calibration(runs)
        linux = b['platforms']['linux-x64']['objectAllocation']['all']['tolerance']['time']
        mac = b['platforms']['macos-arm64']['objectAllocation']['all']
        self.assertEqual(mac['runs'], 1)
        self.assertEqual(mac['tolerance']['time'], linux)

    def test_a_mixed_cpu_pool_gets_per_class_rows_and_a_pooled_fallback(self):
        amd = 'AMD64 Family 25 Model 1 Stepping 1, AuthenticAMD'
        intel = 'Intel64 Family 6 Model 207 Stepping 2, GenuineIntel'
        runs = [('windows-x64', {'objectAllocation': (v, 0.4)}, amd) for v in (6.3, 7.4, 7.0)]
        runs += [('windows-x64', {'objectAllocation': (v, 0.56)}, intel) for v in (2.6, 2.7)]
        b = self.run_calibration(runs)['platforms']
        self.assertEqual(b['windows-x64@amd']['objectAllocation']['all']['time'], 7.0)
        self.assertEqual(b['windows-x64@intel']['objectAllocation']['all']['memory'], 0.56)
        self.assertEqual(b['windows-x64']['objectAllocation']['all']['runs'], 5)
        self.assertEqual(gate.baseline_key(b, 'windows-x64', intel), 'windows-x64@intel')
        self.assertEqual(gate.baseline_key(b, 'windows-x64', 'Some Future CPU'), 'windows-x64')

    def test_a_single_cpu_pool_is_not_split(self):
        cpu = 'AMD EPYC 7763 64-Core Processor'
        runs = [('linux-x64', {'quicksort': (v, 0.1)}, cpu) for v in (1.0, 1.01)]
        b = self.run_calibration(runs)['platforms']
        self.assertEqual(sorted(b), ['linux-x64'])
        self.assertEqual(gate.baseline_key(b, 'linux-x64', cpu), 'linux-x64')


    def test_an_unmeasured_platform_keeps_its_rows_and_a_measured_one_is_replaced(self):
        old = {'macos-arm64': {'quicksort': {'all': {'time': 0.9, 'memory': 0.1, 'runs': 1}}},
               'linux-x64': {'quicksort': {'all': {'time': 9.9, 'memory': 0.9, 'runs': 1}}},
               'linux-x64@intel': {'quicksort': {'all': {'time': 9.9, 'memory': 0.9, 'runs': 1}}}}
        b = self.run_calibration([('linux-x64', {'quicksort': (1.0, 0.1)})], existing=old)
        b = b['platforms']
        self.assertEqual(b['macos-arm64'], old['macos-arm64'])
        self.assertEqual(b['linux-x64']['quicksort']['all']['time'], 1.0)
        self.assertNotIn('linux-x64@intel', b)  # measured platform: stale split rows go


class CpuClassTests(unittest.TestCase):
    def test_x64_vendors_and_everything_else(self):
        self.assertEqual(gate.cpu_class('AMD EPYC 7763 64-Core Processor'), 'amd')
        self.assertEqual(gate.cpu_class('Intel64 Family 6 Model 207 Stepping 2, GenuineIntel'),
                         'intel')
        self.assertIsNone(gate.cpu_class('Neoverse-N2'))
        self.assertIsNone(gate.cpu_class('Apple M1 (Virtual)'))
        self.assertIsNone(gate.cpu_class(None))


if __name__ == '__main__':
    unittest.main()
