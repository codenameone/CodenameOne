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


def _normalize_status(status: str) -> str:
    return status.strip()


def _has_nonzero_status(status: str) -> bool:
    status = _normalize_status(status)
    return bool(status and status != "0")


def summarize_asciidoc(report: Path, status: str, summary_key: str, output: Path | None) -> None:
    text = ""
    if report.is_file():
        text = report.read_text(encoding="utf-8", errors="ignore")
    issues = re.findall(r"\b(?:WARN|ERROR|SEVERE)\b", text)

    if issues:
        summary = f"{len(issues)} issue(s) flagged"
        if _has_nonzero_status(status):
            summary += f" (exit code {_normalize_status(status)})"
    elif _has_nonzero_status(status):
        summary = f"Linter failed (exit code {_normalize_status(status)})"
    else:
        summary = "No issues found"

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
        if isinstance(data, dict):
            if isinstance(data.get("alerts"), list):
                alerts.extend(data["alerts"])
            files = data.get("files")
            if isinstance(files, dict):
                for file_result in files.values():
                    if not isinstance(file_result, dict):
                        continue
                    file_alerts = file_result.get("alerts")
                    if isinstance(file_alerts, list):
                        alerts.extend(file_alerts)

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
        if _has_nonzero_status(status):
            summary += f" (exit code {_normalize_status(status)})"
    elif _has_nonzero_status(status):
        summary = f"Vale failed (exit code {_normalize_status(status)})"
    else:
        summary = "No alerts found"

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


def build_common_parser() -> argparse.ArgumentParser:
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument(
        "--output",
        type=Path,
        default=None,
        help="File to append GitHub Actions outputs to (defaults to stdout).",
    )
    return common


def parse_args() -> argparse.Namespace:
    common = build_common_parser()
    parser = argparse.ArgumentParser(description=__doc__, parents=[common])
    subparsers = parser.add_subparsers(dest="command", required=True)

    ascii_parser = subparsers.add_parser(
        "ascii",
        help="Summarize docToolchain AsciiDoc linter results.",
        parents=[common],
    )
    ascii_parser.add_argument("--report", type=Path, required=True)
    ascii_parser.add_argument("--status", default="0")
    ascii_parser.add_argument("--summary-key", default="summary")

    vale_parser = subparsers.add_parser(
        "vale",
        help="Summarize Vale style linter results.",
        parents=[common],
    )
    vale_parser.add_argument("--report", type=Path, required=True)
    vale_parser.add_argument("--status", default="0")
    vale_parser.add_argument("--summary-key", default="summary")

    unused_parser = subparsers.add_parser(
        "unused-images",
        help="Summarize unused image report results.",
        parents=[common],
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
