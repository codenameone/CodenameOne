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


def feature_test_entries(feature: dict):
    """Yield ``(test name, ports or None)`` for one feature.

    A feature lists its tests either as plain names, which every port is expected to
    run, or as ``{"test": ..., "ports": [...]}`` for a test that only applies to some
    of them. The scoped form exists for a capability a port does not have -- a
    windowing system, say. Without it such a test is absent from that port's report
    forever, which the coverage gate reads as a test the port dropped, and the only
    way to quiet that is a row of skips on the public table inviting the reader to
    count a capability the port was never asked for as something it failed to do.
    """
    for entry in feature.get("tests", []):
        if isinstance(entry, str):
            yield entry, None
            continue
        if not isinstance(entry, dict) or not entry.get("test"):
            raise ContractError(
                f"Feature {feature.get('id')} has a test entry that is neither a name "
                f"nor an object with a test: {entry!r}"
            )
        ports = entry.get("ports")
        if not isinstance(ports, list) or not ports:
            raise ContractError(
                f"Scoped test {entry['test']} must list the ports it applies to"
            )
        yield entry["test"], set(ports)


def test_to_feature(manifest: dict) -> dict[str, str]:
    mapping: dict[str, str] = {}
    for feature in manifest.get("features", []):
        feature_id = feature.get("id")
        for test, _ports in feature_test_entries(feature):
            if test in mapping:
                raise ContractError(
                    f"Test {test} is mapped to both {mapping[test]} and {feature_id}"
                )
            mapping[test] = feature_id
    return mapping


def test_scopes(manifest: dict) -> dict[str, set[str] | None]:
    """Map every test to the ports it applies to, or None when that is all of them."""
    scopes: dict[str, set[str] | None] = {}
    for feature in manifest.get("features", []):
        for test, ports in feature_test_entries(feature):
            scopes[test] = ports
    return scopes


def tests_for_port(manifest: dict, port_id: str) -> set[str]:
    """The tests a given port is expected to report on."""
    return {
        test
        for test, ports in test_scopes(manifest).items()
        if ports is None or port_id in ports
    }


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

    try:
        for test, scoped_ports in test_scopes(manifest).items():
            if scoped_ports is None:
                continue
            unknown_ports = sorted(scoped_ports - set(port_ids))
            if unknown_ports:
                problems.append(
                    f"Test {test} is scoped to unknown ports: " + ", ".join(unknown_ports)
                )
    except ContractError as exc:
        problems.append(str(exc))

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

    # The checked-in reports are a snapshot of what CI measured, not a second
    # copy of the contract. Requiring them to carry exactly the manifest's test
    # set made every test-adding PR hand-edit eleven files, and the cheapest way
    # to satisfy that was to invent a result -- twelve "pass" entries reached
    # master attributed to runs that never executed the test. Classify them with
    # the same drift / malformed split publication uses: a snapshot that predates
    # a test is drift and is reported, never fatal; a snapshot nothing can render
    # is still a defect.
    skipped_tests, snapshot_drift, snapshot_malformed = stored_report_problems(manifest)
    problems.extend(snapshot_malformed)

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
    # Only tests the contract still defines. A snapshot taken before a test was
    # retired still carries its skip, and demanding an erratum for something
    # nobody can run again would be unfixable except by editing the snapshot.
    missing_skip_reasons = sorted((skipped_tests & set(mapped)) - set(skip_reason_tests))
    if missing_skip_reasons:
        problems.append("Skipped tests without errata: " + ", ".join(missing_skip_reasons))
    for item in supplement.get("skip_reasons", []):
        required = ("test", "reason", "platform_support", "verification")
        if not all(item.get(field) for field in required):
            problems.append(
                "Every skip erratum needs test, reason, platform_support, and verification"
            )
        # A reason code with no prefix documents everything. Both matchers ask
        # whether the reason starts with it, and every string starts with the
        # empty one -- so an erratum that lost this field by a typo would turn
        # any future skip of that test green, on the nightly gate and on the
        # page alike, which is the opposite of what writing an erratum is for.
        for code in item.get("reason_codes") or []:
            prefix = code.get("prefix")
            if not isinstance(prefix, str) or not prefix:
                problems.append(
                    f"Skip erratum {item.get('test')} has a reason code with no prefix"
                )
            # Deliberately not named `ports`: that is the manifest's port list,
            # in scope for the whole of validate(), and rebinding it here left
            # the returned port count reading whichever erratum happened to be
            # last. The counts test caught it, which is what it is for.
            code_ports = code.get("ports")
            if code_ports is not None and (
                not isinstance(code_ports, list)
                or not code_ports
                or any(port not in port_ids for port in code_ports)
            ):
                problems.append(
                    f"Skip erratum {item.get('test')} has a reason code naming unknown ports"
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
        "drift": snapshot_drift,
        "ports": len(ports),
        "features": len(features),
        "tests": len(mapped),
        "performance_tests": len(performance_tests),
        "goldens": len(golden_names),
        "manual_features": manual_feature_count,
        "deployment_platforms": len(deployment_rows),
        "browser_engines": len(browsers),
    }


