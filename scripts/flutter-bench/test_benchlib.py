#!/usr/bin/env python3
"""Tests for the parts of the benchmark that decide what a number MEANS.

    python3 scripts/flutter-bench/test_benchlib.py

The arithmetic here is what turns two measurements into a published claim, so
it is the part worth pinning: a ratio computed the wrong way round, or a
regression gate that compares against the wrong side, produces a confident
number that is simply false. The platform adapters are not covered -- they need
real builds and real devices, and `run_bench.py --list` reports which of them
have ever been run rather than pretending otherwise.
"""

import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import benchlib  # noqa: E402


def _report(cn1, flutter):
    return benchlib.build_report(
        "test",
        {"codenameone": dict(cn1), "flutter": dict(flutter)},
        runs=3)


class VerdictTest(unittest.TestCase):

    def test_lower_is_better_so_a_ratio_above_one_is_our_win(self):
        report = _report(
            {"install_bytes": 10 * 1024 * 1024, "cold_start_runs": [200]},
            {"install_bytes": 25 * 1024 * 1024, "cold_start_runs": [500]})
        size = report["verdict"]["install_bytes"]
        self.assertEqual(size["winner"], "codenameone")
        self.assertAlmostEqual(size["ratio"], 2.5, places=3)

    def test_a_loss_is_reported_as_a_loss(self):
        """The harness must be able to say we lost, or it is not a measurement."""
        report = _report(
            {"install_bytes": 40 * 1024 * 1024},
            {"install_bytes": 20 * 1024 * 1024})
        size = report["verdict"]["install_bytes"]
        self.assertEqual(size["winner"], "flutter")
        self.assertLess(size["ratio"], 1.0)

    def test_a_missing_side_is_not_measured_rather_than_a_win(self):
        report = _report({"install_bytes": 10}, {})
        self.assertEqual(report["verdict"]["install_bytes"]["status"],
                         "not measured")

    def test_a_zero_is_not_measured_rather_than_an_infinite_ratio(self):
        report = _report({"install_bytes": 0}, {"install_bytes": 10})
        self.assertEqual(report["verdict"]["install_bytes"]["status"],
                         "not measured")


class StatisticsTest(unittest.TestCase):

    def test_best_of_takes_the_minimum_not_the_mean(self):
        # A shared runner's long right tail measures the runner, not the build.
        self.assertEqual(benchlib.best_of([210, 205, 900, 207]), 205)

    def test_best_of_no_samples_is_none(self):
        self.assertIsNone(benchlib.best_of([]))

    def test_percentile_is_nearest_rank(self):
        values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        self.assertEqual(benchlib.percentile(values, 50), 5)
        self.assertEqual(benchlib.percentile(values, 100), 10)

    def test_summarise_brackets_start_up(self):
        side = benchlib.summarise({
            "cold_start_runs": [300, 280],
            "cold_start_lower_runs": [210, 200],
        })
        self.assertEqual(side["cold_start_ms"], 280)
        self.assertEqual(side["cold_start_lower_ms"], 200)


class RegressionGateTest(unittest.TestCase):

    BASELINE = {
        "codenameone": {"install_bytes": 1000, "cold_start_ms": 200},
        "tolerances": {"install_bytes": 0.02, "cold_start_ms": 0.25},
    }

    def test_within_tolerance_is_silent(self):
        report = _report({"install_bytes": 1015}, {"install_bytes": 5000})
        self.assertEqual(
            benchlib.check_regressions(report, self.BASELINE), [])

    def test_past_tolerance_is_reported(self):
        report = _report({"install_bytes": 1100}, {"install_bytes": 5000})
        found = benchlib.check_regressions(report, self.BASELINE)
        self.assertEqual(len(found), 1)
        self.assertEqual(found[0]["metric"], "install_bytes")
        self.assertAlmostEqual(found[0]["over_by"], 10.0, places=1)

    def test_an_improvement_never_fails_the_build(self):
        report = _report({"install_bytes": 500}, {"install_bytes": 5000})
        self.assertEqual(
            benchlib.check_regressions(report, self.BASELINE), [])

    def test_only_our_own_numbers_are_gated(self):
        """A Flutter SDK upgrade must not be able to turn our build red.

        Their size is outside our control and is recorded for the ratio only.
        """
        report = _report({"install_bytes": 1000}, {"install_bytes": 99999999})
        self.assertEqual(
            benchlib.check_regressions(report, self.BASELINE), [])

    def test_a_metric_with_no_tolerance_is_not_gated(self):
        baseline = {"codenameone": {"install_bytes": 1000}, "tolerances": {}}
        report = _report({"install_bytes": 99999}, {"install_bytes": 5000})
        self.assertEqual(benchlib.check_regressions(report, baseline), [])

    def test_no_baseline_yet_is_not_a_failure(self):
        report = _report({"install_bytes": 99999}, {"install_bytes": 5000})
        self.assertEqual(benchlib.check_regressions(report, None), [])


class SizingTest(unittest.TestCase):

    def test_tree_size_sums_file_lengths(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.makedirs(os.path.join(tmp, "sub"))
            for name, size in (("a", 100), ("sub/b", 250)):
                with open(os.path.join(tmp, name), "wb") as handle:
                    handle.write(b"x" * size)
            self.assertEqual(benchlib.tree_size(tmp), 350)

    def test_tree_size_ignores_symlinks(self):
        """A symlink is not bytes the user downloads; counting it double-counts."""
        with tempfile.TemporaryDirectory() as tmp:
            target = os.path.join(tmp, "real")
            with open(target, "wb") as handle:
                handle.write(b"x" * 100)
            os.symlink(target, os.path.join(tmp, "link"))
            self.assertEqual(benchlib.tree_size(tmp), 100)

    def test_wire_size_is_smaller_than_the_tree_for_compressible_input(self):
        with tempfile.TemporaryDirectory() as tmp:
            with open(os.path.join(tmp, "a"), "wb") as handle:
                handle.write(b"x" * 100000)
            self.assertLess(benchlib.wire_size(tmp, tmp),
                            benchlib.tree_size(tmp))


class MarkdownTest(unittest.TestCase):

    def test_an_empty_run_says_so(self):
        body = benchlib.render_markdown([])
        self.assertIn("No benchmark results", body)

    def test_the_table_shows_both_magnitudes_not_only_the_ratio(self):
        report = _report(
            {"install_bytes": 10 * 1024 * 1024},
            {"install_bytes": 20 * 1024 * 1024})
        body = benchlib.render_markdown([report])
        self.assertIn("10.0 MB", body)
        self.assertIn("20.0 MB", body)
        self.assertIn("2.00x", body)

    def test_the_bracket_is_disclosed_when_present(self):
        report = _report(
            {"cold_start_runs": [250]},
            {"cold_start_runs": [500], "cold_start_lower_runs": [400]})
        body = benchlib.render_markdown([report])
        self.assertIn("bracketed", body)
        self.assertIn("400 ms", body)

    def test_tally_counts_only_measured_metrics(self):
        report = _report({"install_bytes": 10}, {"install_bytes": 20})
        wins, measured = benchlib.tally([report])
        self.assertEqual((wins, measured), (1, 1))


if __name__ == "__main__":
    unittest.main(verbosity=2)
