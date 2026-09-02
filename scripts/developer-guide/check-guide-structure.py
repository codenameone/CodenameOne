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
INLINE_CONDITIONAL_INCLUDE_RE = re.compile(r"^(ifdef|ifndef|ifeval)::[^\[]*\[.*include::")
HEADING_RE = re.compile(r"^(=+) +(\S.*)$")
# Literal-content blocks only. The container delimiters -- ==== example,
# **** sidebar, ____ quote -- hold ordinary AsciiDoc, so an include or a heading
# inside one is real and must not be skipped.
# Literal and comment blocks -- an example (====) or sidebar (****) block contains
# live markup, so its headings and includes are real and must keep counting. A
# delimiter may be LONGER than four characters, and closes on one of the same
# character AND length, so a four-dash line inside a five-dash block is content
# rather than the close. Matching exactly four left every ----- block's contents
# live: the guide has 20 such lines, all [source] blocks wrapping an include::.
#
# //// is here because asciidoctor drops a comment block entirely, so an include
# inside one does not put its target in the book and prose inside one promises
# the reader nothing. Note check-guide-links.py deliberately does NOT skip
# comments: a dead link commented out is still dead debt in the source, and
# letting the ratchet shrink for it would turn "comment the line out" into a way
# to silence that gate. Reachability is the opposite case -- treating a
# commented-out include as reachable states something about the book that is
# simply untrue.
FENCE_RE = re.compile(r"^(-{4,}|\.{4,}|`{4,}|\+{4,}|/{4,})\s*$")
# Directives are a different question from content, and asciidoctor answers them
# differently. Measured:
#
#                       inside ---- / ----- / ....      inside ////
#   include::           PROCESSED                       dropped
#   ifdef:: / ifeval::  ACTIVE                           dropped
#   heading / prose     literal text                    dropped
#
# A literal block hides CONTENT but not DIRECTIVES: include:: and ifdef:: are
# resolved before block parsing and fire from inside a listing exactly as they
# would outside it. Only a comment block removes them. So first_heading() keeps
# FENCE_RE -- a heading in a listing really is only text -- while the include walk
# and the conditional test use this narrower one.
COMMENT_FENCE_RE = re.compile(r"^(/{4,})\s*$")
# Only the BLOCK forms vanish. ifdef/ifndef with empty brackets open a region and
# leave nothing behind, and endif closes one; but the single-line form carries its
# content in the brackets and EXPANDS to it -- measured, `ifdef::feature[== Inline
# Chapter]` renders as a real chapter when the attribute is set. So the spacing
# scan may step over the first kind and must treat the second as content, or it
# would walk past a heading that the preceding paragraph is about to swallow.
BLOCK_DIRECTIVE_RE = re.compile(r"^(ifdef|ifndef)::[^\[]*\[\s*\]\s*$|^ifeval::\[|^endif::")
# Inline AsciiDoc markup that never survives into the rendered heading text.
INLINE_MARKUP_RE = re.compile(r"[`*_#]|\[\[[^\]]*\]\]|\[[^\]]*\]")


def parse_lines(path: Path) -> list[str]:
    return path.read_text(encoding="utf-8").split("\n")


def heading_is_conditional(path: Path) -> bool:
    """Whether the file's first heading sits inside a backend conditional.

    A chapter whose own title is conditional has no title in the branch that
    excludes it, and its body merges into whatever precedes it there. The outcome
    check cannot see that from one render: first_heading() reads the source, so
    the title is found, and the HTML render contains it. Rather than render the
    book a second time for a construct the guide does not use -- measured, zero
    headings sit inside a backend conditional -- refuse the construct.
    """
    open_fence: str | None = None
    depth = 0
    for line in parse_lines(path):
        fence = COMMENT_FENCE_RE.match(line)
        if fence:
            token = fence.group(1)
            open_fence = token if open_fence is None else (None if token == open_fence else open_fence)
            continue
        if open_fence is not None:
            continue
        if BLOCK_DIRECTIVE_RE.match(line.strip()):
            depth += 1 if not line.strip().startswith("endif::") else -1
            depth = max(0, depth)
            continue
        if HEADING_RE.match(line):
            return depth > 0
    return False


