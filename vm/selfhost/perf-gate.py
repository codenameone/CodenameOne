#!/usr/bin/env python3
"""ParparVM against JDK 25, per benchmark, as ratios only, gated against checked-in baselines.

    python3 vm/selfhost/perf-gate.py [--cores 1,2,4] [--rounds 5] [--out results.json]
                                     [--markdown section.md] [--hello-workload record.jsonl]

Every platform's own CI build runs this after it has built and run its application, so a
performance regression fails THAT build and shows in THAT platform's pull-request comment,
naming the benchmark that regressed and by how much.

THE BENCHMARKS
  hello        the translator translating this platform's HelloCodenameOne application --
               exactly the translation the build just performed (--hello-workload, the
               file CleanTargetIntegrationTest.recordTranslation writes), or on macOS the
               corpus prepare-hello-corpus.py snapshots from the macOS build
  translator   the translator translating itself (JavaAPI + ASM + its own classes)
  <workload>   each of vm/benchmarks' Bench workloads (intArithmetic ... quicksort), run
               one per process so each gets its own peak-memory figure

For the two translation benchmarks the ParparVM arm is the self-hosted translator
(build-selfhost.sh -O3) and the JDK arm runs the same translator classes; for the
workloads it is the Bench binary (build-bench.sh -O3) against the same Bench classes on
JDK 25.

HOW A RATIO IS MADE
Each round runs both arms back to back, alternating which goes first, and yields a
PAIRED ratio (ParparVM over the JDK run beside it); the result is the median of those.
Nothing absolute is printed or recorded: a slow minute on a shared runner slows both
halves of a pair, so JDK 25 is the unit of measure and one baseline holds on a fast runner
and a slow one. A ratio below 1.00x means ParparVM is faster (time) or smaller (RAM).

  time   translation: wall time of the whole process. Workloads: the fastest measured
         repetition inside the process (Bench's own ns timing), so JVM startup does not
         count against the JDK and the JIT gets Bench's warmup.
  RAM    peak memory of the whole process: peak footprint on macOS, maximum RSS on Linux,
         peak working set on Windows.

Every run is verified: translation output byte for byte against the first run, workload
checksums across arms and rounds. A ratio can never come from doing less work.

A ratio more than the tolerance above its baseline in perf-baseline.json is a regression
and the exit status is 1. A benchmark with no baseline is reported, not gated, and the
report carries the entry to add.

Core counts are enforced with CPU affinity on Linux and Windows (inherited by the child)
and by CN1_GC_MARK_THREADS plus -XX:ActiveProcessorCount everywhere. macOS has no affinity
API, so there a core count is LOGICAL -- both arms are told it, neither is confined to it
-- and the report marks it.
"""
import argparse
import ctypes
import importlib.util
import json
import os
from pathlib import Path
import platform
import re
import shutil
import statistics
import subprocess
import sys
import time

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
TARGET = HERE / 'target'

_spec = importlib.util.spec_from_file_location('bench_selfhost', HERE / 'bench-selfhost.py')
bench = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(bench)

HELLO_APP = 'com_codenameone_examples_hellocodenameone_HelloCodenameOneStub'
HELLO_PKG = 'com.codenameone.examples.hellocodenameone'
TRANSLATOR_APP = 'com_codename1_tools_translator_ByteCodeTranslator'
TRANSLATOR_PKG = 'com.codename1.tools.translator'
# Bench.java's workloads, in its own order. Keep in step with Bench.main.
WORKLOADS = ['intArithmetic', 'longArithmetic', 'mathTranscendental', 'arraySequential',
             'arrayRandom', 'objectAllocation', 'valueEscape', 'hashMapChurn',
             'stringBuilding', 'recursion', 'quicksort']
# Read from the run's stdout log only (bench.error_log holds stderr). The two once shared a
# file, and a runtime diagnostic landing inside a BENCH record both hid records from an
# anchored pattern and split one mid-checksum. Left unanchored anyway: the pattern is
# specific enough, and the program itself may print ahead of a record.
BENCH_LINE = re.compile(r'BENCH (\S+) rep (\d+) ns=(\d+) checksum=(-?\d+)')
PLATFORM_NAMES = {'linux-x64': 'Linux x64', 'linux-arm64': 'Linux arm64',
                  'macos-arm64': 'macOS arm64', 'windows-x64': 'Windows x64',
                  'windows-arm64': 'Windows arm64'}