def stored_report_problems(manifest: dict) -> tuple[set[str], list[str], list[str]]:
    """Classify the checked-in fallback reports.

    Returns (skipped tests, drift, malformed). The reports under
    ``report_directory`` are produced by the port workflows and refreshed from
    the data branch before Hugo runs; nothing about a pull request is supposed
    to touch them. Read them exactly the way publication reads a persisted
    report, so "this snapshot predates a test the branch just registered" is the
    ordinary, expected state it already is everywhere else in this pipeline
    rather than a build failure a human resolves by inventing a result.
    """
    skipped: set[str] = set()
    drift: list[str] = []
    malformed: list[str] = []
    report_directory = manifest.get("report_directory")
    if not report_directory:
        return skipped, drift, malformed
    root = REPO_ROOT / report_directory
    for port in manifest.get("ports", []):
        port_id = port.get("id")
        if not port_id:
            continue
        path = root / f"{port_id}.json"
        if not path.is_file():
            # A port with no published report renders as "No stored report" on
            # every one of its cells, which is what is true of a port CI has
            # never heard from. Demanding a file here is what made adding a port
            # start by hand-authoring one, and a hand-authored report is the
            # thing this whole contract is trying to stop existing.
            drift.append(f"{port_id}: no stored report yet")
            continue
        try:
            report = read_json(path)
        except ContractError as exc:
            malformed.append(str(exc))
            continue
        port_drift, port_malformed = publishable_report_problems(manifest, port_id, report)
        drift.extend(f"{port_id}: {item}" for item in port_drift)
        malformed.extend(f"{port_id}: {item}" for item in port_malformed)
        tests = report.get("tests")
        if not isinstance(tests, dict):
            continue
        for name, result in tests.items():
            if isinstance(result, dict) and result.get("status") == "skip":
                skipped.add(name)
    return skipped, drift, malformed


def report_stamp(report: dict) -> datetime | None:
    raw = report.get("generated_at")
    if not isinstance(raw, str) or not raw:
        return None
    try:
        stamp = datetime.fromisoformat(raw.replace("Z", "+00:00"))
    except ValueError:
        return None
    return stamp if stamp.tzinfo else None


def skip_is_documented(supplement: dict, port_id: str, test: str, reasons: list) -> bool:
    """The page's rule for a green documented skip, applied to a report.

    Mirrors port-status-feature-status.html deliberately: an erratum documents a
    skip only when it names the test AND, where it lists reason codes, every
    reason the run gave matches one of them from a port that code applies to.
    Matching on the test name alone would let any future skip of a named test
    read as documented -- an encoder that regressed would render green under an
    erratum written about a simulator.
    """
    for item in supplement.get("skip_reasons", []):
        if item.get("test") != test:
            continue
        codes = item.get("reason_codes")
        if not codes:
            return True
        if not reasons:
            continue
        if all(
            any(
                code.get("prefix")
                and (not code.get("ports") or port_id in code["ports"])
                and isinstance(reason, str)
                and reason.startswith(code["prefix"])
                for code in codes
            )
            for reason in reasons
        ):
            return True
    return False


