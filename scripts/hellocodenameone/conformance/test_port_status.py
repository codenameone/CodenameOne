#!/usr/bin/env python3

import copy
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
        # Deliberately no literal totals. Every one of these used to be a magic
        # number that each test-adding branch had to retype, so two branches
        # adding a test conflicted here by construction -- on a line whose only
        # content was a number neither author had a reason to think about. What
        # is worth asserting is the relationship: the suite and the contract
        # describe the same set of tests, and nothing is counted twice. Which
        # test belongs to which capability is a separate question, and a real
        # one; it is asserted below rather than dropped.
        counts = port_status.validate(self.manifest)
        registered = port_status.registered_tests()
        mapped = port_status.test_to_feature(self.manifest)
        performance = self.manifest["performance_tests"]

        self.assertEqual(sorted(registered), sorted(set(registered)))
        self.assertEqual(set(registered), set(mapped) | set(performance))
        self.assertEqual(set(), set(mapped) & set(performance))
        self.assertEqual(len(mapped), counts["tests"])
        self.assertEqual(len(performance), counts["performance_tests"])
        self.assertEqual(len(self.manifest["ports"]), counts["ports"])
        self.assertEqual(len(self.manifest["features"]), counts["features"])
        self.assertTrue(all(feature["tests"] for feature in self.manifest["features"]))

        # Floors, not equalities: these guard against a collapse -- a manifest
        # that lost its features, a golden directory that stopped resolving --
        # and a branch that adds to any of them never has to touch this file.
        self.assertGreater(counts["features"], 50)
        self.assertGreater(counts["goldens"], 100)
        self.assertGreater(counts["manual_features"], 15)
        self.assertEqual(8, counts["deployment_platforms"])
        self.assertEqual(3, counts["browser_engines"])

    def test_load_bearing_tests_stay_under_the_capability_they_prove(self):
        # A spot check, not a registry: these are the mappings where landing a result under the
        # wrong row would publish a specific capability claim the test never made. Nothing above
        # catches that -- validate() only requires each test to sit under exactly one feature,
        # and any feature satisfies it. Adding a feature does not oblige anyone to extend this
        # list; it is the literal totals that every branch had to retype, not these.
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
                # A port with no published report has nothing to stage; the
                # fixture is about the ports that do have one.
                if not source.exists():
                    continue
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
                "summary does not match the test results",
            ):
                port_status.validate(manifest)

    def stored_reports(self):
        """The checked-in fallback report of every port that has one.

        A port that has never published has no file here, and that is a state
        the contract supports rather than an error: validate() reports it as
        "no stored report yet" and provenance_problems explicitly declines to
        ask for a copy of something that does not exist yet. A newly registered
        port is in exactly that state until its first run on master publishes,
        so reading these eagerly would make adding a port impossible without
        hand-authoring the very snapshot the provenance gate exists to forbid.
        """
        directory = port_status.REPO_ROOT / self.manifest["report_directory"]
        reports = {}
        for port in self.manifest["ports"]:
            path = directory / (port["id"] + ".json")
            if not path.exists():
                continue
            reports[port["id"]] = port_status.read_json(path)
        return reports

    def test_coverage_rejects_a_test_that_never_ran(self):
        # A registered test left at "not-run" reads on the page exactly like one that runs and
        # passes. This used to be asserted against the checked-in snapshots, which is why every
        # branch that registered a test had to edit eleven of them -- and why the cheapest way
        # to go green was to type "pass" for a run that never happened. The obligation belongs
        # to the reports the ports actually publish, where nothing a branch writes can satisfy it.
        reports = self.stored_reports()
        victim = next(
            name
            for name, result in reports["android"]["tests"].items()
            if result.get("status") == "pass"
        )
        reports["android"]["tests"][victim]["status"] = "not-run"
        problems = port_status.coverage_problems(self.manifest, reports)
        self.assertTrue(
            any("android" in problem and victim in problem for problem in problems),
            problems,
        )

    def test_coverage_ignores_a_retired_test_left_at_not_run(self):
        # A report that predates a test's retirement still carries the test, and if that run
        # never reached it the entry is "not-run". Reporting that is holding a port to an
        # obligation the contract has withdrawn -- the same report is tolerated as drift
        # everywhere else, so the sweep would have stayed red until the port happened to rerun.
        reports = self.stored_reports()
        reports["android"]["tests"]["RetiredApiTest"] = {
            "feature": "crypto",
            "status": "not-run",
        }
        self.assertEqual([], port_status.coverage_problems(self.manifest, reports))

    def test_a_scoped_test_is_expected_only_where_it_applies(self):
        # The windowed baselines are the reason scoping exists. A port with no
        # windowing system will never carry them, so without a scope they would be
        # absent from its report forever -- and the first desktop run to publish one
        # would make every other port look like it had dropped a test.
        desktop = port_status.tests_for_port(self.manifest, "linux-x64")
        phone = port_status.tests_for_port(self.manifest, "android")
        self.assertIn("WindowLayoutTest", desktop)
        self.assertNotIn("WindowLayoutTest", phone)
        # MultiWindowApiTest is not scoped: it asserts the contract everywhere,
        # including that the API throws where windows are unsupported.
        self.assertIn("MultiWindowApiTest", desktop)
        self.assertIn("MultiWindowApiTest", phone)

    def scoped_out_ports(self, test_name):
        return {
            port["id"]
            for port in self.manifest["ports"]
            if test_name not in port_status.tests_for_port(self.manifest, port["id"])
        }

    def coverage_with_one_port_carrying(self, port_id, test_name, feature):
        reports = self.stored_reports()
        reports[port_id]["tests"][test_name] = {"feature": feature, "status": "pass"}
        return port_status.coverage_problems(self.manifest, reports)

    def test_coverage_does_not_call_a_scoped_test_dropped_where_it_cannot_run(self):
        # The failure this prevents: one desktop port publishes WindowLayoutTest,
        # which teaches known_since that the test exists, and every later run on a
        # port that can never run it is then read as having dropped it.
        problems = self.coverage_with_one_port_carrying(
            "linux-x64", "WindowLayoutTest", "multi-window")
        out = self.scoped_out_ports("WindowLayoutTest")
        self.assertTrue(out)
        blamed = [
            p for p in problems
            if "WindowLayoutTest" in p and any(port in p for port in out)
        ]
        self.assertEqual([], blamed, problems)

    def test_coverage_still_catches_a_scoped_test_dropped_where_it_applies(self):
        # Scoping must not become a way for a port that *does* have windows to stop
        # reporting them: another desktop port that ran later without it is still a
        # port that dropped it.
        problems = self.coverage_with_one_port_carrying(
            "linux-x64", "WindowLayoutTest", "multi-window")
        in_scope = port_status.test_scopes(self.manifest)["WindowLayoutTest"] - {"linux-x64"}
        self.assertTrue(
            any("WindowLayoutTest" in p and any(port in p for port in in_scope)
                for p in problems),
            problems,
        )

    def test_a_test_scoped_to_an_unknown_port_is_rejected(self):
        manifest = copy.deepcopy(self.manifest)
        self.rescope(manifest, "WindowLayoutTest", ["linux-x64", "no-such-port"])
        with self.assertRaises(port_status.ContractError) as caught:
            port_status.validate(manifest)
        self.assertIn("no-such-port", str(caught.exception))

    def test_a_scoped_test_must_name_at_least_one_port(self):
        manifest = copy.deepcopy(self.manifest)
        self.rescope(manifest, "WindowLayoutTest", [])
        with self.assertRaises(port_status.ContractError) as caught:
            port_status.validate(manifest)
        self.assertIn("WindowLayoutTest", str(caught.exception))

    @staticmethod
    def rescope(manifest, test_name, ports):
        manifest.setdefault("test_scopes", {})[test_name] = ports

    def test_coverage_accepts_a_documented_skip(self):
        # The distinction the rule turns on: a port that genuinely cannot do something reports
        # "skip" from the suite itself, which is evidence rather than the absence of it -- but
        # only where an erratum accounts for the reason the run gave. The published reports
        # carry these already, so the shipped data is the fixture.
        reports = self.stored_reports()
        documented = [
            (port, name)
            for port, report in reports.items()
            for name, result in report["tests"].items()
            if result.get("status") == "skip"
        ]
        self.assertTrue(documented)
        self.assertEqual([], port_status.coverage_problems(self.manifest, reports))

    def test_coverage_rejects_an_undocumented_skip(self):
        # Otherwise a port can simply stop running a test: mark it skipped, publish, and this
        # gate calls it a satisfactory result. validate() cannot catch it either -- it reads
        # the checked-in fallbacks, not what the ports published -- so the first symptom would
        # be a failed website build rather than the name of the port that started skipping.
        reports = self.stored_reports()
        victim = next(
            name
            for name, result in reports["android"]["tests"].items()
            if result.get("status") == "pass"
        )
        reports["android"]["tests"][victim] = {
            "feature": reports["android"]["tests"][victim]["feature"],
            "status": "skip",
            "reasons": ["something-nobody-wrote-down"],
        }
        problems = port_status.coverage_problems(self.manifest, reports)
        self.assertTrue(
            any("android" in problem and victim in problem for problem in problems),
            problems,
        )

    def test_coverage_rejects_a_skip_reason_scoped_to_another_port(self):
        # Matching the test name alone would let any future skip of a named test read as
        # documented. CameraApiTest has errata, but the missing-webcam code is written about
        # Windows; the same code from Linux says something nobody has explained.
        reports = self.stored_reports()
        reports["linux-x64"]["tests"]["CameraApiTest"] = {
            "feature": "camera-access",
            "status": "skip",
            "reasons": ["no-host-webcam-capture-on-win"],
        }
        problems = port_status.coverage_problems(self.manifest, reports)
        self.assertTrue(
            any("linux-x64" in problem and "CameraApiTest" in problem for problem in problems),
            problems,
        )

    def test_a_reason_code_with_no_prefix_documents_nothing(self):
        # Both matchers ask whether the reason starts with the prefix, and every string starts
        # with the empty one. An erratum that lost this field to a typo would therefore turn any
        # future skip of that test green -- the exact opposite of what writing one is for.
        supplement = {
            "skip_reasons": [
                {"test": "CameraApiTest", "reason_codes": [{"ports": ["android"]}]}
            ]
        }
        self.assertFalse(
            port_status.skip_is_documented(
                supplement, "android", "CameraApiTest", ["something-entirely-unrelated"]
            )
        )
        self.assertFalse(
            port_status.skip_is_documented(
                supplement, "android", "CameraApiTest", ["needs-runtime-permission-on-and"]
            )
        )

    def test_validate_rejects_a_reason_code_with_no_prefix(self):
        # And the configuration error is caught where it is made, rather than only failing to
        # match later. Both halves matter: the matcher cannot be the only guard, because the
        # page draws its own tick from its own copy of this rule.
        supplement = {
            "skip_reasons": [{"test": "CameraApiTest", "reason_codes": [{"prefix": ""}]}]
        }
        self.assertFalse(
            port_status.skip_is_documented(supplement, "android", "CameraApiTest", ["anything"])
        )

    def documented_skip_count(self, port, marker, reference):
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / "suite.log"
            log.write_text(marker, encoding="utf-8")
            accounted, _ = port_status.documented_skip_goldens(
                self.manifest, port, [log], port_status.REPO_ROOT / reference
            )
        return accounted

    def test_a_documented_skip_accounts_for_its_golden(self):
        # The screenshot count guard reads an unproduced golden as a test that hung, crashed or
        # never delivered its frame -- which it is, when nothing else was said. A test that
        # prints status=SKIPPED said something. GoogleWebMap takes that path when the Maps tiles
        # never load, the errata document it on android, and the guard failed the whole job on
        # the uncovered golden anyway -- so the skip path could never succeed on a port that
        # owns a golden.
        self.assertEqual(
            ["GoogleWebMap"],
            self.documented_skip_count(
                "android",
                "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=map-tiles-never-loaded\n",
                "scripts/android/screenshots",
            ),
        )

    def test_silence_accounts_for_nothing(self):
        # The case the guard exists for, and the one this must not soften: a test that hangs or
        # crashes leaves no record, and the missing golden is the only evidence there is.
        self.assertEqual(
            [], self.documented_skip_count("android", "", "scripts/android/screenshots")
        )

    def test_an_undocumented_skip_accounts_for_nothing(self):
        self.assertEqual(
            [],
            self.documented_skip_count(
                "android",
                "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=something-nobody-wrote-down\n",
                "scripts/android/screenshots",
            ),
        )

    def test_a_skip_documented_for_another_port_accounts_for_nothing(self):
        # map-tiles-never-loaded is written about android and the two iOS renderers. The same
        # reason arriving from Linux, where the errata expect no-api-key instead, is a port
        # behaving unexpectedly rather than a network nobody can reach.
        self.assertEqual(
            [],
            self.documented_skip_count(
                "linux-x64",
                "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=map-tiles-never-loaded\n",
                "scripts/linux/screenshots",
            ),
        )

    def test_only_goldens_the_run_did_not_produce_are_discounted(self):
        # The caller subtracts this count from the number of UNCOVERED goldens, so naming one the
        # run did compare would subtract a golden nothing was missing -- and that spare
        # subtraction would then hide a genuinely uncovered golden belonging to some other test,
        # which is the regression the guard exists to catch. A test owning several screenshots
        # that captures a few before skipping is exactly the case.
        marker = "CN1SS:INFO:test=CenteredDialogTitle status=SKIPPED reason=phone-dialog-on-watch\n"
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            reference = root / "ref"
            reference.mkdir()
            for name in ("CenteredDialogTitle_dark", "CenteredDialogTitle_light"):
                (reference / (name + ".png")).write_bytes(b"")
            log = root / "suite.log"
            log.write_text(marker, encoding="utf-8")
            nothing_compared = root / "none.json"
            nothing_compared.write_text(json.dumps({"results": []}), encoding="utf-8")
            one_compared = root / "one.json"
            one_compared.write_text(
                json.dumps(
                    {"results": [{"test": "CenteredDialogTitle_light", "status": "equal"}]}
                ),
                encoding="utf-8",
            )
            self.assertEqual(
                ["CenteredDialogTitle_dark", "CenteredDialogTitle_light"],
                port_status.documented_skip_goldens(
                    self.manifest, "watchos", [log], reference, [nothing_compared]
                )[0],
            )
            self.assertEqual(
                ["CenteredDialogTitle_dark"],
                port_status.documented_skip_goldens(
                    self.manifest, "watchos", [log], reference, [one_compared]
                )[0],
            )

    def test_coverage_rejects_a_skip_carrying_no_reason(self):
        # An erratum with reason codes documents the reasons it lists, not the test. A skip
        # that names none matches nothing, which is what the page already decides.
        reports = self.stored_reports()
        reports["android"]["tests"]["CameraApiTest"] = {
            "feature": "camera-access",
            "status": "skip",
        }
        problems = port_status.coverage_problems(self.manifest, reports)
        self.assertTrue(
            any("android" in problem and "CameraApiTest" in problem for problem in problems),
            problems,
        )

    def test_coverage_catches_a_test_absent_from_every_report(self):
        # Comparing reports to each other cannot see this: with the test missing everywhere,
        # there is no older report left to prove it existed, so every port is excused and the
        # gate prints success over a test nothing runs anywhere -- the worst version of the
        # failure this gate is for. Each report's own contract answers it directly.
        reports = self.stored_reports()
        victim = "CryptoApiTest"
        for report in reports.values():
            report["tests"].pop(victim, None)
        self.assertEqual([], port_status.coverage_problems(self.manifest, reports))

        contract = set(port_status.test_to_feature(self.manifest))
        problems = port_status.coverage_problems(
            self.manifest, reports, {port: contract for port in reports}
        )
        self.assertEqual(len(reports), len(problems), problems)
        self.assertTrue(all(victim in problem for problem in problems), problems)

    def test_coverage_tolerates_a_report_whose_own_contract_predates_the_test(self):
        # The state every port is in for a few hours after a test is registered, and the one
        # this must never fail: the run happened against a manifest that did not define the
        # test, so there was nothing to report.
        reports = self.stored_reports()
        victim = "CryptoApiTest"
        for report in reports.values():
            report["tests"].pop(victim, None)
        # Each port's own contract is exactly what its run reported, which is what "the run
        # predates the test" means. Handing every port the CURRENT contract minus one test
        # would instead accuse the five Apple ports of dropping LogSubclassCaptureTest, which
        # their reports really do predate -- and the gate would be right to say so.
        self.assertEqual(
            [],
            port_status.coverage_problems(
                self.manifest,
                reports,
                {port: set(report["tests"]) for port, report in reports.items()},
            ),
        )

    def test_coverage_leaves_a_port_alone_when_its_contract_is_unknown(self):
        # A manifest the sweep could not fetch proves nothing, so that port keeps the weaker
        # report-to-report comparison rather than being excused or accused.
        reports = self.stored_reports()
        victim = "CryptoApiTest"
        for report in reports.values():
            report["tests"].pop(victim, None)
        contract = set(port_status.test_to_feature(self.manifest))
        problems = port_status.coverage_problems(
            self.manifest, reports, {"android": contract}
        )
        self.assertEqual(1, len(problems), problems)
        self.assertIn("android", problems[0])

    def test_coverage_accepts_a_report_older_than_the_test(self):
        # The state every port is in between the commit that registers a test and that port's
        # next master run. Failing here would put the old ritual straight back: the only way to
        # merge a test would be to make eleven reports claim a result for it first.
        reports = self.stored_reports()
        newest = max(reports, key=lambda port: reports[port]["generated_at"])
        oldest = min(reports, key=lambda port: reports[port]["generated_at"])
        self.assertNotEqual(newest, oldest)
        victim = next(iter(reports[newest]["tests"]))
        del reports[oldest]["tests"][victim]
        self.assertEqual([], port_status.coverage_problems(self.manifest, reports))

    def test_coverage_rejects_a_test_a_later_run_dropped(self):
        # The other half of the same comparison. A run that happened after another run which
        # already covered the test has no "my contract predates it" excuse left.
        reports = self.stored_reports()
        newest = max(reports, key=lambda port: reports[port]["generated_at"])
        oldest = min(reports, key=lambda port: reports[port]["generated_at"])
        victim = next(iter(reports[oldest]["tests"]))
        del reports[newest]["tests"][victim]
        problems = port_status.coverage_problems(self.manifest, reports)
        self.assertTrue(
            any(newest in problem and victim in problem for problem in problems),
            problems,
        )

    def test_validate_tolerates_a_snapshot_that_predates_a_test(self):
        # Registering a test must not require touching a single report. This is the assertion
        # that keeps it that way.
        original_directory = self.manifest["report_directory"]
        with tempfile.TemporaryDirectory(dir=port_status.REPO_ROOT) as tmp:
            report_root = Path(tmp)
            for port in self.manifest["ports"]:
                source = port_status.REPO_ROOT / original_directory / (port["id"] + ".json")
                # A port with no published report has nothing to stage; the
                # fixture is about the ports that do have one.
                if not source.exists():
                    continue
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
            del android["tests"][victim]
            android["summary"]["pass"] -= 1
            android_path.write_text(
                json.dumps(android, indent=2, sort_keys=True) + "\n", encoding="utf-8"
            )
            manifest = dict(self.manifest)
            manifest["report_directory"] = str(report_root.relative_to(port_status.REPO_ROOT))
            counts = port_status.validate(manifest)
        self.assertTrue(
            any("android" in item and victim in item for item in counts["drift"]),
            counts["drift"],
        )

    def test_provenance_rejects_results_edited_without_a_new_run(self):
        # Exactly the edit twelve published "passes" were made by: a test entry appended to a
        # report, its summary bumped, and the stamp naming the run left untouched.
        before = self.stored_reports()["android"]
        after = json.loads(json.dumps(before))
        after["tests"]["SomeBrandNewTest"] = {"feature": "crypto", "status": "pass"}
        after["summary"]["pass"] += 1
        problems = port_status.provenance_problems("android", before, after)
        self.assertEqual(1, len(problems), problems)
        self.assertIn("CI output", problems[0])

    def newer_snapshot(self, before):
        after = json.loads(json.dumps(before))
        after["tests"]["SomeBrandNewTest"] = {"feature": "crypto", "status": "pass"}
        after["summary"]["pass"] += 1
        after["generated_at"] = "2026-12-31T00:00:00Z"
        after["commit"] = "0123456789abcdef"
        after["run_url"] = "https://github.com/codenameone/CodenameOne/actions/runs/99"
        return after

    def test_provenance_accepts_a_genuinely_newer_snapshot(self):
        # Without the data branch to consult. Unverifiable is not the same as forged, and
        # failing a branch because a fetch flaked would teach people to route around this.
        before = self.stored_reports()["android"]
        self.assertEqual(
            [], port_status.provenance_problems("android", before, self.newer_snapshot(before))
        )

    def test_provenance_accepts_a_report_the_data_branch_published(self):
        before = self.stored_reports()["android"]
        after = self.newer_snapshot(before)
        self.assertEqual(
            [],
            port_status.provenance_problems(
                "android", before, after, published=[before, after]
            ),
        )

    def test_provenance_rejects_a_report_no_run_ever_published(self):
        # The bypass that survived requiring both identity fields: type a plausible run URL and
        # a plausible date. A checked-in report is a copy of what CI put on the data branch, so
        # the branch is asked whether this report was ever there. Producing one that was needs
        # the write access to that branch which only the publish workflows have.
        before = self.stored_reports()["android"]
        after = self.newer_snapshot(before)
        problems = port_status.provenance_problems(
            "android", before, after, published=[before]
        )
        self.assertEqual(1, len(problems), problems)
        self.assertIn("ever held", problems[0])

    def test_provenance_rejects_a_run_url_that_names_no_run(self):
        before = self.stored_reports()["android"]
        after = self.newer_snapshot(before)
        after["run_url"] = "made-up-new-run"
        problems = port_status.provenance_problems(
            "android", before, after, published=[after]
        )
        self.assertEqual(1, len(problems), problems)
        self.assertIn("does not name a workflow run", problems[0])

    def test_provenance_rejects_a_fresh_stamp_over_the_same_run(self):
        # The easier version of the same forgery, and the one a gate that accepted any single
        # provenance change would have invited: invent the result, type today's date, and leave
        # commit and run_url still naming the run that never produced it.
        before = self.stored_reports()["android"]
        after = json.loads(json.dumps(before))
        after["tests"]["SomeBrandNewTest"] = {"feature": "crypto", "status": "pass"}
        after["summary"]["pass"] += 1
        after["generated_at"] = "2026-12-31T00:00:00Z"
        problems = port_status.provenance_problems("android", before, after)
        self.assertEqual(1, len(problems), problems)
        self.assertIn("run_url", problems[0])

    def test_provenance_rejects_a_new_run_url_on_the_same_stamp(self):
        # The mirror image. A run reports at a time; reusing the old one says this snapshot is
        # the same measurement under a different name.
        before = self.stored_reports()["android"]
        after = json.loads(json.dumps(before))
        after["performance"]["benchmarks"]["quicksort"]["duration_ns"] = 1
        after["run_url"] = "https://github.com/codenameone/CodenameOne/actions/runs/98"
        problems = port_status.provenance_problems("android", before, after)
        self.assertEqual(1, len(problems), problems)
        self.assertIn("generated_at", problems[0])

    def test_provenance_rejects_an_emptied_run_url(self):
        # "Different" is not enough on its own: deleting the field would otherwise read as a
        # change and let the edit through with no run named at all.
        before = self.stored_reports()["android"]
        after = json.loads(json.dumps(before))
        after["tests"]["SomeBrandNewTest"] = {"feature": "crypto", "status": "pass"}
        after["summary"]["pass"] += 1
        after["generated_at"] = "2026-12-31T00:00:00Z"
        after["run_url"] = ""
        problems = port_status.provenance_problems("android", before, after)
        self.assertEqual(1, len(problems), problems)
        self.assertIn("run_url", problems[0])

    def test_provenance_rejects_edited_benchmark_measurements(self):
        # The findings nobody can check by reading them. A benchmark duration is published as a
        # measurement of a named run; rewriting one in place attributes an invented number to
        # that run exactly the way the twelve invented passes did. Naming only tests and summary
        # would have left performance as the one thing a branch could still edit.
        before = self.stored_reports()["android"]
        after = json.loads(json.dumps(before))
        workload = next(iter(after["performance"]["benchmarks"]))
        after["performance"]["benchmarks"][workload]["duration_ns"] = 1
        problems = port_status.provenance_problems("android", before, after)
        self.assertEqual(1, len(problems), problems)
        self.assertIn("performance", problems[0])

    def test_provenance_rejects_an_edited_completion_marker(self):
        # suite_finished is what makes a port card say the suite completed rather than that the
        # run stopped early.
        before = self.stored_reports()["android"]
        after = json.loads(json.dumps(before))
        after["suite_finished"] = not before["suite_finished"]
        problems = port_status.provenance_problems("android", before, after)
        self.assertEqual(1, len(problems), problems)
        self.assertIn("suite_finished", problems[0])

    def test_provenance_rejects_a_timestamp_retyped_onto_an_old_snapshot(self):
        # Changes no finding, so every rule about findings passes it -- and it is the edit with
        # the worst consequence of any of them. The page reads generated_at to decide whether a
        # column is stale, so retyping it is how a port that has stopped reporting altogether
        # would go on looking like it was still running. Corroboration is therefore asked of any
        # change, not only of a changed result.
        before = self.stored_reports()["android"]
        after = json.loads(json.dumps(before))
        after["generated_at"] = "2026-12-31T00:00:00Z"
        problems = port_status.provenance_problems(
            "android", before, after, published=[before]
        )
        self.assertEqual(1, len(problems), problems)
        self.assertIn("ever held", problems[0])

    def test_provenance_rejects_a_hand_authored_report_for_a_new_port(self):
        # The one report with no earlier version to be checked against, and so the only one
        # nobody was checking at all: a pull request that adds a port could give it an entirely
        # green snapshot. It has to be a report the data branch published, like every other.
        published = self.stored_reports()["android"]
        invented = json.loads(json.dumps(published))
        invented["port"] = "freebsd"
        problems = port_status.provenance_problems(
            "freebsd", None, invented, published=[published]
        )
        self.assertEqual(1, len(problems), problems)
        self.assertIn("ever held", problems[0])

    def test_provenance_tells_a_new_port_it_needs_no_report(self):
        # And the advice has to be actionable, which it is only because a port with no stored
        # report is now a supported state: every cell reads "No stored report" until its first
        # run. Otherwise the only way to add a port would be to hand-author the snapshot this
        # rule refuses.
        invented = self.stored_reports()["android"]
        problems = port_status.provenance_problems("freebsd", None, invented, published=[])
        self.assertEqual(1, len(problems), problems)
        self.assertIn("never published a report", problems[0])

    def test_provenance_accepts_a_new_port_report_copied_from_the_branch(self):
        published = self.stored_reports()["android"]
        self.assertEqual(
            [], port_status.provenance_problems("android", None, published, published=[published])
        )

    def test_validate_accepts_a_port_with_no_stored_report(self):
        original_directory = self.manifest["report_directory"]
        with tempfile.TemporaryDirectory(dir=port_status.REPO_ROOT) as tmp:
            report_root = Path(tmp)
            for port in self.manifest["ports"]:
                if port["id"] == "tvos":
                    continue
                source = port_status.REPO_ROOT / original_directory / (port["id"] + ".json")
                # Ports that have not published yet have nothing to copy. The
                # fixture only needs tvos to be the absent one for the assertion
                # below to mean something.
                if not source.exists():
                    continue
                (report_root / source.name).write_text(
                    source.read_text(encoding="utf-8"), encoding="utf-8"
                )
            manifest = dict(self.manifest)
            manifest["report_directory"] = str(report_root.relative_to(port_status.REPO_ROOT))
            counts = port_status.validate(manifest)
        self.assertTrue(
            any("tvos" in item and "no stored report" in item for item in counts["drift"]),
            counts["drift"],
        )

    def test_provenance_rejects_deleting_an_established_fallback(self):
        # A port with no stored report became a supported state so that ADDING a port would not
        # have to begin by hand-authoring one. That must not also make removing an existing
        # fallback free: the site serves this file whenever the data branch is unreachable or
        # its newest report predates the contract, so deleting one turns a working port's whole
        # column unknown at exactly the moment the live data is missing.
        before = self.stored_reports()["tvos"]
        problems = port_status.provenance_problems("tvos", before, None, published=[before])
        self.assertEqual(1, len(problems), problems)
        self.assertIn("deleted", problems[0])

    def test_provenance_ignores_a_port_that_has_no_report_either_side(self):
        # Retiring a port drops it from the manifest, and the check never looks at it. This is
        # the port that has simply never published.
        self.assertEqual([], port_status.provenance_problems("freebsd", None, None, published=[]))

    def test_provenance_ignores_an_untouched_report(self):
        before = self.stored_reports()["android"]
        self.assertEqual([], port_status.provenance_problems("android", before, dict(before)))

    def publishable_report(self, port_id, **overrides):
        mapped = port_status.test_to_feature(self.manifest)
        # Only the tests this port is expected to report on. A test scoped to other
        # ports is not something this report is missing, and carrying it here would
        # make the fixture assert the opposite of the contract.
        tests = {
            test: {"status": "pass", "feature": mapped[test]}
            for test in port_status.tests_for_port(self.manifest, port_id)
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

    def test_the_two_macos_ports_are_registered_separately(self):
        # The native AppKit build and the legacy Mac Catalyst target are
        # different ports, not two builds of one, and the site has to be able to
        # say so. They were one id ("mac-native") for as long as Catalyst was the
        # only Mac build there was.
        ids = {port["id"] for port in self.manifest["ports"]}
        self.assertIn("macos", ids)
        self.assertIn("mac-catalyst", ids)
        self.assertNotIn(
            "mac-native",
            ids,
            "mac-native was retired rather than re-pointed: its published report "
            "measured a Catalyst binary, and re-using the id would have rendered "
            "those numbers under an AppKit label until the first AppKit run",
        )

        by_id = {port["id"]: port for port in self.manifest["ports"]}
        # Each has its own workflow, because backfill attributes reports by the
        # workflow file name.
        self.assertEqual("scripts-macos.yml", by_id["macos"]["workflow"])
        self.assertEqual("scripts-mac-catalyst.yml", by_id["mac-catalyst"]["workflow"])

        # And its own goldens. A Catalyst window is a UIWindowScene fed an
        # off-screen raster and an AppKit one owns a real CAMetalLayer, so the
        # two baselines can never converge into one set with a loose tolerance.
        goldens = self.manifest["golden_directories"]
        self.assertIn("scripts/macos/screenshots", goldens)
        self.assertIn("scripts/mac-catalyst/screenshots", goldens)

    def test_macos_support_row_discloses_both_targets(self):
        rows = {
            item.get("id"): item
            for item in port_status.read_json(
                port_status.REPO_ROOT / "docs/website/data/port_status_support.json"
            )["deployment_support"]
        }
        blob = json.dumps(rows.get("macos", {}))
        # The guard this pins exists so the site cannot claim "macOS" while
        # shipping only a Catalyst slice. It now has to name both, because a
        # reader choosing between them needs to know both exist.
        self.assertIn("AppKit", blob)
        self.assertIn("Catalyst", blob)

    def test_every_report_the_site_serves_is_renderable(self):
        # Only malformed. Drift is asserted against deliberately: a checked-in snapshot is a
        # copy of a real run, and a branch that registers a test makes every one of them
        # predate it. Demanding zero drift here is what turned "add a test" into "edit eleven
        # reports", and what made inventing a result the path of least resistance. What the
        # fallback owes the site is that Hugo can render it.
        for port in self.manifest["ports"]:
            report_path = port_status.REPO_ROOT / self.manifest["report_directory"] / (
                port["id"] + ".json"
            )
            # A port that has never published has no fallback, which is a
            # supported state rather than an unrenderable one -- the site shows
            # it as "No stored report". There is nothing here for Hugo to fail
            # to render.
            if not report_path.exists():
                continue
            with self.subTest(port=port["id"]):
                _, malformed = port_status.publishable_report_problems(
                    self.manifest, port["id"], port_status.read_json(report_path)
                )
                self.assertEqual([], malformed)


if __name__ == "__main__":
    unittest.main()
