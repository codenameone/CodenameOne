#!/usr/bin/env python3
"""Does the hellocodenameone suite have a screenshot test with no golden yet?

Usage: cn1ss-golden-plan.py [--verbose] [--from-git]

--from-git lists the golden directories from HEAD's tree instead of the disk,
so a partial (blobless, sparse) checkout that holds only the sources is enough.

Prints the simple class names of screenshot tests in Cn1ssDeviceRunner's
DEFAULT_TEST_CLASSES that no golden directory has an image for, comma
separated, or nothing. A golden capture run (.github/workflows/golden-capture.yml)
starts when it prints anything.

This reads the source, not a run, so it decides before any runner is
allocated. It follows the suite's naming conventions:

  * The suite is DEFAULT_TEST_CLASSES plus every class passed to
    Cn1ssDeviceRunner.addTest (KotlinUiTest arrives that way).
  * A test takes screenshots when it is named *ScreenshotTest, or its class,
    or a superclass in the app's sources below BaseTest, calls createForm /
    captureWhenSettled / emitCurrentFormScreenshot / emitImage or uses
    screenshotName(). Assertion-only tests do none of that.
  * Its images are named after the class (with or without a trailing
    "ScreenshotTest", "Test" or "Screenshot"), or after a string literal in its
    own source (createForm's image name, screenshotName()'s value, an explicit
    emit). Images may carry a suffix ("_dark", "_light", "-round", ...).
  * A test is covered when ANY golden directory has such an image. A test
    that some platforms skip on purpose (watch-only, JavaSE-only, a port that
    lacks the feature) has goldens only where it runs, and the tree's golden
    set is complete, so presence anywhere is the reliable signal and absence
    everywhere is the only thing that means "never captured".

Two limits, both deliberate:

  * Presence anywhere means an author who commits a new test's golden for ONE
    platform (from a local run) turns capture off for it; the other legs then
    fail on the PR as they always did. Per-directory checking would need a
    hand-kept table of which tests each port skips, which is exactly the
    knowledge the source does not state.
  * A stripped name matches with a separator after it, so a new FooTest is
    "covered" by an older Foo_dark.png of a different test. Two tests that
    share a stem would also share golden files at run time, so that clash is
    already a bug of its own.

The test of this script is that the current tree reports nothing; see
test_cn1ss_golden_plan.py.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCES = os.path.join(ROOT, "scripts/hellocodenameone/common/src/main")
RUNNER = os.path.join(SOURCES, "java/com/codenameone/examples/hellocodenameone/tests/Cn1ssDeviceRunner.java")

# Every directory a screenshot leg compares against. Keep in step with the
# SCREENSHOT_REF_DIR / REF values in the screenshot workflows.
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
                src = strip_comments(fh.read())
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
            classes[name] = (sup, src)
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
    if name in classes:
        names.update(LITERAL.findall(classes[name][1]))
    return names


def golden_stems_from_git(root, dirs):
    import subprocess
    out = subprocess.run(["git", "-C", root, "ls-tree", "-r", "--name-only", "HEAD", "--"] + list(dirs),
                         check=True, capture_output=True, text=True).stdout
    return {os.path.basename(l)[:-4] for l in out.splitlines()
            if l.endswith(".png") and os.path.dirname(l) in dirs}


def golden_stems(root, dirs):
    stems = set()
    for d in dirs:
        path = os.path.join(root, d)
        if os.path.isdir(path):
            stems.update(f[:-4] for f in os.listdir(path) if f.endswith(".png"))
    return stems


def covered(names, stems):
    for stem in stems:
        for n in names:
            if stem == n or (stem.startswith(n) and stem[len(n)] in "_-."):
                return True
    return False


def missing(root=ROOT, sources_dir=SOURCES, runner=RUNNER, dirs=GOLDEN_DIRS, from_git=False):
    classes, added = index_sources(sources_dir)
    with open(runner, encoding="utf-8", errors="replace") as fh:
        suite = suite_tests(fh.read(), added)
    stems = golden_stems_from_git(root, dirs) if from_git else golden_stems(root, dirs)
    if not stems:
        # An empty set means the listing failed, not that nothing is captured.
        raise SystemExit("cn1ss-golden-plan: no goldens found under %s" % ", ".join(dirs))
    return [t for t in suite
            if t in classes and takes_screenshots(t, classes)
            and not covered(candidate_names(t, classes), stems)]


def main(argv):
    result = missing(from_git="--from-git" in argv)
    if "--verbose" in argv:
        print("tests without any golden: %d" % len(result), file=sys.stderr)
    print(",".join(result))


if __name__ == "__main__":
    main(sys.argv)
