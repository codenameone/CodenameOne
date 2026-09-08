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

import collections
import pathlib
import re
import sys
import urllib.parse
from html.parser import HTMLParser

class _LinkCollector(HTMLParser):
    """Collects ids and internal API hrefs from one page."""

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.ids: set[str] = set()
        self.hrefs: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        values = dict(attrs)
        identifier = values.get("id")
        if identifier:
            self.ids.add(identifier)
        href = values.get("href")
        if tag == "a" and href and href.startswith("/javadoc/"):
            self.hrefs.append(href)

    def handle_startendtag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self.handle_starttag(tag, attrs)


def check_internal_links(hugo: pathlib.Path) -> int:
    """Every /javadoc/ link on the site must land on a page and an id that exist.

    Separate from the parity comparison, and worth its own pass: the generator
    mints these addresses itself rather than copying them, so it can invent one
    that resolves to nothing while still reproducing every address javadoc
    publishes. Both defects this caught were of that shape -- a member promoted
    off a package private supertype linking into the page that supertype would
    have had, and a constructor fragment mangled by markdown because "<init>"
    reads as a delimiter inside a link destination.
    """
    pages = {p.relative_to(hugo).as_posix() for p in hugo.rglob("*.html")}
    parsed: dict[str, _LinkCollector] = {}
    for path in hugo.rglob("*.html"):
        collector = _LinkCollector()
        collector.feed(path.read_text(errors="replace"))
        collector.close()
        parsed[path.relative_to(hugo).as_posix()] = collector

    missing_pages: collections.Counter = collections.Counter()
    missing_anchors: collections.Counter = collections.Counter()
    checked = 0
    for collector in parsed.values():
        for href in collector.hrefs:
            path_part, _, fragment = href.partition("#")
            target = path_part[len("/javadoc/"):] or "index.html"
            if target.endswith("/"):
                target += "index.html"
            if target not in pages:
                missing_pages[target] += 1
                continue
            if not fragment:
                continue
            checked += 1
            # A fragment is compared after percent decoding, which is why
            # encoding one is safe in the first place.
            if urllib.parse.unquote(fragment) not in parsed[target].ids:
                missing_anchors[f"{target}#{urllib.parse.unquote(fragment)}"] += 1

    total = sum(missing_pages.values()) + sum(missing_anchors.values())
    print(f"internal links checked: {checked} fragment link(s) across {len(pages)} page(s)")
    if not total:
        print("OK: every internal API link resolves")
        return 0
    if missing_pages:
        print(f"{sum(missing_pages.values())} link(s) to a page that does not exist:")
        for target, count in missing_pages.most_common(15):
            print(f"  {count:5d}  /javadoc/{target}")
    if missing_anchors:
        print(f"{sum(missing_anchors.values())} link(s) to an id that does not exist:")
        for target, count in missing_anchors.most_common(15):
            print(f"  {count:5d}  /javadoc/{target}")
    return 1


class _IdCollector(HTMLParser):
    """Collects every element's id attribute.

    A real parser rather than a regex over the text, because only one side of
    this comparison is minified and a pattern that copes with both is a pattern
    that reads too much. The site is built with --minify and the minifier drops
    the quotes wherever HTML allows -- `id=top`, and a parenthesised signature is
    legal unquoted too -- so a quotes-only pattern saw almost no fragments on the
    minified side and reported 27784 of 29583 as missing on a build that was
    correct. Widening it to accept unquoted values then matched Java source
    inside <pre> blocks: `int id = row.getInteger(0);` is not an attribute.

    HTMLParser knows the difference between markup and text, which is the whole
    problem, and it resolves character references on the way through.
    """

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.ids: set[str] = set()

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        for name, value in attrs:
            if name == "id" and value:
                self.ids.add(value)

    # Void and self-closing elements arrive here instead.
    def handle_startendtag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self.handle_starttag(tag, attrs)


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

# Floors that make a vacuous pass impossible. See the check in main().
MINIMUM_PAGES = 1000
MINIMUM_FRAGMENTS = 10000

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
    collector = _IdCollector()
    collector.feed(path.read_text(errors="replace"))
    collector.close()
    return {
        value
        for value in collector.ids
        if not CHROME_ID_RE.match(value) and not PROSE_ID_RE.match(value)
    }


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

    # A comparison that compared almost nothing reports nothing missing, which
    # reads exactly like success. The API is roughly 2270 pages and 29500
    # fragments, so these floors are far below any real tree and only trip when
    # one side failed to generate or the parser stopped recognising anything --
    # which is precisely what a quotes-only id pattern did against the minified
    # build, silently on the passing side.
    compared = len(standard_pages & hugo_pages)
    if compared < MINIMUM_PAGES or checked_anchors < MINIMUM_FRAGMENTS:
        print(f"only {compared} page(s) and {checked_anchors} fragment(s) were compared; "
              f"expected at least {MINIMUM_PAGES} and {MINIMUM_FRAGMENTS}. "
              "One side did not generate, or nothing was parsed out of it.")
        return 1

    print()
    print(f"pages compared:    {compared}")
    print(f"fragments checked: {checked_anchors}")
    if failures:
        print(f"FAILED: {len(missing_pages)} missing page(s), {missing_anchors} missing fragment(s)")
        return 1
    print("OK: every published API address is answered by the site")

    print()
    return check_internal_links(hugo)


if __name__ == "__main__":
    sys.exit(main(sys.argv))
