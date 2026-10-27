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
  --cn1-server /tmp/flask-todo --out scripts/python-compare/RESULTS.md
```

GraalPy is an optional arm (`--graalpy`). It is not in `RESULTS.md` because
it was not installed on the machine that produced it.
