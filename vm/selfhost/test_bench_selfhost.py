import importlib.util
from pathlib import Path
import tempfile
import platform
import subprocess
import sys
import unittest

spec = importlib.util.spec_from_file_location('benchmark', Path(__file__).with_name('bench-selfhost.py'))
bench = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bench)


class BenchmarkTests(unittest.TestCase):
    def test_resource_formats_and_missing_measurements(self):
        self.assertEqual({'peak_bytes': 2048, 'cpu_seconds': 0.5}, bench.parse_usage(
            ' 0.7 real 0.3 user 0.2 sys\n 2048 peak memory footprint\n', 'Darwin'))
        self.assertEqual({'peak_bytes': 2048, 'cpu_seconds': 0.5}, bench.parse_usage(
            'User time (seconds): 0.3\nSystem time (seconds): 0.2\n'
            'Maximum resident set size (kbytes): 2\n', 'Linux'))
        with self.assertRaises(RuntimeError):
            bench.parse_usage('0.7 real 0.3 user 0.2 sys', 'Darwin')

    def test_failure_never_becomes_a_sample(self):
        with tempfile.TemporaryDirectory() as root:
            with self.assertRaisesRegex(RuntimeError, 'Process exited'):
                bench.run(['/bin/sh', '-c', 'exit 7'], {}, Path(root) / 'run.log', platform.system())

    def test_stderr_never_lands_inside_a_stdout_line(self):
        # The shape that split a BENCH checksum on Windows: half a record on stdout, a
        # diagnostic on stderr, then the rest of the record.
        script = ("import sys\n"
                  "sys.stdout.write('BENCH w rep 0 ns=5 checksum=-12345'); sys.stdout.flush()\n"
                  "sys.stderr.write('[GC] diagnostic\\n'); sys.stderr.flush()\n"
                  "sys.stdout.write('678\\n'); sys.stdout.flush()\n")
        with tempfile.TemporaryDirectory() as root:
            log = Path(root) / 'run.log'
            bench.run([sys.executable, '-c', script], {}, log, platform.system())
            self.assertEqual('BENCH w rep 0 ns=5 checksum=-12345678\n', log.read_text())
            self.assertIn('[GC] diagnostic', bench.error_log(log).read_text())

    def test_stalled_process_never_becomes_a_sample(self):
        with tempfile.TemporaryDirectory() as root:
            with self.assertRaises(subprocess.TimeoutExpired):
                bench.run([sys.executable, '-c', 'import time; time.sleep(30)'], {},
                          Path(root) / 'run.log', platform.system(), timeout=0.05)

    def test_manifest_detects_changes_and_deletions(self):
        with tempfile.TemporaryDirectory() as root:
            root = Path(root)
            (root / 'a').write_text('one')
            first = bench.digest(root)
            (root / 'a').write_text('two')
            self.assertNotEqual(first, bench.digest(root))
            second = bench.digest(root)
            (root / 'a').unlink()
            self.assertNotEqual(second, bench.digest(root))
            with self.assertRaises(RuntimeError):
                bench.output_manifest(root)


if __name__ == '__main__':
    unittest.main()
