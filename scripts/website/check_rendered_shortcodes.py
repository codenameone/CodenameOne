#!/usr/bin/env python3
"""Fail when a rendered page shows a Hugo shortcode as literal text.

A shortcode that Hugo does not expand still renders, as visible text such as
"{{< guide-block >}}" or a "{{< post-link path=..." with a link spliced into
it. Nothing else in the build reports this: Hugo exits 0, the prose gate reads
the Markdown, and the link checkers see a valid href in the garbage. It reaches
readers looking like a broken page.

The check reads the generated HTML, ignores <pre>, <code> and <script>
contents (where a post may legitimately show shortcode syntax), and reports
every page that still contains a shortcode delimiter.

Usage: check_rendered_shortcodes.py PUBLIC_DIR [--only blog]
"""
import argparse
import pathlib
import re
import sys

STRIP = re.compile(r"<(pre|code|script|style|textarea)\b.*?</\1\s*>", re.S | re.I)
LEAK = re.compile(r"\{\{(?:&lt;|<|%)\s*/?\s*[A-Za-z][\w-]*")


def leaks(html):
    body = STRIP.sub(" ", html)
    for m in LEAK.finditer(body):
        start = max(0, m.start() - 40)
        yield re.sub(r"\s+", " ", body[start:m.end() + 60])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("public_dir")
    ap.add_argument("--only", help="limit to this subdirectory, e.g. blog")
    args = ap.parse_args()
    root = pathlib.Path(args.public_dir)
    if args.only:
        root = root / args.only
    if not root.is_dir():
        print(f"Not a directory: {root}", file=sys.stderr)
        return 2
    bad = 0
    for page in sorted(root.rglob("*.html")):
        found = list(leaks(page.read_text(encoding="utf-8", errors="replace")))
        if found:
            bad += 1
            print(f"{page}: {len(found)} unexpanded shortcode(s)")
            for snippet in found[:3]:
                print(f"    ...{snippet}...")
    if bad:
        print(f"\n{bad} page(s) render shortcode syntax as text.", file=sys.stderr)
        return 1
    print("No unexpanded shortcodes in rendered pages.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
