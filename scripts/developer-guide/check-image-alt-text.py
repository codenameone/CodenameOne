#!/usr/bin/env python3
"""Check that no image macro's alt text is truncated by an unquoted comma.

AsciiDoc attribute lists are comma delimited, so an unquoted alt text containing
a comma is not one attribute: everything after the first comma is read as the
next positional attribute, which for ``image::`` is the width. Asciidoctor is
happy to do it, the book builds, the image appears -- and a screen reader gets
half a sentence::

    image::x.png[How the IDE, the proxy and the app connect,scaledwidth=95%]
    -> <img src="x.png" alt="How the IDE" width="the proxy and the app connect">

That rendered ``width`` is the symptom, and it is what this checks. An earlier
version of this script parsed the attribute list itself and lost repeatedly to
escaped quotes, quoted values containing commas, attribute references, listing
blocks, escaped macros and line continuations -- every one of them a case where
reimplementing the parser disagreed with the parser. Rendering the book and
reading the output delegates all of that to Asciidoctor, which cannot disagree
with itself.

Fix a finding by quoting the alt text::

    image::x.png["How the IDE, the proxy and the app connect",scaledwidth=95%]
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
DIMENSION_RE = re.compile(r"^\s*\d+(?:\.\d+)?\s*(?:%|px|pt|pc|em|rem|ex|in|cm|mm|vw|vh)?\s*$", re.I)


def render(source: Path) -> str:
    """Render to HTML and return it, or an empty string if Asciidoctor refuses."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "out.html"
        proc = subprocess.run(
            ["asciidoctor", "--safe-mode=unsafe", "-a", "skip-front-matter",
             "-o", str(out), str(source)],
            capture_output=True, text=True)
        if proc.returncode != 0 or not out.exists():
            print(f"check-image-alt-text: could not render {source}", file=sys.stderr)
            if proc.stderr.strip():
                print(proc.stderr.strip()[:500], file=sys.stderr)
            return ""
        return out.read_text(encoding="utf-8", errors="replace")


def offenders(markup: str) -> List[Tuple[str, str, str]]:
    """Every rendered image whose width or height holds something that is not one."""
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

    total = 0
    for source in sources:
        markup = render(source)
        if not markup:
            return 1
        for src, alt, spilled in offenders(markup):
            total += 1
            print(f"{source}: {src}")
            print(f'    alt text was cut to "{alt}"')
            print(f'    and "{spilled}" landed in the width slot')

    if total:
        print()
        print(f"check-image-alt-text: {total} image(s) whose alt text is cut short by a comma.")
        print('Quote the alt text: image::x.svg["a, b",scaledwidth=50%]')
        return 1
    print(f"check-image-alt-text: {len(sources)} document(s) rendered, no truncated alt text.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
