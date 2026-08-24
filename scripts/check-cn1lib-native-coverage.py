#!/usr/bin/env python3
"""Fail when a cn1lib's native sources are not compiled by any CI job.

Native sources in a cn1lib are shipped, never built by us. Two workflows close
that gap by compiling them -- ai-cn1lib-native-check.yml and
ad-cn1lib-ios-native-check.yml -- but a workflow only covers the libraries
named in its matrix, so a new cn1lib is uncovered by default and nothing says
so. That is exactly how cn1-admob and cn1-unity-levelplay shipped Objective-C
that had never been through a compiler.

This checks the inverse of what those workflows check: not "does the code
compile" but "is there a job that would have found out". It also requires a
library that pulls a CocoaPod to pin it, because an unpinned pod moves the API
underneath sources that are only compiled when the pod is fetched.

Coverage is read from evidence rather than from a workflow's name. A file
counts only if it stages a cn1lib's Objective-C and runs xcodebuild over it,
and a library counts only if that workflow also triggers on its own path --
otherwise a matrix over "lib:" in some unrelated packaging job would report a
library as covered while nothing ever compiled it, and a library missing from
the trigger paths would be compiled only when something else changed.

The Android half needs no registry: check-cn1lib-android-api.py enumerates
libraries from the filesystem, so it cannot miss one.

  scripts/check-cn1lib-native-coverage.py
"""

import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORKFLOWS = os.path.join(REPO, '.github', 'workflows')

# Both spellings of a GitHub Actions matrix over libraries:
#   lib: [cn1-admob, cn1-applovin]
#   - { lib: cn1-ai-whisper, pod: '' }
MATRIX_LIST = re.compile(r'lib:\s*\[([^\]]*)\]')
MATRIX_ENTRY = re.compile(r'\{\s*lib:\s*([\w.\-]+)')

POD_HINT = re.compile(r'^codename1\.arg\.ios\.pods=(.*)$', re.M)


def libraries_with_ios_sources():
    maven = os.path.join(REPO, 'maven')
    for name in sorted(os.listdir(maven)):
        if not name.startswith('cn1-'):
            continue
        objc = os.path.join(maven, name, 'ios', 'src', 'main', 'objectivec')
        if not os.path.isdir(objc):
            continue
        if any(f.endswith(('.m', '.mm')) for f in os.listdir(objc)):
            yield name


def compiles_cn1lib_natives(text):
    """True when a workflow stages a cn1lib's Objective-C and builds it.

    Naming a library in a matrix proves nothing on its own; these two markers
    are what separate a native check from any other job that happens to loop
    over libraries.
    """
    return 'ios/src/main/objectivec' in text and 'xcodebuild' in text


def covered_libraries():
    """Two maps: libraries a native check compiles, and libraries a native
    check names but never triggers for."""
    covered = {}
    untriggered = {}
    if not os.path.isdir(WORKFLOWS):
        return covered, untriggered
    for name in sorted(os.listdir(WORKFLOWS)):
        if not name.endswith(('.yml', '.yaml')):
            continue
        with open(os.path.join(WORKFLOWS, name), encoding='utf-8') as f:
            text = f.read()
        if not compiles_cn1lib_natives(text):
            continue
        found = set(MATRIX_ENTRY.findall(text))
        for group in MATRIX_LIST.findall(text):
            found.update(part.strip() for part in group.split(','))
        for lib in found:
            if not lib.startswith('cn1-'):
                continue
            # A library the workflow never triggers for is compiled only when
            # something else in that workflow's paths changes, which is not
            # coverage of a change to the library. Recorded separately so the
            # finding can say which of the two is missing.
            if ("maven/%s/**" % lib) in text:
                covered.setdefault(lib, name)
            else:
                untriggered.setdefault(lib, name)
    return covered, untriggered


def pod_findings(lib):
    props = os.path.join(REPO, 'maven', lib, 'common',
                         'codenameone_library_required.properties')
    if not os.path.isfile(props):
        return []
    with open(props, encoding='utf-8') as f:
        text = f.read()
    findings = []
    for value in POD_HINT.findall(text):
        for pod in value.split(','):
            pod = pod.strip()
            if not pod:
                continue
            if ' ' not in pod:
                findings.append(
                    '%s pulls the CocoaPod "%s" without a version. Pin it (for '
                    'example "%s ~> 1.0") so the SDK the native sources are '
                    'compiled against cannot change without a commit.'
                    % (lib, pod, pod))
    return findings


def main():
    covered, untriggered = covered_libraries()
    findings = []
    libs = list(libraries_with_ios_sources())
    for lib in libs:
        if lib in untriggered:
            findings.append(
                "%s is in %s's matrix, but that workflow does not trigger on "
                "'maven/%s/**', so a change to the library does not run the "
                "check. Add the path to its pull_request and push filters."
                % (lib, untriggered[lib], lib))
        elif lib not in covered:
            findings.append(
                '%s ships Objective-C under maven/%s/ios/src/main/objectivec '
                'but no workflow both compiles it and triggers on its path, so '
                'nothing compiles it before a customer does. Add it to a '
                'native-check workflow.'
                % (lib, lib))
        findings.extend(pod_findings(lib))

    if findings:
        sys.stderr.write('cn1lib native sources without CI coverage:\n\n')
        for finding in findings:
            sys.stderr.write('  ' + finding + '\n')
        sys.stderr.write('\n')
        return 1
    for lib in libs:
        print('  %s: compiled by %s' % (lib, covered[lib]))
    print('check-cn1lib-native-coverage: %d librar%s covered'
          % (len(libs), 'y' if len(libs) == 1 else 'ies'))
    return 0


if __name__ == '__main__':
    sys.exit(main())
