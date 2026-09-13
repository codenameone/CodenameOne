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
#
# Dimension-SHAPED spill is the hard case and cannot be settled from the render:
# [Results, 2024] and [Diagram,640] both come out as alt="X" width="N", and only
# the author knows which was meant. Those are held against a baseline instead, so
# the ones already in the guide stay quiet and a new one has to be declared.
DIMENSION_RE = re.compile(r"^\s*\d+(?:\.\d+)?\s*(?:%|px|pt|pc|em|rem|ex|in|cm|mm|vw|vh)?\s*$", re.I)


# The book has ifdef::backend-pdf[] branches and the PDF ships beside the HTML,
# so an image that exists only in the PDF is never seen by a default render. The
# xref checker models the PDF the same way: an HTML render with backend-pdf and
# basebackend-pdf defined, which turns those branches on. Its documented blind
# spot -- backend-html5 stays defined, so an ifdef::backend-html5[] branch would
# survive into the surrogate -- applies here too, and the guide has no such
# conditional.
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


BASELINE = Path("scripts/developer-guide/image-dimension-baseline.txt")

# Named attributes an image macro takes. Used only to recognise a tail that
# Asciidoctor swallowed as an unknown named attribute: [Plot coordinates x,
# y=2 and z=3] becomes alt="Plot coordinates x" and a "y" attribute, with no
# width or height emitted at all, so the rendered output shows nothing wrong.
IMAGE_ATTRIBUTES = frozenset({
    "alt", "align", "caption", "float", "format", "height", "id", "link",
    "loading", "opts", "options", "poster", "role", "scale", "scaledwidth",
    "pdfwidth", "title", "width", "window", "rel", "nofollow", "start", "end",
    "loop", "autoplay", "theme", "lang", "fallback", "target", "reftext",
})
SOURCE_MACRO_RE = re.compile(r"image::?([^\[\]\s]+)\[([^\]]*)\]")
FIELD_NAME_RE = re.compile(r"^\s*([A-Za-z_][A-Za-z0-9_.-]*)\s*=")


def unquote(value: str) -> str:
    """Strip one layer of matching quotes, the way Asciidoctor reads a value."""
    value = value.strip()
    if len(value) >= 2 and value[0] in "\"'" and value[-1] == value[0]:
        return value[1:-1]
    return value


def split_fields(attrs: str) -> List[str]:
    """Split an attribute list on commas outside quoted values.

    A raw split loses title="coordinates x, y=2", which is valid and whose
    comma belongs to the value. A quote opens a value at the start of a field
    or straight after the "=" of a named one, and nowhere else -- an apostrophe
    in prose is not an opening quote.
    """
    out, buf, quote, can_open = [], [], "", True
    for ch in attrs:
        if quote:
            if ch == quote:
                quote = ""
            buf.append(ch)
            continue
        if ch == ",":
            out.append("".join(buf))
            buf, can_open = [], True
            continue
        if can_open and ch in "\"'":
            quote, can_open = ch, False
            buf.append(ch)
            continue
        if ch == "=":
            buf.append(ch)
            can_open = True
            continue
        if not ch.isspace():
            can_open = False
        buf.append(ch)
    out.append("".join(buf))
    return out