def platform_key():
    system = {'Linux': 'linux', 'Darwin': 'macos', 'Windows': 'windows'}.get(
        platform.system(), platform.system().lower())
    machine = platform.machine().lower()
    arch = {'x86_64': 'x64', 'amd64': 'x64', 'aarch64': 'arm64', 'arm64': 'arm64'}.get(
        machine, machine)
    return '%s-%s' % (system, arch)


def available_cores():
    if hasattr(os, 'sched_getaffinity'):
        return len(os.sched_getaffinity(0))
    return os.cpu_count() or 1


def _kernel32():
    """kernel32 with its signatures DECLARED. ctypes defaults every result to a 32-bit
    int, so GetCurrentProcess's pseudo-handle (-1) came back truncated and
    GetProcessAffinityMask then failed on an invalid 64-bit handle."""
    from ctypes import wintypes
    kernel32 = ctypes.WinDLL('kernel32', use_last_error=True)
    kernel32.GetCurrentProcess.restype = wintypes.HANDLE
    kernel32.GetCurrentProcess.argtypes = []
    kernel32.GetProcessAffinityMask.restype = wintypes.BOOL
    kernel32.GetProcessAffinityMask.argtypes = [wintypes.HANDLE, ctypes.POINTER(ctypes.c_size_t),
                                                ctypes.POINTER(ctypes.c_size_t)]
    kernel32.SetProcessAffinityMask.restype = wintypes.BOOL
    kernel32.SetProcessAffinityMask.argtypes = [wintypes.HANDLE, ctypes.c_size_t]
    return kernel32


class Affinity:
    """Confine THIS process to the first n cores for the duration of a run; the child
    inherits the mask on Linux and Windows. A no-op on macOS, which has no such API."""

    def __init__(self, cores):
        self.cores = cores
        self.saved = None
        self.enforced = False

    def __enter__(self):
        system = platform.system()
        if system == 'Linux':
            self.saved = os.sched_getaffinity(0)
            os.sched_setaffinity(0, sorted(self.saved)[:self.cores])
            self.enforced = True
        elif system == 'Windows':
            kernel32 = _kernel32()
            process = kernel32.GetCurrentProcess()
            proc_mask = ctypes.c_size_t()
            sys_mask = ctypes.c_size_t()
            if not kernel32.GetProcessAffinityMask(process, ctypes.byref(proc_mask),
                                                   ctypes.byref(sys_mask)):
                raise RuntimeError('GetProcessAffinityMask failed (error %d)' % ctypes.get_last_error())
            self.saved = proc_mask.value
            bits = [b for b in range(64) if self.saved & (1 << b)][:self.cores]
            if not kernel32.SetProcessAffinityMask(process, sum(1 << b for b in bits)):
                raise RuntimeError('SetProcessAffinityMask failed (error %d)' % ctypes.get_last_error())
            self.enforced = True
        return self

    def __exit__(self, *exc):
        system = platform.system()
        if self.saved is not None and system == 'Linux':
            os.sched_setaffinity(0, self.saved)
        elif self.saved is not None and system == 'Windows':
            kernel32 = _kernel32()
            kernel32.SetProcessAffinityMask(kernel32.GetCurrentProcess(), self.saved)
        return False


def base_env(cores):
    """cores None is the CI default: every CPU the runner has, and each runtime's own
    default thread counts -- the configuration an application actually runs in."""
    env = {k: v for k, v in os.environ.items() if not k.startswith('CN1_')}
    env['LC_ALL'] = 'C'
    env['CN1_RESOURCE_PATH'] = str(REPO / 'vm/ByteCodeTranslator/src')
    if cores is not None:
        env['CN1_GC_MARK_THREADS'] = str(cores)
    return env


def jvm_cores(cores):
    return [] if cores is None else ['-XX:ActiveProcessorCount=%d' % cores]


