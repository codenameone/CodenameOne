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
import html
import json
import pathlib
import re
import sys
import urllib.parse

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
# Anchored end to end on purpose. The first version matched any id merely
# *starting* with one of these words, which quietly removed real members from the
# comparison: methodType(...), fieldSubmitted(...), propertyChanged(...),
# annotationType() and propertyNames() were all excluded, so a regression in
# exactly those published URLs would have passed the compatibility gate.
CHROME_ID_RE = re.compile(
    r"^(?:"
    r"class-description"
    r"|(?:constructor|method|field|property|enum-constant|nested-class"
    r"|annotation-interface[a-z-]*)-(?:summary|detail)(?:-table[A-Za-z0-9._-]*)?"
    # The type table on a package page, and the tab controls javadoc gives it.
    r"|class-summary(?:-tab[0-9]+|\.tabpanel)?"
    r"|(?:methods|fields|nested-classes|properties)-inherited-from-(?:class|interface)-[A-Za-z0-9_.$]*"
    r"|navbar[A-Za-z0-9._-]*|skip-navbar[A-Za-z0-9._-]*|search-input|reset-search"
    r"|type-param-[A-Za-z0-9_$]*|related-package-summary"
    r"|package-description|package-summary|uses-of|hierarchy"
    r")$"
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


# Tags are found by scanning rather than with html.parser, and that is not a
# preference. HTMLParser switches into raw-text mode on <title>, <script> and
# friends and stops recognising markup until the matching close tag -- and
# documentation prose contains those. com.codename1.push.PushBuilder documents a
# payload as "<title>;<body>" with no close tag, so everything after it on the
# page went unseen: build(), getType(), isRichPush() and its constructor. Across
# the standard tree that lost 1809 ids on 1150 pages, every one of them an
# address this gate reports as checked. Deleting any of those anchors from the
# Hugo side would have passed.
#
# A regex over the whole text is the other wrong answer, tried earlier: it reads
# Java source in a <pre> block, where "int id = row.getInteger(0);" is not an
# attribute. Scanning only inside <...> avoids both, and copes with the minified
# build's unquoted attribute values.
_TAG_START_RE = re.compile(r"<(/?)([A-Za-z][A-Za-z0-9:-]*)")
_ATTR_RE = re.compile(
    r"""([A-Za-z_:][-A-Za-z0-9_:.]*)\s*(?:=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+)))?"""
)


def scan_tags(text: str):
    """Yields (tag name, attributes) for every start tag in the document."""
    i = 0
    n = len(text)
    while i < n:
        match = _TAG_START_RE.search(text, i)
        if not match:
            return
        if match.group(1):  # a close tag carries no attributes worth reading
            i = match.end()
            continue
        # Find the tag's end, honouring quoted attribute values so that a ">"
        # inside one does not end it early.
        j = match.end()
        quote = None
        while j < n:
            c = text[j]
            if quote:
                if c == quote:
                    quote = None
            elif c in "\"'":
                quote = c
            elif c == ">":
                break
            j += 1
        attributes = {}
        for attr in _ATTR_RE.finditer(text, match.end(), j):
            value = next((g for g in attr.groups()[1:] if g is not None), "")
            attributes.setdefault(attr.group(1).lower(), value)
        yield match.group(2).lower(), attributes
        i = j + 1


def collect(path: pathlib.Path) -> tuple[set[str], list[str]]:
    """Every id on a page, and every internal API link it makes."""
    text = path.read_text(errors="replace")
    ids: set[str] = set()
    hrefs: list[str] = []
    for tag, attributes in scan_tags(text):
        identifier = attributes.get("id")
        if identifier:
            ids.add(html.unescape(identifier))
        href = attributes.get("href")
        if tag == "a" and href and href.startswith("/javadoc/"):
            hrefs.append(html.unescape(href))
    return ids, hrefs


def check_search_index(hugo: pathlib.Path, index: pathlib.Path) -> int:
    """Every entry in the API search index must land on a page and an id.

    Its own pass because the index is JSON, not markup: the link checker walks
    <a href> attributes and cannot see it at all. That blind spot shipped a real
    defect -- constructors promoted from an undocumented superclass were indexed
    against <init> fragments on the subclass page, which does not render them --
    so search offered results that went nowhere.
    """
    if not index.exists():
        print(f"search index not found at {index}; skipping")
        return 0

    pages = {p.relative_to(hugo).as_posix() for p in hugo.rglob("*.html")}
    ids: dict[str, set[str]] = {}
    for path in hugo.rglob("*.html"):
        ids[path.relative_to(hugo).as_posix()] = collect(path)[0]

    payload = json.loads(index.read_text())
    missing: collections.Counter = collections.Counter()
    checked = 0
    for entry in payload.get("types", []):
        url = entry.get("u", "")
        if not url.startswith("/javadoc/"):
            continue
        target = url[len("/javadoc/"):] or "index.html"
        # Same normalisation the link pass does: a directory URL is served by the
        # index.html inside it. Without this every entry looked broken.
        if target.endswith("/") or not target:
            target += "index.html"
        checked += 1
        if target not in pages:
            missing[url] += 1
            continue
        for member in entry.get("m", []) or []:
            if len(member) != 2:
                continue
            checked += 1
            if member[1] not in ids[target]:
                missing[f"{url}#{member[1]}"] += 1

    print(f"search index entries checked: {checked}")
    if not missing:
        print("OK: every search result lands on something that exists")
        return 0
    print(f"{sum(missing.values())} search entr(ies) pointing at nothing:")
    for target, count in missing.most_common(15):
        print(f"  {count:5d}  {target}")
    return 1


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
    parsed: dict[str, tuple[set[str], list[str]]] = {}
    for path in hugo.rglob("*.html"):
        parsed[path.relative_to(hugo).as_posix()] = collect(path)

    missing_pages: collections.Counter = collections.Counter()
    missing_anchors: collections.Counter = collections.Counter()
    checked = 0
    for _, hrefs in parsed.values():
        for href in hrefs:
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
            if urllib.parse.unquote(fragment) not in parsed[target][0]:
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


def pages(root: pathlib.Path, hugo: bool = False) -> set[str]:
    """The API pages a tree publishes, keyed by the standard doclet's spelling.

    The two trees spell the same page differently and have to. Cloudflare Pages
    will not serve a .html URL at all -- its own html_handling redirects /x.html
    to /x before an asset is considered, and the site's redirect table maps
    /*.html to /:splat/ on top of that -- so the Hugo pages are published at
    Label/index.html and the javadoc spelling reaches them by redirect. Comparing
    the raw filenames would report all 2272 pages missing.
    """
    found = set()
    for path in root.rglob("*.html"):
        relative = path.relative_to(root).as_posix()
        # Normalise BEFORE the filters, not after: every Hugo page is named
        # index.html, and index.html is in CHROME_PAGES, so filtering first
        # discarded the entire tree and the comparison silently had nothing left
        # to do. The floor in main() is what caught that.
        if hugo and relative.endswith("/index.html"):
            relative = relative[: -len("/index.html")] + ".html"
        if relative.split("/")[-1] in CHROME_PAGES:
            continue
        if relative.startswith(CHROME_DIRS):
            continue
        if relative.endswith(SKIPPED_PAGE_SUFFIX):
            continue
        found.add(relative)
    return found


def hugo_path(hugo: pathlib.Path, page: str) -> pathlib.Path:
    """The file behind a standard-doclet page name in the Hugo tree."""
    direct = hugo / page
    if direct.exists():
        return direct
    return hugo / page[: -len(".html")] / "index.html"


def anchors(path: pathlib.Path) -> set[str]:
    ids, _ = collect(path)
    return {
        value
        for value in ids
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
    hugo_pages = pages(hugo, hugo=True)

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
        actual = anchors(hugo_path(hugo, page))
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
    if check_internal_links(hugo):
        return 1

    print()
    return check_search_index(hugo, hugo.parent / "javadoc-search.json")


if __name__ == "__main__":
    sys.exit(main(sys.argv))
