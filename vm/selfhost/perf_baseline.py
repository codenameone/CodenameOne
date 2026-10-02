#!/usr/bin/env python3
"""The performance gate's baselines: where they live, how a pull request changes them,
and how those changes are folded in.

    python3 vm/selfhost/perf_baseline.py check [--base REF] [--pr N]
    python3 vm/selfhost/perf_baseline.py fold
    python3 vm/selfhost/perf_baseline.py summary --out FILE
    python3 vm/selfhost/perf_baseline.py import-legacy --pr N [--reason TEXT] --ref BRANCH

THE LAYOUT (vm/selfhost/perf-baseline/)
  policy.json            the global tolerance per metric and the RAM floor
  base/<key>.json        the consolidated rows of one runner, <key> = platform@cpu-model
                         (or the bare platform where the CPU model is unknown):
                         {benchmark: {cores: {time, memory, runs, tolerance?}}}
  pr/<number>.json       ONE pull request's changes to those rows

Every row used to sit in a single perf-baseline.json, and every branch that met a new
runner CPU or moved a benchmark edited it. Rows that have nothing to do with each other
sat within git's three lines of context of each other, so independent branches
conflicted, and two branches calibrating the same new CPU conflicted by construction.
A pull request now writes only its own pr/<number>.json -- a file no other branch can
write -- and the change is still in that pull request's diff, which is the point of
keeping baselines in the tree at all. base/ is written by `fold` alone, nightly on
master, and a pull request that edits it fails `check`.

AN OVERLAY (pr/<number>.json)
  {"pr": 5931,
   "calibrate":  {key: {benchmark: {cores: row}}},
   "rebaseline": {key: {benchmark: {cores: row + "from": {"time": t, "memory": m, ...}
                                                + "reason": "why THIS row moved"}}}}

  calibrate    a row that does not exist yet: a runner CPU model no run had measured.
               Hardware, not code -- whichever branch met the runner first carries it.
               Several overlays calibrating the same row are COMBINED (median of their
               ratios, summed runs, the wider tolerance), so the result does not depend
               on which merged first. A calibrate row whose row already exists is
               superseded and ignored: once one branch's calibration is folded, a second
               branch's adds nothing and must not fail for it.
  rebaseline   a row that exists and moved: a deliberate change in performance, better
               or worse. "from" is the baseline the pull request measured against, and
               "reason" says why this row moved -- per row, because a pull request can
               rebaseline rows at different times for different causes, and one reason
               for the whole file let a later row pass under an earlier row's
               explanation.
               Rebaselines of one row chain, each from the value the last one left, so a
               pull request measured on top of a merged one need not wait for the fold.
               Two from the SAME value, or one whose "from" the row never reaches --
               another merged change moved it first -- are rejected, naming them: two
               changes moved the same benchmark, and someone has to re-measure. That is
               the conflict git used to report as a JSON hunk, now reported as what it is.

Every overlay on master belongs to a merged pull request, so `fold` needs no knowledge
of GitHub: it writes the resolved rows into base/, deletes every overlay, and refuses to
commit unless the resolved baseline is identical before and after -- the fold changes no
verdict, by construction and by check.

OTHER GATES ON THE SAME MODEL
Everything above is parameterised by a Layout: where the tree lives, what a row's
metrics are called, what a key looks like, and which retired file a pull request may no
longer touch. The module-level functions default to SELFHOST, the layout this file was
written for, so `perf_baseline.resolve(...)` means exactly what it always did. The
Flutter benchmark's regression gate (scripts/flutter-bench/flutter_baseline.py) is the
second layout: its rows are one metric each, {platform[@cpu]: {app: {metric: row}}},
and it shares the overlay rules, the fold, the check and the calibrator's arithmetic
(calibration_context / plan_calibration) rather than keeping a copy that drifts.
"""
import argparse
import copy
import json
import math
import os
from pathlib import Path
import re
import statistics
import subprocess
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE / 'perf-baseline'
LEGACY_FILE = 'vm/selfhost/perf-baseline.json'   # the retired single-file layout
REPO = HERE.parents[1]
METRICS = ('time', 'memory')
KEY_RE = re.compile(r'^[a-z0-9]+-[a-z0-9]+(@[a-z0-9-]+)?$')
ROW_FIELDS = {'time', 'memory', 'runs', 'tolerance'}
# As the Port Status page spells them (scripts/website/validate_port_status.mjs).
PLATFORM_NAMES = {'linux-x64': 'Linux x64', 'linux-arm64': 'Linux ARM64',
                  'macos-arm64': 'macOS ARM64', 'macos-x64': 'macOS x64',
                  'windows-x64': 'Windows x64', 'windows-arm64': 'Windows ARM64'}
PLATFORM_ORDER = ['linux-x64', 'linux-arm64', 'windows-x64', 'windows-arm64',
                  'macos-arm64', 'macos-x64']
# What the Port Status page says about each benchmark. The workloads are
# vm/benchmarks/common's CommonWorkloads, the same code the page's absolute table runs
# inside each port's application, so these descriptions match
# docs/website/data/port_status_support.json's (test_perf_gate holds them in step).
BENCHMARKS = [
    ('hello', 'Translating an application',
     'The ParparVM translator translating the compliance test application, exactly as '
     'the platform build translates it.'),
    ('translator', 'Translating the translator',
     'The ParparVM translator translating its own classes, the Java API and ASM.'),
    ('intArithmetic', 'Integer arithmetic',
     '40 million dependent 32-bit multiply, add, shift, and XOR operations.'),
    ('longArithmetic', 'Long arithmetic',
     '30 million dependent 64-bit arithmetic and bitwise operations.'),
    ('mathTranscendental', 'Transcendental math',
     'Eight million data-dependent sqrt, sin, cos, and remainder operations.'),
    ('arraySequential', 'Sequential arrays',
     'Fill eight million integers and perform four complete reduction passes.'),
    ('arrayRandom', 'Random array access',
     '20 million data-dependent reads from a four-million-element array.'),
    ('objectAllocation', 'Object allocation',
     'Allocate eight million short-lived linked nodes with periodic traversal.'),
    ('valueEscape', 'Non-escaping value objects',
     'Eight million two-field value objects that never escape the loop, the shape escape '
     'analysis turns into registers.'),
    ('hashMapChurn', 'Hash map churn',
     'Three million boxed-key lookups and updates with repeated table clearing.'),
    ('stringBuilding', 'String building',
     'Build, retain, and hash 400,000 data-dependent strings.'),
    ('recursion', 'Recursion',
     'Three recursive Fibonacci calls using inputs 35 and 36.'),
    ('quicksort', 'Quicksort',
     'Generate and sort 1.5 million integers, then verify ordering and checksum.'),
]


