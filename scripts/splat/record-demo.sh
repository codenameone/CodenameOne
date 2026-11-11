#!/usr/bin/env bash
# Records the gameplay demos with sound -- one on the simulator's phone, one
# with the simulator as a desktop window -- into the directory given (default
# target/demo). Runs on a desktop with a display: the simulator's recorder
# films its own window. Preferences are kept in memory for the run
# (MemoryPreferences), so the developer's own simulator settings are untouched.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$(mkdir -p "${1:-$HERE/target/demo}" && cd "${1:-$HERE/target/demo}" && pwd)"
MVN="${MVN:-mvn}"
PORT="${SPLAT_PORT:-18090}"
LOG="$HERE/target/demo-server.log"
mkdir -p "$HERE/target"

"$HERE/backend/server.sh" build
CN1_PROFILE=test "$HERE/backend/server.sh" run --prebuilt --port "$PORT" >"$LOG" 2>&1 &
SERVER_PID=$!
trap 'kill "$SERVER_PID" >/dev/null 2>&1 || true; wait "$SERVER_PID" 2>/dev/null || true' EXIT
for _ in $(seq 1 90); do
  curl -fs -o /dev/null "http://localhost:$PORT/oauth2/jwks" 2>/dev/null && break
  kill -0 "$SERVER_PID" 2>/dev/null || { cat "$LOG" >&2; exit 1; }
  sleep 1
done

BASE="${JAVA_TOOL_OPTIONS:-} -Xmx2560m -Dsplat.server.url=http://localhost:$PORT -Dsplat.demo.dir=$OUT -Dsplat.e2e.only=DemoVideoTest,MissionBuilderTest -Djava.util.prefs.PreferencesFactory=com.codenameone.examples.splat.MemoryPreferences"
JAVA_TOOL_OPTIONS="$BASE" "$MVN" -B -f "$HERE/pom.xml" -Dcodename1.platform=javase -Ptest verify
JAVA_TOOL_OPTIONS="$BASE -Dsplat.e2e.desktop=true -Dsplat.prefs.seed=desktopSkin=true -Dcodename1.arg.desktop.titleBar=toolbar" \
  "$MVN" -B -f "$HERE/pom.xml" -Dcodename1.platform=javase -Ptest verify
ls -l "$OUT"
