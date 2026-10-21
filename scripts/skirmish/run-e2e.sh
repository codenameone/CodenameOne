#!/usr/bin/env bash
#
# Runs the client's end-to-end tests against a real server.
#
# Builds the server, starts it with the test profile (an in-memory database,
# simulated store receipts, the bot matched at once), then runs the
# simulator tests in common/src/test against it. The screenshots they take --
# the ones the developer guide shows -- land in target/e2e-shots.
#
#   run-e2e.sh [--prebuilt]
#
# On Linux it wraps the simulator in xvfb-run when that is installed.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
MVN="${MVN:-mvn}"
PORT="${SKIRMISH_PORT:-18090}"
LOG="${SKIRMISH_SERVER_LOG:-$HERE/target/e2e-server.log}"
SHOTS="$HERE/target/e2e-shots"
BUILD=1
for arg in "$@"; do
  case "$arg" in
    --prebuilt) BUILD=0 ;;
    *) echo "[skirmish-e2e] unknown option: $arg" >&2; exit 2 ;;
  esac
done

mkdir -p "$HERE/target"
rm -rf "$SHOTS"
mkdir -p "$SHOTS"

if [ "$BUILD" = 1 ]; then
  "$HERE/backend/server.sh" build
fi

echo "[skirmish-e2e] starting the server on port $PORT"
CN1_PROFILE=test "$HERE/backend/server.sh" run --prebuilt --port "$PORT" >"$LOG" 2>&1 &
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
  echo "[skirmish-e2e] the server did not start; its log:" >&2
  cat "$LOG" >&2 || true
  exit 1
fi

RUNNER=()
if command -v xvfb-run >/dev/null 2>&1; then
  RUNNER=(xvfb-run -a -s "-screen 0 1280x1024x24")
fi

export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Xmx768m -Dskirmish.server.url=http://localhost:$PORT -Dskirmish.shots.dir=$SHOTS"

${RUNNER[@]+"${RUNNER[@]}"} "$MVN" -B -f "$HERE/pom.xml" -Dcodename1.platform=javase -Ptest verify
