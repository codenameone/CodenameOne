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
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
REPO="${1:?usage: visual/capture.sh <maven-repo>}"
if [ "${2:-}" != "--inside" ]; then
  NAME="bazaar-visual-$$"
  podman build -q -t localhost/bazaar-visual-sim "$HERE/visual" >/dev/null
  trap 'podman rm -f "$NAME" >/dev/null 2>&1 || true' EXIT
  podman run --name "$NAME" --rm -v "$REPO:/ro-repo:ro" -v "$HERE:/app" localhost/bazaar-visual-sim \
    bash -c 'cp -a /ro-repo /repo && (Xvfb :99 -screen 0 1600x1200x24 -nolisten tcp &) && sleep 2 && DISPLAY=:99 bash /app/visual/capture.sh /repo --inside'
  exit 0
fi
PORT="${BAZAAR_VISUAL_PORT:-18083}"
OUT="$HERE/common/target/visual"
mkdir -p "$OUT" "$HERE/target"
WRAP="$HERE/target/visual-mvn"
printf '%s\n' '#!/bin/sh' "exec mvn -Dmaven.repo.local=$REPO \"\$@\"" >"$WRAP"
chmod +x "$WRAP"
export MVN="$WRAP"

"$HERE/backend/server.sh" build --jvm
CN1_PROFILE=test "$HERE/backend/server.sh" run --jvm --prebuilt --port "$PORT" \
  >"$HERE/target/visual-server.log" 2>&1 &
SERVER=$!
trap 'kill $SERVER 2>/dev/null || true' EXIT
for _ in $(seq 1 120); do
  curl -fs -o /dev/null "http://localhost:$PORT/oauth2/jwks" && break
  sleep 1
done

BASE="-Xmx1g -Dbazaar.server.url=http://localhost:$PORT -Dbazaar.shots.dir=$OUT -Dbazaar.demo.tour=visual"
for dark in false true; do
  echo "[visual] dark=$dark"
  JAVA_TOOL_OPTIONS="$BASE -Dbazaar.visual.dark=$dark" "$WRAP" -B -f "$HERE/pom.xml" \
    -Dcodename1.platform=javase -Ptest verify >"$HERE/target/visual-$dark.log" 2>&1 \
    || { echo "[visual] run failed, see target/visual-$dark.log"; tail -20 "$HERE/target/visual-$dark.log"; }
done
ls "$OUT"