class BaselineError(Exception):
    """A baseline the gate cannot judge against. Always fatal: a gate that skips what it
    cannot read stops preventing regressions without anyone noticing."""


def _same_default(tolerance, key, bench, cores):
    """The policy value a row with none of its own is judged by: the policy's own map,
    whose keys are the row's metrics."""
    return tolerance


class Layout(object):
    """What differs between two gates that share this module's rules.

    root               the directory holding policy.json, base/ and pr/
    metrics            the value fields of one row (each needs a positive number)
    key_re             what a base/ file name or overlay key may be
    legacy             the retired path(s) a pull request may no longer change
    migration_marker   a path under root whose ABSENCE on the base branch marks the one
                       pull request that creates the layout (and so may write base/)
    policy_metrics     the names policy.json gives a tolerance and floor for
    zero_tolerance     whether a policy tolerance of 0 is allowed (deterministic sizes)
    row_default        (policy map, key, bench, cores) -> {metric: value}: the policy
                       values a row without its own is judged by
    widest_key         (bench, cores, metric) -> what "the same benchmark" means when a
                       thin calibration borrows the widest tolerance seen elsewhere
    round_value        how a combined value is rounded
    tolerance_step     (key, bench, cores) -> the step a learned tolerance is rounded up
                       to: 5% for timing ratios, finer for a deterministic size
    calibrate_command  what to run to write an overlay, as the messages print it
    legacy_message     what `check` says when a pull request changed a retired path
    """

    def __init__(self, root, metrics, key_re, legacy, migration_marker='base',
                 policy_metrics=None, policy_metrics_text=None, zero_tolerance=False,
                 value_word='ratio', row_default=None, widest_key=None, round_value=None,
                 tolerance_step=None, calibrate_command='calibrate-perf-baseline.py --pr N', legacy_message=None):
        self.root = Path(root)
        self.metrics = tuple(metrics)
        self.row_fields = set(self.metrics) | {'runs', 'tolerance'}
        self.key_re = key_re
        self.legacy = legacy
        self.migration_marker = migration_marker
        self.policy_metrics = tuple(policy_metrics or self.metrics)
        self.policy_metrics_text = policy_metrics_text or ', '.join(self.policy_metrics)
        self.metrics_text = ' and '.join(self.metrics)
        self.zero_tolerance = zero_tolerance
        self.value_word = value_word
        self.row_default = row_default or _same_default
        self.widest_key = widest_key or (lambda bench, cores, metric: (bench, metric))
        self.round_value = round_value or (lambda value: round(value, 3))
        self.tolerance_step = tolerance_step or (lambda key, bench, cores: 0.05)
        self.calibrate_command = calibrate_command
        self.legacy_message = legacy_message


SELFHOST = Layout(
    ROOT, METRICS, KEY_RE, LEGACY_FILE, policy_metrics_text='time and memory',
    legacy_message='%s changed: that file is retired and nothing reads it. Convert the '
                   'branch\'s edits with `perf_baseline.py import-legacy --pr N --ref '
                   '<branch>` and delete it.' % LEGACY_FILE)


def _read_json(path):
    try:
        return json.loads(path.read_text())
    except ValueError as error:
        raise BaselineError('%s is not valid JSON: %s' % (path, error))


def dump(value):
    """The one serialization every file here uses, so a rewrite of unchanged data is a
    byte-for-byte no-op."""
    return json.dumps(value, indent=1, sort_keys=True) + '\n'


def _number(value):
    """A real, finite number: JSON's 1e999 parses to infinity, and a bool is an int."""
    return not isinstance(value, bool) and isinstance(value, (int, float)) and math.isfinite(value)


def _check_ratios(where, row, layout=None):
    layout = layout or SELFHOST
    for metric in layout.metrics:
        value = row.get(metric)
        if not _number(value) or not value > 0:
            raise BaselineError('%s: %s must be a positive %s, not %r'
                                % (where, metric, layout.value_word, value))
    tolerance = row.get('tolerance', {})
    if not isinstance(tolerance, dict) or set(tolerance) - set(layout.metrics):
        raise BaselineError('%s: tolerance must map %s to a fraction'
                            % (where, '/'.join(layout.metrics)))
    for metric, value in tolerance.items():
        if not _number(value) or not 0 < value < 5:
            raise BaselineError('%s: %s tolerance %r is not a fraction' % (where, metric, value))


def _check_row(where, row, extra=(), layout=None):
    layout = layout or SELFHOST
    unknown = set(row) - layout.row_fields - set(extra)
    if unknown:
        raise BaselineError('%s: unknown field(s) %s' % (where, ', '.join(sorted(unknown))))
    _check_ratios(where, row, layout)
    runs = row.get('runs')
    if isinstance(runs, bool) or not isinstance(runs, int) or runs < 1:
        raise BaselineError('%s: runs must be a positive integer, not %r' % (where, runs))


def _rows(where, tree, layout=None):
    """(key, benchmark, cores, row) for every row of a {key: {bench: {cores: row}}} tree."""
    layout = layout or SELFHOST
    if not isinstance(tree, dict):
        raise BaselineError('%s must be an object' % where)
    for key, benches in sorted(tree.items()):
        if not layout.key_re.match(key):
            raise BaselineError('%s: %r is not a platform or platform@cpu-model key' % (where, key))
        if not isinstance(benches, dict):
            raise BaselineError('%s: %s must be an object' % (where, key))
        for bench, per_cores in sorted(benches.items()):
            if not isinstance(per_cores, dict):
                raise BaselineError('%s: %s/%s must be an object' % (where, key, bench))
            for cores, row in sorted(per_cores.items()):
                if not isinstance(row, dict):
                    raise BaselineError('%s: %s/%s/%s must be an object' % (where, key, bench, cores))
                yield key, bench, cores, row


