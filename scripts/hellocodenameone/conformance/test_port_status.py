#!/usr/bin/env python3

import json
import re
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
        self.assertEqual(164, counts["tests"])
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
        self.assertEqual(
            "SurfacesRemoteViewsScreenshotTest",
            port_status.screenshot_test(self.manifest, "SurfacesRemoteViews"),
        )

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

    def test_publication_requires_complete_passing_evidence(self):
        report = port_status.read_json(
            port_status.REPO_ROOT / "docs/website/data/port_status_reports/android.json"
        )
        self.assertIsNone(port_status.publication_issue(report))

        report["suite_finished"] = False
        self.assertIsNone(
            port_status.publication_issue(report),
            "all mapped results are sufficient even when an older runner omitted the suite marker",
        )

        report["tests"]["CryptoApiTest"]["status"] = "fail"
        report["summary"]["pass"] -= 1
        report["summary"]["fail"] += 1
        self.assertEqual("1 test(s) failed", port_status.publication_issue(report))

        report["tests"]["CryptoApiTest"] = {
            "status": "fail",
            "feature": report["tests"]["CryptoApiTest"]["feature"],
            "reasons": ["screenshot-missing_actual:crypto"],
        }
        self.assertEqual(
            "CI capture environment missed evidence for 1 test(s)",
            port_status.publication_issue(report),
        )

    def test_publication_rejects_not_run_partial_performance_and_bad_summary(self):
        report = port_status.read_json(
            port_status.REPO_ROOT / "docs/website/data/port_status_reports/android.json"
        )
        report["tests"]["CryptoApiTest"]["status"] = "not-run"
        report["summary"]["pass"] -= 1
        report["summary"]["not-run"] += 1
        self.assertIn("incomplete evidence", port_status.publication_issue(report))

        report["tests"]["CryptoApiTest"]["status"] = "pass"
        report["summary"]["pass"] += 1
        report["summary"]["not-run"] -= 1
        report["performance"]["status"] = "partial"
        self.assertEqual("performance evidence is incomplete", port_status.publication_issue(report))

        report["performance"]["status"] = "complete"
        report["summary"]["pass"] -= 1
        self.assertIn("summary does not match", port_status.publication_issue(report))

    def test_javascript_declared_skips_have_public_errata(self):
        supplement = port_status.read_json(port_status.SUPPLEMENT)
        documented = {item["test"] for item in supplement["skip_reasons"]}
        expected = {
            "AccessibilityTest", "BackgroundThreadUiAccessTest",
            "Base64NativePerformanceTest", "BrowserComponentScreenshotTest",
            "BytecodeTranslatorRegressionTest", "CallDetectionAPITest",
            "ChartCombinedXYScreenshotTest", "CryptoApiTest",
            "FileSystemStorageOpenInputStreamMissingTest", "FloatingToStringTest",
            "Gpu3DModelScreenshotTest", "LightweightPickerButtonsScreenshotTest",
            "LocalNotificationOverrideTest", "SimdLargeAllocaTest", "StringApiTest",
            "TimeApiTest", "VideoIORoundTripTest", "VPNDetectionAPITest",
        }
        self.assertTrue(expected.issubset(documented))

    def test_javascript_runtime_and_bridge_skip_lists_match(self):
        expected = {
            "AccessibilityTest", "BackgroundThreadUiAccessTest",
            "Base64NativePerformanceTest", "BrowserComponentScreenshotTest",
            "BytecodeTranslatorRegressionTest", "CallDetectionAPITest",
            "ChartCombinedXYScreenshotTest", "CryptoApiTest",
            "FileSystemStorageOpenInputStreamMissingTest", "FloatingToStringTest",
            "Gpu3DModelScreenshotTest", "LightweightPickerButtonsScreenshotTest",
            "LocalNotificationOverrideTest", "SimdLargeAllocaTest", "StringApiTest",
            "TimeApiTest", "VideoIORoundTripTest", "VPNDetectionAPITest",
        }

        runner = port_status.RUNNER.read_text(encoding="utf-8")
        java_skips = set()
        for method in ("isJsSkippedNativeTest", "isJsSkippedKnownRuntimeBug"):
            start = runner.index(f"boolean {method}")
            end = runner.index("\n    }", start)
            java_skips.update(re.findall(r'"([A-Za-z0-9_]+)"\.equals\(testName\)', runner[start:end]))
        self.assertEqual(expected, java_skips)

        bridge_path = port_status.REPO_ROOT / "Ports/JavaScriptPort/src/main/webapp/port.js"
        bridge = bridge_path.read_text(encoding="utf-8")
        bridge_skips = set()
        for object_name in ("cn1ssForcedTimeoutTestClasses", "cn1ssForcedTimeoutTestNames"):
            start = bridge.index(f"const {object_name}")
            end = bridge.index("\n});", start)
            body = bridge[start:end]
            body = re.sub(r"/\*.*?\*/", "", body, flags=re.DOTALL)
            body = re.sub(r"//.*", "", body)
            for key in re.findall(r'^\s*"([^"]+)"\s*:', body, flags=re.MULTILINE):
                bridge_skips.add(key.rsplit("_", 1)[-1])
        self.assertEqual(expected, bridge_skips)


if __name__ == "__main__":
    unittest.main()
