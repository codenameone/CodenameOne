#!/usr/bin/env python3
"""Fresh-process translation benchmark. Failed or divergent runs never yield ratios."""
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import signal
import statistics
import subprocess
import sys
import threading
import time

REPO = Path(__file__).resolve().parents[2]
TARGET = REPO / 'vm/selfhost/target'


def digest(path):
    path = Path(path)
    h = hashlib.sha256()
    files = sorted(p for p in path.rglob('*') if p.is_file()) if path.is_dir() else [path]
    for p in files:
        h.update(str(p.relative_to(path) if path.is_dir() else p.name).encode() + b'\0')
        with p.open('rb') as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                h.update(chunk)
        h.update(b'\0')
    return h.hexdigest()


def sources():
    return {name: digest(REPO / name) for name in (
        'vm/ByteCodeTranslator/src', 'vm/JavaAPI/src', 'vm/selfhost/stubs')}


def record_build(binary):
    binary = Path(binary).resolve()
    snapshot = json.loads(Path(os.environ['CN1_BUILD_SOURCE_SNAPSHOT']).read_text())
    if snapshot != sources():
        raise RuntimeError('Sources changed during build; rebuild before benchmarking')
    manifest = {'sources': snapshot, 'binary': digest(binary),
                'host_classes': digest(REPO / 'vm/ByteCodeTranslator/target/classes'),
                'javaapi': digest(TARGET / 'javaapi-classes'),
                'compiler': subprocess.check_output(
                    [os.environ.get('CN1_SELFHOST_CC', 'clang'), '--version'], text=True),
                'java': subprocess.check_output(
                    [os.environ['JDK_8_HOME'] + '/bin/java', '-version'],
                    stderr=subprocess.STDOUT, text=True),
                'flags': os.environ.get('CN1_BUILD_FLAGS', ''),
                'translator_options': os.environ.get('CN1_SELFHOST_JAVA_OPTS', '')}
    Path(str(binary) + '.build.json').write_text(json.dumps(manifest, indent=2) + '\n')


def parse_usage(output, system):
    if system == 'Darwin':
        peak = re.search(r'^\s*(\d+)\s+peak memory footprint\s*$', output, re.M)
        cpu = re.search(r'([\d.]+)\s+user\s+([\d.]+)\s+sys', output)
        factor = 1
    else:
        peak = re.search(r'Maximum resident set size \(kbytes\):\s*(\d+)', output)
        user = re.search(r'User time \(seconds\):\s*([\d.]+)', output)
        system_time = re.search(r'System time \(seconds\):\s*([\d.]+)', output)
        cpu = (float(user[1]), float(system_time[1])) if user and system_time else None
        factor = 1024
    if not peak or not cpu or int(peak[1]) <= 0:
        raise RuntimeError('Missing or invalid process resource usage; see the run log')
    if system == 'Darwin':
        cpu = (float(cpu[1]), float(cpu[2]))
    return {'peak_bytes': int(peak[1]) * factor, 'cpu_seconds': sum(cpu)}


def error_log(log):
    """Where a run's stderr goes: beside its stdout log, never into it.

    The two used to share one file, and they are separate handles with separate
    buffering, so a runtime diagnostic on stderr could land between two chunks of one
    stdout line. A BENCH record was split mid-number that way on Windows arm64 -- the
    checksum read -228848789171527 in one repetition and -2288487891715278 in the
    rest -- which failed a row on a parse, not on a result. The stdout log now holds
    only what the program printed, and resource usage (GNU/BSD time reports on
    stderr) is read from this file."""
    return log.with_name(log.name + '.err')


