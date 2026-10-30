#!/usr/bin/env bash
# Builds and runs batch-check.c: the GPU crypto batches against the CPU ones. Metal on macOS, Vulkan
# elsewhere; on a machine with neither it checks the CPU path alone, unless --require-gpu is given.
#
#   scripts/crypto-gpu/batch-check.sh [entries] [--require-gpu]
#
# WORK names the build directory (default: a fresh one under TMPDIR); CC the compiler.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
KERNELS="$REPO/vm/ByteCodeTranslator/src"
WORK="${WORK:-$(mktemp -d "${TMPDIR:-/tmp}/cn1-crypto-gpu.XXXXXX")}"
CC="${CC:-cc}"
# the kernel file includes cn1_globals.h from its own directory, so it is built from a copy beside
# the bench's stand-in; the rest is found through -I, as it is beside the file in a translated project
cp "$KERNELS/cn1_crypto_kernels.c" "$REPO/scripts/crypto-bench/native/cn1_globals.h" "$WORK/"
FLAGS="-O2 -fwrapv -fno-strict-aliasing -ffp-contract=off -w -I$WORK -I$KERNELS"
# shellcheck disable=SC2086
if [ "$(uname -s)" = Darwin ]; then
    "$CC" $FLAGS -c "$HERE/batch-check.c" -o "$WORK/check.o"
    "$CC" $FLAGS -fobjc-arc -c "$KERNELS/cn1_crypto_gpu_metal.m" -o "$WORK/metal.o"
    "$CC" "$WORK/check.o" "$WORK/metal.o" -framework Metal -framework Foundation -o "$WORK/check"
else
    "$CC" $FLAGS "$HERE/batch-check.c" -o "$WORK/check" -lpthread -ldl
fi
"$WORK/check" "$@"