def first_heading(path: Path) -> tuple[int, str] | None:
    """Return (level, title) of the file's first heading outside a fenced block."""
    open_fence: str | None = None
    for line in parse_lines(path):
        fence = FENCE_RE.match(line)
        if fence:
            token = fence.group(1)
            if open_fence is None:
                open_fence = token
            elif token == open_fence:
                open_fence = None
            continue
        if open_fence is not None:
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
        # Keyed by (parent, target) rather than by target alone. A fragment reused
        # from two different parents is deliberate -- that is what a fragment is
        # for -- but the SAME parent including the SAME file twice renders it
        # twice, at the root or nested. Keying only by target missed the nested
        # case entirely, and _visit() returns early on a revisit, so the second
        # edge left no trace at all: the outcome check then saw one declaration
        # and two rendered titles and passed, because it only asks whether a
        # title appears AT LEAST as often as it is declared.
        self.include_edges: collections.Counter = collections.Counter()
        self.errors: list[str] = []
        self._visit(root, "")

    def _visit(self, path: Path, attrs_from_parent: str) -> None:
        if path in self.reachable:
            return
        self.reachable[path] = attrs_from_parent
        lines = parse_lines(path)
        open_fence: str | None = None
        for index, line in enumerate(lines):
            fence = COMMENT_FENCE_RE.match(line)
            if fence:
                token = fence.group(1)
                if open_fence is None:
                    open_fence = token
                elif token == open_fence:
                    open_fence = None
                continue
            if open_fence is not None:
                continue
            if INLINE_CONDITIONAL_INCLUDE_RE.match(line):
                # asciidoctor expands the bracket content and processes the include
                # inside it; INCLUDE_RE is anchored, so the line looked like nothing
                # at all and the edge went uncounted. Refused for the same reason as
                # any other conditional include: neither the duplicate count nor the
                # rendered-title check can model one.
                self.errors.append(
                    f"{path.name}:{index + 1}: an include inside an inline conditional. "
                    f"asciidoctor expands and processes it, but neither the duplicate "
                    f"check nor the rendered-title check can model a conditional "
                    f"include, so this refuses it rather than skipping it silently."
                )
                continue
            match = INCLUDE_RE.match(line)
            if not match:
                continue
            target_raw, attrs = match.group(1), match.group(2)
            if "{" in target_raw:
                # An attribute-built target resolves to a literal "{name}" here, which
                # has no .adoc suffix and was therefore filed as a snippet and ignored.
                # asciidoctor expands it and includes the chapter, so a second copy
                # rendered with no edge counted and no extra title expected. Expanding
                # attributes means reimplementing asciidoctor's resolution, inheritance
                # through includes included; the guide uses none, so refuse instead.
                self.errors.append(
                    f"{path.name}:{index + 1}: include target {target_raw} is built from "
                    f"an attribute. This does not expand attributes, so the target "
                    f"cannot be identified or counted; write the path literally."
                )
                continue
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
            # Conditional includes are refused at EVERY depth, not just in the
            # manifest. Nothing in the guide is conditional today (measured: zero
            # include:: lines sit inside a conditional anywhere), and neither
            # check that matters can validate one. The edge count has to skip it,
            # because the same file under two exclusive branches is one rendering;
            # the rendered-title check has to skip it too, for the same reason.
            # Skipping BOTH silently means a file included twice inside a single
            # ACTIVE branch passes -- _visit() deduplicates the second target and
            # the title check only requires the title once. Refusing the construct
            # is the honest answer while nothing uses it; an earlier version
            # refused it only under path == self.root and left exactly that hole
            # one level down.
            if self._inside_conditional(lines, index):
                self.conditional.add(target)
                self.errors.append(
                    f"{path.name}:{index + 1}: {target_raw} is included inside a "
                    f"conditional. Neither the duplicate check nor the rendered-title "
                    f"check can validate a conditional include, so this refuses it "
                    f"rather than skipping it silently. Teach the checker which "
                    f"branches are mutually exclusive before adding one."
                )
            else:
                self.include_edges[(path, target)] += 1
            self._visit(target, attrs)

    @staticmethod
    def _inside_conditional(lines: list[str], index: int) -> bool:
        """Whether this line sits inside an ifdef/ifndef/ifeval region.

        A chapter included once per branch of a conditional appears twice in the
        source and once in the output, so counting it as a duplicate would reject
        valid markup.
        """
        depth = 0
        open_fence: str | None = None
        for line in lines[:index]:
            # Only a comment block, for the reason given at COMMENT_FENCE_RE: a
            # conditional written inside a listing is not "displayed", it is
            # active, so treating the listing as a hiding place would make this
            # disagree with the renderer.
            fence = COMMENT_FENCE_RE.match(line)
            if fence:
                token = fence.group(1)
                if open_fence is None:
                    open_fence = token
                elif token == open_fence:
                    open_fence = None
                continue
            if open_fence is not None:
                continue
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
        # Look PAST preprocessor directives rather than treating one as a
        # separator. Asciidoctor removes ifdef/ifndef/ifeval/endif during
        # preprocessing, so they leave nothing behind to close the paragraph --
        # measured: a heading with `ifdef::backend-html5[]` between it and the
        # preceding paragraph is swallowed exactly as if the directive were not
        # there. An earlier version of this rule returned here and would have
        # passed that.
        cursor = index + 1
        while cursor < len(lines) and BLOCK_DIRECTIVE_RE.match(lines[cursor].strip()):
            cursor += 1
        following = lines[cursor] if cursor < len(lines) else ""
        if not following.strip():
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


