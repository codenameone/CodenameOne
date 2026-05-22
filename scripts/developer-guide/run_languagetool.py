#!/usr/bin/env python3
"""Run LanguageTool against the rendered developer guide.

This produces an advisory report only — LanguageTool's signal/noise ratio is
not high enough to gate the build on, but its findings are useful for spot-
checks and as a soft signal in PR review. The companion paragraph
capitalization check (check_paragraph_capitalization.rb) is the hard gate.

Input: the asciidoctor-rendered HTML for the full developer guide.
Output: JSON report compatible with the existing summarize_reports.py
pipeline, written to the path given by --output.
"""

import argparse
import json
import os
import sys
from html.parser import HTMLParser


SKIP_TAGS = {"script", "style", "code", "pre", "kbd", "samp", "var", "tt"}


class TextExtractor(HTMLParser):
    """Pull the prose content out of asciidoctor's HTML output."""

    def __init__(self):
        super().__init__()
        self._chunks = []
        self._skip_depth = 0

    def handle_starttag(self, tag, attrs):
        if tag in SKIP_TAGS:
            self._skip_depth += 1
        elif tag in ("p", "li", "h1", "h2", "h3", "h4", "h5", "h6", "div"):
            self._chunks.append("\n\n")

    def handle_endtag(self, tag):
        if tag in SKIP_TAGS and self._skip_depth > 0:
            self._skip_depth -= 1
        elif tag in ("p", "li", "h1", "h2", "h3", "h4", "h5", "h6"):
            self._chunks.append("\n")

    def handle_data(self, data):
        if self._skip_depth == 0:
            self._chunks.append(data)

    def text(self):
        return "".join(self._chunks)


def extract_text(html_path):
    parser = TextExtractor()
    with open(html_path, "r", encoding="utf-8") as fh:
        parser.feed(fh.read())
    return parser.text()


CHUNK_BYTES = 40_000


def chunk_text(text, max_bytes=CHUNK_BYTES):
    """Split text on paragraph boundaries into chunks under max_bytes each.

    The local LanguageTool server crashes ('Connection reset by peer') when
    fed multi-megabyte inputs in a single request, so we batch by paragraph.
    Yields (offset, chunk_text) pairs so callers can translate per-chunk
    offsets back to a global offset.
    """
    paragraphs = text.split("\n\n")
    buf = []
    buf_len = 0
    offset = 0
    chunk_start = 0
    for para in paragraphs:
        segment = para + "\n\n"
        if buf and buf_len + len(segment) > max_bytes:
            yield chunk_start, "".join(buf)
            chunk_start = offset
            buf = [segment]
            buf_len = len(segment)
        else:
            buf.append(segment)
            buf_len += len(segment)
        offset += len(segment)
    if buf:
        yield chunk_start, "".join(buf)


def run_languagetool(text, language="en-US"):
    try:
        import language_tool_python
    except ImportError:
        print(
            "language_tool_python is not installed; skipping LanguageTool check.",
            file=sys.stderr,
        )
        return None

    tool = language_tool_python.LanguageTool(language)
    all_matches = []
    try:
        for global_offset, chunk in chunk_text(text):
            try:
                matches = tool.check(chunk)
            except Exception as exc:  # noqa: BLE001 — advisory check must not crash CI
                print(
                    f"LanguageTool failed on chunk at offset {global_offset} ({len(chunk)} bytes): {exc}",
                    file=sys.stderr,
                )
                continue
            for m in matches:
                # Wrap match so callers see global offsets, not chunk-local.
                m._global_offset = global_offset + m.offset
                all_matches.append(m)
    finally:
        tool.close()
    return all_matches


def matches_to_json(matches, text):
    out = []
    for m in matches:
        # Translate offset to a line number in the plain-text input.
        offset = getattr(m, "_global_offset", m.offset)
        line = text.count("\n", 0, offset) + 1
        out.append({
            "rule": m.rule_id,
            "category": m.category,
            "message": m.message,
            "line": line,
            "offset": offset,
            "length": m.error_length,
            "context": m.context,
            "replacements": list(m.replacements[:5]),
        })
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--html", required=True, help="Rendered HTML input")
    parser.add_argument("--output", required=True, help="JSON output path")
    parser.add_argument("--language", default="en-US")
    args = parser.parse_args()

    text = extract_text(args.html)

    try:
        matches = run_languagetool(text, language=args.language)
    except Exception as exc:  # noqa: BLE001 — advisory check must not crash CI
        print(f"LanguageTool failed to start: {exc}", file=sys.stderr)
        matches = None
        run_status = "error"
        run_reason = str(exc)
    else:
        run_status = "ok" if matches is not None else "skipped"
        run_reason = None if matches is not None else "language_tool_python not installed"

    if matches is None:
        report = {"status": run_status, "reason": run_reason, "matches": [], "total": 0}
        issue_count = 0
    else:
        report = {"status": "ok", "matches": matches_to_json(matches, text), "total": len(matches)}
        issue_count = len(matches)

    os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as fh:
        json.dump(report, fh, indent=2)

    print(f"LanguageTool report written to {args.output} ({issue_count} match(es), status={report['status']}).")
    # Advisory check: never fails the build.
    return 0


if __name__ == "__main__":
    sys.exit(main())
