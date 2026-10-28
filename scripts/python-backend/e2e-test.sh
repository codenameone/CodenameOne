#!/bin/bash
# End-to-end test of the REAL server path of a Python backend: builds the
# FastAPI and Flask samples (build.sh --jvm, the same generated server glue
# a native build uses, on the backend's JavaSE runtime), starts each, and
# drives it over real sockets with e2e_client.py -- HTTP routes, both kinds
# of middleware, websockets and streamed responses.
#
#   e2e-test.sh [--native] [maven repo]
#
# --native builds each sample with build.sh's native path instead (ParparVM
# translation, clang) and drives the binary with the same checks. That is
# the only run of the GIL's virtual-thread scheduler, which the generated
# BackendServer installs and which exists only under ParparVM.
#
# Needs JAVA17_HOME, python3, and the inputs build.sh lists in its header.
# --native also needs a JDK 8 for the backend build and the translator:
# JDK_8_HOME, else JAVA_HOME when that is a JDK 8, else (macOS) the one
# /usr/libexec/java_home finds. A native run takes over ten minutes.
set -e
BUILD_MODE=--jvm
if [ "$1" = "--native" ]; then BUILD_MODE=; shift; fi
is_jdk8() {
    [ -x "$1/bin/javac" ] && "$1/bin/javac" -version 2>&1 | grep -q ' 1\.8\.'
}
if [ -z "$BUILD_MODE" ] && [ -z "$JDK_8_HOME" ]; then
    if [ -n "$JAVA_HOME" ] && is_jdk8 "$JAVA_HOME"; then
        JDK_8_HOME="$JAVA_HOME"
    elif [ -x /usr/libexec/java_home ] && J8="$(/usr/libexec/java_home -v 1.8 2>/dev/null)" && is_jdk8 "$J8"; then
        JDK_8_HOME="$J8"
    else
        echo "--native needs a JDK 8: set JDK_8_HOME" >&2
        exit 2
    fi
    export JDK_8_HOME
fi
HERE="$(cd "$(dirname "$0")" && pwd)"
M2="${1:-$(cd "$HERE/../.." && pwd)/.m2-repo}"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cn1-py-e2e.XXXXXX")"
PIDS=()
cleanup() {
    for p in "${PIDS[@]}"; do kill "$p" 2>/dev/null || true; done
}
trap cleanup EXIT

free_port() {
    python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])'
}

run_sample() {
    local name="$1" kind="$2"
    "$HERE/build.sh" $BUILD_MODE "$HERE/samples/$name" "$WORK/$name" "$M2"
    local port
    port="$(free_port)"
    "$WORK/$name" "$port" > "$WORK/$name.log" 2>&1 &
    local pid=$!
    PIDS+=("$pid")
    for _ in $(seq 1 100); do
        if grep -q "serving on port" "$WORK/$name.log"; then break; fi
        if ! kill -0 "$pid" 2>/dev/null; then break; fi
        sleep 0.2
    done
    grep -q "serving on port" "$WORK/$name.log" || { echo "$name did not start:"; cat "$WORK/$name.log"; exit 1; }
    local status=0
    python3 "$HERE/e2e_client.py" "$kind" "$port" || status=$?
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    if [ "$status" != 0 ]; then
        echo "--- $name server log"; tail -50 "$WORK/$name.log"; exit "$status"
    fi
}

run_sample fastapi-chat fastapi
run_sample flask-todo flask
echo "python backend e2e${BUILD_MODE:+ ($BUILD_MODE)}${BUILD_MODE:- (native)}: passed"
