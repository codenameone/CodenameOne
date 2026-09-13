#!/usr/bin/env python3
"""Check that no image macro's alt text is truncated by an unquoted comma.

AsciiDoc attribute lists are comma delimited, so an unquoted alt text containing
a comma is not one attribute: everything after the first comma is read as the
next positional attribute, which for ``image::`` is the width. Asciidoctor is
happy to do it, the book builds, the image appears -- and a screen reader gets
half a sentence::

    image::x.png[How the IDE, the proxy and the app connect,scaledwidth=95%]
    -> <img src="x.png" alt="How the IDE" width="the proxy and the app connect">

That rendered ``width`` is the symptom. The book is rendered in both backends
and the output read, which delegates every parsing question -- escaped quotes,
quoted values holding commas, attribute references, listing blocks, escaped
macros, line continuations -- to Asciidoctor, which cannot disagree with itself.

Fix a finding by quoting the alt text::

    image::x.png["How the IDE, the proxy and the app connect",scaledwidth=95%]

Two cases are deliberately out of scope, both documented rather than guessed at:

* A tail that is itself dimension-shaped. ``[Results, 2024]`` renders exactly
  like ``[Diagram,640]``, and only the author knows which was meant. Earlier
  revisions carried a baseline and an exemption for this; across ten review
  rounds that machinery never caught a real defect in this guide and repeatedly
  produced false positives, so it is gone. Naming dimensions (``width=640``,
  which the guide now does everywhere) keeps the ambiguity from arising.
* A tail that parses as an attribute assignment. ``[Plot coordinates x, y=2]``
  is stored as a named attribute, so nothing reaches the width slot and the
  render looks clean. Catching it needs a second parser over the source, which
  is what this script used to be and what it kept getting wrong.

Both are narrow, neither has occurred in the guide, and both would be caught by
the rule every figure here follows anyway: quote an alt text that contains a
comma.
"""
from __future__ import annotations

import argparse
import html
import re
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import List, Tuple

IMG_RE = re.compile(r"<img\b[^>]*>", re.I)
ATTR_RE = re.compile(r'(\w+)\s*=\s*"([^"]*)"')

# A rendered width/height is a number, optionally with a unit. Anything else in
# that slot is alt text that spilled into it.
DIMENSION_RE = re.compile(
    r"^\s*\d+(?:\.\d+)?\s*(?:%|px|pt|pc|em|rem|ex|in|cm|mm|vw|vh)?\s*$", re.I)

# The book has ifdef::backend-pdf[] branches and the PDF ships beside the HTML,
# so an image that exists only in the PDF is never seen by a default render. The
# xref checker models the PDF the same way: an HTML render with backend-pdf and
# basebackend-pdf defined. Its documented blind spot -- backend-html5 stays
# defined, so an ifdef::backend-html5[] branch would survive -- applies here
# too, and the guide has no such conditional.
BACKENDS = (("html", ()), ("pdf-surrogate", ("backend-pdf", "basebackend-pdf")))


def render(source: Path, attributes: Tuple[str, ...] = ()) -> str:
    """Render to HTML and return it, or an empty string if Asciidoctor refuses."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "out.html"
        command = ["asciidoctor", "--safe-mode=unsafe", "-a", "skip-front-matter"]
        for attribute in attributes:
            command += ["-a", attribute]
        command += ["-o", str(out), str(source)]
        proc = subprocess.run(command, capture_output=True, text=True)
        if proc.returncode != 0 or not out.exists():
            print(f"check-image-alt-text: could not render {source}", file=sys.stderr)
            if proc.stderr.strip():
                print(proc.stderr.strip()[:500], file=sys.stderr)
            return ""
        return out.read_text(encoding="utf-8", errors="replace")


def offenders(markup: str) -> List[Tuple[str, str, str]]:
    """Rendered images whose width or height holds something that is not one."""
    found = []
    for tag in IMG_RE.findall(markup):
        attrs = {k.lower(): v for k, v in ATTR_RE.findall(tag)}
        for slot in ("width", "height"):
            value = attrs.get(slot)
            if value is None or DIMENSION_RE.match(value):
                continue
            found.append((attrs.get("src", "?"),
                          html.unescape(attrs.get("alt", "")),
                          html.unescape(value)))
            break
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("sources", nargs="*",
                        help="AsciiDoc files to render (default: the whole guide)")
    args = parser.parse_args()

    sources = [Path(p) for p in args.sources] or [
        Path("docs/developer-guide/developer-guide.asciidoc")]

    total, seen = 0, set()
    for source in sources:
        for name, attributes in BACKENDS:
            markup = render(source, attributes)
            if not markup:
                return 1
            for src, alt, spilled in offenders(markup):
                # the same image usually appears in both renders; report it once
                if (src, alt, spilled) in seen:
                    continue
                seen.add((src, alt, spilled))
                total += 1
                where = "" if name == "html" else f" (in the {name} render)"
                print(f"{source}: {src}{where}")
                print(f'    alt text was cut to "{alt}"')
                print(f'    and "{spilled}" landed in the width slot')

    if total:
        print()
        print(f"check-image-alt-text: {total} image(s) whose alt text is cut short by a comma.")
        print('Quote the alt text: image::x.svg["a, b",scaledwidth=50%]')
        return 1
    print(f"check-image-alt-text: {len(sources)} document(s) rendered "
          f"in {len(BACKENDS)} backends, no truncated alt text.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
