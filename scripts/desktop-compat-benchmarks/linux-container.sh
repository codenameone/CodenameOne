#!/usr/bin/env bash
#
# Runs a command inside the bench-linux container with this checkout mounted at
# the path it has on the host, so a Maven repository or an application
# directory named by an absolute path means the same thing on both sides.
#
# Usage: linux-container.sh <container-name-suffix> <command> [args...]
#
# Environment:
#   BENCH_IMAGE     image to run (default: bench-linux)
#   BENCH_VOLUME    named volume mounted at /work (default: bench-work). Builds
#                   go there rather than on the shared host mount: a translated
#                   application is tens of thousands of small C files, and a
#                   virtiofs mount makes that the slowest part of the build.
#   BENCH_MOUNTS    extra host directories to mount read-only, colon separated
#
# The container is privileged so the benchmark can drop the page cache for its
# cold-start samples; without that it falls back to evicting only the
# application's own files and says so in its output.
set -euo pipefail

if [ "$#" -lt 2 ]; then
  echo "usage: linux-container.sh <name-suffix> <command> [args...]" >&2
  exit 2
fi
SUFFIX="$1"
shift

HERE="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)"
ROOT="$(CDPATH='' cd -- "$HERE/../.." && pwd)"
IMAGE="${BENCH_IMAGE:-bench-linux}"
VOLUME="${BENCH_VOLUME:-bench-work}"

MOUNTS=(-v "$ROOT:$ROOT" -v "$VOLUME:/work")
if [ -n "${BENCH_MOUNTS:-}" ]; then
  IFS=':' read -r -a EXTRA <<< "$BENCH_MOUNTS"
  for DIR in "${EXTRA[@]}"; do
    MOUNTS+=(-v "$DIR:$DIR:ro")
  done
fi

exec podman run --rm --privileged --name "bench-$SUFFIX" "${MOUNTS[@]}" -w "$ROOT" "$IMAGE" "$@"
