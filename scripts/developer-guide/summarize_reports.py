#!/usr/bin/env python3
"""Utilities for summarizing developer guide quality reports."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Iterable


def write_output(lines: Iterable[str], output: Path | None) -> None:
    content = "\n".join(lines) + "\n"
    if output:
        output.parent.mkdir(parents=True, exist_ok=True)
        with output.open("a", encoding="utf-8") as fh:
            fh.write(content)
    else:
        print(content, end="")


def summarize_asciidoc(report: Path, status: str, summary_key: str, output: Path | None) -> None:
    text = ""
    if report.is_file():
        text = report.read_text(encoding="utf-8", errors="ignore")
    issues = re.findall(r"\b(?:WARN|ERROR|SEVERE)\b", text)
    summary = f"{len(issues)} issue(s) flagged" if issues else "No issues found"
    status = status.strip()
    if status and status != "0":
        summary += f" (exit code {status})"
    write_output([f"{summary_key}={summary}"], output)


def summarize_vale(
    report: Path, status: str, summary_key: str, output: Path | None
) -> None:
    alerts: list[dict[str, object]] = []
    if report.is_file():
        try:
            data = json.loads(report.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            data = {}
        alerts = data.get("alerts", []) if isinstance(data, dict) else []

    counts = {"error": 0, "warning": 0, "suggestion": 0}
    total = 0
    for alert in alerts:
        if not isinstance(alert, dict):
            continue
        severity = str(alert.get("Severity", "")).lower()
        if severity in counts:
            counts[severity] += 1
            total += 1

    if total:
        parts = [
            f"{counts['error']} errors",
            f"{counts['warning']} warnings",
            f"{counts['suggestion']} suggestions",
        ]
        summary = f"{total} alert(s) ({', '.join(parts)})"
    else:
        summary = "No alerts found"

    status = status.strip()
    if status and status != "0":
        summary += f" (exit code {status})"

    write_output([f"{summary_key}={summary}"], output)


def summarize_unused_images(
    report: Path,
    summary_key: str,
    details_key: str | None,
    preview_limit: int,
    output: Path | None,
) -> None:
    unused: list[str] = []
    if report.is_file():
        try:
            data = json.loads(report.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            data = {}
        if isinstance(data, dict):
            value = data.get("unused_images", [])
            if isinstance(value, list):
                unused = [str(item) for item in value]

    count = len(unused)
    if count:
        summary = f"{count} unused image(s) found"
        preview = unused[: max(preview_limit, 0)] if preview_limit else []
        lines = [f"- {path}" for path in preview]
        if preview_limit and count > preview_limit:
            lines.append(f"- ... and {count - preview_limit} more")
    else:
        summary = "No unused images detected"
        lines = []

    output_lines = [f"{summary_key}={summary}"]
    if details_key and lines:
        details_value = "\\n".join(lines)
        output_lines.append(f"{details_key}={details_value}")

    write_output(output_lines, output)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="File to append GitHub Actions outputs to (defaults to stdout).",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    ascii_parser = subparsers.add_parser(
        "ascii",
        help="Summarize docToolchain AsciiDoc linter results.",
    )
    ascii_parser.add_argument("--report", type=Path, required=True)
    ascii_parser.add_argument("--status", default="0")
    ascii_parser.add_argument("--summary-key", default="summary")

    vale_parser = subparsers.add_parser(
        "vale",
        help="Summarize Vale style linter results.",
    )
    vale_parser.add_argument("--report", type=Path, required=True)
    vale_parser.add_argument("--status", default="0")
    vale_parser.add_argument("--summary-key", default="summary")

    unused_parser = subparsers.add_parser(
        "unused-images",
        help="Summarize unused image report results.",
    )
    unused_parser.add_argument("--report", type=Path, required=True)
    unused_parser.add_argument("--summary-key", default="summary")
    unused_parser.add_argument("--details-key", default=None)
    unused_parser.add_argument("--preview-limit", type=int, default=10)

    return parser.parse_args()


def main() -> None:
    args = parse_args()
    output = args.output
    command = args.command
    if command == "ascii":
        summarize_asciidoc(args.report, args.status, args.summary_key, output)
    elif command == "vale":
        summarize_vale(args.report, args.status, args.summary_key, output)
    elif command == "unused-images":
        summarize_unused_images(
            args.report,
            args.summary_key,
            args.details_key,
            args.preview_limit,
            output,
        )
    else:
        raise ValueError(f"Unsupported command: {command}")


if __name__ == "__main__":
    main()
