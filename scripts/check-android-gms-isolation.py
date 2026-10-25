#!/usr/bin/env python3
"""Keep Google Play services behind the Android port's guarded proxies.

A Codename One application runs on Android builds without Google's apps --
GrapheneOS, CalyxOS, /e/OS, LineageOS without GApps, Huawei and Amazon devices --
and on GrapheneOS where Play services is an ordinary app the user can remove.
The Play services client libraries are still inside the APK there, so nothing
fails to load. What fails is the service behind them: a GoogleApiClient never
connects (the port used to spin on that forever), a Task completes with
API_UNAVAILABLE, and a connection resolution shows the "Get Google Play
services" prompt.

So every call into Play services, the Play Store's Play Core APIs or Firebase
has to sit behind a check of
com.codename1.impl.android.GoogleServices.isPlayServicesAvailable (or
isPlayStoreInstalled), with a fallback or an honest "unsupported" when it
answers false. This gate holds the port to that structurally: a file in
Ports/Android/src that names a Google Play type -- an import, a qualified name
or a Class.forName string -- must be listed in
scripts/android-gms-isolation-allowlist.txt, whose entry says how that file is
guarded. Anywhere else, the reference has to move into a listed proxy.

A bare package name ("com.google.android.gms" as an Intent package or a
PackageManager lookup) is not a type reference and is not reported. Comments
are ignored, string literals are not: a type reached by reflection fails the
same way on a device without the service.

The allow-list is exact, not a baseline: an entry whose file no longer names a
Google Play type, or no longer exists, fails too, so the list always describes
the port as it is.

Usage:
  scripts/check-android-gms-isolation.py
  scripts/check-android-gms-isolation.py --root PATH   # scan another tree (tests)
"""

import argparse
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE_ROOT = "Ports/Android/src"
ALLOWLIST = "scripts/android-gms-isolation-allowlist.txt"

# A qualified name under one of the Google Play namespaces with at least one
# segment that starts with an upper-case letter: a class, not a package.
TYPE_REFERENCE = re.compile(
    r"\bcom\.google\.(?:android\.gms|android\.play|firebase)"
    r"(?:\.[a-z_][A-Za-z0-9_]*)*\.[A-Z][A-Za-z0-9_]*")

# What reaches a type without spelling its name: a wildcard import, and a string
# that only begins a qualified name (Class.forName("com.google.firebase." + x)).
# A bare package string with no trailing dot -- an Intent package, a
# PackageManager lookup -- matches neither.
INDIRECT_REFERENCE = re.compile(
    r"\bimport\s+(?:static\s+)?com\.google\.(?:android\.gms|android\.play|firebase)"
    r"(?:\.[A-Za-z_][A-Za-z0-9_]*)*\.\*"
    r"|\"com\.google\.(?:android\.gms|android\.play|firebase)(?:\.[a-z_][A-Za-z0-9_]*)*\.\"")


def strip_comments(text):
    """Remove // and /* */ comments, keeping string and char literals intact."""
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"' or c == "'":
            quote = c
            j = i + 1
            while j < n and text[j] != quote and text[j] != "\n":
                j += 2 if text[j] == "\\" else 1
            out.append(text[i:j + 1])
            i = j + 1
        elif text.startswith("//", i):
            j = text.find("\n", i)
            i = n if j < 0 else j
        elif text.startswith("/*", i):
            j = text.find("*/", i + 2)
            # Keep the newlines so reported line numbers stay right.
            chunk = text[i:n if j < 0 else j + 2]
            out.append("\n" * chunk.count("\n"))
            i = n if j < 0 else j + 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def read_allowlist(path):
    entries = {}
    with open(path, encoding="utf-8") as f:
        for number, raw in enumerate(f, 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split(None, 1)
            if len(parts) < 2:
                sys.exit(f"{path}:{number}: an entry needs a path and the reason "
                         "the file is safe without Play services")
            entries[parts[0]] = number
    return entries


def allowed(rel, entries):
    for entry in entries:
        if rel == entry or (entry.endswith("/") and rel.startswith(entry)):
            return entry
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=REPO)
    args = parser.parse_args()
    root = os.path.abspath(args.root)
    entries = read_allowlist(os.path.join(root, ALLOWLIST))

    findings = []
    used = set()
    source_root = os.path.join(root, SOURCE_ROOT)
    for directory, _, files in os.walk(source_root):
        for name in sorted(files):
            if not name.endswith(".java"):
                continue
            path = os.path.join(directory, name)
            rel = os.path.relpath(path, root).replace(os.sep, "/")
            with open(path, encoding="utf-8", errors="replace") as f:
                code = strip_comments(f.read())
            hits = []
            for number, line in enumerate(code.split("\n"), 1):
                match = TYPE_REFERENCE.search(line) or INDIRECT_REFERENCE.search(line)
                if match:
                    hits.append((number, match.group(0)))
            if not hits:
                continue
            entry = allowed(rel, entries)
            if entry:
                used.add(entry)
                continue
            for number, symbol in hits:
                findings.append(f"{rel}:{number}: names {symbol}")

    stale = sorted(e for e in entries if e not in used)
    if findings:
        print("Google Play types named outside a guarded proxy:")
        for finding in findings:
            print("  " + finding)
        print("\nMove the call into a class listed in " + ALLOWLIST + " (usually "
              "com.codename1.impl.android.GoogleServices) and check "
              "isPlayServicesAvailable before making it. A new proxy needs an entry "
              "saying how it is guarded.")
    if stale:
        print("Allow-list entries that name no Google Play type any more:")
        for entry in stale:
            print(f"  {ALLOWLIST}:{entries[entry]}: {entry}")
        print("\nDelete them; the list must describe the port as it is.")
    if findings or stale:
        return 1
    print(f"OK: Google Play types appear only in the {len(used)} guarded "
          "proxies of " + ALLOWLIST)
    return 0


if __name__ == "__main__":
    sys.exit(main())
