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
        self.assertEqual(178, counts["tests"])
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

    def test_skip_reported_under_a_screenshot_name_is_not_a_pass(self):
        # A screenshot test announces its skip under its screenshot OUTPUT name,
        # not its class name. Matching the class name alone dropped the marker,
        # and the surrounding start/finish pair then scored the test as a pass --
        # a skipped VideoIO decode, a skipped map and a skipped watch dialog all
        # rendered green on the public table.
        log_text = "\n".join(
            [
                "CN1SS:INFO:suite starting test=VideoIODecodedFramesScreenshotTest",
                "CN1SS:INFO:test=VideoIODecodedFrames status=SKIPPED reason=videoio-unavailable-on-linux",
                "CN1SS:INFO:suite finished test=VideoIODecodedFramesScreenshotTest",
                "CN1SS:INFO:suite starting test=CenteredDialogTitleScreenshotTest",
                "CN1SS:INFO:test=CenteredDialogTitle status=SKIPPED reason=phone-dialog-on-watch",
                "CN1SS:INFO:suite finished test=CenteredDialogTitleScreenshotTest",
                "CN1SS:SUITE:FINISHED",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            log_path = root / "suite.log"
            log_path.write_text(log_text, encoding="utf-8")
            report = port_status.normalize(
                manifest=self.manifest,
                port_id="linux-x64",
                logs=[log_path],
                comparisons=[],
                output=root / "report.json",
                run_url="https://example.invalid/run/3",
                commit="abc123",
                generated_at="2026-07-15T00:00:00Z",
            )

        decoded = report["tests"]["VideoIODecodedFramesScreenshotTest"]
        self.assertEqual("skip", decoded["status"])
        self.assertEqual(["videoio-unavailable-on-linux"], decoded["reasons"])
        # The bare base name of a test whose outputs are all suffixed has to
        # resolve too, or the same false pass comes back for the dialog tests.
        dialog = report["tests"]["CenteredDialogTitleScreenshotTest"]
        self.assertEqual("skip", dialog["status"])
        self.assertEqual(["phone-dialog-on-watch"], dialog["reasons"])

    def test_skip_marker_naming_nothing_in_the_contract_is_rejected(self):
        log_text = "\n".join(
            [
                "CN1SS:INFO:test=SomethingNobodyMapped status=SKIPPED reason=whatever",
                "CN1SS:SUITE:FINISHED",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            log_path = root / "suite.log"
            log_path.write_text(log_text, encoding="utf-8")
            with self.assertRaises(port_status.ContractError) as caught:
                port_status.normalize(
                    manifest=self.manifest,
                    port_id="linux-x64",
                    logs=[log_path],
                    comparisons=[],
                    output=root / "report.json",
                    run_url="https://example.invalid/run/4",
                    commit="abc123",
                    generated_at="2026-07-15T00:00:00Z",
                )
        self.assertIn("SomethingNobodyMapped", str(caught.exception))

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

    def test_validate_rejects_a_test_that_never_ran(self):
        # A registered test left at "not-run" reads on the page exactly like one that runs and
        # passes. Seven database tests were published that way -- added to the manifest, never
        # run in any stored report -- and nothing here objected.
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
            victim = next(
                name
                for name, result in android["tests"].items()
                if result.get("status") == "pass"
            )
            android["tests"][victim]["status"] = "not-run"
            android["summary"]["pass"] -= 1
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
                "reports tests that never ran: " + victim,
            ):
                port_status.validate(manifest)

    def test_validate_accepts_a_test_the_port_skipped(self):
        # The distinction the rule turns on: a port that genuinely cannot do something reports
        # "skip" from the suite itself, which is evidence rather than the absence of it.
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
            victim = next(
                name
                for name, result in android["tests"].items()
                if result.get("status") == "pass"
            )
            android["tests"][victim]["status"] = "skip"
            android["summary"]["pass"] -= 1
            android["summary"]["skip"] += 1
            android_path.write_text(
                json.dumps(android, indent=2, sort_keys=True) + "\n",
                encoding="utf-8",
            )
            manifest = dict(self.manifest)
            manifest["report_directory"] = str(
                report_root.relative_to(port_status.REPO_ROOT)
            )
            # Asserted against this rule alone: a skip carries its own separate obligation --
            # errata explaining it -- and that is what would be reported here instead.
            try:
                port_status.validate(manifest)
            except port_status.ContractError as exc:
                self.assertNotIn("never ran", str(exc))

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
        # The summary counts the results the report carries, so dropping a test
        # means dropping its count too. Leaving it stale would now -- correctly
        # -- be a malformed report rather than pure drift.
        report["summary"]["pass"] -= 1
        drift, malformed = port_status.publishable_report_problems(
            self.manifest, "android", report
        )
        self.assertEqual([], malformed)
        self.assertIn("CameraApiTest", drift[0])

    def test_publishable_rejects_a_stale_summary_even_under_drift(self):
        # Drift used to suppress the summary check entirely, so a genuinely
        # broken report could be filed as drift and fall back quietly.
        report = self.publishable_report("android")
        del report["tests"]["CameraApiTest"]

        _, malformed = port_status.publishable_report_problems(
            self.manifest, "android", report
        )
        self.assertTrue(any("summary" in item for item in malformed), malformed)

    def test_publishable_rejects_a_workload_both_measured_and_skipped(self):
        report = self.publishable_report("linux-x64")
        report["performance"]["skipped"]["quicksort"] = "some-reason"

        _, malformed = port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        )
        self.assertTrue(
            any("both measured and skipped" in item for item in malformed), malformed
        )

    def test_workload_gap_message_names_the_offender(self):
        # The message must point at the workload that is wrong, not list the
        # nine that are fine.
        message = port_status.describe_workload_gap(
            ["a", "c"], ["a", "b"]
        )
        self.assertIn("missing b", message)
        self.assertIn("unexpected c", message)

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

    def test_publishable_rejects_a_crashed_suite_that_loses_a_workload(self):
        # The crashed-suite concession is not a hole: normalize derives
        # `missing` from the contract, so measured + skipped + missing must
        # still name every workload even when the suite died.
        report = self.publishable_report("linux-x64")
        report["suite_finished"] = False
        report["performance"]["status"] = "partial"
        del report["performance"]["benchmarks"]["quicksort"]
        report["performance"]["missing"] = []

        _, malformed = port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        )
        self.assertTrue(any("unaccounted for" in item for item in malformed), malformed)

    def test_publishable_rejects_a_wrongly_typed_skipped_section(self):
        # "skipped": [] is malformed, not "no skips" -- `or {}` used to coerce
        # it past the type check.
        report = self.publishable_report("linux-x64")
        report["performance"]["skipped"] = []

        _, malformed = port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        )
        self.assertTrue(any("not objects" in item for item in malformed), malformed)

    def test_publishable_rejects_a_malformed_missing_field(self):
        # "missing": true used to raise TypeError out of the join, and main()
        # catches only ContractError -- so the gate crashed with a status the
        # sweep does not treat as unusable, and it fell back quietly.
        for bad in (True, 7, "quicksort", ["quicksort", 3]):
            with self.subTest(bad=bad):
                report = self.publishable_report("linux-x64")
                report["performance"]["missing"] = bad
                _, malformed = port_status.publishable_report_problems(
                    self.manifest, "linux-x64", report
                )
                self.assertTrue(
                    any("missing list" in item for item in malformed), malformed)

    def test_publishable_rejects_a_complete_label_on_partial_benchmark_data(self):
        # normalize writes "partial" whenever a workload went unrun, but nothing
        # downstream re-derives it: port-status.html renders the timings of any
        # benchmark whose status is "complete". A crashed suite that mislabelled
        # itself would present the handful of workloads it managed to measure as
        # a finished measurement run, and measured | missing still covers the
        # contract so the accounting check above stays quiet.
        report = self.publishable_report("linux-x64")
        report["suite_finished"] = False
        report["performance"]["status"] = "complete"
        report["performance"]["missing"] = ["quicksort"]
        del report["performance"]["benchmarks"]["quicksort"]

        _, malformed = port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        )
        self.assertTrue(
            any("declares missing workloads" in item for item in malformed), malformed
        )

    def test_publishable_rejects_a_malformed_reason_list(self):
        # The reason list is optional, but the feature template calls len, range
        # and hasPrefix on whatever is there. A wrongly typed one has to be
        # caught as a single unusable report rather than reaching the data
        # branch and failing the Hugo build for the whole site.
        for bad in (True, 7, "flaky", {"why": "flaky"}, ["ok", 3]):
            with self.subTest(bad=bad):
                report = self.publishable_report("linux-x64")
                report["tests"]["ClipboardRoundTripTest"]["reasons"] = bad
                _, malformed = port_status.publishable_report_problems(
                    self.manifest, "linux-x64", report
                )
                self.assertTrue(
                    any("reasons is not an array of strings" in item
                        for item in malformed), malformed)

    def test_publishable_accepts_a_well_formed_reason_list(self):
        report = self.publishable_report("linux-x64")
        report["tests"]["ClipboardRoundTripTest"]["reasons"] = ["documented skip"]

        self.assertEqual(([], []), port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        ))

    def test_a_declared_skip_survives_a_missing_screenshot(self):
        # A test that skips produces no screenshot, so the comparison reports the actual as
        # missing. Counting that as a failure made "we could not test this here" and "this is
        # broken" the same red, which is the distinction the gate exists to draw -- and it is why
        # the map test stayed red after it started reporting the skip properly.
        log_text = "\n".join(
            [
                "CN1SS:INFO:suite starting test=GoogleWebMapScreenshotTest",
                "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=map-tiles-never-loaded",
                "CN1SS:INFO:suite finished test=GoogleWebMapScreenshotTest",
                "CN1SS:SUITE:FINISHED",
            ]
        )
        comparison = {"results": [{"test": "GoogleWebMap", "status": "missing_actual"}]}
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            log_path = root / "suite.log"
            log_path.write_text(log_text, encoding="utf-8")
            comparison_path = root / "comparison.json"
            comparison_path.write_text(json.dumps(comparison), encoding="utf-8")
            report = port_status.normalize(
                manifest=self.manifest,
                port_id="ios-gl",
                logs=[log_path],
                comparisons=[comparison_path],
                output=root / "report.json",
                run_url="https://example.invalid/run/9",
                commit="abc123",
                generated_at="2026-07-15T00:00:00Z",
            )
        self.assertEqual("skip", report["tests"]["GoogleWebMapScreenshotTest"]["status"])

    def test_a_screenshot_that_differs_still_fails_a_skipped_test(self):
        # The other half, which is what keeps the forgiveness narrow: only a missing actual is
        # excused. Anything that was captured and compared is a real result, and a difference in
        # it fails whatever the log said.
        log_text = "\n".join(
            [
                "CN1SS:INFO:suite starting test=GoogleWebMapScreenshotTest",
                "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=map-tiles-never-loaded",
                "CN1SS:INFO:suite finished test=GoogleWebMapScreenshotTest",
                "CN1SS:SUITE:FINISHED",
            ]
        )
        comparison = {"results": [{"test": "GoogleWebMap", "status": "different"}]}
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            log_path = root / "suite.log"
            log_path.write_text(log_text, encoding="utf-8")
            comparison_path = root / "comparison.json"
            comparison_path.write_text(json.dumps(comparison), encoding="utf-8")
            report = port_status.normalize(
                manifest=self.manifest,
                port_id="ios-gl",
                logs=[log_path],
                comparisons=[comparison_path],
                output=root / "report.json",
                run_url="https://example.invalid/run/10",
                commit="abc123",
                generated_at="2026-07-15T00:00:00Z",
            )
        self.assertEqual("fail", report["tests"]["GoogleWebMapScreenshotTest"]["status"])

    def test_publishable_requires_a_boolean_suite_completion_marker(self):
        # bool() accepted "false" and 1 as a finished suite, and Hugo reads the
        # same value as truthy -- so a report with no failures and a pile of
        # not-run tests rendered a green "Suite completed" card.
        for bad in ("false", "true", 1, 0, [], {}):
            with self.subTest(bad=bad):
                report = self.publishable_report("linux-x64")
                report["suite_finished"] = bad
                _, malformed = port_status.publishable_report_problems(
                    self.manifest, "linux-x64", report
                )
                self.assertTrue(
                    any("suite_finished" in item for item in malformed), malformed)

    def test_publishable_rejects_an_unhashable_test_status(self):
        # A list status raises TypeError out of the set-membership test, and
        # main() catches only ContractError -- so the gate died with a status the
        # sweep does not read as unusable and fell back quietly.
        for bad in (["skip"], {"status": "skip"}, 3, None):
            with self.subTest(bad=bad):
                report = self.publishable_report("linux-x64")
                report["tests"]["ClipboardRoundTripTest"]["status"] = bad
                _, malformed = port_status.publishable_report_problems(
                    self.manifest, "linux-x64", report
                )
                self.assertTrue(
                    any("invalid result" in item for item in malformed), malformed)

    def test_publishable_rejects_boolean_summary_counts(self):
        # Python considers True == 1, so a count of exactly one serialized as a
        # JSON boolean compared equal to the calculated summary and published;
        # Hugo then rendered "true skipped".
        report = self.publishable_report("linux-x64")
        report["tests"]["ClipboardRoundTripTest"]["status"] = "skip"
        report["summary"] = {
            "pass": len(report["tests"]) - 1, "fail": 0, "skip": True, "not-run": 0
        }
        _, malformed = port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        )
        self.assertTrue(
            any("non-negative integers" in item for item in malformed), malformed)

    def test_publishable_rejects_a_malformed_performance_status_when_unfinished(self):
        # port-status.html does `eq .status "complete"`, and Hugo aborts the whole
        # site build when that compares a map with a string. The unfinished branch
        # used to skip status validation entirely.
        report = self.publishable_report("linux-x64")
        report["suite_finished"] = False
        report["performance"]["status"] = {"state": "partial"}
        _, malformed = port_status.publishable_report_problems(
            self.manifest, "linux-x64", report
        )
        self.assertTrue(
            any("performance status is" in item for item in malformed), malformed)

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
