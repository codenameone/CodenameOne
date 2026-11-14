#!/bin/bash
# Builds one kernel as a native Linux application, the way a native desktop
# build lowers it: Python -> Java -> bytecode -> C -> clang, linked with the
# Linux port and its bundled SQLite. The port is the embedded variant (no
# GTK); run the binary with CN1_DISPLAY=headless. Linux only -- elsewhere use
# container/run.sh, which runs the whole table in a Linux container.
#
#   build_parparvm.sh <kernel> [maven repo]
#
# Every input comes from the checkout's own Maven repository (the second
# argument, else PYBENCH_M2, else <repo>/.m2-repo), never ~/.m2: concurrent checkouts overwrite each other's
# snapshots there. Install them first:
#
#   cd maven && mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -pl core,linux,python-transpiler -am install -DskipTests
#   JAVA_HOME=$JAVA17_HOME mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -pl python-runtime install -DskipTests
#   mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -f ../vm/pom.xml -pl JavaAPI,ByteCodeTranslator -am install -DskipTests
#
# PY_EXTRA_FILES names more modules the program imports (space separated),
# and PY_JAVA_SOURCES a directory of Java sources the Python calls into --
# the hot spots an application moves to Java (see common/java_hotspots.py).
#
# Output: scripts/python-bench/target/<kernel>/<kernel> (and the translated
# sources beside it). Needs JAVA17_HOME (javac for the generated code, which is
# Java 17) and JDK_8_HOME or JAVA_HOME (the translator), clang, cmake, ninja
# and the embedded port's libraries (freetype, libpng, libjpeg, zlib, libcrypto,
# libcurl; see container/Containerfile).
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
LINUX=$(art com/codenameone codenameone-linux 8.0-SNAPSHOT)
RUNTIME=$(art com/codenameone codenameone-python-runtime 8.0-SNAPSHOT)
TRANSPILER=$(art com/codenameone codenameone-python-transpiler 8.0-SNAPSHOT)
JAVAAPI=$(art com/codename1/parparvm JavaAPI 1.0-SNAPSHOT)
TRANSLATOR=$(art com/codename1/parparvm ByteCodeTranslator 1.0-SNAPSHOT)
for j in "$CORE" "$LINUX" "$RUNTIME" "$TRANSPILER" "$JAVAAPI" "$TRANSLATOR"; do
    [ -f "$j" ] || { echo "missing $j -- install it (see the header of this script)" >&2; exit 1; }
done

W="$HERE/target/$K"
rm -rf "$W"
mkdir -p "$W/src" "$W/gen/com/pybench" "$W/jclasses" "$W/javaapi" "$W/natives" "$W/out"
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
        com.codename1.ui.Display.init(null);
        com.codename1.python.runtime.Py.setArgv("bench", args);
        System.exit(com.codename1.generated.python.PythonModules.runMain());
    }
}
JAVA
fi
# The runtime, core and the Linux port, as an application ships them;
# dead-code elimination keeps what the program reaches. The translator reads
# class directories, not jars.
(cd "$W/javaapi" && unzip -qo "$JAVAAPI" 'java/*' 'com/*')
(cd "$W/jclasses" && unzip -qo "$RUNTIME" 'com/*' && unzip -qo "$CORE" 'com/*' && unzip -qo "$LINUX" 'com/*')
cp "$REPO"/Ports/LinuxPort/nativeSources/* "$W/natives/"
[ -n "$APPCP" ] && cp -R "$W/appclasses/." "$W/jclasses/"
"$J17/bin/javac" -nowarn --release 17 -cp "$W/jclasses" -d "$W/jclasses" $(find "$W/gen" -name '*.java')
"$J8/bin/java" -Xmx4g -Dcn1.sqlite=true -Dcn1.linux.embedded=true -cp "$TRANSLATOR" \
    com.codename1.tools.translator.ByteCodeTranslator \
    clean "$W/javaapi;$W/jclasses;$W/natives" "$W/out" PyBench com.pybench PyBench 1.0 linux none \
    > "$W/translate.log" 2>&1 || { tail -30 "$W/translate.log"; exit 1; }

# PY_OPT lowers the level for a build that tests behaviour, not speed: the
# conformance binary holds every program and takes minutes at -O3.
PY_OPT="${PY_OPT:--O3}"
cmake -S "$W/out/dist" -B "$W/out/build" -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_C_COMPILER="$CC" -DCMAKE_C_FLAGS_RELEASE="$PY_OPT -DNDEBUG" > "$W/cc.log" 2>&1 \
    && cmake --build "$W/out/build" >> "$W/cc.log" 2>&1 || { tail -40 "$W/cc.log"; exit 1; }
cp "$W/out/build/PyBench" "$W/$K"
# The symbols, which the binary names by debuglink, for perf.
[ -f "$W/out/build/PyBench.debug" ] && cp "$W/out/build/PyBench.debug" "$W/PyBench.debug"
