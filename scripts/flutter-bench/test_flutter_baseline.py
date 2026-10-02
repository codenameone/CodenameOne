#!/usr/bin/env python3
"""Tests for the Flutter benchmark's regression baselines (flutter_baseline.py).

    python3 -m unittest test_flutter_baseline      (from scripts/flutter-bench)

The overlay rules themselves are vm/selfhost/perf_baseline.py's and are tested there
(test_perf_gate). These pin what this layout adds: which statistic is judged, which row
judges it, the sizes' fine tolerance step, the ungated policy, the retired files, and the
migration.
"""

import json
import os
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import flutter_baseline as fb  # noqa: E402

pb = fb.perf_baseline

POLICY = {
    "tolerance": {"code_bytes": 0.0, "install_bytes": 0.0, "wire_bytes": 0.0,
                  "cold_start_ratio": 0.25, "idle_memory_ratio": 0.15},
    "floor": {m: 0.0 for m in fb.METRIC_IDS},
    "ungated": {"macos": {"cold_start_ratio": "too noisy on the hosted runner"}},
}


def row(value, runs=1, tolerance=None):
    r = {"value": value, "runs": runs}
    if tolerance:
        r["tolerance"] = {"value": tolerance}
    return r


def rows(**per_key):
    """rows(android={"code_bytes": row(...)}) -> the {key: {app: {metric: row}}} tree."""
    return {key.replace("__", "@"): {fb.APP: metrics} for key, metrics in per_key.items()}


class Tree(object):
    def __init__(self, base=None, overlays=None, policy=None):
        self._tmp = tempfile.mkdtemp()
        self.root = Path(self._tmp) / "baseline"
        (self.root / "pr").mkdir(parents=True)
        (self.root / "policy.json").write_text(json.dumps(policy or POLICY))
        pb.write_base(self.root, base or {})
        for number, overlay in (overlays or {}).items():
            (self.root / "pr" / ("%d.json" % number)).write_text(json.dumps(overlay))

    def overlay(self, number):
        path = self.root / "pr" / ("%d.json" % number)
        return json.loads(path.read_text()) if path.exists() else None

    def run_file(self, name, report):
        path = Path(self._tmp) / name
        path.write_text(json.dumps(report))
        return str(path)

    def close(self):
        shutil.rmtree(self._tmp)


def result(platform="android", cpu=None, cn1_start=None, fl_start=None, cn1_mem=None,
           fl_mem=None, **sizes):
    """A run's result JSON, as run_bench.py writes it."""
    def side(start, mem, extra):
        out = dict(extra)
        if start:
            out["cold_start_runs"] = list(start)
            out["cold_start_rounds"] = list(range(len(start)))
        if mem:
            out["idle_memory_runs"] = list(mem)
            out["idle_memory_rounds"] = list(range(len(mem)))
        return out
    report = {"platform": platform, "status": "measured",
              "codenameone": side(cn1_start, cn1_mem, sizes),
              "flutter": side(fl_start, fl_mem, {})}
    if cpu:
        report["cpu"] = cpu
    return report


