#!/usr/bin/env python3
"""Merge downloaded cn1ss-capture-* artifacts into the checkout.

Usage: golden-capture-collect.py DOWNLOADS DEST

DOWNLOADS/<run>/<artifact>/<repo path>.png. DEST is the repository checkout.
An image is taken only when it fills a gap cn1ss-golden-plan.py finds in DEST:
its directory lacks a golden for the test that owns it. Nothing in DEST is
overwritten. Two legs that share a directory (Windows x64 and arm64) yield one
file; when their bytes differ the first is kept and a warning names it.

Exits 1 when a gap is still open afterwards, naming the test and directory: the
leg failed, or the test captures nothing there and needs a
`// cn1ss-skip: <ports>` declaration.
"""
import filecmp
import importlib.util
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("plan", os.path.join(HERE, "cn1ss-golden-plan.py"))
plan = importlib.util.module_from_spec(spec)
spec.loader.exec_module(plan)


def collect(dl, dest, **plan_args):
    gaps, _ = plan.plan(root=dest, **plan_args)
    classes, _ = plan.index_sources(plan_args.get("sources_dir", plan.SOURCES))
    mappings = plan.load_mappings(plan_args.get("manifest", plan.MANIFEST))
    open_gaps = set(gaps)
    added = {}
    for run in sorted(os.listdir(dl)):
        for art in sorted(os.listdir(os.path.join(dl, run))):
            base = os.path.join(dl, run, art)
            for d, _, files in os.walk(base):
                for f in sorted(files):
                    src = os.path.join(d, f)
                    rel = os.path.relpath(src, base)
                    gdir, stem = os.path.dirname(rel), f[:-4]
                    owner = [t for t, gd in gaps if gd == gdir and f.endswith(".png")
                             and plan.owns(t, {stem}, mappings, classes)]
                    if not owner:
                        print("[golden-capture] ignored %s (%s): fills no gap" % (rel, art), file=sys.stderr)
                        continue
                    if rel in added:
                        if not filecmp.cmp(added[rel], src, shallow=False):
                            print("::warning::%s differs between %s and the leg captured first; kept the first"
                                  % (rel, art))
                        continue
                    target = os.path.join(dest, rel)
                    if os.path.exists(target):
                        continue
                    os.makedirs(os.path.dirname(target), exist_ok=True)
                    shutil.copyfile(src, target)
                    added[rel] = src
                    open_gaps.discard((owner[0], gdir))
                    print("[golden-capture] added %s (%s)" % (rel, art), file=sys.stderr)
    for t, gd in sorted(open_gaps):
        print("::error::%s produced no image for %s: its leg failed, or it captures nothing "
              "there and needs a `// cn1ss-skip: <ports>` declaration" % (t, gd))
    return not open_gaps


if __name__ == "__main__":
    sys.exit(0 if collect(sys.argv[1], sys.argv[2]) else 1)
