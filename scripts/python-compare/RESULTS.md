# Codename One Python vs CPython, PyPy, GraalPy

Measured on Darwin arm64, 2026-10-27. Codename One rows are native binaries (Python -> Java -> ParparVM C -> clang -O3).

- CPython: `python3.14 (venv with Flask)` (3.14.5 cpython)
- PyPy: `~/.pyenv/versions/pypy3.11-7.3.19/bin/pypy3` (3.11.11 pypy)
- GraalPy: `graalpy-community3.13-25.4.4.1.1-macos-aarch64/bin/graalpy` (3.13.14 graalpy)

## Startup: `print('hello')`

| | wall ms (best of 10) | peak RSS MB |
|---|---:|---:|
| CPython | 20.3 | 14.3 |
| PyPy | 18.2 | 29.2 |
| GraalPy | 34.9 | 116.6 |
| Codename One | 5.9 | 8.7 |

## CPU kernels (fastest of the kernel's rounds, ms; peak RSS MB)

| kernel | CPython ms | CPython MB | PyPy ms | PyPy MB | GraalPy ms | GraalPy MB | Codename One ms | Codename One MB |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| nbody | 776.0 | 14.9 | 62.7 | 32.3 | 192.5 | 233.6 | 89.2 | 10.2 |
| richards | 634.8 | 15.5 | 39.9 | 33.8 | 146.5 | 260.1 | 44.7 | 10.6 |
| fannkuch | 2652.3 | 14.5 | 604.7 | 32.8 | 541.2 | 244.0 | 723.4 | 10.4 |
| spectral_norm | 729.4 | 14.6 | 12.0 | 32.2 | 7.4 | 248.3 | 11.1 | 10.2 |
| dicts_strings | 404.2 | 62.6 | 141.5 | 87.2 | 274.4 | 382.8 | 208.6 | 126.5 |

## Flask JSON API (`scripts/python-backend/samples/flask-todo`)

`ab -n 20000 -c 8 /fib/30` after a 2000-request warm-up. CPython, PyPy and GraalPy serve with `app.run()` (Werkzeug's threaded server), Codename One with the same call, which it lowers onto the native backend HTTP server.

| | first response ms | idle RSS MB | RSS after load MB | req/s | p99 ms |
|---|---:|---:|---:|---:|---:|
| CPython | 85 | 39.0 | 24.5 | 685 | 18 |
| PyPy | 196 | 65.3 | 88.7 | 688 | 17 |
| GraalPy | 609 | 325.7 | 931.0 | 561 | 44 |
| Codename One | 58 | 26.8 | 423.4 | 13564 | 2 |

## What you deploy

| | size MB | what |
|---|---:|---|
| CPython | 82.0 | interpreter install (`/opt/homebrew/opt/python@3.14/Frameworks/Python.framework/Versions/3.14`) + Flask and its dependencies (5.2) |
| PyPy | 137.7 | interpreter install (`~/.pyenv/versions/pypy3.11-7.3.19`) + Flask and its dependencies (4.9) |
| GraalPy | 343.1 | interpreter install (`graalpy-community3.13-25.4.4.1.1-macos-aarch64`) + Flask and its dependencies (11.2) |
| Codename One | 22.7 | one self-contained executable, Flask included |


## CommonWorkloads (common_compare.py; ms, peak RSS MB)

| workload | CPython ms | MB | PyPy ms | MB | GraalPy ms | MB | Codename One ms | MB | Codename One, hot spot in Java ms | MB |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| intArithmetic | 15124.8 | 15.0 | 141.8 | 32.1 | 114.2 | 353.0 | 1753.8 | 85.6 | 65.2 | 11.4 |
| longArithmetic | 11839.2 | 14.8 | 5410.0 | 41.6 | 11761.8 | 259.0 | 26378.6 | 135.3 | 40.6 | 11.3 |
| mathTranscendental | 1024.5 | 15.0 | 191.6 | 31.8 | 292.2 | 342.9 | 2015.7 | 93.0 | 180.8 | 11.4 |
| arraySequential | 1778.4 | 382.2 | 33.7 | 154.0 | 26.2 | 331.3 | 282.4 | 273.1 | 10.5 | 41.8 |
| arrayRandom | 4662.5 | 198.6 | 308.2 | 93.1 | 252.1 | 248.1 | 549.5 | 104.6 | 239.8 | 26.6 |
| objectAllocation | 527.8 | 15.1 | 19.1 | 63.3 | 62.0 | 344.0 | 68.2 | 133.1 | 16.6 | 191.7 |
| valueEscape | 805.3 | 14.8 | 6.3 | 32.0 | 59.1 | 330.7 | 114.2 | 91.0 | 5.0 | 11.5 |
| hashMapChurn | 160.9 | 25.5 | 43.6 | 77.6 | 97.8 | 243.5 | 127.3 | 126.6 | 20.8 | 30.3 |
| stringBuilding | 670.0 | 14.9 | 30.5 | 40.1 | 70.7 | 256.9 | 695.2 | 86.5 | 14.4 | 86.4 |
| recursion | 2227.7 | 14.8 | 181.9 | 57.2 | 193.3 | 280.4 | 355.1 | 11.0 | 87.6 | 11.3 |
| quicksort | 1779.8 | 83.6 | 171.4 | 58.0 | 160.0 | 267.6 | 410.4 | 113.5 | 89.3 | 17.2 |
