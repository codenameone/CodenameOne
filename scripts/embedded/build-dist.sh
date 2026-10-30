#!/usr/bin/env bash
# Builds a translated application (the dist/ that translate.sh or the
# linux-native builder wrote) into a native binary for an embedded board, in a
# container so the host needs nothing but Docker or Podman.
#
#   scripts/embedded/build-dist.sh <out-dir> [arch] [image]
#
#   <out-dir>  the directory translate.sh wrote; dist/CMakeLists.txt inside it
#   arch       arm64 (default), arm (armv7 hard float), amd64 or riscv64
#   image      a Debian based image with the build dependencies; the default,
#              cn1-embedded-dev:<arch>, is built from Containerfile.dev here
#
# The binary lands in <out-dir>/build-<arch>/, stripped by the generated
# project, with its symbols in a .debug file beside it. A foreign architecture runs under QEMU user emulation,
# which Docker Desktop provides and a Linux host gets from qemu-user-static: it
# is several times slower than a cross compiler, but it is the board's own
# compiler and the board's own libraries, so what links here links there.
#
# The generated CMakeLists.txt names its sources by absolute path, so the
# directory is mounted at the same path inside the container.
set -euo pipefail

OUT="$(cd "${1:?usage: build-dist.sh <out-dir> [arch] [image]}" && pwd)"
ARCH="${2:-arm64}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENGINE="${CN1_CONTAINER_ENGINE:-$(command -v docker || command -v podman)}"
IMAGE="${3:-cn1-embedded-dev:$ARCH}"

[ -f "$OUT/dist/CMakeLists.txt" ] || { echo "build-dist: $OUT/dist/CMakeLists.txt not found" >&2; exit 2; }
case "$ARCH" in arm64|arm|amd64|riscv64) ;; *) echo "build-dist: unknown arch $ARCH" >&2; exit 2;; esac

if [ -z "${3:-}" ] && ! "$ENGINE" image inspect "$IMAGE" >/dev/null 2>&1; then
    echo "build-dist: building $IMAGE"
    "$ENGINE" build --platform "linux/$ARCH" -t "$IMAGE" -f "$HERE/Containerfile.dev" "$HERE"
fi

BUILD="$OUT/build-$ARCH"
"$ENGINE" run --rm --platform "linux/$ARCH" -v "$OUT:$OUT" "$IMAGE" bash -euc "
    cmake -S '$OUT/dist' -B '$BUILD' -DCMAKE_BUILD_TYPE=Release ${CN1_CMAKE_ARGS:-}
    cmake --build '$BUILD' -j\$(nproc)
    # The generated project already splits the symbols into <exe>.debug and
    # strips the binary; splitting again would overwrite them with nothing.
    find '$BUILD' -maxdepth 1 -type f -perm -u+x ! -name '*.debug' -exec ls -l {} +"