class MeasurementTest(unittest.TestCase):
    def test_a_ratio_is_the_median_of_the_per_round_ratios_ours_over_theirs(self):
        # Rounds 1/4, 3/4 and 2/4: median 2/4. The quotient of the medians would agree
        # here; the point is the direction (ours on top, lower is better).
        report = result(cn1_start=[100, 300, 200], fl_start=[400, 400, 400])
        self.assertAlmostEqual(0.5, fb.ratio(report, "cold_start_ratio"))

    def test_a_slow_runner_slows_both_halves_and_cancels(self):
        fast = result(cn1_start=[300, 310, 305], fl_start=[1000, 1000, 1000])
        slow = result(cn1_start=[600, 620, 610], fl_start=[2000, 2000, 2000])
        self.assertAlmostEqual(fb.ratio(fast, "cold_start_ratio"),
                               fb.ratio(slow, "cold_start_ratio"))

    def test_the_web_bracket_uses_flutters_end_least_favourable_to_us(self):
        report = result(cn1_start=[200], fl_start=[400])
        report["flutter"]["cold_start_lower_runs"] = [100]
        report["flutter"]["cold_start_lower_rounds"] = [0]
        self.assertAlmostEqual(2.0, fb.ratio(report, "cold_start_ratio"))

    def test_sizes_are_absolute_and_unmeasured_metrics_are_absent(self):
        values = fb.measurements(result(code_bytes=10, install_bytes=20))
        self.assertEqual({"code_bytes": 10, "install_bytes": 20}, values)

    def test_the_candidate_round_trips_through_measurements(self):
        report = result(cpu="AMD EPYC 7763 64-Core Processor", code_bytes=10,
                        cn1_mem=[50, 50, 50], fl_mem=[100, 100, 100])
        candidate = fb.candidate(report)
        self.assertEqual({"code_bytes": 10, "idle_memory_ratio": 0.5}, fb.measurements(candidate))
        self.assertEqual(report["cpu"], candidate["cpu"])


class JudgeTest(unittest.TestCase):
    def judge(self, report, base, policy=None):
        tree = Tree(base, policy=policy)
        try:
            return fb.judge(report, fb.load(tree.root))
        finally:
            tree.close()

    def test_within_tolerance_passes(self):
        base = rows(android={"code_bytes": row(1000), "cold_start_ratio": row(0.5)})
        report = result(code_bytes=1000, cn1_start=[110], fl_start=[200])
        self.assertEqual([], self.judge(report, base))

    def test_a_size_moved_either_way_fails(self):
        base = rows(android={"code_bytes": row(1000)})
        grown = self.judge(result(code_bytes=1001), base)
        shrunk = self.judge(result(code_bytes=999), base)
        self.assertEqual(["regression"], [f["verdict"] for f in grown])
        self.assertEqual(["improved"], [f["verdict"] for f in shrunk])
        self.assertIn("IMPROVED", fb.describe(shrunk[0]))

    def test_a_row_tolerance_absorbs_small_growth(self):
        base = rows(android={"code_bytes": row(1000000, tolerance=0.005)})
        self.assertEqual([], self.judge(result(code_bytes=1004000), base))
        self.assertEqual(1, len(self.judge(result(code_bytes=1006000), base)))

    def test_a_ratio_regression_beyond_tolerance_fails(self):
        base = rows(android={"cold_start_ratio": row(0.3)})
        found = self.judge(result(cn1_start=[400], fl_start=[1000]), base)
        self.assertEqual("regression", found[0]["verdict"])
        self.assertAlmostEqual(33.33, found[0]["moved_by"], places=1)

    def test_a_measured_metric_without_a_row_is_uncalibrated(self):
        found = self.judge(result(code_bytes=5), {})
        self.assertEqual([("code_bytes", "uncalibrated", "android")],
                         [(f["metric"], f["verdict"], f["key"]) for f in found])

    def test_a_gated_metric_the_run_did_not_produce_fails(self):
        # Sizes come from the build output, so they fill in even when the app never
        # reached its first frame; the start-up ratio is simply absent.
        base = rows(android={"code_bytes": row(5), "cold_start_ratio": row(0.3)})
        found = self.judge(result(code_bytes=5), base)
        self.assertEqual([("cold_start_ratio", "missing")],
                         [(f["metric"], f["verdict"]) for f in found])
        self.assertIn("was not measured", fb.describe(found[0]))

    def test_an_ungated_metric_is_recorded_not_judged(self):
        found = self.judge(result(platform="macos", cn1_start=[900], fl_start=[100]), {})
        self.assertEqual([], found)

    def test_flutters_own_numbers_only_enter_as_the_denominator(self):
        # A Flutter SDK that grows its build cannot fail OUR size rows.
        base = rows(android={"code_bytes": row(1000)})
        report = result(code_bytes=1000)
        report["flutter"]["code_bytes"] = 99999999
        self.assertEqual([], self.judge(report, base))