def load_policy(root=ROOT, layout=None):
    layout = layout or SELFHOST
    policy = _read_json(Path(root) / 'policy.json')
    for name in ('tolerance', 'floor'):
        values = policy.get(name)
        if not isinstance(values, dict) or set(values) != set(layout.policy_metrics):
            raise BaselineError('policy.json: %s must give %s' % (name, layout.policy_metrics_text))
        for metric, value in values.items():
            # A pull request may edit the policy, so a value verdict() would choke on --
            # or a negative tolerance, which inverts the gate -- is refused here, before
            # ten minutes of measurement per platform consume it.
            numeric = not isinstance(value, bool) and isinstance(value, (int, float))
            positive = name == 'tolerance' and not layout.zero_tolerance
            if not numeric or not (value > 0 if positive else value >= 0) or \
                    not value < 5:
                raise BaselineError('policy.json: %s %s %r is not a %s fraction' % (
                    name, metric, value, 'positive' if positive else 'non-negative'))
    return policy


def load_base(root=ROOT, layout=None):
    layout = layout or SELFHOST
    base = {}
    directory = Path(root) / 'base'
    for path in sorted(directory.glob('*')):
        if path.suffix != '.json':
            raise BaselineError('%s: only <key>.json files belong in base/' % path)
        key = path.stem
        for _, bench, cores, row in _rows(str(path), {key: _read_json(path)}, layout):
            _check_row('%s %s/%s' % (path.name, bench, cores), row, layout=layout)
            base.setdefault(key, {}).setdefault(bench, {})[cores] = row
    return base


def load_overlays(root=ROOT, layout=None):
    overlays = []
    for path in sorted(Path(root).joinpath('pr').glob('*')):
        if not re.match(r'^[1-9][0-9]*\.json$', path.name):
            raise BaselineError('%s: an overlay is named <pull request number>.json' % path)
        overlay = _read_json(path)
        validate_overlay(int(path.stem), overlay, path.name, layout)
        overlays.append((int(path.stem), overlay))
    return sorted(overlays)


def validate_overlay(number, overlay, where, layout=None):
    layout = layout or SELFHOST
    if not isinstance(overlay, dict):
        raise BaselineError('%s must be an object' % where)
    if 'reason' in overlay:
        raise BaselineError('%s: "reason" belongs to each rebaseline row, not the file: one '
                            'reason for the file lets a later row pass under an earlier '
                            'row\'s explanation' % where)
    unknown = set(overlay) - {'pr', 'calibrate', 'rebaseline'}
    if unknown:
        raise BaselineError('%s: unknown field(s) %s' % (where, ', '.join(sorted(unknown))))
    if overlay.get('pr') != number:
        raise BaselineError('%s: "pr" must be %d, the number in its file name' % (where, number))
    for _, bench, cores, row in _rows(where + ' calibrate', overlay.get('calibrate', {}), layout):
        _check_row('%s calibrate %s/%s' % (where, bench, cores), row, layout=layout)
    rebaselines = list(_rows(where + ' rebaseline', overlay.get('rebaseline', {}), layout))
    for key, bench, cores, row in rebaselines:
        here = '%s rebaseline %s %s/%s' % (where, key, bench, cores)
        _check_row(here, row, extra=('from', 'reason'), layout=layout)
        reason = row.get('reason')
        if not isinstance(reason, str) or not reason.strip() or \
                reason.strip().upper().startswith('TODO'):
            raise BaselineError('%s: a rebaseline needs a "reason" saying why this row moved'
                                % here)
        old = row.get('from')
        if not isinstance(old, dict) or set(old) - {'tolerance'} != set(layout.metrics):
            raise BaselineError('%s: "from" must give the %s baseline it '
                                'replaces, and its tolerance if it had one'
                                % (here, layout.metrics_text))
        _check_ratios(here + ' from', old, layout)
    if not overlay.get('calibrate') and not rebaselines:
        raise BaselineError('%s changes nothing; delete it' % where)


def from_row(row, layout=None):
    """What a rebaseline records as the row it replaces: the ratios AND the tolerance. A
    tolerance-only recalibration (an --all run whose ratios round to the old values) is a
    change too, and a later rebaseline that compared ratios alone would pass and then put
    the stale tolerance back."""
    old = {m: row[m] for m in (layout or SELFHOST).metrics}
    if row.get('tolerance'):
        old['tolerance'] = dict(row['tolerance'])
    return old


def _same(a, b, layout=None):
    """Whether the row a rebaseline was measured against (`a`, its "from") is still the
    row in the tree (`b`). No tolerance on either side means none."""
    return all(math.isclose(a[m], b[m], rel_tol=0, abs_tol=5e-4)
               for m in (layout or SELFHOST).metrics) and \
        a.get('tolerance', {}) == b.get('tolerance', {})


def _combine(rows, default=None, layout=None, step=0.05):
    """Several branches calibrating the same new row: one calibration from all of them.

    The baseline is the median of their ratios, and the tolerance is wide enough that every
    contributing row's own band (its ratio, plus or minus its tolerance, or `default`'s
    where it gives none) still passes around that median. Keeping only the widest of the
    rows' tolerances is not enough: calibrations at 1.0x and 2.0x, each at 15%, would fold
    to 1.5x at 15%, which neither of the runs they came from passes. The median is not
    weighted by `runs`: each overlay's row is already the median of its own runs, and
    two branches meeting the same new CPU in the window before a fold are rare enough
    that a plain median of the two is the honest summary."""
    if len(rows) == 1:
        return copy.deepcopy(rows[0])
    layout = layout or SELFHOST
    default = default or {}
    combined = {m: layout.round_value(statistics.median(r[m] for r in rows))
                for m in layout.metrics}
    combined['runs'] = sum(r['runs'] for r in rows)
    tolerance = {}
    for metric in layout.metrics:
        base = combined[metric]
        need = 0.0
        for r in rows:
            own = r.get('tolerance', {}).get(metric, default.get(metric, 0.0))
            need = max(need, own, r[metric] * (1 + own) / base - 1, 1 - r[metric] * (1 - own) / base)
        need = round_up(need, step)
        if need > default.get(metric, 0.0):
            tolerance[metric] = need
    if tolerance:
        combined['tolerance'] = tolerance
    return combined


