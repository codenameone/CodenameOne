#!/usr/bin/env python3
"""Run the scheduler's actual park/wake methods with independently stepped clocks.

The JVM VirtualThread implementation is a stub. Substitute its native wait
metadata and the clocks here; native conformance covers the real host loop.
"""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

from test_windows_clock import ROOT


class SchedulerClockTest(unittest.TestCase):
    def test_relative_parks_ignore_wall_clock_steps(self):
        server = (ROOT / "vm/backend/src/com/codename1/backend/HttpServer.java").read_text()
        source = '''import java.util.*;
import java.io.IOException;
public class SchedulerClockTest {
    static class Clock {
        static long wall = 1800000000000L, nanos = Long.MAX_VALUE - 2000000L;
        static long currentTimeMillis() { return wall; }
        static long nanoTime() { return nanos; }
        static void advance(long millis) { nanos += millis * 1000000L; }
    }
    static class VirtualThread {
        static int count;
        static long timeout;
        static int waitCount(long handle) { return count; }
        static long waitTimeout(long handle) { return timeout; }
        static int waitDescriptor(long handle, int index) { return index + 10; }
        static int waitEvents(long handle, int index) { return 1; }
        static long current() { return 7; }
        static void yieldNow() { }
    }
    static class Poller {
        void add(int fd, int events) throws IOException { }
        void remove(int fd) { }
    }
    static class VtHost {
        Poller poller = new Poller();
        HashMap napping = new HashMap();
        ArrayList<Long> ring = new ArrayList<Long>();
        long[] waiters = new long[16], waiterDeadlines = new long[16];
        int waiterCount;
        void ringAdd(long handle) { ring.add(handle); }
        void setWaiter(int fd, long handle) { }
        void addWaiter(long handle, long deadline, boolean task) {
            waiters[waiterCount] = handle; waiterDeadlines[waiterCount++] = deadline;
        }
        void removeWaiterAt(int index) {
            --waiterCount;
            waiters[index] = waiters[waiterCount];
            waiterDeadlines[index] = waiterDeadlines[waiterCount];
        }
    }
'''
        for declaration in ("private static final long SCHEDULER_START_NANOS", "private static final java.util.HashMap NAP_REQUESTS"):
            start = server.index("    " + declaration)
            source += server[start:server.index(";", start) + 1] + "\n"
        for method in ("private static long schedulerMillis(", "private static long schedulerDeadline(",
                       "private static void parkWaiter(", "private static void wakeWaiterAt(",
                       "private static long wakeExpiredWaiters(", "static void napUntil(",
                       "private void ringOrNap(", "private static long wakeNappers("):
            start = server.index("    " + method)
            source += server[start:server.index("\n    }", start) + 6] + "\n"
        source = source.replace("System.", "Clock.")
        source += '''
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        SchedulerClockTest server = new SchedulerClockTest();
        // No-descriptor waits are the path used by NativeProcessSupport.pauseImpl.
        for (int descriptors = 0; descriptors <= 2; descriptors += 2) {
            for (long wallStep : new long[] {-3600000, 7200000}) {
                VtHost host = new VtHost();
                VirtualThread.count = descriptors; VirtualThread.timeout = 5;
                parkWaiter(host, 42, true);
                Clock.wall += wallStep;
                Clock.advance(4);
                wakeNappers(host, schedulerMillis());
                wakeExpiredWaiters(host, schedulerMillis());
                check(host.ring.isEmpty());
                Clock.advance(1);
                wakeNappers(host, schedulerMillis());
                wakeExpiredWaiters(host, schedulerMillis());
                check(host.ring.equals(Arrays.asList(42L)));
            }
        }
        // Existing absolute-wall-time callers enter the same monotonic nap table.
        VtHost host = new VtHost();
        napUntil(Clock.wall + 10);
        Clock.wall -= 3600000;
        server.ringOrNap(host, 7);
        check(host.ring.isEmpty());
        Clock.advance(10);
        wakeNappers(host, schedulerMillis());
        check(host.ring.equals(Arrays.asList(7L)));
        check(schedulerDeadline(-1) == 0);
        check(schedulerDeadline(0) != 0);
        check(schedulerDeadline(Long.MAX_VALUE) == Long.MAX_VALUE);
        host = new VtHost();
        VirtualThread.count = 2; VirtualThread.timeout = -1;
        parkWaiter(host, 43, true);
        Clock.advance(100000);
        wakeExpiredWaiters(host, schedulerMillis());
        check(host.ring.isEmpty());
        host.napping.put(44L, Long.MAX_VALUE);
        wakeNappers(host, Long.MAX_VALUE);
        check(host.ring.equals(Arrays.asList(44L)));
    }
}
'''
        java_home = os.environ.get("JDK_8_HOME") or os.environ.get("JAVA_HOME")
        javac = str(Path(java_home) / "bin/javac") if java_home else "javac"
        java = str(Path(java_home) / "bin/java") if java_home else "java"
        with tempfile.TemporaryDirectory(prefix="cn1-scheduler-clock-") as directory:
            test = Path(directory) / "SchedulerClockTest.java"
            test.write_text(source)
            subprocess.run([javac, str(test)], check=True)
            subprocess.run([java, "-cp", directory, "SchedulerClockTest"], check=True)


if __name__ == "__main__":
    unittest.main()
