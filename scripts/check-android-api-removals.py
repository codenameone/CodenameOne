#!/usr/bin/env python3
"""Compile the Android port against a newer platform and report what broke.

Android removes API. Codename One issue #5701 was the first time it cost us:
API 37 deleted `android.hardware.fingerprint.FingerprintManager` and
`Context.FINGERPRINT_SERVICE`, the port named both from files every generated
application compiles, and an unmodified Hello World failed
`:app:compileDebugJavaWithJavac` in sources the developer never wrote.

Nothing in the tree could have caught that. The port's own Maven build
compiles against the cn1-binaries `android.jar`, which is old enough to still
*contain* the removed class, and the only place port sources ever meet a
current platform is the generated Gradle project -- which CI pins to a fixed
compile SDK for reproducibility. So the port could regress against a new
platform and every check stayed green.

This closes that. It compiles the port twice from the same sources, against
two platform jars, and reports the errors that appear only against the newer
one. The comparison is what makes it usable without a perfect classpath: the
port deliberately excludes its optional packages (ai, ar, cipher, nearby) from
the module build, so their dependencies are absent and they cannot compile
here at all -- but they fail *identically* against both jars, so their noise
cancels and a real removal still stands out. Error lines are compared whole,
line numbers included, which is exact because the two runs see byte-identical
source.

    scripts/check-android-api-removals.py
    scripts/check-android-api-removals.py --api 37 --baseline 36
    scripts/check-android-api-removals.py --require-all   # what CI runs

With no platform new enough installed the check reports that it did not run
and succeeds, so a partial local SDK still gives a useful answer. CI passes
--require-all, where "did not run" is a failure -- a check that is satisfied
by having nothing to check is not a check.
"""

import argparse
import collections
import os
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ElementTree

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# The API level this gate exists for. Anything installed at or above it is a
# candidate target; below it there is nothing new to say, because the port is
# built and tested against those platforms elsewhere.
MIN_TARGET_API = 37

DEFAULT_SOURCE_ROOTS = [os.path.join('Ports', 'Android', 'src')]

ERROR_LINE = re.compile(r'^(.*?):(\d+): error: (.*)$')

# Everything that ends an error's continuation lines. javac follows a header
# with the source line, a caret, and indented "symbol:"/"location:" detail --
# but it also emits notes and a trailing count, and those belong to nobody.
# Left attached they ride on whichever error happened to be last, and the
# count ("576 errors") differs between the two runs by construction, so the
# last diagnostic always looked new. Note the capital N: javac writes
# "Note: Some input files ..." at column 0, which a lowercase-only pattern
# missed.
STOP_LINE = re.compile(
    r'^(?:\d+\s+(?:error|warning)s?\s*$'
    r'|[Nn]ote:\s'
    r'|[Ww]arning:\s'
    r'|(?:.*?):\d+:\s(?:warning|note):\s)')


def sdk_root():
    """The Android SDK to read platforms out of."""
    for candidate in (os.environ.get('ANDROID_HOME'),
                      os.environ.get('ANDROID_SDK_ROOT'),
                      os.path.expanduser('~/Library/Android/sdk'),
                      os.path.expanduser('~/Android/Sdk')):
        if candidate and os.path.isdir(os.path.join(candidate, 'platforms')):
            return candidate
    return None


def platform_api_level(platform_dir):
    """The API level a platform directory holds, as an int, or None.

    Read from source.properties rather than the directory name because from
    API 37 the name carries a minor -- there is no `platforms;android-37`,
    only android-37.0, android-37.1 and android-37.2 -- and the minor selects
    a revision of the level, not a level of its own. A preview platform
    (`android-Baklava`) has no number and is skipped: its API is not final,
    so a removal seen there is not yet a fact about a shipped release.
    """
    properties = os.path.join(platform_dir, 'source.properties')
    if os.path.isfile(properties):
        with open(properties, encoding='utf-8', errors='replace') as handle:
            for line in handle:
                if line.startswith('AndroidVersion.ApiLevel='):
                    value = line.split('=', 1)[1].strip().split('.')[0]
                    if value.isdigit():
                        return int(value)
    name = os.path.basename(platform_dir)
    if name.startswith('android-'):
        value = name[len('android-'):].split('.')[0]
        if value.isdigit():
            return int(value)
    return None


