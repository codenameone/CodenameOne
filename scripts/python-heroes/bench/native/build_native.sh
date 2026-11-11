#!/bin/bash
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
#
# Builds a transpiled hero workload as a native ParparVM executable, the way
# an iOS build lowers it: the generated Java -> bytecode -> C -> clang -O3.
# No JVM is involved at run time. This is the arm that stands for the device.
#
#   build_native.sh <workload build dir> <output binary>
#
# The build dir is what HeroBuild wrote (gen/ and classes/cn1python.dat).
# Inputs come from the checkout's own Maven repository (PYBENCH_M2, else
# <repo>/.m2-repo), never ~/.m2; scripts/python-bench/build_parparvm.sh
# lists how to install them. Needs JAVA17_HOME, JDK_8_HOME or JAVA_HOME, clang.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../../../.." && pwd)"
B="$(cd "$1" && pwd)"
BIN="$2"
M2="${PYBENCH_M2:-$REPO/.m2-repo}"
J17="${JAVA17_HOME:?set JAVA17_HOME}"
J8="${JDK_8_HOME:-$JAVA_HOME}"
CC="${CC:-clang}"
art() { echo "$M2/$1/$2/$3/$2-$3.jar"; }
CORE=$(art com/codenameone codenameone-core 8.0-SNAPSHOT)
RUNTIME=${PYBENCH_RUNTIME:-$(art com/codenameone codenameone-python-runtime 8.0-SNAPSHOT)}
JAVAAPI=$(art com/codename1/parparvm JavaAPI 1.0-SNAPSHOT)
TRANSLATOR=$(art com/codename1/parparvm ByteCodeTranslator 1.0-SNAPSHOT)
for j in "$CORE" "$RUNTIME" "$JAVAAPI" "$TRANSLATOR"; do
    [ -f "$j" ] || { echo "missing $j" >&2; exit 1; }
done
W="$B/native"
# A fresh output directory every run: the translator reads every C file under it as
# a native source, and a previous run's output is tens of thousands of them.
# The last build stays beside this one as out.previous: a kept build directory
# (run_heroes.py --work) compiles again only what changed since. Anything older is
# moved aside, never deleted from here.
if [ -e "$W/out.previous" ]; then
    mv "$W/out.previous" "$W/out.$$.stale"
fi
if [ -e "$W/out" ]; then
    mv "$W/out" "$W/out.previous"
    export CN1_COMPILE_PREVIOUS="$W/out.previous"
fi
# The classes too: a kept build directory would hand the translator the previous
# program's modules beside this one's.
if [ -e "$W/jclasses" ]; then
    mv "$W/jclasses" "$W/jclasses.$$.stale"
fi
mkdir -p "$W/jclasses" "$W/javaapi" "$W/out"
# JdkHost with the class library: it is what java.security.SecureRandom reads, and
# core does not carry it.
(cd "$W/javaapi" && unzip -qo "$JAVAAPI" 'java/*' 'com/codename1/impl/JdkHost*')
# The runtime and all of core, as an application ships it. Dead-code elimination
# keeps what the program reaches; the runtime's HTTP bridge names ConnectionRequest
# and Display, so a partial core does not compile.
(cd "$W/jclasses" && unzip -qo "$RUNTIME" 'com/*' && unzip -qo "$CORE" 'com/*')
# The host's file system behind core's FileSystemStorage, as the python-bench
# kernels run, and SQLite behind PySql (HostSql over the system libsqlite3).
"$J17/bin/javac" -nowarn --release 17 -d "$W/jclasses" \
    $(find "$REPO/scripts/python-bench/hostfs" -name '*.java')
find "$B/gen" -name '*.java' > "$W/sources.txt"
echo "$HERE/HostSql.java" >> "$W/sources.txt"
echo "$HERE/HostEntropy.java" >> "$W/sources.txt"
echo "$HERE/NativeMain.java" >> "$W/sources.txt"
cp "$HERE"/cn1_hostsql.c "$W/jclasses/"
"$J17/bin/javac" -nowarn --release 17 -cp "$W/jclasses:$W/javaapi" -d "$W/jclasses" @"$W/sources.txt"
"$J8/bin/java" -Xmx8g -cp "$TRANSLATOR" com.codename1.tools.translator.ByteCodeTranslator \
    clean "$W/javaapi;$W/jclasses" "$W/out" NativeMain com.pyhero NativeMain 1.0 clean none \
    > "$W/translate.log" 2>&1 || { tail -30 "$W/translate.log"; exit 1; }
PYTHON=python3
WINDOWS=0
native_path() { echo "$1"; }
# One clang over every file is serial, and this is tens of thousands of files: compile
# them on every core. HERO_NATIVE_LTO=0 leaves out link-time optimisation, most of what
# is left of the build, for a binary a few percent slower (run_heroes.py --fast).
export CN1_COMPILE_JOBS="${CN1_COMPILE_JOBS:-$(getconf _NPROCESSORS_ONLN)}"
LTO="-flto=thin"
if [ "${HERO_NATIVE_LTO:-1}" = 0 ]; then
    LTO=""
fi
CN1_SELFHOST_CFLAGS="$LTO -lsqlite3 ${CN1_SELFHOST_CFLAGS:-}"
. "$REPO/vm/selfhost/compile-dist.sh"
cn1_compile_dist "$W/out/dist" NativeMain "$BIN" -O3 "$W/cc.log"
