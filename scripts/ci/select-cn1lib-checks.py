#!/usr/bin/env python3
"""Select cn1lib checks from a verified diff; an unavailable diff runs all checks.

An empty successful diff is different from an unavailable base. BASE_SHA is a
commit from the event payload, never a branch name or a shell expression.
"""
import json
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
AI = ('cn1-ai-whisper', 'cn1-ai-stablediffusion')
ADS = ('cn1-admob', 'cn1-applovin', 'cn1-unity-levelplay')
PACKAGES = AI + ADS + ('cn1-ads-mock',)


def select(paths, libraries=None):
    libraries = libraries if libraries is not None else sorted(
        p.name for p in (ROOT / 'maven').glob('cn1-*') if p.is_dir())
    result = {key: set() for key in ('package', 'android', 'desktop', 'ios_ads', 'ios_ai')}
    link = paths is None
    if paths is None:
        result.update(package=set(PACKAGES), android=set(libraries), desktop=set(libraries),
                      ios_ads=set(ADS), ios_ai=set(AI))
    for path in paths or []:
        if path.endswith('.md'):
            continue
        parts = path.split('/')
        if len(parts) > 2 and parts[0] == 'maven' and parts[1] in libraries:
            lib = parts[1]
            if lib in PACKAGES: result['package'].add(lib)
            # Library build hints and common interfaces can affect every platform.
            if parts[2] in ('common', 'pom.xml'):
                result['android'].add(lib)
                result['desktop'].add(lib)
                if lib in ADS: result['ios_ads'].add(lib)
                if lib in AI: result['ios_ai'].add(lib)
            if parts[2] == 'android': result['android'].add(lib)
            if parts[2] in ('linux', 'win', 'javascript'): result['desktop'].add(lib)
            if parts[2] == 'ios':
                if lib in ADS: result['ios_ads'].add(lib)
                if lib in AI: result['ios_ai'].add(lib)
            if lib == 'cn1-admob' and parts[2] in ('ios', 'common', 'pom.xml'): link = True
        if path == 'scripts/gen-ai-cn1libs.py':
            result['package'].update(AI)
            result['android'].update(AI)
            result['desktop'].update(AI)
            result['ios_ai'].update(AI)
        if path == 'maven/pom.xml' or path.startswith('maven/codenameone-maven-plugin/'):
            result['package'].update(PACKAGES)
        if path.startswith(('CodenameOne/src/', 'Ports/Android/', 'scripts/cn1lib-api-check/')) or path in (
                'scripts/check-cn1lib-android-api.py', '.ci/container/Dockerfile'):
            result['android'].update(libraries)
        if path.startswith('vm/ByteCodeTranslator/src/') and path.endswith('.h'):
            result['desktop'].update(libraries)
            result['ios_ads'].update(ADS)
        if path == 'scripts/check-cn1lib-native-sources.py' or path == '.ci/container/Dockerfile':
            result['desktop'].update(libraries)
        if path.startswith('vm/ByteCodeTranslator/') or path in ('vm/pom.xml', 'scripts/check-admob-ios-link.sh'):
            link = True
        if path in ('scripts/ci/check-ios-cn1lib.py', '.github/workflows/ad-cn1lib-ios-native-check.yml'):
            result['ios_ads'].update(ADS)
            link = True
        if path in ('scripts/ci/check-ios-cn1lib.py', '.github/workflows/ai-cn1lib-native-check.yml'):
            result['ios_ai'].update(AI)
        if path in ('scripts/ci/select-cn1lib-checks.py', '.github/workflows/pr.yml'):
            return select(None, libraries)
    # Only pass libraries with native sources to checkers that reject unknown names.
    result['android'] = {lib for lib in result['android'] if
                         any((ROOT / 'maven' / lib / 'android/src/main/java').rglob('*.java'))}
    out = {key: ','.join(sorted(value)) for key, value in result.items()}
    out['modules'] = ','.join(lib if lib == 'cn1-ads-mock' else lib + '/common'
                             for lib in sorted(result['package']))
    out['libs'] = out['package']
    out['admob_link'] = str(link).lower()
    return out


def changed_files():
    base = os.environ.get('BASE_SHA', '')
    if not re.fullmatch(r'[0-9a-fA-F]{40}', base) or set(base) == {'0'}:
        return None
    try:
        # Check first: local validation can supply an existing base without network.
        if subprocess.run(['git', 'cat-file', '-e', base + '^{commit}'], cwd=ROOT,
                          stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode:
            subprocess.run(['git', 'fetch', '--no-tags', '--depth=1', 'origin', base], cwd=ROOT, check=True)
        return subprocess.check_output(['git', 'diff', '--name-only', '-z', base, 'HEAD'], cwd=ROOT).decode().split('\0')[:-1]
    except (subprocess.CalledProcessError, UnicodeDecodeError):
        print('::warning::Diff unavailable; running all cn1lib checks', file=sys.stderr)
        return None


if __name__ == '__main__':
    selected = select(changed_files())
    print(json.dumps(selected, indent=2))
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
            for key, value in selected.items(): output.write(key + '=' + value + '\n')