def platform_minor(platform_dir):
    """The minor of a platform's API level, as an int, 0 when it has none.

    Sorted numerically rather than as text on purpose: "37.10" is newer than
    "37.2" and sorts before it as a string, which would pick the older jar the
    first time a level reaches ten revisions.
    """
    name = os.path.basename(platform_dir)
    _, _, suffix = name.partition('.')
    return int(suffix) if suffix.isdigit() else 0


def installed_platforms(root):
    """Every usable platform, as a list of (api_level, path), newest first.

    A platform with no android.jar is not usable and is left out rather than
    reported: it is an interrupted download, not something this gate has an
    opinion about.
    """
    found = []
    platforms_dir = os.path.join(root, 'platforms')
    for name in sorted(os.listdir(platforms_dir)):
        path = os.path.join(platforms_dir, name)
        if not os.path.isfile(os.path.join(path, 'android.jar')):
            continue
        api = platform_api_level(path)
        if api is not None:
            found.append((api, path))
    return sorted(found, key=lambda entry: (entry[0], platform_minor(entry[1])),
                  reverse=True)


def pick(platforms, api):
    """The newest installed revision of exactly `api`."""
    for level, path in platforms:
        if level == api:
            return path
    return None


def removed_at(platform_dir, api):
    """What the platform's own api-versions.xml says it removed at `api`.

    Diagnostics only -- the compile above is what decides. Worth printing
    because a "cannot find symbol" against a platform jar says nothing about
    why the symbol went away, and this names the whole set in one place.

    Note the file records a removal against the minor it happened in ("37.0",
    "37.1"), never the bare major, which is why this matches on the prefix.
    """
    path = os.path.join(platform_dir, 'data', 'api-versions.xml')
    if not os.path.isfile(path):
        return []
    try:
        root = ElementTree.parse(path).getroot()
    except ElementTree.ParseError:
        return []
    prefix = str(api)
    removals = []
    for element in root.iter('class'):
        name = (element.get('name') or '').replace('/', '.')
        if (element.get('removed') or '').split('.')[0] == prefix:
            removals.append('class %s' % name)
        for kind in ('method', 'field'):
            for member in element.findall(kind):
                if (member.get('removed') or '').split('.')[0] == prefix:
                    removals.append('%s %s.%s'
                                    % (kind, name, member.get('name')))
    return removals


def javac():
    """The compiler to use, preferring the JDK the Android port needs."""
    for home in (os.environ.get('JAVA17_HOME'), os.environ.get('JDK_HOME'),
                 os.environ.get('JAVA_HOME')):
        if home:
            candidate = os.path.join(home, 'bin', 'javac')
            if os.access(candidate, os.X_OK):
                return candidate
    for directory in os.environ.get('PATH', '').split(os.pathsep):
        candidate = os.path.join(directory, 'javac')
        if os.access(candidate, os.X_OK):
            return candidate
    return None


def sources(roots):
    found = []
    for root in roots:
        absolute = root if os.path.isabs(root) else os.path.join(REPO, root)
        for directory, _, names in os.walk(absolute):
            for name in names:
                if name.endswith('.java'):
                    found.append(os.path.join(directory, name))
    return sorted(found)


def without_platform_stubs(entries):
    """Drop any second android.jar from a classpath.

    The port's Maven compile classpath carries the cn1-binaries android.jar,
    and that jar is old enough to still contain what newer platforms removed.
    Left in, javac resolves the platform jar first, misses the removed class,
    falls through to the stub and finds it -- so the target compile succeeds
    on exactly the symbol this gate exists to catch. Verified: with the stub
    present a probe naming FingerprintManager reported only the removed field
    and not the removed class.

    Matching on the file name is exact rather than lucky. A platform stub is
    always android.jar, in the SDK and in cn1-binaries alike, and no library
    ships under that name.
    """
    return [entry for entry in entries
            if os.path.basename(entry) != 'android.jar']


class CompilerFailure(Exception):
    """javac did not run far enough to have an opinion about the source."""


