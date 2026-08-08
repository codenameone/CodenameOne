#!/usr/bin/env python3
"""Validate and normalize HelloCodenameOne port conformance results."""

from __future__ import annotations

import argparse
import fnmatch
import json
import os
import re
import sys
from collections import Counter
from datetime import datetime, timedelta, timezone
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
DEFAULT_MANIFEST = REPO_ROOT / "docs/website/data/port_status.json"
SUPPLEMENT = REPO_ROOT / "docs/website/data/port_status_supplement.json"
SUPPORT = REPO_ROOT / "docs/website/data/port_status_support.json"
ENVIRONMENT = REPO_ROOT / "docs/website/data/port_status_environment.json"
RUNNER = REPO_ROOT / (
    "scripts/hellocodenameone/common/src/main/java/com/codenameone/"
    "examples/hellocodenameone/tests/Cn1ssDeviceRunner.java"
)
COMMON_SOURCES = REPO_ROOT / "scripts/hellocodenameone/common/src/main"
STRICT_GATE_FAILED = 10
# "accept" exit codes: the caller keeps the checked-in fallback for both, but
# only an unusable report is a defect worth failing the website build over.
ACCEPT_CONTRACT_DRIFT = 11
ACCEPT_UNUSABLE = 12

# Producers and this checker can disagree by a little without anything
# being wrong -- runner clocks drift, and a report is stamped slightly
# before it is published. An hour absorbs that; a skewed clock or a
# mistyped --generated-at lands far outside it.
FUTURE_STAMP_TOLERANCE = timedelta(hours=1)

START_RE = re.compile(r"suite starting test=([A-Za-z0-9_]+)")
FINISH_RE = re.compile(r"suite finished test=([A-Za-z0-9_]+)")
SKIP_RE = re.compile(r"test=([A-Za-z0-9_]+) status=SKIPPED(?: reason=([^\s]+))?")
ERROR_RE = re.compile(r"CN1SS:ERR:suite test=([A-Za-z0-9_]+)(?:\s+(.*))?$")
PERF_BENCH_RE = re.compile(
    r"CN1SS:PERF:benchmark id=([A-Za-z0-9-]+) duration_ns=(\d+) checksum=(-?\d+)"
)
PERF_SKIP_RE = re.compile(
    r"CN1SS:PERF:skipped id=([A-Za-z0-9-]+) reason=([A-Za-z0-9-]+)"
)
PERF_COMPLETE_RE = re.compile(
    r"CN1SS:PERF:complete benchmark_version=(\d+) checksum=(-?\d+)"
)


class ContractError(RuntimeError):
    pass


