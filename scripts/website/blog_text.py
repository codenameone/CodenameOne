"""Shared helpers for prose-checking Hugo Markdown blog posts.

The developer guide's prose tooling (scripts/developer-guide/) is AsciiDoc
oriented. The blog under docs/website/content/blog is Hugo Markdown with YAML
front matter and Hugo shortcodes, so before any prose check runs we strip that
non-prose scaffolding. Both check_paragraph_capitalization.py and
render_blog_prose_html.py import these helpers so the stripping stays
consistent (and, critically, so neither ever touches the front matter that the
syndication pipeline keys off — see scripts/website/syndicate_blog_posts.py).
"""

import re

# Leading YAML front matter: a "---" line, content, then a closing "---" line.
# Tolerates a UTF-8 BOM. DOTALL so "." spans the metadata block.
_FRONT_MATTER_RE = re.compile(r"\A﻿?---\n.*?\n---[ \t]*\n", re.DOTALL)

# Code-bearing PAIRED shortcodes whose body is a diagram/listing, not prose.
# The whole block (open tag, body, close tag) is removed so the diagram source
# (e.g. Mermaid "flowchart LR" with flattened node labels) never reaches the
# prose checkers. Other paired shortcodes (notice/tip/…) wrap real prose, so
# only their tags are stripped, by _SHORTCODE_RE below.
_PAIRED_CODE_RE = re.compile(
    r"\{\{[<%]\s*(mermaid|highlight|gist|code)\b.*?[%>]\}\}"
    r".*?"
    r"\{\{[<%]\s*/\1\s*[%>]\}\}",
    re.DOTALL,
)

# Hugo shortcodes: {{< ... >}} and {{% ... %}}. These are template directives,
# not prose.
_SHORTCODE_RE = re.compile(r"\{\{[<%].*?[%>]\}\}", re.DOTALL)


def split_front_matter(text):
    """Split a post into (front_matter, body, body_start_line).

    front_matter is "" when the post has no leading --- block. body_start_line
    is the 1-based line in the ORIGINAL file where the body begins, so callers
    can report findings against true source line numbers.
    """
    m = _FRONT_MATTER_RE.match(text)
    if not m:
        return "", text, 1
    front_matter = m.group(0)
    body = text[m.end():]
    body_start_line = front_matter.count("\n") + 1
    return front_matter, body, body_start_line


def strip_shortcodes(text):
    """Strip Hugo shortcodes so surrounding prose stays intact.

    Code-bearing paired shortcodes (mermaid/highlight/…) are removed body and
    all; every other shortcode tag is replaced with a space.
    """
    text = _PAIRED_CODE_RE.sub("\n\n", text)
    return _SHORTCODE_RE.sub(" ", text)


def read_post(path):
    """Read a post file and return (front_matter, body, body_start_line)."""
    with open(path, "r", encoding="utf-8") as fh:
        return split_front_matter(fh.read())


def body_to_html(body):
    """Render a post's Markdown to HTML for grammar checks.

    Strips YAML front matter defensively (so callers may pass either the body or
    the whole file — front matter must never reach the prose checkers) and Hugo
    shortcodes, then renders. Imports python-markdown lazily so the
    dependency-free paragraph check does not require it. fenced_code/tables
    produce <pre><code> and <table> markup that run_languagetool.py skips.
    """
    import markdown  # lazy: only render paths need it

    _fm, body, _start = split_front_matter(body)
    return markdown.markdown(
        strip_shortcodes(body),
        extensions=["fenced_code", "tables"],
    )
