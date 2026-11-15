#!/usr/bin/env python3
"""Which hellocodenameone screenshot tests have no golden on a leg that runs them?

Usage: cn1ss-golden-plan.py [--verbose] [--from-git] [--pairs]

Prints the simple class names of the screenshot tests that lack a golden in at
least one golden directory whose port runs them, comma separated, or nothing.
--pairs prints one "Test<TAB>golden/dir" line per gap instead. Exits 2 when the
declarations below disagree with the tree, so a wrong one fails the PR rather
than starting or suppressing a capture.

Golden capture (.github/workflows/golden-capture.yml) starts when it prints
anything, and every screenshot workflow's first job (golden-plan.yml) skips the
suite. Reading the source rather than a run is what lets it decide before any
runner is allocated:

  * The suite is DEFAULT_TEST_CLASSES plus every class passed to
    Cn1ssDeviceRunner.addTest (KotlinUiTest arrives that way).
  * A test takes screenshots when docs/website/data/port_status.json maps an
    image to it, it is named *ScreenshotTest, or its class chain below BaseTest
    calls createForm / captureWhenSettled / emitCurrentFormScreenshot /
    emitImage / screenshotName(); unless the nearest shouldTakeScreenshot()
    override is a plain `return false`.
  * An image belongs to the test port_status.json's screenshot_mappings name
    (the port-status contract requires every golden to be mapped). A test with
    no mapping yet -- a new one -- is matched by name against the images no
    mapping claims: the class name with or without "ScreenshotTest", "Test" or
    "Screenshot", or a string literal in its class chain, optionally followed
    by a "_", "-" or "." suffix.
  * Every golden directory is checked on its own. A test that captures nothing
    on some ports says so in its class, or the base class its guard lives in:

        // cn1ss-skip: macos, watchos

    using port_status.json's port ids (ios-metal covers both iOS Metal
    directories, windows-x64 and windows-arm64 share one). A test whose chain
    has a skip guard -- a "status=SKIPPED" line, a conditional
    shouldTakeScreenshot(), or an `if (...) { done(); return true; }` early
    exit -- must carry a declaration (`cn1ss-skip: none` when the guard never
    fires on a CI leg). A declaration is checked both ways against the tree:
    a declared port with a golden, or a guard with no declaration, exits 2.

The test of this script is that the current tree reports nothing; see
test_cn1ss_golden_plan.py.
"""
import fnmatch
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCES = os.path.join(ROOT, "scripts/hellocodenameone/common/src/main")
RUNNER = os.path.join(SOURCES, "java/com/codenameone/examples/hellocodenameone/tests/Cn1ssDeviceRunner.java")

MANIFEST = os.path.join(ROOT, "docs/website/data/port_status.json")

# Every directory a screenshot leg compares against, and the port_status.json
# port ids whose runs it holds. Keep in step with the SCREENSHOT_REF_DIR / REF
# values in the screenshot workflows.
DIR_PORTS = {
    "scripts/ios/screenshots-metal": {"ios-metal"},
    "scripts/ios/screenshots-metal-27": {"ios-metal"},
    "scripts/ios/screenshots-tv": {"tvos"},
    "scripts/ios/screenshots-watch": {"watchos"},
    "scripts/android/screenshots": {"android"},
    "scripts/javascript/screenshots": {"javascript"},
    "scripts/linux/screenshots": {"linux-x64"},
    "scripts/linux/screenshots-arm": {"linux-arm64"},
    "scripts/windows/screenshots": {"windows-x64", "windows-arm64"},
    "scripts/macos/screenshots": {"macos"},
    "scripts/mac-catalyst/screenshots": {"mac-catalyst"},
}
PORTS = set().union(*DIR_PORTS.values())
GOLDEN_DIRS = [
    "scripts/ios/screenshots-metal",
    "scripts/ios/screenshots-metal-27",
    "scripts/ios/screenshots-tv",
    "scripts/ios/screenshots-watch",
    "scripts/android/screenshots",
    "scripts/javascript/screenshots",
    "scripts/linux/screenshots",
    "scripts/linux/screenshots-arm",
    "scripts/windows/screenshots",
    "scripts/macos/screenshots",
    "scripts/mac-catalyst/screenshots",
]

CAPTURE_CALL = re.compile(r"\b(createForm|captureWhenSettled|emitCurrentFormScreenshot|emitImage)\s*\("
                          r"|\bscreenshotName\s*\(")
