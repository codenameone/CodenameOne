#!/usr/bin/env python3
"""Every java.* type the backend's public API exposes must be one the backend API
reference documents.

    scripts/check-backend-jdk-surface.py

The backend compiles against vm/JavaAPI, which is larger than the CLDC java.* set the
client reference documents, so a backend signature can use a JDK type -- Future,
TimeUnit -- that no reference page describes and nothing promised to keep. That is
how @Async's Future shipped: usable, compiled, and undocumented. A type the backend
exposes has to be either a CLDC class or one of PROMOTED, which build_javadocs.sh
adds to the backend reference from vm/JavaAPI.
"""
import os
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
# The vm/JavaAPI classes the backend reference documents beyond the CLDC set. Keep
# in step with the PROMOTED list in .github/scripts/build_javadocs.sh.
PROMOTED = [
    'java.util.Properties',
    'java.util.concurrent.CancellationException',
    'java.util.concurrent.ExecutionException',
    'java.util.concurrent.Future',
    'java.util.concurrent.TimeUnit',
    'java.util.concurrent.TimeoutException',
]


def exists(root, fq):
    return (REPO / root / (fq.replace('.', '/') + '.java')).is_file()


def main():
    missing = {}
    # The runtime, and the test library's public package, which the same
    # reference documents.
    api = sorted((REPO / 'vm/backend/src').rglob('*.java')) + sorted(
        (REPO / 'vm/backend/test/src/com/codename1/backend/test').rglob('*.java'))
    for path in api:
        text = path.read_text(encoding='utf-8')
        imports = {m.group(1).split('.')[-1]: m.group(1)
                   for m in re.finditer(r'^import (java\.[\w.]+);', text, re.M)}
        for sig in re.finditer(r'^\s*(?:public|protected)\s[^;{=]*[({]', text, re.M):
            s = sig.group(0)
            found = set(re.findall(r'java\.[a-z]+(?:\.[a-z]+)*\.[A-Z]\w*', s))
            found |= {fq for simple, fq in imports.items() if re.search(r'\b' + simple + r'\b', s)}
            for fq in found:
                if not exists('Ports/CLDC11/src', fq) and fq not in PROMOTED:
                    missing.setdefault(fq, set()).add(str(path.relative_to(REPO)))
    # And the guide's backend examples: they compile against the JDK in their own
    # module, so a class the translated backend does not have -- the first example
    # used ConcurrentHashMap -- builds there and fails for everyone who copies it.
    for path in sorted((REPO / 'docs/demos/backend/src').rglob('*.java')):
        text = path.read_text(encoding='utf-8')
        used = set(re.findall(r'^import (java\.[\w.]+);', text, re.M))
        used |= set(re.findall(r'\b(java\.[a-z]+(?:\.[a-z]+)*\.[A-Z]\w*)', text))
        for fq in used:
            if fq.endswith('.*'):
                continue
            if not exists('Ports/CLDC11/src', fq) and fq not in PROMOTED:
                missing.setdefault(fq, set()).add(str(path.relative_to(REPO)))
    for fq in PROMOTED:
        if not exists('vm/JavaAPI/src', fq):
            print('check-backend-jdk-surface: %s is promoted but vm/JavaAPI has no source for it'
                  % fq)
            return 1
    if missing:
        print('check-backend-jdk-surface: the backend API exposes JDK types its reference '
              'does not document:')
        for fq in sorted(missing):
            print('  %s  (%s)' % (fq, ', '.join(sorted(missing[fq]))))
        print('Document it by adding it to PROMOTED here and in build_javadocs.sh -- with '
              'test coverage on ParparVM -- or keep it out of the public API.')
        return 1
    print('check-backend-jdk-surface: every JDK type the backend exposes is documented')
    return 0


if __name__ == '__main__':
    sys.exit(main())
