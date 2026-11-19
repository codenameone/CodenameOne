#!/bin/bash
# Runs parparvm_table.py inside the benchmark container (Containerfile), with
# PyPy and GraalPy as extra columns. Arguments go to the table:
#
#   scripts/python-bench/container/run.sh --rounds 5 sqlite_rows json_codec
#
# The checkout is mounted at /cn1; the framework comes from its own Maven
# repository (<repo>/.m2-repo, or PYBENCH_M2), installed with the commands in
# build_parparvm.sh's header. Uses podman, or docker when podman is absent.
# PYBENCH_OTHERS=0 leaves PyPy and GraalPy out, for quick iterations.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"
M2="${PYBENCH_M2:-$REPO/.m2-repo}"
ENGINE="$(command -v podman || command -v docker)"
IMAGE=cn1-python-bench
"$ENGINE" build -q -t "$IMAGE" -f "$HERE/Containerfile" "$HERE" > /dev/null
OTHERS="--impl pypy=/opt/pypy/bin/pypy3 --impl graalpy=/opt/graalpy/bin/graalpy"
[ "${PYBENCH_OTHERS:-1}" = 0 ] && OTHERS=""
exec "$ENGINE" run --rm -v "$REPO":/cn1 -v "$M2":/m2 -w /cn1 \
    -e PYBENCH_M2=/m2 "$IMAGE" \
    sh -c 'export JAVA17_HOME=$(ls -d /usr/lib/jvm/java-17-openjdk-*); \
        exec python3 scripts/python-bench/parparvm_table.py "$@"' table $OTHERS "$@"
