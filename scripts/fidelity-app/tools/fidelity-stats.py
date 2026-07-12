#!/usr/bin/env python3
"""Summarize native-fidelity baselines into a Markdown report.

Reads the committed per-platform baseline JSON files
(baseline/<platform>-fidelity-baseline.json, each {"pairs": {name: percent}}) and
prints a Markdown "where we stand" report: per-component means, light/dark splits,
and the sorted improvement backlog.

Usage: fidelity-stats.py [baseline.json ...]   (defaults to the committed ones)
"""
import json
import os
import statistics as st
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
APP = os.path.dirname(HERE)
DEFAULTS = [
    ("Android (Material 3)", os.path.join(APP, "baseline", "android-fidelity-baseline.json")),
    ("iOS (Modern, Metal)", os.path.join(APP, "baseline", "ios-metal-fidelity-baseline.json")),
]


def load(path):
    with open(path) as fh:
        return json.load(fh).get("pairs", {})


def component(name):
    return name.split("_")[0]


def report(label, pairs):
    if not pairs:
        print(f"### {label}\n\n_No baseline recorded yet._\n")
        return
    vals = list(pairs.values())
    light = [v for k, v in pairs.items() if k.endswith("_light")]
    dark = [v for k, v in pairs.items() if k.endswith("_dark")]
    comps = {}
    for k, v in pairs.items():
        comps.setdefault(component(k), []).append(v)
    print(f"### {label}\n")
    print(f"- **Pairs:** {len(vals)}  ")
    print(f"- **Overall mean:** {st.mean(vals):.1f}%  median: {st.median(vals):.1f}%  ")
    if light:
        print(f"- **Light mean:** {st.mean(light):.1f}%  ", end="")
    if dark:
        print(f"**Dark mean:** {st.mean(dark):.1f}%  ")
    else:
        print()
    print(f"- **At/above 95%:** {sum(1 for v in vals if v >= 95)}/{len(vals)}  "
          f"**below 60%:** {sum(1 for v in vals if v < 60)}/{len(vals)}\n")
    print("| Component | Mean fidelity |")
    print("| --- | --- |")
    for c in sorted(comps, key=lambda c: st.mean(comps[c])):
        print(f"| {c} | {st.mean(comps[c]):.1f}% |")
    worst = sorted(pairs.items(), key=lambda kv: kv[1])[:8]
    print("\n**Lowest-fidelity pairs (improvement backlog):**\n")
    for k, v in worst:
        print(f"- `{k}` -- {v:.2f}%")
    print()


def main(argv):
    targets = []
    if len(argv) > 1:
        for p in argv[1:]:
            targets.append((os.path.basename(p), p))
    else:
        targets = DEFAULTS
    print("# Native theme fidelity -- where we stand\n")
    print("Each score is the visual similarity between Codename One's render of a "
          "component (under the native theme) and the REAL native OS widget, both "
          "rendered in the same environment. 100% = pixel-identical.\n")
    for label, path in targets:
        if os.path.isfile(path):
            report(label, load(path))
        else:
            print(f"### {label}\n\n_No baseline file at {path}._\n")


if __name__ == "__main__":
    main(sys.argv)
