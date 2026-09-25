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

import json
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



class PlatformIdsMatchTheWorkflow(unittest.TestCase):
    """The plan job's matrix and benchlib.PLATFORM_IDS name the same platforms."""

    def test_the_workflow_measures_exactly_the_known_platforms(self):
        import re
        workflow = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                "..", "..", ".github", "workflows", "flutter-bench.yml")
        with open(workflow) as handle:
            text = handle.read()
        ids = re.findall(r'\{"platform": "([a-z]+)", "runner"', text)
        self.assertTrue(ids, "the plan job's ALL list was not found")
        self.assertEqual(list(benchlib.PLATFORM_IDS), ids)


class BehindFlutterGateTest(unittest.TestCase):
    """Losing to Flutter on any measured metric fails the gate by itself."""

    def test_a_loss_on_any_metric_is_a_finding(self):
        report = _report({"install_bytes": 200, "code_bytes": 50},
                         {"install_bytes": 100, "code_bytes": 100})
        found = benchlib.check_behind(report)
        self.assertEqual([f["metric"] for f in found], ["install_bytes"])
        self.assertTrue(found[0]["behind"])
        self.assertIn("requires 1.00x", benchlib.render_regressions("macos", found)[0])

    def test_winning_everything_passes(self):
        report = _report({"install_bytes": 50, "code_bytes": 50},
                         {"install_bytes": 100, "code_bytes": 100})
        self.assertEqual(benchlib.check_behind(report), [])

    def test_a_tie_is_not_behind(self):
        report = _report({"install_bytes": 100}, {"install_bytes": 100})
        self.assertEqual(benchlib.check_behind(report), [])

    def test_an_unmeasured_metric_is_not_judged_here(self):
        report = _report({"install_bytes": 50}, {})
        self.assertEqual(benchlib.check_behind(report), [])

    def test_the_gate_line_names_what_we_lost(self):
        report = _report({"install_bytes": 200}, {"install_bytes": 100})
        report["gate"] = {"status": "armed"}
        report["behind"] = benchlib.check_behind(report)
        self.assertIn("BEHIND FLUTTER", benchlib.render_gate(report))

    def test_size_metrics_have_no_tolerance(self):
        for key in ("install_bytes", "code_bytes", "wire_bytes"):
            self.assertEqual(benchlib.DEFAULT_TOLERANCES[key], 0.0)


