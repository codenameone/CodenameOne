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

if screenshot_path.is_file():
    screenshot_text = screenshot_path.read_text().strip()
    screenshot_text = screenshot_text.replace("<!-- CN1SS_SCREENSHOT_COMMENT -->", "").strip()
    if not screenshot_text:
        screenshot_text = "✅ Native Android screenshot tests passed."
else:
    screenshot_text = "✅ Native Android screenshot tests passed."

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
