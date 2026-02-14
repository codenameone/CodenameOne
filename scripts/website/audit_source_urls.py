#!/usr/bin/env python3
"""Audit website source files for legacy absolute/WordPress URLs."""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Tuple


FILE_GLOBS = ("**/*.md", "**/*.html")
IGNORED_FRONTMATTER_KEYS = ("original_url", "source_url")

ABS_CN1_RE = re.compile(r"https?://www\.codenameone\.com/[^\s)\"'>]+", re.IGNORECASE)
WP_RE = re.compile(
    r"(wp-content|wp-login\.php|\?p=\d+|\?page_id=\d+|/wp-admin/)",
    re.IGNORECASE,
)


@dataclass
class Finding:
    kind: str
    file: Path
    line_no: int
    url: str
    suggestion: str


def iter_files(root: Path) -> Iterable[Path]:
    for pattern in FILE_GLOBS:
        yield from root.glob(pattern)


def should_ignore_line(line: str) -> bool:
    stripped = line.strip()
    for key in IGNORED_FRONTMATTER_KEYS:
        if stripped.startswith(f"{key}:"):
            return True
    return False


def suggestion_for_abs(url: str) -> str:
    parsed = re.sub(r"^https?://www\.codenameone\.com", "", url, flags=re.IGNORECASE)
    if not parsed.startswith("/"):
        parsed = "/" + parsed
    return parsed


def audit(root: Path) -> List[Finding]:
    findings: List[Finding] = []
    for file_path in sorted(iter_files(root)):
        if not file_path.is_file():
            continue
        rel = file_path.relative_to(root)
        text = file_path.read_text(encoding="utf-8", errors="ignore")
        for idx, line in enumerate(text.splitlines(), start=1):
            if should_ignore_line(line):
                continue

            for match in ABS_CN1_RE.finditer(line):
                url = match.group(0)
                findings.append(
                    Finding(
                        kind="ABSOLUTE_CN1_URL",
                        file=rel,
                        line_no=idx,
                        url=url,
                        suggestion=suggestion_for_abs(url),
                    )
                )

            wp_match = WP_RE.search(line)
            if wp_match:
                findings.append(
                    Finding(
                        kind="WORDPRESS_URL",
                        file=rel,
                        line_no=idx,
                        url=wp_match.group(0),
                        suggestion="Replace with current migrated site path/resource.",
                    )
                )
    return findings


def write_report(findings: List[Finding], report_file: Path) -> None:
    report_file.parent.mkdir(parents=True, exist_ok=True)
    lines = []
    lines.append(f"Total findings: {len(findings)}")
    by_kind: dict[str, int] = {}
    for f in findings:
        by_kind[f.kind] = by_kind.get(f.kind, 0) + 1
    for kind in sorted(by_kind):
        lines.append(f"- {kind}: {by_kind[kind]}")
    lines.append("")
    for f in findings:
        lines.append(
            f"{f.kind} {f.file}:{f.line_no} :: {f.url} :: suggestion={f.suggestion}"
        )
    report_file.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit website sources for legacy absolute/WordPress URLs.")
    parser.add_argument("--root", default="docs/website", help="Website root directory.")
    parser.add_argument("--report-file", default="docs/website/reports/source-url-audit.txt", help="Report output path.")
    parser.add_argument("--max-log", type=int, default=80, help="Max findings to print in console.")
    parser.add_argument("--fail-on-findings", action="store_true", help="Exit non-zero if findings exist.")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    if not root.exists():
        print(f"Root directory not found: {root}", file=sys.stderr)
        return 2

    findings = audit(root)
    report_file = Path(args.report_file).resolve()
    write_report(findings, report_file)

    print(f"Source URL audit findings: {len(findings)}")
    if findings:
        shown = min(args.max_log, len(findings))
        for f in findings[:shown]:
            print(f"- {f.kind} {f.file}:{f.line_no} -> {f.url}")
        if len(findings) > shown:
            print(f"... {len(findings) - shown} more findings omitted")
        print(f"Full report: {report_file}")

    return 1 if findings and args.fail_on_findings else 0


if __name__ == "__main__":
    raise SystemExit(main())