def parse_diagnostics(output):
    """Every error javac reported, each as one whole multi-line string.

    The header line alone is not enough to tell two errors apart. javac writes
    "cannot find symbol" and then, on following lines, the symbol and the
    location that could not be found -- and those detail lines are the only
    thing distinguishing one missing symbol from another on the same source
    line. Comparing headers, a line that already fails against the baseline
    (an optional package whose dependency is absent) would absorb a NEW
    removal on that same line and the gate would report nothing.

    Returned as a list rather than a set for the same reason at a smaller
    scale: one source line can carry two missing symbols, and collapsing them
    hides the second.
    """
    diagnostics = []
    current = None
    for line in output.splitlines():
        if ERROR_LINE.match(line):
            if current is not None:
                diagnostics.append('\n'.join(current))
            current = [line.rstrip()]
        elif current is not None:
            if STOP_LINE.match(line):
                diagnostics.append('\n'.join(current))
                current = None
            else:
                current.append(line.rstrip())
    if current is not None:
        diagnostics.append('\n'.join(current))
    return diagnostics


def compile_against(compiler, android_jar, classpath, source_files, workdir):
    """Compile everything and return every error javac reported.

    -Xmaxerrs is raised because the default of 100 truncates, and a truncated
    run against one jar and not the other invents a difference that is really
    just where javac stopped counting.

    A javac that exits non-zero having reported nothing did not compile the
    source -- it rejected an option, could not read the platform jar, or was
    killed. That is not "no errors", and silently reading it as one is how a
    broken toolchain satisfies this gate: both sides come back empty, the
    difference is empty, and the check reports OK.
    """
    output = os.path.join(workdir, 'classes')
    os.makedirs(output, exist_ok=True)
    argfile = os.path.join(workdir, 'sources.txt')
    with open(argfile, 'w', encoding='utf-8') as handle:
        handle.write('\n'.join(source_files))
    entries = [android_jar] + without_platform_stubs(
        [item for item in classpath if item])
    command = [compiler, '-nowarn', '-proc:none', '-Xmaxerrs', '100000',
               '-d', output, '-cp', os.pathsep.join(entries), '@' + argfile]
    result = subprocess.run(command, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, universal_newlines=True)
    diagnostics = parse_diagnostics(result.stdout)
    if result.returncode != 0 and not diagnostics:
        raise CompilerFailure(
            'javac exited %d against %s without reporting a single source '
            'diagnostic, so it never compiled anything:\n%s'
            % (result.returncode, android_jar, result.stdout.strip()[:4000]))
    return diagnostics


