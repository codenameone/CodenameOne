#!/usr/bin/env python3
"""ParparVM against JDK 25, as ratios only, gated against checked-in baselines.

    python3 vm/selfhost/perf-gate.py [--cores 1,2,4] [--rounds 5] [--out results.json]

Runs the self-hosted translator and JDK 25 on the same corpus, INTERLEAVED -- each
round runs both arms back to back, alternating which goes first -- once per core
count. Every run's output is compared byte for byte against the first run's, so a
ratio can never come from doing less work.

Nothing absolute is printed or recorded: each round yields a PAIRED ratio (ParparVM
over the JDK run next to it), and the summary is the median of those ratios. Pairing is
what makes a shared CI runner usable at all -- a slow minute slows both halves of a
pair, where it would move a ratio of two independent medians. JDK 25 is the unit of
measure, so the same baseline file holds on a fast runner and a slow one.

A ratio below 1.00 means ParparVM is faster (time) or smaller (peak memory). The gate
fails when a ratio exceeds its baseline by more than the tolerance in the baseline file.
A platform or core count with no baseline is REPORTED, not gated, and the report says so
and prints the entry to calibrate it with.

Core counts are enforced with CPU affinity on Linux and Windows (inherited by the child)
and by CN1_GC_MARK_THREADS plus -XX:ActiveProcessorCount on every platform. macOS has no
affinity API, so there a core count is LOGICAL -- both arms are told the count, neither
is confined to it -- and the report marks it.
"""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import platform
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
            wanted = sorted(self.saved)[:self.cores]
            os.sched_setaffinity(0, wanted)
            self.enforced = True
        elif system == 'Windows':
            import ctypes
            kernel32 = ctypes.windll.kernel32
            process = kernel32.GetCurrentProcess()
            proc_mask = ctypes.c_size_t()
            sys_mask = ctypes.c_size_t()
            if not kernel32.GetProcessAffinityMask(process, ctypes.byref(proc_mask),
                                                   ctypes.byref(sys_mask)):
                raise RuntimeError('GetProcessAffinityMask failed')
            self.saved = proc_mask.value
            bits = [b for b in range(64) if self.saved & (1 << b)][:self.cores]
            mask = sum(1 << b for b in bits)
            if not kernel32.SetProcessAffinityMask(process, ctypes.c_size_t(mask)):
                raise RuntimeError('SetProcessAffinityMask failed')
            self.enforced = True
        return self

    def __exit__(self, *exc):
        system = platform.system()
        if self.saved is not None and system == 'Linux':
            os.sched_setaffinity(0, self.saved)
        elif self.saved is not None and system == 'Windows':
            import ctypes
            kernel32 = ctypes.windll.kernel32
            kernel32.SetProcessAffinityMask(kernel32.GetCurrentProcess(),
                                            ctypes.c_size_t(self.saved))
        return False


def paired(values):
    return {'median': statistics.median(values), 'min': min(values), 'max': max(values),
            'rounds': values}


def measure(cores, rounds, binary, java, corpus, app, package, work, expected):
    """One core count: a discarded warmup round, then `rounds` interleaved pairs.
    Returns (time ratios, memory ratios, affinity enforced). Raises on any failed run
    or divergent output."""
    host = REPO / 'vm/ByteCodeTranslator/target/classes'
    asm = (REPO / 'vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt').read_text().strip()
    javaapi = TARGET / 'javaapi-classes'
    env = {k: v for k, v in os.environ.items() if not k.startswith('CN1_')}
    env['LC_ALL'] = 'C'
    env['CN1_RESOURCE_PATH'] = str(REPO / 'vm/ByteCodeTranslator/src')
    env['CN1_GC_MARK_THREADS'] = str(cores)
    translator_args = ['clean', ';'.join(map(str, [javaapi] + corpus)), None,
                       app, package, app, '1.0', 'clean', 'none']
    arms = [('parpar', [str(binary)]),
            ('jdk25', [java, '-XX:ActiveProcessorCount=%d' % cores,
                       '-cp', str(host) + os.pathsep + asm,
                       'com.codename1.tools.translator.ByteCodeTranslator'])]
    system = platform.system()
    out = work / 'out'
    time_ratios, memory_ratios = [], []
    enforced = False
    for round_index in range(rounds + 1):
        # Alternate who goes first, so neither arm always inherits the other's
        # cache and thermal state.
        order = arms if round_index % 2 == 0 else arms[::-1]
        sample = {}
        for name, prefix in order:
            if out.exists():
                shutil.rmtree(out)
            out.mkdir()
            args = list(translator_args)
            args[2] = str(out)
            log = work / ('c%d-r%02d-%s.log' % (cores, round_index, name))
            with Affinity(cores) as affinity:
                result = bench.run(prefix + args, env, log, system, timeout=900)
                enforced = affinity.enforced
            manifest = bench.output_manifest(out)
            if expected[0] is None:
                expected[0] = manifest
            elif manifest != expected[0]:
                differing = sorted(k for k in set(manifest) | set(expected[0])
                                   if manifest.get(k) != expected[0].get(k))
                raise RuntimeError('output divergence at %d cores (%s): %s'
                                   % (cores, name, ', '.join(differing[:10])))
            sample[name] = result
        if round_index == 0:
            continue   # warmup: verified, not measured
        time_ratios.append(sample['parpar']['elapsed_seconds'] / sample['jdk25']['elapsed_seconds'])
        memory_ratios.append(sample['parpar']['peak_bytes'] / sample['jdk25']['peak_bytes'])
        print('  %d cores, round %d: time %.3fx, memory %.3fx'
              % (cores, round_index, time_ratios[-1], memory_ratios[-1]), flush=True)
    return time_ratios, memory_ratios, enforced


