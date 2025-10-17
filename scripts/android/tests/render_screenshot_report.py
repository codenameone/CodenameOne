#!/usr/bin/env python3
"""Render screenshot comparison summaries and PR comment content.

This module transforms the JSON output produced by ``process_screenshots.py``
into a short summary file (used for logs/artifacts) and a Markdown document
that can be posted back to a pull request.  It mirrors the logic that used to
live inline inside ``run-android-instrumentation-tests.sh``.
"""

from __future__ import annotations

import argparse
import json
import pathlib
from typing import Any, Dict, List

MARKER = "<!-- CN1SS_SCREENSHOT_COMMENT -->"


def build_summary_and_comment(data: Dict[str, Any]) -> tuple[List[str], List[str]]:
    summary_lines: List[str] = []
    comment_entries: List[Dict[str, Any]] = []

    for result in data.get("results", []):
        test = result.get("test", "unknown")
        status = result.get("status", "unknown")
        expected_path = result.get("expected_path")
        actual_path = result.get("actual_path", "")
        details = result.get("details") or {}
        base64_data = result.get("base64")
        base64_omitted = result.get("base64_omitted")
        base64_length = result.get("base64_length")
        base64_mime = result.get("base64_mime") or "image/png"
        base64_codec = result.get("base64_codec")
        base64_quality = result.get("base64_quality")
        base64_note = result.get("base64_note")
        message = ""
        copy_flag = "0"

        preview = result.get("preview") or {}
        preview_name = preview.get("name")
        preview_path = preview.get("path")
        preview_mime = preview.get("mime")
        preview_note = preview.get("note")
        preview_quality = preview.get("quality")

        if status == "equal":
            message = "Matches stored reference."
        elif status == "missing_expected":
            message = f"Reference screenshot missing at {expected_path}."
            copy_flag = "1"
            comment_entries.append(
                {
                    "test": test,
                    "status": "missing reference",
                    "message": message,
                    "artifact_name": f"{test}.png",
                    "preview_name": preview_name,
                    "preview_path": preview_path,
                    "preview_mime": preview_mime,
                    "preview_note": preview_note,
                    "preview_quality": preview_quality,
                    "base64": base64_data,
                    "base64_omitted": base64_omitted,
                    "base64_length": base64_length,
                    "base64_mime": base64_mime,
                    "base64_codec": base64_codec,
                    "base64_quality": base64_quality,
                    "base64_note": base64_note,
                }
            )
        elif status == "different":
            dims = ""
            if details:
                dims = (
                    f" ({details.get('width')}x{details.get('height')} px, "
                    f"bit depth {details.get('bit_depth')})"
                )
            message = f"Screenshot differs{dims}."
            copy_flag = "1"
            comment_entries.append(
                {
                    "test": test,
                    "status": "updated screenshot",
                    "message": message,
                    "artifact_name": f"{test}.png",
                    "preview_name": preview_name,
                    "preview_path": preview_path,
                    "preview_mime": preview_mime,
                    "preview_note": preview_note,
                    "preview_quality": preview_quality,
                    "base64": base64_data,
                    "base64_omitted": base64_omitted,
                    "base64_length": base64_length,
                    "base64_mime": base64_mime,
                    "base64_codec": base64_codec,
                    "base64_quality": base64_quality,
                    "base64_note": base64_note,
                }
            )
        elif status == "error":
            message = f"Comparison error: {result.get('message', 'unknown error')}"
            copy_flag = "1"
            comment_entries.append(
                {
                    "test": test,
                    "status": "comparison error",
                    "message": message,
                    "artifact_name": f"{test}.png",
                    "preview_name": preview_name,
                    "preview_path": preview_path,
                    "preview_mime": preview_mime,
                    "preview_note": preview_note,
                    "preview_quality": preview_quality,
                    "base64": None,
                    "base64_omitted": base64_omitted,
                    "base64_length": base64_length,
                    "base64_mime": base64_mime,
                    "base64_codec": base64_codec,
                    "base64_quality": base64_quality,
                    "base64_note": base64_note,
                }
            )
        elif status == "missing_actual":
            message = "Actual screenshot missing (test did not produce output)."
            copy_flag = "1"
            comment_entries.append(
                {
                    "test": test,
                    "status": "missing actual screenshot",
                    "message": message,
                    "artifact_name": None,
                    "preview_name": preview_name,
                    "preview_path": preview_path,
                    "preview_mime": preview_mime,
                    "preview_note": preview_note,
                    "preview_quality": preview_quality,
                    "base64": None,
                    "base64_omitted": base64_omitted,
                    "base64_length": base64_length,
                    "base64_mime": base64_mime,
                    "base64_codec": base64_codec,
                    "base64_quality": base64_quality,
                    "base64_note": base64_note,
                }
            )
        else:
            message = f"Status: {status}."

        note_column = preview_note or base64_note or ""
        summary_lines.append("|".join([status, test, message, copy_flag, actual_path, note_column]))

    comment_lines: List[str] = []
    if comment_entries:
        comment_lines.extend(["### Android screenshot updates", ""])

        def add_line(text: str = "") -> None:
            comment_lines.append(text)

        for entry in comment_entries:
            entry_header = f"- **{entry['test']}** — {entry['status']}. {entry['message']}"
            add_line(entry_header)

            preview_name = entry.get("preview_name")
            preview_quality = entry.get("preview_quality")
            preview_note = entry.get("preview_note")
            base64_note = entry.get("base64_note")
            preview_mime = entry.get("preview_mime")

            preview_notes: List[str] = []
            if preview_mime == "image/jpeg" and preview_quality:
                preview_notes.append(f"JPEG preview quality {preview_quality}")
            if preview_note:
                preview_notes.append(str(preview_note))
            if base64_note and base64_note != preview_note:
                preview_notes.append(str(base64_note))

            if preview_name:
                add_line("")
                add_line(f"  ![{entry['test']}](attachment:{preview_name})")
                if preview_notes:
                    add_line(f"  _Preview info: {'; '.join(preview_notes)}._")
            elif entry.get("base64"):
                add_line("")
                add_line(
                    "  _Preview generated but could not be published; see workflow artifacts for JPEG preview._"
                )
                if preview_notes:
                    add_line(f"  _Preview info: {'; '.join(preview_notes)}._")
            elif entry.get("base64_omitted") == "too_large":
                size_note = ""
                if entry.get("base64_length"):
                    size_note = f" (base64 length ≈ {entry['base64_length']:,} chars)"
                codec = entry.get("base64_codec")
                quality = entry.get("base64_quality")
                note = entry.get("base64_note")
                extra_bits: List[str] = []
                if codec == "jpeg" and quality:
                    extra_bits.append(f"attempted JPEG quality {quality}")
                if note:
                    extra_bits.append(str(note))
                tail = f" ({'; '.join(extra_bits)})" if extra_bits else ""
                add_line("")
                add_line(
                    "  _Screenshot omitted from comment because the encoded payload exceeded GitHub's size limits"
                    + size_note
                    + "."
                    + tail
                    + "_"
                )
            else:
                add_line("")
                add_line("  _No preview available for this screenshot._")

            artifact_name = entry.get("artifact_name")
            if artifact_name:
                add_line(f"  _Full-resolution PNG saved as `{artifact_name}` in workflow artifacts._")
            add_line("")

        if comment_lines and comment_lines[-1] != "":
            comment_lines.append("")
        comment_lines.append(MARKER)
    else:
        comment_lines = ["✅ Native Android screenshot tests passed.", "", MARKER]

    return summary_lines, comment_lines


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--compare-json", required=True, help="Path to screenshot comparison JSON output")
    parser.add_argument("--comment-out", required=True, help="Destination Markdown file for PR comment")
    parser.add_argument("--summary-out", required=True, help="Destination summary file")
    args = parser.parse_args()

    compare_path = pathlib.Path(args.compare_json)
    comment_path = pathlib.Path(args.comment_out)
    summary_path = pathlib.Path(args.summary_out)

    if not compare_path.is_file():
        raise SystemExit(f"Comparison JSON not found: {compare_path}")

    data = json.loads(compare_path.read_text(encoding="utf-8"))
    summary_lines, comment_lines = build_summary_and_comment(data)

    summary_text = "\n".join(summary_lines)
    if summary_text:
        summary_text += "\n"
    summary_path.write_text(summary_text, encoding="utf-8")

    comment_text = "\n".join(line.rstrip() for line in comment_lines).rstrip() + "\n"
    comment_path.write_text(comment_text, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
