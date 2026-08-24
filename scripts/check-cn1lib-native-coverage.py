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

Coverage is read from evidence rather than from a workflow's name, and per
job rather than per file. A job counts only if it stages a cn1lib's
Objective-C and runs xcodebuild over it, its matrix is read from that job
alone, and the library also has to appear in the workflow's trigger paths.

Each of those rules exists because dropping it lets something claim coverage
it does not have: a matrix over "lib:" in an unrelated packaging job, a
packaging job sitting in the same file as a real native check, or a library
the workflow never fires for. The trigger paths are read per trigger, since a
library listed under push but not pull_request is compiled only after it has
already merged. Jobs and triggers are separated by indentation rather than
with a YAML parser, so this keeps running wherever python3 does.

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


JOB_START = re.compile(r'^  ([A-Za-z_][\w.\-]*):\s*$')
# The triggers a change to a library has to run under. push alone would mean
# the break is found after it merged; pull_request alone leaves master
# unguarded against anything that lands another way.
REQUIRED_TRIGGERS = ('pull_request', 'push')


def jobs(text):
    """Yield each job's body from a workflow, split on indentation.

    A YAML parser would be tidier, but PyYAML is not in the standard library
    and this has to run in the CI container as it is.
    """
    lines = text.splitlines()
    starts = []
    in_jobs = False
    for index, line in enumerate(lines):
        if line.startswith('jobs:'):
            in_jobs = True
            continue
        if not in_jobs:
            continue
        # A non-indented line ends the jobs mapping.
        if line.strip() and not line.startswith(' ') and not line.startswith('#'):
            break
        if JOB_START.match(line):
            starts.append(index)
    for position, start in enumerate(starts):
        end = starts[position + 1] if position + 1 < len(starts) else len(lines)
        yield '\n'.join(lines[start:end])


def indented_block(text, header, indent):
    """The lines under `header` that are indented past it, as one string."""
    lines = text.splitlines()
    prefix = ' ' * indent
    out = []
    collecting = False
    for line in lines:
        if line.startswith(prefix + header):
            collecting = True
            continue
        if collecting:
            if line.strip() and not line.startswith(prefix + ' '):
                break
            out.append(line)
    return '\n'.join(out)


def triggers_for(text, lib):
    """True when every required trigger lists this library's path.

    on: is read as a block and each trigger inside it separately, because a
    path present under one trigger and missing from the other reads as covered
    to any whole-file search while half the cases run nothing.
    """
    on_block = indented_block(text, 'on:', 0)
    entry = "maven/%s/**" % lib
    for trigger in REQUIRED_TRIGGERS:
        if entry not in indented_block(on_block, trigger + ':', 2):
            return False
    return True


def compiles_cn1lib_natives(body):
    """True when a job stages a cn1lib's Objective-C and builds it.

    Naming a library in a matrix proves nothing on its own; these two markers
    are what separate a native check from any other job that happens to loop
    over libraries. Applied per job, because one file can hold both.
    """
    return 'ios/src/main/objectivec' in body and 'xcodebuild' in body


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
        found = set()
        for body in jobs(text):
            if not compiles_cn1lib_natives(body):
                continue
            found.update(MATRIX_ENTRY.findall(body))
            for group in MATRIX_LIST.findall(body):
                found.update(part.strip() for part in group.split(','))
        for lib in found:
            if not lib.startswith('cn1-'):
                continue
            # A library the workflow never triggers for is compiled only when
            # something else in that workflow's paths changes, which is not
            # coverage of a change to the library. Recorded separately so the
            # finding can say which of the two is missing.
            if triggers_for(text, lib):
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
                "%s is in %s's matrix, but 'maven/%s/**' is missing from at "
                "least one of that workflow's %s trigger path lists, so a "
                "change to the library does not run the check in every context. "
                "Add it to each."
                % (lib, untriggered[lib], lib, ' and '.join(REQUIRED_TRIGGERS)))
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