class RowKeyTest(unittest.TestCase):
    CPU = "AMD EPYC 7763 64-Core Processor"
    MODEL = "android@amd-epyc-7763-64-core-processor"

    def test_one_plain_row_judges_every_cpu_until_models_have_rows(self):
        tree = rows(android={"cold_start_ratio": row(0.3)})
        self.assertEqual("android", fb.row_key(tree, "android", self.CPU, "cold_start_ratio"))

    def test_a_model_row_judges_its_model_and_an_unseen_model_is_uncalibrated(self):
        tree = rows(android={"cold_start_ratio": row(0.3)},
                    android__amd_x={"cold_start_ratio": row(0.4)})
        tree = {k.replace("_", "-"): v for k, v in tree.items()}
        self.assertEqual("android@amd-x", fb.row_key(tree, "android", "AMD X", "cold_start_ratio"))
        self.assertIsNone(fb.row_key(tree, "android", self.CPU, "cold_start_ratio"))

    def test_a_size_ignores_the_cpu(self):
        tree = rows(android={"code_bytes": row(1)})
        self.assertEqual("android", fb.row_key(tree, "android", self.CPU, "code_bytes"))

    def test_a_size_under_a_cpu_key_is_refused(self):
        tree = Tree({"android@x": {fb.APP: {"code_bytes": row(1)}}})
        try:
            with self.assertRaises(fb.BaselineError):
                fb.load(tree.root)
        finally:
            tree.close()

    def test_an_unknown_metric_is_refused(self):
        tree = Tree(rows(android={"cold_start_ms": row(1)}))
        try:
            with self.assertRaisesRegex(fb.BaselineError, "unknown metric"):
                fb.load(tree.root)
        finally:
            tree.close()

    def test_an_ungated_entry_needs_a_reason(self):
        policy = dict(POLICY, ungated={"macos": {"cold_start_ratio": " "}})
        tree = Tree({}, policy=policy)
        try:
            with self.assertRaisesRegex(fb.BaselineError, "reason"):
                fb.load(tree.root)
        finally:
            tree.close()


