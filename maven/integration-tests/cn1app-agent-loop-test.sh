#!/bin/bash
# Gate on the loop an AI agent uses to verify its own UI work, with no display.
#
# Agents working in Codename One projects reported "cannot verify, the
# environment is headless" instead of running the app, because nothing they
# were told showed them how. cn1:agent and the skill's tools/Sim.java are that
# path; this proves it works from a freshly generated project exactly as an
# agent would run it:
#
#   * DISPLAY and WAYLAND_DISPLAY are cleared, and this must never be wrapped
#     in xvfb-run: cn1:agent has to start Xvfb itself.
#   * Sim.java is the copy the archetype shipped into the project, so a skill
#     packaging change that drops it fails here.
#   * The default port is deliberately taken first, so the free-port fallback
#     and the port file Sim.java reads are exercised.
#
# The project is generated at javaVersion 17 whatever JDK runs Maven: only
# Java 17 projects ship the skill, and that is the project an agent works in.
#
# Needs a JDK 11+ for the simulator and 17+ for Sim.java: AGENT_JDK (default:
# CN1_BACKEND_PACKAGE_JDK, which the workflow sets to its JDK 21).
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh

AGENT_JDK="${AGENT_JDK:-${CN1_BACKEND_PACKAGE_JDK:-$JAVA_HOME}}"
JAVA="$AGENT_JDK/bin/java"
APP=myappagent

cd $SCRIPTPATH/build
if [ -d $APP ]; then
  rm -rf $APP
fi
mvn -B -ntp archetype:generate \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion=$CN1_VERSION \
  -DartifactId=$APP \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DmainName=MyApp \
  -DjavaVersion=17 \
  -DinteractiveMode=false
cd $APP
SIM=.agent-skills/codename-one/tools/Sim.java
if [ ! -f "$SIM" ]; then
  echo "The generated project has no $SIM; the skill packaging dropped it." >&2
  exit 1
fi
OUT=$(pwd -P)/agent-out
mkdir -p "$OUT"

# Hold 8765 so cn1:agent must choose another port and record it.
python3 -c 'import socket,time; s=socket.socket(); s.bind(("127.0.0.1",8765)); s.listen(1); time.sleep(3600)' &
BLOCKER=$!
for i in $(seq 1 50); do
  python3 -c 'import socket,sys; s=socket.socket(); sys.exit(s.connect_ex(("127.0.0.1",8765)))' && break
  sleep 0.2
done

# Job control gives the agent its own process group, so cleanup can stop
# Maven, the simulator it forks and the CSS watcher together.
set -m
env -u DISPLAY -u WAYLAND_DISPLAY JAVA_HOME="$AGENT_JDK" PATH="$AGENT_JDK/bin:$PATH" \
  mvn -B -ntp cn1:agent > "$OUT/agent.log" 2>&1 &
AGENT=$!
cleanup() {
  kill -- -$AGENT 2>/dev/null || true
  kill $BLOCKER 2>/dev/null || true
}
trap cleanup EXIT

fail() {
  echo "FAIL: $1" >&2
  tail -n 60 "$OUT/agent.log" >&2
  exit 1
}

# The port file appears before the simulator starts.
for i in $(seq 1 120); do
  [ -f common/target/cn1-agent.port ] && break
  sleep 1
done
[ -f common/target/cn1-agent.port ] || fail "cn1:agent wrote no common/target/cn1-agent.port"
PORT=$(cat common/target/cn1-agent.port)
[ "$PORT" != "8765" ] || fail "cn1:agent kept port 8765 although it was taken"

"$JAVA" $SIM wait 600 || fail "the simulator never answered on port $PORT"
"$JAVA" $SIM snapshot > "$OUT/snapshot.json"
grep -q '"label":"Hello World"' "$OUT/snapshot.json" || fail "the first form has no Hello World button"
"$JAVA" $SIM screenshot "$OUT/before.png"
"$JAVA" $SIM record-start "$OUT/flow.mp4" > "$OUT/record-start.json"
"$JAVA" $SIM tap "Hello World" > "$OUT/tap.json"
sleep 2
"$JAVA" $SIM snapshot > "$OUT/after.json"
grep -q 'Hello Codename One' "$OUT/after.json" || fail "tapping Hello World did not open the dialog"
"$JAVA" $SIM screenshot "$OUT/after.png"
"$JAVA" $SIM record-stop > "$OUT/record-stop.json"

for png in before after; do
  python3 -c 'import sys; sys.exit(open(sys.argv[1],"rb").read(4) != b"\x89PNG")' "$OUT/$png.png" \
    || fail "$png.png is not a PNG"
done
cmp -s "$OUT/before.png" "$OUT/after.png" && fail "the screenshot did not change after the tap"
# Without ffmpeg the recorder writes PNG frames and says so; either is a recording.
if grep -q '"path"' "$OUT/record-stop.json"; then
  echo "recording: $(cat "$OUT/record-stop.json")"
else
  fail "record-stop returned no path: $(cat "$OUT/record-stop.json")"
fi
echo "Agent loop OK on port $PORT: snapshot, tap, screenshots and a recording with no display."
