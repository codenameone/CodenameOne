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

# image::target[attrlist] and the inline image:target[attrlist]
IMAGE_RE = re.compile(r"^(?P<indent>\s*)image::?(?P<target>[^\[\]\s]+)\[(?P<attrs>.*)\]\s*$")


def split_attrs(attrs: str) -> List[str]:
    """Split on commas that are not inside quotes, the way AsciiDoc does."""
    out: List[str] = []
    buf: List[str] = []
    quote = ""
    for ch in attrs:
        if quote:
            if ch == quote:
                quote = ""
            buf.append(ch)
        elif ch in "\"'":
            quote = ch
            buf.append(ch)
        elif ch == ",":
            out.append("".join(buf))
            buf = []
        else:
            buf.append(ch)
    out.append("".join(buf))
    return out


def offenders(path: Path) -> List[Tuple[int, str, str]]:
    found = []
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        match = IMAGE_RE.match(line)
        if not match:
            continue
        fields = split_attrs(match.group("attrs"))
        if len(fields) < 2:
            continue
        first = fields[0].strip()
        if first.startswith(('"', "'")):
            # already quoted, so its commas belong to the alt text
            continue
        # A positional field after the alt text that carries no "=" is alt text
        # that was split, not an attribute anybody wrote on purpose. A bare
        # number is the legacy width/height positional form and is legitimate.
        for field in fields[1:]:
            value = field.strip()
            if not value or "=" in value or value.isdigit():
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