def coverage_problems(
    manifest: dict,
    reports: dict[str, dict],
    contracts: dict[str, set[str]] | None = None,
) -> list[str]:
    """Hold the *published* reports to "every registered test runs on every port".

    This is the gate that used to live, badly, in the checked-in snapshots. Two
    rules, both decidable from the reports themselves:

    ``not-run`` is always a defect. The suite reached that port, the test was in
    its contract, and nothing reported back.

    A test absent from a report is normally just a run that predates it -- the
    port has not merged past the commit that registered the test yet. It becomes
    a defect the moment some *older* report carries that test: a run that
    happened earlier already knew about it, so a later run that does not is a
    test the port has dropped rather than one it has not reached. No history
    lookup and no grace period to tune; the reports date themselves.

    That comparison is blind to a test missing from *every* report, because then
    no report is the older one that proves it existed -- and a test nothing runs
    anywhere is the worst version of the failure this gate is for, not a
    tolerable one. ``contracts`` closes it: the set of tests each report's own
    commit defined, which the caller reads at that commit. A report whose
    contract already listed the test has no excuse for omitting it, whatever the
    other ports did. Offline callers pass None and keep the weaker comparison.

    A ``skip`` is the one permitted exception, and only with an erratum that
    accounts for the reason the run actually gave. Reading skips out of the
    checked-in fallbacks instead -- which is all validate() can see -- would let
    a port start skipping a test, publish it, and pass this gate, with the
    undocumented skip surfacing later as a failed website build rather than as
    the name of the port that started skipping.
    """
    problems: list[str] = []
    supplement = read_json(SUPPLEMENT)
    mapped = test_to_feature(manifest)
    stamps = {port: report_stamp(report) for port, report in reports.items()}

    # The earliest run that proves a test was in the contract. Anything younger
    # than this has no excuse for missing it.
    known_since: dict[str, datetime] = {}
    for port, report in reports.items():
        stamp = stamps.get(port)
        tests = report.get("tests")
        if stamp is None or not isinstance(tests, dict):
            continue
        for name in tests:
            if name in mapped and (name not in known_since or stamp < known_since[name]):
                known_since[name] = stamp

    for port in sorted(reports):
        report = reports[port]
        tests = report.get("tests")
        if not isinstance(tests, dict):
            problems.append(f"{port}: report has no test result map")
            continue
        # `name in mapped`, the same filter the skip check below uses. Scanning
        # every entry meant a report that predates a test's retirement and
        # carries it as not-run failed this gate over a test nobody can run any
        # more -- and the same report is tolerated as drift everywhere else, so
        # the sweep stayed red until that port happened to rerun.
        unrun = sorted(
            name
            for name, result in tests.items()
            if isinstance(result, dict)
            and result.get("status") == "not-run"
            and name in mapped
        )
        if unrun:
            problems.append(f"{port}: reported no result for " + ", ".join(unrun))
        undocumented = sorted(
            name
            for name, result in tests.items()
            if isinstance(result, dict)
            and result.get("status") == "skip"
            and name in mapped
            and not skip_is_documented(
                supplement, port, name, result.get("reasons") or []
            )
        )
        if undocumented:
            problems.append(
                f"{port}: skipped without an erratum that explains the reason given: "
                + ", ".join(undocumented)
            )
        stamp = stamps.get(port)
        if stamp is None:
            problems.append(f"{port}: report has no usable generated_at")
            continue
        own_contract = (contracts or {}).get(port)
        # Scoped to this port. A test that does not apply here -- a windowed
        # baseline on a port with no windowing system -- is absent from every
        # report this port will ever publish, so without this the first desktop
        # run to carry it would make every other port look like it dropped it.
        absent = tests_for_port(manifest, port) - set(tests)
        dropped = sorted(
            name
            for name in absent
            if name in known_since and known_since[name] < stamp
        )
        if dropped:
            problems.append(
                f"{port}: ran at {report.get('generated_at')} without "
                + ", ".join(dropped)
                + ", which an earlier run on another port already covered"
            )
        if own_contract is not None:
            unreported = sorted(name for name in absent - set(dropped) if name in own_contract)
            if unreported:
                problems.append(
                    f"{port}: ran at {report.get('commit') or 'an unknown commit'}, which "
                    "defines " + ", ".join(unreported) + ", and reported nothing for them"
                )
    return problems


