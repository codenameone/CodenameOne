#!/usr/bin/env python3
"""A builder must switch a preprocessor define through the name-aware helper, never String.replace.

The port's natives carry their optional features as commented-out defines that a builder
uncomments for the applications that need them. Related switches sit on consecutive lines
and share a prefix:

    //#define CN1_INCLUDE_CRYPTO
    //#define CN1_INCLUDE_CRYPTO_GCM

A plain `String.replace("//#define CN1_INCLUDE_CRYPTO", ...)` matches both. That is what
happened: every application using any crypto API also got CN1_INCLUDE_CRYPTO_GCM, which at
the time compiled CommonCrypto's private AES-GCM SPI, and App Store Connect rejected the
upload for non-public symbols. The ios.crypto.gcm hint that was meant to control it was
never consulted, and no build or test noticed.

`Executor.replaceMarker` (and `replaceInFile`, which uses it) treats a define marker as a
whole name. This check makes sure nothing goes around it: a `.replace`, `.replaceAll` or
`.replaceFirst` whose first argument is a `#define` literal is reported, whether or not the
name collides with anything today -- the collision appears when someone adds a define, in a
file the builder change never touched.

No baseline and no exclusions; the fix is always to call `replaceMarker(text, marker, value)`.

    scripts/check-builder-define-toggles.py                    # this repository's builders
    scripts/check-builder-define-toggles.py --root DIR ...     # another tree (the BuildDaemon)
    scripts/check-builder-define-toggles.py --self-test
"""

import argparse
import os
import re
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_ROOTS = [os.path.join(REPO_ROOT, "maven", "codenameone-maven-plugin", "src", "main", "java")]

# `.replace(` then optional whitespace (including a line break) then a string literal whose
# content is a define toggle. `\s*` is inside the literal on purpose: "// #define X" is the
# same toggle to the preprocessor.
RAW_REPLACE = re.compile(
    r'\.(replace|replaceAll|replaceFirst)\(\s*"((?://\s*)?#define\s+[A-Za-z_][A-Za-z0-9_]*)"')

SELF_TEST_BAD = '''
    str = str.replace("//#define CN1_INCLUDE_CRYPTO", "#define CN1_INCLUDE_CRYPTO");
    s = s.replaceAll(
            "#define INCLUDE_CN1_PUSH", "");
'''
SELF_TEST_GOOD = '''
    str = replaceMarker(str, "//#define CN1_INCLUDE_CRYPTO", "#define CN1_INCLUDE_CRYPTO");
    replaceInFile(f, "//#define CN1_INCLUDE_CRYPTO", "#define CN1_INCLUDE_CRYPTO");
    str = str.replace("@PLACEHOLDER@", value);
'''


def findings_in(text):
    out = []
    for match in RAW_REPLACE.finditer(text):
        line = text.count("\n", 0, match.start()) + 1
        out.append((line, match.group(1), match.group(2)))
    return out


def self_test():
    bad = findings_in(SELF_TEST_BAD)
    good = findings_in(SELF_TEST_GOOD)
    if len(bad) != 2 or good:
        sys.stderr.write("self-test failed: expected 2 findings and 0, got %d and %d\n"
                         % (len(bad), len(good)))
        return 1
    print("self-test: OK")
    return 0


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", action="append",
                    help="a source tree to scan; repeatable. Defaults to the maven plugin.")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        return self_test()

    roots = args.root or DEFAULT_ROOTS
    scanned = 0
    findings = []
    for root in roots:
        if not os.path.isdir(root):
            # A missing root would scan nothing and report success.
            sys.stderr.write("check-builder-define-toggles: no such directory: %s\n" % root)
            return 2
        for base, _dirs, files in os.walk(root):
            for name in files:
                if not name.endswith(".java"):
                    continue
                path = os.path.join(base, name)
                with open(path, "r", encoding="utf-8", errors="replace") as handle:
                    text = handle.read()
                scanned += 1
                for line, method, marker in findings_in(text):
                    findings.append((os.path.relpath(path, REPO_ROOT), line, method, marker))
    if scanned == 0:
        sys.stderr.write("check-builder-define-toggles: no Java sources under %s\n"
                         % ", ".join(roots))
        return 2
    if not findings:
        print("check-builder-define-toggles: OK (%d Java files)" % scanned)
        return 0
    print("check-builder-define-toggles: a define is switched by prefix-matching %s:\n"
          % "String.replace")
    for path, line, method, marker in findings:
        print("  %s:%d: .%s(\"%s\", ...)" % (path, line, method, marker))
    print("\nA longer define that starts with the same name is switched too. Use")
    print("replaceMarker(text, marker, value), or replaceInFile, which match the whole name.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
