#!/usr/bin/env python3
import json
import os
import re
from pathlib import Path

marker = "<!-- CN1SS_ANDROID_COMMENT -->"
comment_lines = [marker, "### Android screenshot tests", ""]

screenshot_path = Path("artifacts/screenshot-comment.md")
compare_path = Path("artifacts/screenshot-compare.json")

# Still used for coverage HTML; not for screenshots
preview_base = os.environ.get("ANDROID_PREVIEW_BASE_URL", "").strip()
if preview_base.endswith("/"):
    preview_base = preview_base.rstrip("/")

# Collect base64 payloads keyed by preview file name (e.g. "mainactivity.jpg")
attachment_payloads = {}
if compare_path.is_file():
    try:
        compare_data = json.loads(compare_path.read_text())
        results = compare_data.get("results", [])
        for item in results:
            # Guard against non-dict items without using isinstance
            if not hasattr(item, "get"):
                continue

            preview = item.get("preview") or {}
            preview_name = preview.get("name") or preview.get("path")

            if preview_name is not None:
                preview_name = str(preview_name)
                if "/" in preview_name:
                    preview_name = preview_name.split("/")[-1]

            base64_data = item.get("base64")
            if base64_data:
                base64_data = str(base64_data)
                mime = item.get("base64_mime") or "image/png"
                if preview_name:
                    # Keep the first entry for a given preview name
                    attachment_payloads.setdefault(preview_name, (mime, base64_data))
    except Exception:
        # If parsing fails, just skip embedding previews
        attachment_payloads = {}

# Load screenshot comment text
if screenshot_path.is_file():
    screenshot_text = screenshot_path.read_text().strip()
    screenshot_text = screenshot_text.replace("<!-- CN1SS_SCREENSHOT_COMMENT -->", "").strip()
    if not screenshot_text:
        screenshot_text = "✅ Native Android screenshot tests passed."
else:
    screenshot_text = "✅ Native Android screenshot tests passed."

# Rewrite (attachment:foo.jpg) -> data: URL if we have base64 for that name
if screenshot_text:
    pattern = re.compile(r"\(attachment:([^)]+)\)")

    def replace_attachment(match):
        name = match.group(1)
        payload = attachment_payloads.get(name)
        if payload:
            mime, data = payload
            return "(data:%s;base64,%s)" % (mime, data)
        # No payload -> leave as-is
        return match.group(0)

    screenshot_text = pattern.sub(replace_attachment, screenshot_text)

comment_lines.append(screenshot_text)
comment_lines.append("")
comment_lines.append("### Android coverage")
comment_lines.append("")

# Coverage section (unchanged logic)
coverage_section = "Coverage report was not generated."
coverage_path = Path("artifacts/android-coverage/coverage.json")
if coverage_path.is_file():
    data = json.loads(coverage_path.read_text())
    if data.get("available"):
        lines = data.get("lines", {})
        covered = lines.get("covered", 0)
        total = lines.get("total", 0)
        percent = data.get("lines", {}).get("percent", 0.0)
        detail = f"{covered}/{total} lines" if total else "0/0 lines"
        coverage_html = os.environ.get("COVERAGE_HTML_URL", "")
        if coverage_html:
            coverage_section = (
                f"- 📊 **Line coverage:** {percent:.2f}% ({detail}) "
                f"([HTML report]({coverage_html}))"
            )
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