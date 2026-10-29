#!/usr/bin/env bash
# The committed Android crypto kernels (libcn1crypto.so, one per ABI) must be built from the
# sources in this tree: the stamp build.sh writes has to match a digest of those sources now.
# A mismatch means the kernels or the JNI shim changed and Ports/Android/crypto-jni/build.sh was
# not re-run, so applications would ship the old C behind new Java.
set -euo pipefail
REPO="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$REPO/maven/build-engine/src/main/resources/com/codename1/builders/android-crypto/jniLibs"
want="$(cat "$DIR/SOURCES.sha256" 2>/dev/null || true)"
have="$("$REPO/Ports/Android/crypto-jni/sources-digest.sh")"
if [ "$want" != "$have" ]; then
    echo "check-android-crypto-kernels: libcn1crypto.so is stale (stamp ${want:-missing}, sources $have);" >&2
    echo "  re-run Ports/Android/crypto-jni/build.sh with NDK r28 or newer and commit the result." >&2
    exit 1
fi
for abi in arm64-v8a armeabi-v7a x86_64 x86; do
    if [ ! -s "$DIR/$abi/libcn1crypto.so" ]; then
        echo "check-android-crypto-kernels: $abi/libcn1crypto.so is missing" >&2
        exit 1
    fi
done
echo "check-android-crypto-kernels: libcn1crypto.so matches its sources for 4 ABIs."
