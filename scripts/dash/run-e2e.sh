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
PORT="${DASH_PORT:-18090}"
LOG="${DASH_SERVER_LOG:-$HERE/target/e2e-server.log}"
SHOTS="$HERE/target/e2e-shots"
BUILD=1
for arg in "$@"; do
  case "$arg" in
    --prebuilt) BUILD=0 ;;
    *) echo "[dash-e2e] unknown option: $arg" >&2; exit 2 ;;
  esac
done

mkdir -p "$HERE/target"
rm -rf "$SHOTS"
mkdir -p "$SHOTS"

if [ "$BUILD" = 1 ]; then
  "$HERE/backend/server.sh" build
fi

echo "[dash-e2e] starting the server on port $PORT"
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
  echo "[dash-e2e] the server did not start; its log:" >&2
  cat "$LOG" >&2 || true
  exit 1
fi

RUNNER=()
if command -v xvfb-run >/dev/null 2>&1; then
  RUNNER=(xvfb-run -a -s "-screen 0 1280x1024x24")
fi

# Every test runs in one simulator, and the Forge's turntable previews and
# 3D battles hold enough textures that 768m ran out in the test after them.
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Xmx1536m -Ddash.server.url=http://localhost:$PORT -Ddash.shots.dir=$SHOTS"

set +e
${RUNNER[@]+"${RUNNER[@]}"} "$MVN" -B -f "$HERE/pom.xml" -Dcodename1.platform=javase -Ptest verify
STATUS=$?
set -e

# The second pass: the same tests with the simulator as a desktop window and
# not a phone, which is where the admin console is a side bar and a page and
# the Promote tab is shown (PromoteTest). Each test knows which pass it is for
# and passes untouched in the other (E2e.desktop()).
#
# The simulator is a desktop when its "desktopSkin" preference says so; there
# is no switch for it on the command line. Java keeps preferences in files only
# on Linux, where java.util.prefs.userRoot can point them at a directory of
# this run's own -- so that is where this pass runs, which covers CI and a
# container. Elsewhere it would mean rewriting the developer's own simulator
# preferences, and it is skipped with a word.
if [ "$STATUS" -eq 0 ] && [ "$(uname -s)" = "Linux" ]; then
  PREFS="$HERE/target/e2e-desktop-prefs"
  NODE="$PREFS/.java/.userPrefs/com/codename1/impl/javase"
  rm -rf "$PREFS"
  mkdir -p "$NODE"
  cat >"$NODE/prefs.xml" <<'XML'
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">
<map MAP_XML_VERSION="1.0">
  <entry key="desktopSkin" value="true"/>
</map>
XML
  echo "[dash-e2e] the desktop pass"
  export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Ddash.e2e.desktop=true -Djava.util.prefs.userRoot=$PREFS -Dcodename1.arg.desktop.titleBar=native"
  set +e
  ${RUNNER[@]+"${RUNNER[@]}"} "$MVN" -B -f "$HERE/pom.xml" -Dcodename1.platform=javase -Ptest verify
  STATUS=$?
  set -e
elif [ "$STATUS" -eq 0 ]; then
  echo "[dash-e2e] not on Linux: the desktop pass was skipped"
fi

if [ "$STATUS" -ne 0 ]; then
  echo "[dash-e2e] the tests failed; the end of the server's log:" >&2
  tail -n 80 "$LOG" >&2 || true
  exit "$STATUS"
fi
