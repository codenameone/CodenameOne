#!/usr/bin/env python3
"""Exercise the compiled fidelity gate against complete and shrinking capture sets."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class FidelityGateTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.classes = tempfile.TemporaryDirectory(prefix="cn1-fidelity-gate-")
        cls.addClassCleanup(cls.classes.cleanup)
        java_home = os.environ.get("JAVA_HOME")
        cls.java = str(Path(java_home) / "bin/java") if java_home else "java"
        javac = str(Path(java_home) / "bin/javac") if java_home else "javac"
        subprocess.run([javac, "-d", cls.classes.name,
                        str(ROOT / "scripts/common/java/FidelityGate.java")], check=True)

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="cn1-fidelity-data-")
        self.addCleanup(self.directory.cleanup)
        path = Path(self.directory.name)
        self.baseline = path / "baseline.json"
        self.comparison = path / "compare.json"
        self.baseline.write_text(json.dumps({"pairs": {"kept": 90, "removed": 80}}))

    def gate(self, scores, update=False, geometry=None):
        self.comparison.write_text(json.dumps({"results": [
            {"test": key, "status": "compared", "details": {"fidelity_percent": value, **({"geometry": geometry} if geometry is not None else {})}}
            for key, value in scores.items()]}))
        command = [self.java, "-cp", self.classes.name, "FidelityGate",
                   "--compare-json", str(self.comparison), "--baseline", str(self.baseline)]
        if update:
            command += ["--update-baseline", str(self.baseline)]
        return subprocess.run(command, capture_output=True, text=True)

    def test_complete_set_passes_and_score_regressions_still_fail(self):
        self.assertEqual(0, self.gate({"kept": 90, "removed": 80}).returncode)
        self.assertEqual(20, self.gate({"kept": 70, "removed": 80}).returncode)

    def test_missing_pair_fails_until_its_baseline_is_explicitly_removed(self):
        result = self.gate({"kept": 90})
        self.assertEqual(20, result.returncode)
        self.assertIn("removed (baseline pair absent", result.stderr)
        self.assertEqual(0, self.gate({"kept": 90}, update=True, geometry={"center_offset": 0, "width_ratio": 1, "height_ratio": 1}).returncode)
        self.assertIn("removed", json.loads(self.baseline.read_text())["pairs"],
                      "a partial baseline update must not silently delete coverage")
        self.assertEqual(20, self.gate({"kept": 90}).returncode)
        self.baseline.write_text(json.dumps({"pairs": {"kept": 90}}))
        self.assertEqual(0, self.gate({"kept": 90}).returncode)

    def test_existing_geometry_cannot_disappear_or_be_incomplete(self):
        geometry = {"center_offset": 0, "width_ratio": 1, "height_ratio": 1}
        self.baseline.write_text(json.dumps({"pairs": {"kept": 90}, "geometry": {"kept": geometry}}))
        self.assertEqual(0, self.gate({"kept": 90}, geometry=geometry).returncode)
        for missing in (None, {}, {"empty": True}, {"center_offset": 0, "width_ratio": 1}):
            for update in (False, True):
                with self.subTest(geometry=missing, update=update):
                    result = self.gate({"kept": 90}, update=update, geometry=missing)
                    self.assertEqual(20, result.returncode)
                    self.assertIn("missing or incomplete geometry", result.stderr)
        # A partial refresh can omit a pair entirely without deleting its geometry.
        self.assertEqual(0, self.gate({}, update=True).returncode)
        self.assertEqual(geometry, json.loads(self.baseline.read_text())["geometry"]["kept"])

    def test_baseline_updates_require_geometry_for_new_and_legacy_pairs(self):
        geometry = {"center_offset": 0, "width_ratio": 1, "height_ratio": 1}
        for name in ("new", "kept"):
            for missing in (None, {}, {"empty": True}, {"center_offset": 0}):
                with self.subTest(pair=name, geometry=missing):
                    before = self.baseline.read_text()
                    result = self.gate({name: 90}, update=True, geometry=missing)
                    self.assertEqual(20, result.returncode)
                    self.assertIn("missing or incomplete geometry", result.stderr)
                    self.assertEqual(before, self.baseline.read_text())
            self.assertEqual(0, self.gate({name: 90}, update=True, geometry=geometry).returncode)
            self.assertEqual(geometry, json.loads(self.baseline.read_text())["geometry"][name])

    def test_empty_capture_set_cannot_pass_an_existing_baseline(self):
        result = self.gate({})
        self.assertEqual(20, result.returncode)
        self.assertIn("kept (baseline pair absent", result.stderr)
        self.assertIn("removed (baseline pair absent", result.stderr)


if __name__ == "__main__":
    unittest.main()
