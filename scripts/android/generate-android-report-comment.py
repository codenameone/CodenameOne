#!/usr/bin/env python3
import json
import os
import re
from pathlib import Path

marker = "<!-- CN1SS_ANDROID_COMMENT -->"
comment_lines = [marker, "### Android screenshot tests", ""]

screenshot_path = Path("artifacts/screenshot-comment.md")
compare_path = Path("artifacts/screenshot-compare.json")
preview_base = os.environ.get("ANDROID_PREVIEW_BASE_URL", "").strip()
if preview_base.endswith("/"):
    preview_base = preview_base.rstrip("/")

attachment_payloads = {}
if compare_path.is_file():
    try:
        compare_data = json.loads(compare_path.read_text())
        for item in compare_data.get("results", []):
            if not isinstance(item, dict):
                continue
            preview = item.get("preview") or {}
            preview_name = preview.get("name") or preview.get("path")
            if isinstance(preview_name, str) and "/" in preview_name:
                preview_name = preview_name.split("/")[-1]
            base64_data = item.get("base64")
            mime = item.get("base64_mime", "image/png")
            if isinstance(base64_data, str) and base64_data:
                if preview_name:
                    attachment_payloads.setdefault(preview_name, (mime, base64_data))
    except json.JSONDecodeError:
        attachment_payloads = {}
if screenshot_path.is_file():
    screenshot_text = screenshot_path.read_text().strip()
    screenshot_text = screenshot_text.replace("<!-- CN1SS_SCREENSHOT_COMMENT -->", "").strip()
    if not screenshot_text:
        screenshot_text = "✅ Native Android screenshot tests passed."
else:
    screenshot_text = "✅ Native Android screenshot tests passed."

if screenshot_text:
    pattern = re.compile(r"\(attachment:([^)]+)\)")

    def replace_attachment(match: re.Match) -> str:
        name = match.group(1)
        if preview_base:
            return f"({preview_base}/{name})"
        payload = attachment_payloads.get(name)
        if payload:
            mime, data = payload
            return f"(data:{mime};base64,{data})"
        return match.group(0)

    screenshot_text = pattern.sub(replace_attachment, screenshot_text)

comment_lines.append(screenshot_text)
comment_lines.append("")
comment_lines.append("### Android coverage")
comment_lines.append("")

coverage_section = "Coverage report was not generated."
coverage_path = Path("artifacts/android-coverage/coverage.json")
if coverage_path.is_file():
    data = json.loads(coverage_path.read_text())
    if data.get("available"):
        lines = data.get("lines", {})
        covered = lines.get("covered", 0)
        total = lines.get("total", 0)
        percent = lines.get("percent", 0.0)
        detail = f"{covered}/{total} lines" if total else "0/0 lines"
        coverage_html = os.environ.get("COVERAGE_HTML_URL", "")
        if coverage_html:
            coverage_section = f"- 📊 **Line coverage:** {percent:.2f}% ({detail}) ([HTML report]({coverage_html}))"
        else:
            coverage_section = f"- 📊 **Line coverage:** {percent:.2f}% ({detail})"
    else:
        note = data.get("note")
        if note:
            coverage_section = f"Coverage report is unavailable ({note})."
        else:
            coverage_section = "Coverage report is unavailable."

comment_lines.append(coverage_section)
comment_lines.append("")

Path("android-comment.md").write_text("\n".join(comment_lines) + "\n")
