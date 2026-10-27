#!/usr/bin/env python3
"""Reject Android dependencies the builder injects whose natives are 4 KB aligned.

`check-16k-page-alignment.py` reads every *tracked* artifact. Most of the
native code a Codename One Android app ships is not tracked at all: the
builder writes Maven coordinates into the generated `build.gradle`
(`PlatformFeatureCatalog` entries, the play-services defaults) and Gradle
downloads the AARs on the build machine. CameraX 1.3.4 reached every
`CodeScanner` / `VisionCameraView` app that way with a 4 KB aligned
`libimage_processing_util_jni.so`, Play refused the upload, and the tree gate
stayed green because it never saw the file.

So this resolves those coordinates the way the generated project does --
Gradle, from `google()`, `mavenCentral()` and Huawei's repository, transitively -- and runs the same
ELF check over everything that comes back.

Each group is resolved in a configuration of its own. Resolving them together
is wrong in a way that hides exactly this bug: Gradle's conflict resolution
picks the highest requested version of a module, so one entry pinning
`barcode-scanning:17.3.0` silently upgraded another's 4 KB aligned `17.2.0`
and the old pin read as clean. An app whose classes hit only that one entry
would have shipped the old version.

Input is a text file, one dependency per line: `<group><TAB><coordinate>`.
The group names what the builder adds them for (a catalog class prefix, a
play-services hint) and is what a failure is reported under. Lines starting
with `#` are ignored. The builders' own listers write it:

  CodenameOne:  com.codename1.builders.AndroidDependencyCoordinates (build-engine tests)
  BuildDaemon:  com.codename1.build.daemon.AndroidDependencyCoordinates

Usage:
  scripts/check-android-dependency-alignment.py --coordinates FILE \
      [--gradle PATH] [--work DIR] [--verbose]

Needs network access, JDK 17+ and Gradle 8 (`--gradle`, `$GRADLE`, or `gradle`
on `PATH`). A coordinate that does not resolve is a failure, never a skip: a
pin that names nothing is a broken build waiting for an app, and an
unresolvable one is also one this gate has not read.
"""

import importlib.util
import os
import re
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))

# A run that resolves this little has not looked at the builder's real
# dependency set, whatever it prints. Both builders list well over a hundred
# coordinates, dozens of them carrying natives.
MIN_GROUPS = 20
MIN_LIBRARIES_SCANNED = 20


