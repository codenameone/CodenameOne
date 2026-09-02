#!/usr/bin/env python3
"""Report developer guide cross-references that point at nothing.

Asciidoctor does not fail, or even warn, on a dangling internal reference. A
bare ``<<missing>>`` at least renders as the literal text ``[missing]``, which a
reader might notice; but ``<<missing,Some words>>`` renders as an ordinary link
with the right words and a href to an id that does not exist, so it looks
perfectly fine and silently goes nowhere. The introduction shipped two of those
pointing at an "Application Lifecycle" sidebar nobody ever wrote.

Working from the rendered HTML rather than the AsciiDoc source means both forms
are caught, along with anchors declared in any of AsciiDoc's several syntaxes.
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
import tempfile
from collections import defaultdict
from pathlib import Path

ID_RE = re.compile(r'\bid="([^"]+)"')
HREF_RE = re.compile(r'href="#([^"]+)"')
# The guide renders as one page, so a RELATIVE href resolves against wherever that
# page happens to be served and reaches nothing that ships with it. Three survived
# from the wiki this book replaced -- link:Images[], link:Fonts[] and
# link:Supported-Properties#text-decoration[] -- naming pages that were never
# carried across, and a reader following one landed on a 404 while every gate
# reported success. Absolute URLs (checked by check-guide-links.py), root-relative
# paths and same-page fragments are all excluded.
RELATIVE_HREF_RE = re.compile(r'href="(?!#|/|[a-zA-Z][a-zA-Z0-9+.-]*:)([^"]+)"')
# Asciidoctor falls back to printing the raw id in brackets when a reference
# resolves to an anchor that carries no title -- an anchor on an image or a
# paragraph rather than on a section. The link works; the sentence reads
# "see [watch-complications]".
RAW_ID_LINK_RE = re.compile(r'<a href="#([^"]+)">\[\1\]</a>')
# Anchor syntaxes, used only to point the reader at the offending source file.
ANCHOR_SOURCE_RE = re.compile(r"<<([^>,]+)")


# The book has ifdef::backend-pdf[] branches, and the PDF is published alongside
# the HTML. Rendering only the default backend drops that content before any
# reference in it can be examined, so a dangling PDF-only xref would ship
# unchecked. Setting the attribute on an HTML render selects exactly the content
# the PDF build includes, without needing asciidoctor-pdf here. The two runs are
# checked SEPARATELY, never pooled: an anchor that exists only in the HTML branch
# must not be allowed to satisfy a reference made in the PDF branch.
BACKENDS = (("html", ()), ("pdf", ("backend-pdf",)))


def render(root: Path, attributes: tuple[str, ...] = ()) -> str:
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "guide.html"
        command = ["asciidoctor", "--require", "rouge"]
        for attribute in attributes:
            command += ["-a", attribute]
        command += ["-o", str(out), str(root)]
        result = subprocess.run(command, capture_output=True, text=True)
        if result.returncode != 0:
            print(result.stderr, file=sys.stderr)
            raise SystemExit("asciidoctor failed to render the guide")
        return out.read_text(encoding="utf-8")


def source_locations(guide_dir: Path, target: str) -> list[str]:
    """Find where a dangling target is referenced, so the error is actionable."""
    hits = []
    for path in sorted(guide_dir.rglob("*")):
        if path.suffix not in {".adoc", ".asciidoc"} or not path.is_file():
            continue
        for number, line in enumerate(path.read_text(encoding="utf-8").split("\n"), 1):
            for match in ANCHOR_SOURCE_RE.finditer(line):
                if match.group(1).strip() == target:
                    hits.append(f"{path.name}:{number}")
    return hits


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--guide-dir", default="docs/developer-guide", type=Path)
    args = parser.parse_args()

    guide_dir = args.guide_dir.resolve()
    root = guide_dir / "developer-guide.asciidoc"
    dangling: dict[str, int] = defaultdict(int)
    raw_id_links: dict[str, int] = defaultdict(int)
    relative: dict[str, int] = defaultdict(int)
    branches: dict[str, set[str]] = defaultdict(set)
    anchor_total = 0

    for name, attributes in BACKENDS:
        markup = render(root, attributes)
        ids = set(ID_RE.findall(markup))
        anchor_total = max(anchor_total, len(ids))
        for target in HREF_RE.findall(markup):
            if target not in ids:
                dangling[target] += 1
                branches[target].add(name)
        for target in RAW_ID_LINK_RE.findall(markup):
            raw_id_links[target] += 1
            branches[target].add(name)
        for target in RELATIVE_HREF_RE.findall(markup):
            relative[target] += 1
            branches[target].add(name)

    if not dangling and not raw_id_links and not relative:
        print(
            f"Cross-references OK: {anchor_total} anchors, every internal link "
            f"resolves in both the default and backend-pdf renders."
        )
        return 0

    for target in sorted(dangling):
        where = source_locations(guide_dir, target)
        location = ", ".join(where) if where else "not found in source (generated content?)"
        where_rendered = "/".join(sorted(branches[target])) + " render"
        print(
            f"{location}: <<{target}>> points at an id that does not exist in the "
            f"rendered book ({dangling[target]} reference(s), {where_rendered}).",
            file=sys.stderr,
        )
    for target in sorted(raw_id_links):
        where = source_locations(guide_dir, target)
        location = ", ".join(where) if where else "unknown"
        print(
            f"{location}: <<{target}>> resolves to an anchor with no title, so the "
            f"sentence renders the raw id as \"[{target}]\" "
            f"({raw_id_links[target]} reference(s)). Give the reference link text "
            f"(<<{target},some words>>) or move the anchor onto the section.",
            file=sys.stderr,
        )
    for target in sorted(relative):
        print(
            f"css.asciidoc or elsewhere: link:{target}[] renders as a relative URL "
            f"({relative[target]} reference(s)). The guide is one page, so this "
            f"reaches nothing that ships with it -- use an xref, or an absolute URL "
            f"if the destination really is off-site.",
            file=sys.stderr,
        )
    print(
        f"\n{len(dangling)} dangling, {len(raw_id_links)} untitled and "
        f"{len(relative)} relative cross-reference target(s).",
        file=sys.stderr,
    )
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
