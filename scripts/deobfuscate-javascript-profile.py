#!/usr/bin/env python3
"""Restore the function names in a Codename One JavaScript profile or stack trace.

The JavaScript build renames every translated function to a short ``$M...``
name and writes the mapping beside the bundle as ``<App>-js-symbols.txt``:
one line per function, the short name, a tab, the translator's name
(``cn1_<package>_<Class>_<method>_<signature>``).

This script replaces each short name in a file with the translator's name.
It works on anything that names functions as plain text: a profile saved
from the Chrome DevTools Performance panel (``.json``), a ``.cpuprofile``,
or a pasted stack trace. Load the rewritten profile back into DevTools.

    deobfuscate-javascript-profile.py MyApp-js-symbols.txt Trace.json > Trace-named.json

Use the map from the same build as the bundle you profiled: the short names
are assigned per build.
"""

import re
import sys

# A whole identifier only: $Mab must not match inside $Mabc or x$Mab.
SHORT_NAME = re.compile(r"(?<![A-Za-z0-9_$])\$M[a-z]+(?![A-Za-z0-9_$])")


def load_map(path):
    names = {}
    with open(path, encoding="utf-8") as f:
        for number, line in enumerate(f, 1):
            line = line.rstrip("\r\n")
            if not line:
                continue
            parts = line.split("\t")
            if len(parts) != 2:
                sys.exit("%s:%d: expected '<short name><TAB><name>'" % (path, number))
            names[parts[0]] = parts[1]
    if not names:
        sys.exit("%s: the symbol map is empty" % path)
    return names


def main(argv):
    if len(argv) not in (2, 3):
        sys.exit("usage: %s <App>-js-symbols.txt [profile-or-trace] > output" % sys.argv[0])
    names = load_map(argv[1])
    if len(argv) == 3:
        with open(argv[2], encoding="utf-8") as f:
            text = f.read()
    else:
        text = sys.stdin.read()
    missing = set()

    def replace(match):
        short = match.group(0)
        name = names.get(short)
        if name is None:
            missing.add(short)
            return short
        return name

    sys.stdout.write(SHORT_NAME.sub(replace, text))
    if missing:
        # A name the map lacks means the map is from another build; say so rather
        # than leave a half-translated profile looking complete.
        sys.stderr.write("warning: %d short names are not in %s (a map from a different build?): %s\n"
                         % (len(missing), argv[1], ", ".join(sorted(missing)[:10])))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