def run_windows(command, env, log, timeout):
    """Windows has no /usr/bin/time. The peak working set and CPU times are read off the
    process handle after exit -- still open, because Popen keeps it until the object is
    collected -- which is the same quantity GNU time reports as the maximum RSS."""
    import ctypes
    from ctypes import wintypes

    class PROCESS_MEMORY_COUNTERS(ctypes.Structure):
        _fields_ = [('cb', wintypes.DWORD), ('PageFaultCount', wintypes.DWORD),
                    ('PeakWorkingSetSize', ctypes.c_size_t), ('WorkingSetSize', ctypes.c_size_t),
                    ('QuotaPeakPagedPoolUsage', ctypes.c_size_t),
                    ('QuotaPagedPoolUsage', ctypes.c_size_t),
                    ('QuotaPeakNonPagedPoolUsage', ctypes.c_size_t),
                    ('QuotaNonPagedPoolUsage', ctypes.c_size_t),
                    ('PagefileUsage', ctypes.c_size_t), ('PeakPagefileUsage', ctypes.c_size_t)]

    start = time.monotonic()
    with log.open('w') as output, error_log(log).open('w') as errors:
        process = subprocess.Popen(command, env=env, stdout=output, stderr=errors)
        try:
            process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            # Kill the whole tree: a JVM launcher can outlive a plain terminate().
            subprocess.call(['taskkill', '/F', '/T', '/PID', str(process.pid)],
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            process.wait()
            raise
        elapsed = time.monotonic() - start
        handle = wintypes.HANDLE(int(process._handle))
        counters = PROCESS_MEMORY_COUNTERS()
        counters.cb = ctypes.sizeof(counters)
        if not ctypes.windll.psapi.GetProcessMemoryInfo(handle, ctypes.byref(counters),
                                                        counters.cb):
            raise RuntimeError('GetProcessMemoryInfo failed; see the run log')
        created, exited, kernel, user = (wintypes.FILETIME() for _ in range(4))
        if not ctypes.windll.kernel32.GetProcessTimes(handle, ctypes.byref(created),
                                                      ctypes.byref(exited),
                                                      ctypes.byref(kernel),
                                                      ctypes.byref(user)):
            raise RuntimeError('GetProcessTimes failed; see the run log')
    if process.returncode:
        raise RuntimeError('Process exited with %s: %s (stderr: %s)'
                           % (process.returncode, log, error_log(log)))

    def seconds(filetime):
        return ((filetime.dwHighDateTime << 32) | filetime.dwLowDateTime) / 1e7

    if counters.PeakWorkingSetSize <= 0:
        raise RuntimeError('Missing or invalid process resource usage; see the run log')
    return {'peak_bytes': int(counters.PeakWorkingSetSize),
            'cpu_seconds': seconds(kernel) + seconds(user), 'elapsed_seconds': elapsed}


def run(command, env, log, system, timeout=300):
    if system == 'Windows':
        return run_windows(command, env, log, timeout)
    wrapper = ['/usr/bin/time', '-l' if system == 'Darwin' else '-v']
    start = time.monotonic()
    with log.open('w') as output, error_log(log).open('w') as errors:
        result = subprocess.Popen(wrapper + command, env=env, stdout=output, stderr=errors,
                                  start_new_session=True)
        timed_out = threading.Event()

        def expire():
            if result.poll() is None:
                timed_out.set()
                try:
                    os.killpg(result.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass

        watchdog = threading.Timer(timeout, expire)
        watchdog.daemon = True
        watchdog.start()
        try:
            # wait(timeout=...) polls with sleeps of up to 50ms on POSIX. That
            # biases short whole-workload timings. Block in waitpid instead;
            # the watchdog still terminates the complete process group on timeout.
            result.wait()
            elapsed = time.monotonic() - start
            if timed_out.is_set():
                raise subprocess.TimeoutExpired(command, timeout)
        except BaseException:
            # time may have spawned the actual VM; terminate the complete run.
            if not timed_out.is_set():
                try:
                    os.killpg(result.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
            result.wait()
            raise
        finally:
            watchdog.cancel()
    if result.returncode:
        raise RuntimeError('Process exited with %s: %s (stderr: %s)'
                           % (result.returncode, log, error_log(log)))
    usage = parse_usage(error_log(log).read_text(errors='replace'), system)
    usage['elapsed_seconds'] = elapsed
    return usage


def output_manifest(directory):
    files = {str(p.relative_to(directory)): digest(p)
             for p in sorted(directory.rglob('*')) if p.is_file()}
    if len(files) < 10 or not any(p.endswith(('.c', '.m')) for p in files):
        raise RuntimeError('Translation output is empty or incomplete')
    return files


def main(args):
    if len(args) == 2 and args[0] == '--snapshot-sources':
        Path(args[1]).write_text(json.dumps(sources(), sort_keys=True) + '\n')
        return
    if len(args) == 2 and args[0] == '--record-build':
        record_build(args[1])
        return
    if len(args) not in (3, 4):
        raise RuntimeError('usage: bench-selfhost.sh <classesDir> <AppName> <package> [rounds]')
    classes, app, package = args[:3]
    rounds = int(args[3]) if len(args) == 4 else 7
    if rounds < 1:
        raise RuntimeError('rounds must be positive')
    # The translator accepts semicolon-separated class directories.
    corpus = [Path(p).resolve() for p in classes.split(';')]
    if not all(p.is_dir() for p in corpus):
        raise RuntimeError('Every corpus input must be an existing class directory')
    binary = Path(os.environ.get('CN1_SELFHOST_BIN', str(TARGET / 'parpar-O3'))).resolve()
    manifest = json.loads(Path(str(binary) + '.build.json').read_text())
    if manifest['sources'] != sources() or manifest['binary'] != digest(binary):
        raise RuntimeError('Stale native build: rebuild with build-selfhost.sh -O3')
    host = REPO / 'vm/ByteCodeTranslator/target/classes'
    javaapi = TARGET / 'javaapi-classes'
    if manifest['host_classes'] != digest(host) or manifest['javaapi'] != digest(javaapi):
        raise RuntimeError('Host/JavaAPI classes differ from the verified native build inputs')
    asm_file = REPO / 'vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt'
    if not asm_file.is_file():
        # See verify-selfhost.sh: `mvn clean package` removes target/, and the
        # gauntlet triggers one. Absent this file the bench prints no ratio line at
        # all, which perf-guard then reports as a possible correctness failure.
        raise SystemExit('missing %s -- run vm/selfhost/build-selfhost.sh -O3 first; '
                         'mvn clean package removes target/ and it regenerates this.'
                         % asm_file)
    asm = asm_file.read_text().strip()
    refs = os.environ.get('SELFHOST_REF_JAVAS')
    if refs:
        javas = refs.split(',')
    else:
        javas = [os.environ[name] + '/bin/java' for name in ('JDK_25_HOME', 'JDK_8_HOME')]
    arms = [('parpar', str(binary))]
    versions = {}
    for index, java in enumerate(javas):
        java = str(Path(java).resolve())
        version = subprocess.check_output([java, '-version'], stderr=subprocess.STDOUT, text=True)
        # NAME THE ARM AFTER THE JDK IT ACTUALLY IS, not after its position. A
        # positional name cannot tell anyone which JVM produced a number, and it
        # hides the failure it most needs to expose: when JDK_25_HOME is unset or
        # wrong, both reference arms resolve to the SAME JVM and a positional
        # scheme happily reports them as two different ones. Deriving the name
        # from `java -version` makes that collide loudly instead.
        feature = re.search(r'version "(?:1\.)?(\d+)', version)
        name = 'jdk%s' % (feature.group(1) if feature else 'unknown-%d' % (index + 1))
        if name in versions:
            raise RuntimeError(
                'both reference JVMs report %s (%s). Set JDK_25_HOME and JDK_8_HOME to '
                'different JDKs, or pass SELFHOST_REF_JAVAS deliberately -- comparing a '
                'JDK against itself is not a baseline.' % (name, java))
        arms.append((name, java))
        versions[name] = version
    # Keep past evidence; all arms within this invocation use the same output path.
    work = TARGET / 'bench' / time.strftime('%Y%m%d-%H%M%S')
    work.mkdir(parents=True, exist_ok=False)
    out = work / 'out'
    env = {k: v for k, v in os.environ.items() if not k.startswith('CN1_')}
    # THE CORE-COUNT AXIS HAS TO REACH BOTH ARMS. run-matrix.sh sets
    # CN1_GC_MARK_THREADS for ours and CN1_SELFHOST_JDK_OPTS
    # (-XX:ActiveProcessorCount) for the JVM, and the CN1_ filter above dropped
    # the first while nothing read the second -- so its 1, 2 and 4 core selfhost
    # rows were three runs of ONE configuration. That hid a serial marker taking
    # 362s on this corpus against 4.8s at four markers. Pass exactly these
    # through; every other CN1_ knob stays filtered.
    if os.environ.get('CN1_GC_MARK_THREADS'):
        env['CN1_GC_MARK_THREADS'] = os.environ['CN1_GC_MARK_THREADS']
    jdk_opts = os.environ.get('CN1_SELFHOST_JDK_OPTS', '').split()
    env['LC_ALL'] = 'C'
    env['CN1_RESOURCE_PATH'] = str(REPO / 'vm/ByteCodeTranslator/src')
    system = platform.system()
    report = {'platform': platform.platform(), 'rounds': rounds, 'build': manifest,
              'metric': 'peak phys_footprint' if system == 'Darwin' else 'peak RSS',
              'corpus': {str(p): digest(p) for p in corpus},
              'executables': {name: {'path': path, 'sha256': digest(path)} for name, path in arms},
              'versions': versions, 'asm': {p: digest(p) for p in asm.split(os.pathsep)},
              'samples': [], 'complete': False}
    result_file = work / 'results.json'

    def save():
        result_file.write_text(json.dumps(report, indent=2) + '\n')

    save()
    expected = None
    try:
        # Round zero is a correctness preflight, excluded from the timing summary.
        for round_index in range(rounds + 1):
            order = arms[round_index % len(arms):] + arms[:round_index % len(arms)]
            for name, executable in order:
                if out.exists():
                    shutil.rmtree(out)
                out.mkdir()
                command = [executable]
                if name != 'parpar':
                    command += jdk_opts
                    command += ['-cp', str(host) + os.pathsep + asm,
                                'com.codename1.tools.translator.ByteCodeTranslator']
                command += ['clean', ';'.join(map(str, [javaapi] + corpus)), str(out),
                            app, package, app, '1.0', 'clean', 'none']
                log = work / ('%02d-%s.log' % (round_index, name))
                sample = run(command, env, log, system)
                actual = output_manifest(out)
                if expected is None:
                    expected = actual
                    (work / 'output-manifest.json').write_text(json.dumps(expected, indent=2) + '\n')
                elif actual != expected:
                    differences = sorted(k for k in set(actual) | set(expected)
                                         if actual.get(k) != expected.get(k))
                    raise RuntimeError('Output divergence in %s: %s' % (log, ', '.join(differences[:10])))
                sample.update(arm=name, round=round_index, log=str(log), command=command)
                report['samples'].append(sample)
                save()
                print('%s round=%d elapsed=%.3fs cpu=%.3fs peak=%d bytes verified' %
                      (name, round_index, sample['elapsed_seconds'], sample['cpu_seconds'],
                       sample['peak_bytes']), flush=True)
        # Reject a mixed-source or mixed-input experiment even if its outputs happened
        # to compare equal. This check is outside the measured intervals.
        if manifest['sources'] != sources() or manifest['binary'] != digest(binary):
            raise RuntimeError('Sources or native binary changed during benchmark')
        if manifest['host_classes'] != digest(host) or manifest['javaapi'] != digest(javaapi):
            raise RuntimeError('Host or JavaAPI classes changed during benchmark')
        for group in ('corpus', 'asm'):
            if any(digest(path) != sha for path, sha in report[group].items()):
                raise RuntimeError(group + ' changed during benchmark')
        if any(digest(entry['path']) != entry['sha256'] for entry in report['executables'].values()):
            raise RuntimeError('Executable changed during benchmark')
        report['summary'] = {}
        for name, _ in arms:
            samples = [s for s in report['samples'] if s['arm'] == name and s['round'] > 0]
            times = [s['elapsed_seconds'] for s in samples]
            report['summary'][name] = dict(elapsed_median=statistics.median(times),
                                          elapsed_min=min(times), elapsed_max=max(times),
                                          cpu_median=statistics.median(s['cpu_seconds'] for s in samples),
                                          peak_max=max(s['peak_bytes'] for s in samples),
                                          # Peak min and median as well as max. The max
                                          # alone was the only figure reported, and on a
                                          # metric with 15-20% of internal variance that
                                          # silently picks the worst statistic: the same
                                          # run reads 1.147x on min/min and 1.349x on
                                          # max/max. Print all three so the reader can
                                          # see which one a claim rests on.
                                          peak_min=min(s['peak_bytes'] for s in samples),
                                          peak_median=statistics.median(
                                              s['peak_bytes'] for s in samples))
        report['complete'] = True
        save()
        print(json.dumps(report['summary'], indent=2))
        native = report['summary']['parpar']
        # HOW NOISY WAS THIS MEASUREMENT? A ratio with no spread beside it cannot be
        # gated responsibly: on a shared CI runner wall clock moves with whatever else
        # the host is doing, while peak memory barely moves at all. perf-guard.sh reads
        # this line and declines to gate a TIME ratio it can see is not a measurement,
        # because a perf gate that fails at random gets ignored, and an ignored gate is
        # worse than no gate. Memory has no such escape hatch -- if peak memory is
        # unstable something is actually wrong.
        for name, _ in arms:
            arm = report['summary'][name]
            lo, hi = arm['elapsed_min'], arm['elapsed_max']
            plo, phi = arm['peak_min'], arm['peak_max']
            print('spread %s: elapsed min %.3fs median %.3fs max %.3fs (%.1f%%), '
                  'peak min %d median %d max %d (%.1f%%)'
                  % (name, lo, arm['elapsed_median'], hi,
                     100.0 * (hi - lo) / lo if lo > 0 else 0.0,
                     plo, arm['peak_median'], phi,
                     100.0 * (phi - plo) / plo if plo > 0 else 0.0))
        for name, _ in arms[1:]:
            other = report['summary'][name]
            print('vs %s: elapsed %.3fx, peak memory %.3fx' %
                  (name, native['elapsed_median'] / other['elapsed_median'],
                   native['peak_max'] / other['peak_max']))
    except Exception as error:
        report['error'] = str(error)
        save()
        raise
    finally:
        print('Evidence: %s' % result_file, flush=True)


if __name__ == '__main__':
    try:
        main(sys.argv[1:])
    except (RuntimeError, OSError, ValueError, KeyError, subprocess.TimeoutExpired) as error:
        sys.exit('REFUSING: %s' % error)
