# Codename One Python vs CPython, PyPy, GraalPy

Measured on Darwin arm64, 2026-11-01. Codename One rows are native binaries (Python -> Java -> ParparVM C -> clang -O3).

- CPython: `<scratch>/fjvenv/bin/python` (3.14.5 cpython)
- PyPy: `<scratch>/pvenv/bin/pypy3` (3.11.13 pypy)
- GraalPy: `<scratch>/gvenv/bin/graalpy` (3.13.14 graalpy)

## Startup: `print('hello')`

| | wall ms (best of 10) | peak footprint MB |
|---|---:|---:|
| CPython | 25.2 | 6.6 |
| PyPy | 22.0 | 22.0 |
| GraalPy | 41.6 | 28.2 |
| Codename One Python | 7.8 | 4.5 |

## Two ways to run the same binary

Every Codename One row appears twice. **Codename One Python** is the binary as it ships, on a machine with memory to spare. Its collector is adaptive: while it has CPU to spare it holds the heap near the live set, and when collecting starts to cost the program throughput it lets the heap grow instead, because unused memory buys nothing and the speed it pays for is real. **Codename One Python, 128 MB budget** is the same binary given a per-process memory budget, the way iOS limits every app and a container limits a server. Then memory is the scarce resource and the trade flips: the heap is held tight however busy the collector is, and the program pays in collections, not memory.

Memory is **physical footprint**, what the kernel meters a process by and what a budget is measured in, for every runtime alike.

## CPU kernels (fastest of the kernel's rounds, ms; peak footprint MB)

| kernel | CPython ms | CPython MB | PyPy ms | PyPy MB | GraalPy ms | GraalPy MB | Codename One Python ms | Codename One Python MB | Codename One Python, 128 MB budget ms | Codename One Python, 128 MB budget MB |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| nbody | 1091.3 | 7.3 | 88.6 | 22.9 | 344.9 | 120.3 | 125.2 | 5.2 | 126.8 | 5.2 |
| richards | 906.5 | 7.6 | 54.5 | 24.2 | 195.1 | 149.3 | 51.1 | 5.3 | 60.4 | 5.4 |
| fannkuch | 3655.8 | 6.9 | 1375.7 | 23.4 | 1119.3 | 121.1 | 941.1 | 5.3 | 879.8 | 5.2 |
| spectral_norm | 1173.0 | 7.0 | 22.7 | 22.7 | 16.8 | 124.7 | 23.2 | 5.2 | 22.4 | 5.3 |
| dicts_strings | 727.1 | 53.3 | 246.9 | 84.1 | 538.7 | 255.6 | 276.5 | 124.3 | 345.4 | 98.3 |

## Flask JSON API (`scripts/python-backend/samples/flask-todo`)

Thirty seconds of `ab -c 8 /fib/30` after a 2000-request warm-up -- the same length of load for every server, not the same request count, so a fast server is not measured before its collector settles. Footprint is read as the load ends, again 5 seconds later, and at its peak. CPython, PyPy and GraalPy serve with `app.run()` (Werkzeug's threaded server), Codename One with the same call, which it lowers onto the native backend HTTP server.

| | first response ms | idle MB | after load MB | 5s later MB | peak MB | RSS after load MB | req/s | p99 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| CPython | 346 | 28.3 | 27.7 | 27.7 | 29.5 | 40.5 | 670 | 21 |
| PyPy | 392 | 59.5 | 70.5 | 70.5 | 81.2 | 101.6 | 673 | 22 |
| GraalPy | 1298 | 243.8 | 854.0 | 868.9 | 863.8 | 1001.8 | 520 | 53 |
| Codename One Python | 90 | 14.0 | 110.5 | 101.8 | 427.4 | 254.7 | 22876 | 2 |
| Codename One Python, 128 MB budget | 29 | 14.0 | 67.7 | 66.0 | 103.9 | 121.8 | 19504 | 2 |

Unconstrained, the server spends memory for speed: 110 MB after load at 22876 requests a second. Under the 128 MB budget it gives that speed back for memory, 68 MB at 19504 requests a second -- 85% of the unconstrained throughput and still 29 times CPython's, which serves 670 a second in 28 MB.

## What you deploy

| | size MB | what |
|---|---:|---|
| CPython | 82.2 | interpreter install (`/opt/homebrew/opt/python@3.14/Frameworks/Python.framework/Versions/3.14`) + Flask and its dependencies (5.2) |
| PyPy | 110.5 | interpreter install (`/Users/shai/dev/pyimpls/pypy3.11-v7.3.20-macos_arm64`) + Flask and its dependencies (5.1) |
| GraalPy | 305.8 | interpreter install (`<scratch>/rt/graalpy-community3.13-25.4.4.1.1-macos-aarch64`) + Flask and its dependencies (11.2) |
| Codename One Python | 35.0 | one self-contained executable, Flask included |

## CommonWorkloads (`common_compare.py`; ms, peak RSS MB)

| workload | CPython ms | MB | PyPy ms | MB | GraalPy ms | MB | Codename One Python ms | MB | Codename One Java ms | MB |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| intArithmetic | 14452.1 | 14.8 | 131.6 | 32.1 | 101.6 | 365.0 | 65.7 | 10.9 | 57.5 | 11.3 |
| longArithmetic | 11708.8 | 14.9 | 5300.7 | 41.6 | 11133.7 | 263.1 | 38.6 | 10.9 | 38.4 | 11.3 |
| mathTranscendental | 1009.9 | 14.8 | 186.4 | 31.8 | 288.5 | 354.1 | 179.8 | 11.3 | 167.2 | 11.6 |
| arraySequential | 1745.4 | 382.0 | 32.6 | 154.2 | 25.7 | 337.6 | 31.5 | 41.8 | 9.3 | 41.8 |
| arrayRandom | 4541.6 | 198.6 | 275.4 | 93.2 | 233.5 | 248.0 | 244.4 | 26.3 | 219.7 | 26.6 |
| objectAllocation | 521.6 | 14.9 | 18.1 | 63.1 | 58.1 | 351.9 | 30.8 | 163.8 | 14.2 | 179.1 |
| valueEscape | 801.2 | 14.9 | 6.1 | 32.0 | 55.5 | 330.6 | 3.5 | 10.9 | 4.5 | 11.2 |
| hashMapChurn | 158.4 | 23.1 | 41.1 | 70.8 | 86.7 | 250.6 | 43.7 | 26.9 | 20.4 | 30.3 |
| stringBuilding | 629.5 | 14.9 | 31.5 | 40.1 | 66.7 | 263.0 | 28.9 | 83.6 | 13.9 | 86.0 |
| recursion | 2151.7 | 14.9 | 177.3 | 57.0 | 188.6 | 282.6 | 85.7 | 10.9 | 81.5 | 11.3 |
| quicksort | 1519.2 | 83.8 | 168.0 | 57.9 | 110.9 | 247.0 | 117.7 | 16.7 | 79.8 | 17.0 |
