#!/usr/bin/env bash
#
# Design-review captures of the register's sheets and the merchant charts,
# light and dark, with no window on the host. The simulator cannot start
# under java.awt.headless, so off Linux the script runs itself in a podman
# container under Xvfb; the Maven repository is mounted read-only and copied.
#
#   visual/capture.sh <maven-repo>
#
# PNGs land in common/target/visual (DemoTourTest, tour "visual").
#
# BAZAAR_VISUAL_DEVICE picks the window: "desktop" is the desktop skin at
# 1920x1200, as visual/record/rec.sh records it; "tablet" and "phone" use the
# iPadAir2 (landscape) and GooglePixel skins, copied from SKINS (default
# ~/.codenameone) into target/visual-skins. Unset keeps BAZAAR_VISUAL_SKIN.
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
REPO="${1:?usage: visual/capture.sh <maven-repo>}"
if [ "${2:-}" != "--inside" ]; then
  NAME="bazaar-visual-$$"
  case "${BAZAAR_VISUAL_DEVICE:-}" in
    tablet|phone)
      SK=GooglePixel; [ "$BAZAAR_VISUAL_DEVICE" = tablet ] && SK=iPadAir2
      mkdir -p "$HERE/target/visual-skins"
      cp "${SKINS:-$HOME/.codenameone}/$SK.skin" "$HERE/target/visual-skins/"
      BAZAAR_VISUAL_SKIN="/app/target/visual-skins/$SK.skin";;
  esac
  podman build -q -t localhost/bazaar-visual-sim "$HERE/visual" >/dev/null
  trap 'podman rm -f "$NAME" >/dev/null 2>&1 || true' EXIT
  # The Codename One Maven plugin wants the build client even for a
  # simulator run; lend the host's, read-only.
  CLIENT=()
  if [ -f "$HOME/.codenameone/CodeNameOneBuildClient.jar" ]; then
    CLIENT=(-v "$HOME/.codenameone/CodeNameOneBuildClient.jar:/root/.codenameone/CodeNameOneBuildClient.jar:ro")
  fi
  podman run --name "$NAME" --rm -e "BAZAAR_VISUAL_OUT=${BAZAAR_VISUAL_OUT:-visual}" -e "BAZAAR_VISUAL_TOUR=${BAZAAR_VISUAL_TOUR:-visual}" -e "BAZAAR_VISUAL_MODES=${BAZAAR_VISUAL_MODES:-false true}" -e "BAZAAR_VISUAL_SKIN=${BAZAAR_VISUAL_SKIN:-}" -e "BAZAAR_VISUAL_DEVICE=${BAZAAR_VISUAL_DEVICE:-}" -v "$REPO:/ro-repo:ro" "${CLIENT[@]}" -v "$HERE:/app" localhost/bazaar-visual-sim \
    bash -c 'cp -a /ro-repo /repo && (Xvfb :99 -screen 0 1920x1200x24 -nolisten tcp &) && sleep 2 && DISPLAY=:99 bash /app/visual/capture.sh /repo --inside'
  exit 0
fi
PORT="${BAZAAR_VISUAL_PORT:-18083}"
OUT="$HERE/common/target/${BAZAAR_VISUAL_OUT:-visual}"
mkdir -p "$OUT" "$HERE/target"
WRAP="$HERE/target/visual-mvn"
printf '%s\n' '#!/bin/sh' "exec mvn -Dmaven.repo.local=$REPO \"\$@\"" >"$WRAP"
chmod +x "$WRAP"
export MVN="$WRAP"

"$HERE/backend/server.sh" build --jvm
CN1_PROFILE=test BAZAAR_DEMO_PIPELINE=true BAZAAR_CARRIER_SIMULATED_STEP_SECONDS=8 "$HERE/backend/server.sh" run --jvm --prebuilt --port "$PORT" \
  >"$HERE/target/visual-server.log" 2>&1 &
SERVER=$!
trap 'kill $SERVER 2>/dev/null || true' EXIT
for _ in $(seq 1 120); do
  curl -fs -o /dev/null "http://localhost:$PORT/oauth2/jwks" && break
  sleep 1
done

BASE="-Xmx1g -Dbazaar.server.url=http://localhost:$PORT -Dbazaar.shots.dir=$OUT -Dbazaar.demo.tour=${BAZAAR_VISUAL_TOUR:-visual}"
# cn1:test runs every test class in one simulator JVM and has no filter of
# its own (-Dtest only narrows surefire); the other classes pass without doing
# anything on a tour run, but each re-initialises the display and reloads the
# skin until the heap is gone and the run hangs. So the run is started in a
# process group of its own (this is inside the container) and that group is
# stopped once the tour is through.
SKIN_OPT=""
if [ -n "${BAZAAR_VISUAL_SKIN:-}" ]; then
  SKIN_OPT="-Dskin=$BAZAAR_VISUAL_SKIN -Ddskin=$BAZAAR_VISUAL_SKIN"
fi
PREFS="$HERE/target/visual-prefs"; NODE="$PREFS/.java/.userPrefs/com/codename1/impl/javase"
rm -rf "$PREFS"; mkdir -p "$NODE"
case "${BAZAAR_VISUAL_DEVICE:-}" in
  desktop)
    printf '%s\n' '<?xml version="1.0" encoding="UTF-8" standalone="no"?>' '<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">' '<map MAP_XML_VERSION="1.0">' '<entry key="desktopSkin" value="true"/>' '</map>' >"$NODE/prefs.xml"
    SKIN_OPT="-Djava.util.prefs.userRoot=$PREFS -Dbazaar.e2e.desktop=true -Dcodename1.arg.nativeTheme=native -Dcodename1.arg.desktop.titleBar=native";;
  tablet|phone)
    P=true; [ "$BAZAAR_VISUAL_DEVICE" = tablet ] && P=false
    printf '%s\n' '<?xml version="1.0" encoding="UTF-8" standalone="no"?>' '<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">' '<map MAP_XML_VERSION="1.0">' "<entry key=\"Portrait\" value=\"$P\"/>" '</map>' >"$NODE/prefs.xml"
    SKIN_OPT="$SKIN_OPT -Djava.util.prefs.userRoot=$PREFS -Dbazaar.demo.device=$BAZAAR_VISUAL_DEVICE";;
esac
for dark in ${BAZAAR_VISUAL_MODES:-false true}; do
  echo "[visual] dark=$dark"
  LOG="$HERE/target/visual-$dark.log"
  JAVA_TOOL_OPTIONS="$BASE $SKIN_OPT -Dbazaar.visual.dark=$dark" setsid "$WRAP" -B -f "$HERE/pom.xml" \
    -Dcodename1.platform=javase -Ptest -Dtest=DemoTourTest -Dsurefire.failIfNoSpecifiedTests=false \
    verify >"$LOG" 2>&1 &
  RUNPID=$!
  while kill -0 "$RUNPID" 2>/dev/null; do
    if grep -qE 'DemoTourTest (passed|failed)' "$LOG"; then
      sleep 2
      kill -- "-$RUNPID" 2>/dev/null || true
      break
    fi
    sleep 2
  done
  wait "$RUNPID" 2>/dev/null || true
  grep -E 'DemoTourTest (passed|failed)' "$LOG" || { echo "[visual] tour did not finish, see $LOG"; tail -20 "$LOG"; }
done
ls "$OUT"
