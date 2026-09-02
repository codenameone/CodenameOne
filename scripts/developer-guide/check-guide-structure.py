#!/usr/bin/env python3
"""Verify that the developer guide's book structure is what the manifest says.

Three defects motivated this check, all of which shipped in a green build:

* ``Working-With-Linux.asciidoc`` was included on the line directly after
  ``Working-With-Windows.asciidoc``, whose last line is a paragraph.  After
  include expansion the Linux chapter's ``== `` title became a continuation of
  that paragraph, so the whole chapter rendered as subsections of the Windows
  chapter and its title vanished.  Asciidoctor reports nothing.
* Three chapters opened with a level-0 ``= `` heading.  Under ``doctype: book``
  that turns each into a *part*, promotes its own sections to chapters and drops
  its title from the numbered sequence.  Also silent.
* Six complete chapters sat in the tree while being included by nothing, so they
  never reached a reader at all.

The first two are caught by rendering the book and checking that every included
chapter's title survives into the output; the third by walking the include graph.
"""
from __future__ import annotations

import argparse
import html
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ASCIIDOC_EXTENSIONS = {".adoc", ".asciidoc"}
INCLUDE_RE = re.compile(r"^include::([^\[]+)\[([^\]]*)\]\s*$")
HEADING_RE = re.compile(r"^(=+) +(\S.*)$")
FENCE_RE = re.compile(r"^(----|\.\.\.\.|````|\*\*\*\*|\+\+\+\+)\s*$")
# Inline AsciiDoc markup that never survives into the rendered heading text.
INLINE_MARKUP_RE = re.compile(r"[`*_#]|\[\[[^\]]*\]\]|\[[^\]]*\]")


def parse_lines(path: Path) -> list[str]:
    return path.read_text(encoding="utf-8").split("\n")


def first_heading(path: Path) -> tuple[int, str] | None:
    """Return (level, title) of the file's first heading outside a fenced block."""
    in_fence = False
    for line in parse_lines(path):
        if FENCE_RE.match(line):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        match = HEADING_RE.match(line)
        if match:
            return len(match.group(1)), match.group(2).strip()
    return None


def normalize(title: str) -> str:
    """Reduce a heading to something comparable across AsciiDoc and HTML."""
    text = html.unescape(title)
    text = INLINE_MARKUP_RE.sub("", text)
    return re.sub(r"\s+", " ", text).strip().lower()


class Walker:
    """Expands the include graph, recording every edge for the adjacency check."""

    def __init__(self, root: Path, guide_dir: Path) -> None:
        self.root = root
        self.guide_dir = guide_dir
        self.reachable: dict[Path, str] = {}
        self.direct: set[Path] = set()
        self.errors: list[str] = []
        self._visit(root, "")

    def _visit(self, path: Path, attrs_from_parent: str) -> None:
        if path in self.reachable:
            return
        self.reachable[path] = attrs_from_parent
        lines = parse_lines(path)
        in_fence = False
        for index, line in enumerate(lines):
            if FENCE_RE.match(line):
                in_fence = not in_fence
                continue
            if in_fence:
                continue
            match = INCLUDE_RE.match(line)
            if not match:
                continue
            target_raw, attrs = match.group(1), match.group(2)
            target = (path.parent / target_raw).resolve()
            if target.suffix not in ASCIIDOC_EXTENSIONS:
                continue  # a snippet include, validated by validate-guide-snippets.py
            if not target.exists():
                self.errors.append(
                    f"{path.name}:{index + 1}: include target does not exist: {target_raw}"
                )
                continue
            if path == self.root:
                self.direct.add(target)
                self._check_manifest_spacing(index, lines, target_raw)
            self._visit(target, attrs)

    def _check_manifest_spacing(self, index: int, lines: list[str], target_raw: str) -> None:
        """Require a blank line after every include in the top-level manifest.

        Two adjacent include lines put the first target's last line next to the
        second target's first line. When the first ends on a paragraph, the second
        chapter's title becomes a continuation of it and the chapter disappears
        without a single warning -- which is exactly how the native Linux chapter
        spent its life rendered as subsections of the Windows one.
        """
        following = lines[index + 1] if index + 1 < len(lines) else ""
        if following.strip():
            self.errors.append(
                f"developer-guide.asciidoc:{index + 1}: include of {target_raw} is not "
                f"followed by a blank line. Two adjacent includes let the first "
                f"chapter's trailing paragraph swallow the second chapter's title."
            )


def render(root: Path) -> str:
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "guide.html"
        result = subprocess.run(
            ["asciidoctor", "--require", "rouge", "-o", str(out), str(root)],
            capture_output=True,
            text=True,
        )
        if result.returncode != 0:
            print(result.stderr, file=sys.stderr)
            raise SystemExit("asciidoctor failed to render the guide")
        return out.read_text(encoding="utf-8")