def resolve(base, overlays, tolerance=None, layout=None):
    """The rows the gate judges against: base with every overlay applied. `tolerance` is
    the policy's global one, which a row without its own is judged by.

    Returns (rows, notes). Raises BaselineError for overlays that contradict each other or
    the base, naming the pull requests involved."""
    layout = layout or SELFHOST
    rows = copy.deepcopy(base)
    notes = []
    calibrations = {}
    for number, overlay in overlays:
        for key, bench, cores, row in _rows('pr/%d.json' % number, overlay.get('calibrate', {}),
                                            layout):
            calibrations.setdefault((key, bench, cores), []).append((number, row))
    for (key, bench, cores), entries in sorted(calibrations.items()):
        if cores in rows.get(key, {}).get(bench, {}):
            notes.append('%s %s/%s: already calibrated; the calibration in %s is superseded'
                         % (key, bench, cores, ', '.join('pr/%d.json' % n for n, _ in entries)))
            continue
        combined = _combine([r for _, r in entries],
                            layout.row_default(tolerance, key, bench, cores), layout,
                            layout.tolerance_step(key, bench, cores))
        # Calibrations of one CPU that disagree wildly combine into a tolerance past any
        # sane fraction: a row that gates nothing, which the fold would only refuse after
        # the fact. Refuse it here, naming the calibrations. There is deliberately no
        # automatic recovery (the calibrator does not rewrite a pull request's calibrate
        # entry when this fires): one CPU measured an order of magnitude apart by runs of
        # the same code is a broken measurement, and the fix is to delete that entry from
        # the pr/<number>.json this message names, not to re-measure around it.
        _check_row('%s %s/%s as combined from %s' % (
            key, bench, cores, ', '.join('pr/%d.json' % n for n, _ in entries)), combined,
            layout=layout)
        rows.setdefault(key, {}).setdefault(bench, {})[cores] = combined
    rebaselines = {}
    for number, overlay in overlays:
        for key, bench, cores, row in _rows('pr/%d.json' % number, overlay.get('rebaseline', {}),
                                            layout):
            rebaselines.setdefault((key, bench, cores), []).append((number, row))
    for (key, bench, cores), entries in sorted(rebaselines.items()):
        where = '%s %s/%s' % (key, bench, cores)
        # Several rebaselines of one row are applied as a CHAIN, each from the value the
        # previous one left: a pull request that measured on top of one merged but not yet
        # folded names that one's result as its "from", and must not wait for the nightly
        # fold. Two entries from the SAME value are two changes that each moved the row
        # without seeing the other -- the real conflict. The order comes from the "from"
        # values alone, so it does not depend on which merged first.
        pending = list(entries)
        while pending:
            current = rows.get(key, {}).get(bench, {}).get(cores)
            if current is None:
                raise BaselineError('pr/%d.json rebaselines %s, which has no row; a new row is '
                                    'a "calibrate" entry' % (pending[0][0], where))
            ready = [(n, r) for n, r in pending if _same(r['from'], current, layout)]
            if len(ready) > 1:
                raise BaselineError(
                    '%s is rebaselined from the same value by more than one pull request (%s): '
                    'two changes moved the same benchmark without seeing each other. '
                    'Re-measure on top of both and keep one rebaseline.'
                    % (where, ', '.join('pr/%d.json' % n for n, _ in ready)))
            if not ready:
                number, row = pending[0]
                raise BaselineError(
                    'pr/%d.json rebaselines %s from %s, but the row is now %s: another merged '
                    'change moved it first. Re-measure on top of it and update the rebaseline.'
                    % (number, where, json.dumps(row['from'], sort_keys=True),
                       json.dumps(from_row(current, layout), sort_keys=True)))
            number, row = ready[0]
            rows[key][bench][cores] = {k: v for k, v in row.items()
                                       if k not in ('from', 'reason')}
            pending.remove(ready[0])
    return rows, notes


def load(root=ROOT, exclude=None, layout=None):
    """Everything perf-gate.py needs: tolerance, floor and the resolved rows. `exclude` leaves
    one pull request's overlay out (see perf-gate.py: a stale overlay of one's own)."""
    policy = load_policy(root, layout)
    rows, notes = resolve(load_base(root, layout),
                          [o for o in load_overlays(root, layout) if o[0] != exclude],
                          policy['tolerance'], layout)
    return {'tolerance': policy['tolerance'], 'floor': policy['floor'], 'platforms': rows,
            'notes': notes, 'policy': policy}


def write_base(root, rows):
    directory = Path(root) / 'base'
    directory.mkdir(parents=True, exist_ok=True)
    wanted = {'%s.json' % key for key in rows}
    for path in directory.glob('*.json'):
        if path.name not in wanted:
            path.unlink()
    for key, benches in rows.items():
        (directory / ('%s.json' % key)).write_text(dump(benches))


def fold(root=ROOT, layout=None):
    """Write the resolved rows into base/ and delete the overlays. Returns the overlays
    folded. Verifies the gate reads the same baseline afterwards."""
    root = Path(root)
    overlays = load_overlays(root, layout)
    tolerance = load_policy(root, layout)['tolerance']
    before, _ = resolve(load_base(root, layout), overlays, tolerance, layout)
    if not overlays:
        return []
    write_base(root, before)
    for number, _ in overlays:
        (root / 'pr' / ('%d.json' % number)).unlink()
    after, _ = resolve(load_base(root, layout), load_overlays(root, layout), tolerance, layout)
    if after != before:
        raise BaselineError('folding changed the resolved baseline; nothing may be committed')
    return [number for number, _ in overlays]


