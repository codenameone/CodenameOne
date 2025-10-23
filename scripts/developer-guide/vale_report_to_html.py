#!/usr/bin/env python3
"""Convert a Vale JSON report into a standalone HTML file."""

from __future__ import annotations

import argparse
import html
import json
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

    return _collect_alerts(data)


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
