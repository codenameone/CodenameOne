#!/usr/bin/env bash
# Records the app walkthroughs in podman/Xvfb; see README.md.
#   visual/record/rec.sh <maven-repo> [--inside]
# RUNS: space separated tour:device:skin (device "desktop" => desktop pass, skin "-")
# SKINS: directory holding <skin>.skin (default ~/.codenameone), copied into target/record.
set -uo pipefail
HERE="$(cd "$(dirname "$0")/../.." && pwd)"
REPO="$1"
RUNS="${RUNS:-shop:phone:GooglePixel pos:tablet:iPadAir2 merchant:desktop:- admin:desktop:- pos:desktop:-}"
WORK="$HERE/target/record"
if [ "${2:-}" != "--inside" ]; then
  NAME="bazaar-rec-media-$$"
  mkdir -p "$WORK"
  for run in $RUNS; do
    skin="${run##*:}"
    [ "$skin" = - ] || cp "${SKINS:-$HOME/.codenameone}/$skin.skin" "$WORK/"
  done
  podman build -q -t localhost/bazaar-visual-sim "$HERE/visual" >/dev/null
  podman run --name "$NAME" --rm -e "RUNS=$RUNS" -e "FPS=${FPS:-60}" -e "PORTRAIT=${PORTRAIT:-}" -e "TABLET_SCALE=${TABLET_SCALE:-0.625}" -v "$REPO:/ro-repo:ro" -v "$HERE:/app" localhost/bazaar-visual-sim \
    bash -c 'cp -a /ro-repo /repo && (Xvfb :99 -screen 0 1920x1200x24 -nolisten tcp &) && sleep 2 && DISPLAY=:99 bash /app/visual/record/rec.sh /repo --inside'
  exit 0
fi
PORT=18093
OUT="$WORK/out"
mkdir -p "$OUT/shots" "$OUT/video"
WRAP="$HERE/target/rec-mvn"
printf '%s\n' '#!/bin/sh' "exec mvn -Dmaven.repo.local=$REPO \"\$@\"" >"$WRAP"
chmod +x "$WRAP"
export MVN="$WRAP"
"$HERE/backend/server.sh" build --jvm >"$OUT/build.log" 2>&1
for run in $RUNS; do
  IFS=: read -r tour device skin <<<"$run"
  # A fresh database per run, so every tour starts from the seed.
  CN1_PROFILE=test "$HERE/backend/server.sh" run --jvm --prebuilt --port "$PORT" >"$OUT/server-$tour-$device.log" 2>&1 &
  SERVER=$!
  for _ in $(seq 1 120); do curl -fs -o /dev/null "http://localhost:$PORT/oauth2/jwks" && break; sleep 1; done
  OPTS="-Xmx1500m -Dbazaar.server.url=http://localhost:$PORT -Dbazaar.shots.dir=$OUT/shots -Dbazaar.demo.tour=$tour -Dbazaar.demo.dir=$OUT/video -Dbazaar.demo.fps=${FPS:-60}"
  if [ "$device" = desktop ]; then
    PREFS="$WORK/prefs"; NODE="$PREFS/.java/.userPrefs/com/codename1/impl/javase"
    rm -rf "$PREFS"; mkdir -p "$NODE"
    printf '%s\n' '<?xml version="1.0" encoding="UTF-8" standalone="no"?>' '<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">' '<map MAP_XML_VERSION="1.0">' '<entry key="desktopSkin" value="true"/>' '</map>' >"$NODE/prefs.xml"
    OPTS="$OPTS -Dbazaar.e2e.desktop=true -Djava.util.prefs.userRoot=$PREFS -Dcodename1.arg.nativeTheme=native -Dcodename1.arg.desktop.titleBar=native"
  else
    PREFS="$WORK/prefs"; NODE="$PREFS/.java/.userPrefs/com/codename1/impl/javase"
    rm -rf "$PREFS"; mkdir -p "$NODE"; P=true; [ "$device" = tablet ] && P=false; P="${PORTRAIT:-$P}"
    printf '%s\n' '<?xml version="1.0" encoding="UTF-8" standalone="no"?>' '<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">' '<map MAP_XML_VERSION="1.0">' "<entry key=\"Portrait\" value=\"$P\"/>" '</map>' >"$NODE/prefs.xml"
    SCALE=1; [ "$device" = tablet ] && SCALE="${TABLET_SCALE:-0.625}"
    OPTS="$OPTS -Dbazaar.demo.scale=$SCALE -Djava.util.prefs.userRoot=$PREFS -Dbazaar.demo.device=$device -Dskin=$WORK/$skin.skin -Ddskin=$WORK/$skin.skin"
  fi
  LOG="$OUT/run-$tour-$device.log"
  echo "[rec] $run"
  JAVA_TOOL_OPTIONS="$OPTS" setsid "$WRAP" -B -f "$HERE/pom.xml" -Dcodename1.platform=javase -Ptest \
    -Dtest=DemoTourTest -Dsurefire.failIfNoSpecifiedTests=false verify >"$LOG" 2>&1 &
  RUNPID=$!
  while kill -0 "$RUNPID" 2>/dev/null; do
    if grep -qE 'DemoTourTest (passed|failed)' "$LOG"; then sleep 4; kill -- "-$RUNPID" 2>/dev/null; break; fi
    sleep 2
  done
  wait "$RUNPID" 2>/dev/null
  grep -E 'DemoTourTest (passed|failed)|Recorded ' "$LOG" | tail -3
  kill $SERVER 2>/dev/null; wait $SERVER 2>/dev/null
done
echo "[rec] done"