def auto_classpath():
    """A best-effort classpath, so the check is useful with no arguments.

    Incompleteness here is survivable by design: whatever fails to resolve
    fails against both jars and cancels out of the comparison. It is only
    worth supplying a real classpath (--classpath, as CI does from Maven) to
    keep the raw error counts small enough to read.
    """
    entries = []
    for candidate in (os.environ.get('CN1_BINARIES'),
                      os.path.join(REPO, 'maven', 'target', 'cn1-binaries'),
                      os.path.join(os.path.dirname(REPO), 'cn1-binaries')):
        android = os.path.join(candidate, 'android') if candidate else None
        if android and os.path.isdir(android):
            for name in sorted(os.listdir(android)):
                if name.endswith('.jar') and name != 'android.jar':
                    entries.append(os.path.join(android, name))
            break
    core = os.path.join(REPO, 'maven', 'core', 'target', 'classes')
    if os.path.isdir(core):
        entries.append(core)
    return entries


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--api', type=int,
                        help='target API level (default: newest installed at '
                             'or above %d)' % MIN_TARGET_API)
    parser.add_argument('--baseline', type=int,
                        help='API level to compare against (default: newest '
                             'installed below the target)')
    parser.add_argument('--classpath', default='',
                        help='extra classpath entries, %r separated'
                             % os.pathsep)
    parser.add_argument('--source-root', action='append', default=[],
                        metavar='DIR',
                        help='source tree to compile (repeatable; default: %s)'
                             % ', '.join(DEFAULT_SOURCE_ROOTS))
    parser.add_argument('--require-all', action='store_true',
                        help='treat "could not run" as a failure, which is '
                             'what CI wants')
    arguments = parser.parse_args()

    def cannot_run(reason):
        print('check-android-api-removals: did not run: %s' % reason)
        if arguments.require_all:
            print('  --require-all was passed, so this is a failure.')
            return 1
        return 0

    root = sdk_root()
    if root is None:
        return cannot_run('no Android SDK (set ANDROID_HOME)')

    platforms = installed_platforms(root)
    if not platforms:
        return cannot_run('no platform with an android.jar under %s' % root)

    target_api = arguments.api
    if target_api is None:
        candidates = [level for level, _ in platforms if level >= MIN_TARGET_API]
        if not candidates:
            return cannot_run('no platform at API %d or newer is installed; '
                              'install one with "sdkmanager \'platforms;'
                              'android-%d.0\'"' % (MIN_TARGET_API,
                                                   MIN_TARGET_API))
        target_api = max(candidates)

    target = pick(platforms, target_api)
    if target is None:
        return cannot_run('no platform at API %d is installed' % target_api)

    baseline_api = arguments.baseline
    if baseline_api is None:
        # Below MIN_TARGET_API, not merely below the target. Once a runner
        # carries API 38 the newest-below-target rule would pick 37 as the
        # baseline, and every removal API 37 made would then fail identically
        # in both compilations and cancel out -- the gate would go on passing
        # while quietly no longer checking the level it was built for. The
        # baseline has to sit under the whole range of removals being watched,
        # so it is pinned below the first of them and only the target floats.
        bound = min(target_api, MIN_TARGET_API)
        below = [level for level, _ in platforms if level < bound]
        if not below:
            return cannot_run('nothing older than API %d to compare against; '
                              'install a baseline platform below the first '
                              'API level this gate watches' % bound)
        baseline_api = max(below)

    baseline = pick(platforms, baseline_api)
    if baseline is None:
        return cannot_run('no platform at API %d is installed' % baseline_api)

    compiler = javac()
    if compiler is None:
        return cannot_run('no javac (set JAVA17_HOME)')

    roots = arguments.source_root or DEFAULT_SOURCE_ROOTS
    source_files = sources(roots)
    if not source_files:
        return cannot_run('no .java under %s' % ', '.join(roots))

    classpath = auto_classpath()
    classpath += [entry for entry in arguments.classpath.split(os.pathsep)
                  if entry]

    print('check-android-api-removals: %d sources from %s'
          % (len(source_files), ', '.join(roots)))
    print('  baseline API %d: %s' % (baseline_api, baseline))
    print('  target   API %d: %s' % (target_api, target))
    # Printed because an empty classpath is survivable but not silent: it
    # raises the error counts on both sides, and a reader comparing two runs
    # needs to know which of them was resolving dependencies.
    print('  classpath entries: %d' % len(without_platform_stubs(classpath)))

    try:
        with tempfile.TemporaryDirectory(prefix='cn1-api-removals-') as workdir:
            baseline_errors = compile_against(
                compiler, os.path.join(baseline, 'android.jar'), classpath,
                source_files, os.path.join(workdir, 'baseline'))
            target_errors = compile_against(
                compiler, os.path.join(target, 'android.jar'), classpath,
                source_files, os.path.join(workdir, 'target'))
    except CompilerFailure as broken:
        return cannot_run(str(broken))

    # A multiset difference, not a set one: two identical diagnostics are two
    # errors, and an error that appears twice against the target and once
    # against the baseline is a new one.
    remaining = collections.Counter(baseline_errors)
    new_errors = []
    for diagnostic in target_errors:
        if remaining[diagnostic] > 0:
            remaining[diagnostic] -= 1
        else:
            new_errors.append(diagnostic)
    print('  errors at baseline: %d, at target: %d, new at target: %d'
          % (len(baseline_errors), len(target_errors), len(new_errors)))

    if not new_errors:
        print('OK: the port compiles against API %d wherever it compiles '
              'against API %d.' % (target_api, baseline_api))
        return 0

    print('')
    print('FAIL: %d error(s) appear against API %d and not against API %d.'
          % (len(new_errors), target_api, baseline_api))
    for diagnostic in new_errors:
        for line in diagnostic.replace(REPO + os.sep, '').splitlines():
            print('  %s' % line)

    removals = removed_at(target, target_api)
    if removals:
        print('')
        print('API %d removed the following, per its own api-versions.xml:'
              % target_api)
        for entry in removals:
            print('  %s' % entry)
    print('')
    print('A platform API the port names has gone away. Reach it by name '
          'from a package the builder can delete, or route through an '
          'androidx compat class, the way com.codename1.impl.android'
          '.fingerprint does -- do not simply raise the floor, because the '
          'APK still has to run on the older devices where the removed API '
          'is the only one there is.')
    return 1


if __name__ == '__main__':
    sys.exit(main())