class PublishRefusalTest(unittest.TestCase):
    """Publishing replaces the public document, so only a complete run may."""

    def _report(self, statuses):
        return {"schema_version": 1,
                "platforms": dict((p, {"status": s}) for p, s in statuses.items())}

    def setUp(self):
        sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                        "..", "hellocodenameone", "conformance"))
        import publish_benchmark
        self.refusal = publish_benchmark.refusal

    def test_a_complete_run_publishes(self):
        statuses = dict((p, "measured") for p in benchlib.PLATFORM_IDS)
        statuses["javascript"] = "not measured"
        self.assertIsNone(self.refusal(self._report(statuses)))

    def test_a_platform_whose_leg_left_no_result_blocks_publication(self):
        statuses = dict((p, "measured") for p in benchlib.PLATFORM_IDS if p != "windows")
        reason = self.refusal(self._report(statuses))
        self.assertIsNotNone(reason)
        self.assertIn("windows", reason)

    def test_every_platform_unmeasured_blocks_publication(self):
        statuses = dict((p, "not measured") for p in benchlib.PLATFORM_IDS)
        self.assertIn("no measured platforms", self.refusal(self._report(statuses)))

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
        "codenameone": {"install_bytes": 1000},
        "tolerances": {"install_bytes": 0.02},
    }

    STARTUP_BASELINE = {
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

    SLOW_RUNNER_BASELINE = {
        "codenameone": {"cold_start_ms": 418},
        "tolerances": {"cold_start_ms": 0.25},
        "flutter_reference": {"cold_start_ms": 1505},
    }

    def test_a_slow_runner_is_discounted_by_flutters_own_slowdown(self):
        """The run that motivated it: the emulator was slow for everyone.

        Flutter (pinned) came in at 2244 ms against its 1505 ms reference, and
        Codename One at 754 ms against a 418 ms baseline; discounted by the same
        ratio it is 505 ms, inside the 25% band. Linux and Windows showed the code
        had not moved.
        """
        report = _report({"cold_start_runs": [754]}, {"cold_start_runs": [2244]})
        self.assertEqual(
            benchlib.check_regressions(report, self.SLOW_RUNNER_BASELINE), [])

    def test_a_normal_runner_gets_no_discount(self):
        """A slow Flutter cannot hide our regression on a runner that is not slow."""
        report = _report({"cold_start_runs": [754]}, {"cold_start_runs": [1200]})
        found = benchlib.check_regressions(report, self.SLOW_RUNNER_BASELINE)
        self.assertEqual(len(found), 1)
        self.assertNotIn("judged", found[0])

    def test_a_regression_beyond_the_slowdown_still_fails(self):
        report = _report({"cold_start_runs": [1500]}, {"cold_start_runs": [2244]})
        found = benchlib.check_regressions(report, self.SLOW_RUNNER_BASELINE)
        self.assertEqual(len(found), 1)
        self.assertAlmostEqual(found[0]["judged"], 1500 * 1505 / 2244.0, places=0)

    def test_without_a_reference_nothing_is_discounted(self):
        report = _report({"cold_start_runs": [754]}, {"cold_start_runs": [2244]})
        baseline = {"codenameone": {"cold_start_ms": 418}, "tolerances": {"cold_start_ms": 0.25}}
        self.assertEqual(len(benchlib.check_regressions(report, baseline)), 1)

    def test_the_candidate_records_the_flutter_reference(self):
        report = _report({"cold_start_runs": [300]}, {"cold_start_runs": [1400]})
        candidate = benchlib.baseline_candidate(report)
        self.assertEqual(candidate["flutter_reference"], {"cold_start_ms": 1400})

    def test_a_gated_metric_the_run_did_not_produce_fails_the_gate(self):
        # Sizes come from the build output, so they fill in even when the app
        # never reached its first frame; the start-up row is simply absent.
        report = _report({"install_bytes": 1000}, {"install_bytes": 5000})
        found = benchlib.check_regressions(report, self.STARTUP_BASELINE)
        self.assertEqual([f["metric"] for f in found], ["cold_start_ms"])
        self.assertTrue(found[0]["missing"])
        line = benchlib.render_regressions("linux", found)[0]
        self.assertIn("was not measured", line)

    def test_an_ungated_metric_the_run_did_not_produce_is_not_a_failure(self):
        baseline = {"codenameone": {"install_bytes": 1000, "cold_start_ms": 200},
                    "tolerances": {"install_bytes": 0.02}}
        report = _report({"install_bytes": 1000}, {"install_bytes": 5000})
        self.assertEqual(benchlib.check_regressions(report, baseline), [])

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


class GateArming(unittest.TestCase):
    """A gate with no baseline must say so where gates are read: the exit status."""

    def _report(self, install=1000):
        return {"platform": "fake", "generated_at": "2026-01-01T00:00:00Z",
                "codenameone": {"install_bytes": install, "cold_start_ms": 500.0,
                                "code_bytes": None},
                "flutter": {"install_bytes": 2000}}

    def test_candidate_holds_only_codenameone_values_with_their_bands(self):
        c = benchlib.baseline_candidate(self._report())
        self.assertEqual({"install_bytes": 1000, "cold_start_ms": 500.0}, c["codenameone"])
        self.assertEqual({"install_bytes": 0.0, "cold_start_ms": 0.25}, c["tolerances"])
        self.assertEqual([], benchlib.check_regressions(self._report(), c),
                         "a run compared against its own candidate is within tolerance")

    def test_unarmed_gate_is_rendered_as_such(self):
        report = dict(self._report(), gate={"status": "unarmed", "reason": "no committed baseline",
                                            "baseline": "scripts/flutter-bench/baselines/fake.json"})
        self.assertIn("NOT ARMED", benchlib.render_gate(report))

    def _main(self, baseline):
        import run_bench
        work = tempfile.mkdtemp()
        report = self._report()

        class Stub(object):
            id = "fake"
            label = "Fake"
            exercised = True

            def available(self):
                return True, None

            def notes(self):
                return []

        saved = (run_bench.build_adapter, run_bench.measure, run_bench.BASELINES)
        run_bench.build_adapter = lambda args: Stub()
        run_bench.measure = lambda adapter, runs, workdir: (
            {side: {"artifact": "x", "install_bytes": report[side]["install_bytes"],
                    "cold_start_runs": [500.0], "cold_start_lower_runs": [],
                    "idle_memory_runs": []} for side in benchlib.SIDES}, [])
        run_bench.BASELINES = work
        try:
            if baseline is not None:
                with open(os.path.join(work, "fake.json"), "w") as handle:
                    json.dump(baseline, handle)
            out = os.path.join(work, "result.json")
            candidate = os.path.join(work, "candidate.json")
            code = run_bench.main(["--platform", "fake", "--gate", "--json", out,
                                   "--baseline-out", candidate, "--runs", "1"])
            with open(out) as handle:
                written = json.load(handle)
            return code, written, os.path.exists(candidate)
        finally:
            run_bench.build_adapter, run_bench.measure, run_bench.BASELINES = saved

    def test_a_missing_baseline_fails_the_run_and_leaves_a_candidate(self):
        code, written, candidate_written = self._main(None)
        self.assertEqual(1, code, "an unarmed gate must not report success")
        self.assertEqual("unarmed", written["gate"]["status"])
        self.assertTrue(candidate_written)

    def test_an_unmeasured_platform_fails_the_gate(self):
        import run_bench

        class Gone(object):
            id = "fake"
            label = "Fake"
            exercised = True

            def available(self):
                return False, "no attached device or emulator"

        saved = run_bench.build_adapter
        run_bench.build_adapter = lambda args: Gone()
        try:
            out = os.path.join(tempfile.mkdtemp(), "r.json")
            self.assertEqual(1, run_bench.main(["--platform", "fake", "--gate", "--json", out]))
            self.assertEqual(0, run_bench.main(["--platform", "fake", "--json", out]),
                             "ungated, an unavailable platform is simply reported")
            with open(out) as handle:
                self.assertEqual("unavailable", json.load(handle)["status"])
        finally:
            run_bench.build_adapter = saved

    def test_an_armed_gate_within_tolerance_passes(self):
        baseline = {"codenameone": {"install_bytes": 1000}, "tolerances": {"install_bytes": 0.02}}
        code, written, _ = self._main(baseline)
        self.assertEqual(0, code)
        self.assertEqual("armed", written["gate"]["status"])


class StartupBracket(unittest.TestCase):
    """The start-up ratio comes from Flutter's end least favourable to us."""

    def _report(self):
        # The first Windows run that measured both sides: Codename One 219 ms,
        # Flutter bracketed 116 ms (FIRSTCONTENT) to 260 ms (RASTERDONE).
        return {"codenameone": {"cold_start_ms": 219.0},
                "flutter": {"cold_start_ms": 260.0, "cold_start_lower_ms": 116.0}}

    def test_the_ratio_uses_flutters_lower_end(self):
        entry = benchlib.verdict(self._report())["cold_start_ms"]
        self.assertAlmostEqual(116.0 / 219.0, entry["ratio"], places=3)
        self.assertEqual("flutter", entry["winner"],
                         "judged by the upper end this read as a Codename One win")

    def test_the_table_still_shows_the_whole_bracket(self):
        report = self._report()
        report.update(platform="windows", runs=5, verdict=benchlib.verdict(report))
        text = benchlib.render_markdown([report])
        self.assertIn("116", text)
        self.assertIn("260", text)


class FlatAssetNames(unittest.TestCase):
    """Must agree with FlutterAssets and TranscodeFlutterMojo, name for name."""

    def test_same_vectors_as_the_runtime_and_the_plugin(self):
        sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "app"))
        import stage_assets
        self.assertEqual("cn1f_assets_sstudies_sreply__card.png",
                         stage_assets.flat_name("assets/studies/reply_card.png"))
        self.assertEqual("cn1f_a___sb.png", stage_assets.flat_name("a_/b.png"))
        self.assertEqual("cn1f_a_s__b.png", stage_assets.flat_name("a/_b.png"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
