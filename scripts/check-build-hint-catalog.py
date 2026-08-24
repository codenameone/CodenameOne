#!/usr/bin/env python3
"""Reports build hints the code reads that the catalog does not describe.

A hint the catalog does not know about is invisible to everything downstream:
it gets no annotation, no doc row, no entry in the Settings tool, and no
value checking. That is how `android.xPermissions` shipped in our own agent
reference for a hint the builder actually spells `android.xpermissions` --
green build, no effect, nobody noticed.

Held against a baseline rather than failing outright: a large tail of hints
predates the catalog. The point is that *new* code cannot add another one.
"""
import collections, fnmatch, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "scripts"))
BASELINE = os.path.join(ROOT, "scripts", "build-hint-catalog-baseline.txt")
COMPUTED_SITES = os.path.join(ROOT, "scripts", "build-hint-computed-sites.txt")
CATALOG_CLASSES = os.path.join(ROOT, "maven/build-hint-catalog/target/classes")


def catalog():
    """(known names, dynamic patterns) straight out of the compiled catalog."""
    src = os.path.join(ROOT, "maven/build-hint-catalog/src/main/java/com/codename1/build/shared")
    names, patterns = set(), set()
    for fn in sorted(os.listdir(src)):
        if not fn.startswith("BuildHints") or not fn.endswith(".java"):
            continue
        with open(os.path.join(src, fn), encoding="utf-8") as fh:
            text = fh.read()
        for m in re.finditer(r'new Hint\("((?:[^"\\]|\\.)*)"\)', text):
            names.add(m.group(1))
        for m in re.finditer(r'\.dynamic\("((?:[^"\\]|\\.)*)"\)', text):
            patterns.add(m.group(1))
        # BuildHintsDynamic registers through a helper; take its literals too
        for m in re.finditer(r'family\(h,\s*"((?:[^"\\]|\\.)*)"', text):
            names.add(m.group(1))
            patterns.add(m.group(1))
    return names, patterns


DOC_ROOTS = [
    "scripts/initializr/common/src/main/resources/skill",
    "maven/cn1app-archetype/src/main/resources/archetype-resources",
]


def documented_hints():
    """Every codename1.arg.* key our own docs and templates name.

    These are read by people and by coding agents, and a key that no builder
    reads is silently inert -- which is exactly how android.xPermissions,
    android.minSdkVersion and android.sdkVersion came to be recommended in the
    agent reference for hints the builder spells differently or not at all.
    """
    found = {}
    for root in DOC_ROOTS:
        base = os.path.join(ROOT, root)
        for dirpath, _, files in os.walk(base):
            for fn in files:
                if not fn.endswith((".md", ".properties", ".java", ".adoc")):
                    continue
                path = os.path.join(dirpath, fn)
                try:
                    with open(path, encoding="utf-8", errors="replace") as fh:
                        text = fh.read()
                except OSError:
                    continue
                for m in re.finditer(r'codename1\.arg\.([A-Za-z][A-Za-z0-9_.]*)', text):
                    key = m.group(1)
                    # "codename1.arg.var.<name>" is written with a placeholder suffix;
                    # keep the trailing dot so it still matches the var.* family.
                    if key.endswith("."):
                        key += "*"
                    found.setdefault(key, os.path.relpath(path, ROOT))
    return found


