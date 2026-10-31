# Codename One Python vs CPython, PyPy, GraalPy

Measured on Darwin arm64, 2026-10-28. Codename One rows are native binaries (Python -> Java -> ParparVM C -> clang -O3).

- CPython: `python3.14` in a venv with Flask (3.14.5 cpython)
- PyPy: `pypy3` in a venv with Flask (3.11.13 pypy)
- GraalPy: `graalpy` in a venv with Flask (3.13.14 graalpy)

## Startup: `print('hello')`

| | wall ms (best of 10) | peak RSS MB |
|---|---:|---:|
| CPython | 15.1 | 14.4 |
| PyPy | 13.9 | 29.1 |
| GraalPy | 28.2 | 118.6 |
| Codename One Python | 4.7 | 8.6 |

## CPU kernels (fastest of the kernel's rounds, ms; peak RSS MB)

| kernel | CPython ms | CPython MB | PyPy ms | PyPy MB | GraalPy ms | GraalPy MB | Codename One Python ms | Codename One Python MB |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| nbody | 764.4 | 14.9 | 61.9 | 32.4 | 180.4 | 236.2 | 88.0 | 10.3 |
| richards | 631.7 | 15.2 | 38.5 | 33.8 | 104.2 | 271.6 | 36.0 | 10.6 |
| fannkuch | 2606.4 | 14.6 | 581.9 | 32.9 | 519.0 | 247.8 | 567.9 | 10.0 |
| spectral_norm | 711.3 | 14.6 | 11.7 | 32.2 | 7.2 | 252.9 | 32.4 | 10.2 |
| dicts_strings | 386.5 | 62.6 | 133.8 | 80.4 | 213.3 | 433.9 | 188.9 | 125.1 |

## Flask JSON API (`scripts/python-backend/samples/flask-todo`)

`ab -n 20000 -c 8 /fib/30` after a 2000-request warm-up. CPython, PyPy and GraalPy serve with `app.run()` (Werkzeug's threaded server), Codename One with the same call, which it lowers onto the native backend HTTP server.

| | first response ms | idle RSS MB | RSS after load MB | req/s | p99 ms |
|---|---:|---:|---:|---:|---:|
| CPython | 81 | 39.1 | 39.9 | 611 | 16 |
| PyPy | 184 | 64.8 | 102.3 | 610 | 24 |
| GraalPy | 438 | 322.8 | 1270.4 | 468 | 59 |
| Codename One Python | 33 | 26.6 | 375.2 | 16749 | 1 |

## What you deploy

| | size MB | what |
|---|---:|---|
| CPython | 82.0 | interpreter install (`/opt/homebrew/opt/python@3.14/Frameworks/Python.framework/Versions/3.14`) + Flask and its dependencies (5.2) |
| PyPy | 110.5 | interpreter install (`pypy3.11-v7.3.20-macos_arm64`) + Flask and its dependencies (5.1) |
| GraalPy | 305.8 | interpreter install (`graalpy-community3.13-25.4.4.1.1-macos-aarch64`) + Flask and its dependencies (11.2) |
| Codename One Python | 22.8 | one self-contained executable, Flask included |


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
