#!/bin/bash
# Runs the transpiler's conformance programs as native ParparVM executables
# and compares each output with CPython's recorded one (the .out beside it).
# The JVM run of the same programs is ConformanceTest; this is the device
# half, where a cast that fails is a crash instead of an exception.
#
#   conformance_parparvm.sh [program ...]
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
CONF="$REPO/maven/python-transpiler/src/test/resources/conformance"
progs="$*"
[ -n "$progs" ] || progs=$(cd "$CONF" && ls *.py | sed 's/\.py$//')
failed=0
for p in $progs; do
    if ! PY_SOURCES="$CONF" "$HERE/build_parparvm.sh" "$p" > /dev/null 2>&1; then
        echo "BUILD FAILED $p"; failed=1; continue
    fi
    got="$HERE/target/$p/output.txt"
    (cd "$HERE/target/$p/src" && "../$p" > "$got" 2>&1) || true
    if diff -q "$CONF/$p.out" "$got" > /dev/null; then
        echo "ok $p"
    else
        echo "DIFF $p"; diff "$CONF/$p.out" "$got" | head -20; failed=1
    fi
done
exit $failed
