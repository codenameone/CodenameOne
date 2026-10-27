# Shared by every kernel: runs `work(size)` `repeat` times and prints the
# result once, then the fastest round (BENCH), the first, cold one (COLD)
# and all rounds together (TOTAL), in the line format the scripts read.
#
# The size is a run-time input (the first argument, else the kernel's own
# default), so no compiler, ahead of time or just in time, can fold the work
# into a constant. Every round must compute the same result; a round a
# compiler skipped or memoized would have to reproduce it exactly anyway.
import sys
import time


def measure(name, work, size, repeat):
    if len(sys.argv) > 1:
        size = int(sys.argv[1])
    best = None
    first = None
    total = 0.0
    result = None
    for _ in range(repeat):
        t0 = time.perf_counter()
        r = work(size)
        dt = time.perf_counter() - t0
        if result is not None and r != result:
            raise RuntimeError("rounds disagree: %r != %r" % (r, result))
        result = r
        total += dt
        if first is None:
            first = dt
        if best is None or dt < best:
            best = dt
    print("RESULT", name, result)
    print("BENCH", name, int(best * 1000000))
    print("COLD", name, int(first * 1000000))
    print("TOTAL", name, int(total * 1000000))
