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
import collections
import html
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ASCIIDOC_EXTENSIONS = {".adoc", ".asciidoc"}
INCLUDE_RE = re.compile(r"^include::([^\[]+)\[([^\]]*)\]\s*$")
HEADING_RE = re.compile(r"^(=+) +(\S.*)$")
# Literal-content blocks only. The container delimiters -- ==== example,
# **** sidebar, ____ quote -- hold ordinary AsciiDoc, so an include or a heading
# inside one is real and must not be skipped.
FENCE_RE = re.compile(r"^(----|\.\.\.\.|````|\+\+\+\+)\s*$")
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
        # Direct entries that sit inside an ifdef/ifndef region. Only one branch
        # renders, so the outcome check cannot demand every branch's title.
        self.conditional: set[Path] = set()
        # Counted at the EDGE, not per visited file: _visit returns early on a
        # revisit, so a document included twice would otherwise leave no trace.
        # Only direct manifest entries are counted. A nested fragment may be
        # reused from two parents on purpose, and a file included under
        # mutually exclusive ifdef/ifndef branches appears twice in the source
        # while rendering once -- neither is a duplicated chapter.
        self.direct_include_count: collections.Counter = collections.Counter()
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
            self._check_include_spacing(path, index, lines, target_raw, attrs, target)
            if path == self.root:
                # A conditional entry is still a chapter: it must open at chapter
                # level and be spaced correctly. Only the DUPLICATE count skips
                # it, because the same chapter under two exclusive branches
                # appears twice in the source and once in the output.
                self.direct.add(target)
                if self._inside_conditional(lines, index):
                    # Nothing in the manifest is conditional today (measured: zero
                    # include:: lines sit inside a conditional anywhere in the guide),
                    # and the two checks that matter cannot see one. The duplicate
                    # count has to skip it, because the same chapter under two
                    # exclusive branches is one chapter in the output; the
                    # rendered-title check has to skip it too, because only one
                    # branch renders. Skipping BOTH silently means a chapter
                    # included twice inside a single active branch would pass.
                    # So refuse the construct instead of quietly not validating it.
                    self.conditional.add(target)
                    self.errors.append(
                        f"developer-guide.asciidoc:{index + 1}: {target_raw} is included "
                        f"inside a conditional. Neither the duplicate check nor the "
                        f"rendered-title check can validate a conditional entry, so this "
                        f"refuses it rather than skipping it silently. Teach the checker "
                        f"which branches are mutually exclusive before adding one."
                    )
                else:
                    self.direct_include_count[target] += 1
            self._visit(target, attrs)

    @staticmethod
    def _inside_conditional(lines: list[str], index: int) -> bool:
        """Whether this line sits inside an ifdef/ifndef/ifeval region.

        A chapter included once per branch of a conditional appears twice in the
        source and once in the output, so counting it as a duplicate would reject
        valid markup.
        """
        depth = 0
        for line in lines[:index]:
            # ifdef/ifndef open a block only with EMPTY brackets -- with content
            # they are the single-line form and guard just that line. ifeval has
            # no single-line form and always carries its expression in the
            # brackets, so requiring them empty meant this could never match one.
            if re.match(r"^(ifdef|ifndef)::[^\[]*\[\s*\]\s*$", line) or re.match(
                r"^ifeval::\[", line
            ):
                depth += 1
            elif re.match(r"^endif::", line):
                depth = max(0, depth - 1)
        return depth > 0

    # A delimited block, a heading, an attribute entry or a directive all close the
    # paragraph context. Only ordinary paragraph text leaves it open, and only an
    # open paragraph can absorb the heading that follows it.
    _CLOSES_PARAGRAPH = re.compile(
        r"^(=+\s|:[^:]+:|//|\[|\||([-=_.*+/])\2{3,}\s*$)"
    )

    def _check_include_spacing(
        self,
        path: Path,
        index: int,
        lines: list[str],
        target_raw: str,
        attrs: str,
        target: Path,
    ) -> None:
        """Reject an include whose target can swallow whatever follows it.

        This is the defect the whole checker was written for: two adjacent
        include:: lines put the first file's last line against the second file's
        first line, and if the first ends mid-paragraph the second's title becomes
        a continuation of it. Asciidoctor emits no warning, and the native Linux
        chapter spent its life rendered as subsections of the Windows one.

        Measured with a minimal reproduction rather than assumed, because three of
        the four ways out are not obvious:

        * a blank line in the parent separates them -- safe;
        * leveloffset= wraps the include in :leveloffset: attribute entries, and
          those lines close the paragraph -- safe, which is why the five adjacent
          includes in Maven-Project-Workflow.asciidoc render correctly;
        * the included file ending on a blank line -- safe;
        * the included file ending on a delimiter, table row, heading, attribute or
          comment -- safe, which is why _generated-build-hints.adoc ending on
          "|===" does not eat the "Versioned builds" heading after it.

        What is left -- an adjacent include, no leveloffset, whose file ends on
        ordinary paragraph text -- is the one shape that silently deletes content.
        """
        following = lines[index + 1] if index + 1 < len(lines) else ""
        if not following.strip():
            return
        # A preprocessor directive is not content and cannot absorb a paragraph.
        if re.match(r"^(ifdef|ifndef|ifeval|endif)::", following.strip()):
            return
        # A leveloffset on EITHER include protects: asciidoctor brackets the
        # included content with :leveloffset: attribute entries, and an attribute
        # entry closes the paragraph. The one on the following include lands
        # between the paragraph and the heading, so it works just as well as the
        # one on this include. Measured both ways.
        if "leveloffset" in attrs:
            return
        following_include = INCLUDE_RE.match(following.strip())
        if following_include and "leveloffset" in following_include.group(2):
            return
        try:
            text = target.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            return
        if text.endswith("\n\n") or not text.strip():
            return
        last = next(
            (line for line in reversed(text.split("\n")) if line.strip()), ""
        )
        if self._CLOSES_PARAGRAPH.match(last.strip()):
            return
        self.errors.append(
            f"{path.name}:{index + 1}: include of {target_raw} is followed immediately "
            f"by content, and {target.name} ends on a paragraph. That paragraph will "
            f"absorb whatever comes next, deleting it from the book without a warning. "
            f"Add a blank line after the include, or a leveloffset attribute, or end "
            f"{target.name} with a blank line."
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


def rendered_titles(markup: str) -> tuple[dict[str, int], dict[str, int]]:
    """Count rendered headings: all levels, and chapter level (h2) separately.

    Counting every level lets an unrelated subsection stand in for a chapter that
    was swallowed -- "Analytics" is a chapter and also a subsection of Commerce,
    and "Getting started" collides the same way. A chapter renders as h2, so
    holding manifest entries to that count removes the substitution.
    """
    body = markup.split('id="content"', 1)[-1]
    counts: dict[str, int] = {}
    chapters: dict[str, int] = {}
    for match in re.finditer(r"<h([1-6])[^>]*>(.*?)</h\1>", body, re.S):
        text = re.sub(r"<[^>]+>", "", match.group(2))
        text = re.sub(r"^(Appendix [A-Z]:|(\d+|[A-Z])(\.\d+)*\.)\s*", "", html.unescape(text).strip())
        key = normalize(text)
        counts[key] = counts.get(key, 0) + 1
        if match.group(1) == "2":
            chapters[key] = chapters.get(key, 0) + 1
    return counts, chapters


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

    # A chapter included twice by the manifest is rendered twice. The title check
    # below cannot see it, because it only asks whether a title appears AT LEAST
    # as often as it is declared.
    for path, count in sorted(walker.direct_include_count.items()):
        if count > 1:
            errors.append(
                f"{path.name}: is included {count} times, so the book renders it "
                f"{count} times. Remove the duplicate include."
            )

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
    rendered, rendered_chapters = rendered_titles(render(root))
    expected: dict[str, list[str]] = {}
    expected_chapters: dict[str, list[str]] = {}
    for path in sorted(walker.reachable):
        if path == root or path in walker.conditional:
            # A conditional entry renders in one branch only, so requiring its
            # title in this render would report a chapter that is deliberately
            # absent. Its level and spacing are still checked above.
            continue
        heading = first_heading(path)
        if not heading:
            continue
        key = normalize(heading[1])
        # A direct entry must appear at CHAPTER level. A nested fragment sits at
        # whatever depth its parent puts it, so it is only counted at all.
        target = expected_chapters if path in walker.direct else expected
        target.setdefault(key, []).append(path.name)

    for title, sources in sorted(expected_chapters.items()):
        found = rendered_chapters.get(title, 0)
        if found < len(sources):
            errors.append(
                f"{', '.join(sources)}: the title '{title}' appears {found} time(s) as a "
                f"chapter in the rendered book but {len(sources)} manifest entr(ies) "
                f"declare it. A chapter was swallowed by whatever precedes it."
            )
    for title, sources in sorted(expected.items()):
        if rendered.get(title, 0) < len(sources):
            errors.append(
                f"{', '.join(sources)}: the title '{title}' appears "
                f"{rendered.get(title, 0)} time(s) in the rendered book but "
                f"{len(sources)} document(s) declare it. A fragment was swallowed by "
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
