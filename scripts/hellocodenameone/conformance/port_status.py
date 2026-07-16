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
from datetime import datetime, timezone
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
DEFAULT_MANIFEST = REPO_ROOT / "docs/website/data/port_status.json"
RUNNER = REPO_ROOT / (
    "scripts/hellocodenameone/common/src/main/java/com/codenameone/"
    "examples/hellocodenameone/tests/Cn1ssDeviceRunner.java"
)
COMMON_SOURCES = REPO_ROOT / "scripts/hellocodenameone/common/src/main"

START_RE = re.compile(r"suite starting test=([A-Za-z0-9_]+)")
FINISH_RE = re.compile(r"suite finished test=([A-Za-z0-9_]+)")
SKIP_RE = re.compile(r"test=([A-Za-z0-9_]+) status=SKIPPED(?: reason=([^\s]+))?")
ERROR_RE = re.compile(r"CN1SS:ERR:suite test=([A-Za-z0-9_]+)(?:\s+(.*))?$")


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

    missing = sorted(set(registered) - set(mapped))
    extra = sorted(set(mapped) - set(registered))
    if missing:
        problems.append("Registered tests without a feature: " + ", ".join(missing))
    if extra:
        problems.append("Manifest tests not registered by the suite: " + ", ".join(extra))

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

    seed_directory = manifest.get("seed_report_directory")
    if seed_directory:
        seed_root = REPO_ROOT / seed_directory
        for port_id in port_ids:
            seed_path = seed_root / f"{port_id}.json"
            try:
                seed = read_json(seed_path)
            except ContractError as exc:
                problems.append(str(exc))
                continue
            if seed.get("schema_version") != manifest.get("schema_version"):
                problems.append(f"Seed report {seed_path} has the wrong schema version")
            if seed.get("port") != port_id:
                problems.append(f"Seed report {seed_path} identifies port {seed.get('port')}")
            if seed.get("bootstrap_source") != "successful-master-workflow":
                problems.append(f"Seed report {seed_path} has no successful-workflow provenance")
            if seed.get("workflow_conclusion") != "success":
                problems.append(f"Seed report {seed_path} did not come from a successful workflow")
            seed_tests = seed.get("tests")
            if not isinstance(seed_tests, dict):
                problems.append(f"Seed report {seed_path} has no test result map")
                continue
            unknown_tests = sorted(set(seed_tests) - set(mapped))
            if unknown_tests:
                problems.append(
                    f"Seed report {seed_path} contains unknown tests: "
                    + ", ".join(unknown_tests)
                )
            missing_tests = sorted(set(mapped) - set(seed_tests))
            if missing_tests:
                problems.append(
                    f"Seed report {seed_path} is missing tests: "
                    + ", ".join(missing_tests)
                )
            failing_tests = sorted(
                test for test, result in seed_tests.items()
                if result.get("status") == "fail"
            )
            if failing_tests:
                problems.append(
                    f"Successful-workflow seed report {seed_path} contains failures: "
                    + ", ".join(failing_tests)
                )

    if problems:
        raise ContractError("\n".join(problems))
    return {
        "ports": len(ports),
        "features": len(features),
        "tests": len(registered),
        "goldens": len(golden_names),
    }


def add_reason(entry: dict, reason: str) -> None:
    reasons = entry.setdefault("reasons", [])
    if reason and reason not in reasons:
        reasons.append(reason)


def parse_logs(paths: list[Path], states: dict[str, dict]) -> bool:
    suite_finished = False
    for path in paths:
        if not path.is_file():
            continue
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            if "CN1SS:SUITE:FINISHED" in line:
                suite_finished = True
            match = START_RE.search(line)
            if match and match.group(1) in states:
                states[match.group(1)]["started"] = True
            match = FINISH_RE.search(line)
            if match and match.group(1) in states:
                states[match.group(1)]["finished"] = True
            match = SKIP_RE.search(line)
            if match and match.group(1) in states:
                entry = states[match.group(1)]
                entry["skipped"] = True
                add_reason(entry, match.group(2) or "reported-skip")
            match = ERROR_RE.search(line)
            if match and match.group(1) in states:
                entry = states[match.group(1)]
                entry["failed"] = True
                add_reason(entry, (match.group(2) or "").strip() or "suite-error")
    return suite_finished


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
    suite_finished = parse_logs(logs, states)
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
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return report


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("validate", help="validate feature and screenshot coverage")

    normalize_parser = subparsers.add_parser("normalize", help="write a normalized port report")
    normalize_parser.add_argument("--port", required=True)
    normalize_parser.add_argument("--log", action="append", type=Path, default=[])
    normalize_parser.add_argument("--compare", action="append", type=Path, default=[])
    normalize_parser.add_argument("--output", required=True, type=Path)
    normalize_parser.add_argument("--run-url", default=os.environ.get("GITHUB_RUN_URL", ""))
    normalize_parser.add_argument("--commit", default=os.environ.get("GITHUB_SHA", ""))
    normalize_parser.add_argument("--generated-at", default=utc_now())
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
        report = normalize(
            manifest=manifest,
            port_id=args.port,
            logs=args.log,
            comparisons=args.compare,
            output=args.output,
            run_url=args.run_url,
            commit=args.commit,
            generated_at=args.generated_at,
        )
        print(
            f"Wrote {args.output}: "
            + ", ".join(f"{key}={value}" for key, value in report["summary"].items())
        )
        return 0
    except ContractError as exc:
        print(f"port-status: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
