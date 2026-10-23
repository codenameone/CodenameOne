#!/usr/bin/env bash
#
# Fills scripts/reel/media with the demo catalog's video: downloads the Blender
# open movies (CC BY, from download.blender.org) into scripts/reel/sources,
# generates the academy lessons and the ads, packages each with fresh random
# keys and writes media/seed.json, which a development server reads at start-up.
#
#   tools/fetch-demo-media.sh [--small] [--films bbb,sintel,tos,ed]
#
# --small keeps the first minute of each film, which is enough to see every
# screen and takes a fraction of the time and space. A second run packages only
# what is missing. Neither directory is ever committed.
#
# Needs JDK 17 or newer, Maven, and ffmpeg and ffprobe on the PATH.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$(cd "$HERE/.." && pwd)"
MVN="${MVN:-mvn}"

for tool in ffmpeg ffprobe; do
  command -v "$tool" >/dev/null 2>&1 || { echo "[reel-media] $tool is not on the PATH" >&2; exit 1; }
done

JAR="$HERE/packager/target/reel-packager-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR" ]; then
  echo "[reel-media] building the packager"
  "$MVN" -B -q -f "$HERE/packager/pom.xml" package -DskipTests
fi

# The packager needs Java 17; prefer JAVA_HOME over whatever java is on PATH.
JAVA=java
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVA="$JAVA_HOME/bin/java"; fi
exec "$JAVA" -jar "$JAR" seed --media "$APP/media" "$@"
