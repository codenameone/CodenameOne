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
import platforms  # noqa: E402


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

class ComputeTest(unittest.TestCase):
    """The VM-workload comparison: same checksum or no ratio, geomean gates."""

    def test_parses_both_apps_lines_whatever_the_log_prefix(self):
        self.assertEqual(benchlib.parse_compute_line(
            "I/System.out( 123): BENCH:COMPUTE name=recursion checksum=-9 ms=12"),
            ("recursion", "-9", 12))
        self.assertEqual(benchlib.parse_compute_line(
            'INFO:CONSOLE(3)] "BENCH:COMPUTE name=intArithmetic checksum=5 ms=40"'),
            ("intArithmetic", "5", 40))
        self.assertIsNone(benchlib.parse_compute_line("BENCH:FIRSTFRAME after=3ms"))

    def test_ratio_is_flutter_over_ours_and_the_mean_is_geometric(self):
        ours = {"intArithmetic": ("1", 100), "recursion": ("2", 50)}
        theirs = {"intArithmetic": ("1", 400), "recursion": ("2", 50)}
        v = benchlib.compute_verdict(ours, theirs)
        rows = dict((r["name"], r) for r in v["workloads"])
        self.assertEqual(rows["intArithmetic"]["ratio"], 4.0)
        self.assertEqual(rows["recursion"]["ratio"], 1.0)
        self.assertEqual(v["geomean"], 2.0)
        self.assertEqual(v["compared"], 2)

    def test_a_different_checksum_is_not_a_measurement(self):
        v = benchlib.compute_verdict({"longArithmetic": ("1", 10), "recursion": ("2", 10)},
                                     {"longArithmetic": ("999", 1), "recursion": ("2", 20)})
        rows = dict((r["name"], r) for r in v["workloads"])
        self.assertEqual(rows["longArithmetic"]["status"], "checksum mismatch")
        self.assertEqual(v["compared"], 1)
        self.assertEqual(v["geomean"], 2.0, "the mismatch must not reach the mean")

    def test_every_workload_has_a_row_even_when_missing(self):
        v = benchlib.compute_verdict({}, {})
        self.assertEqual(len(v["workloads"]), len(benchlib.COMPUTE_WORKLOADS))
        self.assertNotIn("geomean", v)

    def test_a_geomean_behind_flutter_fails_the_gate(self):
        report = _report({"install_bytes": 50}, {"install_bytes": 100})
        report["compute"] = {"status": "measured",
                             "verdict": benchlib.compute_verdict({"recursion": ("1", 200)},
                                                                 {"recursion": ("1", 100)})}
        found = benchlib.check_behind(report)
        self.assertEqual([f["metric"] for f in found], [benchlib.COMPUTE_METRIC])
        self.assertIn("0.50x", benchlib.render_regressions("linux", found)[0])

    def test_the_comment_carries_the_compute_table(self):
        report = _report({"install_bytes": 50}, {"install_bytes": 100})
        report.update(platform="macos", runs=5)
        report["compute"] = {"status": "measured",
                             "verdict": benchlib.compute_verdict({"recursion": ("1", 50)},
                                                                 {"recursion": ("1", 100)})}
        body = benchlib.render_markdown([report])
        self.assertIn("| recursion | 50 ms | 100 ms | 2.00x |", body)
        self.assertIn("**Geometric mean**", body)

    def test_the_reference_names_the_side_with_the_wrong_result(self):
        # Flutter web's intArithmetic: a result, but not Java's.
        v = benchlib.compute_verdict({"intArithmetic": ("5", 300), "recursion": ("2", 10)},
                                     {"intArithmetic": ("7", 500), "recursion": ("2", 20)},
                                     {"intArithmetic": "5", "recursion": "2"})
        rows = dict((r["name"], r) for r in v["workloads"])
        self.assertEqual(rows["intArithmetic"]["failed"],
                         [{"side": "flutter", "how": benchlib.FAILED_WRONG}])
        self.assertEqual(v["failures"], [{"name": "intArithmetic", "side": "flutter",
                                          "how": benchlib.FAILED_WRONG}])
        self.assertEqual(v["geomean"], 2.0, "a failed workload is shown, not averaged")

    def test_a_side_that_produced_nothing_did_not_run(self):
        v = benchlib.compute_verdict({"longArithmetic": ("9", 40)}, {},
                                     {"longArithmetic": "9"})
        rows = dict((r["name"], r) for r in v["workloads"])
        self.assertEqual(rows["longArithmetic"]["failed"],
                         [{"side": "flutter", "how": benchlib.FAILED_NOT_RUN}])

    def test_sides_that_agree_are_compared_whatever_the_jvm_says(self):
        # A transcendental workload may differ from the host JVM's libm on both.
        v = benchlib.compute_verdict({"mathTranscendental": ("4", 50)},
                                     {"mathTranscendental": ("4", 100)},
                                     {"mathTranscendental": "3"})
        rows = dict((r["name"], r) for r in v["workloads"])
        self.assertEqual(rows["mathTranscendental"]["status"], "measured")
        self.assertEqual(v["failures"], [])

    def test_without_a_reference_a_disagreement_stays_unattributed(self):
        v = benchlib.compute_verdict({"recursion": ("1", 10)}, {"recursion": ("2", 10)})
        rows = dict((r["name"], r) for r in v["workloads"])
        self.assertEqual(rows["recursion"]["status"], "checksum mismatch")
        self.assertNotIn("failed", rows["recursion"])

    def test_our_wrong_result_fails_the_gate_and_theirs_does_not(self):
        ref = {"intArithmetic": "5", "recursion": "2"}
        report = _report({"install_bytes": 50}, {"install_bytes": 100})
        report["compute"] = {"status": "measured", "verdict": benchlib.compute_verdict(
            {"intArithmetic": ("5", 10), "recursion": ("8", 10)},
            {"intArithmetic": ("7", 20), "recursion": ("2", 20)}, ref)}
        found = benchlib.check_behind(report)
        self.assertEqual(len(found), 1)
        self.assertEqual(found[0]["failed"]["name"], "recursion")
        self.assertIn("Codename One gave a wrong result on compute workload recursion",
                      benchlib.render_regressions("javascript", found)[0])

    def test_the_table_says_who_failed(self):
        report = _report({"install_bytes": 50}, {"install_bytes": 100})
        report.update(platform="javascript", runs=5)
        report["compute"] = {"status": "measured", "verdict": benchlib.compute_verdict(
            {"intArithmetic": ("5", 300), "longArithmetic": ("9", 40)},
            {"intArithmetic": ("7", 500)}, {"intArithmetic": "5", "longArithmetic": "9"})}
        body = benchlib.render_markdown([report])
        self.assertIn("| intArithmetic | 300 ms | wrong result | Flutter: wrong result |", body)
        self.assertIn("| longArithmetic | 40 ms | did not run | Flutter: did not run |", body)

    def test_an_unmeasured_compute_run_says_why(self):
        report = _report({"install_bytes": 50}, {"install_bytes": 100})
        report.update(platform="ios", runs=5)
        report["compute"] = {"status": "not measured", "reason": "needs signed hardware."}
        body = benchlib.render_markdown([report])
        self.assertIn("Not measured: needs signed hardware._", body)
        self.assertNotIn(".._", body)


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
    """The gate's result is the exit status: a missing row fails, a judged one passes."""

    def _report(self, install=1000):
        return {"platform": "fake", "generated_at": "2026-01-01T00:00:00Z",
                "codenameone": {"install_bytes": install, "code_bytes": None},
                "flutter": {"install_bytes": 2000}}

    def test_the_gate_line_says_what_to_run(self):
        report = dict(self._report(), regressions=[{"metric": "install_bytes"}],
                      gate={"status": "armed", "fix": "flutter_baseline.py calibrate --pr 9 x"})
        self.assertIn("flutter_baseline.py calibrate --pr 9", benchlib.render_gate(report))

    def test_an_unreadable_baseline_is_rendered_as_not_judged(self):
        report = dict(self._report(), gate={"status": "refused", "reason": "two overlays"})
        self.assertIn("NOT JUDGED", benchlib.render_gate(report))

    def _main(self, rows):
        import run_bench
        import flutter_baseline
        work = tempfile.mkdtemp()
        root = os.path.join(work, "baseline")
        os.makedirs(os.path.join(root, "pr"))
        with open(os.path.join(flutter_baseline.ROOT, "policy.json")) as src, \
                open(os.path.join(root, "policy.json"), "w") as dst:
            dst.write(src.read())
        flutter_baseline.perf_baseline.write_base(root, rows)
        report = self._report()

        class Stub(object):
            id = "fake"
            label = "Fake"
            exercised = True

            def available(self):
                return True, None

            def notes(self):
                return []

            def run_compute(self, side):
                raise platforms.Unavailable("the stub runs no workloads")

        saved = (run_bench.build_adapter, run_bench.measure, run_bench.BASELINE_ROOT,
                 os.environ.get("CN1_PR_NUMBER"))
        run_bench.build_adapter = lambda args: Stub()
        run_bench.measure = lambda adapter, runs, workdir: (
            {side: {"artifact": "x", "install_bytes": report[side]["install_bytes"],
                    "cold_start_runs": [], "cold_start_lower_runs": [],
                    "idle_memory_runs": []} for side in benchlib.SIDES}, [])
        run_bench.BASELINE_ROOT = root
        os.environ["CN1_PR_NUMBER"] = "9"
        try:
            out = os.path.join(work, "result.json")
            candidate = os.path.join(work, "candidate.json")
            code = run_bench.main(["--platform", "fake", "--gate", "--json", out,
                                   "--baseline-out", candidate, "--runs", "1"])
            with open(out) as handle:
                written = json.load(handle)
            with open(candidate) as handle:
                return code, written, json.load(handle)
        finally:
            (run_bench.build_adapter, run_bench.measure, run_bench.BASELINE_ROOT, number) = saved
            if number is None:
                os.environ.pop("CN1_PR_NUMBER", None)
            else:
                os.environ["CN1_PR_NUMBER"] = number

    def test_a_missing_row_fails_the_run_and_leaves_a_candidate(self):
        code, written, candidate = self._main({})
        self.assertEqual(1, code, "an uncalibrated metric must not report success")
        self.assertEqual(["uncalibrated"], [f["verdict"] for f in written["regressions"]])
        self.assertEqual({"install_bytes": 1000}, candidate["values"])
        self.assertIn("calibrate --pr 9 baseline-fake.json", written["gate"]["fix"])

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

    def test_a_judged_run_within_tolerance_passes(self):
        code, written, _ = self._main({"fake": {"gallery": {"install_bytes": {
            "value": 1000, "runs": 1}}}})
        self.assertEqual(0, code)
        self.assertEqual("armed", written["gate"]["status"])
        self.assertEqual([], written["regressions"])

    def test_a_size_that_shrank_fails_until_it_is_rebaselined(self):
        code, written, _ = self._main({"fake": {"gallery": {"install_bytes": {
            "value": 1200, "runs": 1}}}})
        self.assertEqual(1, code)
        self.assertEqual("improved", written["regressions"][0]["verdict"])


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