def rendered_titles(markup: str) -> dict[str, int]:
    """Count every rendered heading, normalized and stripped of its number."""
    body = markup.split('id="content"', 1)[-1]
    counts: dict[str, int] = {}
    for match in re.finditer(r"<h([1-6])[^>]*>(.*?)</h\1>", body, re.S):
        text = re.sub(r"<[^>]+>", "", match.group(2))
        text = re.sub(r"^(Appendix [A-Z]:|(\d+|[A-Z])(\.\d+)*\.)\s*", "", html.unescape(text).strip())
        key = normalize(text)
        counts[key] = counts.get(key, 0) + 1
    return counts


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--guide-dir", default="docs/developer-guide", type=Path)
    args = parser.parse_args()

    guide_dir = args.guide_dir.resolve()
    root = guide_dir / "developer-guide.asciidoc"
    if not root.exists():
        raise SystemExit(f"guide root not found: {root}")

    walker = Walker(root, guide_dir)
    errors = list(walker.errors)

    # 1. Every chapter in the tree is either in the book or declared out of it.
    declared_path = guide_dir / "not-in-book.txt"
    declared = set()
    if declared_path.exists():
        for line in declared_path.read_text(encoding="utf-8").split("\n"):
            line = line.split("#", 1)[0].strip()
            if line:
                declared.add(line)

    on_disk = {
        path.resolve()
        for path in guide_dir.rglob("*")
        if path.suffix in ASCIIDOC_EXTENSIONS and path.is_file()
    }
    unreachable = sorted(on_disk - set(walker.reachable))
    for path in unreachable:
        name = path.relative_to(guide_dir).as_posix()
        if name not in declared:
            errors.append(
                f"{name}: present in the guide directory but included by nothing, so it "
                f"never reaches a reader. Include it, delete it, or list it in "
                f"not-in-book.txt with a reason."
            )
    reachable_names = {p.relative_to(guide_dir).as_posix() for p in walker.reachable}
    for name in sorted(declared & reachable_names):
        errors.append(f"not-in-book.txt lists {name}, but it is included. Remove the entry.")

    # 2. A chapter's own heading level decides whether it is a chapter at all.
    #    A level-0 heading turns it into a book PART; a level-3 heading (or none)
    #    makes it a subsection of whatever chapter precedes it. Both nest silently,
    #    and the rendered-title check below cannot see either, because it accepts a
    #    title at any depth. Only entries the manifest includes DIRECTLY are held
    #    to this: nested fragments such as the appendix_goal_* files legitimately
    #    start at level 3 under their parent.
    for path, attrs in sorted(walker.reachable.items()):
        if path == root:
            continue
        heading = first_heading(path)
        if heading and heading[0] == 1 and "leveloffset" not in attrs:
            errors.append(
                f"{path.name}: opens with a level-0 '= {heading[1]}' heading. Under "
                f"doctype:book that renders as a PART and promotes its own sections to "
                f"chapters. Use '== ' or include it with leveloffset=+1."
            )
        if path not in walker.direct:
            continue
        if heading is None:
            errors.append(
                f"{path.name}: is included directly by developer-guide.asciidoc but has "
                f"no heading, so its content is absorbed into the chapter before it."
            )
            continue
        # A leveloffset shifts every heading in the included file, so what decides
        # whether this renders as a chapter is the declared level PLUS the offset.
        # Exempting offset includes entirely would leave the same nesting bug one
        # step further along: '== Chapter' at leveloffset=+1 renders as a
        # subsection, and the outcome check cannot see it because it accepts a
        # title at any depth.
        offset = 0
        match = re.search(r"leveloffset=([+-]?\d+)", attrs)
        if match:
            offset = int(match.group(1))
        effective = heading[0] + offset
        if effective != 2:
            detail = (
                f"level {heading[0]} with leveloffset={offset:+d}"
                if offset
                else f"level {heading[0]}"
            )
            errors.append(
                f"{path.name}: is included directly by developer-guide.asciidoc at "
                f"{detail}, so it renders at level {effective} rather than as a chapter. "
                f"A direct manifest entry must come out at level 2."
            )

    # 3. Outcome check: every included chapter's title survives into the book.
    rendered = rendered_titles(render(root))
    expected: dict[str, list[str]] = {}
    for path in sorted(walker.reachable):
        if path == root:
            continue
        heading = first_heading(path)
        if heading:
            expected.setdefault(normalize(heading[1]), []).append(path.name)
    for title, sources in sorted(expected.items()):
        # Compare counts, not membership: a chapter sharing its title with another
        # heading would otherwise hide the fact that it was swallowed.
        if rendered.get(title, 0) < len(sources):
            errors.append(
                f"{', '.join(sources)}: the title '{title}' appears "
                f"{rendered.get(title, 0)} time(s) in the rendered book but "
                f"{len(sources)} document(s) declare it. A chapter was swallowed by "
                f"whatever precedes it."
            )

    if errors:
        for error in errors:
            print(f"::error::{error}" if sys.stdout.isatty() is False else error, file=sys.stderr)
        print(f"\n{len(errors)} guide structure problem(s).", file=sys.stderr)
        return 1
    print(
        f"Guide structure OK: {len(walker.reachable) - 1} included documents, "
        f"{len(declared)} declared out of book."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