PROVENANCE_FIELDS = ("generated_at", "commit", "run_url")
# What has to be *new* before changed findings are believable. `commit` is not
# among them: a port legitimately re-runs the same master commit, and its second
# run is a different run. `run_url` is the run's identity and `generated_at` is
# when it reported; a real snapshot carries new values for both.
RUN_IDENTITY_FIELDS = ("generated_at", "run_url")
# A run URL names a run on this forge. Shape alone proves nothing about whether
# the run happened -- the data branch below is what establishes that -- but it
# costs nothing and rejects a field filled in with a placeholder.
RUN_URL_RE = re.compile(
    r"^https://github\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/actions/runs/\d+(?:/[A-Za-z0-9_/-]*)?$"
)


def provenance_problems(
    port_id: str,
    before: dict | None,
    after: dict | None,
    published: list[dict] | None = None,
) -> list[str]:
    """Refuse a hand-edited report.

    A report says "at this commit, this run, at this time, this is what the port
    did". Editing what it did while leaving that provenance alone does not
    correct the record, it forges it -- which is how twelve tests came to be
    published as passing on ports that had never executed them. Changing the
    findings is legitimate only as part of taking a new snapshot, and a new
    snapshot carries a new stamp.

    Everything except the provenance fields counts as a finding, not just the
    test map: ``performance`` is the ten benchmark durations the page publishes
    as measurements of that run, and ``suite_finished`` is what makes a port card
    say the suite completed. Naming a subset here would leave the numbers most
    worth doubting -- the ones nobody can check by reading them -- as the one
    thing a branch could still rewrite in place.

    ``published`` is what turns this from a shape check into a verification. It
    is every version of this port's report the ``port-status-data`` branch has
    held recently, and a changed snapshot has to *be* one of them -- which it
    will be, because the only way to refresh one is to copy what CI published.
    A new ``run_url`` and stamp are then not two strings a branch can invent;
    they have to belong to a report that a run really produced, and producing one
    needs the write access to the data branch that only the publish workflows
    have. Pass None when the branch could not be reached: unverifiable is not the
    same as forged, and failing a pull request because a fetch flaked would teach
    people to route around this.
    """
    if after is None:
        if before is None:
            return []
        # Absent because it never existed and absent because someone removed it
        # are different things, and only the first is harmless. The site serves
        # this file whenever the data branch cannot be reached or its newest
        # report predates the contract, so deleting one turns an established
        # port's whole column unknown at exactly the moment the live data is
        # missing -- which is the moment the fallback exists for. Retiring a
        # port is still fine: drop it from the manifest and this check never
        # looks at it.
        return [
            f"{port_id}: the checked-in report was deleted. It is the fallback "
            "the site serves when the data branch is unreachable, so an "
            "established port would render as unknown. Only a port that has "
            "never published needs no report."
        ]

    if before == after:
        return []

    advice = (
        "A checked-in report is a copy of what CI put on the port-status-data "
        "branch, so refresh it from there rather than editing it; adding a test "
        "needs no report change at all."
    )

    # Checked on any change to the field, not only alongside changed findings.
    if before is None or before.get("run_url") != after.get("run_url"):
        run_url = after.get("run_url")
        if not isinstance(run_url, str) or not RUN_URL_RE.match(run_url):
            return [
                f"{port_id}: run_url {run_url!r} does not name a workflow run. "
                + advice
            ]

    # Every identity field, not any provenance field. Accepting a change to one
    # of the three left the gate open to the easier version of the same forgery:
    # invent a result, type today's date into `generated_at`, and leave the
    # `commit` and `run_url` still naming the run that never produced it. A
    # snapshot that came from a run has a new run behind it.
    findings = tuple(
        {key: value for key, value in (report or {}).items() if key not in PROVENANCE_FIELDS}
        for report in (before, after)
    )
    if before is not None and findings[0] != findings[1] and not all(
        before.get(field) != after.get(field) and after.get(field)
        for field in RUN_IDENTITY_FIELDS
    ):
        changed = sorted(
            key
            for key in set(findings[0]) | set(findings[1])
            if findings[0].get(key) != findings[1].get(key)
        )
        stale = sorted(
            field
            for field in RUN_IDENTITY_FIELDS
            if not (before.get(field) != after.get(field) and after.get(field))
        )
        return [
            f"{port_id}: {', '.join(changed)} changed without a new run behind it -- "
            f"{', '.join(stale)} still name{'s' if len(stale) == 1 else ''} the "
            "previous one. These reports are CI output, and a branch never needs to "
            "edit one. Adding a test needs no report change at all; each port picks "
            "it up on its next master run."
        ]

    # Asked of *any* change, including one that touches only the provenance
    # fields. Retyping generated_at alone changes no finding, and the page reads
    # that field to decide whether a column is stale -- so the edit nothing else
    # objected to was the one that made a port which had stopped reporting look
    # like it was still running.
    if published is None:
        return []
    if any(candidate == after for candidate in published):
        return []
    if not published:
        return [
            f"{port_id}: this port has never published a report, so there is "
            "nothing for a checked-in one to be a copy of. Leave it out -- every "
            "cell reads 'No stored report' until the port's first run, which is "
            "what is true. " + advice
        ]
    return [
        f"{port_id}: this report is not a version the port-status-data branch "
        "ever held. " + advice
    ]


