# Codename One Python vs CPython, PyPy, GraalPy

Measured on Darwin arm64, 2026-11-02. Codename One rows are native binaries (Python -> Java -> ParparVM C -> clang -O3).

- CPython: `/private/tmp/claude-501/-Users-shai-dev-cn3-CodenameOne/c06f7f0a-b3fa-4b20-9022-2c46ff8816df/scratchpad/cpyv2/bin/python` (3.14.5 cpython)
- PyPy: `/private/tmp/claude-501/-Users-shai-dev-cn3-CodenameOne/c06f7f0a-b3fa-4b20-9022-2c46ff8816df/scratchpad/pypyv2/bin/pypy3` (3.11.13 pypy)
- GraalPy: `/private/tmp/claude-501/-Users-shai-dev-cn3-CodenameOne/c06f7f0a-b3fa-4b20-9022-2c46ff8816df/scratchpad/gvenv2/bin/graalpy` (3.13.14 graalpy)

## Startup: `print('hello')`

| | wall ms (best of 10) | peak footprint MB |
|---|---:|---:|
| CPython | 24.2 | 6.6 |
| PyPy | 21.5 | 22.0 |
| GraalPy | 41.7 | 30.0 |
| Codename One Python | 7.6 | 4.5 |

## Two ways to run the same binary

Every Codename One row appears more than once. **Codename One Python** is the binary as it ships, on a machine with memory to spare. Its collector is adaptive: while it has CPU to spare it holds the heap near the live set, and when collecting starts to cost the program throughput it lets the heap grow instead, because unused memory buys nothing and the speed it pays for is real. **Codename One Python, 128 MB budget** is the same binary given a per-process memory budget, the way iOS limits every app and a container limits a server. Then memory is the scarce resource and the trade flips: the heap is held tight however busy the collector is, and the program pays in collections, not memory.

Memory is **physical footprint**, what the kernel meters a process by and what a budget is measured in, for every runtime alike.

## CPU kernels (fastest of the kernel's rounds, ms; peak footprint MB)

| kernel | CPython ms | CPython MB | PyPy ms | PyPy MB | GraalPy ms | GraalPy MB | Codename One Python ms | Codename One Python MB | Codename One Python, 128 MB budget ms | Codename One Python, 128 MB budget MB |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| nbody | 1088.6 | 7.2 | 88.5 | 22.8 | 267.1 | 119.2 | 126.1 | 5.2 | 125.5 | 5.1 |
| richards | 901.2 | 7.6 | 54.9 | 24.2 | 293.5 | 145.9 | 58.1 | 5.3 | 60.8 | 5.3 |
| fannkuch | 3784.1 | 7.0 | 827.1 | 23.3 | 776.4 | 123.3 | 800.2 | 5.3 | 800.8 | 5.3 |
| spectral_norm | 1013.2 | 7.0 | 16.7 | 22.7 | 10.3 | 134.4 | 16.6 | 5.1 | 16.4 | 5.1 |
| dicts_strings | 550.6 | 54.8 | 188.8 | 87.1 | 350.9 | 261.4 | 277.2 | 124.8 | 301.3 | 94.4 |

## Flask JSON API (`scripts/python-backend/samples/flask-todo`)

Thirty seconds of `ab -c 8 /fib/30` after a 2000-request warm-up -- the same length of load for every server, not the same request count, so a fast server is not measured before its collector settles. Footprint is read as the load ends, again 5 seconds later, and at its peak. CPython, PyPy and GraalPy serve with `app.run()` (Werkzeug's threaded server), Codename One with the same call, which it lowers onto the native backend HTTP server.

| | first response ms | idle MB | after load MB | 5s later MB | peak MB | RSS after load MB | req/s | p99 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| CPython | 134 | 28.4 | 28.0 | 28.0 | 29.6 | 40.6 | 683 | 17 |
| PyPy | 440 | 59.8 | 87.7 | 87.7 | 87.7 | 110.6 | 677 | 18 |
| GraalPy | 1207 | 238.5 | 831.1 | 848.2 | 901.9 | 979.4 | 517 | 50 |
| Codename One Python | 78 | 14.1 | 104.5 | 98.3 | 346.5 | 214.5 | 15926 | 3 |
| Codename One Python, 128 MB budget | 31 | 14.0 | 67.1 | 64.7 | 106.8 | 114.6 | 14286 | 3 |

Unconstrained, the server spends memory for speed: 104 MB after load at 15926 requests a second. CPython serves 683 a second in 28 MB.

- Under a 128 MB budget: 67 MB after load, 107 MB at peak, 14286 requests a second -- 90% of the unconstrained throughput, 21 times CPython's.

## What you deploy

| | size MB | what |
|---|---:|---|
| CPython | 82.2 | interpreter install (`/opt/homebrew/opt/python@3.14/Frameworks/Python.framework/Versions/3.14`) + Flask and its dependencies (5.2) |
| PyPy | 110.5 | interpreter install (`/Users/shai/dev/pyimpls/pypy3.11-v7.3.20-macos_arm64`) + Flask and its dependencies (5.1) |
| GraalPy | 305.8 | interpreter install (`/private/tmp/claude-501/-Users-shai-dev-cn3-CodenameOne/c06f7f0a-b3fa-4b20-9022-2c46ff8816df/scratchpad/rt2/graalpy-community3.13-25.4.4.1.1-macos-aarch64`) + Flask and its dependencies (11.2) |
| Codename One Python | 35.0 | one self-contained executable, Flask included |

