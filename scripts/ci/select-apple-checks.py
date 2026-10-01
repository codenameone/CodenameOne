#!/usr/bin/env python3
"""Select Apple suites before allocating runners; preserve ordered path filters."""
import importlib.util
import json
import os
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('cn1lib_selection', Path(__file__).with_name('select-cn1lib-checks.py'))
shared = importlib.util.module_from_spec(spec)
spec.loader.exec_module(shared)


def matches(path, pattern):
    # These checked-in filters use literals, *, ** and ?. Reject an unfamiliar
    # pattern rather than silently narrowing coverage when a filter is edited.
    if any(char in pattern for char in '[]+{}'):
        raise ValueError('Unsupported path filter: ' + pattern)
    regex = ''
    index = 0
    while index < len(pattern):
        if pattern[index:index+3] == '**/':
            regex += '(?:.*/)?'
            index += 3
        elif pattern[index:index+2] == '**':
            regex += '.*'
            index += 2
        else:
            char = pattern[index]
            regex += '[^/]*' if char == '*' else '[^/]' if char == '?' else re.escape(char)
            index += 1
    return re.fullmatch(regex, path) is not None


def affected(paths, patterns):
    for path in paths:
        included = False
        for pattern in patterns:
            negative = pattern.startswith('!')
            if matches(path, pattern[1:] if negative else pattern):
                included = not negative
        if included: return True
    return False


def select(paths, event='pull_request'):
    filters = json.loads((ROOT / '.github/ci/apple-checks.json').read_text())
    # Planner changes must exercise every consumer. Missing diffs, schedules and
    # manual dispatches retain full coverage; a known empty diff selects none.
    full = paths is None or any(path in (
        '.github/workflows/scripts-ios.yml', '.github/ci/apple-checks.json',
        'scripts/ci/select-apple-checks.py',
        'scripts/ci/select-cn1lib-checks.py') for path in paths)
    result = {suite: full or affected(paths, events[event]) for suite, events in filters.items()}
    result['any'] = any(result.values())
    return {key: str(value).lower() for key, value in result.items()}


if __name__ == '__main__':
    result = select(shared.changed_files(), os.environ.get('GITHUB_EVENT_NAME', 'pull_request')
                    if os.environ.get('GITHUB_EVENT_NAME') in ('pull_request', 'push') else 'pull_request')
    print(json.dumps(result, indent=2))
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        for key, value in result.items(): output.write(key + '=' + value + '\n')
