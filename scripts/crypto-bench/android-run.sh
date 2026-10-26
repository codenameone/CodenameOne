#!/usr/bin/env bash
# The JVM benchmark on ART, against a connected device or emulator (adb), twice: the core rows on
# the Java engine, then on the JNI kernels an application gets (libcn1crypto.so, built by
# Ports/Android/crypto-jni/build.sh). Both arms run BouncyCastle as the reference in the same
# process; the kernels arm first checks the kernels answer what the Java engine answers.
#
#   scripts/crypto-bench/android-run.sh [work-dir]
#
# Needs JAVA17_HOME (d8), ANDROID_HOME and one device on adb. Reported, not gated: an emulator's
# numbers move with the host.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
WORK="${1:-${TMPDIR:-/tmp}/cn1-crypto-bench-android}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="$SDK/platform-tools/adb"
D8="$(ls -d "$SDK"/build-tools/*/d8 | sort -V | tail -1)"
J17="${JAVA17_HOME:?JAVA17_HOME must name a JDK 17 or newer}"
BC_VERSION=1.84
BC_JAR="$WORK/bcprov-jdk18on-$BC_VERSION.jar"
mkdir -p "$WORK/classes"
if [ ! -f "$BC_JAR" ]; then
    curl -sSfL -o "$BC_JAR" \
        "https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/$BC_VERSION/bcprov-jdk18on-$BC_VERSION.jar"
fi
SEC="$REPO/CodenameOne/src/com/codename1/security"
"$J17/bin/javac" -nowarn -encoding ASCII --release 8 -d "$WORK/classes" -cp "$BC_JAR" \
    "$REPO/CodenameOne/src/com/codename1/impl/SharedWithBackend.java" \
    "$SEC/CryptoException.java" "$SEC/CryptoEngine.java" "$SEC/NativeCryptoEngine.java" \
    "$SEC/NativeCryptoKernels.java" "$SEC/MessageDigestImpl.java" "$SEC/Hash.java" \
    "$SEC/Hmac.java" "$SEC/Pbkdf2.java" "$SEC/Secp256k1.java" "$SEC/Secp256k1Math.java" "$SEC/Schnorr.java" \
    "$SEC/Ed25519.java" "$SEC/Ed25519Math.java" "$HERE/src/JvmBench.java"
( cd "$WORK/classes" && "$J17/bin/jar" cf "$WORK/bench.jar" . )
JAVA_HOME="$J17" PATH="$J17/bin:$PATH" "$D8" --release --min-api 26 --output "$WORK/bench-dex.zip" "$WORK/bench.jar" "$BC_JAR"

ABI="$("$ADB" shell getprop ro.product.cpu.abi | tr -d '\r')"
LIB="$REPO/maven/build-engine/src/main/resources/com/codename1/builders/android-crypto/jniLibs/$ABI/libcn1crypto.so"
DEV=/data/local/tmp/cn1-crypto-bench
"$ADB" shell mkdir -p "$DEV" >/dev/null
"$ADB" push "$WORK/bench-dex.zip" "$DEV/bench.zip" >/dev/null
"$ADB" push "$LIB" "$DEV/libcn1crypto.so" >/dev/null
echo "# android: $("$ADB" shell getprop ro.product.model | tr -d '\r'), API $("$ADB" shell getprop ro.build.version.sdk | tr -d '\r'), $ABI; BouncyCastle $BC_VERSION"
echo "## Java engine"
"$ADB" shell "cd $DEV && dalvikvm -Djava.library.path=$DEV -cp $DEV/bench.zip JvmBench"
echo "## native kernels"
"$ADB" shell "cd $DEV && dalvikvm -Djava.library.path=$DEV -Dbench.kernels=cn1crypto -cp $DEV/bench.zip JvmBench"
