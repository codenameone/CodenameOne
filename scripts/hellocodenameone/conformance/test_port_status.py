#!/usr/bin/env python3

import json
import tempfile
import unittest
from pathlib import Path

import port_status


class PortStatusTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = port_status.read_json(port_status.DEFAULT_MANIFEST)

    def test_contract_covers_registered_tests_and_goldens(self):
        counts = port_status.validate(self.manifest)
        self.assertEqual(165, counts["tests"])
        self.assertEqual(1, counts["performance_tests"])
        self.assertGreaterEqual(counts["features"], 51)
        self.assertEqual(11, counts["ports"])
        self.assertEqual(20, counts["manual_features"])
        self.assertEqual(8, counts["deployment_platforms"])
        self.assertEqual(3, counts["browser_engines"])
        self.assertGreaterEqual(counts["goldens"], 100)
        features = {feature["id"]: feature["tests"] for feature in self.manifest["features"]}
        self.assertEqual(["ARApiTest", "MotionSensorDeviceTest"], features["ar-motion-sensors"])
        self.assertEqual(["CameraApiTest"], features["camera-access"])
        self.assertEqual(["VideoIODecodedFramesScreenshotTest"], features["video-decoding"])
        self.assertEqual(["VideoIORoundTripTest"], features["video-round-trip"])

    def test_normalize_preserves_pass_skip_and_screenshot_failure(self):
        log_text = "\n".join(
            [
                "CN1SS:INFO:suite starting test=DrawLine",
                "CN1SS:INFO:suite finished test=DrawLine",
                "CN1SS:INFO:suite starting test=CameraApiTest",
                "CN1SS:INFO:test=CameraApiTest status=SKIPPED reason=no-camera",
                "CN1SS:INFO:suite finished test=CameraApiTest",
                *[
                    f"CN1SS:PERF:benchmark id={benchmark} duration_ns=12000000 checksum=42"
                    for benchmark in self.manifest["performance_benchmarks"]
                ],
                "CN1SS:PERF:complete benchmark_version=1 checksum=42",
                "CN1SS:SUITE:FINISHED",
            ]
        )
        comparison = {
            "results": [
                {"test": "graphics-draw-line", "status": "different"}
            ]
        }
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            log_path = root / "suite.log"
            compare_path = root / "compare.json"
            output_path = root / "report.json"
            log_path.write_text(log_text, encoding="utf-8")
            compare_path.write_text(json.dumps(comparison), encoding="utf-8")

            report = port_status.normalize(
                manifest=self.manifest,
                port_id="android",
                logs=[log_path],
                comparisons=[compare_path],
                output=output_path,
                run_url="https://example.invalid/run/1",
                commit="abc123",
                generated_at="2026-07-15T00:00:00Z",
            )

        self.assertTrue(report["suite_finished"])
        self.assertEqual("fail", report["tests"]["DrawLine"]["status"])
        self.assertEqual("skip", report["tests"]["CameraApiTest"]["status"])
        self.assertIn("no-camera", report["tests"]["CameraApiTest"]["reasons"])
        self.assertEqual("not-run", report["tests"]["CryptoApiTest"]["status"])
        self.assertEqual("complete", report["performance"]["status"])
        self.assertNotIn("binary_size_bytes", report["performance"])
        self.assertNotIn("memory", report["performance"])
        self.assertEqual(12000000, report["performance"]["benchmarks"]["intArithmetic"]["duration_ns"])

    def test_error_lines_allow_messages_or_no_message(self):
        log_text = "\n".join(
            [
                "CN1SS:ERR:suite test=CryptoApiTest crypto failed",
                "CN1SS:ERR:suite test=StringApiTest",
                "CN1SS:SUITE:FINISHED",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            log_path = root / "suite.log"
            output_path = root / "report.json"
            log_path.write_text(log_text, encoding="utf-8")

            report = port_status.normalize(
                manifest=self.manifest,
                port_id="android",
                logs=[log_path],
                comparisons=[],
                output=output_path,
                run_url="https://example.invalid/run/2",
                commit="def456",
                generated_at="2026-07-15T00:00:00Z",
            )

        self.assertEqual("fail", report["tests"]["CryptoApiTest"]["status"])
        self.assertEqual(["crypto failed"], report["tests"]["CryptoApiTest"]["reasons"])
        self.assertEqual("fail", report["tests"]["StringApiTest"]["status"])
        self.assertEqual(["suite-error"], report["tests"]["StringApiTest"]["reasons"])


if __name__ == "__main__":
    unittest.main()