def cores_text(cores):
    return 'all cores' if cores is None else '%d cores' % cores


def interleave(cores, rounds, arms, run_one, work, tag):
    """A discarded warmup round, then `rounds` interleaved pairs. run_one(name, prefix,
    log) runs one arm and returns (time, peak bytes). Returns the paired ratio lists."""
    time_ratios, memory_ratios = [], []
    enforced = False
    for round_index in range(rounds + 1):
        # Alternate who goes first, so neither arm always inherits the other's cache and
        # thermal state.
        order = arms if round_index % 2 == 0 else arms[::-1]
        sample = {}
        for name, prefix in order:
            log = work / ('%s-c%s-r%02d-%s.log' % (tag, 'all' if cores is None else cores,
                                                   round_index, name))
            if cores is None:
                sample[name] = run_one(name, prefix, log)
                enforced = True   # nothing to confine: the whole runner
            else:
                with Affinity(cores) as affinity:
                    sample[name] = run_one(name, prefix, log)
                    enforced = affinity.enforced
        if round_index == 0:
            continue   # warmup: verified, not measured
        time_ratios.append(sample['parpar'][0] / sample['jdk25'][0])
        memory_ratios.append(sample['parpar'][1] / sample['jdk25'][1])
        print('  %s, %s, round %d: time %.3fx, RAM %.3fx'
              % (tag, cores_text(cores), round_index, time_ratios[-1], memory_ratios[-1]),
              flush=True)
    return time_ratios, memory_ratios, enforced


def measure_translation(spec, cores, rounds, binary, java, work):
    host = REPO / 'vm/ByteCodeTranslator/target/classes'
    asm = (REPO / 'vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt').read_text().strip()
    env = base_env(cores)
    arms = [('parpar', [str(binary)]),
            ('jdk25', [java] + jvm_cores(cores) + [
                       '-cp', str(host) + os.pathsep + asm,
                       'com.codename1.tools.translator.ByteCodeTranslator'])]
    out = work / ('out-' + spec['id'])
    system = platform.system()
    expected = spec.setdefault('expected', [None])

    def run_one(name, prefix, log):
        if out.exists():
            shutil.rmtree(out)
        out.mkdir()
        # 'clean' whatever the build translated for: parsing and optimizing the classes is
        # the workload, and clean is the target the self-hosted translator is verified
        # byte-identical on.
        args = ['clean', ';'.join(spec['sources']), str(out), spec['app'], spec['package'],
                spec['app'], '1.0', 'clean', 'none']
        result = bench.run(prefix + args, env, log, system, timeout=1800)
        manifest = bench.output_manifest(out)
        if expected[0] is None:
            expected[0] = manifest
        elif manifest != expected[0]:
            differing = sorted(k for k in set(manifest) | set(expected[0])
                               if manifest.get(k) != expected[0].get(k))
            raise RuntimeError('%s: output divergence at %s (%s): %s'
                               % (spec['id'], cores_text(cores), name, ', '.join(differing[:10])))
        return result['elapsed_seconds'], result['peak_bytes']

    return interleave(cores, rounds, arms, run_one, work, spec['id'])


def measure_workload(spec, cores, rounds, bench_binary, java, reps, work):
    classes = TARGET / 'bench-classes'
    env = base_env(cores)
    name = spec['id']
    arms = [('parpar', [str(bench_binary), str(reps), name]),
            ('jdk25', [java] + jvm_cores(cores) + ['-cp', str(classes),
                       'com.bench.Bench', str(reps), name])]
    system = platform.system()
    expected = spec.setdefault('expected', [None])

    def run_one(arm, prefix, log):
        # A workload process finishes in seconds; one that runs for minutes is hung.
        result = bench.run(prefix, env, log, system, timeout=300)
        lines = [m for m in BENCH_LINE.finditer(log.read_text(errors='replace'))
                 if m.group(1) == name]
        if len(lines) != reps:
            # Say what the log held instead: the run directory is not an artifact, so the
            # message is the only evidence a CI failure leaves.
            text = log.read_text(errors='replace')
            seen = sorted(int(m.group(2)) for m in lines)
            missing = [i for i in range(reps) if i not in seen]
            odd = [l.strip()[:120] for l in text.splitlines()
                   if name in l and not BENCH_LINE.search(l)][:3]
            raise RuntimeError('%s (%s): expected %d measured repetitions, found %d '
                               '(missing %s)%s' % (name, arm, reps, len(lines), missing,
                                                  ('; unparsed: ' + ' | '.join(odd)) if odd else ''))
        checksums = {m.group(4) for m in lines}
        if len(checksums) != 1:
            raise RuntimeError('%s (%s): repetitions disagree on the checksum: %s'
                               % (name, arm, sorted(checksums)))
        checksum = checksums.pop()
        if expected[0] is None:
            expected[0] = checksum
        elif checksum != expected[0]:
            raise RuntimeError('%s: checksum divergence at %s (%s): %s against %s'
                               % (name, cores_text(cores), arm, checksum, expected[0]))
        return min(int(m.group(3)) for m in lines) / 1e9, result['peak_bytes']

    return interleave(cores, rounds, arms, run_one, work, name)