def main():
    write = "--write-baseline" in sys.argv
    import build_hint_miner as miner

    known, patterns = catalog()
    if not known:
        print("check-build-hint-catalog: found no catalog entries -- is the source tree intact?",
              file=sys.stderr)
        return 2

    findings = []
    for key, sites in sorted(miner.hits.items()):
        if key in known:
            continue
        if any(fnmatch.fnmatch(key, p) for p in patterns):
            continue
        rel, line = sites[0][1], sites[0][2]
        findings.append(f"{key}|{rel}:{line}")

    if write:
        with open(BASELINE, "w") as f:
            f.write(HEADER)
            for line in findings:
                f.write(line + "\n")
        print(f"check-build-hint-catalog: wrote {len(findings)} baseline entries")
        return 0

    baseline = set()
    if os.path.exists(BASELINE):
        with open(BASELINE, encoding="utf-8") as fh:
            for line in fh:
                line = line.strip()
                if line and not line.startswith("#"):
                    baseline.add(line.split("|")[0])

    current = {f.split("|")[0]: f for f in findings}
    added = sorted(set(current) - baseline)
    removed = sorted(baseline - set(current))

    if added:
        print("check-build-hint-catalog: build hints read by the code that the catalog "
              "does not describe:", file=sys.stderr)
        for key in added:
            print("  " + current[key].replace("|", "  read at "), file=sys.stderr)
        print("\nAdd each one to maven/build-hint-catalog/.../BuildHints*.java. A hint the "
              "catalog does not know about gets no annotation, no documentation and no "
              "value checking.", file=sys.stderr)
    if removed:
        print("\ncheck-build-hint-catalog: these baseline entries are now catalogued; "
              "delete them from\n  scripts/build-hint-catalog-baseline.txt", file=sys.stderr)
        for key in removed:
            print("  " + key, file=sys.stderr)
    if added or removed:
        return 1

    # Our own docs and project templates must not name a hint that does not exist.
    doc_bad = []
    for key, where in sorted(documented_hints().items()):
        if key in known:
            continue
        if any(fnmatch.fnmatch(key, p) for p in patterns):
            continue
        doc_bad.append(f"{key}  named in {where}")
    if doc_bad:
        print("check-build-hint-catalog: our own documentation names build hints that do "
              "not exist:", file=sys.stderr)
        for line in doc_bad:
            print("  " + line, file=sys.stderr)
        print("\nA hint nothing reads is silently ignored, so a reader who copies it gets a "
              "green build and no effect.", file=sys.stderr)
        return 1

    # Sites that build a hint name instead of writing it. Nothing in the tree
    # names the hint they read, so the pass above cannot see it -- which is how
    # this gate came to report "all described" while android.maps.provider and
    # ios.nativeVerify had no catalog row.
    declared = {}
    if os.path.exists(COMPUTED_SITES):
        with open(COMPUTED_SITES, encoding="utf-8") as fh:
            for line in fh:
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                # Split from both ends: the expression sits in the middle and may
                # itself contain a pipe, while the file and the expansion list
                # cannot.
                path, _, rest = line.partition("|")
                expr, _, expansions = rest.rpartition("|")
                names, _, count = expansions.partition("#")
                declared[(path, " ".join(expr.split()))] = (
                    [e for e in names.split(",") if e],
                    int(count) if count.strip() else 1)

    computed_bad = []
    mined = miner.hits_computed()
    seen_counts = collections.Counter(
        (path, " ".join(expr.split())) for expr, path, _ in mined)
    for expr, path, line_no in mined:
        key = (path, " ".join(expr.split()))
        if key not in declared:
            # Keyed by the expression as well as the file, so a builder already
            # listed cannot absorb a second computed hint for free.
            computed_bad.append(
                f"{path}:{line_no}  builds a hint name from `{expr}` and is not listed")
            continue
        for name in declared[key][0]:
            if name in known or any(fnmatch.fnmatch(name, p) for p in patterns):
                continue
            computed_bad.append(
                f"{path}:{line_no}  expands to {name}, which the catalog does not describe")
    # Two calls in one file can read the same expression -- a second
    # getArg(hintAndMarker[0], ...) over a different table -- and the key alone
    # cannot tell them apart, so the newcomer would be validated against the old
    # table's expansions and its own hints would never be checked. The declared
    # occurrence count is what notices; the accounting line has to be revisited
    # for the second call, which is the point.
    for key, expected in sorted(declared.items()):
        actual = seen_counts.get(key, 0)
        if actual != expected[1]:
            path, expr = key
            computed_bad.append(
                f"{path}  is listed once for `{expr}` covering {expected[1]} call(s), "
                f"but {actual} were mined -- list the expansions of every one and "
                f"record the count as `#{actual}`")
    if computed_bad:
        print("check-build-hint-catalog: computed hint names are unaccounted for:",
              file=sys.stderr)
        for line in sorted(set(computed_bad)):
            print("  " + line, file=sys.stderr)
        print("\nList the site in scripts/build-hint-computed-sites.txt with what it "
              "expands to, and catalogue each expansion. A hint whose name is only ever "
              "computed is invisible to every literal search, including this one.",
              file=sys.stderr)
        return 1

    print(f"check-build-hint-catalog: {len(miner.hits)} hints read, all described by the "
          f"catalog; {len(declared)} computed site(s) accounted for"
          + (f" ({len(baseline)} baselined)" if baseline else ""))
    return 0


HEADER = """# Build hints read by a builder or a mojo that the catalog does not describe,
# as of the day this gate was added.
#
# This is a ratchet, not an allow-list: new code must not add entries. Delete a
# line when the hint is added to maven/build-hint-catalog. Regenerate with
#   scripts/check-build-hint-catalog.sh --write-baseline
#
# Format: <hint-name>|<file>:<line>
"""

if __name__ == "__main__":
    sys.exit(main())
