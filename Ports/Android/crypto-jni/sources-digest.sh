#!/usr/bin/env bash
# One SHA-256 over every input of libcn1crypto.so, in a fixed order; build.sh records it beside the
# libraries and scripts/check-android-crypto-kernels.sh recomputes it.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"
cat "$REPO/vm/ByteCodeTranslator/src/cn1_crypto_kernels.c" \
    "$REPO/vm/ByteCodeTranslator/src/cn1_crypto_kernels_amalgamation.h" \
    "$HERE/cn1_globals.h" "$HERE/cn1_crypto_jni.c" "$HERE/build.sh" | shasum -a 256 | cut -d' ' -f1
