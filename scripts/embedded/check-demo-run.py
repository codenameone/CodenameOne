#!/usr/bin/env python3
"""Judges one run of the embedded demo on the memory display.

    check-demo-run.py [--max-rss-mb N] [--reference ref.png] <demo-log> <snapshot.png> [summary.md]

The demo was run with CN1_DISPLAY=memory CN1_SIZE=800x480 CN1_BENCH_SECONDS and
CN1_SNAPSHOT, so its log carries the CN1BENCH lines and the snapshot is frame 60
as a PNG. Fails when the frame is missing, the wrong size or blank (a renderer
that draws nothing still runs at full speed), or when the speed or memory the
developer guide reports has slipped past the limits below. Standard library only.

--reference holds the frame to another run's, pixel for pixel: CI renders the
demo with the GPU renderer and the software one, and the two must agree.
--max-rss-mb replaces the memory limit; a GPU run carries Mesa's driver, which
on the runner is llvmpipe and costs far more than a board's GPU driver does.
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
    previous, colours, i, pixels = bytearray(stride), set(), 0, bytearray()
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
            pixels += line[x:x + 3]
        previous = line
    return width, height, len(colours), bytes(pixels)


def main():
    args, max_rss, reference = sys.argv[1:], MAX_RSS_MB, None
    while args and args[0].startswith("--") and len(args) > 1:
        if args[0] == "--max-rss-mb":
            max_rss = float(args[1])
        elif args[0] == "--reference":
            reference = args[1]
        else:
            print(__doc__)
            return 2
        args = args[2:]
    if len(args) < 2:
        print(__doc__)
        return 2
    log = open(args[0], errors="replace").read()
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
        check("resident memory (MB)", rss, max_rss, rss <= max_rss)
    try:
        width, height, colours, pixels = png_colours(args[1])
        check("snapshot size", "{}x{}".format(width, height), "{}x{}".format(WIDTH, HEIGHT),
              (width, height) == (WIDTH, HEIGHT))
        check("snapshot colours", colours, MIN_COLOURS, colours >= MIN_COLOURS)
        if reference:
            other = png_colours(reference)[3]
            differing = sum(1 for i in range(0, min(len(pixels), len(other)), 3)
                            if pixels[i:i + 3] != other[i:i + 3])
            if len(pixels) != len(other):
                differing = max(differing, 1)
            check("pixels differing from the reference", differing, 0, differing == 0)
    except (OSError, ValueError, zlib.error, TypeError) as error:
        check("snapshot readable ({})".format(error), 0, 1, False)

    lines = ["| Check | Value | Limit | |", "|---|---|---|---|"]
    for name, value, limit, ok in checks:
        lines.append("| {} | {} | {} | {} |".format(name, value, limit, "pass" if ok else "**FAIL**"))
    report = "\n".join(lines)
    print(report)
    if len(args) > 2:
        with open(args[2], "a") as summary:
            summary.write("### Embedded demo\n\n" + report + "\n")
    return 0 if all(ok for _, _, _, ok in checks) else 1


if __name__ == "__main__":
    sys.exit(main())
