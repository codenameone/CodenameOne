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
report = load('perf_report', 'perf-report.py')


def metric(median, baseline, verdict):
    return {'median': median, 'min': median, 'max': median, 'rounds': [median],
            'baseline': baseline, 'verdict': verdict}


def result(platform, entries, skipped=()):
    return {'platform': platform, 'rounds': 5, 'tolerance': {'time': 0.1, 'memory': 0.1},
            'available_cores': 4, 'skipped_cores': list(skipped),
            'results': {cores: {'enforced': True, 'time': t, 'memory': m}
                        for cores, (t, m) in entries.items()}}


class VerdictTests(unittest.TestCase):
    def test_tolerance_is_relative_to_the_baseline(self):
        self.assertEqual('ok', gate.verdict(1.09, 1.0, 0.1))
        self.assertEqual('regression', gate.verdict(1.11, 1.0, 0.1))
        self.assertEqual('improved', gate.verdict(0.89, 1.0, 0.1))
        self.assertEqual('uncalibrated', gate.verdict(5.0, None, 0.1))

    def test_platform_keys(self):
        self.assertRegex(gate.platform_key(), r'^(linux|macos|windows)-(x64|arm64)$')


class ReportTests(unittest.TestCase):
    def test_regression_fails_and_is_marked(self):
        text, failed = report.render([result('linux-x64', {
            '1': (metric(1.3, 1.0, 'regression'), metric(0.9, 0.9, 'ok'))})], ['linux-x64'])
        self.assertTrue(failed)
        self.assertIn('**REGRESSION**', text)

    def test_missing_platform_fails(self):
        text, failed = report.render([result('linux-x64', {
            '1': (metric(0.7, 0.7, 'ok'), metric(0.9, 0.9, 'ok'))})],
            ['linux-x64', 'windows-x64'])
        self.assertTrue(failed)
        self.assertIn('no result', text)

    def test_uncalibrated_reports_without_failing(self):
        text, failed = report.render([result('macos-arm64', {
            '1': (metric(0.7, None, 'uncalibrated'), metric(0.6, None, 'uncalibrated'))},
            skipped=[4])], ['macos-arm64'])
        self.assertFalse(failed)
        self.assertIn('not gated', text)
        self.assertIn('not run', text)

    def test_marker_leads_so_the_workflow_can_find_its_comment(self):
        text, _ = report.render([], [])
        self.assertTrue(text.startswith(report.MARKER))

    def test_only_ratios_are_printed(self):
        text, _ = report.render([result('linux-x64', {
            '1': (metric(0.7234, 0.72, 'ok'), metric(0.88, 0.9, 'ok')),
            '4': (metric(1.01, 1.0, 'ok'), metric(0.75, 0.8, 'ok'))})], ['linux-x64'])
        # Every decimal in the table is a ratio ("0.72x"); a bare decimal would be an
        # absolute measurement leaking in.
        table = [line for line in text.splitlines() if line.startswith('| Linux')]
        for line in table:
            for number in re.findall(r'\d+\.\d+x?', line):
                self.assertTrue(number.endswith('x'), line)


if __name__ == '__main__':
    unittest.main()