def load_page_alignment_gate():
    path = os.path.join(HERE, "check-16k-page-alignment.py")
    spec = importlib.util.spec_from_file_location("page_alignment_gate", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


COORDINATE = re.compile(r"^[A-Za-z0-9_.\-]+:[A-Za-z0-9_.\-]+:[A-Za-z0-9_.\-+]+(@aar)?$")


def read_groups(path):
    """Returns an ordered list of (group, [coordinate, ...])."""
    groups = {}
    order = []
    with open(path) as source:
        for number, raw in enumerate(source, 1):
            line = raw.rstrip("\n")
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            if "\t" not in line:
                raise ValueError("%s:%d: expected <group><TAB><coordinate>: %r"
                                 % (path, number, line))
            group, coordinate = [part.strip() for part in line.split("\t", 1)]
            if not COORDINATE.match(coordinate):
                raise ValueError("%s:%d: not a group:artifact:version coordinate: %r"
                                 % (path, number, coordinate))
            if group not in groups:
                groups[group] = []
                order.append(group)
            if coordinate not in groups[group]:
                groups[group].append(coordinate)
    return [(group, groups[group]) for group in order]


BUILD_SCRIPT = r"""
// The builder adds Huawei's repository for the HMS push and map kits.
repositories { google(); mavenCentral(); maven { url 'https://developer.huawei.com/repo/' } }

def groups = new File(projectDir, "groups.txt").readLines().findAll { it.trim() }
groups.eachWithIndex { line, index ->
    def parts = line.split("\t")
    def conf = configurations.create("g" + index) {
        transitive = true
        canBeConsumed = false
        attributes { attribute(Attribute.of("org.gradle.usage", String), "java-runtime") }
    }
    parts[1].split(" ").each { dependencies.add(conf.name, it) }
}

tasks.register("collect") { doLast {
    groups.eachWithIndex { line, index ->
        def out = new File(projectDir, "out/g" + index)
        out.mkdirs()
        def view = configurations.getByName("g" + index).incoming.artifactView { lenient = true }
        def artifacts = view.artifacts
        // One directory per artifact: two modules can share a file name
        // (artifactId-version.aar under different groupIds), and a flat copy
        // would let one overwrite the other unscanned.
        artifacts.eachWithIndex { a, i -> ant.copy(file: a.file, todir: new File(out, "a" + i)) }
        artifacts.failures.each { f ->
            new File(out, "FAILURES.txt") << (f.message.replace("\n", " ") + "\n")
        }
    }
}}
"""


def find_gradle(explicit):
    for candidate in (explicit, os.environ.get("GRADLE"), shutil.which("gradle")):
        if candidate:
            return candidate
    return None


def main(argv):
    verbose = "--verbose" in argv or "-v" in argv
    args = [a for a in argv if a not in ("--verbose", "-v")]
    options = {}
    while args:
        flag = args.pop(0)
        if flag not in ("--coordinates", "--gradle", "--work") or not args:
            print(__doc__, file=sys.stderr)
            return 2
        options[flag] = args.pop(0)
    if "--coordinates" not in options:
        print(__doc__, file=sys.stderr)
        return 2

    try:
        groups = read_groups(options["--coordinates"])
    except (IOError, ValueError) as error:
        print("check-android-dependency-alignment: %s" % error, file=sys.stderr)
        return 2
    if len(groups) < MIN_GROUPS:
        print("check-android-dependency-alignment: %d dependency groups, "
              "expected at least %d -- the lister produced next to nothing, "
              "which is not the same as a clean builder."
              % (len(groups), MIN_GROUPS), file=sys.stderr)
        return 2

    gradle = find_gradle(options.get("--gradle"))
    if gradle is None:
        print("check-android-dependency-alignment: no Gradle (pass --gradle, "
              "set GRADLE, or put gradle on PATH)", file=sys.stderr)
        return 2

    work = options.get("--work") or tempfile.mkdtemp(prefix="cn1-dep-align-")
    os.makedirs(work, exist_ok=True)
    with open(os.path.join(work, "settings.gradle"), "w") as f:
        f.write("rootProject.name = 'cn1-dependency-alignment'\n")
    with open(os.path.join(work, "build.gradle"), "w") as f:
        f.write(BUILD_SCRIPT)
    with open(os.path.join(work, "groups.txt"), "w") as f:
        for group, coordinates in groups:
            f.write("%s\t%s\n" % (group, " ".join(coordinates)))
    out_root = os.path.join(work, "out")
    if os.path.isdir(out_root) and os.listdir(out_root):
        print("check-android-dependency-alignment: %s is not empty; pass a "
              "fresh --work directory so an earlier run's AARs are not read "
              "as this one's" % out_root, file=sys.stderr)
        return 2

    result = subprocess.call([gradle, "-q", "--no-daemon", "-p", work, "collect"])
    if result != 0:
        print("check-android-dependency-alignment: Gradle failed (%d)" % result,
              file=sys.stderr)
        return 2

    gate = load_page_alignment_gate()
    scanner = gate.Scanner(verbose)
    unresolved = []
    for index, (group, coordinates) in enumerate(groups):
        directory = os.path.join(out_root, "g%d" % index)
        failures = os.path.join(directory, "FAILURES.txt")
        if os.path.isfile(failures):
            with open(failures) as f:
                for message in f.read().splitlines():
                    unresolved.append("%s: %s" % (group, message))
        files = []
        for root, _, names in os.walk(directory):
            for name in names:
                if os.path.join(root, name) != failures:
                    files.append(os.path.relpath(os.path.join(root, name), directory))
        files.sort()
        if not files:
            unresolved.append("%s: resolved to no artifacts (%s)"
                              % (group, " ".join(coordinates)))
        before = len(scanner.failures)
        for name in files:
            scanner.scan_path(os.path.join(directory, name))
        # Name the group on every finding: the AAR file name says which
        # library is broken, the group says which pin brought it in.
        for i in range(before, len(scanner.failures)):
            scanner.failures[i] = "[%s] %s" % (
                group, scanner.failures[i].replace(directory + os.sep, ""))
        if verbose:
            print("%s -> %d artifacts" % (group, len(files)))

    if unresolved:
        print("Android dependencies that did not resolve:", file=sys.stderr)
        for line in unresolved:
            print("  %s" % line, file=sys.stderr)
    if scanner.failures:
        print("16 KB page alignment check failed for builder-injected "
              "Android dependencies:", file=sys.stderr)
        for failure in scanner.failures:
            print("  %s" % failure, file=sys.stderr)
        print("", file=sys.stderr)
        print("Raise the pin to the first release whose 64-bit libraries are "
              "linked for 16 KB pages, including its transitive natives. The "
              "developer cannot fix this from the app.", file=sys.stderr)
    if unresolved or scanner.failures:
        return 1

    if scanner.libraries_scanned < MIN_LIBRARIES_SCANNED:
        print("check-android-dependency-alignment: only %d 64-bit native "
              "libraries in %d groups, expected at least %d -- the resolution "
              "returned something other than the builder's real dependencies."
              % (scanner.libraries_scanned, len(groups), MIN_LIBRARIES_SCANNED),
              file=sys.stderr)
        return 2

    print("check-android-dependency-alignment: %d groups, %d Android 64-bit "
          "native libraries aligned to 0x%x or more"
          % (len(groups), scanner.libraries_scanned, gate.REQUIRED_ALIGNMENT))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
