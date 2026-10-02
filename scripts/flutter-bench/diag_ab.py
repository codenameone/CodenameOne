#!/usr/bin/env python3
"""DIAGNOSTIC BRANCH ONLY: interleaved cold starts of several macOS arms.

usage: diag_ab.py --rounds N --arm name=codenameone:/path/App.app[:ENV=V,...] ...
                  --arm flutter=flutter:/path/gallery.app

Every round launches every arm once, in a rotating order, so a runner that
slows down part way through slows every arm alike. Prints each launch, then a
per-arm best/median and the per-round ratio of each arm to the first one.
"""
import argparse
import os
import statistics
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import platforms  # noqa: E402


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--rounds", type=int, default=10)
    ap.add_argument("--arm", action="append", required=True)
    args = ap.parse_args()
    arms = []
    for spec in args.arm:
        name, rest = spec.split("=", 1)
        parts = rest.split(":")
        side, path = parts[0], parts[1]
        env = {}
        if len(parts) > 2 and parts[2]:
            for kv in parts[2].split(","):
                k, v = kv.split("=", 1)
                env[k] = v
        arms.append((name, side, path, env))
    results = {a[0]: [] for a in arms}
    base_env = dict(os.environ)
    for r in range(args.rounds):
        order = arms[r % len(arms):] + arms[:r % len(arms)]
        for name, side, path, env in order:
            os.environ.clear()
            os.environ.update(base_env)
            os.environ.update(env)
            if side == "codenameone":
                adapter = platforms.MacOSAdapter(path, path)
            else:
                adapter = platforms.MacOSAdapter(path, path)
            try:
                upper, lower, memory = adapter.launch_and_time(side)
            except Exception as e:  # noqa: BLE001
                print("  round %d %-12s FAILED %s" % (r + 1, name, e), flush=True)
                upper = None
            results[name].append(upper)
            print("  round %d %-12s cold=%s" % (r + 1, name,
                  "%7.0fms" % upper if upper is not None else "   none"), flush=True)
    os.environ.clear()
    os.environ.update(base_env)
    print("\nAB SUMMARY")
    first = arms[0][0]
    for name, _, _, _ in arms:
        xs = [x for x in results[name] if x is not None]
        if not xs:
            print("  %-12s no data" % name)
            continue
        ratios = [a / b for a, b in zip(results[name], results[first])
                  if a is not None and b is not None]
        print("  %-12s best=%6.0f median=%6.0f  runs=%s  median ratio to %s=%.2f" % (
            name, min(xs), statistics.median(xs),
            ",".join("%.0f" % x for x in xs), first,
            statistics.median(ratios) if ratios else float("nan")))


if __name__ == "__main__":
    main()
