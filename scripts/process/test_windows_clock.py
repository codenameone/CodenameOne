#!/usr/bin/env python3
"""Exercise the Windows nanoTime binding with controlled QPC and wall clocks."""
import os
from pathlib import Path
import shlex
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
RUNTIME = ROOT / "vm/ByteCodeTranslator/src"


def function(path, signature):
    source = path.read_text()
    start = source.index("\n" + signature) + 1
    return source[start:source.index("\n}", start) + 2]


class WindowsClockTest(unittest.TestCase):
    def test_wall_clock_steps_do_not_change_elapsed_time(self):
        source = r'''
#include <assert.h>
#include <stdint.h>
#include <stddef.h>
#define _WIN32 1
#define JAVA_LONG int64_t
#define CODENAME_ONE_THREAD_STATE void
#define __STATIC_INITIALIZER_java_lang_System(ignored) ((void)0)
typedef struct { int64_t QuadPart; } LARGE_INTEGER;
struct timeval { long tv_sec; long tv_usec; };
static int64_t ticks, frequency = 10000000;
static long wall_seconds = 1800000000;
static int QueryPerformanceFrequency(LARGE_INTEGER *out) {
    out->QuadPart = frequency; return 1;
}
static int QueryPerformanceCounter(LARGE_INTEGER *out) {
    out->QuadPart = ticks; return 1;
}
static int gettimeofday(struct timeval *out, void *unused) {
    (void)unused;
    out->tv_sec = wall_seconds; out->tv_usec = 0; return 0;
}
'''
        source += function(RUNTIME / "cn1_win_compat.c", "long long cn1_monotonic_micros(void)")
        for name in ("currentTimeMillis", "nanoTime"):
            source += "\n" + function(RUNTIME / "nativeMethods.m",
                                    "JAVA_LONG java_lang_System_" + name + "___R_long(")
        source += r'''
int main(void) {
    /* Long uptime exercises the split conversion, not just tiny counters. */
    ticks = frequency * 86400LL * 365 * 10;
    int64_t start = java_lang_System_nanoTime___R_long();
    int64_t wall_start = java_lang_System_currentTimeMillis___R_long();
    wall_seconds -= 3600;
    ticks += frequency / 40;
    assert(java_lang_System_currentTimeMillis___R_long() == wall_start - 3600000);
    assert(java_lang_System_nanoTime___R_long() - start == 25000000);
    wall_seconds += 7200;
    ticks += frequency / 40;
    assert(java_lang_System_currentTimeMillis___R_long() == wall_start + 3600000);
    assert(java_lang_System_nanoTime___R_long() - start == 50000000);
    /* A non-integral tick/microsecond ratio still rounds monotonically. */
    frequency = 3579545;
    ticks = frequency * 100000LL;
    start = java_lang_System_nanoTime___R_long();
    for (int i = 0; i < 10000; ++i) {
        ++ticks;
        int64_t next = java_lang_System_nanoTime___R_long();
        assert(next >= start);
        start = next;
    }
    return 0;
}
'''
        with tempfile.TemporaryDirectory(prefix="cn1-windows-clock-") as work:
            test = Path(work) / "clock.c"
            binary = Path(work) / "clock"
            test.write_text(source)
            subprocess.run(shlex.split(os.environ.get("CC", "cc")) +
                           ["-std=c99", "-Wall", "-Wextra", "-Werror", str(test), "-o", str(binary)],
                           check=True)
            subprocess.run([str(binary)], check=True, cwd=work)


if __name__ == "__main__":
    unittest.main()
