#!/bin/bash
# Runs the transpiler's conformance programs as native ParparVM executables
# and compares each output with CPython's recorded one (the .out beside it).
# The JVM run of the same programs is ConformanceTest; this is the device
# half, where a cast that fails is a crash instead of an exception.
#
#   conformance_parparvm.sh [program ...]
#
# Every program goes into ONE binary, run once: a native build costs minutes
# (clang -O3 over the whole runtime), and one per program was nine hours of
# runner time. A Java driver runs each module as __main__ in turn and prints
# a marker line before it, and the output is split on those markers.
#
# The directory programs (cext_, imports_, ...) are projects of their own,
# with natives and Java beside the Python, and stay on the JVM run.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
CONF="$REPO/maven/python-transpiler/src/test/resources/conformance"
progs="$*"
# http_* programs talk to ConformanceTest's server through the host's HTTP
# client, which a bare native executable has none of (ConnectionRequest
# needs the UI port): they run on the JVM only. The packages they test are
# Python, so this binary runs the same code paths everywhere but the socket.
# java_interop* call the Codename One UI classes, pil_* draw through the
# port's images, and sqlite3_ and the SQLAlchemy programs (through its
# sqlite dialect) open core's Database: each needs a port to link.
[ -n "$progs" ] || progs=$(cd "$CONF" && ls *.py | sed 's/\.py$//' \
    | grep -vE '^(http_|java_interop|pil_|sqlalchemy_|guide_sqlalchemy$|sqlite3_$)')
K=conformance_all
STAGE="$HERE/target/$K-stage"
mkdir -p "$STAGE"
echo "pass" > "$STAGE/$K.py"
MARK="@@CN1CONF@@"
{
    echo "package com.pybench;"
    echo
    echo "public class PyBench {"
    echo "    private static void run(String name, Runnable r) {"
    echo "        System.out.print(\"\\n$MARK \" + name + \"\\n\");"
    echo "        com.codename1.python.runtime.Gil.enter();"
    echo "        try {"
    echo "            r.run();"
    echo "        } catch (com.codename1.python.runtime.PyException e) {"
    echo "            com.codename1.python.runtime.StdModules.exitStatus(e);"
    echo "        } finally {"
    echo "            com.codename1.python.runtime.StdModules.runExitHandlers();"
    echo "            com.codename1.python.runtime.Gil.exit();"
    echo "        }"
    echo "    }"
    echo
    echo "    public static void main(String[] args) {"
    echo "        com.codename1.generated.python.PythonModules.registerAll();"
    for p in $progs; do
        echo "        com.codename1.python.runtime.Py.setArgv(\"$p\", args);"
        echo "        run(\"$p\", () -> com.codename1.generated.python.py_$p.exec(\"__main__\"));"
    done
    echo "        System.out.print(\"\\n$MARK end\\n\");"
    echo "        System.exit(0);"
    echo "    }"
    echo "}"
} > "$STAGE/PyBench.java"
extra=""
for p in $progs; do extra="$extra $CONF/$p.py"; done
if ! PY_SOURCES="$STAGE" PY_EXTRA_FILES="$extra" PY_DRIVER="$STAGE/PyBench.java" PY_OPT=-O1 \
        CN1_COMPILE_JOBS="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)" \
        "$HERE/build_parparvm.sh" "$K" > "$STAGE/build.log" 2>&1; then
    tail -40 "$STAGE/build.log"
    echo "BUILD FAILED"
    exit 1
fi
got="$HERE/target/$K/output.txt"
(cd "$HERE/target/$K/src" && "../$K" > "$got" 2>&1) || true
python3 - "$CONF" "$got" "$MARK" $progs <<'PY'
import difflib, sys
conf, got, mark, progs = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:]
text = open(got, encoding="utf-8", errors="replace").read()
sections = {}
for chunk in text.split("\n" + mark + " ")[1:]:
    name, _, body = chunk.partition("\n")
    sections[name] = body
failed = False
if "end" not in sections:
    # Whatever ran last crashed the process; it and everything after it
    # are reported, rather than read as empty output.
    print("the binary did not finish; last program started: %s"
          % (list(sections)[-1] if sections else "none"))
    failed = True
for p in progs:
    want = open("%s/%s.out" % (conf, p), encoding="utf-8").read()
    if p not in sections:
        print("NOT RUN %s" % p)
        failed = True
    elif sections[p] == want:
        print("ok %s" % p)
    else:
        print("DIFF %s" % p)
        diff = difflib.unified_diff(want.splitlines(), sections[p].splitlines(),
                                    "expected", "native", lineterm="")
        for line in list(diff)[:20]:
            print(line)
        failed = True
sys.exit(1 if failed else 0)
PY