JAVA_CLASS = re.compile(r"\bclass\s+(\w+)\s*(?:<[^>{]*>)?\s*(?:extends\s+([\w.]+))?")
KOTLIN_CLASS = re.compile(r"\bclass\s+(\w+)\s*(?:\([^)]*\))?\s*(?::\s*([\w.]+))?")
ADD_TEST = re.compile(r"\baddTest\s*\(\s*(?:new\s+)?([A-Za-z_][\w.]*)\s*\(")
LITERAL = re.compile(r'"([A-Za-z0-9_.\-]{3,})"')
SUFFIXES = ("ScreenshotTest", "Screenshot", "Test")


SKIP_DECL = re.compile(r"cn1ss-skip:\s*([a-z0-9][a-z0-9, -]*?)\s*(?:--|$)", re.M)
SKIP_GUARD = re.compile(r"status=SKIPPED"
                        r"|shouldTakeScreenshot\s*\(\s*\)\s*(?::\s*Boolean\s*)?\{\s*return\s+(?!true\s*;|false\s*;)"
                        r"|\bif\s*\([^{;]*\)\s*\{\s*(?:[^{};]*;\s*)?done\(\)\s*;\s*return\s+true\s*;\s*\}")


class PlanError(Exception):
    pass


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def suite_tests(source, extra=()):
    """DEFAULT_TEST_CLASSES, then every class handed to Cn1ssDeviceRunner.addTest."""
    names = list(_default_tests(source))
    for name in extra:
        if name not in names:
            names.append(name)
    return names


def _default_tests(source):
    start = source.find("DEFAULT_TEST_CLASSES = new BaseTest[]{")
    if start < 0:
        return []
    end = source.find("};", start)
    body = strip_comments(source[start:end if end >= 0 else len(source)])
    names = []
    for name in re.findall(r"\bnew\s+([A-Za-z_][\w.]*)\s*\(", body):
        name = name.rsplit(".", 1)[-1]
        if name != "BaseTest" and name not in names:
            names.append(name)
    return names


def index_sources(sources_dir):
    """Top-level class name -> (superclass simple name, comment-free source),
    over the app's Java and Kotlin sources, plus the addTest registrations."""
    classes = {}
    added = []
    for d, _, files in os.walk(sources_dir):
        for f in files:
            if f.endswith(".java"):
                decl = JAVA_CLASS
            elif f.endswith(".kt"):
                decl = KOTLIN_CLASS
            else:
                continue
            with open(os.path.join(d, f), encoding="utf-8", errors="replace") as fh:
                raw = fh.read()
            src = strip_comments(raw)
            for name in ADD_TEST.findall(src):
                name = name.rsplit(".", 1)[-1]
                if name not in added:
                    added.append(name)
            name = f.rsplit(".", 1)[0]
            m = None
            for cand in decl.finditer(src):
                if cand.group(1) == name:
                    m = cand
                    break
            sup = m.group(2).rsplit(".", 1)[-1] if m and m.group(2) else None
            classes[name] = (sup, src, SKIP_DECL.findall(raw))
    return classes, added


def chain(name, classes):
    seen = []
    while name and name in classes and name != "BaseTest" and name not in seen:
        seen.append(name)
        name = classes[name][0]
    return seen


SHOULD_TAKE = re.compile(r"\bshouldTakeScreenshot\s*\(\s*\)\s*(?::\s*Boolean\s*)?\{\s*return\s+([^;}]+?)\s*;?\s*\}")


def opted_out(name, classes):
    """The nearest shouldTakeScreenshot() override in the chain is a plain
    `return false`. A conditional one (`return !isHtml5()`) is a per-platform
    skip, which the any-directory rule already covers, so it does not count."""
    for c in chain(name, classes):
        m = SHOULD_TAKE.search(classes[c][1])
        if m:
            return m.group(1).strip() == "false"
    return False


def takes_screenshots(name, classes):
    if opted_out(name, classes):
        return False
    # The suite's naming convention says so outright; the calls catch the
    # rest (the graphics, chart and window tests are not named that way).
    if name.endswith("ScreenshotTest"):
        return True
    return any(CAPTURE_CALL.search(classes[c][1]) for c in chain(name, classes))


def candidate_names(name, classes):
    names = {name}
    for suffix in SUFFIXES:
        if name.endswith(suffix) and len(name) > len(suffix):
            names.add(name[:-len(suffix)])
    # The image name may be a literal in a base class the test inherits.
    for c in chain(name, classes):
        names.update(LITERAL.findall(classes[c][1]))
    return names


def golden_stems_from_git(root, d):
    import subprocess
    out = subprocess.run(["git", "-C", root, "ls-tree", "--name-only", "HEAD", "--", d + "/"],
                         check=True, capture_output=True, text=True).stdout
    return {os.path.basename(l)[:-4] for l in out.splitlines() if l.endswith(".png")}


