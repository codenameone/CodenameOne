#!/bin/bash
# Generates the JNI shim layer for the iOS port's IOSNative class and
# cross-checks the generated ParparVM symbol names against the symbols
# actually defined by Ports/iOSPort/nativeSources - validating that the
# generator's mangling matches the handwritten native code.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

CORE_JAR="$HOME/.m2/repository/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar"
if [ -d "$REPO_ROOT/maven/core/target/classes" ]; then
    CORE_JAR="$REPO_ROOT/maven/core/target/classes"
fi

OUT_DIR="${1:-$REPO_ROOT/maven/cn1-sim-native/generator/target/generated-sources/cn1sim}"
BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT

JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

echo "[generate-ios-shims] Compiling IOSNative + generator"
"$JAVAC" -nowarn -cp "$CORE_JAR" -sourcepath "$REPO_ROOT/Ports/iOSPort/src" -d "$BUILD_DIR" \
    "$REPO_ROOT/Ports/iOSPort/src/com/codename1/impl/ios/IOSNative.java" \
    "$REPO_ROOT"/maven/cn1-sim-native/generator/src/main/java/com/codename1/simnative/gen/*.java

echo "[generate-ios-shims] Extracting defined symbols from nativeSources"
DEFINED="$BUILD_DIR/defined-symbols.txt"
grep -ohE "com_codename1_impl_ios_IOSNative_[A-Za-z0-9_]+" \
    "$REPO_ROOT"/Ports/iOSPort/nativeSources/*.m \
    "$REPO_ROOT"/Ports/iOSPort/nativeSources/*.h 2>/dev/null | sort -u > "$DEFINED"

echo "[generate-ios-shims] Generating shims into $OUT_DIR"
"$JAVA" -cp "$BUILD_DIR:$CORE_JAR" com.codename1.simnative.gen.ShimGenerator \
    com.codename1.impl.ios.IOSNative "$OUT_DIR" ios "$DEFINED"

TOTAL=$(wc -l < "$OUT_DIR/cn1sim_symbols_ios.txt" | tr -d ' ')
UNRESOLVED=$(wc -l < "$OUT_DIR/cn1sim_unresolved_ios.txt" | tr -d ' ')
echo "[generate-ios-shims] $TOTAL methods, $UNRESOLVED unresolved (weak-stubbed)"
if [ "$UNRESOLVED" -gt 0 ]; then
    echo "[generate-ios-shims] Unresolved (need weak stubs or signature fixes):"
    cat "$OUT_DIR/cn1sim_unresolved_ios.txt"
fi
# A large unresolved fraction means the mangling logic regressed
if [ "$UNRESOLVED" -gt $((TOTAL / 20)) ]; then
    echo "[generate-ios-shims] More than 5% unresolved - mangling regression suspected" >&2
    exit 1
fi
echo "[generate-ios-shims] Symbol resolution OK"
