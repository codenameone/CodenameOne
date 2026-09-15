#!/usr/bin/env bash
###
# Build and run the GNOME (GTK4 + libadwaita) native reference app.
#
# Counterpart to scripts/build-ios-native-ref.sh and scripts/build-android-native-ref.sh.
# Unlike those two this one is intended to run on a hosted CI runner, because that runner
# is the closest thing available to a default-configured GNOME desktop -- see
# .github/workflows/fidelity-desktop-native-ref.yml for why the capture moved to CI for
# the desktop platforms and stayed local for the mobile ones.
#
# Honours NATIVEREF_MODE (probe|capture) and CN1SS_FIDELITY_GOLDEN_SET.
###
set -euo pipefail

log() { echo "[build-gnome-native-ref] $1" >&2; }

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$REPO_ROOT"

SRC="$REPO_ROOT/scripts/fidelity-app/gnome-native-ref/native-ref.c"
BUILD="$(mktemp -d "${TMPDIR:-/tmp}/cn1-gnome-ref-XXXXXX")"
OUT_DIR="$REPO_ROOT/artifacts/desktop-native-ref/gnome"
mkdir -p "$OUT_DIR"

log "Compiling $SRC"
cc -O1 -std=c11 -Wall -o "$BUILD/native-ref" "$SRC" \
    $(pkg-config --cflags --libs gtk4 libadwaita-1)

# A bare Xvfb has no window manager, so nothing ever takes focus and every GTK toplevel
# renders in its dimmed GTK_STATE_FLAG_BACKDROP style. openbox is about a megabyte, adds
# no compositing (GTK4 draws its own client-side shadows into its own surface, so the
# tile capture needs no compositor) and makes activation real. native-ref.c asserts
# gtk_window_is_active rather than trusting this to have worked.
export DISPLAY=:99
Xvfb :99 -screen 0 1600x1200x24 >"$BUILD/xvfb.log" 2>&1 &
XVFB_PID=$!
trap 'kill "$XVFB_PID" "${WM_PID:-}" 2>/dev/null || true' EXIT
for _ in $(seq 1 50); do
  if xdpyinfo -display :99 >/dev/null 2>&1; then break; fi
  sleep 0.2
done
openbox --sm-disable >"$BUILD/openbox.log" 2>&1 &
WM_PID=$!
sleep 1

# The Vulkan renderer GTK 4.14+ prefers is unpredictable under llvmpipe; cairo is fully
# software and deterministic, which is the same property scripts/linux/screenshots relies
# on for the Linux port baselines. Recorded in the manifest either way, because if a soft
# shadow ever looks wrong this is the first thing to question.
# GTK warns loudly on every run that it cannot reach the accessibility bus and tells you to
# set this; there is no a11y bus on a bare Xvfb and the reference does not need one.
export GTK_A11Y=none
export GSK_RENDERER="${GSK_RENDERER:-cairo}"
export LIBGL_ALWAYS_SOFTWARE=1
export GDK_SCALE=1
export GDK_DPI_SCALE=1
export NATIVEREF_OUT="$OUT_DIR"
export NATIVEREF_MODE="${NATIVEREF_MODE:-probe}"

log "Running (mode=$NATIVEREF_MODE, renderer=$GSK_RENDERER)"
set +e
dbus-run-session -- "$BUILD/native-ref"
rc=$?
set -e

if [ "$rc" -ne 0 ]; then
  log "FAILED: the reference app exited $rc; see the BLOCKER lines above."
  log "A blocker means the capture would have been quietly wrong, not that it crashed."
  exit "$rc"
fi
log "Wrote $(ls -1 "$OUT_DIR" | wc -l | tr -d ' ') file(s) to $OUT_DIR"