def paired(values):
    return {'median': statistics.median(values), 'min': min(values), 'max': max(values),
            'rounds': values}


def verdict(ratio, base, tolerance, floor=0.0):
    """A regression is a ratio more than `tolerance` above its baseline AND more than
    `floor` above it in absolute terms. The floor is for RAM: a workload whose ParparVM
    footprint is a few MB against the JVM's ~40MB sits at ratios near 0.05, where under a
    megabyte of jitter is a +30% change that means nothing."""
    if base is None:
        return 'uncalibrated'
    if ratio > base * (1 + tolerance) and ratio - base > floor:
        return 'regression'
    if ratio < base * (1 - tolerance):
        return 'improved'
    return 'ok'


def load_hello(args):
    """The hello benchmark's inputs: a recorded translation, else the macOS corpus."""
    if args.hello_workload:
        records = [json.loads(line) for line in Path(args.hello_workload).read_text().splitlines()
                   if line.strip()]
        if args.hello_app:
            records = [r for r in records if r['app'] == args.hello_app]
        if not records:
            raise RuntimeError('%s holds no recorded translation%s' % (
                args.hello_workload, (' of ' + args.hello_app) if args.hello_app else ''))
        record = records[-1]
        missing = [s for s in record['sources'] if not Path(s).exists()]
        if missing:
            raise RuntimeError('the recorded %s translation read inputs that are gone: %s'
                               % (record['app'], ', '.join(missing[:5])))
        return {'id': 'hello', 'label': 'hello (%s)' % record['app'], 'kind': 'translation',
                'sources': record['sources'], 'app': record['app'], 'package': record['package']}
    corpus = TARGET / 'hello-corpus'
    if not corpus.is_dir():
        raise RuntimeError('no hello workload: pass --hello-workload, or run '
                           'prepare-hello-corpus.py for the macOS corpus')
    return {'id': 'hello', 'label': 'hello (HelloCodenameOne)', 'kind': 'translation',
            'sources': [str(TARGET / 'javaapi-classes'), str(corpus)],
            'app': HELLO_APP, 'package': HELLO_PKG}


def fmt_ratio(metric):
    text = '%.2fx' % metric['median']
    if metric['baseline'] is not None:
        change = (metric['median'] / metric['baseline'] - 1) * 100
        text += ' (base %.2fx, %+.1f%%)' % (metric['baseline'], change)
    return text


def status_cell(entry):
    regressed = ['%s %+.1f%%' % ('time' if m == 'time' else 'RAM',
                                 (entry[m]['median'] / entry[m]['baseline'] - 1) * 100)
                 for m in ('time', 'memory') if entry[m]['verdict'] == 'regression']
    if regressed:
        return '**REGRESSION** (%s)' % ', '.join(regressed)
    verdicts = {entry['time']['verdict'], entry['memory']['verdict']}
    if 'uncalibrated' in verdicts:
        return 'not gated (no baseline)'
    if 'improved' in verdicts:
        return 'better than baseline'
    return 'ok'


def _where(report, key):
    """How a row's core count reads in prose."""
    if key == 'all':
        return 'on all %d CPUs' % report['available_cores']
    return 'at %s core%s' % (key, '' if str(key) == '1' else 's')


