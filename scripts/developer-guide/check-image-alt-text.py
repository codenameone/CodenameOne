#!/usr/bin/env python3
"""Check that an image macro's alt text is a single attribute.

AsciiDoc attribute lists are comma delimited, so an unquoted alt text containing
a comma is not one attribute: everything after the first comma is read as the
next positional attribute, which for ``image::`` is the width. A figure written
as::

    image::img/x.svg[How the IDE, the proxy and the app connect,scaledwidth=95%]

renders ``alt="How the IDE"`` and puts ``the proxy and the app connect`` in the
width slot. Screen reader users get a truncated description and the width is
nonsense. Quoting the string fixes it::

    image::img/x.svg["How the IDE, the proxy and the app connect",scaledwidth=95%]

The failure is silent in every other gate: asciidoctor accepts it, the HTML
builds, and the image renders.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import List, Tuple

ASCIIDOC_EXTENSIONS = {".adoc", ".asciidoc"}

# Block form image::target[attrs] and the inline form image:target[attrs], which
# appears mid sentence, so this is searched rather than anchored to a whole line.
IMAGE_RE = re.compile(r"image::?(?P<target>[^\[\]\s]+)\[(?P<attrs>[^\]]*)\]")


def split_attrs(attrs: str) -> List[str]:
    """Split on commas that are not inside a quoted value, the way AsciiDoc does.

    A quote opens a value in two places: the start of a field, and straight
    after the "=" of a named attribute, which is where link="https://a/b,c"
    puts it. Anywhere else it is ordinary text -- the apostrophe in "Don't
    change the classpath, ..." is not an opening quote, and treating it as one
    swallows the comma after it and reports a broken macro as clean.
    """
    out: List[str] = []
    buf: List[str] = []
    quote = ""
    can_open = True
    for ch in attrs:
        if quote:
            if ch == quote:
                quote = ""
            buf.append(ch)
            continue
        if ch == ",":
            out.append("".join(buf))
            buf = []
            can_open = True
            continue
        if can_open and ch in "\"'":
            quote = ch
            buf.append(ch)
            can_open = False
            continue
        if ch == "=":
            # a named attribute's value begins here, and it may be quoted
            buf.append(ch)
            can_open = True
            continue
        if not ch.isspace():
            can_open = False
        buf.append(ch)
    out.append("".join(buf))
    return out


# A positional width or height: a number, optionally with a unit. "50%" is as
# valid as "640", and rejecting it would fail CI on a correct attribute list.
DIMENSION_RE = re.compile(r"^\d+(?:\.\d+)?(?:%|px|pt|pc|em|rem|ex|in|cm|mm|vw|vh)?$")

# Named attributes an image macro actually takes. Checked by name rather than by
# the presence of an "=", because alt text is prose and prose contains equals
# signs: "Plot coordinates x, y=2 and z=3" splits into a field holding "y=2 and
# z=3", which an any-equals test waves through while the rendered alt text is
# just "Plot coordinates x".
IMAGE_ATTRIBUTES = frozenset({
    "alt", "align", "caption", "float", "format", "height", "id", "link",
    "opts", "options", "poster", "role", "scale", "scaledwidth", "pdfwidth",
    "title", "width", "window", "rel", "nofollow", "start", "end", "loop",
    "autoplay", "theme", "lang", "fallback", "target", "reftext",
})
NAMED_ATTRIBUTE_RE = re.compile(r"^([A-Za-z_][A-Za-z0-9_.-]*)\s*=")


def is_named_attribute(field: str) -> bool:
    """True when the field is a named image attribute rather than split alt text."""
    match = NAMED_ATTRIBUTE_RE.match(field.strip())
    return bool(match) and match.group(1).lower() in IMAGE_ATTRIBUTES


def offenders(path: Path) -> List[Tuple[int, str, str]]:
    found = []
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        for match in IMAGE_RE.finditer(line):
            fields = split_attrs(match.group("attrs"))
            if len(fields) < 2:
                continue
            first = fields[0].strip()
            if first.startswith(('"', "'")):
                # already quoted, so its commas belong to the alt text
                continue
            # A positional field after the alt text that carries no "=" is alt
            # text that was split, not an attribute anybody wrote on purpose. A
            # dimension is the legacy width/height positional form and is
            # legitimate, units included.
            for field in fields[1:]:
                value = field.strip()
                if not value or is_named_attribute(value) or DIMENSION_RE.match(value):
                    continue
                found.append((number, match.group("target"), match.group("attrs")))
                break
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="*", default=None,
                        help="files or directories to check (default: the developer guide)")
    args = parser.parse_args()

    roots = [Path(p) for p in args.paths] if args.paths else [Path("docs/developer-guide")]
    files: List[Path] = []
    for root in roots:
        if root.is_dir():
            files.extend(sorted(p for p in root.rglob("*") if p.suffix in ASCIIDOC_EXTENSIONS))
        elif root.suffix in ASCIIDOC_EXTENSIONS:
            files.append(root)

    total = 0
    for path in files:
        for number, target, attrs in offenders(path):
            total += 1
            print(f"{path}:{number}: alt text is split by a comma; quote it")
            print(f"    image::{target}[{attrs}]")

    if total:
        print()
        print(f"check-image-alt-text: {total} image macro(s) whose alt text is cut short.")
        print('Wrap the alt text in double quotes: image::x.svg["a, b",scaledwidth=50%]')
        return 1
    print(f"check-image-alt-text: {len(files)} file(s) clean.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