def write_overlay(root, number, calibrate=None, rebaseline=None, reason=None, layout=None):
    """Add rows to pr/<number>.json, creating it if needed. A row written again replaces
    the earlier one, so re-running the calibration after another CI round is safe.

    `reason` is stamped on every rebaseline row written that does not carry its own. A
    row re-measured without one keeps the reason it already had -- the same row, moved for
    the same cause -- but a row new to this overlay gets no inherited explanation: with
    neither, validation fails and says so."""
    path = Path(root) / 'pr' / ('%d.json' % number)
    overlay = _read_json(path) if path.exists() else {'pr': number}
    for kind, tree in (('calibrate', calibrate), ('rebaseline', rebaseline)):
        for key, bench, cores, row in _rows(kind, tree or {}, layout):
            if kind == 'rebaseline' and 'reason' not in row:
                earlier = overlay.get('rebaseline', {}).get(key, {}).get(bench, {}).get(cores, {})
                if reason or earlier.get('reason'):
                    row = dict(row, reason=reason or earlier['reason'])
            overlay.setdefault(kind, {}).setdefault(key, {}).setdefault(bench, {})[cores] = row
            # One row is one kind: a row calibrated earlier on this branch and now moved
            # again is still a calibration, and a stale entry of the other kind would
            # contradict it.
            other = overlay.get('rebaseline' if kind == 'calibrate' else 'calibrate', {})
            other.get(key, {}).get(bench, {}).pop(cores, None)
    for kind in ('calibrate', 'rebaseline'):
        tree = overlay.get(kind, {})
        for key in list(tree):
            for bench in list(tree[key]):
                if not tree[key][bench]:
                    del tree[key][bench]
            if not tree[key]:
                del tree[key]
        if kind in overlay and not tree:
            del overlay[kind]
    validate_overlay(number, overlay, path.name, layout)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(dump(overlay))
    return path


# The calibrator's arithmetic, shared by every layout (calibrate-perf-baseline.py for this
# one, scripts/flutter-bench/flutter_baseline.py for the Flutter benchmark), so the rules
# for what a run writes cannot drift between two copies.
SPREAD_MARGIN = 1.5
# Fewer runs than this cannot estimate a row's spread: EPYC 7763's hello row, from two
# runs, failed unchanged code at +15.7% against a 15% tolerance. Such a row takes the
# widest tolerance its benchmark needed anywhere, as a single-run row always did.
MIN_RUNS_FOR_OWN_SPREAD = 5


def round_up(value, step=0.05):
    return round(math.ceil(value / step - 1e-9) * step, max(2, -int(math.floor(math.log10(step)))))


def spread_tolerance(values, base, floor, margin=SPREAD_MARGIN, step=0.05):
    """The tolerance a set of runs needs to pass themselves, or `floor` if smaller."""
    if len(values) < 2:
        return floor
    spread = max(max(values) / base - 1, 1 - min(values) / base)
    return max(floor, round_up(spread * margin, step))


def calibration_context(root, number, layout=None):
    """What a calibration of pull request `number` measures against.

    judged   the rows the gate judged the runs by (this pull request's overlay included)
    others   the baseline without this pull request's overlay -- what a rebaseline's
             "from" names, since a re-measurement replaces this pull request's earlier
             rebaseline rather than stacking on it
    stale    this pull request's own rebaselines whose "from" another merged change moved

    Resolving `others` raises when another overlay chains its rebaseline ON TOP of this
    pull request's (its "from" is our value), and that is deliberate rather than a gap:
    re-measuring a link something else was measured on would move the value that later
    link starts from and make IT stale. Recalibrate a pull request nothing has built on."""
    layout = layout or SELFHOST
    policy = load_policy(root, layout)
    base = load_base(root, layout)
    overlays = load_overlays(root, layout)
    own = dict(overlays).get(number, {})
    others, _ = resolve(base, [o for o in overlays if o[0] != number], policy['tolerance'],
                        layout)
    try:
        judged, _ = resolve(base, overlays, policy['tolerance'], layout)
    except BaselineError:
        # This pull request's own overlay went stale (another merged change moved a row it
        # rebaselines), and the gate then judged the run without it. Do the same, and
        # rewrite every stale row from the fresh runs -- which is the recovery the
        # stale-overlay message asks for.
        judged = others
    stale = set()
    for key, bench, cores, row in _rows('own', own.get('rebaseline', {}), layout):
        now = others.get(key, {}).get(bench, {}).get(cores)
        if now is None or not _same(row['from'], now, layout):
            stale.add((key, bench, cores))
    return {'layout': layout, 'number': number, 'policy': policy, 'own': own,
            'others': others, 'judged': judged, 'stale': stale}


def plan_calibration(context, runs, verdict, everything=False, metric=None,
                     margin=SPREAD_MARGIN, min_runs=MIN_RUNS_FOR_OWN_SPREAD):
    """(calibrate, rebaseline) trees for `runs`, {key: {(bench, cores): {metric: [value
    per run]}}}, each value the run's median. `verdict(value, base, tolerance, floor)`
    is the gate's own judgement ('ok' or not).

    A row the baseline lacks is calibrated; a row a run put outside its tolerance, either
    way, is rebaselined (only the metric that moved, unless `everything`); `metric` limits
    a re-measurement to one metric. A row's baseline is the median of the runs' values and
    its tolerance max(the policy's, `margin` x the observed spread in either direction).
    Fewer than `min_runs` runs cannot estimate a spread: a new row then takes the widest
    tolerance the same benchmark needed anywhere, a re-measured one keeps at least its
    own."""
    layout = context['layout']
    tolerance = context['policy']['tolerance']
    floor = context['policy']['floor']
    judged, others, own, stale = (context['judged'], context['others'], context['own'],
                                  context['stale'])
    widest = {}   # layout.widest_key(...) -> widest tolerance any row has
    for _, bench, cores, row in _rows('baseline', judged, layout):
        for name, tol in row.get('tolerance', {}).items():
            where = layout.widest_key(bench, cores, name)
            widest[where] = max(widest.get(where, 0.0), tol)
    for key, benches in runs.items():
        for (bench, cores), metrics in benches.items():
            default = layout.row_default(tolerance, key, bench, cores)
            step = layout.tolerance_step(key, bench, cores)
            for name, values in metrics.items():
                own_tol = spread_tolerance(values, statistics.median(values), default[name],
                                           margin, step)
                if len(values) > 1:
                    where = layout.widest_key(bench, cores, name)
                    widest[where] = max(widest.get(where, 0.0), own_tol)

    calibrate, rebaseline = {}, {}
    for key, benches in sorted(runs.items()):
        for (bench, cores), metrics in sorted(benches.items()):
            default = layout.row_default(tolerance, key, bench, cores)
            lowest = layout.row_default(floor, key, bench, cores)
            step = layout.tolerance_step(key, bench, cores)
            current = judged.get(key, {}).get(bench, {}).get(cores)
            before = others.get(key, {}).get(bench, {}).get(cores)
            # This pull request's own calibration counts only while it is the row: once
            # another pull request's calibration of the same CPU has been folded into base/,
            # resolve() supersedes ours, and writing a calibration again would be ignored
            # just the same -- the gate would fail forever. Then the row is base/'s, and a
            # move past it is a rebaseline FROM it (write_overlay drops the stale entry).
            new_row = current is None or (
                before is None and cores in own.get('calibrate', {}).get(key, {}).get(bench, {}))
            row = {} if current is None else copy.deepcopy(current)
            moved = []
            for name in layout.metrics:
                values = metrics[name]
                if metric and name != metric and not new_row:
                    continue
                if not (everything or new_row or (key, bench, cores) in stale or any(
                        verdict(v, current[name],
                                current.get('tolerance', {}).get(name, default[name]),
                                lowest[name]) != 'ok' for v in values)):
                    continue
                moved.append(name)
                median = statistics.median(values)
                row[name] = layout.round_value(median)
                tol = spread_tolerance(values, median, default[name], margin, step)
                if len(values) < min_runs:
                    kept = (current or {}).get('tolerance', {}).get(name, 0.0)
                    tol = max(tol, kept, widest.get(layout.widest_key(bench, cores, name), 0.0)
                              if new_row else 0.0)
                row.setdefault('tolerance', {})[name] = tol
            if not moved:
                continue
            # A row whose tolerance is just the global one does not need to repeat it.
            tol = {m: t for m, t in row.pop('tolerance', {}).items() if t > default[m]}
            if tol:
                row['tolerance'] = tol
            row['runs'] = min(len(metrics[m]) for m in moved)
            if new_row or before is None:
                calibrate.setdefault(key, {}).setdefault(bench, {})[cores] = row
            else:
                row['from'] = from_row(before, layout)
                rebaseline.setdefault(key, {}).setdefault(bench, {})[cores] = row
    return calibrate, rebaseline


