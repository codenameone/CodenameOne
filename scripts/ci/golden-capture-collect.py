#!/usr/bin/env python3
"""Merge downloaded cn1ss-capture-* artifacts into the checkout.

Usage: golden-capture-collect.py [--harvest] [--manifest=PORT_STATUS] DOWNLOADS DEST [BASE]

DOWNLOADS/<run>/<artifact>/<repo path>.png, plus each artifact's
cn1ss-emitted.json (every image that leg produced, golden or not). DEST is the
repository checkout; BASE, the PR's base revision.

Capture (golden-capture.yml): an image is taken only when it fills a gap
cn1ss-golden-plan.py finds in DEST: its directory lacks a golden for the test
that owns it, or lost one the base has. A gap still open afterwards fails,
naming the test and directory. A deleted golden counts as closed when a leg
writing that directory shows the suite no longer renders it.

Harvest (golden-harvest.yml, after a normal run): every image without a golden
is taken, provided screenshot_mappings names its owner. These are the goldens
the source cannot predict: a new variant of an existing test, a platform's own
image set. An image no mapping owns fails, naming it.

Either way nothing in DEST is overwritten, and two legs that share a directory
(the Windows legs) must produce the same image. Glyph anti-aliasing may differ
between x64 and arm64 by a few pixels; when the legs differ only within
AA_MAX_DELTA on at most AA_MAX_PERCENT of the pixels, the first leg's image is
committed with a <stem>.tolerance sidecar of exactly those bounds. Anything
more is a real difference: neither image is committed.
"""
import filecmp
import importlib.util
import json
import os
import shutil
import struct
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("plan", os.path.join(HERE, "cn1ss-golden-plan.py"))
plan = importlib.util.module_from_spec(spec)
spec.loader.exec_module(plan)


# Bounds within which two legs sharing a directory count as rendering alike
# (see the module comment). They match the hand-written sidecars this
# replaced; widening them hides real layout differences.
AA_MAX_DELTA = 16
AA_MAX_PERCENT = 0.1


