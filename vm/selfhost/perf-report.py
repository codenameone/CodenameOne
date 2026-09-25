#!/usr/bin/env python3
"""Merge perf-gate.py results from every platform into one markdown report.

    python3 vm/selfhost/perf-report.py <results-dir> <out.md> linux-x64 linux-arm64 ...

Reads every *.json under <results-dir> (one per platform, as perf-gate.py wrote them) and
writes a table of RATIOS -- nothing absolute -- with each platform's verdict. Exits 1 when
any platform regressed or when an expected platform produced no result at all: a job that
crashed or timed out must not read as a pass just because it has no rows.

The first line is an HTML comment the workflow uses to find and update its own PR
comment instead of posting a new one per push.
"""
import json
from pathlib import Path
import sys

MARKER = '<!-- parparvm-perf-gate -->'
NAMES = {'linux-x64': 'Linux x64', 'linux-arm64': 'Linux arm64',
         'macos-arm64': 'macOS arm64', 'windows-x64': 'Windows x64'}
LABELS = {'ok': 'ok', 'improved': 'better than baseline', 'regression': '**REGRESSION**',
          'uncalibrated': 'not gated (no baseline)'}


def cell(metric):
    text = '%.2fx' % metric['median']
    if metric['baseline'] is not None:
        text += ' (baseline %.2fx)' % metric['baseline']
    return text


def status(entry):
    verdicts = {entry['time']['verdict'], entry['memory']['verdict']}
    for v in ('regression', 'uncalibrated', 'improved', 'ok'):
        if v in verdicts:
            return LABELS[v]
    return '?'


def render(results, expected):
    by_platform = {r['platform']: r for r in results}
    rounds = sorted({r['rounds'] for r in results}) or ['?']
    tolerance = results[0]['tolerance'] if results else {'time': 0, 'memory': 0}
    lines = [MARKER, '### ParparVM vs HotSpot (JDK 25)', '',
             'Ratios are **ParparVM / JDK 25** translating the HelloCodenameOne application: '
             'below 1.00x ParparVM is faster (time) or smaller (peak memory). Each value is the '
             'median of %s interleaved, paired rounds, and every run\'s output was verified '
             'byte-identical. A regression is a ratio more than %d%% (time) / %d%% (memory) above '
             'its baseline in `vm/selfhost/perf-baseline.json`.'
             % ('/'.join(map(str, rounds)), round(tolerance['time'] * 100),
                round(tolerance['memory'] * 100)), '',
             '| Platform | Cores | Time | Memory | Status |', '|---|---:|---|---|---|']
    regressed = False
    missing = False
    notes = []
    for platform in expected:
        name = NAMES.get(platform, platform)
        report = by_platform.get(platform)
        if report is None:
            lines.append('| %s | - | - | - | **no result** (the job failed or was cancelled) |'
                         % name)
            missing = True
            continue
        for cores, entry in sorted(report['results'].items(), key=lambda kv: int(kv[0])):
            logical = '' if entry.get('enforced') else '*'
            lines.append('| %s | %s%s | %s | %s | %s |' % (
                name, cores, logical, cell(entry['time']), cell(entry['memory']), status(entry)))
            if logical:
                notes.append(platform)
            if 'regression' in (entry['time']['verdict'], entry['memory']['verdict']):
                regressed = True
        for cores in report.get('skipped_cores', []):
            lines.append('| %s | %s | - | - | not run: the runner has %d cores |'
                         % (name, cores, report['available_cores']))
    if notes:
        lines += ['', '\\* No CPU affinity on this platform: both arms are told the core count '
                  '(`CN1_GC_MARK_THREADS`, `-XX:ActiveProcessorCount`) but neither is confined to it.']
    uncalibrated = [r for r in results for e in r['results'].values()
                    if 'uncalibrated' in (e['time']['verdict'], e['memory']['verdict'])]
    if uncalibrated:
        lines += ['', 'Rows marked "not gated" have no baseline yet; the job log prints the '
                  'entry to add for them.']
    outcome = []
    if regressed:
        outcome.append('performance regression')
    if missing:
        outcome.append('a platform produced no result')
    lines += ['', '**Result: %s**' % (' and '.join(outcome) if outcome else 'no regression')]
    return '\n'.join(lines) + '\n', regressed or missing


def main(argv):
    if len(argv) < 3:
        sys.exit('usage: perf-report.py <results-dir> <out.md> <platform>...')
    results = [json.loads(p.read_text()) for p in sorted(Path(argv[0]).rglob('*.json'))]
    text, failed = render(results, argv[2:])
    Path(argv[1]).write_text(text)
    print(text)
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
