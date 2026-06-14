#!/usr/bin/env python3
"""Sanity-check a JavaScript-port screenshot comparison so a threshold/measurement
mix-up or a capture bug cannot read as "green".

Two failure classes this guards against (both bit us once):

1. THRESHOLD-AS-MEASUREMENT: screenshot-compare.json carries both the per-run
   tolerance *config* (details.max_channel_delta / details.max_mismatch_percent,
   which are CONSTANT, e.g. 4 / 0.3) and the actual *measurements*
   (details.mismatch_percent / details.mismatch_count). Reading the constant
   threshold as if it were the measured delta makes wildly-different images look
   "within 4". This script categorises purely on details.mismatch_percent.

2. CAPTURE BUG / FROZEN DISPLAY: when the capture grabs the wrong/stale surface,
   many DIFFERENT tests deliver the byte-identical image. We flag any group of
   >=N tests whose delivered PNG is identical (same fnv1a64 / file hash) -- a
   strong signal the screenshots are not per-test renders.

Exit non-zero (fail) when too many tests mismatch heavily or a duplicate cluster
is found, unless --report-only is passed.

Usage:
  jsport-screenshot-sanity.py <screenshot-compare.json> [delivered_dir]
      [--wrong-threshold 50] [--max-wrong 0] [--dup-cluster 3] [--report-only]
"""
import argparse
import hashlib
import json
import os
import sys


def measured_mismatch(result):
    det = result.get("details") or {}
    return det.get("mismatch_percent")


def file_hash(path):
    try:
        with open(path, "rb") as fh:
            return hashlib.sha1(fh.read()).hexdigest()
    except OSError:
        return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("compare_json")
    ap.add_argument("delivered_dir", nargs="?", default=None)
    ap.add_argument("--wrong-threshold", type=float, default=50.0,
                    help="mismatch_percent at/above which a test is 'wrong' (default 50)")
    ap.add_argument("--near-threshold", type=float, default=25.0,
                    help="mismatch_percent below which a test is treated as a correct render / AA reseed (default 25)")
    ap.add_argument("--max-wrong", type=int, default=0,
                    help="fail if more than this many tests are 'wrong' (default 0)")
    ap.add_argument("--dup-cluster", type=int, default=3,
                    help="fail if >= this many tests deliver an identical image (default 3)")
    ap.add_argument("--report-only", action="store_true")
    args = ap.parse_args()

    with open(args.compare_json) as fh:
        data = json.load(fh)
    results = data["results"] if isinstance(data, dict) and "results" in data else data

    measured, no_measure = [], []
    for r in results:
        if measured_mismatch(r) is None:
            no_measure.append(r)
        else:
            measured.append(r)

    wrong = [r for r in measured if measured_mismatch(r) >= args.wrong_threshold]
    near = [r for r in measured if measured_mismatch(r) < args.near_threshold]
    mid = [r for r in measured
           if args.near_threshold <= measured_mismatch(r) < args.wrong_threshold]

    print("=== JS-port screenshot sanity (measured on mismatch_percent) ===")
    print("compared: %d  (correct/reseed <%.0f%%: %d, mid: %d, WRONG >=%.0f%%: %d, no-measure: %d)"
          % (len(results), args.near_threshold, len(near), len(mid),
             args.wrong_threshold, len(wrong), len(no_measure)))

    if wrong:
        print("\nWRONG (>=%.0f%% mismatch -- not a real render):" % args.wrong_threshold)
        for r in sorted(wrong, key=lambda r: -measured_mismatch(r)):
            print("  %6.1f%%  %s" % (measured_mismatch(r), r["test"]))

    # Duplicate-image clustering (capture-bug signal).
    dup_failures = []
    if args.delivered_dir and os.path.isdir(args.delivered_dir):
        by_hash = {}
        for r in results:
            p = os.path.join(args.delivered_dir, r["test"] + ".png")
            h = file_hash(p)
            if h:
                by_hash.setdefault(h, []).append(r["test"])
        clusters = [tests for tests in by_hash.values() if len(tests) >= args.dup_cluster]
        if clusters:
            print("\nDUPLICATE-IMAGE CLUSTERS (different tests, identical delivered PNG -> capture bug):")
            for tests in sorted(clusters, key=len, reverse=True):
                dup_failures.append(tests)
                print("  %d tests share one image: %s" % (len(tests), ", ".join(sorted(tests)[:12])
                      + (" ..." if len(tests) > 12 else "")))
    else:
        print("\n(no delivered_dir given -- skipping duplicate-image check)")

    failed = (len(wrong) > args.max_wrong) or bool(dup_failures)
    if failed and not args.report_only:
        print("\nFAIL: %d wrong (max %d) ; %d duplicate cluster(s)."
              % (len(wrong), args.max_wrong, len(dup_failures)))
        return 1
    print("\nOK" if not failed else "\n(report-only) issues found")
    return 0


if __name__ == "__main__":
    sys.exit(main())
