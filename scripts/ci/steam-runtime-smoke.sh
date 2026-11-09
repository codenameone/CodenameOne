#!/usr/bin/env bash
# Runs a native Linux executable inside the Steam Linux Runtime 3 ("sniper")
# platform image, the container Steam launches Linux games in, and fails when
# the binary needs a library the runtime lacks or dies on start-up.
#
#   scripts/ci/steam-runtime-smoke.sh <elf> [seconds]
#
# It needs docker and an X server: the host's Xvfb socket is shared with the
# container. The binary passes when it is still running after <seconds> (default
# 15), or exits 0 before then.
set -euo pipefail

ELF="${1:?usage: steam-runtime-smoke.sh <elf> [seconds]}"
SECS="${2:-15}"
# Deliberately the moving tag: Steam ships the current runtime to players, so a
# new one that breaks the binary is a failure worth seeing on the next PR.
IMAGE="${CN1_STEAM_RUNTIME_IMAGE:-registry.gitlab.steamos.cloud/steamrt/sniper/platform:latest}"
DISPLAY_NUM="${CN1_STEAM_SMOKE_DISPLAY:-:97}"

if [ ! -f "$ELF" ]; then
  echo "steam-runtime-smoke: $ELF does not exist" >&2
  exit 1
fi
DIR="$(cd "$(dirname "$ELF")" && pwd)"
NAME="$(basename "$ELF")"

Xvfb "$DISPLAY_NUM" -ac -screen 0 1280x800x24 >/dev/null 2>&1 &
XPID=$!
trap 'kill $XPID 2>/dev/null || true' EXIT
sleep 2

docker pull -q "$IMAGE" >/dev/null

# 1. Every DT_NEEDED resolves inside the runtime.
MISSING="$(docker run --rm -v "$DIR:/game:ro" "$IMAGE" ldd "/game/$NAME" | grep 'not found' || true)"
if [ -n "$MISSING" ]; then
  echo "steam-runtime-smoke: libraries missing from the Steam Linux Runtime:" >&2
  echo "$MISSING" >&2
  exit 1
fi

# 2. It starts and keeps running, full screen as under Gaming Mode.
rc=0
# --ipc=host: GTK and cairo attach MIT-SHM segments the X server must see.
docker run --rm --ipc=host -v "$DIR:/game:ro" -v /tmp/.X11-unix:/tmp/.X11-unix \
  -e DISPLAY="$DISPLAY_NUM" -e LIBGL_ALWAYS_SOFTWARE=1 -e SteamGamepadUI=1 \
  "$IMAGE" timeout "$SECS" "/game/$NAME" || rc=$?
if [ "$rc" -eq 124 ] || [ "$rc" -eq 0 ]; then
  echo "steam-runtime-smoke: $NAME ran in the Steam Linux Runtime (${SECS}s)"
  exit 0
fi
echo "steam-runtime-smoke: $NAME exited with $rc inside the Steam Linux Runtime" >&2
exit 1