class CalibrateTest(unittest.TestCase):
    def calibrate(self, runs, base=None, overlays=None, reason=None, per_cpu=False,
                  everything=False):
        tree = Tree(base, overlays)
        try:
            files = [tree.run_file("r%d.json" % i, r) for i, r in enumerate(runs)]
            fb.calibrate(tree.root, 7, files, reason, everything, None, per_cpu)
            return tree.overlay(7), fb.load(tree.root)["platforms"]
        finally:
            tree.close()

    def test_new_ratio_rows_are_calibrated_with_a_spread_learned_both_ways(self):
        runs = [result(cn1_start=[c], fl_start=[1000]) for c in (250, 300, 350, 300, 300)]
        overlay, resolved = self.calibrate(runs)
        new = overlay["calibrate"]["android"][fb.APP]["cold_start_ratio"]
        self.assertEqual(0.3, new["value"])
        self.assertEqual(5, new["runs"])
        # (0.35 / 0.3 - 1) x 1.5 = 0.25, the policy's own, so not repeated in the row.
        self.assertNotIn("tolerance", new)
        self.assertEqual(new, resolved["android"][fb.APP]["cold_start_ratio"])

    def test_a_moved_size_is_rebaselined_from_its_old_value_and_needs_a_reason(self):
        base = rows(android={"code_bytes": row(1000)})
        with self.assertRaises(SystemExit):
            self.calibrate([result(code_bytes=1200)], base)
        overlay, _ = self.calibrate([result(code_bytes=1200)], base, reason="grew")
        moved = overlay["rebaseline"]["android"][fb.APP]["code_bytes"]
        self.assertEqual({"value": 1000}, moved["from"])
        self.assertEqual(1200, moved["value"])
        self.assertEqual("grew", moved["reason"])

    def test_size_jitter_learns_a_fine_tolerance_not_a_five_percent_one(self):
        base = rows(android={"install_bytes": row(1000000)})
        overlay, _ = self.calibrate([result(install_bytes=81949941),
                                     result(install_bytes=81949945)], base, reason="r")
        moved = overlay["rebaseline"]["android"][fb.APP]["install_bytes"]
        self.assertEqual({"value": 0.001}, moved["tolerance"])

    def test_a_row_inside_its_tolerance_is_left_alone(self):
        base = rows(android={"code_bytes": row(1000000, tolerance=0.005)})
        tree = Tree(base)
        try:
            path = tree.run_file("r.json", result(code_bytes=1001000))
            self.assertIsNone(fb.calibrate(tree.root, 7, [path]))
            self.assertIsNone(tree.overlay(7))
        finally:
            tree.close()

    def test_per_cpu_calibrates_a_model_row_beside_the_plain_one(self):
        base = rows(android={"cold_start_ratio": row(0.3)})
        runs = [result(cpu="AMD EPYC 7763 64-Core Processor", cn1_start=[400], fl_start=[1000])]
        overlay, resolved = self.calibrate(runs, base, per_cpu=True)
        self.assertIn("android@amd-epyc-7763-64-core-processor", overlay["calibrate"])
        self.assertEqual(0.3, resolved["android"][fb.APP]["cold_start_ratio"]["value"])

    def test_an_ungated_metric_is_not_calibrated(self):
        overlay, _ = self.calibrate([result(platform="macos", code_bytes=5, cn1_start=[1],
                                            fl_start=[2])])
        self.assertEqual(["code_bytes"], list(overlay["calibrate"]["macos"][fb.APP]))

    def test_the_candidate_file_calibrates_like_the_result(self):
        report = result(code_bytes=7, cn1_mem=[40], fl_mem=[80])
        a, _ = self.calibrate([report])
        b, _ = self.calibrate([fb.candidate(report)])
        self.assertEqual(a, b)


class OverlayRulesTest(unittest.TestCase):
    """The shared rules, through this layout."""

    def test_two_pull_requests_moving_one_size_conflict_by_name(self):
        base = rows(android={"code_bytes": row(1000)})
        def moved(number, value):
            return {"pr": number, "rebaseline": {"android": {fb.APP: {"code_bytes": dict(
                row(value), **{"from": {"value": 1000}, "reason": "r"})}}}}
        tree = Tree(base, {11: moved(11, 1100), 12: moved(12, 1200)})
        try:
            with self.assertRaisesRegex(fb.BaselineError, "pr/11.json, pr/12.json"):
                fb.load(tree.root)
        finally:
            tree.close()

    def test_fold_moves_overlays_into_base_and_changes_nothing(self):
        overlay = {"pr": 3, "calibrate": {"linux": {fb.APP: {"idle_memory_ratio": row(0.4, 6)}}}}
        tree = Tree(rows(linux={"code_bytes": row(9)}), {3: overlay})
        try:
            before = fb.load(tree.root)["platforms"]
            self.assertEqual([3], pb.fold(tree.root, fb.FLUTTER))
            self.assertEqual(before, fb.load(tree.root)["platforms"])
            self.assertIsNone(tree.overlay(3))
        finally:
            tree.close()

    def test_the_checked_in_baselines_resolve(self):
        data = fb.load()
        self.assertIn("android", data["platforms"])


