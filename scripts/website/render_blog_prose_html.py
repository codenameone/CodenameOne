#!/usr/bin/env python3
"""Render blog Markdown posts to a single HTML file for LanguageTool.

scripts/developer-guide/run_languagetool.py consumes rendered HTML and extracts
the prose from it (skipping <pre>/<code>, headings, table cells). The developer
guide feeds it Asciidoctor output; the blog feeds it the HTML produced here.

We render the changed posts ourselves (front matter + Hugo shortcodes stripped,
then python-markdown) rather than depending on a full Hugo site build, so the
prose gate stays fast and isolated. Each post becomes an <article> so
LanguageTool's sentence detection never bleeds across post boundaries.
"""

import argparse
import html
import os
import sys

from blog_text import body_to_html, read_post


def render(paths, output):
    chunks = []
    for path in paths:
        _front_matter, body, _start = read_post(path)
        rendered = body_to_html(body)
        # A comment with the source path keeps the combined file debuggable;
        # the extractor ignores comments.
        chunks.append(f"<!-- {html.escape(path)} -->\n<article>\n{rendered}\n</article>")
    os.makedirs(os.path.dirname(output) or ".", exist_ok=True)
    with open(output, "w", encoding="utf-8") as fh:
        fh.write("<!DOCTYPE html>\n<html><body>\n")
        fh.write("\n\n".join(chunks))
        fh.write("\n</body></html>\n")


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
    parser.add_argument("--output", required=True, help="Combined HTML output path")
    args = parser.parse_args()
    files = expand_paths(args.paths)
    render(files, args.output)
    print(f"Rendered {len(files)} post(s) to {args.output}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
