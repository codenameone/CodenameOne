#!/bin/bash
# Builds a Python web application (Flask or FastAPI) as a native backend
# binary: Python -> Java -> bytecode, then vm/backend/build.sh translates it
# with the backend runtime and its HTTP server into C and compiles it.
#
#   build.sh <dir with src/main.py> <out binary> [maven repo]
#
# The application serves itself the usual way -- `app.run(port=...)` or
# `uvicorn.run(app, ...)` -- and the transpiler bridges that to the backend's
# HttpServer because the backend is on the class path.
#
# Inputs come from the checkout's own Maven repository (third argument, else
# <repo>/.m2-repo). Install them first:
#
#   cd maven && mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -pl core,python-transpiler,backend -am install -DskipTests
#   JAVA_HOME=$JAVA17_HOME mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -Pflutter-modules -pl python-runtime install -DskipTests
#   mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -f ../vm/pom.xml -pl JavaAPI,ByteCodeTranslator -am install -DskipTests
#
# Needs JAVA17_HOME (javac for the generated code) and JDK_8_HOME or
# JAVA_HOME (the backend build and the translator), and clang.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
APP="$(cd "${1:?usage: build.sh <app dir> <out binary> [maven repo]}" && pwd)"
OUT="${2:?usage: build.sh <app dir> <out binary> [maven repo]}"
case "$OUT" in /*) ;; *) OUT="$(pwd)/$OUT" ;; esac
M2="${3:-$REPO/.m2-repo}"
J17="${JAVA17_HOME:?set JAVA17_HOME}"
art() { echo "$M2/$1/$2/$3/$2-$3.jar"; }
CORE=$(art com/codenameone codenameone-core 8.0-SNAPSHOT)
RUNTIME=$(art com/codenameone codenameone-python-runtime 8.0-SNAPSHOT)
TRANSPILER=$(art com/codenameone codenameone-python-transpiler 8.0-SNAPSHOT)
BACKEND=$(art com/codenameone codenameone-backend 8.0-SNAPSHOT)
JAVAAPI=$(art com/codename1/parparvm JavaAPI 1.0-SNAPSHOT)
for j in "$CORE" "$RUNTIME" "$TRANSPILER" "$BACKEND" "$JAVAAPI"; do
    [ -f "$j" ] || { echo "missing $j -- install it (see the header of this script)" >&2; exit 1; }
done

W="$APP/target/backend"
mkdir -p "$W"
W="$(cd "$W" && pwd)"
GEN="$W/gen"
CLASSES="$W/classes"
DEMO="$W/demo"
mkdir -p "$GEN" "$CLASSES" "$DEMO"
"$J17/bin/java" -cp "$TRANSPILER" com.codename1.python.transpiler.api.Main -cp "$JAVAAPI:$CORE:$BACKEND" \
    "$APP/src" "$GEN" main
[ -f "$GEN/com/codename1/generated/python/BackendServer.java" ] || {
    echo "the application does not serve anything (no Flask/FastAPI run): nothing to build" >&2; exit 1; }
mkdir -p "$GEN/com/pybackend"
cat > "$GEN/com/pybackend/PyBackendMain.java" <<JAVA
package com.pybackend;

public class PyBackendMain {
    public static void main(String[] args) {
        com.codename1.python.runtime.Py.setArgv("main", args);
        System.exit(com.codename1.generated.python.PythonModules.runMain());
    }
}
JAVA
# The runtime, the one core package it uses, and the host file system shim.
(cd "$CLASSES" && unzip -qo "$RUNTIME" 'com/*' && unzip -qo "$CORE" 'com/codename1/util/*')
"$J17/bin/javac" -nowarn --release 17 -d "$CLASSES" $(find "$REPO/scripts/python-bench/hostfs" -name '*.java')
"$J17/bin/javac" -nowarn --release 17 -cp "$CLASSES:$BACKEND" -d "$CLASSES" $(find "$GEN" -name '*.java')

# The backend build compiles a demo directory with JDK 8; the application is
# already compiled, so it is an empty one and the classes come in through
# CN1_BACKEND_EXTRA_CLASSES.
export JDK_8_HOME="${JDK_8_HOME:-$JAVA_HOME}"
cd "$REPO/vm/backend"
CN1_BACKEND_DEMO="$DEMO" CN1_BACKEND_STANDALONE_DEMO=1 CN1_BACKEND_EXTRA_CLASSES="$CLASSES" \
    ./build.sh PyBackendMain com.pybackend "$OUT"