def _cores_cell(report, key):
    return str(report['available_cores']) if key == 'all' else str(key)


def _cores_order(key):
    return 1 << 30 if key == 'all' else int(key)


def render_markdown(report):
    name = PLATFORM_NAMES.get(report['platform'], report['platform'])
    tol = report['tolerance']
    lines = ['### ParparVM vs HotSpot (JDK 25): %s' % name, '']
    if report.get('error'):
        lines += ['**The performance gate could not complete:** `%s`' % report['error'], '']
    regressions = []
    for bench_id, per_cores in report['results'].items():
        for cores, entry in per_cores.items():
            if 'failed' in entry:
                continue
            for metric in ('time', 'memory'):
                e = entry[metric]
                if e['verdict'] == 'regression':
                    regressions.append('%s %s: %s %.2fx against a %.2fx baseline (%+.1f%%, '
                                       'tolerance %d%%)'
                                       % (report['labels'][bench_id], _where(report, cores),
                                          'time' if metric == 'time' else 'RAM', e['median'],
                                          e['baseline'], (e['median'] / e['baseline'] - 1) * 100,
                                          round(e.get('tolerance', tol[metric]) * 100)))
    failures = report.get('failures') or []
    if failures:
        lines.append('**%d benchmark%s failed to run:**' % (len(failures),
                                                            '' if len(failures) == 1 else 's'))
        lines += ['- %s %s: %s' % (report['labels'].get(f['benchmark'], f['benchmark']),
                                   _where(report, f['cores']), f['reason']) for f in failures]
        lines.append('')
    if regressions:
        lines.append('**%d performance regression%s:**' % (len(regressions),
                                                           '' if len(regressions) == 1 else 's'))
        lines += ['- ' + r for r in regressions]
        lines.append('')
    lines += ['Ratios are **ParparVM / JDK 25**: below 1.00x ParparVM is faster (time) or '
              'smaller (RAM). Median of %d interleaved, paired rounds; every run\'s output '
              'was verified. A regression is a ratio more than %d%% (time) / %d%% (RAM) above '
              'its baseline in `vm/selfhost/perf-baseline.json` (more, for a row whose '
              'calibration runs were noisier; the file records it), and for RAM also more '
              'than 0.05x above it in absolute terms.%s'
              % (report['rounds'], round(tol['time'] * 100), round(tol['memory'] * 100),
                 ' Both run unpinned on all of the runner\'s CPUs, with their own default '
                 'thread counts.' if any('all' in per for per in report['results'].values())
                 else ''), '',
              '| Benchmark | Cores | Time | RAM | Status |', '|---|---:|---|---|---|']
    logical = False
    for bench_id, per_cores in report['results'].items():
        for cores, entry in sorted(per_cores.items(), key=lambda kv: _cores_order(kv[0])):
            if 'failed' in entry:
                lines.append('| %s | %s | - | - | **FAILED**: %s |'
                             % (report['labels'][bench_id], _cores_cell(report, cores),
                                entry['failed']))
                continue
            mark = '' if entry['enforced'] else '*'
            logical = logical or bool(mark)
            lines.append('| %s | %s%s | %s | %s | %s |' % (
                report['labels'][bench_id], _cores_cell(report, cores), mark, fmt_ratio(entry['time']),
                fmt_ratio(entry['memory']), status_cell(entry)))
    if report['skipped_cores']:
        lines += ['', 'Not run at %s cores: this runner has %d.'
                  % (','.join(map(str, report['skipped_cores'])), report['available_cores'])]
    if logical:
        lines += ['', '\\* No CPU affinity on this platform: both arms are told the core count '
                  '(`CN1_GC_MARK_THREADS`, `-XX:ActiveProcessorCount`) but neither is confined to it.']
    if report.get('calibration'):
        lines += ['', '<details><summary>Baseline entry for %s</summary>' % report['platform'], '',
                  '```json', json.dumps({report['platform']: report['calibration']}, indent=1),
                  '```', '', '</details>']
    lines += ['', '**Result: %s**' % (
        'performance regression' if report['regression'] else
        ('gate did not complete' if report.get('error') else
         ('benchmark failed' if report.get('failures') else 'no regression')))]
    return '\n'.join(lines) + '\n'


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    # 'all' in CI: a hosted runner has a fixed handful of CPUs (4 on Linux and Windows,
    # 3 on macOS), pinning a subset of an x64 runner's vCPUs picks hyperthread siblings
    # rather than cores, and macOS cannot pin at all -- so a 1/2/4 sweep there measured
    # the runner's topology, not the VM. A comma list (1,2,4) still sweeps, pinned,
    # on a machine where that means something.
    parser.add_argument('--cores', default='all')
    parser.add_argument('--rounds', type=int, default=5)
    # 25, not Bench's default 5. objectAllocation's repetitions last tens of ms, so at 5
    # whether a collection lands inside the window decides the number: identical work
    # measured 22-51ms (Bench.java), and two CI runs of unchanged code put its ratio 37%
    # apart and failed the gate on noise. Twenty-five consecutive repetitions reach the
    # allocator's steady state, where six processes agreed to 0.8%.
    parser.add_argument('--reps', type=int, default=25,
                        help='measured repetitions per workload process (Bench argv[0])')
    parser.add_argument('--baseline', default=str(HERE / 'perf-baseline.json'))
    parser.add_argument('--platform', default=platform_key())
    parser.add_argument('--out', default=None)
    parser.add_argument('--markdown', default=None)
    parser.add_argument('--binary', default=None)
    parser.add_argument('--bench-binary', default=None)
    parser.add_argument('--java', default=None)
    parser.add_argument('--hello-workload', default=None)
    parser.add_argument('--hello-app', default=None)
    parser.add_argument('--only', default=None, help='comma-separated benchmark ids')
    args = parser.parse_args(argv)
    if args.rounds < 3:
        raise RuntimeError('at least 3 rounds are needed for a median to mean anything')

    exe = '.exe' if platform.system() == 'Windows' else ''
    report = {'platform': args.platform, 'rounds': args.rounds, 'results': {}, 'labels': {},
              'available_cores': available_cores(), 'skipped_cores': [], 'regression': False}
    out = Path(args.out) if args.out else None
    markdown = Path(args.markdown) if args.markdown else None

    def write():
        if out:
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps(report, indent=2) + '\n')
        if markdown:
            markdown.parent.mkdir(parents=True, exist_ok=True)
            markdown.write_text(render_markdown(report))

    baseline = json.loads(Path(args.baseline).read_text())
    report['tolerance'] = baseline['tolerance']
    try:
        binary = Path(args.binary or os.environ.get('CN1_SELFHOST_BIN') or
                      TARGET / ('parpar-O3' + exe)).resolve()
        manifest = json.loads(Path(str(binary) + '.build.json').read_text())
        if manifest['sources'] != bench.sources() or manifest['binary'] != bench.digest(binary):
            raise RuntimeError('stale native build: rebuild with build-selfhost.sh -O3')
        bench_binary = Path(args.bench_binary or TARGET / ('bench-O3' + exe)).resolve()
        if not bench_binary.is_file():
            raise RuntimeError('%s is missing: run build-bench.sh -O3' % bench_binary)
        java = args.java or str(Path(os.environ['JDK_25_HOME']) / 'bin' / ('java' + exe))
        version = subprocess.check_output([java, '-version'], stderr=subprocess.STDOUT, text=True)
        if 'version "25' not in version:
            raise RuntimeError('the reference arm is not JDK 25:\n' + version)

        wanted = set(args.only.split(',')) if args.only else None
        specs = [load_hello(args)] if wanted is None or 'hello' in wanted else []
        specs += [{'id': 'translator', 'label': 'translator (self)', 'kind': 'translation',
                  'sources': [str(TARGET / d) for d in ('javaapi-classes', 'asm-classes', 'classes')],
                  'app': TRANSLATOR_APP, 'package': TRANSLATOR_PKG}]
        specs += [{'id': w, 'label': w, 'kind': 'workload'} for w in WORKLOADS]
        if wanted is not None:
            specs = [s for s in specs if s['id'] in wanted]
        for s in specs:
            report['labels'][s['id']] = s['label']

        tolerance = baseline['tolerance']
        bases = baseline['platforms'].get(args.platform, {})
        have = report['available_cores']
        if args.cores == 'all':
            # None = unpinned, all of the runner's CPUs, each runtime's own defaults.
            cores_list = [None]
        else:
            wanted_cores = [int(c) for c in args.cores.split(',')]
            cores_list = [c for c in wanted_cores if c <= have]
            report['skipped_cores'] = [c for c in wanted_cores if c > have]

        work = TARGET / 'perf-gate' / time.strftime('%Y%m%d-%H%M%S')
        work.mkdir(parents=True)
        print('perf-gate: %s, %d rounds, %s, %d benchmarks (this runner has %d CPUs)'
              % (args.platform, args.rounds,
                 ', '.join(cores_text(c) for c in cores_list), len(specs), have), flush=True)
        calibration = {}
        report['failures'] = []
        for spec in specs:
            for cores in cores_list:
                key = 'all' if cores is None else str(cores)
                # ONE benchmark failing -- a hang, a crash, a divergent output -- fails
                # its own row and the gate, and the rest of the table is still measured.
                # Aborting the whole gate on the first one hid every row after it.
                try:
                    if spec['kind'] == 'translation':
                        times, memories, enforced = measure_translation(
                            spec, cores, args.rounds, binary, java, work)
                    else:
                        times, memories, enforced = measure_workload(
                            spec, cores, args.rounds, bench_binary, java, args.reps, work)
                except (RuntimeError, OSError, subprocess.TimeoutExpired) as error:
                    reason = str(error).strip().splitlines()[0][:200]
                    if isinstance(error, subprocess.TimeoutExpired):
                        reason = 'did not finish within %ds (hung)' % error.timeout
                    report['failures'].append({'benchmark': spec['id'], 'cores': key,
                                               'reason': reason})
                    report['results'].setdefault(spec['id'], {})[key] = {
                        'failed': reason}
                    print('perf-gate: %-20s %s  FAILED: %s' % (spec['id'], cores_text(cores), reason),
                          flush=True)
                    write()
                    continue
                base = bases.get(spec['id'], {}).get(key, {})
                entry = {'enforced': enforced}
                for metric, values in (('time', times), ('memory', memories)):
                    median = statistics.median(values)
                    # A row may carry its own tolerance: where two calibration runs of
                    # unchanged code already disagreed by more than the global one, the
                    # global one would fail on noise alone.
                    row_tolerance = base.get('tolerance', {}).get(metric, tolerance[metric])
                    floor = baseline.get('floor', {}).get(metric, 0.0)
                    entry[metric] = dict(paired(values), baseline=base.get(metric),
                                         tolerance=row_tolerance,
                                         verdict=verdict(median, base.get(metric), row_tolerance,
                                                         floor))
                    if entry[metric]['verdict'] == 'regression':
                        report['regression'] = True
                report['results'].setdefault(spec['id'], {})[key] = entry
                if base.get('time') is None or base.get('memory') is None:
                    calibration.setdefault(spec['id'], {})[key] = {
                        'time': round(entry['time']['median'], 3),
                        'memory': round(entry['memory']['median'], 3)}
                print('perf-gate: %-20s %s  time %s  RAM %s  -> %s'
                      % (spec['id'], cores_text(cores), fmt_ratio(entry['time']), fmt_ratio(entry['memory']),
                         status_cell(entry)), flush=True)
                write()
        report['calibration'] = calibration
    except (RuntimeError, OSError, ValueError, KeyError, subprocess.TimeoutExpired) as error:
        # A gate that could not measure is a failed gate, and it says why in the comment.
        report['error'] = str(error).strip().splitlines()[0][:300]
        write()
        print('perf-gate: REFUSING: %s' % error, flush=True)
        return 2
    write()
    print(render_markdown(report))
    failed = bool(report.get('failures'))
    print('perf-gate: %s' % ('REGRESSION' if report['regression'] else
                             ('FAILED' if failed else 'OK')))
    return 1 if report['regression'] or failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