class CheckTest(unittest.TestCase):
    def check(self, changed, migrating=False, legacy=False, number=31):
        saved = pb.changed_files, pb.exists_at, pb.legacy_touched
        seen = {}

        def touched(base_ref, path=None):
            seen["path"] = path
            return legacy
        pb.changed_files = lambda base_ref, root=None: changed
        pb.exists_at = lambda ref, path, root=None: (
            not migrating if path == "policy.json" else False)
        pb.legacy_touched = touched
        try:
            return fb.check(fb.ROOT, "base-sha", number), seen
        finally:
            pb.changed_files, pb.exists_at, pb.legacy_touched = saved

    def test_base_is_the_folds_alone(self):
        problems, _ = self.check(["base/android.json"])
        self.assertEqual(1, len(problems))
        self.assertIn("flutter_baseline.py calibrate --pr N", problems[0])

    def test_a_pull_request_writes_its_own_overlay_and_the_policy(self):
        self.assertEqual([], self.check(["pr/31.json", "policy.json"])[0])

    def test_another_pull_requests_overlay_is_off_limits(self):
        self.assertIn("pr/30.json", self.check(["pr/30.json"])[0][0])

    def test_the_retired_files_are_refused_and_it_asks_about_the_right_path(self):
        problems, seen = self.check([], legacy=True)
        self.assertEqual(fb.LEGACY_DIR, seen["path"])
        self.assertIn("retired", problems[0])

    def test_the_migration_may_create_base(self):
        self.assertEqual([], self.check(["base/android.json"], migrating=True, legacy=True)[0])


class LegacyTest(unittest.TestCase):
    ORIGINAL = {
        "android": {"codenameone": {"code_bytes": 4720336, "install_bytes": 91589516,
                                    "cold_start_ms": 418.0},
                    "tolerances": {"code_bytes": 0.0, "install_bytes": 0.0,
                                   "cold_start_ms": 0.25}},
        "macos": {"codenameone": {"code_bytes": 7, "cold_start_ms": 465.0},
                  "tolerances": {"code_bytes": 0.0}},
    }

    def branch(self):
        legacy = json.loads(json.dumps(self.ORIGINAL))
        legacy["android"]["codenameone"]["code_bytes"] = 4773504
        legacy["android"]["tolerances"]["code_bytes"] = 0.005
        return legacy

    def test_sizes_carry_over_exactly_and_gated_absolutes_are_reported(self):
        new, gated, recorded = fb.legacy_rows(self.branch(), POLICY)
        self.assertEqual(row(4773504, tolerance=0.005), new["android"][fb.APP]["code_bytes"])
        self.assertEqual(row(91589516), new["android"][fb.APP]["install_bytes"])
        self.assertEqual({"android": ["cold_start_ratio"]}, gated)
        self.assertEqual({"macos": ["cold_start_ratio"]}, recorded)

    def test_a_branchs_edit_becomes_its_rebaseline_and_resolves_to_the_branch(self):
        base, _, _ = fb.legacy_rows(self.ORIGINAL, POLICY)
        tree = Tree(base)
        try:
            fb.import_legacy(tree.root, 7, self.branch(), self.ORIGINAL, "features")
            moved = tree.overlay(7)["rebaseline"]["android"][fb.APP]["code_bytes"]
            self.assertEqual({"value": 4720336}, moved["from"])
            branch_rows, _, _ = fb.legacy_rows(self.branch(), POLICY)
            self.assertEqual(branch_rows, fb.load(tree.root)["platforms"])
        finally:
            tree.close()

    def test_an_unchanged_branch_writes_nothing(self):
        base, _, _ = fb.legacy_rows(self.ORIGINAL, POLICY)
        tree = Tree(base)
        try:
            self.assertIsNone(fb.import_legacy(tree.root, 7, self.ORIGINAL, self.ORIGINAL))
        finally:
            tree.close()

    def test_the_policy_ungates_exactly_what_the_retired_files_did_not_gate(self):
        """The migration kept the gating decisions: every absolute start-up or memory
        figure the old files recorded without a tolerance is ungated, and nothing else."""
        policy = json.loads((fb.ROOT / "policy.json").read_text())
        ungated = {(p, m) for p, ms in policy.get("ungated", {}).items() for m in ms}
        self.assertEqual({("macos", "cold_start_ratio")}, ungated)


if __name__ == "__main__":
    unittest.main()
