#!/usr/bin/env bash
# Builds libcn1crypto.so -- the ParparVM crypto kernels (libsecp256k1, ed25519-donna, Keccak, and the
# Vulkan batches, whose loader is opened at run time) behind JNI entry points for
# com.codename1.security.NativeCryptoKernels -- for the four Android ABIs, into
# the build engine's resources. The Android builder copies them into an application's jniLibs only
# when the application uses the crypto API (Executor.DatabaseUsage.usesCryptoKernels).
#
# NDK r28 or newer: the first release that links for 16 KB pages by default, which Google Play
# requires of every 64-bit library (scripts/check-16k-page-alignment.py gates the output). Its API
# floor is 21; AndroidImplementation loads the library under a guard, so an older device keeps the
# Java engine instead of failing to start.
#
#   Ports/Android/crypto-jni/build.sh [path-to-ndk]
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
NDK="${1:-${ANDROID_NDK_HOME:-$(ls -d "$SDK"/ndk/28.* 2>/dev/null | sort -V | tail -1)}}"
if [ -z "$NDK" ] || [ ! -d "$NDK" ]; then
    echo "NDK r28 or newer not found; pass its path" >&2
    exit 2
fi
MAJOR="$(sed -n 's/^Pkg.Revision *= *\([0-9]*\).*/\1/p' "$NDK/source.properties")"
if [ "${MAJOR:-0}" -lt 28 ]; then
    echo "NDK $MAJOR is older than r28, whose libraries are not 16 KB aligned by default" >&2
    exit 2
fi
HOST="$(ls "$NDK/toolchains/llvm/prebuilt" | head -1)"
BIN="$NDK/toolchains/llvm/prebuilt/$HOST/bin"
OUT="$REPO/maven/build-engine/src/main/resources/com/codename1/builders/android-crypto/jniLibs"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cn1-crypto-jni.XXXXXX")"

# The kernel file includes "cn1_globals.h" from its own directory, so it is compiled from a copy
# that sits beside this directory's shim rather than beside the ParparVM runtime's header.
cp "$REPO/vm/ByteCodeTranslator/src/cn1_crypto_kernels.c" \
   "$REPO/vm/ByteCodeTranslator/src/cn1_crypto_kernels_amalgamation.h" \
   "$REPO"/vm/ByteCodeTranslator/src/cn1_crypto_gpu.h "$REPO"/vm/ByteCodeTranslator/src/cn1_crypto_gpu_*.h \
   "$HERE/cn1_globals.h" "$HERE/cn1_crypto_jni.c" "$WORK/"

API=21
for spec in arm64-v8a:aarch64-linux-android armeabi-v7a:armv7a-linux-androideabi \
            x86_64:x86_64-linux-android x86:i686-linux-android; do
    abi="${spec%%:*}"
    triple="${spec#*:}"
    mkdir -p "$OUT/$abi"
    "$BIN/clang" --target="$triple$API" -O3 -fPIC -fwrapv -fno-strict-aliasing -ffp-contract=off \
        -fvisibility=hidden -ffunction-sections -fdata-sections -w \
        -shared -Wl,-soname,libcn1crypto.so -Wl,--gc-sections -Wl,-z,max-page-size=16384 \
        -Wl,--build-id=none -o "$OUT/$abi/libcn1crypto.so" "$WORK/cn1_crypto_jni.c"
    "$BIN/llvm-strip" --strip-unneeded "$OUT/$abi/libcn1crypto.so"
    echo "$abi: $(wc -c < "$OUT/$abi/libcn1crypto.so") bytes"
done

# The stamp scripts/check-android-crypto-kernels.sh compares against the sources, so a change to
# the kernels that is not followed by this script fails CI instead of shipping stale binaries.
"$HERE/sources-digest.sh" > "$OUT/SOURCES.sha256"
