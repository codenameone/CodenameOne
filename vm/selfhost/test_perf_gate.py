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

    def run_calibration(self, runs, existing=None, fresh=False):
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
            calibrate.main(['--out', str(out)] + (['--fresh'] if fresh else []) + files)
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

    def test_rows_are_per_cpu_model_and_an_unseen_model_is_not_gated(self):
        zen3 = 'AMD64 Family 25 Model 1 Stepping 1, AuthenticAMD'
        zen4 = 'AMD64 Family 25 Model 17 Stepping 1, AuthenticAMD'
        intel = 'Intel64 Family 6 Model 207 Stepping 2, GenuineIntel'
        runs = [('windows-x64', {'arraySequential': (v, 0.4)}, zen3) for v in (1.35, 1.36, 1.38)]
        runs += [('windows-x64', {'arraySequential': (1.98, 0.38)}, intel)]
        b = self.run_calibration(runs)['platforms']
        self.assertEqual(sorted(b), ['windows-x64@amd64-family-25-model-1-authenticamd',
                                     'windows-x64@intel64-family-6-model-207-genuineintel'])
        self.assertEqual(gate.baseline_key(b, 'windows-x64', zen3.replace('Stepping 1', 'Stepping 2')),
                         'windows-x64@amd64-family-25-model-1-authenticamd')
        # A model no run has measured is not judged against another model's ratios.
        self.assertIsNone(gate.baseline_key(b, 'windows-x64', zen4))
        self.assertIsNone(gate.baseline_key(b, 'windows-x64', None))

    def test_a_run_with_no_known_cpu_feeds_the_plain_row(self):
        b = self.run_calibration([('macos-arm64', {'quicksort': (1.0, 0.1)})])['platforms']
        self.assertEqual(sorted(b), ['macos-arm64'])
        self.assertEqual(gate.baseline_key(b, 'macos-arm64', 'Apple M1 (Virtual)'), 'macos-arm64')

    def test_only_measured_rows_are_replaced_unless_fresh(self):
        old = {'macos-arm64': {'quicksort': {'all': {'time': 0.9, 'memory': 0.1, 'runs': 1}}},
               'linux-x64': {'quicksort': {'all': {'time': 9.9, 'memory': 0.9, 'runs': 1}}},
               'linux-x64@intel': {'quicksort': {'all': {'time': 9.9, 'memory': 0.9, 'runs': 1}}}}
        b = self.run_calibration([('linux-x64', {'quicksort': (1.0, 0.1)})], existing=old)
        b = b['platforms']
        self.assertEqual(b['macos-arm64'], old['macos-arm64'])
        self.assertEqual(b['linux-x64']['quicksort']['all']['time'], 1.0)
        # Adding one CPU model's rows must not drop the platform's other models.
        self.assertEqual(b['linux-x64@intel'], old['linux-x64@intel'])
        fresh = self.run_calibration([('linux-x64', {'quicksort': (1.0, 0.1)})], existing=old,
                                     fresh=True)['platforms']
        self.assertEqual(sorted(fresh), ['linux-x64'])

    def test_a_thinly_sampled_row_borrows_the_widest_tolerance(self):
        runs = [('linux-x64', {'hello': (v, 0.8)}, 'CPU A') for v in (0.50, 0.70, 0.60, 0.55, 0.65)]
        runs += [('linux-x64', {'hello': (v, 0.8)}, 'CPU B') for v in (0.79, 0.80)]
        b = self.run_calibration(runs)['platforms']
        wide = b['linux-x64@cpu-a']['hello']['all']['tolerance']['time']
        self.assertEqual(b['linux-x64@cpu-a']['hello']['all']['runs'], 5)
        # Two runs 1% apart would give 15%; two runs cannot estimate a spread.
        self.assertEqual(b['linux-x64@cpu-b']['hello']['all']['tolerance']['time'], wide)

    def test_a_single_run_row_borrows_tolerance_from_the_existing_file(self):
        old = {'windows-x64@a': {'arraySequential': {'all': {
            'time': 1.35, 'memory': 0.4, 'runs': 3, 'tolerance': {'time': 0.7}}}}}
        b = self.run_calibration([('windows-x64', {'arraySequential': (1.67, 0.38)}, 'Model B')],
                                 existing=old)['platforms']
        self.assertEqual(b['windows-x64@model-b']['arraySequential']['all']['tolerance']['time'], 0.7)


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
        return {'all': {'time': {'median': 1.0, 'baseline': None if verdict_ == 'uncalibrated'
                                 else 1.0, 'verdict': verdict_},
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


class FailureLogTests(unittest.TestCase):
    def test_a_failed_benchmark_keeps_its_logs_beside_the_results(self):
        import tempfile
        with tempfile.TemporaryDirectory() as tmp:
            work = Path(tmp) / 'work'
            work.mkdir()
            for name in ('hello-call-r00-parpar.log', 'hello-call-r00-parpar.log.err',
                         'hello-c2-r00-parpar.log', 'bench-call-r00-parpar.log'):
                (work / name).write_text(name)
            out = Path(tmp) / 'out' / 'perf-results.json'
            out.parent.mkdir()
            kept = gate.keep_failure_logs(work, 'hello', None, str(out))
            self.assertEqual(sorted(p.name for p in Path(kept).iterdir()),
                             ['hello-call-r00-parpar.log', 'hello-call-r00-parpar.log.err'])
            self.assertEqual(Path(kept).parent, out.parent.resolve())

    def test_nothing_is_kept_without_an_output_directory(self):
        self.assertIsNone(gate.keep_failure_logs(Path('.'), 'hello', None, None))


if __name__ == '__main__':
    unittest.main()