def scan_source(guide_dir: Path):
    """One pass over the macros, yielding facts per OCCURRENCE.

    Returns (tails, named) where tails are macros whose alt text is followed by
    a field naming no real image attribute -- Asciidoctor stores those as named
    attributes and emits no width, so the render shows nothing wrong -- and
    named identifies the occurrences that state width= or height= outright,
    which are unambiguous and need no baseline entry.

    Both are keyed by (src, alt) rather than by image, because the same image is
    reused with different captions and an exemption must not travel between
    them.
    """
    tails, named = [], set()
    for path in sorted(guide_dir.rglob("*")):
        if path.suffix not in {".adoc", ".asciidoc"} or not path.is_file():
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        for number, line in enumerate(text.splitlines(), 1):
            for match in SOURCE_MACRO_RE.finditer(line):
                src, attrs = match.group(1), match.group(2)
                fields = split_fields(attrs)
                if len(fields) < 2:
                    continue
                first = fields[0].strip()
                positional_alt = True
                named_alt = FIELD_NAME_RE.match(first)
                if named_alt and named_alt.group(1).lower() == "alt":
                    # The all-named form, image::x.png[alt="Diagram",width=640].
                    # The first field is an assignment, not positional alt text.
                    positional_alt = False
                    first = first[named_alt.end():].strip()
                alt = unquote(first)
                for field in fields[1:]:
                    name = FIELD_NAME_RE.match(field)
                    if not name:
                        continue
                    lowered = name.group(1).lower()
                    if lowered in {"width", "height"}:
                        # Keyed by slot: naming a height says nothing about a
                        # positional width sitting beside it, which may be the
                        # tail of the alt text.
                        named.add((src, alt, lowered))
                    elif lowered not in IMAGE_ATTRIBUTES:
                        if positional_alt and not first.startswith(('"', "'")):
                            tails.append((path, number, src, field.strip()))
                        break
    return tails, named


def load_baseline() -> set:
    if not BASELINE.exists():
        return set()
    entries = set()
    for line in BASELINE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            entries.add(line)
    return entries


def offenders(markup: str, baseline: set, named_dimensions: set) -> List[Tuple[str, str, str, bool]]:
    """Rendered images whose width or height is not one, or is undeclared.

    Returns (src, alt, value, ambiguous). ambiguous marks the dimension-shaped
    case, where the render cannot say whether the author meant a width or lost
    the tail of their alt text.
    """
    found = []
    for tag in IMG_RE.findall(markup):
        attrs = {k.lower(): v for k, v in ATTR_RE.findall(tag)}
        for slot in ("width", "height"):
            value = attrs.get(slot)
            if value is None:
                continue
            src = attrs.get("src", "?")
            alt = html.unescape(attrs.get("alt", ""))
            if not DIMENSION_RE.match(value):
                found.append((src, alt, html.unescape(value), False))
                break
            if (src, alt, slot) in named_dimensions:
                continue
            key = f"{src}|{slot}|{value}|{alt}"
            if key not in baseline:
                found.append((src, alt, f"{slot}={value}", True))
                break
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("sources", nargs="*",
                        help="AsciiDoc files to render (default: the whole guide)")
    args = parser.parse_args()

    sources = [Path(p) for p in args.sources] or [
        Path("docs/developer-guide/developer-guide.asciidoc")]

    guide_dir = Path("docs/developer-guide")
    baseline = load_baseline()
    tails, named_dimensions = scan_source(guide_dir)
    total = 0
    seen = set()
    for source in sources:
        for name, attributes in BACKENDS:
            markup = render(source, attributes)
            if not markup:
                return 1
            for src, alt, spilled, ambiguous in offenders(markup, baseline, named_dimensions):
                # the same image usually appears in both renders; report it once
                if (src, alt, spilled) in seen:
                    continue
                seen.add((src, alt, spilled))
                total += 1
                where = "" if name == "html" else f" (in the {name} render)"
                print(f"{source}: {src}{where}")
                if ambiguous:
                    print(f'    renders as alt="{alt}" with an undeclared {spilled}')
                    print("    If that is a real width, add it to "
                          f"{BASELINE}. If it is the tail of the alt")
                    print("    text, quote the alt text instead.")
                else:
                    print(f'    alt text was cut to "{alt}"')
                    print(f'    and "{spilled}" landed in the width slot')

    for path, number, src, field in tails:
        total += 1
        print(f"{path}:{number}: {src}")
        print(f'    "{field}" is not an image attribute, so Asciidoctor takes it')
        print("    as a named one and cuts the alt text at the comma before it")

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
