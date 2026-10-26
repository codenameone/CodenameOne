#!/usr/bin/env python3
"""Judges one run of the embedded demo on the memory display.

    check-demo-run.py <demo-log> <snapshot.png> [summary.md]

The demo was run with CN1_DISPLAY=memory CN1_SIZE=800x480 CN1_BENCH_SECONDS and
CN1_SNAPSHOT, so its log carries the CN1BENCH lines and the snapshot is frame 60
as a PNG. Fails when the frame is missing, the wrong size or blank (a renderer
that draws nothing still runs at full speed), or when the speed or memory the
developer guide reports has slipped past the limits below. Standard library only.
"""

import re
import struct
import sys
import zlib

WIDTH, HEIGHT = 800, 480
# The demo paces itself to 60 frames a second; the guide reports 57.5.
MIN_FPS = 50
# Process start to the first frame on the runner's warm disk; about 50 ms on a
# Pi-class board, and an order of magnitude below what this would catch.
MAX_FIRST_FRAME_MS = 500
# Resident set while animating; the guide reports 18 MB of PSS.
MAX_RSS_MB = 40
# Distinct colours frame 60 must hold: the gauge, its text and the controls.
MIN_COLOURS = 50


def png_colours(path):
    """(width, height, distinct RGB triples) of an 8-bit RGB or RGBA PNG."""
    data = open(path, "rb").read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    pos, idat, header = 8, b"", None
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            header = struct.unpack(">IIBBBBB", body)
        elif kind == b"IDAT":
            idat += body
        pos += 12 + length
    width, height, depth, colour = header[0], header[1], header[2], header[3]
    if depth != 8 or colour not in (2, 6):
        raise ValueError("unexpected PNG format depth={} colour={}".format(depth, colour))
    bpp = 3 if colour == 2 else 4
    raw, stride = zlib.decompress(idat), width * bpp
    previous, colours, i = bytearray(stride), set(), 0
    for _ in range(height):
        kind, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = previous[x]
            c = previous[x - bpp] if x >= bpp else 0
            if kind == 1:
                line[x] = (line[x] + a) & 255
            elif kind == 2:
                line[x] = (line[x] + b) & 255
            elif kind == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif kind == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        for x in range(0, stride, bpp):
            colours.add(bytes(line[x:x + 3]))
        previous = line
    return width, height, len(colours)


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    log = open(sys.argv[1], errors="replace").read()
    checks = []

    def check(name, value, limit, ok):
        checks.append((name, value, limit, ok))

    first = re.search(r"CN1BENCH first_frame_epoch_ms=\d+ first_frame_ms=(\d+)", log)
    run = re.findall(r"CN1BENCH fps=(\d+) frames=\d+ rss_kb=(\d+)", log)
    check("benchmark lines in the log", int(bool(first and run)), 1, bool(first and run))
    if first and run:
        fps, rss = int(run[-1][0]), int(run[-1][1]) / 1000.0
        check("first frame (ms)", int(first.group(1)), MAX_FIRST_FRAME_MS,
              int(first.group(1)) <= MAX_FIRST_FRAME_MS)
        check("frames per second", fps, MIN_FPS, fps >= MIN_FPS)
        check("resident memory (MB)", rss, MAX_RSS_MB, rss <= MAX_RSS_MB)
    try:
        width, height, colours = png_colours(sys.argv[2])
        check("snapshot size", "{}x{}".format(width, height), "{}x{}".format(WIDTH, HEIGHT),
              (width, height) == (WIDTH, HEIGHT))
        check("snapshot colours", colours, MIN_COLOURS, colours >= MIN_COLOURS)
    except (OSError, ValueError, zlib.error, TypeError) as error:
        check("snapshot readable ({})".format(error), 0, 1, False)

    lines = ["| Check | Value | Limit | |", "|---|---|---|---|"]
    for name, value, limit, ok in checks:
        lines.append("| {} | {} | {} | {} |".format(name, value, limit, "pass" if ok else "**FAIL**"))
    report = "\n".join(lines)
    print(report)
    if len(sys.argv) > 3:
        with open(sys.argv[3], "a") as summary:
            summary.write("### Embedded demo\n\n" + report + "\n")
    return 0 if all(ok for _, _, _, ok in checks) else 1


if __name__ == "__main__":
    sys.exit(main())
