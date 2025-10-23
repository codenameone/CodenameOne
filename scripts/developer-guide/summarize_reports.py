#!/usr/bin/env python3
"""Utilities for summarizing developer guide quality reports."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Iterable


def _is_alert(candidate: dict[str, object]) -> bool:
    required_keys = {"Severity", "Check", "Message"}
    return required_keys.issubset(candidate.keys())


def _collect_alerts(node: object, path_hint: str = "") -> list[dict[str, object]]:
    alerts: list[dict[str, object]] = []

    if isinstance(node, dict):
        if _is_alert(node):
            alert = dict(node)
            if path_hint and not alert.get("Path"):
                alert["Path"] = path_hint
            alerts.append(alert)
            return alerts

        node_path = path_hint
        path_value = node.get("Path")
        if isinstance(path_value, str) and path_value:
            node_path = path_value

        alert_list = node.get("alerts")
        if isinstance(alert_list, list):
            alerts.extend(_collect_alerts(alert_list, node_path))

        file_map = node.get("files")
        if isinstance(file_map, dict):
            for file_path, file_node in file_map.items():
                hint = file_path if isinstance(file_path, str) else node_path
                alerts.extend(_collect_alerts(file_node, hint))

        for key, value in node.items():
            if key in {"alerts", "files", "Path"}:
                continue
            hint = node_path
            if isinstance(key, str) and ("/" in key or key.endswith(".adoc") or key.endswith(".asciidoc")):
                hint = key
            alerts.extend(_collect_alerts(value, hint))

    elif isinstance(node, list):
        for item in node:
            alerts.extend(_collect_alerts(item, path_hint))

    return alerts


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

    pattern = re.compile(
        r"^\s*(?:[^:\n]+:\d+:\d+:\s+|asciidoctor:\s+)(ERROR|WARN(?:ING)?|INFO)\b",
        re.MULTILINE,
    )
    matches = pattern.findall(text)

    counts = {"error": 0, "warning": 0, "info": 0}
    for severity in matches:
        normalized = severity.upper()
        if normalized == "ERROR":
            counts["error"] += 1
        elif normalized in {"WARN", "WARNING"}:
            counts["warning"] += 1
        elif normalized == "INFO":
            counts["info"] += 1

    total = sum(counts.values())

    if total:
        parts = [f"{counts['error']} errors"] if counts["error"] else []
        if counts["warning"]:
            parts.append(f"{counts['warning']} warnings")
        if counts["info"]:
            parts.append(f"{counts['info']} info")
        detail = f" ({', '.join(parts)})" if parts else ""
        summary = f"{total} issue(s) flagged{detail}"
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
        alerts = _collect_alerts(data)

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
        details_value = "\n".join(lines)
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
        help="Summarize Asciidoctor diagnostics.",
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
