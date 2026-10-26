#!/usr/bin/env bash
# Builds and runs JvmBench: the core's portable Java engine next to
# BouncyCastle on the same JVM. The core classes are compiled straight from
# CodenameOne/src (the shared, JDK-only ones), so nothing needs to be built first.
#
#   scripts/crypto-bench/jvm-run.sh [work-dir]
#
# BC_JAR names a bcprov jar; otherwise the pinned one is downloaded.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
WORK="${1:-${TMPDIR:-/tmp}/cn1-crypto-bench-jvm}"
BC_VERSION=1.84
JAVA="${JAVA:-java}"
JAVAC="${JAVAC:-javac}"
mkdir -p "$WORK/classes"
BC_JAR="${BC_JAR:-$WORK/bcprov-jdk18on-$BC_VERSION.jar}"
if [ ! -f "$BC_JAR" ]; then
    curl -sSfL -o "$BC_JAR" \
        "https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/$BC_VERSION/bcprov-jdk18on-$BC_VERSION.jar"
fi
SEC="$REPO/CodenameOne/src/com/codename1/security"
"$JAVAC" -nowarn -encoding ASCII -d "$WORK/classes" -cp "$BC_JAR" \
    "$REPO/CodenameOne/src/com/codename1/impl/SharedWithBackend.java" \
    "$SEC/CryptoException.java" "$SEC/CryptoEngine.java" "$SEC/MessageDigestImpl.java" "$SEC/Hash.java" \
    "$SEC/Hmac.java" "$SEC/Pbkdf2.java" "$SEC/Secp256k1.java" "$SEC/Secp256k1Math.java" "$SEC/Schnorr.java" \
    "$SEC/Ed25519.java" "$SEC/Ed25519Math.java" "$HERE/src/JvmBench.java"
echo "# jvm: $("$JAVA" -version 2>&1 | head -1); BouncyCastle $BC_VERSION"
"$JAVA" -cp "$WORK/classes:$BC_JAR" JvmBench
