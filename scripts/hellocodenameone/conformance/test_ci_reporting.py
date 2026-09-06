"""Exercise the shell gate with real normalized evidence, without a native runner."""
import json
import fnmatch
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import re


ROOT = Path(__file__).resolve().parents[3]


class ReportingTests(unittest.TestCase):
    def test_native_x64_artifacts_cannot_replace_shipping_evidence(self):
        workflow = (ROOT / ".github/workflows/parparvm-tests-windows.yml").read_text()
        x64 = workflow.split("  windows-port-screenshot-comment-x64:", 1)[1].split(
            "  windows-port-screenshot-comment-arm64:", 1)[0]
        artifacts = re.findall(r"uses: \./\.github/actions/upload-artifact-with-retry\s+with:\s+name: (\S+)", x64)
        self.assertIn("windows-native-x64-diagnostics", artifacts)
        self.assertFalse(any(fnmatch.fnmatch(name, "port-status-*") for name in artifacts))
        self.assertNotIn("publish_port_status.sh", x64)
        manifest = json.loads((ROOT / "docs/website/data/port_status.json").read_text())
        port = next(port for port in manifest["ports"] if port["id"] == "windows-x64")
        self.assertEqual("windows-cross-build-run.yml", port["workflow"])

    def run_gate(self, compare, artifacts, strict=True):
        env = dict(os.environ, CN1SS_PORT_ID="windows-x64",
                   CN1SS_FAIL_ON_TEST_PROBLEMS="1" if strict else "0")
        return subprocess.run(["bash", "-c",
                               'source "$1/scripts/lib/cn1ss.sh"; cn1ss_generate_port_status "$2" "$3"',
                               "gate", str(ROOT), str(compare), str(artifacts)],
                              env=env, text=True, capture_output=True)

    def test_empty_capture_writes_missing_test_evidence_and_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            compare = path / "compare.json"
            compare.write_text('{"results": []}')
            result = self.run_gate(compare, path)
            self.assertEqual(19, result.returncode, result.stdout + result.stderr)
            report = json.loads((path / "port-status-windows-x64.json").read_text())
            self.assertGreater(report["summary"]["not-run"], 0)
            self.assertFalse(report["suite_finished"])
            self.assertIn("Normalized report contains failing or missing tests", result.stdout + result.stderr)

    def test_corrupt_comparison_is_a_generation_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            compare = path / "compare.json"
            compare.write_text('not json')
            result = self.run_gate(compare, path)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("Failed to generate normalized port status", result.stdout + result.stderr)
            self.assertFalse((path / "port-status-windows-x64.json").exists())

    def test_screenshot_defect_still_fails_and_is_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            compare = path / "compare.json"
            compare.write_text(json.dumps({"results": [{"test": "VideoIODecodedFrames", "status": "different"}]}))
            result = self.run_gate(compare, path)
            self.assertNotEqual(0, result.returncode)
            report = json.loads((path / "port-status-windows-x64.json").read_text())
            self.assertEqual("fail", report["tests"]["VideoIODecodedFramesScreenshotTest"]["status"])


if __name__ == "__main__":
    unittest.main()
