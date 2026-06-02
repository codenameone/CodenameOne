#!/usr/bin/env python3
"""Fail when a Markdown blog paragraph starts with a lowercase word.

This is the Markdown counterpart of
scripts/developer-guide/check_paragraph_capitalization.rb. It catches the class
of mistake from PR #5000 — a paragraph rendering as "many ways to animate…"
with no leading subject.

The Ruby version leans on the Asciidoctor parser because AsciiDoc block
structure is complex. Markdown paragraph boundaries are simple (blank-line
separated), so we detect them directly: strip the YAML front matter and Hugo
shortcodes, track fenced/indented code, group blank-line-separated runs into
paragraphs, and check the first prose word of each. This keeps the check
dependency-free (no python-markdown needed) and lets us report true source line
numbers.

Emits the same JSON shape as the Ruby script ({"total": N, "findings": [...]})
so reporting stays shared with summarize_reports.py.
"""

import argparse
import json
import os
import re
import sys

from blog_text import read_post, strip_shortcodes

# First tokens that are legitimately lowercase-led brand/identifier names.
ALLOWED_FIRST_TOKENS = {
    t.lower()
    for t in (
        "iOS iPhone iPad iPod iCloud iMac iTunes "
        "macOS tvOS watchOS visionOS "
        "iframe eBay"
    ).split()
}

# A paragraph whose first line begins with one of these is not prose we judge
# for capitalization: headings, blockquotes, list items, tables, thematic
# breaks, raw HTML, images, links, or a code span.
_BLOCK_MARKER_RE = re.compile(
    r"""^(?:
          \#                # ATX heading
        | >                 # blockquote
        | [-*+]\s           # bullet list item ("* " not emphasis "*word*")
        | \d+\\?[.)]\s      # ordered list item (markdown may escape the dot: "3\.")
        | \|                # table row
        | (?:-{3,}|\*{3,}|_{3,})\s*$  # thematic break
        | <                 # raw HTML / inline HTML block
        | !\[               # image
        | \[                # link / reference
        | `                 # inline code span
        | =                 # setext underline / front-matter leftovers
    )""",
    re.VERBOSE,
)

# Strip leading inline emphasis markers (*, _) so "*Note* that x" is judged on
# "Note". A run of these immediately followed by a word char is emphasis; "* "
# (with a space) was already classified as a list item above.
_LEADING_EMPHASIS_RE = re.compile(r"^[*_]{1,3}(?=\w)")

_FENCE_RE = re.compile(r"^\s*(?:```|~~~)")


def iter_paragraphs(body, body_start_line):
    """Yield (source_line_no, paragraph_text) for prose paragraphs in body.

    Skips fenced code blocks and indented (4-space) code lines. A paragraph is
    a maximal run of non-blank lines. source_line_no is the 1-based line of the
    paragraph's first line in the original file.
    """
    body = strip_shortcodes(body)
    lines = body.split("\n")
    in_fence = False
    para_lines = []
    para_start = None
    for idx, line in enumerate(lines):
        if _FENCE_RE.match(line):
            in_fence = not in_fence
            # A fence boundary also terminates any open paragraph.
            if para_lines:
                yield para_start, "\n".join(para_lines)
                para_lines = []
            continue
        if in_fence:
            continue
        if line.strip() == "":
            if para_lines:
                yield para_start, "\n".join(para_lines)
                para_lines = []
            continue
        if not para_lines:
            # First line of a new paragraph. Indented 4+ spaces => code block.
            if re.match(r"^ {4,}\S", line):
                # Treat the whole indented run as code; consume until blank.
                continue
            para_start = body_start_line + idx
        para_lines.append(line)
    if para_lines:
        yield para_start, "\n".join(para_lines)


def first_prose_word(paragraph):
    """Return the first prose word of a paragraph, or None to skip it."""
    first_line = paragraph.lstrip("\n")
    first_line = first_line.split("\n", 1)[0].strip()
    if not first_line:
        return None
    if _BLOCK_MARKER_RE.match(first_line):
        return None
    stripped = _LEADING_EMPHASIS_RE.sub("", first_line)
    if stripped.startswith("`"):
        return None  # code-span-led pseudo-list entry, case set by the language
    # One-word paragraphs are transitional connectors ("becomes", "to"), not
    # sentences — capitalization rules don't apply.
    if len(re.findall(r"\S+", stripped)) <= 1:
        return None
    first_word = re.search(r"[A-Za-z][A-Za-z0-9]*", stripped)
    if not first_word:
        return None
    word = first_word.group(0)
    if word.lower() in ALLOWED_FIRST_TOKENS:
        return None
    # Skip identifier-looking first tokens (qualified names / camelCase) that a
    # post wrote without backticks, e.g. "android.permission.X" or "isInitialized".
    extended = re.match(r"[A-Za-z][A-Za-z0-9._]*", stripped)
    extended_token = extended.group(0) if extended else word
    if "." in extended_token or re.search(r"[a-z][A-Z]", extended_token):
        return None
    if word[0].islower():
        return word
    return None


def find_lowercase_paragraphs(body, body_start_line, rel):
    """Return capitalization findings for a post body (text-level entry point)."""
    findings = []
    for line_no, paragraph in iter_paragraphs(body, body_start_line):
        word = first_prose_word(paragraph)
        if word is None:
            continue
        excerpt = re.sub(r"\s+", " ", paragraph).strip()[:120]
        findings.append(
            {"file": rel, "line": line_no, "word": word, "excerpt": excerpt}
        )
    return findings


def check_file(path, repo_root):
    front_matter, body, body_start_line = read_post(path)
    rel = os.path.relpath(path, repo_root) if repo_root else path
    return find_lowercase_paragraphs(body, body_start_line, rel)


def expand_paths(paths):
    files = []
    for path in paths:
        if os.path.isdir(path):
            for root, _dirs, names in os.walk(path):
                files.extend(
                    os.path.join(root, n) for n in sorted(names) if n.endswith(".md")
                )
        else:
            files.append(path)
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="+", help="Markdown files or directories")
    parser.add_argument("--output", help="Write a JSON report to this path")
    parser.add_argument(
        "--repo-root",
        default=os.getcwd(),
        help="Root used to relativize file paths in the report (default: cwd)",
    )
    args = parser.parse_args()

    files = expand_paths(args.paths)
    errors = []
    for path in files:
        errors.extend(check_file(path, args.repo_root))

    if args.output:
        os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
        with open(args.output, "w", encoding="utf-8") as fh:
            json.dump({"total": len(errors), "findings": errors}, fh, indent=2)

    if errors:
        print(
            f"Paragraph capitalization check failed: {len(errors)} paragraph(s) "
            f"start with a lowercase word.",
            file=sys.stderr,
        )
        for e in errors:
            print(f"  {e['file']}:{e['line']}: '{e['word']}' — {e['excerpt']}", file=sys.stderr)
        print(
            "\nEach flagged paragraph must be rewritten so its first prose word "
            "begins with a capital letter.\n"
            'Example: "many ways to animate..." → "There are many ways to animate..."',
            file=sys.stderr,
        )
        return 1

    print(f"Paragraph capitalization check passed: {len(files)} file(s), 0 issue(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
