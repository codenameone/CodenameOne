# Codename One Python vs CPython, PyPy and GraalPy

`compare.py` runs the same programs on each runtime and writes a Markdown
table; `RESULTS.md` is its last output. Every cell is a measurement on the
machine that ran it.

Rows:

- **Startup**: `hello.py`, best of ten.
- **CPU kernels**: `scripts/python-bench/kernels`, the fastest round.
- **Flask JSON API**: `scripts/python-backend/samples/flask-todo` under `ab`.
- **What you deploy**: interpreter install plus Flask, against one binary.

Build the Codename One binaries first:

```bash
for k in nbody richards fannkuch spectral_norm dicts_strings; do
  scripts/python-bench/build_parparvm.sh $k
done
PY_SOURCES=scripts/python-compare scripts/python-bench/build_parparvm.sh hello
scripts/python-backend/build.sh scripts/python-backend/samples/flask-todo /tmp/flask-todo
python3 scripts/python-compare/compare.py \
  --cpython <python with flask> --pypy <pypy3 with flask> [--graalpy <graalpy with flask>] \
  --cn1-kernels scripts/python-bench/target --cn1-hello scripts/python-bench/target/hello/hello \
  --cn1-server /tmp/flask-todo --out scripts/python-compare/RESULTS.md \
  --json scripts/python-compare/compare-results.json
```

GraalPy is an optional arm (`--graalpy`); `RESULTS.md` includes it. Install
Flask into it with `graalpy -m ensurepip` and `graalpy -m pip install flask`.

## CommonWorkloads

`common_compare.py` runs `com.bench.CommonWorkloads`, the benchmark every
Codename One port runs, ported to Python in `scripts/python-bench/common`.
Each workload runs in its own process, so the peak RSS is that workload's,
and every checksum must equal Java's. Codename One runs twice: the Python as
written, and Codename One Java, the same executable running the Java original
-- the floor the compiled Python is held against.

```bash
C=scripts/python-bench/common
PY_SOURCES=$C PY_EXTRA_FILES="$C/common_workloads.py $C/java_hotspots.py" \
  PY_JAVA_SOURCES=vm/benchmarks/common/src/main/java \
  scripts/python-bench/build_parparvm.sh common_bench
python3 scripts/python-compare/common_compare.py \
  --cn1 scripts/python-bench/target/common_bench/common_bench \
  --cpython <python> --pypy <pypy3> --graalpy <graalpy> --json common.json
python3 scripts/python-compare/render_charts.py common.json \
  --compare scripts/python-compare/compare-results.json --out docs/developer-guide/img
```