def read_json(path: Path) -> dict:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ContractError(f"Unable to read JSON from {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise ContractError(f"Expected a JSON object in {path}")
    return value


def registered_tests() -> list[str]:
    source = RUNNER.read_text(encoding="utf-8")
    marker = "private static final BaseTest[] DEFAULT_TEST_CLASSES"
    start = source.find(marker)
    if start < 0:
        raise ContractError(f"Unable to find DEFAULT_TEST_CLASSES in {RUNNER}")
    end = source.find("\n    };", start)
    if end < 0:
        raise ContractError(f"Unable to find the end of DEFAULT_TEST_CLASSES in {RUNNER}")
    names = re.findall(r"\bnew\s+([A-Za-z0-9_]+)\s*\(", source[start:end])

    # KotlinUiTest is installed through the public addTest() hook before the
    # default array. Find every statically named addTest call so that this path
    # is covered by the same no-unmapped-test gate.
    add_test_re = re.compile(r"Cn1ssDeviceRunner\.addTest\(\s*([A-Za-z0-9_]+)\s*\(")
    for path in sorted(COMMON_SOURCES.rglob("*")):
        if path.suffix not in {".java", ".kt"}:
            continue
        names.extend(add_test_re.findall(path.read_text(encoding="utf-8")))
    return names


def test_to_feature(manifest: dict) -> dict[str, str]:
    mapping: dict[str, str] = {}
    for feature in manifest.get("features", []):
        feature_id = feature.get("id")
        for test in feature.get("tests", []):
            if test in mapping:
                raise ContractError(
                    f"Test {test} is mapped to both {mapping[test]} and {feature_id}"
                )
            mapping[test] = feature_id
    return mapping


def screenshot_test(manifest: dict, output_name: str) -> str | None:
    matches = [
        item.get("test")
        for item in manifest.get("screenshot_mappings", [])
        if fnmatch.fnmatchcase(output_name, item.get("pattern", ""))
    ]
    if len(matches) > 1:
        raise ContractError(
            f"Screenshot {output_name} matches more than one test: {', '.join(matches)}"
        )
    return matches[0] if matches else None


def validate(manifest: dict) -> dict:
    problems: list[str] = []
    ports = manifest.get("ports", [])
    features = manifest.get("features", [])
    port_ids = [item.get("id") for item in ports]
    feature_ids = [item.get("id") for item in features]

    for label, values in (("port", port_ids), ("feature", feature_ids)):
        duplicates = sorted(k for k, count in Counter(values).items() if count > 1)
        if duplicates:
            problems.append(f"Duplicate {label} IDs: {', '.join(duplicates)}")
        if any(not value for value in values):
            problems.append(f"Every {label} needs a non-empty ID")

    try:
        mapped = test_to_feature(manifest)
    except ContractError as exc:
        problems.append(str(exc))
        mapped = {}

    registered = registered_tests()
    duplicate_registrations = sorted(
        name for name, count in Counter(registered).items() if count > 1
    )
    if duplicate_registrations:
        problems.append(
            "Tests registered more than once: " + ", ".join(duplicate_registrations)
        )

    performance_tests = manifest.get("performance_tests", [])
    duplicate_performance_tests = sorted(
        name for name, count in Counter(performance_tests).items() if count > 1
    )
    if duplicate_performance_tests:
        problems.append("Performance tests registered more than once: " + ", ".join(duplicate_performance_tests))
    missing = sorted(set(registered) - set(mapped) - set(performance_tests))
    extra = sorted((set(mapped) | set(performance_tests)) - set(registered))
    if missing:
        problems.append("Registered tests without a feature: " + ", ".join(missing))
    if extra:
        problems.append("Manifest tests not registered by the suite: " + ", ".join(extra))
    performance_benchmarks = manifest.get("performance_benchmarks", [])
    if len(performance_benchmarks) != 10 or len(set(performance_benchmarks)) != 10:
        problems.append("The performance contract must define ten unique common workloads")

    for item in manifest.get("screenshot_mappings", []):
        if not item.get("pattern") or not item.get("test"):
            problems.append("Every screenshot mapping needs pattern and test")
        elif item["test"] not in mapped:
            problems.append(
                f"Screenshot pattern {item['pattern']} references unknown test {item['test']}"
            )

    golden_names: set[str] = set()
    missing_directories: list[str] = []
    for relative in manifest.get("golden_directories", []):
        directory = REPO_ROOT / relative
        if not directory.is_dir():
            missing_directories.append(relative)
            continue
        golden_names.update(path.stem for path in directory.glob("*.png"))
    if missing_directories:
        problems.append("Golden directories not found: " + ", ".join(missing_directories))

    for name in sorted(golden_names):
        try:
            owner = screenshot_test(manifest, name)
        except ContractError as exc:
            problems.append(str(exc))
            continue
        if owner is None:
            problems.append(f"Golden screenshot {name} is not mapped to a test")

    skipped_tests: set[str] = set()
    report_directory = manifest.get("report_directory")
    if report_directory:
        report_root = REPO_ROOT / report_directory
        for port_id in port_ids:
            report_path = report_root / f"{port_id}.json"
            try:
                report = read_json(report_path)
            except ContractError as exc:
                problems.append(str(exc))
                continue
            if report.get("schema_version") != manifest.get("schema_version"):
                problems.append(f"Stored report {report_path} has the wrong schema version")
            if report.get("port") != port_id:
                problems.append(f"Stored report {report_path} identifies port {report.get('port')}")
            report_tests = report.get("tests")
            if not isinstance(report_tests, dict):
                problems.append(f"Stored report {report_path} has no test result map")
                continue
            unknown_tests = sorted(set(report_tests) - set(mapped))
            if unknown_tests:
                problems.append(
                    f"Stored report {report_path} contains unknown tests: "
                    + ", ".join(unknown_tests)
                )
            missing_tests = sorted(set(mapped) - set(report_tests))
            if missing_tests:
                problems.append(
                    f"Stored report {report_path} is missing tests: "
                    + ", ".join(missing_tests)
                )
            for test, result in report_tests.items():
                if not isinstance(result, dict) or result.get("status") not in {
                    "pass", "fail", "skip", "not-run"
                }:
                    problems.append(f"Stored report {report_path} has an invalid result for {test}")
                elif result.get("status") == "skip":
                    skipped_tests.add(test)
            actual_summary = Counter(
                result.get("status")
                for result in report_tests.values()
                if isinstance(result, dict)
            )
            expected_summary = {
                key: actual_summary.get(key, 0)
                for key in ("pass", "fail", "skip", "not-run")
            }
            if report.get("summary") != expected_summary:
                problems.append(
                    f"Stored report {report_path} summary does not match its test results"
                )

    manual_feature_count = 0
    try:
        supplement = read_json(SUPPLEMENT)
    except ContractError as exc:
        problems.append(str(exc))
        supplement = {}

    skip_reason_tests = [
        item.get("test") for item in supplement.get("skip_reasons", [])
    ]
    duplicate_skip_reasons = sorted(
        test for test, count in Counter(skip_reason_tests).items() if count > 1
    )
    if duplicate_skip_reasons:
        problems.append("Duplicate skip errata: " + ", ".join(duplicate_skip_reasons))
    unknown_skip_reasons = sorted(set(skip_reason_tests) - set(mapped))
    if unknown_skip_reasons:
        problems.append(
            "Skip errata references unknown tests: "
            + ", ".join(unknown_skip_reasons)
        )
    missing_skip_reasons = sorted(skipped_tests - set(skip_reason_tests))
    if missing_skip_reasons:
        problems.append("Skipped tests without errata: " + ", ".join(missing_skip_reasons))
    for item in supplement.get("skip_reasons", []):
        required = ("test", "reason", "platform_support", "verification")
        if not all(item.get(field) for field in required):
            problems.append(
                "Every skip erratum needs test, reason, platform_support, and verification"
            )

    manual_features = supplement.get("features", [])
    manual_feature_count = len(manual_features)
    manual_ids = [item.get("id") for item in manual_features]
    duplicate_manual_ids = sorted(
        feature for feature, count in Counter(manual_ids).items() if count > 1
    )
    if duplicate_manual_ids:
        problems.append("Duplicate manual feature IDs: " + ", ".join(duplicate_manual_ids))
    for feature in manual_features:
        feature_id = feature.get("id") or "<unknown>"
        if not all(feature.get(field) for field in (
            "id", "category", "name", "description", "testing", "why_not_automated"
        )):
            problems.append(f"Manual feature {feature_id} is missing its description or test rationale")
        covered_ports: list[str] = []
        for coverage in feature.get("coverage", []):
            state = coverage.get("state")
            if state not in {"supported", "conditional", "fallback", "unavailable"}:
                problems.append(f"Manual feature {feature_id} has invalid state {state}")
            if not coverage.get("label") or not coverage.get("detail"):
                problems.append(
                    f"Manual feature {feature_id} has coverage without a label or detail"
                )
            covered_ports.extend(coverage.get("ports", []))
        duplicate_coverage = sorted(
            port for port, count in Counter(covered_ports).items() if count > 1
        )
        if duplicate_coverage:
            problems.append(
                f"Manual feature {feature_id} covers ports more than once: "
                + ", ".join(duplicate_coverage)
            )
        unknown_coverage = sorted(set(covered_ports) - set(port_ids))
        missing_coverage = sorted(set(port_ids) - set(covered_ports))
        if unknown_coverage:
            problems.append(
                f"Manual feature {feature_id} references unknown ports: "
                + ", ".join(unknown_coverage)
            )
        if missing_coverage:
            problems.append(
                f"Manual feature {feature_id} has no status for: "
                + ", ".join(missing_coverage)
            )

    try:
        support = read_json(SUPPORT)
        environment = read_json(ENVIRONMENT)
    except ContractError as exc:
        problems.append(str(exc))
        support = {}
        environment = {}

    deployment_rows = support.get("deployment_support", [])
    deployment_ids = [item.get("id") for item in deployment_rows]
    expected_deployments = {
        "android", "ios", "macos", "web", "linux", "windows", "watchos", "tvos"
    }
    if set(deployment_ids) != expected_deployments or len(deployment_ids) != len(expected_deployments):
        problems.append("Deployment support must define Android, iOS, macOS, Web, Linux, Windows, watchOS, and tvOS exactly once")
    for item in deployment_rows:
        if not all(item.get(field) for field in (
            "id", "platform", "architectures", "declared_range", "ci_evidence",
            "floor_evidence", "support"
        )):
            problems.append(f"Deployment support row {item.get('id')} is incomplete")
    deployment_by_id = {item.get("id"): item for item in deployment_rows}
    if "Catalyst" not in json.dumps(deployment_by_id.get("macos", {})):
        problems.append("macOS support must disclose its Mac Catalyst scope")
    if "x64" not in json.dumps(deployment_by_id.get("linux", {})) or "ARM64" not in json.dumps(deployment_by_id.get("linux", {})):
        problems.append("Linux support must declare both x64 and ARM64 evidence")
    linux_contract = json.dumps(deployment_by_id.get("linux", {}))
    if "glibc 2.28" not in linux_contract or "Alpine 3.20" not in linux_contract:
        problems.append("Linux support must declare the glibc 2.28 floor and Alpine 3.20 musl evidence")
    if not all(name in linux_contract for name in (
        "Ubuntu 20.04", "Debian 10", "RHEL/Rocky/AlmaLinux 8", "Fedora 30", "Linux Mint 20"
    )):
        problems.append("Linux support must map the glibc floor to named mainstream distributions")

    benchmark = support.get("benchmark", {})
    benchmark_rows = benchmark.get("rows", [])
    benchmark_ids = [item.get("id") for item in benchmark_rows]
    if benchmark_ids != performance_benchmarks or not all(
        item.get("name") and item.get("description") for item in benchmark_rows
    ):
        problems.append("Performance evidence must describe every common workload in contract order")
    if "absolute" not in benchmark.get("configuration", "").lower():
        problems.append("The performance methodology must identify its results as absolute values")

    browsers = environment.get("browsers", [])
    browser_ids = [item.get("id") for item in browsers]
    if browser_ids != ["chromium", "firefox", "webkit"]:
        problems.append("Browser evidence must contain Chromium, Firefox, and WebKit in that order")
    for browser in browsers:
        if browser.get("status") not in {"pending", "pass", "fail"} or not all(
            browser.get(field) for field in ("id", "name", "engine_version", "coverage")
        ):
            problems.append(f"Browser evidence for {browser.get('id')} is incomplete")
        if browser.get("status") != "pending" and browser.get("engine_version", "").startswith("Pending"):
            problems.append(f"Measured browser evidence for {browser.get('id')} has no engine version")

    if problems:
        raise ContractError("\n".join(problems))
    return {
        "ports": len(ports),
        "features": len(features),
        "tests": len(mapped),
        "performance_tests": len(performance_tests),
        "goldens": len(golden_names),
        "manual_features": manual_feature_count,
        "deployment_platforms": len(deployment_rows),
        "browser_engines": len(browsers),
    }


def add_reason(entry: dict, reason: str) -> None:
    reasons = entry.setdefault("reasons", [])
    if reason and reason not in reasons:
        reasons.append(reason)


def log_marker_test(manifest: dict, states: dict[str, dict], name: str) -> str | None:
    """The contract test a CN1SS log marker belongs to, or None for a test this
    port does not register.

    A marker names its test class most of the time, but a screenshot test reports
    a skip under its SCREENSHOT OUTPUT name -- ``test=VideoIODecodedFrames`` for
    VideoIODecodedFramesScreenshotTest, ``test=GoogleWebMap`` for
    GoogleWebMapScreenshotTest. Matching only the class name dropped every one of
    those skips on the floor, and the surrounding "suite starting" / "suite
    finished" pair then scored the test as a PASS: a skipped map test, a skipped
    VideoIO decode and a skipped watch dialog all rendered green. Resolve through
    the same screenshot mapping parse_comparisons uses.
    """
    if name in states:
        return name
    owner = screenshot_test(manifest, name)
    if owner in states:
        return owner
    return None


def parse_logs(manifest: dict, paths: list[Path], states: dict[str, dict]) -> bool:
    suite_finished = False
    for path in paths:
        if not path.is_file():
            continue
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            if "CN1SS:SUITE:FINISHED" in line:
                suite_finished = True
            match = START_RE.search(line)
            test = log_marker_test(manifest, states, match.group(1)) if match else None
            if test:
                states[test]["started"] = True
            match = FINISH_RE.search(line)
            test = log_marker_test(manifest, states, match.group(1)) if match else None
            if test:
                states[test]["finished"] = True
            match = SKIP_RE.search(line)
            if match:
                test = log_marker_test(manifest, states, match.group(1))
                if test is None:
                    # Silence here is what let a skip render as a pass, so an
                    # unattributable skip is a contract defect rather than a line
                    # to ignore: either the test is missing from the manifest or
                    # the marker names something no screenshot mapping covers.
                    raise ContractError(
                        f"Skip marker for {match.group(1)} in {path} is not mapped to a test"
                    )
                entry = states[test]
                entry["skipped"] = True
                add_reason(entry, match.group(2) or "reported-skip")
            match = ERROR_RE.search(line)
            test = log_marker_test(manifest, states, match.group(1)) if match else None
            if test:
                entry = states[test]
                entry["failed"] = True
                add_reason(entry, (match.group(2) or "").strip() or "suite-error")
    return suite_finished


def parse_performance(paths: list[Path], expected_benchmarks: list[str], binary_size: int | None) -> dict:
    benchmarks: dict[str, dict] = {}
    skipped: dict[str, str] = {}
    benchmark_version: int | None = None
    suite_checksum: int | None = None
    for path in paths:
        if not path.is_file():
            continue
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            match = PERF_BENCH_RE.search(line)
            if match:
                benchmark_id = match.group(1)
                if benchmark_id not in expected_benchmarks:
                    raise ContractError(f"Unknown performance benchmark {benchmark_id} in {path}")
                benchmarks[benchmark_id] = {
                    "duration_ns": int(match.group(2)),
                    "checksum": match.group(3),
                }
                skipped.pop(benchmark_id, None)
            match = PERF_SKIP_RE.search(line)
            if match:
                benchmark_id = match.group(1)
                if benchmark_id not in expected_benchmarks:
                    raise ContractError(
                        f"Unknown skipped performance benchmark {benchmark_id} in {path}"
                    )
                if benchmark_id not in benchmarks:
                    skipped[benchmark_id] = match.group(2)
            match = PERF_COMPLETE_RE.search(line)
            if match:
                benchmark_version = int(match.group(1))
                suite_checksum = int(match.group(2))
    missing = [
        item for item in expected_benchmarks
        if item not in benchmarks and item not in skipped
    ]
    status = "complete" if (
        not missing and benchmark_version is not None and suite_checksum is not None
    ) else "partial"
    return {
        "status": status,
        "benchmark_version": benchmark_version,
        "method": "minimum of five measured runs after three in-process warm-ups",
        "benchmarks": {item: benchmarks[item] for item in expected_benchmarks if item in benchmarks},
        "skipped": {item: skipped[item] for item in expected_benchmarks if item in skipped},
        "missing": missing,
        "suite_checksum": suite_checksum,
    }


def parse_comparisons(paths: list[Path], manifest: dict, states: dict[str, dict]) -> None:
    for path in paths:
        if not path.is_file():
            continue
        payload = read_json(path)
        for result in payload.get("results", []):
            if not isinstance(result, dict):
                continue
            output_name = result.get("test")
            if not output_name:
                continue
            owner = screenshot_test(manifest, output_name)
            if owner is None:
                raise ContractError(
                    f"Comparison output {output_name} in {path} is not mapped to a test"
                )
            entry = states[owner]
            entry["compared"] = True
            status = result.get("status", "unknown")
            if status == "equal":
                entry["comparison_passed"] = True
            else:
                entry["failed"] = True
                add_reason(entry, f"screenshot-{status}:{output_name}")


def normalize(
    manifest: dict,
    port_id: str,
    logs: list[Path],
    comparisons: list[Path],
    output: Path,
    run_url: str,
    commit: str,
    generated_at: str,
    binary_size: int | None = None,
) -> dict:
    port_ids = {item.get("id") for item in manifest.get("ports", [])}
    if port_id not in port_ids:
        raise ContractError(f"Unknown port ID: {port_id}")

    mapped = test_to_feature(manifest)
    states = {
        test: {
            "started": False,
            "finished": False,
            "skipped": False,
            "failed": False,
            "compared": False,
            "comparison_passed": False,
        }
        for test in mapped
    }
    suite_finished = parse_logs(manifest, logs, states)
    performance = parse_performance(
        logs,
        manifest.get("performance_benchmarks", []),
        binary_size,
    )
    parse_comparisons(comparisons, manifest, states)

    tests: dict[str, dict] = {}
    for test in sorted(states):
        raw = states[test]
        if raw["failed"]:
            status = "fail"
        elif raw["skipped"]:
            status = "skip"
        elif raw["started"] and raw["finished"]:
            status = "pass"
        elif raw["compared"] and raw["comparison_passed"]:
            status = "pass"
        else:
            status = "not-run"
        item = {"status": status, "feature": mapped[test]}
        if raw.get("reasons"):
            item["reasons"] = raw["reasons"]
        tests[test] = item

    counts = Counter(item["status"] for item in tests.values())
    report = {
        "schema_version": 1,
        "port": port_id,
        "generated_at": generated_at,
        "commit": commit,
        "run_url": run_url,
        "suite_finished": suite_finished,
        "summary": {key: counts.get(key, 0) for key in ("pass", "fail", "skip", "not-run")},
        "tests": tests,
        "performance": performance,
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return report


def strict_report_errors(report: dict) -> list[str]:
    errors: list[str] = []
    if not report.get("suite_finished"):
        errors.append("suite did not emit its completion marker")
    summary = report.get("summary")
    if not isinstance(summary, dict):
        raise ContractError("Expected report summary to be an object")

    counts: dict[str, int] = {}
    for key in ("pass", "fail", "skip", "not-run"):
        if key not in summary:
            raise ContractError(f"Report summary is missing required count {key!r}")
        value = summary[key]
        if isinstance(value, bool) or not isinstance(value, int) or value < 0:
            raise ContractError(
                f"Expected report summary {key!r} to be a non-negative integer"
            )
        counts[key] = value

    failed = counts["fail"]
    not_run = counts["not-run"]
    if failed:
        errors.append(f"{failed} test(s) failed")
    if not_run:
        errors.append(f"{not_run} test(s) did not run")
    return errors


def describe_workload_gap(seen: list[str], expected: list[str]) -> str:
    """Name what is missing or extra, not what happens to be fine.

    Printing the workloads that *are* covered leaves the reader to diff two
    ten-item lists by eye to find the one that is not.
    """
    absent = sorted(set(expected) - set(seen))
    unexpected = sorted(set(seen) - set(expected))
    parts = []
    if absent:
        parts.append("missing " + ", ".join(absent))
    if unexpected:
        parts.append("unexpected " + ", ".join(unexpected))
    return "; ".join(parts) if parts else "counts differ"


def publishable_report_problems(
    manifest: dict, port_id: str, report: dict
) -> tuple[list[str], list[str]]:
    """Decide whether a persisted report may replace the checked-in fallback.

    Returns (drift, malformed). Drift means the report is well formed but was
    produced against a different revision of the test contract, which happens
    for every port between the commit that registers a test and that port's
    next master run; the caller keeps the checked-in report and waits. Anything
    in malformed is a defect in the report or in the producer and must be loud:
    silently falling back for those is what lets a whole column of the public
    table rot into "stale" while the port itself is healthy.
    """
    drift: list[str] = []
    malformed: list[str] = []

    if report.get("schema_version") != manifest.get("schema_version"):
        malformed.append(
            f"schema version {report.get('schema_version')!r} is not "
            f"{manifest.get('schema_version')!r}"
        )
    if report.get("port") != port_id:
        malformed.append(f"report identifies port {report.get('port')!r}")
    generated_at = report.get("generated_at")
    if not isinstance(generated_at, str) or not generated_at:
        malformed.append("report has no generated_at timestamp")
    else:
        # Anything unparseable ("unknown") would sail through publication and
        # then break both the freshness sweep and the page's own time
        # rendering, so classify it as unusable here instead.
        try:
            stamp = datetime.fromisoformat(generated_at.replace("Z", "+00:00"))
        except ValueError:
            malformed.append(f"generated_at {generated_at!r} is not a timestamp")
        else:
            if stamp.tzinfo is None:
                malformed.append(f"generated_at {generated_at!r} has no time zone")
            elif stamp - datetime.now(timezone.utc) > FUTURE_STAMP_TOLERANCE:
                # A clock skewed far ahead poisons the data branch rather than
                # just looking odd: the page reads the report as permanently
                # fresh, and the sweep's lexical "is this newer" comparison
                # then refuses every later, correct timestamp. Nothing
                # downstream can recover from that, so refuse it at the gate.
                malformed.append(
                    f"generated_at {generated_at!r} is in the future"
                )

    mapped = test_to_feature(manifest)
    tests = report.get("tests")
    if not isinstance(tests, dict):
        malformed.append("report has no test result map")
        tests = {}
    else:
        missing = sorted(set(mapped) - set(tests))
        unknown = sorted(set(tests) - set(mapped))
        if missing:
            drift.append("report predates tests: " + ", ".join(missing))
        if unknown:
            drift.append("report carries retired tests: " + ", ".join(unknown))

    statuses = Counter()
    for test, result in tests.items():
        # isinstance before membership, not just membership. An unhashable
        # status -- a producer writing a list or an object -- raises TypeError
        # out of the `in` test, and main() catches only ContractError, so the
        # gate died with a status the sweep does not read as unusable and fell
        # back to an older report while finishing green.
        if not isinstance(result, dict) or not isinstance(result.get("status"), str) \
                or result["status"] not in {"pass", "fail", "skip", "not-run"}:
            malformed.append(f"invalid result for {test}")
            continue
        # The reason list is optional, but when present the feature template
        # calls len, range and hasPrefix on it. A producer emitting "reasons":
        # true or an object would pass the status check here, reach the data
        # branch, and then fail the Hugo build -- taking the whole site down
        # rather than being classified as one unusable report.
        reasons = result.get("reasons")
        if reasons is not None and (
            not isinstance(reasons, list)
            or not all(isinstance(item, str) for item in reasons)
        ):
            malformed.append(f"{test} reasons is not an array of strings")
            continue
        statuses[result["status"]] += 1
    expected_summary = {
        key: statuses.get(key, 0) for key in ("pass", "fail", "skip", "not-run")
    }
    # Checked even under drift. The summary counts the results the report
    # actually carries, so it stays self-consistent whether or not the report
    # predates a test -- suppressing this whenever any drift was seen let a
    # genuinely malformed report be filed as mere drift and fall back quietly,
    # which is the outcome the loud/quiet split exists to avoid.
    # Types before values. Python treats True as equal to 1, so a summary count
    # serialized as a JSON boolean compared equal to a genuine count of one and
    # sailed through the equality below -- Android's "skip": 1 becoming "skip":
    # true is accepted, published, and rendered by Hugo as "true skipped".
    summary = report.get("summary")
    if not isinstance(summary, dict):
        malformed.append("report has no summary")
    elif any(
        isinstance(summary.get(key), bool)
        or not isinstance(summary.get(key), int)
        or summary.get(key) < 0
        for key in ("pass", "fail", "skip", "not-run")
    ):
        malformed.append("summary counts are not non-negative integers")
    elif summary != expected_summary:
        malformed.append("summary does not match the test results")

    expected_benchmarks = manifest.get("performance_benchmarks", [])
    performance = report.get("performance")
    if not isinstance(performance, dict):
        malformed.append("report has no performance section")
        return drift, malformed
    # CommonWorkloadBenchmarkTest runs late in Cn1ssDeviceRunner, so a suite that
    # crashed or timed out never reaches it and its performance section is
    # partial by construction. Rejecting the report for that would throw away the
    # very evidence the page needs -- the fail / not-run counts -- and leave the
    # table serving the last green run, which is the failure-masked-as-pass shape
    # this gate exists to prevent. Completeness is therefore only required of a
    # suite that actually finished; a partial section simply is not presentable
    # as performance. Structural defects below stay loud either way, because
    # those are producer bugs whatever the suite did.
    # An actual boolean, not anything truthy. bool() accepted the string "false"
    # and the integer 1 as a completed suite, and Hugo reads the same value as
    # truthy in port-status-port-state.html -- so a report with no failures but a
    # pile of not-run tests rendered a green "Suite completed" card.
    suite_finished_raw = report.get("suite_finished")
    if not isinstance(suite_finished_raw, bool):
        malformed.append(
            f"suite_finished is {type(suite_finished_raw).__name__}, not a boolean"
        )
        suite_finished_raw = False
    suite_finished = suite_finished_raw

    # Validated before anything joins or iterates it. A producer emitting
    # "missing": true or a number raised TypeError out of the join below, and
    # main() only catches ContractError -- so the gate crashed instead of
    # answering ACCEPT_UNUSABLE, the sweep saw a status it does not treat as
    # unusable, fell back to an older report and finished green.
    declared_missing = performance.get("missing")
    if declared_missing is None:
        declared_missing = []
    if not isinstance(declared_missing, list) or not all(
        isinstance(item, str) for item in declared_missing
    ):
        malformed.append("performance missing list is not an array of workload names")
        declared_missing = []

    # Validated for every report, not only finished ones. port-status.html does
    # `eq .status "complete"`, and Hugo aborts the whole site build with an
    # incompatible-types error when that compares a map against a string -- so a
    # malformed status on an UNFINISHED report used to skip validation entirely
    # (the workload keys still accounted for) and take the build down.
    perf_status = performance.get("status")
    if perf_status not in ("complete", "partial"):
        malformed.append(f"performance status is {perf_status!r}")
    if suite_finished:
        if perf_status != "complete":
            malformed.append(f"performance run is {perf_status!r}")
        if declared_missing:
            malformed.append(
                "performance workloads never reported: " + ", ".join(declared_missing)
            )

    benchmarks = performance.get("benchmarks")
    skipped = performance.get("skipped")
    if skipped is None:
        # Absent is tolerated; a wrong type is not. `or {}` coerced a falsy
        # non-dict -- "skipped": [] -- into {} and walked it straight past the
        # isinstance check below.
        skipped = {}
    if not isinstance(benchmarks, dict) or not isinstance(skipped, dict):
        malformed.append("performance results are not objects")
        return drift, malformed

    # A workload is measured or skipped, never both. Unioning the keys let a
    # report claim both for the same workload and still look complete, which
    # hides the producer bug that wrote it twice.
    both = sorted(set(benchmarks) & set(skipped))
    if both:
        malformed.append(
            "performance workloads both measured and skipped: " + ", ".join(both)
        )

    # A port may legitimately skip a workload (the iOS simulator skips the
    # GC-footprint workloads); measured plus skipped has to cover the contract.
    accounted = sorted(set(benchmarks) | set(skipped))
    if suite_finished:
        if accounted != sorted(expected_benchmarks):
            malformed.append(
                "performance workloads do not match the contract: "
                + describe_workload_gap(accounted, expected_benchmarks)
            )
    else:
        # A crashed suite is allowed to leave workloads unrun, but not to lose
        # them silently: normalize computes `missing` as the contract minus what
        # was measured or skipped, so measured + skipped + missing still has to
        # name every workload. Dropping that check entirely would let a
        # structurally broken section through unnoticed.
        covered = sorted(set(accounted) | set(declared_missing))
        if covered != sorted(expected_benchmarks):
            malformed.append(
                "performance workloads unaccounted for: "
                + describe_workload_gap(covered, expected_benchmarks)
            )
        # A report that names workloads it never ran cannot also call the run
        # complete. normalize never writes that pair, but nothing downstream
        # re-derives the status: port-status.html renders the timings of any
        # benchmark whose status is "complete", so the partial set that a
        # crashed suite did manage to measure would be presented as a finished
        # measurement run. accounted | missing can still cover the contract, so
        # the check above does not catch it.
        if declared_missing and performance.get("status") != "partial":
            malformed.append(
                "performance run is "
                f"{performance.get('status')!r} but declares missing workloads: "
                + ", ".join(declared_missing)
            )
    for name, measurement in benchmarks.items():
        duration = measurement.get("duration_ns") if isinstance(measurement, dict) else None
        if isinstance(duration, bool) or not isinstance(duration, int) or duration < 0:
            malformed.append(f"{name} has no measured duration")
    for name, reason in skipped.items():
        if not isinstance(reason, str) or not reason:
            malformed.append(f"skipped workload {name} has no reason")

    return drift, malformed


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("validate", help="validate feature and screenshot coverage")

    accept_parser = subparsers.add_parser(
        "accept",
        help="decide whether a persisted report may replace the checked-in fallback",
    )
    accept_parser.add_argument("--port", required=True)
    accept_parser.add_argument("--report", required=True, type=Path)

    normalize_parser = subparsers.add_parser("normalize", help="write a normalized port report")
    normalize_parser.add_argument("--port", required=True)
    normalize_parser.add_argument("--log", action="append", type=Path, default=[])
    normalize_parser.add_argument("--compare", action="append", type=Path, default=[])
    normalize_parser.add_argument("--output", required=True, type=Path)
    normalize_parser.add_argument("--run-url", default=os.environ.get("GITHUB_RUN_URL", ""))
    normalize_parser.add_argument("--commit", default=os.environ.get("GITHUB_SHA", ""))
    normalize_parser.add_argument("--generated-at", default=utc_now())
    normalize_parser.add_argument("--binary-size", type=int)
    normalize_parser.add_argument(
        "--fail-on-test-problems",
        action="store_true",
        help="return nonzero after writing the report if tests fail, do not run, or the suite is incomplete",
    )
    return parser


def main() -> int:
    args = build_parser().parse_args()
    try:
        manifest = read_json(args.manifest)
        counts = validate(manifest)
        if args.command == "validate":
            print(
                "Port status contract is valid: "
                f"{counts['tests']} tests, {counts['features']} features, "
                f"{counts['ports']} ports, {counts['goldens']} golden names."
            )
            return 0
        if args.command == "accept":
            drift, malformed = publishable_report_problems(
                manifest, args.port, read_json(args.report)
            )
            for problem in malformed:
                print(f"port-status: {args.port} report is unusable: {problem}", file=sys.stderr)
            for problem in drift:
                print(f"port-status: {args.port} {problem}", file=sys.stderr)
            if malformed:
                return ACCEPT_UNUSABLE
            if drift:
                return ACCEPT_CONTRACT_DRIFT
            print(f"{args.port} report accepted.")
            return 0
        report = normalize(
            manifest=manifest,
            port_id=args.port,
            logs=args.log,
            comparisons=args.compare,
            output=args.output,
            run_url=args.run_url,
            commit=args.commit,
            generated_at=args.generated_at,
            binary_size=args.binary_size,
        )
        print(
            f"Wrote {args.output}: "
            + ", ".join(f"{key}={value}" for key, value in report["summary"].items())
        )
        if args.fail_on_test_problems:
            strict_errors = strict_report_errors(report)
            if strict_errors:
                print(
                    "port-status strict gate: " + "; ".join(strict_errors),
                    file=sys.stderr,
                )
                return STRICT_GATE_FAILED
        return 0
    except ContractError as exc:
        print(f"port-status: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
