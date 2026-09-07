#!/usr/bin/env python3
"""Compare generated guide figures against the committed ones.

Byte equality is the default and is what nearly every figure achieves: with the
font loaded from the port's bundled Roboto and MigLayout's platform pinned, 23 of
the 24 render identically on macOS and on the Linux runner.

The exception is a figure containing a `FontImage` material glyph. Measured
against the runner's own output, the glyph lands at exactly the same size and
position -- a 55x49 bounding box at the same origin -- and differs only in
antialiased edge coverage, 946 fully-white pixels against 916. That is Java2D
rasterizing the same glyph from the same font at the same size slightly
differently on the two platforms, and no amount of pinning on our side changes
it. Demanding byte equality there would mean either deleting legitimate content
from the figure or carrying a permanently red check.

So a figure may carry a `<name>.tolerance` sidecar, in the same key=value shape
the CN1SS screenshot suites already use, and only then is a bounded difference
accepted. Everything without a sidecar must still match exactly.

The two bounds are applied independently, which is where this differs from the
CN1SS comparator: `maxMismatchPercent` limits how much of the image may change at
all, and `maxChannelDelta` caps how far any single pixel may move. Counting only
the pixels that exceed the delta would let an unlimited number of sub-threshold
changes through.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:  # pragma: no cover - the workflow installs Pillow
    print("Pillow is required to compare screenshots", file=sys.stderr)
    raise


def read_tolerance(path: Path) -> tuple[int, float] | None:
    if not path.exists():
        return None
    max_delta, max_percent = 0, 0.0
    for line in path.read_text(encoding="utf-8").split("\n"):
        line = line.split("#", 1)[0].strip()
        if not line or "=" not in line:
            continue
        key, _, value = line.partition("=")
        if key.strip() == "maxChannelDelta":
            max_delta = int(value)
        elif key.strip() == "maxMismatchPercent":
            max_percent = float(value)
    return max_delta, max_percent


def compare(generated: Path, committed: Path, tolerance: tuple[int, float] | None) -> str | None:
    """Return None when the pair is acceptable, else a description of the failure."""
    if generated.read_bytes() == committed.read_bytes():
        return None
    if tolerance is None:
        return "differs and has no tolerance sidecar"
    max_delta, max_percent = tolerance
    # RGBA, not RGB: these figures are saved with an alpha channel, and dropping
    # it would make a change that touches only transparency invisible here --
    # including one that turned the whole figure see-through while leaving every
    # colour channel intact.
    a = Image.open(generated).convert("RGBA")
    b = Image.open(committed).convert("RGBA")
    if a.size != b.size:
        return f"size changed: generated {a.size[0]}x{a.size[1]}, committed {b.size[0]}x{b.size[1]}"
    pa, pb = a.load(), b.load()
    width, height = a.size
    changed = 0
    worst = 0
    for y in range(height):
        for x in range(width):
            first, second = pa[x, y], pb[x, y]
            if first == second:
                continue
            # Every changed pixel counts toward the area budget, and the channel
            # delta is a separate ceiling. Counting only the pixels that EXCEED
            # the delta -- which is what the CN1SS comparator does -- leaves an
            # unbounded hole: with maxChannelDelta=160, recolouring these figures'
            # green #06a806 to #a608a6 moves every channel by exactly 160, so not
            # one pixel would be counted and a completely different image would
            # pass.
            changed += 1
            worst = max(worst, max(abs(first[i] - second[i]) for i in range(4)))
    percent = 100.0 * changed / (width * height)
    if percent > max_percent:
        return f"{percent:.3f}% of pixels changed (allowed {max_percent}%)"
    if worst > max_delta:
        return f"worst channel delta {worst} exceeds maxChannelDelta={max_delta}"
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--generated", required=True, type=Path)
    parser.add_argument("--committed", required=True, type=Path)
    parser.add_argument("--expected-count", type=int, required=True)
    args = parser.parse_args()

    produced = sorted(args.generated.glob("*.png"))
    if len(produced) != args.expected_count:
        print(
            f"::error::Expected {args.expected_count} generated figures, found {len(produced)}",
            file=sys.stderr,
        )
        return 1

    failures = 0
    tolerated = 0
    for image in produced:
        committed = args.committed / image.name
        if not committed.exists():
            print(f"::error::Generated figure has no committed counterpart: {image.name}", file=sys.stderr)
            failures += 1
            continue
        sidecar = args.committed / (image.stem + ".tolerance")
        tolerance = read_tolerance(sidecar)
        problem = compare(image, committed, tolerance)
        if problem:
            print(f"::error::{image.name}: {problem}", file=sys.stderr)
            failures += 1
        elif tolerance is not None and image.read_bytes() != committed.read_bytes():
            tolerated += 1
            print(f"{image.name}: within its tolerance sidecar")

    if failures:
        print(f"\n{failures} figure(s) do not match.", file=sys.stderr)
        return 1
    print(f"All {len(produced)} figures match ({tolerated} within a tolerance sidecar).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