def verdict(ratio, base, tolerance):
    if base is None:
        return 'uncalibrated'
    limit = base * (1 + tolerance)
    if ratio > limit:
        return 'regression'
    if ratio < base * (1 - tolerance):
        return 'improved'
    return 'ok'


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--cores', default='1,2,4')
    parser.add_argument('--rounds', type=int, default=5)
    parser.add_argument('--baseline', default=str(HERE / 'perf-baseline.json'))
    parser.add_argument('--platform', default=platform_key())
    parser.add_argument('--out', default=None)
    parser.add_argument('--binary', default=None)
    parser.add_argument('--java', default=None)
    args = parser.parse_args(argv)
    if args.rounds < 3:
        raise RuntimeError('at least 3 rounds are needed for a median to mean anything')

    binary = Path(args.binary or os.environ.get('CN1_SELFHOST_BIN') or (
        TARGET / ('parpar-O3.exe' if platform.system() == 'Windows' else 'parpar-O3'))).resolve()
    manifest = json.loads(Path(str(binary) + '.build.json').read_text())
    if manifest['sources'] != bench.sources() or manifest['binary'] != bench.digest(binary):
        raise RuntimeError('stale native build: rebuild with build-selfhost.sh -O3')
    java = args.java or str(Path(os.environ['JDK_25_HOME']) / 'bin' / (
        'java.exe' if platform.system() == 'Windows' else 'java'))
    version = subprocess.check_output([java, '-version'], stderr=subprocess.STDOUT, text=True)
    if 'version "25' not in version:
        raise RuntimeError('the reference arm is not JDK 25:\n' + version)

    hello = TARGET / 'hello-corpus'
    if not hello.is_dir():
        raise RuntimeError('%s is missing: run prepare-hello-corpus.py first. The gate '
                           'measures the HelloCodenameOne application, never the '
                           'translator\'s own classes.' % hello)
    corpus = [hello]

    baseline = json.loads(Path(args.baseline).read_text())
    tolerance = baseline['tolerance']
    bases = baseline['platforms'].get(args.platform, {})
    have = available_cores()
    wanted = [int(c) for c in args.cores.split(',')]
    cores_list = [c for c in wanted if c <= have]
    skipped = [c for c in wanted if c > have]

    work = TARGET / 'perf-gate' / time.strftime('%Y%m%d-%H%M%S')
    work.mkdir(parents=True)
    report = {'platform': args.platform, 'rounds': args.rounds,
              'tolerance': tolerance, 'available_cores': have,
              'skipped_cores': skipped, 'results': {}}
    expected = [None]
    print('perf-gate: %s, %d rounds per core count, cores %s%s'
          % (args.platform, args.rounds, cores_list,
             (' (skipping %s: this host has %d)' % (skipped, have)) if skipped else ''))
    failed = False
    for cores in cores_list:
        times, memories, enforced = measure(cores, args.rounds, binary, java, corpus,
                                            HELLO_APP, HELLO_PKG, work, expected)
        base = bases.get(str(cores), {})
        entry = {'enforced': enforced,
                 'time': dict(paired(times), baseline=base.get('time'),
                              verdict=verdict(statistics.median(times), base.get('time'),
                                              tolerance['time'])),
                 'memory': dict(paired(memories), baseline=base.get('memory'),
                                verdict=verdict(statistics.median(memories), base.get('memory'),
                                                tolerance['memory']))}
        report['results'][str(cores)] = entry
        for metric in ('time', 'memory'):
            e = entry[metric]
            print('perf-gate: %d cores %-6s %.3fx (rounds %.3f..%.3f) baseline %s -> %s'
                  % (cores, metric, e['median'], e['min'], e['max'],
                     ('%.3fx' % e['baseline']) if e['baseline'] is not None else 'none',
                     e['verdict'].upper()))
            if e['verdict'] == 'regression':
                failed = True
    report['regression'] = failed
    out = Path(args.out) if args.out else work / 'results.json'
    out.write_text(json.dumps(report, indent=2) + '\n')
    print('perf-gate: results in %s' % out)
    uncalibrated = [c for c, e in report['results'].items()
                    if e['time']['baseline'] is None or e['memory']['baseline'] is None]
    if uncalibrated:
        print('perf-gate: NOT GATED at %s cores -- %s has no baseline there. To calibrate, add:'
              % (','.join(uncalibrated), args.platform))
        print('  "%s": %s' % (args.platform, json.dumps(
            {c: {'time': round(e['time']['median'], 3), 'memory': round(e['memory']['median'], 3)}
             for c, e in report['results'].items()})))
    print('perf-gate: %s' % ('REGRESSION' if failed else 'OK'))
    return 1 if failed else 0


if __name__ == '__main__':
    try:
        sys.exit(main(sys.argv[1:]))
    except (RuntimeError, OSError, ValueError, KeyError, subprocess.TimeoutExpired) as error:
        sys.exit('perf-gate: REFUSING: %s' % error)
