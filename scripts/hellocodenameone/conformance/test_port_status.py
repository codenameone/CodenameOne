#!/usr/bin/env python3

import json
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from unittest.mock import patch

import port_status


class PortStatusTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = port_status.read_json(port_status.DEFAULT_MANIFEST)

    def test_contract_covers_registered_tests_and_goldens(self):
        counts = port_status.validate(self.manifest)
        self.assertEqual(170, counts["tests"])
        self.assertEqual(1, counts["performance_tests"])
        self.assertGreaterEqual(counts["features"], 54)
        self.assertEqual(11, counts["ports"])
        self.assertEqual(20, counts["manual_features"])
        self.assertEqual(8, counts["deployment_platforms"])
        self.assertEqual(3, counts["browser_engines"])
        self.assertGreaterEqual(counts["goldens"], 100)
        features = {feature["id"]: feature["tests"] for feature in self.manifest["features"]}
        self.assertEqual(["ARApiTest", "MotionSensorDeviceTest"], features["ar-motion-sensors"])
        self.assertEqual(["CameraApiTest"], features["camera-access"])
        self.assertEqual(["VisionOnDeviceApiTest"], features["on-device-vision"])
        self.assertEqual(["LanguageOnDeviceApiTest"], features["on-device-language"])
        self.assertEqual(["InferenceOnDeviceApiTest"], features["on-device-inference"])
        self.assertEqual(["CalendarApiTest"], features["calendar-integration"])
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

    def test_performance_skips_are_complete_and_preserve_reasons(self):
        expected = self.manifest["performance_benchmarks"]
        skipped = {"objectAllocation", "hashMapChurn", "stringBuilding"}
        log_text = "\n".join(
            [
                *[
                    (
                        f"CN1SS:PERF:skipped id={benchmark} "
                        "reason=ios-simulator-gc-footprint"
                        if benchmark in skipped
                        else (
                            f"CN1SS:PERF:benchmark id={benchmark} "
                            "duration_ns=12000000 checksum=42"
                        )
                    )
                    for benchmark in expected
                ],
                "CN1SS:PERF:complete benchmark_version=1 checksum=42",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp:
            log_path = Path(tmp) / "suite.log"
            log_path.write_text(log_text, encoding="utf-8")
            performance = port_status.parse_performance([log_path], expected, None)

        self.assertEqual("complete", performance["status"])
        self.assertEqual([], performance["missing"])
        self.assertEqual(
            {
                benchmark: "ios-simulator-gc-footprint"
                for benchmark in expected
                if benchmark in skipped
            },
            performance["skipped"],
        )
        self.assertNotIn("objectAllocation", performance["benchmarks"])

    def test_strict_report_errors_reject_failures_missing_tests_and_incomplete_suite(self):
        report = {
            "suite_finished": False,
            "summary": {"pass": 10, "fail": 2, "skip": 1, "not-run": 3},
        }
        self.assertEqual(
            [
                "suite did not emit its completion marker",
                "2 test(s) failed",
                "3 test(s) did not run",
            ],
            port_status.strict_report_errors(report),
        )

    def test_strict_report_errors_allows_complete_report_with_skips(self):
        report = {
            "suite_finished": True,
            "summary": {"pass": 10, "fail": 0, "skip": 1, "not-run": 0},
        }
        self.assertEqual([], port_status.strict_report_errors(report))

    def test_strict_report_errors_rejects_malformed_summary_counts(self):
        report = {
            "suite_finished": True,
            "summary": {"pass": 0, "fail": None, "skip": 0, "not-run": 0},
        }
        with self.assertRaisesRegex(
            port_status.ContractError,
            "Expected report summary 'fail' to be a non-negative integer",
        ):
            port_status.strict_report_errors(report)

    def test_strict_report_errors_requires_complete_summary(self):
        with self.assertRaisesRegex(
            port_status.ContractError,
            "Expected report summary to be an object",
        ):
            port_status.strict_report_errors({"suite_finished": True})

        report = {
            "suite_finished": True,
            "summary": {"pass": 10, "fail": 0, "not-run": 0},
        }
        with self.assertRaisesRegex(
            port_status.ContractError,
            "Report summary is missing required count 'skip'",
        ):
            port_status.strict_report_errors(report)

    def test_cli_strict_gate_writes_report_before_returning_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            output_path = Path(tmp) / "report.json"
            argv = [
                "port_status.py",
                "normalize",
                "--port",
                "android",
                "--output",
                str(output_path),
                "--generated-at",
                "2026-07-24T00:00:00Z",
                "--fail-on-test-problems",
            ]
            with patch.object(port_status.sys, "argv", argv):
                exit_code = port_status.main()

            self.assertEqual(port_status.STRICT_GATE_FAILED, exit_code)
            self.assertTrue(output_path.is_file())
            report = json.loads(output_path.read_text(encoding="utf-8"))
            self.assertFalse(report["suite_finished"])
            self.assertGreater(report["summary"]["not-run"], 0)

    def test_validate_rejects_inconsistent_stored_report_summary(self):
        original_directory = self.manifest["report_directory"]
        with tempfile.TemporaryDirectory(dir=port_status.REPO_ROOT) as tmp:
            report_root = Path(tmp)
            for port in self.manifest["ports"]:
                source = port_status.REPO_ROOT / original_directory / (
                    port["id"] + ".json"
                )
                (report_root / source.name).write_text(
                    source.read_text(encoding="utf-8"), encoding="utf-8"
                )
            android_path = report_root / "android.json"
            android = port_status.read_json(android_path)
            android["summary"]["not-run"] += 1
            android_path.write_text(
                json.dumps(android, indent=2, sort_keys=True) + "\n",
                encoding="utf-8",
            )
            manifest = dict(self.manifest)
            manifest["report_directory"] = str(
                report_root.relative_to(port_status.REPO_ROOT)
            )
            with self.assertRaisesRegex(
                port_status.ContractError,
                "summary does not match its test results",
            ):
                port_status.validate(manifest)

    def publishable_report(self, port_id, **overrides):
        mapped = port_status.test_to_feature(self.manifest)
        tests = {
            test: {"status": "pass", "feature": feature}
            for test, feature in mapped.items()
        }
        report = {
            "schema_version": self.manifest["schema_version"],
            "port": port_id,
            "generated_at": "2026-07-30T09:24:29Z",
            "suite_finished": True,
            "summary": {"pass": len(tests), "fail": 0, "skip": 0, "not-run": 0},
            "tests": tests,
            "performance": {
                "status": "complete",
                "benchmark_version": 1,
                "missing": [],
                "skipped": {},
                "benchmarks": {
                    benchmark: {"duration_ns": 12000000, "checksum": "42"}
                    for benchmark in self.manifest["performance_benchmarks"]
                },
            },
        }
        report.update(overrides)
        return report

    def test_publishable_accepts_a_report_that_skips_workloads(self):
        # The shape every iOS, tvOS, and watchOS run produces: the simulator
        # skips the three GC-footprint workloads and measures the other seven.
        report = self.publishable_report("ios-gl")
        for benchmark in ("objectAllocation", "hashMapChurn", "stringBuilding"):
            del report["performance"]["benchmarks"][benchmark]
            report["performance"]["skipped"][benchmark] = "ios-simulator-gc-footprint"

        self.assertEqual(([], []), port_status.publishable_report_problems(
            self.manifest, "ios-gl", report
        ))

    def test_publishable_accepts_a_documented_test_skip(self):
        report = self.publishable_report("android")
        report["tests"]["CameraApiTest"]["status"] = "skip"
        report["summary"]["pass"] -= 1
        report["summary"]["skip"] += 1

        self.assertEqual(([], []), port_status.publishable_report_problems(
            self.manifest, "android", report
        ))

    def test_publishable_rejects_a_timestamp_from_the_future(self):
        # A skewed producer clock poisons the data branch rather than merely
        # looking odd: the page reads the report as permanently fresh, and the
        # sweep's "is this newer" comparison then refuses every later correct
        # timestamp. Nothing downstream can undo it, so it has to be refused
        # here.
        ahead = datetime.now(timezone.utc) + timedelta(days=400)
        report = self.publishable_report(
            "android", generated_at=ahead.strftime("%Y-%m-%dT%H:%M:%SZ")
        )
        drift, malformed = port_status.publishable_report_problems(
            self.manifest, "android", report
        )
        self.assertEqual([], drift)
        self.assertTrue(any("future" in problem for problem in malformed), malformed)

    def test_publishable_allows_a_little_clock_skew(self):
        # Runner clocks drift and a report is stamped a moment before it is
        # published, so being marginally ahead is normal rather than a defect.
        skewed = datetime.now(timezone.utc) + timedelta(minutes=5)
        report = self.publishable_report(
            "android", generated_at=skewed.strftime("%Y-%m-%dT%H:%M:%SZ")
        )
        self.assertEqual(([], []), port_status.publishable_report_problems(
            self.manifest, "android", report
        ))

    def test_publishable_separates_contract_drift_from_a_broken_report(self):
        report = self.publishable_report("android")
        del report["tests"]["CameraApiTest"]
        drift, malformed = port_status.publishable_report_problems(
            self.manifest, "android", report
        )
        self.assertEqual([], malformed)
        self.assertIn("CameraApiTest", drift[0])

    def test_publishable_rejects_unaccounted_and_unmeasured_workloads(self):
        report = self.publishable_report("linux-x64")
        del report["performance"]["benchmarks"]["quicksort"]
        report["performance"]["benchmarks"]["recursion"]["duration_ns"] = None
        drift, malformed = port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        )
        self.assertEqual([], drift)
        self.assertEqual(2, len(malformed), malformed)
        self.assertTrue(any("do not match the contract" in item for item in malformed))
        self.assertTrue(any("recursion" in item for item in malformed))

    def test_publishable_rejects_an_incomplete_or_mislabelled_run(self):
        for mutate, expected in (
            (lambda report: report["performance"].update({"status": "partial"}), "partial"),
            (lambda report: report["performance"].update({"missing": ["quicksort"]}), "quicksort"),
            (lambda report: report.update({"port": "android"}), "android"),
            (lambda report: report["summary"].update({"pass": 3}), "summary"),
        ):
            with self.subTest(expected=expected):
                report = self.publishable_report("watchos")
                mutate(report)
                _, malformed = port_status.publishable_report_problems(
                    self.manifest, "watchos", report
                )
                self.assertTrue(any(expected in item for item in malformed), malformed)

    def test_publishable_accepts_a_crashed_suite_with_partial_performance(self):
        # A suite that dies before CommonWorkloadBenchmarkTest (which runs late)
        # cannot produce a complete performance section. That report is exactly
        # the one the page must publish -- refusing it leaves the table serving
        # the previous green run, hiding the failure behind a stale pass.
        report = self.publishable_report("linux-x64")
        report["suite_finished"] = False
        report["performance"].update({
            "status": "partial",
            "missing": sorted(self.manifest["performance_benchmarks"])[3:],
            "benchmarks": {
                benchmark: {"duration_ns": 12000000, "checksum": "42"}
                for benchmark in sorted(self.manifest["performance_benchmarks"])[:3]
            },
        })
        failed, not_run = "ClipboardRoundTripTest", "MutableImageReadbackTest"
        report["tests"][failed]["status"] = "fail"
        report["tests"][not_run]["status"] = "not-run"
        report["summary"] = {
            "pass": len(report["tests"]) - 2, "fail": 1, "skip": 0, "not-run": 1
        }

        self.assertEqual(([], []), port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        ))

    def test_publishable_still_rejects_partial_performance_when_the_suite_finished(self):
        # The concession above is scoped to a suite that did not finish. A run
        # that claims completion may not quietly drop workloads.
        report = self.publishable_report("linux-x64")
        report["performance"]["status"] = "partial"
        del report["performance"]["benchmarks"]["quicksort"]

        _, malformed = port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        )
        self.assertTrue(any("partial" in item for item in malformed), malformed)
        self.assertTrue(
            any("do not match the contract" in item for item in malformed), malformed
        )

    def test_publishable_still_rejects_structural_defects_from_a_crashed_suite(self):
        # Producer bugs stay loud whatever the suite did: an unmeasured workload
        # and a reasonless skip are defects, not consequences of crashing.
        report = self.publishable_report("linux-x64")
        report["suite_finished"] = False
        report["performance"]["benchmarks"]["recursion"]["duration_ns"] = None
        del report["performance"]["benchmarks"]["quicksort"]
        report["performance"]["skipped"]["quicksort"] = ""

        _, malformed = port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        )
        self.assertTrue(any("recursion" in item for item in malformed), malformed)
        self.assertTrue(any("quicksort" in item for item in malformed), malformed)

    def test_publishable_matches_every_report_the_site_serves(self):
        for port in self.manifest["ports"]:
            report_path = port_status.REPO_ROOT / self.manifest["report_directory"] / (
                port["id"] + ".json"
            )
            with self.subTest(port=port["id"]):
                drift, malformed = port_status.publishable_report_problems(
                    self.manifest, port["id"], port_status.read_json(report_path)
                )
                self.assertEqual([], malformed)
                self.assertEqual([], drift)


if __name__ == "__main__":
    unittest.main()
