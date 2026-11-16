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
(the Windows legs) must produce identical bytes.
"""
import filecmp
import importlib.util
import json
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("plan", os.path.join(HERE, "cn1ss-golden-plan.py"))
plan = importlib.util.module_from_spec(spec)
spec.loader.exec_module(plan)


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
    added, emitted = {}, {}
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
                        if added[rel] and not filecmp.cmp(added[rel], src, shallow=False):
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
