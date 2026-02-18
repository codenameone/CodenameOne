#!/usr/bin/env python3
"""
Validate website redirects against a deployed base URL.

By default this script:
1) Parses docs/website/static/_redirects
2) Tests all literal redirect entries (no wildcard/param sources)
3) Optionally tests custom cases (useful for wildcard rules)
"""

from __future__ import annotations

import argparse
import dataclasses
import pathlib
import re
import sys
import urllib.error
import urllib.parse
import urllib.request


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
DEFAULT_REDIRECTS_FILE = REPO_ROOT / "docs" / "website" / "static" / "_redirects"


@dataclasses.dataclass
class RedirectRule:
    source: str
    target: str
    status: int
    line_no: int


@dataclasses.dataclass
class Case:
    source: str
    expected_status: int
    expected_target: str
    label: str
    require_no_redirect: bool = False


@dataclasses.dataclass
class CaseResult:
    case: Case
    ok: bool
    actual_status: int | None
    actual_location: str | None
    error: str | None = None


REQUIRED_CASES = [
    Case(
        source="/files/CodenameOneBuildClient.jar",
        expected_status=302,
        expected_target=(
            "https://github.com/codenameone/CodenameOne/raw/refs/heads/master/"
            "maven/CodeNameOneBuildClient.jar"
        ),
        label="required",
    ),
    Case(
        source="/files/developer-guide.pdf",
        expected_status=302,
        expected_target=(
            r"re:https://github\.com/codenameone/CodenameOne/releases/"
            r"(latest/download|download/[^/]+)/developer-guide\.pdf"
        ),
        label="required",
    ),
]


def discover_local_priority_cases() -> list[Case]:
    public_root = REPO_ROOT / "docs" / "website" / "public"
    directories = [("demos", "/demos"), ("files", "/files")]
    cases: list[Case] = []
    for disk_dir, url_prefix in directories:
        root = public_root / disk_dir
        if not root.exists():
            continue
        for f in sorted(p for p in root.rglob("*") if p.is_file()):
            rel = f.relative_to(root).as_posix()
            if rel == "index.html":
                source = f"{url_prefix}/"
            elif rel.endswith("/index.html"):
                source = f"{url_prefix}/{rel[:-len('index.html')]}"
            else:
                source = f"{url_prefix}/{rel}"
            cases.append(
                Case(
                    source=source,
                    expected_status=200,
                    expected_target="",
                    label=f"local-priority:{disk_dir}",
                    require_no_redirect=True,
                )
            )
    return cases


def parse_redirects_file(path: pathlib.Path) -> list[RedirectRule]:
    rules: list[RedirectRule] = []
    for i, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split()
        if len(parts) < 3:
            continue
        src, tgt, status_text = parts[0], parts[1], parts[2]
        try:
            status = int(status_text.rstrip("!"))
        except ValueError:
            continue
        if status < 300 or status > 399:
            continue
        rules.append(RedirectRule(source=src, target=tgt, status=status, line_no=i))
    return rules


def normalize_location(loc: str | None) -> str | None:
    if not loc:
        return None
    return loc.strip()


def location_matches(expected_target: str, actual_location: str | None) -> bool:
    if actual_location is None:
        return False
    actual = normalize_location(actual_location)
    if expected_target.startswith("re:"):
        return re.fullmatch(expected_target[3:], actual or "") is not None
    if expected_target.startswith("http://") or expected_target.startswith("https://"):
        return actual == expected_target
    # Relative targets may come back relative or absolute.
    if actual == expected_target:
        return True
    parsed = urllib.parse.urlsplit(actual)
    if parsed.scheme and parsed.netloc:
        actual_path_q = urllib.parse.urlunsplit(("", "", parsed.path, parsed.query, ""))
        return actual_path_q == expected_target
    return False


class NoRedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):  # noqa: N803
        return None


NO_REDIRECT_OPENER = urllib.request.build_opener(NoRedirectHandler)