def finish_calibration(root, context, calibrate, rebaseline, reason=None):
    """Write what plan_calibration decided into the pull request's overlay, refusing (with
    SystemExit, naming the rows) what cannot be written. Returns the overlay's path, or
    None when every measured row was inside its tolerance."""
    layout, number, own = context['layout'], context['number'], context['own']
    left = sorted(context['stale'] - {(k, b, c) for k, benches in rebaseline.items()
                                      for b, per in benches.items() for c in per})
    if left:
        raise SystemExit('These rows of pr/%d.json are stale and these runs did not measure '
                         'them; pass runs that do: %s' % (number, ', '.join(
                             '%s %s/%s' % row for row in left)))
    if not calibrate and not rebaseline:
        print('Every measured row is inside its tolerance; nothing to write.')
        return None
    if not reason:
        # A row this overlay already rebaselined keeps its reason when re-measured; a row
        # NEW to it needs its own -- never an earlier row's explanation by default.
        unexplained = ['%s %s/%s' % (key, bench, cores)
                       for key, bench, cores, _ in _rows('new', rebaseline, layout)
                       if not own.get('rebaseline', {}).get(key, {}).get(bench, {})
                       .get(cores, {}).get('reason')]
        if unexplained:
            raise SystemExit('These rows moved past their tolerance and would be rebaselined; '
                             'say why with --reason: %s\n%s' % (', '.join(unexplained),
                                                              json.dumps(rebaseline, indent=1)))
    try:
        path = write_overlay(root, number, calibrate, rebaseline, reason, layout)
        load(root, layout=layout)   # the overlay must resolve against everything else
    except BaselineError as error:
        raise SystemExit(str(error))
    for kind, tree in (('calibrated', calibrate), ('rebaselined', rebaseline)):
        for key, benches in sorted(tree.items()):
            print('%s %s: %s' % (kind, key, ', '.join(sorted(benches))))
    print('Wrote %s -- commit it with this pull request.' % path)
    return path


def pr_number():
    """This pull request's number, from the CI event or the GitHub CLI; None when unknown."""
    explicit = os.environ.get('CN1_PR_NUMBER', '')
    if explicit.isdigit():
        return int(explicit)
    event = os.environ.get('GITHUB_EVENT_PATH')
    if event and Path(event).is_file():
        try:
            number = (json.loads(Path(event).read_text()).get('pull_request') or {}).get('number')
            if number:
                return int(number)
        except (ValueError, OSError):
            pass
    match = re.match(r'^refs/pull/(\d+)/', os.environ.get('GITHUB_REF', ''))
    if match:
        return int(match.group(1))
    # Most of these workflows are run by workflow_dispatch on a branch, which carries no
    # pull request; ask GitHub which pull request the branch belongs to.
    branch = os.environ.get('GITHUB_REF_NAME') if os.environ.get('GITHUB_ACTIONS') else None
    try:
        out = subprocess.run(['gh', 'pr', 'view'] + ([branch] if branch else []) +
                             ['--json', 'number', '-q', '.number'],
                             capture_output=True, text=True, timeout=30, cwd=str(REPO))
        if out.returncode == 0 and out.stdout.strip().isdigit():
            return int(out.stdout.strip())
    except (OSError, subprocess.SubprocessError):
        pass
    return None


def platform_of(key):
    return key.split('@', 1)[0]


def summary(rows):
    """The Port Status page's ParparVM vs JDK 25 table: per platform and benchmark, the
    median ratio across the platform's calibrated CPU models and the range they span.
    Only the default all-cores rows are published."""
    platforms = {}
    for key, benches in rows.items():
        platforms.setdefault(platform_of(key), {})[key] = benches
    order = [p for p in PLATFORM_ORDER if p in platforms] + \
        sorted(p for p in platforms if p not in PLATFORM_ORDER)
    out = []
    for platform in order:
        keys = platforms[platform]
        entry = {'id': platform, 'name': PLATFORM_NAMES.get(platform, platform),
                 'cpus': sorted(k.split('@', 1)[1] if '@' in k else 'unidentified CPU'
                                for k in keys),
                 'benchmarks': {}}
        for bench, _, _ in BENCHMARKS:
            # Which CPU models THIS cell covers. A model can be calibrated for some
            # benchmarks and not yet others, and a cell's median taken over a different set
            # than the platform's list would shift silently as models are filled in.
            contributing = sorted((k, benches[bench]['all']) for k, benches in keys.items()
                                  if 'all' in benches.get(bench, {}))
            values = [v for _, v in contributing]
            if not values:
                continue
            entry['benchmarks'][bench] = {
                metric: {'median': round(statistics.median(v[metric] for v in values), 3),
                         'min': min(v[metric] for v in values),
                         'max': max(v[metric] for v in values)}
                for metric in METRICS}
            entry['benchmarks'][bench]['cpus'] = [
                k.split('@', 1)[1] if '@' in k else 'unidentified CPU' for k, _ in contributing]
        out.append(entry)
    return {'schema_version': 1,
            'source': 'vm/selfhost/perf-baseline',
            'benchmarks': [{'id': b, 'name': n, 'description': d} for b, n, d in BENCHMARKS],
            'platforms': out}