def golden_stems(root, d):
    path = os.path.join(root, d)
    if not os.path.isdir(path):
        return set()
    return {f[:-4] for f in os.listdir(path) if f.endswith(".png")}


def covered(names, stems):
    for stem in stems:
        for n in names:
            if stem == n or (stem.startswith(n) and stem[len(n)] in "_-."):
                return True
    return False


def load_mappings(path=MANIFEST):
    if not os.path.isfile(path):
        return []
    with open(path, encoding="utf-8") as fh:
        return [(m["pattern"], m["test"]) for m in json.load(fh).get("screenshot_mappings", [])
                if m.get("pattern") and m.get("test")]


def owns(test, stems, mappings, classes):
    """Does any of these golden stems belong to test?"""
    patterns = [pat for pat, t in mappings if t == test]
    if patterns:
        return any(fnmatch.fnmatchcase(stem, pat) for stem in stems for pat in patterns)
    # Unmapped (a new test): match by name, but never an image the contract
    # already gives to another test -- a new FooTest is not covered by an older
    # test's Foo_dark.png.
    free = {stem for stem in stems if not any(fnmatch.fnmatchcase(stem, pat) for pat, _ in mappings)}
    return covered(candidate_names(test, classes), free)


def declared_skips(test, classes):
    """Union of the cn1ss-skip declarations in the class chain; None if none."""
    found = None
    for c in chain(test, classes):
        for decl in classes[c][2]:
            ports = {x.strip() for x in decl.split(",") if x.strip()}
            bad = ports - PORTS - {"none"}
            if bad:
                raise PlanError("%s declares unknown ports %s (known: %s)"
                                % (c, ", ".join(sorted(bad)), ", ".join(sorted(PORTS))))
            found = (found or set()) | (ports - {"none"})
    return found


def plan(root=ROOT, sources_dir=SOURCES, runner=RUNNER, dir_ports=DIR_PORTS,
         manifest=MANIFEST, from_git=False):
    """(gaps, problems): gaps are (test, dir) pairs with no golden on a leg that
    runs the test; problems are declarations the tree contradicts."""
    classes, added = index_sources(sources_dir)
    with open(runner, encoding="utf-8", errors="replace") as fh:
        suite = suite_tests(fh.read(), added)
    mappings = load_mappings(manifest)
    mapped = {t for _, t in mappings}
    stems = {d: (golden_stems_from_git(root, d) if from_git else golden_stems(root, d)) for d in dir_ports}
    if not any(stems.values()):
        # An empty set means the listing failed, not that nothing is captured.
        raise PlanError("no goldens found under %s" % ", ".join(dir_ports))
    gaps, problems = [], []
    for t in suite:
        if t not in classes or opted_out(t, classes):
            continue
        if not (t in mapped or takes_screenshots(t, classes)):
            continue
        skips = declared_skips(t, classes)
        guarded = [c for c in chain(t, classes) if SKIP_GUARD.search(classes[c][1])]
        if guarded and skips is None:
            problems.append("%s skips on some platforms (%s) but declares no `// cn1ss-skip: <ports>`"
                            % (t, ", ".join(guarded)))
            continue
        for d, ports in dir_ports.items():
            has = owns(t, stems[d], mappings, classes)
            runs = not ports <= (skips or set())
            if runs and not has:
                gaps.append((t, d))
            elif has and not runs:
                problems.append("%s declares cn1ss-skip for %s but %s has its golden"
                                % (t, ", ".join(sorted(ports)), d))
    return gaps, problems


def missing(**kw):
    gaps, _ = plan(**kw)
    out = []
    for t, _ in gaps:
        if t not in out:
            out.append(t)
    return out


def main(argv):
    try:
        gaps, problems = plan(from_git="--from-git" in argv)
    except PlanError as e:
        print("::error::cn1ss-golden-plan: %s" % e)
        return 2
    for p in problems:
        print("::error::cn1ss-golden-plan: %s" % p, file=sys.stderr)
    if problems:
        return 2
    if "--verbose" in argv:
        print("golden gaps: %d" % len(gaps), file=sys.stderr)
        for t, d in gaps:
            print("  %s has no golden in %s" % (t, d), file=sys.stderr)
    if "--pairs" in argv:
        for t, d in gaps:
            print("%s\t%s" % (t, d))
    else:
        tests = []
        for t, _ in gaps:
            if t not in tests:
                tests.append(t)
        print(",".join(tests))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
