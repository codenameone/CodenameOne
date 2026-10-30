#!/usr/bin/env bash
# Builds and runs bench.c: the shipped crypto kernels next to upstream
# libsecp256k1 (built from the commit the amalgamation records, with its own
# default options), OpenSSL, and libsodium when pkg-config finds it.
#
#   scripts/crypto-bench/native/run.sh [work-dir]
#
# Needs a C compiler, cmake, git and OpenSSL 3 (headers and libcrypto).
# CFLAGS defaults to the Release flags of the translator's Xcode templates
# (vm/ByteCodeTranslator/src/template/template.xcodeproj), so the kernels are
# timed as an iOS or macOS application compiles them.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"
KERNELS="$REPO/vm/ByteCodeTranslator/src"
WORK="${1:-${TMPDIR:-/tmp}/cn1-crypto-bench-native}"
CFLAGS="${CFLAGS:--O3 -fwrapv -fno-strict-aliasing -ffp-contract=off -mllvm -inline-threshold=50}"
CC="${CC:-cc}"
mkdir -p "$WORK"

COMMIT="$(sed -n 's/^ \* libsecp256k1 \([0-9a-f]\{40\}\).*/\1/p' "$KERNELS/cn1_crypto_kernels_amalgamation.h" | head -1)"
if [ -z "$COMMIT" ]; then
    echo "could not read the libsecp256k1 commit from the amalgamation header" >&2
    exit 2
fi
SECP="$WORK/secp256k1"
if [ ! -d "$SECP/.git" ]; then
    git clone -q https://github.com/bitcoin-core/secp256k1 "$SECP"
fi
git -C "$SECP" fetch -q origin "$COMMIT" 2>/dev/null || true
git -C "$SECP" checkout -q "$COMMIT"
if [ ! -f "$WORK/secp-build/lib/libsecp256k1.a" ] && [ ! -f "$WORK/secp-build/src/libsecp256k1.a" ]; then
    cmake -S "$SECP" -B "$WORK/secp-build" -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF \
        -DSECP256K1_BUILD_TESTS=OFF -DSECP256K1_BUILD_EXHAUSTIVE_TESTS=OFF \
        -DSECP256K1_BUILD_BENCHMARK=OFF -DSECP256K1_BUILD_CTIME_TESTS=OFF >/dev/null
    cmake --build "$WORK/secp-build" -j 4 >/dev/null
fi
SECP_LIB="$(find "$WORK/secp-build" -name libsecp256k1.a | head -1)"

OSSL_CFLAGS=""
OSSL_LIBS=""
if pkg-config --exists libcrypto 2>/dev/null; then
    OSSL_CFLAGS="$(pkg-config --cflags libcrypto)"
    OSSL_LIBS="$(pkg-config --libs libcrypto)"
elif command -v brew >/dev/null 2>&1 && [ -d "$(brew --prefix openssl@3 2>/dev/null)" ]; then
    P="$(brew --prefix openssl@3)"
    OSSL_CFLAGS="-I$P/include"
    OSSL_LIBS="-L$P/lib -lcrypto"
else
    OSSL_LIBS="-lcrypto"
fi
SODIUM_FLAGS=""
if pkg-config --exists libsodium 2>/dev/null; then
    SODIUM_FLAGS="-DHAVE_SODIUM $(pkg-config --cflags --libs libsodium)"
fi

# The kernel file exactly as the translator emits it, beside the stand-in
# cn1_globals.h; the amalgamation is found through -I, as __has_include finds
# it beside the file in a translated project.
cp "$KERNELS/cn1_crypto_kernels.c" "$WORK/cn1_crypto_kernels.c"
# shellcheck disable=SC2086
"$CC" $CFLAGS -w -I"$HERE" -I"$KERNELS" -I"$SECP/include" $OSSL_CFLAGS \
    "$HERE/bench.c" "$WORK/cn1_crypto_kernels.c" "$SECP_LIB" $OSSL_LIBS $SODIUM_FLAGS \
    -o "$WORK/bench"
echo "# native: $("$CC" --version | head -1); CFLAGS=$CFLAGS; libsecp256k1 $COMMIT;" \
    "$(openssl version 2>/dev/null || echo OpenSSL)$( [ -n "$SODIUM_FLAGS" ] && echo "; libsodium $(pkg-config --modversion libsodium)")"
"$WORK/bench"