def read_png(path):
    """(width, height, channels, rows) of an 8-bit non-interlaced PNG, or None."""
    with open(path, "rb") as fh:
        data = fh.read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    pos, idat, hdr = 8, b"", None
    while pos < len(data):
        n, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        if kind == b"IHDR":
            hdr = struct.unpack(">IIBBBBB", body)
        elif kind == b"IDAT":
            idat += body
        pos += 12 + n
    if not hdr:
        return None
    w, h, depth, ctype, _, _, interlace = hdr
    ch = {0: 1, 2: 3, 4: 2, 6: 4}.get(ctype)
    if depth != 8 or interlace or ch is None:
        return None
    raw, stride, rows, prev = zlib.decompress(idat), w * ch, [], bytearray(w * ch)
    for y in range(h):
        f, line = raw[y * (stride + 1)], bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for i in range(stride):
            a = line[i - ch] if i >= ch else 0
            b, c = prev[i], prev[i - ch] if i >= ch else 0
            if f == 1:
                line[i] = (line[i] + a) & 255
            elif f == 2:
                line[i] = (line[i] + b) & 255
            elif f == 3:
                line[i] = (line[i] + (a + b) // 2) & 255
            elif f == 4:
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    return w, h, ch, rows


def aa_only(a, b):
    """True when two images differ only by anti-aliasing (see AA_MAX_*)."""
    pa, pb = read_png(a), read_png(b)
    if not pa or not pb or pa[:3] != pb[:3]:
        return False
    w, h, ch, ra = pa
    bad = 0
    for y in range(h):
        x1, x2 = ra[y], pb[3][y]
        if x1 == x2:
            continue
        for x in range(0, w * ch, ch):
            d = max(abs(x1[x + k] - x2[x + k]) for k in range(ch))
            if d > AA_MAX_DELTA:
                return False
            bad += d > 0
    return bad * 100.0 <= AA_MAX_PERCENT * w * h


def write_aa_tolerance(target, legs):
    side = target[:-4] + ".tolerance"
    if os.path.exists(side):
        return
    with open(side, "w") as fh:
        fh.write("# Written by golden-capture-collect.py: %s render this image with\n"
                 "# glyph anti-aliasing that differs by a few pixels.\n"
                 "maxChannelDelta=%d\nmaxMismatchPercent=%s\n" % (" and ".join(legs), AA_MAX_DELTA, AA_MAX_PERCENT))


def owner_of(stem, mappings):
    for pat, t in mappings:
        if plan.fnmatch.fnmatchcase(stem, pat):
            return t
    return None


def collect(dl, dest, base=None, harvest=False, **plan_args):
    mappings = plan.load_mappings(plan_args.get("manifest", plan.MANIFEST))
    dir_ports = plan_args.get("dir_ports", plan.DIR_PORTS)
    ok = True
    if harvest:
        gaps, lost = [], {}
    else:
        lost = {}
        gaps, _ = plan.plan(root=dest, base=base, lost_out=lost, **plan_args)
    open_gaps = set(gaps)
    added, emitted, first_art = {}, {}, {}
    for run in sorted(os.listdir(dl)):
        for art in sorted(os.listdir(os.path.join(dl, run))):
            base_dir = os.path.join(dl, run, art)
            listing = os.path.join(base_dir, "cn1ss-emitted.json")
            if os.path.isfile(listing):
                with open(listing) as fh:
                    for gdir, names in json.load(fh).items():
                        emitted.setdefault(gdir, set()).update(names)
            for d, _, files in os.walk(base_dir):
                for f in sorted(files):
                    if not f.endswith(".png"):
                        continue
                    src = os.path.join(d, f)
                    if os.path.islink(src) or not os.path.isfile(src):
                        print("::error::%s (%s) is not a regular file" % (f, art))
                        ok = False
                        continue
                    rel = os.path.relpath(src, base_dir)
                    gdir, stem = os.path.dirname(rel), f[:-4]
                    if gdir not in dir_ports:
                        print("::error::%s (%s) is outside every golden directory" % (rel, art))
                        ok = False
                        continue
                    owner = owner_of(stem, mappings)
                    if harvest:
                        if owner is None:
                            print("::error::%s (%s) has no golden and no screenshot_mappings entry "
                                  "in docs/website/data/port_status.json owns it; add one" % (rel, art))
                            ok = False
                            continue
                    elif owner is None or (owner, gdir) not in open_gaps and rel not in added:
                        print("[golden-capture] ignored %s (%s): fills no gap" % (rel, art), file=sys.stderr)
                        continue
                    if rel in added:
                        if (added[rel] and not filecmp.cmp(added[rel], src, shallow=False)
                                and aa_only(added[rel], src)):
                            write_aa_tolerance(os.path.join(dest, rel), [first_art[rel], art])
                            print("[golden-capture] %s: %s differs from the first leg only by anti-aliasing; "
                                  "kept the first with a tolerance sidecar" % (rel, art), file=sys.stderr)
                        elif added[rel] and not filecmp.cmp(added[rel], src, shallow=False):
                            # One file cannot be a golden for both legs: take
                            # neither, so no leg is left failing against the
                            # other's image.
                            print("::error::%s differs between %s and the leg captured first; the legs "
                                  "sharing this directory do not render it alike, so neither is committed; make the "
                                  "test render identically on both, or give one leg its own golden "
                                  "directory" % (rel, art))
                            os.remove(os.path.join(dest, rel))
                            added[rel] = None
                            ok = False
                        continue
                    target = os.path.join(dest, rel)
                    if os.path.exists(target):
                        continue
                    os.makedirs(os.path.dirname(target), exist_ok=True)
                    shutil.copyfile(src, target)
                    added[rel] = src
                    first_art[rel] = art
                    print("[golden-capture] added %s (%s)" % (rel, art), file=sys.stderr)
    for t, gd in sorted(open_gaps):
        dropped = [r for r, src in added.items() if src is None and os.path.dirname(r) == gd]
        want = lost.get((t, gd))
        if want:
            gone = {s for s in want if not os.path.exists(os.path.join(dest, gd, s + ".png"))}
            gone |= {os.path.basename(r)[:-4] for r in dropped} & want
            if gd in emitted:
                for s in sorted(gone & emitted[gd]):
                    print("::error::%s/%s.png was deleted, the suite still renders it and no leg "
                          "captured it" % (gd, s))
                obsolete = gone - emitted[gd]
                for s in sorted(obsolete):
                    print("[golden-capture] %s/%s.png: no longer rendered, stays deleted" % (gd, s),
                          file=sys.stderr)
                gone -= obsolete
            if not gone:
                continue
            print("::error::%s: %s not restored in %s; no leg writing it reported in"
                  % (t, ", ".join(sorted(gone)), gd))
            ok = False
        elif not any(added[r] and os.path.dirname(r) == gd and owner_of(os.path.basename(r)[:-4], mappings) == t
                     for r in added):
            print("::error::%s produced no image for %s: its leg failed, or it captures nothing "
                  "there and needs a `// cn1ss-skip: <ports>` declaration" % (t, gd))
            ok = False
    return ok


def main(argv):
    harvest = "--harvest" in argv
    kw = {}
    for a in argv[1:]:
        if a.startswith("--manifest="):
            kw["manifest"] = a[len("--manifest="):]
    args = [a for a in argv[1:] if not a.startswith("--")]
    if len(args) not in (2, 3):
        print(__doc__, file=sys.stderr)
        return 2
    return 0 if collect(args[0], args[1], base=args[2] if len(args) == 3 else None,
                    harvest=harvest, **kw) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
