#!/usr/bin/env bash
#
# The end-to-end tests: starts the Wayline server on the `test` profile, then
# runs the app's test suite against it in the Codename One simulator.
#
#   run-e2e.sh [--native] [--prebuilt]
#
#   --native     runs the tests against the compiled server
#   --prebuilt   uses the server as it was last built, without building it again
#
# The tests drive the real screens: they sign in, request a ride, drive it and
# administer it, while a second user acts through the API. They capture a few
# screens into target/e2e-shots and hold them against screenshots/.
#
# Needs JAVA_HOME on a JDK 17, and the Codename One artifacts this project
# builds against in the local Maven repository. The simulator needs a display:
# xvfb-run is used when there is one, which is what makes this run on a CI
# machine and in a container.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
MVN="${MVN:-mvn}"
PORT="${WAYLINE_PORT:-18080}"
LOG="${WAYLINE_SERVER_LOG:-$HERE/target/e2e-server.log}"
SHOTS="$HERE/target/e2e-shots"
MODE="--jvm"
BUILD=1
for arg in "$@"; do
  case "$arg" in
    --native) MODE="--native" ;;
    --prebuilt) BUILD=0 ;;
    *) echo "[wayline-e2e] unknown option: $arg" >&2; exit 2 ;;
  esac
done

mkdir -p "$HERE/target"
rm -rf "$SHOTS"
mkdir -p "$SHOTS"

echo "[wayline-e2e] java: $(java -version 2>&1 | head -1)"
if [ "$BUILD" = 1 ]; then
  "$HERE/backend/server.sh" build "$MODE"
fi

echo "[wayline-e2e] starting the server on port $PORT"
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
  echo "[wayline-e2e] the server did not start; its log:" >&2
  cat "$LOG" >&2 || true
  exit 1
fi

RUNNER=()
if command -v xvfb-run >/dev/null 2>&1; then
  RUNNER=(xvfb-run -a -s "-screen 0 1280x1024x24")
fi

# The simulator is a JVM the build forks, and JAVA_TOOL_OPTIONS is what reaches
# it: where the server is, where the captures go and where the goldens are.
# The heap is given a ceiling too. Left alone a JVM takes a quarter of the
# machine's memory before it collects anything, which on a shared runner is
# enough to have the kernel end it between two tests.
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Xmx768m -Dwayline.server.url=http://localhost:$PORT -Dwayline.shots.dir=$SHOTS -Dwayline.goldens.dir=$HERE/screenshots"

set +e
${RUNNER[@]+"${RUNNER[@]}"} "$MVN" -B -f "$HERE/pom.xml" -Dcodename1.platform=javase -Ptest verify
STATUS=$?
set -e

# The second pass: the same tests with the simulator as a desktop window and
# not a phone, which is where the admin console is laid out side by side and
# wears the platform's own theme. Each test knows which pass it is for and
# passes untouched in the other (E2e.desktop()).
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
  echo "[wayline-e2e] the desktop pass"
  # The last two are the project's own build hints, from
  # common/codenameone_settings.properties. A packaged desktop app is given
  # them by its launcher; a test run starts the simulator in
  # javase/target/cn1-reports, from where it does not find the project's
  # settings, so it has to be told.
  export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dwayline.e2e.desktop=true -Djava.util.prefs.userRoot=$PREFS -Dcodename1.arg.nativeTheme=native -Dcodename1.arg.desktop.titleBar=native"
  set +e
  ${RUNNER[@]+"${RUNNER[@]}"} "$MVN" -B -f "$HERE/pom.xml" -Dcodename1.platform=javase -Ptest verify
  STATUS=$?
  set -e
elif [ "$STATUS" -eq 0 ]; then
  echo "[wayline-e2e] not on Linux: the desktop pass was skipped"
fi

if [ "$STATUS" -ne 0 ]; then
  echo "[wayline-e2e] the tests failed; the end of the server's log:" >&2
  tail -n 80 "$LOG" >&2 || true
  exit "$STATUS"
fi
echo "[wayline-e2e] passed; captures are in $SHOTS"