def changed_files(base_ref, root=ROOT):
    rel = Path(root).resolve().relative_to(REPO).as_posix()
    out = subprocess.run(['git', 'diff', '--name-only', '--no-renames', '%s...HEAD' % base_ref,
                          '--', rel], capture_output=True, text=True, cwd=str(REPO))
    if out.returncode != 0:
        raise BaselineError('git diff against %s failed: %s' % (base_ref, out.stderr.strip()))
    return [line[len(rel) + 1:] for line in out.stdout.splitlines() if line.strip()]


def exists_at(ref, path, root=ROOT):
    """Whether `path` (relative to root) exists in commit `ref`."""
    rel = Path(root).resolve().relative_to(REPO).as_posix()
    return subprocess.run(['git', 'cat-file', '-e', '%s:%s/%s' % (ref, rel, path)],
                          capture_output=True, cwd=str(REPO)).returncode == 0


def legacy_touched(base_ref, path=None):
    """Whether the pull request changed the retired single-file baseline (or, for another
    layout, its retired path)."""
    out = subprocess.run(['git', 'diff', '--name-only', '%s...HEAD' % base_ref, '--',
                          path or LEGACY_FILE], capture_output=True, text=True, cwd=str(REPO))
    if out.returncode != 0:
        raise BaselineError('git diff against %s failed: %s' % (base_ref, out.stderr.strip()))
    return bool(out.stdout.strip())


def check(root=ROOT, base_ref=None, number=None, layout=None):
    """Problems with the baselines as a list of strings; empty when the gate can use them.
    With base_ref, also what a pull request may change: its own overlay and the policy,
    never base/ (fold's alone) and never another pull request's overlay."""
    layout = layout or SELFHOST
    problems = []
    try:
        data = load(root, layout=layout)
        for note in data['notes']:
            print('note: ' + note)
    except BaselineError as error:
        problems.append(str(error))
    if base_ref:
        try:
            changed = changed_files(base_ref, root)
        except BaselineError as error:
            return problems + [str(error)]
        # The change that introduced this layout creates base/; that is the one pull
        # request allowed to, and it is recognisable by base/ not existing before it.
        migrating = not exists_at(base_ref, layout.migration_marker, root)
        for path in changed:
            if path.startswith('base/') and not migrating:
                problems.append('%s changed: base/ is written only by the nightly fold. Put the '
                                'rows in pr/<this pull request>.json instead '
                                '(%s writes it).' % (path, layout.calibrate_command))
            elif path.startswith('pr/') and number is not None and path != 'pr/%d.json' % number \
                    and not exists_at(base_ref, path, root):
                # An overlay already on the base branch belongs to a MERGED pull request, and
                # editing or deleting it is how master is repaired: two pull requests can each
                # pass while rebaselining the same row (neither sees the other's unmerged
                # overlay), and once both merge, resolve() and the fold both refuse master
                # until one of them is changed. Only another OPEN pull request's overlay --
                # one this branch would be inventing -- is off limits.
                problems.append('%s changed: a pull request writes only its own overlay, '
                                'pr/%d.json, or repairs one already merged' % (path, number))
        touched = legacy_touched(base_ref) if layout is SELFHOST else \
            legacy_touched(base_ref, layout.legacy)
        if not migrating and touched:
            # A branch from before this layout that resolves its merge conflict by keeping
            # the old file would pass every other check, and the gate -- which reads only
            # perf-baseline/ -- would silently ignore the rows it meant to add.
            problems.append(layout.legacy_message)
    return problems


def _git_show(ref, path):
    out = subprocess.run(['git', 'show', '%s:%s' % (ref, path)], capture_output=True,
                         text=True, cwd=str(REPO))
    if out.returncode != 0:
        raise BaselineError('%s has no %s' % (ref, path))
    return json.loads(out.stdout)


def legacy_from_ref(ref):
    """(the branch's perf-baseline.json, the one it branched from). Only the difference
    between the two is the branch's own: a branch that predates a master commit to the old
    file carries master's PREVIOUS value for that row, which is not an edit of its own."""
    out = subprocess.run(['git', 'merge-base', ref, 'HEAD'], capture_output=True, text=True,
                         cwd=str(REPO))
    if out.returncode != 0:
        raise BaselineError('no merge base between %s and HEAD' % ref)
    try:
        original = _git_show(out.stdout.strip(), LEGACY_FILE)
    except BaselineError:
        original = {'platforms': {}}   # the branch created the file: every row is its own
    return _git_show(ref, LEGACY_FILE), original