class PairedRoundsTest(unittest.TestCase):
    """Start-up and memory are judged by the median of the per-round ratios."""

    # A real macOS run, five interleaved rounds. Codename One was faster in
    # rounds 1, 3 and 5; best-of called it for Flutter (356 against 468).
    CN1 = [1787, 707, 468, 584, 668]
    FLUTTER = [2512, 617, 522, 356, 769]

    def test_the_real_macos_rounds_are_a_codename_one_win(self):
        report = _report({"cold_start_runs": self.CN1}, {"cold_start_runs": self.FLUTTER})
        entry = report["verdict"]["cold_start_ms"]
        self.assertEqual("paired_median", entry["statistic"])
        self.assertEqual("codenameone", entry["winner"])
        self.assertAlmostEqual(522.0 / 468.0, entry["ratio"], places=3)
        self.assertEqual((3, 5), (entry["rounds_won"], entry["rounds"]))
        self.assertEqual([], [f for f in benchlib.check_behind(report)
                              if f["metric"] == "cold_start_ms"])
        # The columns are each side's median; the baseline still reads best-of.
        self.assertEqual((668, 617), (entry["codenameone"], entry["flutter"]))
        self.assertEqual(468, report["codenameone"]["cold_start_ms"])
        self.assertEqual(356, report["flutter"]["cold_start_ms"])

    def test_one_lucky_flutter_round_does_not_decide(self):
        report = _report({"cold_start_runs": [500, 500, 500, 500, 500]},
                         {"cold_start_runs": [600, 600, 100, 600, 600]})
        self.assertEqual("codenameone", report["verdict"]["cold_start_ms"]["winner"])

    def test_one_lucky_codename_one_round_does_not_decide(self):
        report = _report({"cold_start_runs": [600, 600, 100, 600, 600]},
                         {"cold_start_runs": [500, 500, 500, 500, 500]})
        entry = report["verdict"]["cold_start_ms"]
        self.assertEqual("flutter", entry["winner"])
        self.assertTrue(benchlib.check_behind(report))

    def test_memory_is_paired_too(self):
        report = _report({"idle_memory_runs": [33, 45, 46, 45, 46]},
                         {"idle_memory_runs": [61, 89, 91, 88, 89]})
        entry = report["verdict"]["idle_memory_bytes"]
        self.assertEqual("paired_median", entry["statistic"])
        self.assertEqual(45, entry["codenameone"])

    def test_unequal_lengths_without_round_numbers_fall_back_to_best_of(self):
        report = _report({"cold_start_runs": [500, 400, 450]},
                         {"cold_start_runs": [450, 420]})
        entry = report["verdict"]["cold_start_ms"]
        self.assertEqual("best_of", entry["statistic"])
        self.assertIn("3 and 2 rounds", entry["fallback"])
        self.assertAlmostEqual(420.0 / 400.0, entry["ratio"], places=3)
        body = benchlib.render_markdown([report])
        self.assertIn("could not be paired round by round", body)

    def test_round_numbers_pair_across_a_dropped_launch(self):
        # Flutter's round 1 failed. Positionally every later pair would be one
        # round out of step; by round number round 1 alone is dropped.
        report = _report(
            {"cold_start_runs": [500, 300, 500], "cold_start_rounds": [0, 1, 2]},
            {"cold_start_runs": [600, 600], "cold_start_rounds": [0, 2]})
        entry = report["verdict"]["cold_start_ms"]
        self.assertEqual("paired_median", entry["statistic"])
        self.assertEqual(2, entry["rounds"])
        self.assertAlmostEqual(1.2, entry["ratio"], places=3)

    def test_the_web_bracket_is_applied_per_round(self):
        # Flutter's least favourable end differs by round: the lower end in
        # rounds 0 and 2, the upper end in round 1 (a lower end is never above
        # the upper in practice, but min() must not care).
        cn1 = {"cold_start_runs": [400, 400, 400]}
        flutter = {"cold_start_runs": [800, 300, 800],
                   "cold_start_lower_runs": [500, 900, 300]}
        entry = _report(cn1, flutter)["verdict"]["cold_start_ms"]
        # Per-round Flutter figures 500, 300, 300 -> ratios 1.25, 0.75, 0.75.
        self.assertAlmostEqual(0.75, entry["ratio"], places=3)
        self.assertEqual("flutter", entry["winner"])
        self.assertEqual(300, entry["flutter"])
        self.assertEqual([500, 800], entry["flutter_bracket"])
        body = benchlib.render_markdown([dict(_report(cn1, flutter), platform="javascript")])
        self.assertIn("round by round", body)
        self.assertIn("300 ms (500 ms-800 ms)", body)

    def test_a_round_without_a_lower_end_is_dropped_not_judged_by_the_upper(self):
        report = _report(
            {"cold_start_runs": [400, 400], "cold_start_rounds": [0, 1]},
            {"cold_start_runs": [800, 800], "cold_start_rounds": [0, 1],
             "cold_start_lower_runs": [300], "cold_start_lower_rounds": [1]})
        entry = report["verdict"]["cold_start_ms"]
        self.assertEqual(1, entry["rounds"])
        self.assertAlmostEqual(0.75, entry["ratio"], places=3)

    def test_the_report_explains_the_ratio_is_not_the_medians_quotient(self):
        report = dict(_report({"cold_start_runs": self.CN1},
                              {"cold_start_runs": self.FLUTTER}), platform="macos")
        body = benchlib.render_markdown([report])
        self.assertIn("median of the per-round ratios", body)
        # 522 / 468 = 1.115: the middle round's ratio, not 617 / 668.
        self.assertIn("1.11x", body)
        self.assertIn("3 of 5 rounds", body)

    def test_the_harness_records_the_round_of_every_sample(self):
        import run_bench

        class Flaky(object):
            def artifact(self, side):
                return "x"

            def sizes(self, side, workdir):
                return {}

            def launch_and_time(self, side):
                self.calls = getattr(self, "calls", 0) + 1
                if side == "flutter" and self.calls == 4:
                    raise platforms.Unavailable("launch failed")
                return (500.0 if side == "codenameone" else 600.0), None, 1000

        sides, notes = run_bench.measure(Flaky(), 3, tempfile.gettempdir())
        self.assertEqual([0, 1, 2], sides["codenameone"]["cold_start_rounds"])
        self.assertEqual([0, 2], sides["flutter"]["cold_start_rounds"])
        self.assertEqual([0, 2], sides["flutter"]["idle_memory_rounds"])
        self.assertEqual([], sides["flutter"]["cold_start_lower_rounds"])
        entry = _report(sides["codenameone"], sides["flutter"])["verdict"]["cold_start_ms"]
        self.assertEqual(("paired_median", 2), (entry["statistic"], entry["rounds"]))

    def test_median_of_an_even_count_averages_the_middle_two(self):
        self.assertEqual(2.5, benchlib.median([4, 1, 3, 2]))
        self.assertEqual(3, benchlib.median([5, 1, 3]))
        self.assertIsNone(benchlib.median([]))


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
