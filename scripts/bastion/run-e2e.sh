#!/usr/bin/env bash
#
# The end-to-end run: starts the Bastion server on the `test` profile and
# drives it over HTTP the way the app does (e2e/LiveE2e.java).
#
#   run-e2e.sh [--native] [--prebuilt]
#
#   --native     runs against the compiled server
#   --prebuilt   uses the server as it was last built, without building it again
#
# Two customers open accounts; one enrols an authenticator and signs in with
# password and code through the authorization-code flow with PKCE, registers
# a device key, and pays the other with a request that key signs, while the
# unsigned and the replayed copies are refused. The two trade until an order
# fills, and coins are refused on their way out until the address is on the
# allow-list and past its cooling-off.
#
# Needs JAVA_HOME on JDK 17 or newer and the Codename One artifacts this
# project builds against in the local Maven repository (MAVEN_OPTS reaches
# the build, for -Dmaven.repo.local). Nothing here opens a window.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
PORT="${BASTION_PORT:-18090}"
LOG="${BASTION_SERVER_LOG:-$HERE/target/e2e-server.log}"
MODE="--jvm"
BUILD=1
for arg in "$@"; do
  case "$arg" in
    --native) MODE="--native" ;;
    --prebuilt) BUILD=0 ;;
    *) echo "[bastion-e2e] unknown option: $arg" >&2; exit 2 ;;
  esac
done

mkdir -p "$HERE/target"
echo "[bastion-e2e] java: $(java -version 2>&1 | head -1)"
if [ "$BUILD" = 1 ]; then
  "$HERE/backend/server.sh" build "$MODE"
fi

echo "[bastion-e2e] starting the server on port $PORT"
CN1_PROFILE=test "$HERE/backend/server.sh" run "$MODE" --prebuilt --port "$PORT" >"$LOG" 2>&1 &
SERVER_PID=$!
cleanup() {
  kill "$SERVER_PID" >/dev/null 2>&1 || true
  wait "$SERVER_PID" 2>/dev/null || true
}
trap cleanup EXIT

ready=0
for _ in $(seq 1 90); do
  if curl -fs -o /dev/null "http://localhost:$PORT/oauth2/jwks" 2>/dev/null; then
    ready=1
    break
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    break
  fi
  sleep 1
done
if [ "$ready" != "1" ]; then
  echo "[bastion-e2e] the server did not start; the end of its log:" >&2
  tail -n 80 "$LOG" >&2 || true
  exit 1
fi

set +e
java "$HERE/e2e/LiveE2e.java" "http://localhost:$PORT"
STATUS=$?
set -e
if [ "$STATUS" -ne 0 ]; then
  echo "[bastion-e2e] the run failed; the end of the server's log:" >&2
  tail -n 80 "$LOG" >&2 || true
  exit "$STATUS"
fi
