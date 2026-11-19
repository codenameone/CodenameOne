#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of ParparVM.
"""Keeps the objects of a previous translation that this one would compile the same.

    reuse-objects.py <sources> <previous sources> <previous objects> <objects> <flags>

Prints the sources that still have to be compiled, one a line, and moves the
object of every other one out of the previous build.

A translation writes every file again, so a time stamp says nothing: files are
compared byte for byte. An object is kept when its source is unchanged, every
header the compiler read for it (the .d file clang wrote beside it) is unchanged,
and the compiler and its flags are the ones it was built with. A class added to
a program renumbers what the generated headers declare, and then nearly nothing
is kept -- which is the right answer, not a failure of this script.
"""
import filecmp
import os
import sys


def dependencies(path, sources):
    """The names, within the sources, of the files a .d file lists."""
    with open(path, encoding="utf-8", errors="replace") as f:
        text = f.read().replace("\\\n", " ")
    names = set()
    for word in text.split(":", 1)[-1].split():
        if os.path.dirname(word) == sources:
            names.add(os.path.basename(word))
    return names


def main():
    sources, old_sources, old_objects, objects, flags = sys.argv[1:6]
    wanted = sorted(n for n in os.listdir(sources) if n.endswith((".c", ".S")))
    try:
        with open(os.path.join(old_objects, "flags.txt"), encoding="utf-8") as f:
            same_flags = f.read() == flags
    except OSError:
        same_flags = False
    changed = set()
    if same_flags and os.path.isdir(old_sources):
        for name in os.listdir(sources):
            old = os.path.join(old_sources, name)
            if not os.path.isfile(old) or not filecmp.cmp(os.path.join(sources, name), old, shallow=False):
                changed.add(name)
    kept = 0
    for name in wanted:
        obj = os.path.join(old_objects, name + ".o")
        dep = os.path.join(old_objects, name + ".d")
        if same_flags and name not in changed and os.path.isfile(obj) and os.path.isfile(dep) \
                and not (dependencies(dep, sources) & changed):
            os.replace(obj, os.path.join(objects, name + ".o"))
            os.replace(dep, os.path.join(objects, name + ".d"))
            kept += 1
        else:
            print(os.path.join(sources, name))
    sys.stderr.write("kept %d of %d objects\n" % (kept, len(wanted)))


if __name__ == "__main__":
    main()