def documented_skip_goldens(
    manifest: dict,
    port_id: str,
    logs: list[Path],
    reference: Path,
    comparisons: list[Path] | None = None,
) -> tuple[list[str], list[str]]:
    """Golden names whose test reported a documented skip, and why.

    The screenshot count guard fails a run when a golden is not re-produced,
    because a test that hangs or crashes leaves no per-test record and the
    missing file is the only evidence there is. A test that prints
    ``status=SKIPPED reason=...`` is the opposite of that: it left a record, and
    the errata already say the reason is expected on this port. GoogleWebMap
    skips on android and both iOS renderers when the Google Maps tiles never
    load, which is a network the run cannot reach rather than anything about the
    port -- and the guard failed the whole job over it anyway, so that skip path
    could never actually succeed on a port that owns a golden.

    Only a skip that is *documented for this port* counts. Silence still fails,
    an unexplained skip still fails, and a reason code written about another
    port still fails, which is what keeps this from being a hole.

    And only goldens that are actually absent. The caller subtracts this count
    from the number of uncovered goldens, so naming one the run did compare
    would subtract a golden nothing was missing -- and the spare subtraction
    would then hide a genuinely uncovered golden belonging to some other test.
    A test that owns several screenshots and captures a few before skipping is
    exactly that case.
    """
    supplement = read_json(SUPPLEMENT)
    skipped: dict[str, list[str]] = {}
    for path in logs:
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        for line in text.splitlines():
            match = SKIP_RE.search(line)
            if not match:
                continue
            name, reason = match.group(1), match.group(2)
            owner = name if name in test_to_feature(manifest) else screenshot_test(manifest, name)
            if owner:
                skipped.setdefault(owner, []).append(reason or "")

    compared: set[str] = set()
    for path in comparisons or []:
        if not path.is_file():
            continue
        try:
            payload = read_json(path)
        except ContractError:
            continue
        for result in payload.get("results", []):
            if isinstance(result, dict) and result.get("status") in {"equal", "different"}:
                name = result.get("test")
                if name:
                    compared.add(name)

    accounted: list[str] = []
    notes: list[str] = []
    if not reference.is_dir():
        return accounted, notes
    for golden in sorted(reference.glob("*.png")):
        owner = screenshot_test(manifest, golden.stem)
        if owner is None or owner not in skipped or golden.stem in compared:
            continue
        reasons = [reason for reason in skipped[owner] if reason]
        if skip_is_documented(supplement, port_id, owner, reasons):
            accounted.append(golden.stem)
            notes.append(f"{golden.stem}: {owner} skipped ({', '.join(reasons)})")
    return accounted, notes


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
            elif status == "missing_actual" and entry["skipped"]:
                # The test said, in the log, that it could not run here, and the reason it gave is
                # registered in the supplement -- the contract check refuses a skip without one.
                # A test that skips produces no screenshot by definition, so counting the absence
                # as a failure made "we could not test this" and "this is broken" the same red,
                # which is the one distinction the gate exists to draw. Only this status is
                # forgiven, and only for a test that skipped: a screenshot that was produced and
                # differs still fails, skip or no skip.
                add_reason(entry, f"screenshot-absent-because-skipped:{output_name}")
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
        for test in tests_for_port(manifest, port_id)
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

    # Scoped to this port: a test that does not apply here is not something the
    # report predates, it is something the report is right never to carry.
    expected = tests_for_port(manifest, port_id)
    tests = report.get("tests")
    if not isinstance(tests, dict):
        malformed.append("report has no test result map")
        tests = {}
    else:
        missing = sorted(expected - set(tests))
        unknown = sorted(set(tests) - expected)
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

    coverage_parser = subparsers.add_parser(
        "coverage",
        help="hold published reports to running every registered test on every port",
    )
    coverage_parser.add_argument(
        "--reports",
        required=True,
        type=Path,
        help="directory of published <port>.json reports",
    )
    coverage_parser.add_argument(
        "--contracts",
        type=Path,
        help=(
            "directory of <port>.json manifests read at each report's own commit; "
            "omit when they cannot be fetched"
        ),
    )

    provenance_parser = subparsers.add_parser(
        "provenance",
        help="refuse a report whose results were edited without a new run",
    )
    provenance_parser.add_argument(
        "--base",
        required=True,
        type=Path,
        help="directory holding the base revision's reports",
    )
    provenance_parser.add_argument(
        "--published",
        type=Path,
        help=(
            "directory of <port>/*.json holding every version of each report the "
            "data branch has held recently; omit when it could not be fetched"
        ),
    )

    skips_parser = subparsers.add_parser(
        "documented-skips",
        help="goldens whose test reported a skip this port's errata explain",
    )
    skips_parser.add_argument("--port", required=True)
    skips_parser.add_argument("--log", action="append", type=Path, default=[])
    skips_parser.add_argument("--reference", required=True, type=Path)
    skips_parser.add_argument("--compare", action="append", type=Path, default=[])

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
            for item in counts["drift"]:
                # Information, not a warning to be silenced. Every port reaches
                # a newly registered test on its next master run, and the page
                # shows the gap as "not run" until it does.
                print(f"port-status: checked-in snapshot {item}")
            return 0
        if args.command == "documented-skips":
            accounted, notes = documented_skip_goldens(
                manifest, args.port, args.log, args.reference, args.compare
            )
            for note in notes:
                print(f"port-status: documented skip accounts for {note}", file=sys.stderr)
            print(len(accounted))
            return 0
        if args.command == "coverage":
            reports = {}
            for port in manifest.get("ports", []):
                port_id = port.get("id")
                path = args.reports / f"{port_id}.json"
                if not path.is_file():
                    print(f"port-status: no published report for {port_id}", file=sys.stderr)
                    return 1
                reports[port_id] = read_json(path)
            contracts = None
            if args.contracts is not None:
                contracts = {}
                for port_id in reports:
                    path = args.contracts / f"{port_id}.json"
                    if not path.is_file():
                        continue
                    try:
                        contracts[port_id] = set(test_to_feature(read_json(path)))
                    except ContractError:
                        # A manifest we cannot read proves nothing. Leaving the
                        # port out keeps the weaker comparison rather than
                        # inventing an obligation or excusing one.
                        continue
            problems = coverage_problems(manifest, reports, contracts)
            for problem in problems:
                print(f"port-status coverage: {problem}", file=sys.stderr)
            if problems:
                print(
                    "Every registered test runs on every port unless the suite itself "
                    "reports a skip with an erratum. Fix the port or record the skip.",
                    file=sys.stderr,
                )
                return 1
            print(f"Every registered test reported a result on all {len(reports)} ports.")
            return 0
        if args.command == "provenance":
            report_directory = manifest.get("report_directory")
            problems = []
            for port in manifest.get("ports", []):
                port_id = port.get("id")
                base_path = args.base / f"{port_id}.json"
                head_path = REPO_ROOT / report_directory / f"{port_id}.json"
                if not head_path.is_file() and not base_path.is_file():
                    # A port that has never published. Nothing to check.
                    continue
                published = None
                if args.published is not None:
                    candidates = args.published / port_id
                    published = [
                        read_json(item)
                        for item in sorted(candidates.glob("*.json"))
                    ] if candidates.is_dir() else []
                problems.extend(
                    provenance_problems(
                        port_id,
                        read_json(base_path) if base_path.is_file() else None,
                        read_json(head_path) if head_path.is_file() else None,
                        published,
                    )
                )
            for problem in problems:
                print(f"port-status: {problem}", file=sys.stderr)
            if problems:
                return 1
            print("No port status report was edited by hand.")
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
