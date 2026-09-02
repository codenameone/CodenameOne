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
import re
import sys
from pathlib import Path

ASCIIDOC_EXTENSIONS = {".adoc", ".asciidoc"}
FENCE_RE = re.compile(r"^(----|\.\.\.\.|````|\*\*\*\*|\+\+\+\+|====|____)\s*$")
# Lines that end in a colon without promising a listing: headings, attributes,
# comments, block titles, list markers, table cells and block delimiters.
NON_PROSE_PREFIX = ("//", "|", "=", ".", ":", "*", "-", "+", "[", "<")


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
        if stripped.lstrip().startswith(NON_PROSE_PREFIX):
            continue
        if index + 2 >= len(lines):
            continue
        if lines[index + 1].strip() or lines[index + 2].strip():
            continue
        findings.append((index + 1, normalize(stripped)))
    return findings


def load_baseline(path: Path) -> dict[str, set[str]]:
    baseline: dict[str, set[str]] = {}
    if not path.exists():
        return baseline
    for raw in path.read_text(encoding="utf-8").split("\n"):
        line = raw.split("#", 1)[0].rstrip() if raw.startswith("#") else raw.rstrip()
        if not line.strip():
            continue
        name, _, text = line.partition("\t")
        if text:
            baseline.setdefault(name, set()).add(text)
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
        help="rewrite the baseline from the current tree (only ever to shrink it)",
    )
    args = parser.parse_args()

    guide_dir = args.guide_dir.resolve()
    current: dict[str, set[str]] = {}
    located: dict[tuple[str, str], int] = {}
    for path in sorted(guide_dir.rglob("*")):
        if path.suffix not in ASCIIDOC_EXTENSIONS or not path.is_file():
            continue
        name = path.relative_to(guide_dir).as_posix()
        for number, text in scan(path):
            current.setdefault(name, set()).add(text)
            located[(name, text)] = number

    if args.write_baseline:
        lines = [
            "# Prose that promises a code block where none follows.",
            "# A ratchet: entries may be removed as holes are filled, never added.",
            "# Regenerate with check-missing-code-blocks.py --write-baseline.",
        ]
        for name in sorted(current):
            for text in sorted(current[name]):
                lines.append(f"{name}\t{text}")
        args.baseline.write_text("\n".join(lines) + "\n", encoding="utf-8")
        total = sum(len(v) for v in current.values())
        print(f"Wrote baseline with {total} entr(ies) across {len(current)} file(s).")
        return 0

    baseline = load_baseline(args.baseline)
    new: list[str] = []
    for name in sorted(current):
        for text in sorted(current[name] - baseline.get(name, set())):
            number = located[(name, text)]
            new.append(f"{name}:{number}: promises a code block that is not there: {text[:100]}")

    # A filled hole has to leave the baseline. Leaving it keeps a slot open: a
    # later change can empty that exact block again and `current - baseline` stays
    # empty, so the regression passes. The ratchet only ratchets if fixes are banked.
    stale: list[str] = []
    for name in sorted(baseline):
        for text in sorted(baseline[name] - current.get(name, set())):
            stale.append(
                f"{name}: filled, but still in the baseline: {text[:80]}. Run "
                f"check-missing-code-blocks.py --write-baseline to bank the fix."
            )

    total = sum(len(v) for v in current.values())
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
