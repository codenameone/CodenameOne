#!/bin/bash
# Builds one kernel as a native ParparVM executable, the way an iOS or native
# desktop build lowers it: Python -> Java -> bytecode -> C -> clang -O3.
#
#   build_parparvm.sh <kernel> [maven repo]
#
# Every input comes from the checkout's own Maven repository (the second
# argument, else PYBENCH_M2, else <repo>/.m2-repo), never ~/.m2: concurrent checkouts overwrite each other's
# snapshots there. Install them first:
#
#   cd maven && mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -pl core,python-transpiler -am install -DskipTests
#   JAVA_HOME=$JAVA17_HOME mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -pl python-runtime install -DskipTests
#   mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -f ../vm/pom.xml -pl JavaAPI,ByteCodeTranslator -am install -DskipTests
#
# PY_EXTRA_FILES names more modules the program imports (space separated),
# and PY_JAVA_SOURCES a directory of Java sources the Python calls into --
# the hot spots an application moves to Java (see common/java_hotspots.py).
#
# Output: scripts/python-bench/target/<kernel>/<kernel> (and the translated
# sources beside it). Needs JAVA17_HOME (javac for the generated code, which is
# Java 17) and JDK_8_HOME or JAVA_HOME (the translator), and clang.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
K="$1"
[ -n "$K" ] || { echo "usage: build_parparvm.sh <kernel> [maven repo]" >&2; exit 2; }
M2="${2:-${PYBENCH_M2:-$REPO/.m2-repo}}"
J17="${JAVA17_HOME:?set JAVA17_HOME}"
J8="${JDK_8_HOME:-$JAVA_HOME}"
CC="${CC:-clang}"
art() { echo "$M2/$1/$2/$3/$2-$3.jar"; }
CORE=$(art com/codenameone codenameone-core 8.0-SNAPSHOT)
RUNTIME=$(art com/codenameone codenameone-python-runtime 8.0-SNAPSHOT)
TRANSPILER=$(art com/codenameone codenameone-python-transpiler 8.0-SNAPSHOT)
JAVAAPI=$(art com/codename1/parparvm JavaAPI 1.0-SNAPSHOT)
TRANSLATOR=$(art com/codename1/parparvm ByteCodeTranslator 1.0-SNAPSHOT)
for j in "$CORE" "$RUNTIME" "$TRANSPILER" "$JAVAAPI" "$TRANSLATOR"; do
    [ -f "$j" ] || { echo "missing $j -- install it (see the header of this script)" >&2; exit 1; }
done

W="$HERE/target/$K"
rm -rf "$W"
mkdir -p "$W/src" "$W/gen/com/pybench" "$W/jclasses" "$W/javaapi" "$W/out"
# PY_SOURCES names another directory of programs (the conformance suite).
SRC_DIR="${PY_SOURCES:-$HERE/kernels}"
cp "$SRC_DIR/$K.py" "$W/src/"
[ -f "$HERE/kernels/_harness.py" ] && cp "$HERE/kernels/_harness.py" "$W/src/"
for f in ${PY_EXTRA_FILES:-}; do cp "$f" "$W/src/"; done
APPCP=""
if [ -n "${PY_JAVA_SOURCES:-}" ]; then
    mkdir -p "$W/appclasses"
    "$J17/bin/javac" -nowarn --release 8 -d "$W/appclasses" $(find "$PY_JAVA_SOURCES" -name '*.java')
    APPCP=":$W/appclasses"
fi
# Java imports resolve against JavaAPI, the class library the binary links.
"$J17/bin/java" -cp "$TRANSPILER" com.codename1.python.transpiler.api.Main -cp "$JAVAAPI:$CORE$APPCP" "$W/src" "$W/gen" "$K"
# PY_DRIVER replaces the entry point: the conformance suite runs every
# program from one binary rather than building one each.
if [ -n "${PY_DRIVER:-}" ]; then
    cp "$PY_DRIVER" "$W/gen/com/pybench/PyBench.java"
else
cat > "$W/gen/com/pybench/PyBench.java" <<JAVA
package com.pybench;

public class PyBench {
    public static void main(String[] args) {
        com.codename1.python.runtime.Py.setArgv("bench", args);
        System.exit(com.codename1.generated.python.PythonModules.runMain());
    }
}
JAVA
fi
# The runtime, and the one core package it uses (com.codename1.util: BigInteger,
# MathUtil). The whole core would drag in the UI and its natives.
# The translator reads class directories, not jars.
(cd "$W/javaapi" && unzip -qo "$JAVAAPI" 'java/*')
# zlib, gzip and zipfile run over core's JZlib port (io/gzip); its one
# networking class extends ConnectionRequest, which is UI, so it stays out.
(cd "$W/jclasses" && unzip -qo "$RUNTIME" 'com/*' && unzip -qo "$CORE" 'com/codename1/util/*' \
    && unzip -qo "$CORE" 'com/codename1/io/gzip/*' -x 'com/codename1/io/gzip/GZConnectionRequest*')
# The file system the runtime reaches through core's FileSystemStorage, over
# the host's files (hostfs/): the real class would bring the UI with it.
"$J17/bin/javac" -nowarn --release 17 -d "$W/jclasses" $(find "$HERE/hostfs" -name '*.java')
[ -n "$APPCP" ] && cp -R "$W/appclasses/." "$W/jclasses/"
"$J17/bin/javac" -nowarn --release 17 -cp "$W/jclasses" -d "$W/jclasses" $(find "$W/gen" -name '*.java')
"$J8/bin/java" -Xmx4g -cp "$TRANSLATOR" com.codename1.tools.translator.ByteCodeTranslator \
    clean "$W/javaapi;$W/jclasses" "$W/out" PyBench com.pybench PyBench 1.0 clean none \
    > "$W/translate.log" 2>&1 || { tail -30 "$W/translate.log"; exit 1; }

# Compiled exactly as vm/selfhost compiles the self-hosted translator and its
# benchmarks, so the flags are the ones a shipped binary gets.
PYTHON=python3
WINDOWS=0
native_path() { echo "$1"; }
# PY_OPT lowers the level for a build that tests behaviour, not speed: the
# conformance binary holds every program and takes minutes at -O3 with LTO.
PY_OPT="${PY_OPT:--O3}"
[ "$PY_OPT" = "-O3" ] && CN1_SELFHOST_CFLAGS="-flto=thin $CN1_SELFHOST_CFLAGS"
. "$REPO/vm/selfhost/compile-dist.sh"
cn1_compile_dist "$W/out/dist" PyBench "$W/$K" "$PY_OPT" "$W/cc.log"
