# Codename One Python vs CPython, PyPy, GraalPy

Measured on Darwin arm64, 2026-11-04. Codename One rows are native binaries (Python -> Java -> ParparVM C -> clang -O3).

- CPython: `/private/tmp/claude-501/-Users-shai-dev-cn3-CodenameOne/c06f7f0a-b3fa-4b20-9022-2c46ff8816df/scratchpad/cpyv2/bin/python` (3.14.5 cpython)
- PyPy: `/private/tmp/claude-501/-Users-shai-dev-cn3-CodenameOne/c06f7f0a-b3fa-4b20-9022-2c46ff8816df/scratchpad/pypyv2/bin/pypy3` (3.11.13 pypy)
- GraalPy: `/private/tmp/claude-501/-Users-shai-dev-cn3-CodenameOne/c06f7f0a-b3fa-4b20-9022-2c46ff8816df/scratchpad/gvenv2/bin/graalpy` (3.13.14 graalpy)

## Startup: `print('hello')`

| | wall ms (best of 10) | peak footprint MB |
|---|---:|---:|
| CPython | 18.1 | 6.6 |
| PyPy | 16.5 | 21.9 |
| GraalPy | 31.9 | 30.0 |
| Codename One Python | 6.1 | 4.5 |

## Two ways to run the same binary

Every Codename One row appears more than once. **Codename One Python** is the binary as it ships, on a machine with memory to spare. Its collector is adaptive: while it has CPU to spare it holds the heap near the live set, and when collecting starts to cost the program throughput it lets the heap grow instead, because unused memory buys nothing and the speed it pays for is real. **Codename One Python, 128 MB budget** is the same binary given a per-process memory budget, the way iOS limits every app and a container limits a server. Then memory is the scarce resource and the trade flips: the heap is held tight however busy the collector is, and the program pays in collections, not memory.

Memory is **physical footprint**, what the kernel meters a process by and what a budget is measured in, for every runtime alike.

## CPU kernels (fastest of the kernel's rounds, ms; peak footprint MB)

| kernel | CPython ms | CPython MB | PyPy ms | PyPy MB | GraalPy ms | GraalPy MB | Codename One Python ms | Codename One Python MB | Codename One Python, 128 MB budget ms | Codename One Python, 128 MB budget MB |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| nbody | 783.2 | 7.1 | 63.1 | 22.7 | 194.9 | 125.6 | 89.3 | 5.1 | 89.9 | 5.1 |
| richards | 648.0 | 7.6 | 43.3 | 24.5 | 113.1 | 157.4 | 37.6 | 5.3 | 36.4 | 5.4 |
| fannkuch | 2679.6 | 6.9 | 606.9 | 23.2 | 540.0 | 137.7 | 580.9 | 5.1 | 576.1 | 5.1 |
| spectral_norm | 735.2 | 6.9 | 12.1 | 22.5 | 7.5 | 146.3 | 11.8 | 5.1 | 11.8 | 5.1 |
| dicts_strings | 390.2 | 53.5 | 136.7 | 90.1 | 229.0 | 294.0 | 199.2 | 122.4 | 219.3 | 87.7 |

## Flask JSON API (`scripts/python-backend/samples/flask-todo`)

Thirty seconds of `ab -c 8 /fib/30` after a 2000-request warm-up -- the same length of load for every server, not the same request count, so a fast server is not measured before its collector settles. Footprint is read as the load ends, again 5 seconds later, and at its peak. CPython, PyPy and GraalPy serve with `app.run()` (Werkzeug's threaded server), Codename One with the same call, which it lowers onto the native backend HTTP server. The gunicorn rows serve the same application the way it is deployed in production, with 8 worker processes; their footprint is the master and every worker added together.

| | first response ms | idle MB | after load MB | 5s later MB | peak MB | RSS after load MB | req/s | p99 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| CPython | 115 | 28.1 | 28.8 | 28.8 | 29.4 | 40.3 | 699 | 12 |
| PyPy | 305 | 59.7 | 80.0 | 80.0 | 92.6 | 99.1 | 692 | 15 |
| GraalPy | 906 | 247.4 | 985.6 | 985.8 | 988.6 | 1082.0 | 559 | 38 |
| CPython, gunicorn 8 workers | 152 | 47.0 | 254.2 | 254.2 | 254.2 | 313.1 | 9337 | 1 |
| PyPy, gunicorn 8 workers | 320 | 129.2 | 653.2 | 652.5 | 653.2 | 766.2 | 9324 | 2 |
| Codename One Python | 66 | 14.0 | 157.2 | 153.2 | 471.9 | 275.9 | 27117 | 2 |
| Codename One Python, 128 MB budget | 27 | 13.9 | 76.4 | 74.2 | 111.4 | 105.6 | 25619 | 2 |
| GraalPy, gunicorn 8 workers | (gunicorn does not run on it) | | | | | | | |

Unconstrained, the server spends memory for speed: 157 MB after load at 27117 requests a second. CPython serves 699 a second in 29 MB.

- Under a 128 MB budget: 76 MB after load, 111 MB at peak, 25619 requests a second -- 94% of the unconstrained throughput, 37 times CPython's.

`app.run()` holds every connection about 10 ms after its response: Werkzeug's development server waits that long for leftover request bytes before it closes the socket, and `ab` counts a request done when the socket closes. Its rows measure that wait, not the interpreter. Deployed behind gunicorn:

- CPython, gunicorn 8 workers: 9337 requests a second in 254 MB after load. Codename One under a 128 MB budget serves 2.7 times as many in 30% of the memory.
- PyPy, gunicorn 8 workers: 9324 requests a second in 653 MB after load. Codename One under a 128 MB budget serves 2.7 times as many in 12% of the memory.

## What you deploy

| | size MB | what |
|---|---:|---|
| CPython | 82.2 | interpreter install (`/opt/homebrew/opt/python@3.14/Frameworks/Python.framework/Versions/3.14`) + Flask and its dependencies (5.2) |
| PyPy | 110.7 | interpreter install (`/Users/shai/dev/pyimpls/pypy3.11-v7.3.20-macos_arm64`) + Flask and its dependencies (5.1) |
| GraalPy | 306.6 | interpreter install (`/private/tmp/claude-501/-Users-shai-dev-cn3-CodenameOne/c06f7f0a-b3fa-4b20-9022-2c46ff8816df/scratchpad/rt2/graalpy-community3.13-25.4.4.1.1-macos-aarch64`) + Flask and its dependencies (11.2) |
| Codename One Python | 35.0 | one self-contained executable, Flask included |

