#!/usr/bin/env bash
#
# Multi-protocol end-to-end test runner.
#
# Builds and starts the Spring Boot test server, then builds the Codename One
# client (full stack: @RestClient / @GraphQLClient / @GrpcClient generated-style
# sources -> process-annotations -> runtime) and runs its cn1:test suite, which
# performs real REST / GraphQL / gRPC round-trips against the server.
#
# Prerequisites (handled by the CI workflow before calling this script):
#   - codenameone-core / codenameone-javase / codenameone-maven-plugin built
#     from this checkout and installed into the local Maven repo.
#   - JAVA_HOME pointing at a JDK 17 (required by Spring Boot 3 and the client).
#
# The client runs the Codename One simulator, which needs a display; this
# script uses xvfb-run when available (Linux CI) and runs directly otherwise
# (e.g. a developer machine with a real display).
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$HERE/server"
CLIENT_DIR="$HERE/client"
MVN="${MVN:-mvn}"
SERVER_LOG="${SERVER_LOG:-/tmp/protocol-e2e-server.log}"
PORT="${E2E_PORT:-8080}"

echo "[protocol-e2e] java: $(java -version 2>&1 | head -1)"

echo "[protocol-e2e] Building server jar..."
"$MVN" -B -q -f "$SERVER_DIR/pom.xml" -DskipTests package

echo "[protocol-e2e] Starting server on port $PORT..."
java -jar "$SERVER_DIR/target/protocol-e2e-server.jar" --server.port="$PORT" >"$SERVER_LOG" 2>&1 &
SERVER_PID=$!
cleanup() {
    echo "[protocol-e2e] Stopping server (pid $SERVER_PID)..."
    kill "$SERVER_PID" >/dev/null 2>&1 || true
    wait "$SERVER_PID" 2>/dev/null || true
}
trap cleanup EXIT

echo "[protocol-e2e] Waiting for server readiness..."
ready=0
for _ in $(seq 1 90); do
    if curl -fs -o /dev/null "http://localhost:$PORT/api/products" 2>/dev/null; then
        ready=1
        break
    fi
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        echo "[protocol-e2e] Server process exited early; log:" >&2
        cat "$SERVER_LOG" >&2 || true
        exit 1
    fi
    sleep 1
done
if [ "$ready" != "1" ]; then
    echo "[protocol-e2e] Server did not become ready in time; log:" >&2
    cat "$SERVER_LOG" >&2 || true
    exit 1
fi
echo "[protocol-e2e] Server is up."

RUNNER=""
if command -v xvfb-run >/dev/null 2>&1; then
    RUNNER="xvfb-run -a"
    echo "[protocol-e2e] Using xvfb-run for the simulator."
fi

echo "[protocol-e2e] Building + running the Codename One client (cn1:test)..."
set +e
$RUNNER "$MVN" -B -f "$CLIENT_DIR/pom.xml" install \
    -Dcodename1.platform=javase \
    -De2e.server.url="http://localhost:$PORT"
STATUS=$?
set -e

if [ "$STATUS" -ne 0 ]; then
    echo "[protocol-e2e] Client tests FAILED (status $STATUS). Server log tail:" >&2
    tail -n 50 "$SERVER_LOG" >&2 || true
    exit "$STATUS"
fi

echo "[protocol-e2e] All three protocols (REST / GraphQL / gRPC) verified end-to-end."
