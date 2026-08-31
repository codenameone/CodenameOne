#!/usr/bin/env python3
"""Reject raw control characters in tracked text files.

A control byte -- NUL, US, SOH, DEL -- written directly into a source file
instead of as an escape is invisible in every editor and silently poisons the
tooling around it. `file` reports the source as `data`, and from that point on
grep treats it as binary and prints *nothing at all*: not "binary file matches",
no error, just success with no output. A search for a symbol in that file comes
back empty and reads as "not there", which is how a real investigation stalled
on `GenerateOpenApiMojo.java` (a NUL inside `"\\u0000enum:"`, written raw).
`git diff`, review tooling and `javac -encoding ascii` all degrade the same way.

There is never a reason to write the byte itself: every language in this tree
has an escape for it, and the escape compiles to the identical value. So this
gate has no baseline and no exclusions list -- a finding is always fixed by
writing the escape.

Allowed: TAB (0x09), LF (0x0a) and CR (0x0d), because those are text.
Rejected: every other C0 control (0x00-0x1f) and DEL (0x7f).

Usage:
  scripts/check-control-characters.py             # every tracked text file
  scripts/check-control-characters.py PATH ...    # only these files
"""

import os
import subprocess
import sys

# Extensions whose contents are text this repository writes by hand or
# generates as source. Deliberately broad: config, scripts and docs corrupt
# exactly the same way source does, and all of them were clean when this gate
# was added, so including them costs nothing and closes the hole early.
TEXT_EXTENSIONS = frozenset([
    ".java", ".js", ".jsx", ".ts", ".tsx", ".mjs", ".cjs", ".css", ".scss",
    ".c", ".cc", ".cpp", ".cxx", ".h", ".hh", ".hpp", ".hxx",
    ".m", ".mm", ".metal", ".kt", ".kts", ".swift", ".cs",
    ".py", ".sh", ".bash", ".zsh", ".rb", ".pl", ".bat", ".ps1",
    ".gradle", ".cmake", ".mk", ".pro", ".toml", ".cfg", ".ini", ".properties",
    ".xml", ".json", ".jsonl", ".yml", ".yaml", ".plist", ".svg", ".html",
    ".htm", ".sql", ".graphql", ".proto", ".csv", ".md", ".adoc", ".txt",
])

ALLOWED = frozenset([0x09, 0x0A, 0x0D])

# Names for the bytes this gate rejects, so a report says what was found
# rather than just a number.
C0_NAMES = [
    "NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK", "BEL",
    "BS", "TAB", "LF", "VT", "FF", "CR", "SO", "SI",
    "DLE", "DC1", "DC2", "DC3", "DC4", "NAK", "SYN", "ETB",
    "CAN", "EM", "SUB", "ESC", "FS", "GS", "RS", "US",
]

# Whole-tree mode must never pass because it looked at nothing: a wrong working
# directory or a `git ls-files` that returns empty would otherwise report
# success. The tree holds roughly ten thousand matching files.
MIN_SCANNED = 1000


def byte_name(value):
    if value == 0x7F:
        return "DEL"
    return C0_NAMES[value]


def is_text_path(path):
    return os.path.splitext(path)[1].lower() in TEXT_EXTENSIONS


def tracked_text_files():
    out = subprocess.run(["git", "ls-files", "-z"],
                         check=True, stdout=subprocess.PIPE).stdout
    return [p.decode("utf-8", "surrogateescape")
            for p in out.split(b"\0") if p and is_text_path(p.decode("utf-8", "surrogateescape"))]


def findings_in(path):
    """Every rejected byte in one file, as (line, column, value) triples."""
    try:
        with open(path, "rb") as handle:
            data = handle.read()
    except OSError as err:
        print("%s: could not read: %s" % (path, err), file=sys.stderr)
        return None

    found = []
    line = 1
    column = 1
    for value in data:
        if value == 0x0A:
            line += 1
            column = 1
            continue
        if value < 0x20 or value == 0x7F:
            if value not in ALLOWED:
                found.append((line, column, value))
        column += 1
    return found


def main(argv):
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    os.chdir(repo_root)

    explicit = [a for a in argv[1:] if not a.startswith("-")]
    if len(explicit) != len(argv[1:]):
        print(__doc__, file=sys.stderr)
        return 2

    if explicit:
        paths = [p for p in explicit if is_text_path(p)]
        skipped = [p for p in explicit if not is_text_path(p)]
        for path in skipped:
            print("check-control-characters: not a checked text type, skipped: %s" % path)
    else:
        paths = tracked_text_files()

    scanned = 0
    failed = 0
    unreadable = 0
    for path in paths:
        if not os.path.isfile(path):
            continue
        found = findings_in(path)
        if found is None:
            unreadable += 1
            continue
        scanned += 1
        for line, column, value in found:
            print("%s:%d:%d: raw %s (0x%02x) control character in source"
                  % (path, line, column, byte_name(value), value), file=sys.stderr)
            print("  Write it as an escape (Java/JS: \\u%04x, C: \\x%02x) -- the compiled "
                  "value is identical and the file stays greppable." % (value, value),
                  file=sys.stderr)
            failed += 1

    if unreadable:
        print("check-control-characters: %d file(s) could not be read." % unreadable,
              file=sys.stderr)
        return 2

    if not explicit and scanned < MIN_SCANNED:
        print("check-control-characters: only %d file(s) scanned, expected at least %d -- "
              "the check did not actually run." % (scanned, MIN_SCANNED), file=sys.stderr)
        return 2

    if failed:
        print("check-control-characters: %d raw control character(s) found in %d file(s) scanned."
              % (failed, scanned), file=sys.stderr)
        return 1

    print("check-control-characters: %d file(s) clean." % scanned)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