def check_case(base_url: str, case: Case, timeout: float) -> CaseResult:
    url = urllib.parse.urljoin(base_url.rstrip("/") + "/", case.source.lstrip("/"))
    req = urllib.request.Request(url, headers={"User-Agent": "cn1-redirect-check/1.0"})
    try:
        with NO_REDIRECT_OPENER.open(req, timeout=timeout) as resp:
            status = int(resp.getcode())
            location = normalize_location(resp.headers.get("Location"))
            if case.expected_status < 300:
                ok = status == case.expected_status
                if ok and case.require_no_redirect and location is not None:
                    ok = False
            else:
                ok = status == case.expected_status and location_matches(
                    case.expected_target, location
                )
            return CaseResult(
                case=case,
                ok=ok,
                actual_status=status,
                actual_location=location,
            )
    except urllib.error.HTTPError as exc:
        status = int(exc.code)
        location = normalize_location(exc.headers.get("Location"))
        if case.expected_status < 300:
            ok = status == case.expected_status
            if ok and case.require_no_redirect and location is not None:
                ok = False
        else:
            ok = status == case.expected_status and location_matches(
                case.expected_target, location
            )
        return CaseResult(
            case=case,
            ok=ok,
            actual_status=status,
            actual_location=location,
        )
    except Exception as exc:  # noqa: BLE001
        return CaseResult(
            case=case,
            ok=False,
            actual_status=None,
            actual_location=None,
            error=str(exc),
        )


def rule_to_case(rule: RedirectRule) -> Case | None:
    # Skip non-literal route patterns from automatic checks.
    if "*" in rule.source or ":" in rule.source:
        return None
    return Case(
        source=rule.source,
        expected_status=rule.status,
        expected_target=rule.target,
        label=f"_redirects:{rule.line_no}",
    )


def parse_custom_case(text: str) -> Case:
    # Format: /path|301|/target/
    parts = text.split("|", 2)
    if len(parts) != 3:
        raise ValueError(
            f"Invalid --case '{text}'. Expected format: /source|301|/target/"
        )
    source, status_text, target = parts
    return Case(
        source=source,
        expected_status=int(status_text),
        expected_target=target,
        label="custom",
    )


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Validate redirects from docs/website/static/_redirects",
    )
    p.add_argument("--base-url", required=True, help="e.g. https://beta.codenameone.com")
    p.add_argument(
        "--redirects-file",
        default=str(DEFAULT_REDIRECTS_FILE),
        help="Path to _redirects file",
    )
    p.add_argument(
        "--timeout",
        type=float,
        default=12.0,
    )
    p.add_argument(
        "--max-cases",
        type=int,
        default=0,
        help="Optional limit for automatic cases (0 = all).",
    )
    p.add_argument(
        "--case",
        action="append",
        default=[],
        help="Custom test case format: /source|301|/target/ (can repeat)",
    )
    p.add_argument(
        "--fail-fast",
        action="store_true",
    )
    p.add_argument(
        "--skip-auto-cases",
        action="store_true",
        help="Skip auto-generated literal cases from _redirects and run only required/local/custom checks.",
    )
    return p.parse_args()


def main() -> int:
    args = parse_args()
    redirects_file = pathlib.Path(args.redirects_file)
    rules = parse_redirects_file(redirects_file)

    auto_cases: list[Case] = []
    if not args.skip_auto_cases:
        for r in rules:
            c = rule_to_case(r)
            if c is not None:
                auto_cases.append(c)
        if args.max_cases > 0:
            auto_cases = auto_cases[: args.max_cases]

    custom_cases = [parse_custom_case(raw) for raw in args.case]
    local_priority_cases = discover_local_priority_cases()
    seen = {(c.source, c.expected_status, c.expected_target) for c in custom_cases}
    dedup_required = [
        c
        for c in REQUIRED_CASES
        if (c.source, c.expected_status, c.expected_target) not in seen
    ]
    seen_all = {
        (c.source, c.expected_status, c.expected_target, c.require_no_redirect)
        for c in dedup_required + custom_cases
    }
    dedup_local_priority = [
        c
        for c in local_priority_cases
        if (c.source, c.expected_status, c.expected_target, c.require_no_redirect)
        not in seen_all
    ]
    cases = dedup_required + dedup_local_priority + custom_cases + auto_cases

    print(f"Redirect rules parsed: {len(rules)}")
    print(f"Auto literal cases: {len(auto_cases)}")
    print(f"Required cases: {len(dedup_required)}")
    print(f"Local priority cases: {len(dedup_local_priority)}")
    print(f"Custom cases: {len(custom_cases)}")
    print(f"Total cases: {len(cases)}")

    failures: list[CaseResult] = []
    for idx, case in enumerate(cases, start=1):
        result = check_case(args.base_url, case, timeout=args.timeout)
        if not result.ok:
            failures.append(result)
            print(
                f"[FAIL] {case.source} ({case.label}) "
                f"expected {case.expected_status} {case.expected_target} "
                f"got status={result.actual_status} location={result.actual_location} error={result.error}"
            )
            if args.fail_fast:
                break
        elif idx % 100 == 0:
            print(f"[OK] checked {idx}/{len(cases)}")

    passed = len(cases) - len(failures)
    print(f"Passed: {passed}")
    print(f"Failed: {len(failures)}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
