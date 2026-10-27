# Codename One Python vs CPython, PyPy

Measured on Darwin arm64, 2026-10-27. Codename One rows are native binaries (Python -> Java -> ParparVM C -> clang -O3).

- CPython: `python3.14 (venv with Flask)` (3.14.5 cpython)
- PyPy: `~/.pyenv/versions/pypy3.11-7.3.19/bin/pypy3` (3.11.11 pypy)

## Startup: `print('hello')`

| | wall ms (best of 10) | peak RSS MB |
|---|---:|---:|
| CPython | 21.2 | 14.4 |
| PyPy | 20.4 | 29.2 |
| Codename One | 7.6 | 8.7 |

## CPU kernels (fastest of the kernel's rounds, ms; peak RSS MB)

| kernel | CPython ms | CPython MB | PyPy ms | PyPy MB | Codename One ms | Codename One MB |
|---|---:|---:|---:|---:|---:|---:|
| nbody | 785.9 | 15.5 | 63.7 | 32.6 | 90.1 | 10.3 |
| richards | 653.9 | 15.3 | 40.3 | 34.0 | 44.7 | 10.6 |
| fannkuch | 2691.2 | 14.6 | 612.5 | 32.8 | 731.8 | 10.2 |
| spectral_norm | 734.5 | 14.8 | 11.7 | 32.2 | 11.1 | 10.3 |
| dicts_strings | 397.5 | 62.2 | 145.0 | 96.7 | 211.2 | 128.9 |

## Flask JSON API (`scripts/python-backend/samples/flask-todo`)

`ab -n 20000 -c 8 /fib/30` after a 2000-request warm-up. CPython, PyPy and GraalPy serve with `app.run()` (Werkzeug's threaded server), Codename One with the same call, which it lowers onto the native backend HTTP server.

| | first response ms | idle RSS MB | RSS after load MB | req/s | p99 ms |
|---|---:|---:|---:|---:|---:|
| CPython | 97 | 39.3 | 24.5 | 687 | 16 |
| PyPy | 200 | 64.9 | 86.5 | 674 | 18 |
| Codename One | 61 | 26.7 | 390.4 | 16332 | 1 |

## What you deploy

| | size MB | what |
|---|---:|---|
| CPython | 82.0 | interpreter install (`/opt/homebrew/opt/python@3.14/Frameworks/Python.framework/Versions/3.14`) + Flask and its dependencies (5.2) |
| PyPy | 137.7 | interpreter install (`~/.pyenv/versions/pypy3.11-7.3.19`) + Flask and its dependencies (4.9) |
| Codename One | 22.7 | one self-contained executable, Flask included |