def import_legacy(root, number, legacy, original=None, reason=None):
    """Turn a branch's edits to the old single perf-baseline.json into its overlay. `legacy`
    is the branch's copy, `original` the copy it branched from; only rows that differ
    between the two are the branch's. Rows the resolved baseline lacks become calibrations,
    rows it has become rebaselines from their current value. Returns (path or None, notes)."""
    current = load(root)['platforms']
    before = (original or {}).get('platforms')
    calibrate, rebaseline, notes, conflicts = {}, {}, [], []
    # The old file also carried the policy (tolerance, floor). An overlay holds rows only, so
    # a branch that changed the policy would otherwise import as "no change" and lose it once
    # check() rejects the retired file. policy.json is one small file a pull request may edit
    # directly, so the edit is refused here with that instruction rather than migrated.
    if original is not None:
        moved_policy = [name for name in ('tolerance', 'floor')
                        if legacy.get(name) != original.get(name)]
        if moved_policy:
            raise BaselineError(
                'the branch changed the gate policy (%s) in the old file; overlays carry rows '
                'only. Make the same change in vm/selfhost/perf-baseline/policy.json in the '
                'pull request, then import the rows again.' % ', '.join(moved_policy))
    if before is not None:
        # The old calibrator's --fresh dropped every row a run did not re-measure, so a
        # branch's file can DELETE rows. An overlay has no way to say that, and importing
        # only the rows still present would report "no change" while the rows the branch
        # meant to retire stay active. Refuse, and say what to do instead.
        after = legacy.get('platforms', {})
        dropped = ['%s %s/%s' % (key, bench, cores) for key, bench, cores, _ in _rows(
            'the original perf-baseline.json', before)
            if cores not in after.get(key, {}).get(bench, {})]
        if dropped:
            raise BaselineError(
                'the branch deleted %d row(s) that overlays cannot delete: %s. Re-measure '
                'them with calibrate-perf-baseline.py --all instead, or ask for a removal '
                'from base/ in its own change.' % (len(dropped), ', '.join(dropped[:8]) +
                                                   (' ...' if len(dropped) > 8 else '')))
    for key, bench, cores, row in _rows('the branch perf-baseline.json', legacy.get('platforms', {})):
        _check_row('%s %s/%s' % (key, bench, cores), row)
        if before is not None and before.get(key, {}).get(bench, {}).get(cores) == row:
            continue   # not this branch's edit
        existing = current.get(key, {}).get(bench, {}).get(cores)
        old = (before or {}).get(key, {}).get(bench, {}).get(cores)
        if existing is None:
            calibrate.setdefault(key, {}).setdefault(bench, {})[cores] = row
        elif before is not None and old is None:
            # The branch calibrated a row master has calibrated since, from its own runs:
            # two calibrations of one CPU, and the one already in the tree stands.
            notes.append('%s %s/%s: calibrated on master too; master\'s row stands'
                         % (key, bench, cores))
        else:
            merged = row if old is None else _merge_fields(old, row, existing,
                                                           '%s %s/%s' % (key, bench, cores),
                                                           conflicts)
            if merged is not None and merged != existing:
                moved = dict(merged, **{'from': from_row(existing)})
                rebaseline.setdefault(key, {}).setdefault(bench, {})[cores] = moved
    if conflicts:
        raise BaselineError('the branch and master both changed these fields since the branch '
                            'point, differently; re-measure them on top of master instead of '
                            'importing:\n  ' + '\n  '.join(conflicts))
    if not calibrate and not rebaseline:
        return None, notes
    return write_overlay(root, number, calibrate, rebaseline, reason), notes


def _flat(row):
    flat = {f: row[f] for f in ('time', 'memory', 'runs')}
    for metric, value in row.get('tolerance', {}).items():
        flat['tolerance.' + metric] = value
    return flat


def _merge_fields(old, branch, master, where, conflicts):
    """Three-way merge of one row, field by field: the branch's value where only the
    branch changed it, master's everywhere else. Copying the branch's whole row would
    silently revert whatever master changed in the same row since the branch point -- a
    tolerance master added for time, say, under a branch that only re-measured memory."""
    o, b, m = _flat(old), _flat(branch), _flat(master)
    merged = {}
    for field in sorted(set(o) | set(b) | set(m)):
        ov, bv, mv = o.get(field), b.get(field), m.get(field)
        if bv == ov:
            value = mv
        elif mv in (ov, bv):
            value = bv
        else:
            conflicts.append('%s %s: branch %r, master %r (was %r)' % (where, field, bv, mv, ov))
            continue
        if value is not None:
            merged[field] = value
    if any(c.startswith(where + ' ') for c in conflicts):
        return None
    row = {f: merged[f] for f in ('time', 'memory', 'runs')}
    tolerance = {f.split('.', 1)[1]: v for f, v in merged.items() if f.startswith('tolerance.')}
    if tolerance:
        row['tolerance'] = tolerance
    return row


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--root', default=str(ROOT))
    sub = parser.add_subparsers(dest='command', required=True)
    p = sub.add_parser('check', help='validate the baselines (and, with --base, a PR diff)')
    p.add_argument('--base', help='the pull request base commit')
    p.add_argument('--pr', type=int, help='the pull request number')
    sub.add_parser('fold', help='fold every overlay into base/ (nightly, on master)')
    p = sub.add_parser('summary', help='write the Port Status JDK 25 table data')
    p.add_argument('--out', required=True)
    p = sub.add_parser('import-legacy', help="convert a branch's perf-baseline.json edits")
    p.add_argument('--pr', type=int)
    p.add_argument('--reason')
    p.add_argument('--ref', help='the branch, read from git with its merge base (preferred)')
    p.add_argument('--legacy', help="the branch's perf-baseline.json, if not using --ref")
    p.add_argument('--original', help='the perf-baseline.json the branch started from')
    args = parser.parse_args(argv)
    root = Path(args.root)
    try:
        if args.command == 'check':
            problems = check(root, args.base, args.pr)
            for problem in problems:
                print('perf-baseline: ' + problem)
            if problems:
                return 1
            print('perf-baseline: OK')
        elif args.command == 'fold':
            folded = fold(root)
            print('perf-baseline: folded %s' % (', '.join('pr/%d.json' % n for n in folded)
                                                 if folded else 'nothing'))
        elif args.command == 'summary':
            out = Path(args.out)
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(dump(summary(load(root)['platforms'])))
            print('perf-baseline: wrote %s' % out)
        elif args.command == 'import-legacy':
            number = args.pr or pr_number()
            if number is None:
                raise BaselineError('no pull request number: pass --pr')
            if args.ref:
                legacy, original = legacy_from_ref(args.ref)
            elif args.legacy:
                legacy = _read_json(Path(args.legacy))
                original = _read_json(Path(args.original)) if args.original else None
            else:
                raise BaselineError('pass --ref BRANCH, or --legacy FILE [--original FILE]')
            path, notes = import_legacy(root, number, legacy, original, args.reason)
            for note in notes:
                print('perf-baseline: note: ' + note)
            print('perf-baseline: %s' % (path or "the branch changed no baseline row"))
    except BaselineError as error:
        print('perf-baseline: ' + str(error))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
