#!/usr/bin/env python3
"""Regenerate the Unicode word-character table for the pure editor.

Requires Python unicodedata 16.0.0 so regeneration is deterministic. This is
classification for editor word movement, not UAX #29 word/grapheme segmentation.
"""
from pathlib import Path
import unicodedata

if unicodedata.unidata_version != "16.0.0":
    raise RuntimeError("Requires Unicode 16.0.0, got " + unicodedata.unidata_version)

ranges = []
for cp in range(0x110000):
    category = unicodedata.category(chr(cp))
    if category[0] in ("L", "M") or category == "Nd" or cp == 95:
        if ranges and cp == ranges[-1][1] + 1:
            ranges[-1][1] = cp
        else:
            ranges.append([cp, cp])

values = [hex(cp) for pair in ranges for cp in pair]
lines = [
    "        " + ", ".join(values[i:i + 12])
    + ("," if i + 12 < len(values) else "")
    for i in range(0, len(values), 12)
]
root = Path(__file__).resolve().parent.parent
path = root / "CodenameOne/src/com/codename1/ui/editor/WordCharacters.java"
source = path.read_text(encoding="utf-8")
marker = "    private static final int[] RANGES = {"
start = source.index(marker)
end = source.index("    };", start)
source = source[:start] + marker + "\n" + "\n".join(lines) + "\n" + source[end:]
path.write_text(source, encoding="utf-8")
print(len(ranges), "ranges")
