#!/usr/bin/env bash
# Builds gpu_check.c against the embedded port's renderer and runs it on Mesa's
# llvmpipe, in the embedded dev image, so no GPU is needed:
#
#   scripts/embedded/gpu-check/check-gpu.sh [arch] [out-dir]
#
# Exits non-zero when the GPU renderer draws any scene differently from the
# software renderer; the differing scenes' images are written to out-dir.
set -euo pipefail
ARCH="${1:-arm64}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
OUT="$(mkdir -p "${2:-$HERE/out}" && cd "${2:-$HERE/out}" && pwd)"
ENGINE="${CN1_CONTAINER_ENGINE:-$(command -v docker || command -v podman)}"
IMAGE="cn1-embedded-dev:$ARCH"
if ! "$ENGINE" image inspect "$IMAGE" >/dev/null 2>&1; then
    "$ENGINE" build --platform "linux/$ARCH" -t "$IMAGE" -f "$ROOT/scripts/embedded/Containerfile.dev" "$ROOT/scripts/embedded"
fi
"$ENGINE" run --rm --platform "linux/$ARCH" -v "$ROOT:/src:ro" -v "$OUT:/out" "$IMAGE" bash -euc '
    N=/src/Ports/LinuxPort/nativeSources
    gcc -O2 -Wall -Wextra -Werror -Wno-unused-parameter -DCN1_LINUX_EMBEDDED -DCN1_EMBEDDED_GPU \
        -I"$N" $(pkg-config --cflags libdrm libpng) \
        "$N/cn1_linux_raster.c" "$N/cn1_linux_gpu.c" /src/scripts/embedded/gpu-check/gpu_check.c \
        -o /tmp/gpu_check $(pkg-config --libs gbm egl glesv2 libdrm libpng) -lm -lpthread
    LIBGL_ALWAYS_SOFTWARE=1 /tmp/gpu_check /out'
