#!/usr/bin/env python3
"""Convert a Vale JSON report into a standalone HTML file."""

from __future__ import annotations

import argparse
import html
import json
from pathlib import Path
from typing import Iterable


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True, help="Path to the Vale JSON report.")
    parser.add_argument("--output", type=Path, required=True, help="Destination path for the HTML report.")
    return parser.parse_args()


def load_alerts(report: Path) -> list[dict[str, object]]:
    if not report.is_file():
        return []
    try:
        data = json.loads(report.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return []

    alerts: list[dict[str, object]] = []
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
    return alerts


def render_alert_rows(alerts: Iterable[dict[str, object]]) -> str:
    normalized: list[dict[str, str]] = []
    for alert in alerts:
        if not isinstance(alert, dict):
            continue
        span = alert.get("Span")
        line = column = ""
        if isinstance(span, dict):
            start = span.get("Start")
            if isinstance(start, dict):
                line = str(start.get("Line", ""))
                column = str(start.get("Column", ""))
        elif isinstance(span, list) and span:
            line = str(span[0])
            if len(span) > 1:
                column = str(span[1])
        normalized.append(
            {
                "file": str(alert.get("Path", "")),
                "line": line,
                "column": column,
                "severity": str(alert.get("Severity", "")),
                "rule": str(alert.get("Check", "")),
                "message": str(alert.get("Message", "")),
            }
        )

    if not normalized:
        return "<tr><td colspan='6'>No alerts found.</td></tr>"

    def sort_key(entry: dict[str, str]) -> tuple:
        def as_int(value: str) -> int:
            try:
                return int(value)
            except (TypeError, ValueError):
                return 0

        return (
            entry["file"],
            as_int(entry["line"]),
            as_int(entry["column"]),
            entry["rule"],
        )

    normalized.sort(key=sort_key)

    rows: list[str] = []
    for entry in normalized:
        severity_value = entry["severity"]
        severity = html.escape(severity_value)
        severity_class = f"severity-{severity_value.lower()}" if severity_value else ""
        rows.append(
            "<tr>"
            f"<td>{html.escape(entry['file'])}</td>"
            f"<td>{html.escape(entry['line'])}</td>"
            f"<td>{html.escape(entry['column'])}</td>"
            f"<td class='{severity_class}'>{severity}</td>"
            f"<td>{html.escape(entry['rule'])}</td>"
            f"<td>{html.escape(entry['message'])}</td>"
            "</tr>"
        )

    return "\n".join(rows)


def main() -> None:
    args = parse_args()
    alerts = load_alerts(args.input)
    counts = {"error": 0, "warning": 0, "suggestion": 0}
    for alert in alerts:
        if not isinstance(alert, dict):
            continue
        severity = str(alert.get("Severity", "")).lower()
        if severity in counts:
            counts[severity] += 1
    args.output.parent.mkdir(parents=True, exist_ok=True)
    table_rows = render_alert_rows(alerts)
    html_content = f"""
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>Vale Report</title>
  <style>
    body {{ font-family: Arial, sans-serif; margin: 2rem; }}
    table {{ border-collapse: collapse; width: 100%; }}
    th, td {{ border: 1px solid #ccc; padding: 0.5rem; text-align: left; }}
    th {{ background-color: #f0f0f0; }}
    tbody tr:nth-child(even) {{ background-color: #fafafa; }}
    .severity-error {{ color: #c62828; font-weight: bold; }}
    .severity-warning {{ color: #ef6c00; font-weight: bold; }}
    .severity-suggestion {{ color: #1565c0; font-weight: bold; }}
  </style>
</head>
<body>
  <h1>Vale Report</h1>
  <p>Total alerts: {len(alerts)}</p>
  <ul>
    <li>Errors: {counts['error']}</li>
    <li>Warnings: {counts['warning']}</li>
    <li>Suggestions: {counts['suggestion']}</li>
  </ul>
  <table>
    <thead>
      <tr>
        <th>File</th>
        <th>Line</th>
        <th>Column</th>
        <th>Severity</th>
        <th>Rule</th>
        <th>Message</th>
      </tr>
    </thead>
    <tbody>
      {table_rows}
    </tbody>
  </table>
</body>
</html>
"""
    args.output.write_text(html_content, encoding="utf-8")


if __name__ == "__main__":
    main()