def occurrence_counts(root: Path, edges: collections.Counter) -> dict[Path, int]:
    """How many times each document renders, following multiplicity down the graph.

    A file included twice renders twice, and so does every file IT includes. The
    include graph is a DAG -- asciidoctor rejects a cycle -- so each node's count
    is the sum over its incoming edges of the parent's count times the edge's
    multiplicity, with the root rendering once.
    """
    incoming: dict[Path, list[tuple[Path, int]]] = collections.defaultdict(list)
    for (parent, child), count in edges.items():
        incoming[child].append((parent, count))

    counts: dict[Path, int] = {}
    visiting: set[Path] = set()

    def count_for(node: Path) -> int:
        if node == root:
            return 1
        if node in counts:
            return counts[node]
        if node in visiting:
            return 1  # a cycle asciidoctor would reject; do not spin on it
        visiting.add(node)
        total = sum(count_for(parent) * n for parent, n in incoming[node]) or 1
        visiting.discard(node)
        counts[node] = total
        return total

    for child in incoming:
        count_for(child)
    return counts


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
    # A chapter reached from the manifest AND from some nested document renders
    # twice, but each (parent, target) pair counts one, so the duplicate rule sees
    # nothing and the title check only asks for "at least once". Cross-parent reuse
    # stays legitimate for a FRAGMENT; a direct chapter is a different thing --
    # it already has its place in the book.
    for target in sorted(walker.direct, key=lambda path: path.name):
        others = sorted(
            parent.name
            for (parent, other) in walker.include_edges
            if other == target and parent != walker.root
        )
        if others:
            errors.append(
                f"{target.name}: is a chapter in the manifest and is also included by "
                f"{', '.join(others)}, so the book renders it twice. Include it in one "
                f"place."
            )

    for (parent, target), count in sorted(
        walker.include_edges.items(), key=lambda item: (item[0][0].name, item[0][1].name)
    ):
        if count > 1:
            errors.append(
                f"{parent.name}: includes {target.name} {count} times, so the book "
                f"renders it {count} times. Remove the duplicate include."
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

    render_counts = occurrence_counts(root, walker.include_edges)

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
        if heading_is_conditional(path):
            errors.append(
                f"{path.name}: its own title sits inside a conditional. In the branch "
                f"that excludes it the chapter has no heading and its body merges into "
                f"whatever precedes it, which one render cannot show. Put the title "
                f"outside the conditional."
            )
        heading = first_heading(path)
        if not heading:
            continue
        key = normalize(heading[1])
        # A direct entry must appear at CHAPTER level. A nested fragment sits at
        # whatever depth its parent puts it, so it is only counted at all.
        #
        # How many times this file RENDERS, which is not how many edges point at
        # it. Multiplicity multiplies down the graph: a fragment two parents both
        # include renders twice, and so does everything it includes in turn --
        # counting the single edge recorded before _visit() returned early on the
        # second traversal expected one title for two renderings, and a swallowed
        # one hid behind the survivor.
        occurrences = render_counts.get(path, 1)
        target = expected_chapters if path in walker.direct else expected
        for _ in range(occurrences):
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
