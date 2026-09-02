#!/usr/bin/env python3
"""Find prose that promises a code block where no block follows.

Commit bbdc6058f0 ("Extract developer guide snippets into demos") converted
inline listings into ``include::`` directives and dropped roughly 415 of them on
the floor, leaving the introducing sentence, its colon, and two blank lines where
the code used to be. The Commerce chapter documents a paid service and, after
that pass, contained no code at all.

``validate-guide-snippets.py`` cannot see this: it validates the blocks that are
present, not the ones that should be. The signature here is the hole itself --
a sentence ending in a colon followed by two blank lines, which is what a removed
block leaves behind (a real block is separated from its introduction by exactly
one blank line).

The baseline is a ratchet: it may shrink, never grow. Entries are keyed by the
text of the promising sentence rather than by line number, so ordinary editing
elsewhere in a chapter does not churn the file.
"""
from __future__ import annotations

import argparse
import collections
import re
import sys
from pathlib import Path

ASCIIDOC_EXTENSIONS = {".adoc", ".asciidoc"}
# Only blocks whose CONTENT is literal: listing, literal, fenced code and
# passthrough. The container delimiters -- ==== example, **** sidebar, ____ quote
# -- hold ordinary prose, and skipping them hid six real holes in basics.asciidoc
# alone, among them the setSameWidth example this check was written to find.
#
# Markdown's three-backtick fence is deliberately absent. The guide contains none,
# and validate-guide-snippets.py requires every listing to be [source,LANG] with a
# bare include:: inside ---- delimiters, so a three-backtick block would fail that
# gate before reaching this one. Add it here if that convention ever changes.
FENCE_RE = re.compile(r"^(----|\.\.\.\.|````|\+\+\+\+)\s*$")
# Lines that end in a colon without promising a listing: headings, attributes,
# comments, block titles, list markers, table cells and block delimiters.
NON_PROSE_PREFIX = ("//", "|", "=", ".", ":", "*", "-", "+", "[", "<")
# A list marker is followed by whitespace; a block title (.Title) and bold text
# (*text*) are not. Stripping the marker lets the prose test see the sentence,
# so an introduction written as a list item is not mistaken for markup.
LIST_MARKER_RE = re.compile(r"^([*\-]+|\.{1,5}|[0-9]+\.)\s+")
# What a real block looks like when it starts. An introduction separated from its
# listing by more than one blank line is untidy, not a hole, and reporting it
# would make the gate reject valid AsciiDoc spacing. Deliberately conservative:
# only unambiguous starts, so a genuine hole is never explained away.
# An admonition is prose, so it can never be the listing a sentence promised.
# Excluded by name rather than by whitelisting the block kinds that ARE code:
# measured, the bracket lines that legitimately answer a promising sentence
# already span [source], [listing], [cols=...], [options=...], [quote] and an
# anchored image, and a whitelist would report the next kind nobody thought of.
ADMONITION = "NOTE|TIP|IMPORTANT|WARNING|CAUTION"
BLOCK_START_RE = re.compile(
    r'^(\[(?!(?:' + ADMONITION + r')\])[a-zA-Z%.#"]'
    r'|image::|include::|\|===|(----|\.\.\.\.|````|\+\+\+\+|====|\*\*\*\*|____)\s*$)'
)


def normalize(line: str) -> str:
    return re.sub(r"\s+", " ", line.strip())


def scan(path: Path) -> list[tuple[int, str]]:
    lines = path.read_text(encoding="utf-8").split("\n")
    in_fence = False
    findings = []
    for index, line in enumerate(lines):
        if FENCE_RE.match(line):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        stripped = line.rstrip()
        if not stripped.endswith(":"):
            continue
        body = stripped.lstrip()
        marker = LIST_MARKER_RE.match(body)
        if marker:
            body = body[marker.end():]
        if not body or body.startswith(NON_PROSE_PREFIX):
            continue
        if index + 2 >= len(lines):
            continue
        if lines[index + 1].strip() or lines[index + 2].strip():
            continue
        following = index + 1
        while following < len(lines) and not lines[following].strip():
            following += 1
        if following < len(lines) and BLOCK_START_RE.match(lines[following].strip()):
            continue
        findings.append((index + 1, normalize(stripped)))
    return findings


