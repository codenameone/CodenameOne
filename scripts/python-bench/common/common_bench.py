# Runs one CommonWorkloads workload the way vm/benchmarks' Bench.java and
# the port applications do: warm-up passes, then the best of the timed ones.
#
#   common_bench.py <workload> [python|java] [warmup] [measured]
#
# Prints `BENCH <workload> ms=<best> checksum=<value>`. `java` runs the same
# workload through java_hotspots.py, the Java implementation called from
# Python, which only a Codename One build can import.
import sys
import time

import common_workloads


def main():
    name = sys.argv[1]
    mode = sys.argv[2] if len(sys.argv) > 2 else "python"
    warmup = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    measured = int(sys.argv[4]) if len(sys.argv) > 4 else 3
    if mode == "java":
        import java_hotspots
        work = java_hotspots.WORKLOADS[name]
    else:
        work = common_workloads.WORKLOADS[name]
    checksum = None
    for _ in range(warmup):
        checksum = work()
    best = None
    for _ in range(measured):
        t0 = time.perf_counter()
        r = work()
        dt = time.perf_counter() - t0
        if checksum is not None and r != checksum:
            raise RuntimeError("rounds disagree: %r != %r" % (r, checksum))
        checksum = r
        if best is None or dt < best:
            best = dt
    print("BENCH %s ms=%.1f checksum=%d" % (name, best * 1000.0, checksum))


main()
