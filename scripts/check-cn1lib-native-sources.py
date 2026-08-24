#!/usr/bin/env python3
"""Compile the toolchain-light native sources every cn1lib ships.

Two platforms in a cn1lib need nothing but a C compiler or node to check:

  * the native win32/Linux ports compile the library's C glue into the app, the
    same way the iOS build compiles its Objective-C, so a typo there is a broken
    customer build; and
  * the JavaScript port loads the library's .js implementation verbatim, where a
    syntax error is a runtime failure with no build step to catch it.

Neither is covered by the per-platform workflows, which need Xcode or an
Android SDK. This runs anywhere.

  scripts/check-cn1lib-native-sources.py [--require-all]

Not covered here, deliberately: cn1-ai-whisper's android-aar JNI sources. They
need the NDK and a whisper.cpp checkout, and unlike everything above they are
compiled by us into a committed .aar rather than by the customer, so a break
shows up when we rebuild that binary and can never reach an app build.
"""

import os
import shutil
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CN1_GLOBALS = os.path.join(REPO, 'vm', 'ByteCodeTranslator', 'src', 'cn1_globals.h')


def libraries():
    maven = os.path.join(REPO, 'maven')
    for name in sorted(os.listdir(maven)):
        if name.startswith('cn1-') and os.path.isdir(os.path.join(maven, name)):
            yield name


def sources(lib, *parts):
    root = os.path.join(REPO, 'maven', lib, *parts)
    if not os.path.isdir(root):
        return
    for base, _dirs, files in os.walk(root):
        for name in sorted(files):
            yield os.path.join(base, name)


def c_sources(lib):
    for platform in ('linux', 'win'):
        for path in sources(lib, platform, 'src', 'main', 'c'):
            if path.endswith('.c'):
                yield path


def js_sources(lib):
    for path in sources(lib, 'javascript', 'src', 'main', 'javascript'):
        if path.endswith('.js'):
            yield path


def prepare_headers(work):
    """The include directory a translated project would give these sources."""
    os.makedirs(work, exist_ok=True)
    shutil.copyfile(CN1_GLOBALS, os.path.join(work, 'cn1_globals.h'))
    # Generated per translation from the app's class list; the glue does not
    # read it, so an empty stand-in lets cn1_globals.h parse on its own.
    with open(os.path.join(work, 'cn1_class_method_index.h'), 'w') as f:
        f.write('#pragma once\n')
    return work


def main(argv):
    require_all = '--require-all' in argv
    findings = []
    skipped = []

    cc = os.environ.get('CC') or shutil.which('cc') or shutil.which('gcc')
    node = shutil.which('node')
    work = prepare_headers(os.path.join(REPO, 'maven', 'target',
                                        'cn1lib-native-sources'))

    checked = 0
    for lib in libraries():
        for path in c_sources(lib):
            rel = os.path.relpath(path, REPO)
            if cc is None:
                skipped.append('%s (no C compiler; set CC)' % rel)
                continue
            # The Windows-only branches of these files are behind _WIN32 and
            # need the Windows SDK, so on any other host this checks the
            # portable half. That is still every line the two files share.
            result = subprocess.run([cc, '-fsyntax-only', '-I', work, path],
                                    stdout=subprocess.PIPE,
                                    stderr=subprocess.STDOUT)
            if result.returncode != 0:
                findings.append('%s does not compile:\n%s'
                                % (rel, result.stdout.decode('utf-8', 'replace').strip()))
            else:
                checked += 1
                print('  %s: compiles' % rel)

        for path in js_sources(lib):
            rel = os.path.relpath(path, REPO)
            if node is None:
                skipped.append('%s (no node)' % rel)
                continue
            result = subprocess.run([node, '--check', path],
                                    stdout=subprocess.PIPE,
                                    stderr=subprocess.STDOUT)
            if result.returncode != 0:
                findings.append('%s is not valid JavaScript:\n%s'
                                % (rel, result.stdout.decode('utf-8', 'replace').strip()))
            else:
                checked += 1
                print('  %s: parses' % rel)

    if skipped:
        message = ('check-cn1lib-native-sources: skipped %d source(s): %s'
                   % (len(skipped), '; '.join(skipped)))
        if require_all:
            sys.stderr.write(message + '\n')
            return 1
        print(message)

    if findings:
        sys.stderr.write('\n')
        for finding in findings:
            sys.stderr.write(finding + '\n\n')
        return 1
    print('check-cn1lib-native-sources: %d source(s) checked, no findings'
          % checked)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
