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


if __name__ == '__main__':
    unittest.main()
