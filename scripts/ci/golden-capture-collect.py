#!/usr/bin/env python3
"""Merge downloaded cn1ss-capture-* artifacts into the checkout.

Usage: golden-capture-collect.py DOWNLOADS DEST TESTS

DOWNLOADS/<run>/<artifact>/<repo path>.png. Keeps only images that belong to
one of TESTS (comma separated) by the detector's own naming rules, never
overwrites a golden DEST already has, and fails when two legs that share a
golden directory captured different bytes for the same image.
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


def collect(dl, dest, tests, classes):
    names = set()
    for t in tests:
        names |= plan.candidate_names(t, classes)
    added, ok = {}, True
    for run in sorted(os.listdir(dl)):
        for art in sorted(os.listdir(os.path.join(dl, run))):
            base = os.path.join(dl, run, art)
            for d, _, files in os.walk(base):
                for f in sorted(files):
                    src = os.path.join(d, f)
                    rel = os.path.relpath(src, base)
                    if not f.endswith(".png") or not plan.covered(names, {f[:-4]}):
                        print("[golden-capture] ignored %s (%s): not a test being captured" % (rel, art), file=sys.stderr)
                        continue
                    target = os.path.join(dest, rel)
                    if rel in added:
                        if not filecmp.cmp(added[rel], src, shallow=False):
                            print("::error::%s differs between %s and the leg captured first" % (rel, art))
                            ok = False
                        continue
                    if os.path.exists(target):
                        print("[golden-capture] kept existing %s (%s)" % (rel, art), file=sys.stderr)
                        continue
                    os.makedirs(os.path.dirname(target), exist_ok=True)
                    shutil.copyfile(src, target)
                    added[rel] = src
                    print("[golden-capture] added %s (%s)" % (rel, art), file=sys.stderr)
    return ok


if __name__ == "__main__":
    classes, _ = plan.index_sources(plan.SOURCES)
    tests = [t for t in sys.argv[3].split(",") if t]
    sys.exit(0 if collect(sys.argv[1], sys.argv[2], tests, classes) else 1)
