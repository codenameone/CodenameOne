#!/usr/bin/env python3
"""Holds a crypto benchmark run to scripts/crypto-bench/floors.json.

    gate.py jvm  jvm-output.txt
    gate.py native native-output.txt

Reads the lines JvmBench and native/bench.c print (name core=... ref=...
ratio=...), prints a Markdown table of them against their floors, and exits 1
when any ratio is below its floor. A floor whose row is absent is listed as
skipped: the reference library it needs was not on the runner.
"""
import json
import os
import re
import sys

ROW = re.compile(r"^(\S+)\s+core=\s*([\d.]+)\s+ref=\s*([\d.]+)\s+ratio=\s*([\d.]+)")


def main(argv):
    if len(argv) != 3 or argv[1] not in ("jvm", "native"):
        print(__doc__, file=sys.stderr)
        return 2
    kind, path = argv[1], argv[2]
    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "floors.json")) as f:
        floors = json.load(f)[kind]
    rows = {}
    notes = []
    with open(path) as f:
        for line in f:
            line = line.rstrip("\n")
            if line.startswith("#"):
                notes.append(line[1:].strip())
                continue
            m = ROW.match(line)
            if m:
                rows[m.group(1)] = (float(m.group(2)), float(m.group(3)), float(m.group(4)))
    if not rows:
        print("no benchmark rows in " + path, file=sys.stderr)
        return 1
    failed = []
    print("| %s operation | core ops/s | reference ops/s | ratio | floor |" % kind)
    print("|---|---:|---:|---:|---:|")
    for name in sorted(set(rows) | set(floors)):
        floor = floors.get(name)
        if name not in rows:
            print("| %s | - | - | skipped | %s |" % (name, floor))
            continue
        core, ref, ratio = rows[name]
        mark = ""
        if floor is not None and ratio < floor:
            failed.append(name)
            mark = " **below floor**"
        print("| %s | %.0f | %.0f | %.2f%s | %s |" % (name, core, ref, ratio, mark,
                                                      "-" if floor is None else floor))
    for n in notes:
        print("\n" + n)
    if failed:
        print("\nbelow floor: " + ", ".join(failed), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