def load_baseline(path: Path) -> collections.Counter:
    """Read the baseline as a multiset: one line per occurrence.

    Two identical introducing sentences in one chapter are two holes to fill, and
    keying by text alone would let the second appear for free.
    """
    baseline: collections.Counter = collections.Counter()
    if not path.exists():
        return baseline
    for raw in path.read_text(encoding="utf-8").split("\n"):
        if raw.startswith("#") or not raw.strip():
            continue
        name, _, text = raw.rstrip("\n").partition("\t")
        if text:
            baseline[f"{name}\t{text}"] += 1
    return baseline


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--guide-dir", default="docs/developer-guide", type=Path)
    parser.add_argument(
        "--baseline",
        default="scripts/developer-guide/missing-code-blocks-baseline.txt",
        type=Path,
    )
    parser.add_argument(
        "--write-baseline",
        action="store_true",
        help="rewrite the baseline from the current tree",
    )
    parser.add_argument(
        "--allow-new",
        action="store_true",
        help="permit --write-baseline to ADD entries; without it the baseline may only shrink",
    )
    args = parser.parse_args()

    guide_dir = args.guide_dir.resolve()
    current: collections.Counter = collections.Counter()
    located: dict[str, list[int]] = {}
    for path in sorted(guide_dir.rglob("*")):
        if path.suffix not in ASCIIDOC_EXTENSIONS or not path.is_file():
            continue
        name = path.relative_to(guide_dir).as_posix()
        for number, text in scan(path):
            entry = f"{name}\t{text}"
            current[entry] += 1
            located.setdefault(entry, []).append(number)

    if args.write_baseline:
        # Same reasoning as check-guide-links.py: the command that banks a fix
        # must not silently bury a new hole.
        added = sorted((current - load_baseline(args.baseline)).elements())
        if added and not args.allow_new:
            for entry in added:
                print(entry.replace("\t", ": "), file=sys.stderr)
            print(
                f"\nRefusing to add {len(added)} entr(ies) to the baseline. Restore the "
                f"block, or pass --allow-new if this is debt you mean to record.",
                file=sys.stderr,
            )
            return 1
        lines = [
            "# Prose that promises a code block where none follows.",
            "# A ratchet: entries may be removed as holes are filled, never added.",
            "# Regenerate with check-missing-code-blocks.py --write-baseline.",
        ]
        lines.extend(sorted(current.elements()))
        args.baseline.write_text("\n".join(lines) + "\n", encoding="utf-8")
        total = sum(current.values())
        print(f"Wrote baseline with {total} entr(ies).")
        return 0

    baseline = load_baseline(args.baseline)
    new: list[str] = []
    for entry in sorted((current - baseline).elements()):
        name, _, text = entry.partition("\t")
        where = ", ".join(str(n) for n in located.get(entry, []))
        new.append(f"{name}:{where}: promises a code block that is not there: {text[:100]}")

    # A filled hole has to leave the baseline. Leaving it keeps a slot open: a
    # later change can empty that exact block again and `current - baseline` stays
    # empty, so the regression passes. The ratchet only ratchets if fixes are banked.
    stale: list[str] = []
    for entry in sorted((baseline - current).elements()):
        name, _, text = entry.partition("\t")
        stale.append(
            f"{name}: filled, but still in the baseline: {text[:80]}. Run "
            f"check-missing-code-blocks.py --write-baseline to bank the fix."
        )

    total = sum(current.values())
    if new or stale:
        for entry in new + stale:
            print(entry, file=sys.stderr)
        print(
            f"\n{len(new)} new hole(s) and {len(stale)} stale baseline entr(ies). Restore "
            f"the block (the originals are recoverable from bbdc6058f0~1) or rewrite the "
            f"sentence so it stops promising one.",
            file=sys.stderr,
        )
        return 1

    print(f"Missing code blocks: {total} known hole(s), none new, none stale.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
