#!/usr/bin/env python3
"""Assert the Hugo API pages answer to every URL the standard doclet publishes.

The website renders the API from `maven/javadoc-hugo-doclet` while the
downloadable zip is still produced by the standard doclet. That split only works
while the two agree on addresses: 304 distinct `/javadoc/...` URLs are linked
from the site content and the developer guide, roughly 2200 more are written in
the directory form, and an unknown number of links point at the site from
outside the project. A fragment is part of the URL, and javadoc's fragment
encoding has details that are easy to get wrong by hand -- type arguments erased
away, arrays keeping their brackets, varargs keeping an ellipsis, a type
variable answering to both its declared spelling and its erasure.

Nothing about that is checkable by reading the generator. This compares the two
renderings of the same sources, so a divergence fails a build rather than
turning into a dead link nobody reports.

Usage:
  scripts/website/check-javadoc-parity.py <standard-doclet-dir> <hugo-public-javadoc-dir>
"""

from __future__ import annotations

import html
import pathlib
import re
import sys

ID_RE = re.compile(r'id="([^"]*)"')

# Pages javadoc writes for its own machinery rather than for an API element.
# None of these is a documented type, and the site provides its own equivalents
# (search for the index, the overview page for the package list).
CHROME_PAGES = {
    "index.html",
    "overview-tree.html",
    "overview-summary.html",
    "allclasses-index.html",
    "allpackages-index.html",
    "allclasses.html",
    "allclasses-frame.html",
    "index-all.html",
    "deprecated-list.html",
    "help-doc.html",
    "serialized-form.html",
    "search.html",
    "system-properties.html",
    "constant-values.html",
    "new-list.html",
}

CHROME_DIRS = ("legal/", "resource-files/", "script-dir/", "resources/")

# Per-package class hierarchy pages. Not generated: the type pages carry the
# inheritance chain and the known subtypes, which is what a reader wanted from
# them, and no link in the tree points at one.
SKIPPED_PAGE_SUFFIX = "package-tree.html"

# Identifiers javadoc puts on its own page furniture rather than on a member.
CHROME_ID_RE = re.compile(
    r"^(class|constructor|method|field|nested-class|property|enum-constant|annotation)"
    r"|^(navbar|skip-navbar|search-input|reset-search|type-param-|related-package)"
    # Structural anchors on the summary pages, not addresses of an API element.
    r"|^(package-description|package-summary|uses-of|hierarchy)$"
)

# Identifiers javadoc auto-generates for headings inside a comment body.
#
# This codebase writes parameters and return values as markdown headings rather
# than as block tags -- 9068 "#### Parameters" against 816 "@param" -- so the
# standard doclet renders them as <h6 id="parameters-heading12"> buried in the
# description. They are an artifact of that rendering, not addresses: the Hugo
# pages turn the same headings into real parameter tables. A member identifier
# cannot collide with this pattern, because javadoc spells members as a bare
# Java identifier or as one followed by a parenthesised signature, and neither
# can contain a hyphen.
# Hyphens run together freely here, because the slug is generated from the
# heading text: a "Deprecated" heading whose sentence contains a dash yields
# an identifier with four consecutive hyphens in it.
PROSE_ID_RE = re.compile(r"^[a-z0-9-]+-heading\d*$")


def pages(root: pathlib.Path, drop_alias_indexes: bool = False) -> set[str]:
    found = set()
    for path in root.rglob("*.html"):
        relative = path.relative_to(root).as_posix()
        if relative.split("/")[-1] in CHROME_PAGES:
            continue
        if relative.startswith(CHROME_DIRS):
            continue
        if relative.endswith(SKIPPED_PAGE_SUFFIX):
            continue
        # The Hugo build adds a directory spelling of each type page as an alias,
        # which the standard doclet has no equivalent for. Extra addresses are
        # not a compatibility problem, so they are not compared.
        if drop_alias_indexes and relative.endswith("/index.html"):
            continue
        found.add(relative)
    return found


def anchors(path: pathlib.Path) -> set[str]:
    text = path.read_text(errors="replace")
    found = set()
    for raw in ID_RE.findall(text):
        value = html.unescape(raw)
        if CHROME_ID_RE.match(value) or PROSE_ID_RE.match(value):
            continue
        found.add(value)
    return found


def main(argv: list[str]) -> int:
    if len(argv) != 3:
        print(__doc__.strip(), file=sys.stderr)
        return 2

    standard = pathlib.Path(argv[1])
    hugo = pathlib.Path(argv[2])
    for root in (standard, hugo):
        if not root.is_dir():
            print(f"check-javadoc-parity: not a directory: {root}", file=sys.stderr)
            return 2

    standard_pages = pages(standard)
    hugo_pages = pages(hugo, drop_alias_indexes=True)

    failures = 0

    missing_pages = sorted(standard_pages - hugo_pages)
    if missing_pages:
        failures += len(missing_pages)
        print(f"{len(missing_pages)} API page(s) the standard doclet publishes and the site does not:")
        for page in missing_pages[:40]:
            print(f"  - /javadoc/{page}")
        if len(missing_pages) > 40:
            print(f"  ... and {len(missing_pages) - 40} more")

    missing_anchors = 0
    checked_anchors = 0
    for page in sorted(standard_pages & hugo_pages):
        expected = anchors(standard / page)
        actual = anchors(hugo / page)
        checked_anchors += len(expected)
        gap = sorted(expected - actual)
        if not gap:
            continue
        missing_anchors += len(gap)
        print(f"/javadoc/{page}: {len(gap)} fragment(s) missing")
        for anchor in gap[:10]:
            print(f"  - #{anchor}")
        if len(gap) > 10:
            print(f"  ... and {len(gap) - 10} more")

    failures += missing_anchors

    print()
    print(f"pages compared:    {len(standard_pages & hugo_pages)}")
    print(f"fragments checked: {checked_anchors}")
    if failures:
        print(f"FAILED: {len(missing_pages)} missing page(s), {missing_anchors} missing fragment(s)")
        return 1
    print("OK: every published API address is answered by the site")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
